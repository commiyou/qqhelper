package com.cooltol.qqhelper;

import android.app.AndroidAppHelper;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.FrameLayout;

import java.lang.reflect.Field;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class QQHelper implements IXposedHookLoadPackage {

    private static final String CLASS_NAME = "com.tencent.mobileqq.activity.BaseChatPie";
    public static Context mContext;
    public static FrameLayout mChatFooter;
    public static String mmUserName;
    private static final String TAG = "[qqhelper]";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Log.d(TAG, lpparam.packageName + " " + lpparam.processName);
        if (lpparam.packageName.equals("com.tencent.mobileqq")) {
            handleQQ(lpparam);
        } else if (lpparam.packageName.equals("com.tencent.mm")) {
            handleMM(lpparam);
        }

    }

    private void handleMM(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        HookUtil.hook("com.tencent.mm",
                "com.tencent.wcdb.database.SQLiteDatabase",
                "insertWithOnConflict",
                lpparam,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args.length != 4) return;
                        String tableName = (String) param.args[0];
                        String columnHack = (String) param.args[1];
                        ContentValues contentValues = (ContentValues) param.args[2];
                        int conflictValue = (Integer) param.args[3];
                        if (tableName == null || tableName != "message" || tableName.length() == 0 || contentValues == null) {
                            return;
                        }
                        int isSend = contentValues.getAsInteger("isSend");
                        String talker = contentValues.getAsString("talker");
                        int msgType = contentValues.getAsInteger("type");
                        String content = contentValues.getAsString("content");
                        if (msgType != 1) return; //must be text
                        if (isSend != 1) return; // must be sender
                        Intent intent = new Intent();
                        intent.setAction("com.cooltol.qqhelper.MM_MSG_SENT");

                        intent.putExtra("talker", talker);
                        intent.putExtra("content", content);
                        Context context = AndroidAppHelper.currentApplication().getApplicationContext();
                        context.sendBroadcast(intent);
                        HookUtil.log("send mm msg to " + talker);
                    }
                }
        );
    }

    private void handleQQ(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        final Class<?> text_msg_class = lpparam.classLoader.loadClass("com.tencent.mobileqq.data.MessageForText");
        final Class<?> msg_record_class = lpparam.classLoader.loadClass("com.tencent.mobileqq.data.MessageRecord");
        Class<?> base_chat_pie_class = XposedHelpers.findClassIfExists("com.tencent.mobileqq.activity.BaseChatPie", lpparam.classLoader);
        if (base_chat_pie_class == null) {
            base_chat_pie_class = XposedHelpers.findClassIfExists("com.tencent.mobileqq.activity.aio.core.BaseChatPie", lpparam.classLoader);
        }
        if (base_chat_pie_class == null) {
            XposedBridge.log("Incompatible version of mobileqq, QQHlepr won't work!");
            return;
        }

        findAndHookMethod(base_chat_pie_class.getName(), lpparam.classLoader,
                "update", "java.util.Observable", "java.lang.Object",
                new XC_MethodHook() {
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

                        if (!senderuin.equals(selfuin)) {
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

                        Context context = AndroidAppHelper.currentApplication().getApplicationContext();
                        context.sendBroadcast(intent);
                        HookUtil.log("send qq msg to " + frienduin);

                    }
                });
    }
}