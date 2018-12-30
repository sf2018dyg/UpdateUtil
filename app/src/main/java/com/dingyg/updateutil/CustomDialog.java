package com.dingyg.updateutil;

import android.app.Activity;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * 作者：DC-DingYG on 2018-05-02 14:13
 * 邮箱：dingyg012655@126.com
 */
public class CustomDialog {

    /**
     * 注销dialog
     *
     * @param activity
     * @param title
     * @param listener
     * @return
     */
    public static Dialog createCancleDialog(final Activity activity, final String title, View.OnClickListener listener) {
        View view = LayoutInflater.from(activity).inflate(
                R.layout.view_cancle_dialog, null);
        final Dialog dialog = new Dialog(activity, R.style.dialog);
        dialog.setContentView(view);
        dialog.setCancelable(true);
        // 标题
        TextView tView = (TextView) view.findViewById(R.id.txt_title);
        tView.setText(title);
        Button sureButton = (Button) view.findViewById(R.id.btn_pos);
        Button cancelButton = (Button) view.findViewById(R.id.btn_neg);
        sureButton.setOnClickListener(listener);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                dialog.dismiss();
            }
        });
        return dialog;
    }
    /**
     * 版本更新提示
     *
     * @param activity
     * @param listener
     * @return
     * @author dignyg
     */
    public static Dialog createUpdateDialog(final Activity activity, View.OnClickListener listener) {

        View view = LayoutInflater.from(activity).inflate(
                R.layout.view_update, null);
        final Dialog dialog = new Dialog(activity, R.style.dialog);
        dialog.setContentView(view);
        dialog.setCancelable(false);
        Button okButton = (Button) view.findViewById(R.id.save);
        Button cancelButton = (Button) view.findViewById(R.id.cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                dialog.dismiss();
            }
        });
        if (listener != null) {
            okButton.setOnClickListener(listener);
        }
        return dialog;
    }

}
