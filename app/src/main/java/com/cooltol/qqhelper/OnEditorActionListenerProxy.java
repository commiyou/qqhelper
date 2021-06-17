package com.cooltol.qqhelper;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.widget.TextView;

import de.robv.android.xposed.XposedBridge;

public class OnEditorActionListenerProxy implements TextView.OnEditorActionListener {
    private final TextView.OnEditorActionListener mOriginalListener;
    private final String mPkg;

    public OnEditorActionListenerProxy(TextView.OnEditorActionListener listener, String pkg) {
        mOriginalListener = listener;
        mPkg = pkg;
    }

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {

        if (mOriginalListener != null) {
            String msg = textView.getText().toString();
            if (msg.length() > 0) {
                XposedBridge.log("[qq helper] hooked listener for " + QQHelper.mmUserName + " " + msg);
                Intent intent = new Intent();
                intent.setAction("com.cooltol.qqhelper.MM_MSG_SENT");
                intent.putExtra("msg", msg);
                intent.putExtra("talker", QQHelper.mmUserName);


                Context context = AndroidAppHelper.currentApplication().getApplicationContext();
                context.sendBroadcast(intent);
            }

            mOriginalListener.onEditorAction(textView, i, keyEvent);
        }
        return false;
    }
}
