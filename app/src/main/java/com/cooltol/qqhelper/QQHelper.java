package com.cooltol.qqhelper;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.Intent;

import java.lang.reflect.Field;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XC_MethodHook;

public class QQHelper implements IXposedHookLoadPackage {

    private static final String CLASS_NAME = "com.tencent.mobileqq.activity.BaseChatPie";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!lpparam.packageName.equals("com.tencent.mobileqq"))
            return;
        final Class<?> text_msg_class = lpparam.classLoader.loadClass("com.tencent.mobileqq.data.MessageForText");
        final Class<?> msg_record_class = lpparam.classLoader.loadClass("com.tencent.mobileqq.data.MessageRecord");

        findAndHookMethod(CLASS_NAME, lpparam.classLoader, "update", "java.util.Observable", "java.lang.Object", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                // this will be called before the clock was updated by the original method
                Object obj = param.args[1];
                if (!text_msg_class.isInstance(obj)) return;
                String msg_str = (String) msg_record_class.getMethod("toString").invoke(obj);
                //XposedBridge.log("msg str " + msg_str);

                Field field = msg_record_class.getDeclaredField("istroop");
                field.setAccessible(true);
                int istroop = (int) field.get(obj);
                if (istroop != 0) return;   // qun

                field = msg_record_class.getDeclaredField("issend");
                field.setAccessible(true);
                int is_local_send = (int) field.get(obj);
                if (is_local_send != 1) return;  // send from local

                Intent intent = new Intent();
                intent.setAction("com.cooltol.qqhelper.QQ_MSG_SENT");

                field = msg_record_class.getDeclaredField("time");
                field.setAccessible(true);
                long send_time = (long) field.get(obj);
                intent.putExtra("time", send_time);

                field = msg_record_class.getDeclaredField("selfuin");
                field.setAccessible(true);
                String selfuin = (String) field.get(obj);
                intent.putExtra("selfuin", selfuin);

                field = msg_record_class.getDeclaredField("senderuin");
                field.setAccessible(true);
                String senderuin = (String) field.get(obj);
                intent.putExtra("senderuin", senderuin);

                if (senderuin != selfuin) {
                    return;
                }


                field = msg_record_class.getDeclaredField("frienduin");
                field.setAccessible(true);
                String frienduin = (String) field.get(obj);
                intent.putExtra("frienduin", frienduin);


                field = msg_record_class.getDeclaredField("msg");
                field.setAccessible(true);
                String msg = (String) field.get(obj);
                intent.putExtra("msg", msg);

                Context context = (Context) AndroidAppHelper.currentApplication().getApplicationContext();
                context.sendBroadcast(intent);
                //XposedBridge.log("send broadcast");

            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                // this will be called after the clock was updated by the original method
            }
        });




    }
}