package com.cooltol.qqhelper;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookUtil {
    /**
     * 对于加固的 App 找到对应的 Application并 attach
     *
     * @param
     */
    public static final String TAG = "[HookUtil]";

    public static void log(String msg) {
        Log.d(TAG, msg);
        XposedBridge.log(TAG + " "+ msg);
    }

    public static boolean hook(final String pkg, final String cls, final String mthd,
                               final XC_LoadPackage.LoadPackageParam lpparam
    ) {
        return hook(pkg, cls, mthd, null);
    }

    public static boolean hook(final String pkg, final String cls, final String mthd,
                               final XC_LoadPackage.LoadPackageParam lpparam,
                               final XC_MethodHook callback
    ) {
        if (!lpparam.packageName.equals(pkg)) {
            log("not find pkg " + pkg);
            return false;
        }

        List<String> applicationList = Arrays.asList(
                "com.stub.StubApp",
                "com.wrapper.proxyapplication.WrapperProxyApplication"
        );
        String attachMethod = "attachBaseContext";

        Class<?> applicationClass = null;
        for (String appClassString : applicationList) {
            applicationClass = XposedHelpers.findClassIfExists(appClassString, lpparam.classLoader);
            if (applicationClass != null) {
                log("Found reinforcement application: " + appClassString);
                break;
            }
        }

        if (applicationClass == null) {
            applicationClass = Application.class;
            attachMethod = "attach";
            log("Not Found reinforcement application, Use " + applicationClass);
        }

        XposedHelpers.findAndHookMethod(applicationClass, attachMethod, Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        log("attach application " + param.thisObject.getClass());
                        Context context = (Context) param.args[0];
                        ClassLoader loader = context.getClassLoader();
                        startHook(cls, mthd, loader, callback);
                    }
                }
        );
        return true;

    }

    private static void startHook(final String className,
                                  final String methodName,
                                  final ClassLoader loader,
                                  final XC_MethodHook callback) {
        Class<?> clazz = null;
        try {
            clazz = loader.loadClass(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            log("hook load class " + className + " failed");
            return;
        }


        Method[] methods = clazz.getDeclaredMethods();
        if (methods.length > 0) {
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    hook(method, callback);
                }
            }
        }

        Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length > 0) {
            for (Constructor<?> c : constructors) {
                hook(c, callback);
            }
        }


    }

    private static void hook(final Member method, final XC_MethodHook callback) {
        log("method : " + method.toString());
        log("call back: " + callback.toString());

        XposedBridge.hookMethod(method, callback != null ? callback : new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                log("before hooked " + method.toString());
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                for (int i = 0; i < param.args.length; i++) {
                    log("params[" + i + "]:" + param.args[i].toString());
                }
                try {
                    log("result：" + param.getResult());
                } catch (Exception e) {
                    log("Easy hook exception:" + e);
                }
            }
        });
    }

}
