package com.termux.x11;

import static android.os.Build.VERSION.SDK_INT;
import static android.system.Os.getuid;
import static android.system.Os.getenv;

import android.annotation.SuppressLint;
import android.app.IActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Keep;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;

@Keep @SuppressLint({"StaticFieldLeak", "UnsafeDynamicallyLoadedCode"})
public class CmdEntryPoint extends ICmdEntryInterface.Stub {
    public static final String ACTION_START = "com.termux.x11.CmdEntryPoint.ACTION_START";
    static final Handler handler;
    public static Context ctx;
    private final Intent intent = createIntent();

    /**
     * Command-line entry point.
     *
     * @param args The command-line arguments
     */
    public static void main(String[] args) {
        android.util.Log.i("CmdEntryPoint", "commit " + BuildConfig.COMMIT);
		// ZeroTermux add {@
        Log.d("SurfaceChangedListener", " main...... ");
		// @}
        handler.post(() -> new CmdEntryPoint(args));
        Looper.loop();
    }
	// ZeroTermux add {@
    public static void mainZeroTermux(String[] args) {
        android.util.Log.i("CmdEntryPoint", "commit " + BuildConfig.COMMIT);
        Log.d("SurfaceChangedListener", " main...... ");
        handler.post(() -> new CmdEntryPoint(args));
        Looper.loop();
    }
	// @}
    CmdEntryPoint(String[] args) {
        if (!start(args)) {
			// ZeroTermux add {@
            Log.i("CmdEn", "start is false");
			// @}
            System.exit(1);
        }
			// ZeroTermux add {@
        Log.i("CmdEn", "start landing......");
        Log.d("SurfaceChangedListener", " start landing...... ");
		// @}
        spawnListeningThread();
        sendBroadcastDelayed();
    }

    @SuppressLint({"WrongConstant", "PrivateApi"})
    private Intent createIntent() {
        String targetPackage = getenv("TERMUX_X11_OVERRIDE_PACKAGE");
		// ZeroTermux del {@
        //if (targetPackage == null)
		// @}
            targetPackage = "com.termux";
        // We should not care about multiple instances, it should be called only by `Termux:X11` app
        // which is single instance...

        Bundle bundle = new Bundle();
        bundle.putBinder(null, this);

        Intent intent = new Intent(ACTION_START);
        intent.putExtra(null, bundle);
        intent.setPackage(targetPackage);
		// ZeroTermux add {@
        Log.d("SurfaceChangedListener", " send bundle :" + bundle);
		// @}
        if (getuid() == 0 || getuid() == 2000)
            intent.setFlags(0x00400000 /* FLAG_RECEIVER_FROM_SHELL */);

        return intent;
    }

    private void sendBroadcast() {
        sendBroadcast(intent);
    }

