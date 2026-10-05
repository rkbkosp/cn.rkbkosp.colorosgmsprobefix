package cn.rkbkosp.colorosgmsprobefix;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Real HTTP 204 verification; never fabricates unconditional network success. */
public final class GoogleProbeFix implements IXposedHookLoadPackage {
    private static final String DETECTOR =
            "com.oplus.battery.restrictdynamicfeature.google.NetworkDetector";
    private static final String RESULT = DETECTOR + "$DetectorResult";
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 7890;
    private static final int TIMEOUT_MS = 3000;
    private static boolean installed;
    private static boolean waitingForAttach;
    private static volatile boolean probeSucceeded;
    private static final String CONTROLLER =
            "com.oplus.battery.restrictdynamicfeature.google.GoogleRestrictionController";

    private static void log(String text) {
        XposedBridge.log("[ColorOSGmsProbeFix] " + text);
    }

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p) {
        if (!"com.oplus.battery".equals(p.packageName)
                && !"com.oplus.athena".equals(p.packageName)) return;
        if (!"com.oplus.athena".equals(p.processName)) return;
        log("load " + p.packageName + " in " + p.processName);
        if (install(p.classLoader)) return;
        synchronized (GoogleProbeFix.class) {
            if (waitingForAttach) return;
            waitingForAttach = true;
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class,
                    new XC_MethodHook() {
                        @Override protected void afterHookedMethod(MethodHookParam p) {
                            install(((Context) p.args[0]).getClassLoader());
                        }
                    });
        }
    }

    private static synchronized boolean install(final ClassLoader loader) {
        if (installed) return true;
        try {
            Class<?> detector = XposedHelpers.findClass(DETECTOR, loader);
            // Shared-process scopes can load two copies of this module. Coordinate
            // in the process VM so the same target class receives one hook only.
            synchronized (System.getProperties()) {
                String marker = "cn.rkbkosp.colorosgmsprobefix.hooked." + System.identityHashCode(detector);
                if ("0.2.1".equals(System.getProperty(marker))) {
                    installed = true;
                    return true;
                }
                Class<?> result = XposedHelpers.findClass(RESULT, loader);
                Method method = detector.getDeclaredMethod("a", Context.class, int.class);
                if (method.getReturnType() != result)
                    throw new NoSuchMethodException("unexpected detector signature");
                final Object wifiSuccess = enumValue(result, "RESULT_WIFI_SUCCESS");
                final Object mobileSuccess = enumValue(result, "RESULT_MOBILE_SUCCESS");
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam p) {
                        try {
                            probeSucceeded = false;
                            log("detector invoked; attempt=" + p.args[1]);
                            Context context = (Context) p.args[0];
                            ConnectivityManager cm = (ConnectivityManager)
                                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
                            NetworkInfo network = cm == null ? null : cm.getActiveNetworkInfo();
                            if (network == null || !network.isConnected()) {
                                log("no connected network; continue original detector");
                                return;
                            }
                            if (!proxyListening()) {
                                log("proxy not ready; continue original detector");
                                return;
                            }
                            for (String address : addresses(context, loader)) {
                                if (probe204(address)) {
                                    probeSucceeded = true;
                                    p.setResult(network.getType() == ConnectivityManager.TYPE_WIFI
                                            ? wifiSuccess : mobileSuccess);
                                    log("real HTTP 204 via Mihomo; Google detection succeeds");
                                    return;
                                }
                            }
                            log("proxy probes failed; continue original detector");
                        } catch (Throwable error) {
                            // Probe errors must not crash Athena or manufacture a success.
                            log("probe exception; continue original detector: " + error);
                        }
                    }
                    @Override protected void afterHookedMethod(MethodHookParam p) {
                        log("detector returned=" + p.getResult());
                    }
                });
                installed = true;
                log("installed NetworkDetector.a(Context,int); version=0.2.1");
                installReadinessCheck(loader);
                System.setProperty(marker, "0.2.1");
                return true;
            }
        } catch (XposedHelpers.ClassNotFoundError missing) {
            return false;
        } catch (Throwable error) {
            log("firmware signature mismatch or hook failure: " + error);
            return false;
        }
    }

    private static Object enumValue(Class<?> type, String expected) {
        Object[] values = type.getEnumConstants();
        if (values != null) {
            for (Object value : values) {
                if (((Enum<?>) value).name().equals(expected)) return value;
            }
        }
        throw new IllegalStateException("missing enum value " + expected);
    }

    private static void installReadinessCheck(ClassLoader loader) {
        try {
            Class<?> controller = XposedHelpers.findClass(CONTROLLER, loader);
            final Method recheck = controller.getDeclaredMethod("R", long.class, int.class);
            recheck.setAccessible(true);
            XposedHelpers.findAndHookConstructor(controller, Context.class, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    try {
                        final Object instance = p.thisObject;
                        Handler found = null;
                        for (Field field : instance.getClass().getDeclaredFields()) {
                            if (field.getType() == Handler.class) {
                                field.setAccessible(true);
                                found = (Handler) field.get(instance);
                                break;
                            }
                        }
                        if (found == null) throw new IllegalStateException("controller handler missing");
                        final Handler handler = found;
                        handler.postDelayed(new Runnable() {
                            private boolean wasListening;
                            private int retries;
                            @Override public void run() {
                                try {
                                    boolean listening = proxyListening();
                                    if (!listening) {
                                        retries = 0;
                                    } else if (!wasListening || (!probeSucceeded && retries < 3)) {
                                        if (!wasListening) retries = 0;
                                        retries++;
                                        // Use the original controller's check queue. boot=1 reapplies
                                        // the real result even if persisted policies disagree with memory.
                                        recheck.invoke(instance, 0L, 1);
                                        log("queued original controller check; proxy ready; retry=" + retries);
                                    }
                                    wasListening = listening;
                                } catch (Throwable error) {
                                    log("readiness check failed: " + error);
                                } finally {
                                    handler.postDelayed(this, 30000L);
                                }
                            }
                        }, 10000L);
                        log("controller captured; proxy readiness recheck enabled");
                    } catch (Throwable error) {
                        log("controller capture failed: " + error);
                    }
                }
            });
        } catch (Throwable error) {
            log("readiness hook unavailable; natural network checks remain: " + error);
        }
    }

    private static boolean proxyListening() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, PORT), 250);
            return true;
        } catch (Exception unavailable) {
            return false;
        }
    }

    private static List<String> addresses(Context context, ClassLoader loader) {
        ArrayList<String> urls = new ArrayList<>();
        try {
            // h6.a is the raw RUS helper class in this device's Battery.apk.
            Class<?> helperClass = XposedHelpers.findClass("h6.a", loader);
            Object helper = XposedHelpers.callStaticMethod(helperClass, "d", context);
            Object configured = XposedHelpers.callMethod(helper, "b", "google_address_list");
            if (configured instanceof List<?>) {
                for (Object value : new ArrayList<Object>((List<?>) configured)) {
                    if (value instanceof String) {
                        URL url = new URL((String) value);
                        if ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol()))
                            urls.add((String) value);
                    }
                }
            }
        } catch (Throwable error) {
            log("RUS address lookup failed; use extracted default URLs: " + error);
        }
        if (urls.isEmpty()) {
            urls.add("https://www.google.com/generate_204");
            urls.add("http://www.google.com/gen_204");
        }
        return urls;
    }

    private static boolean probe204(String address) {
        HttpURLConnection connection = null;
        try {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(HOST, PORT));
            connection = (HttpURLConnection) new URL(address).openConnection(proxy);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            int code = connection.getResponseCode();
            log("probe response=" + code);
            return code == 204;
        } catch (Exception error) {
            log("probe failed: " + error.getClass().getSimpleName());
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