    static void sendBroadcast(Intent intent) {
        try {
            ctx.sendBroadcast(intent);
        } catch (Exception e) {
            if (e instanceof NullPointerException && ctx == null)
                Log.i("Broadcast", "Context is null, falling back to manual broadcasting");
            else
                Log.e("Broadcast", "Falling back to manual broadcasting, failed to broadcast intent through Context:", e);

            // ZeroTermux modify {@
            String packageName;
            try {
                android.content.pm.IPackageManager packageManager = android.app.ActivityThread.getPackageManager();
                if (packageManager != null) {
                    String[] packages = packageManager.getPackagesForUid(getuid());
                    if (packages != null && packages.length > 0 && packages[0] != null)
                        packageName = packages[0];
                    else
                        packageName = intent.getPackage();
                } else {
                    packageName = intent.getPackage();
                }
                if (packageName == null)
                    packageName = "com.termux";
            } catch (RemoteException ex) {
                Log.e("Broadcast", "Failed to resolve package for manual broadcast, using target package", ex);
                packageName = intent.getPackage() != null ? intent.getPackage() : "com.termux";
            }
            Object am;
            try {
                //noinspection JavaReflectionMemberAccess
                am = android.app.ActivityManager.class
                        .getMethod("getService")
                        .invoke(null);
            } catch (Exception e2) {
                try {
                    am = Class.forName("android.app.ActivityManagerNative")
                            .getMethod("getDefault")
                            .invoke(null);
                } catch (Exception e3) {
                    Log.e("Broadcast", "Failed to resolve activity manager for manual broadcast", e3);
                    return;
                }
            }

            if (am == null) {
                Log.w("Broadcast", "Activity manager is null, will retry broadcast later");
                return;
            }
            Object sender;
            try {
                Class<?> activityManagerInterface = Class.forName("android.app.IActivityManager");
                sender = null;
                for (Method method : activityManagerInterface.getMethods()) {
                    if (!"getIntentSenderWithFeature".equals(method.getName()) && !"getIntentSender".equals(method.getName()))
                        continue;

                    Object[] args = createGetIntentSenderArgs(method.getParameterTypes(), packageName, intent);
                    if (args == null)
                        continue;

                    sender = method.invoke(am, args);
                    if (sender != null)
                        break;
                }
            } catch (Exception ex) {
                Log.e("Broadcast", "Failed to create intent sender for manual broadcast", ex);
                return;
            }
            if (sender == null) {
                Log.w("Broadcast", "Intent sender is null, will retry broadcast later");
                return;
            }
            try {
                //noinspection JavaReflectionMemberAccess
                Object finishedReceiver = new IIntentReceiver.Stub() {
                    @Override public void performReceive(Intent i, int r, String d, Bundle e, boolean o, boolean s, int a) {}
                };
                try {
                    IIntentSender.class
                            .getMethod("send", int.class, Intent.class, String.class, IBinder.class, IIntentReceiver.class, String.class, Bundle.class)
                            .invoke(sender, 0, intent, null, null, finishedReceiver, null, null);
                } catch (IllegalArgumentException ex) {
                    sender.getClass()
                            .getMethod("send", int.class, Intent.class, String.class, IBinder.class, IIntentReceiver.class, String.class, Bundle.class)
                            .invoke(sender, 0, intent, null, null, finishedReceiver, null, null);
                }
            } catch (Exception ex) {
                Log.e("Broadcast", "Manual broadcast failed, will retry later", ex);
            }

            //---------------------
           /* String packageName;
            try {
                packageName = android.app.ActivityThread.getPackageManager().getPackagesForUid(getuid())[0];
            } catch (RemoteException ex) {
                throw new RuntimeException(ex);
            }
            IActivityManager am;
            try {
                //noinspection JavaReflectionMemberAccess
                am = (IActivityManager) android.app.ActivityManager.class
                        .getMethod("getService")
                        .invoke(null);
            } catch (Exception e2) {
                try {
                    am = (IActivityManager) Class.forName("android.app.ActivityManagerNative")
                            .getMethod("getDefault")
                            .invoke(null);
                } catch (Exception e3) {
                    throw new RuntimeException(e3);
                }
            }

            assert am != null;
            IIntentSender sender = am.getIntentSender(1, packageName, null, null, 0, new Intent[] { intent },
                    null, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT, null, 0);
            try {
                //noinspection JavaReflectionMemberAccess
                IIntentSender.class
                        .getMethod("send", int.class, Intent.class, String.class, IBinder.class, IIntentReceiver.class, String.class, Bundle.class)
                        .invoke(sender, 0, intent, null, null, new IIntentReceiver.Stub() {
                            @Override public void performReceive(Intent i, int r, String d, Bundle e, boolean o, boolean s, int a) {}
                        }, null, null);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }*/
            // @}
        }
    }

    // In some cases Android Activity part can not connect opened port.
    // In this case opened port works like a lock file.
    private void sendBroadcastDelayed() {
		// ZeroTermux add {@
        Log.d("SurfaceChangedListener", " send intent111 : " + intent);
		// @}
        if (!connected()) {
			// ZeroTermux add {@
            Log.d("SurfaceChangedListener", " send intent : " + intent);
			// @}
            sendBroadcast(intent);
        }


        handler.postDelayed(this::sendBroadcastDelayed, 1000);
    }

    void spawnListeningThread() {
        new Thread(this::listenForConnections).start();
    }

    /** @noinspection DataFlowIssue*/
    @SuppressLint("DiscouragedPrivateApi")
    public static Context createContext() {
        Context context;
        PrintStream err = System.err;
        try {
            java.lang.reflect.Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object unsafe = f.get(null);
            // Hiding harmless framework errors, like this:
            // java.io.FileNotFoundException: /data/system/theme_config/theme_compatibility.xml: open failed: ENOENT (No such file or directory)
            System.setErr(new PrintStream(new OutputStream() { public void write(int arg0) {} }));
            if (System.getenv("OLD_CONTEXT") != null) {
                context = android.app.ActivityThread.systemMain().getSystemContext();
            } else {
                context = ((android.app.ActivityThread) Class.
                        forName("sun.misc.Unsafe").
                        getMethod("allocateInstance", Class.class).
                        invoke(unsafe, android.app.ActivityThread.class))
                        .getSystemContext();
            }
        } catch (Exception e) {
            Log.e("Context", "Failed to instantiate context:", e);
            context = null;
        } finally {
            System.setErr(err);
        }
        return context;
    }

    public static native boolean start(String[] args);
    public native ParcelFileDescriptor getXConnection();
    public native ParcelFileDescriptor getLogcatOutput();
    private static native boolean connected();
    private native void listenForConnections();

    static {
        try {
            if (Looper.getMainLooper() == null)
                Looper.prepareMainLooper();
        } catch (Exception e) {
            Log.e("CmdEntryPoint", "Something went wrong when preparing MainLooper", e);
        }
        handler = new Handler();
        ctx = createContext();

        String path = "lib/" + Build.SUPPORTED_ABIS[0] + "/libXlorie.so";
        ClassLoader loader = CmdEntryPoint.class.getClassLoader();
        URL res = loader != null ? loader.getResource(path) : null;
        String libPath = res != null ? res.getFile().replace("file:", "") : null;
        if (libPath != null) {
            try {
				// ZeroTermux add {@
                System.load("/data/data/com.termux/files/usr/lib/libXlorie.so");
                //System.loadLibrary("Xlorie");
				// @}
            } catch (Exception e) {
                Log.e("CmdEntryPoint", "Failed to dlopen " + libPath, e);
                System.err.println("Failed to load native library. Did you install the right apk? Try the universal one.");
                System.exit(134);
            }
        } else {
            // It is critical only when it is not running in Android application process
            if (MainActivity.getInstance() == null) {
                System.err.println("Failed to acquire native library. Did you install the right apk? Try the universal one.");
                System.exit(134);
            }
        }
    }

    // ZeroTermux add {@
    private static Object[] createGetIntentSenderArgs(Class<?>[] parameterTypes, String packageName, Intent intent) {
        Object[] args = new Object[parameterTypes.length];
        int intIndex = 0;
        int stringIndex = 0;

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> type = parameterTypes[i];
            if (type == int.class) {
                if (intIndex == 0)
                    args[i] = 1;
                else if (intIndex == 2)
                    args[i] = PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT;
                else
                    args[i] = 0;
                intIndex++;
            } else if (type == String.class) {
                args[i] = stringIndex == 0 ? packageName : null;
                stringIndex++;
            } else if (type == IBinder.class) {
                args[i] = null;
            } else if (type == Intent[].class) {
                args[i] = new Intent[] { intent };
            } else if (type == String[].class) {
                args[i] = null;
            } else if (type == Bundle.class) {
                args[i] = null;
            } else {
                return null;
            }
        }

        return intIndex >= 4 ? args : null;
    }
    // @}
}
