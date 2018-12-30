package com.dingyg.updateutil;

import android.app.Dialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class TestActivity extends AppCompatActivity {

    TestActivity mActivity;
    //内网更新
    private IntranetUpdateManager intUpdateManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.text);
        mActivity = TestActivity.this;
        //内网
//        intUpdateManager = new IntranetUpdateManager(this, "","", "");
//        new Thread(checkSelfUpdateInt).start();
    }
    Dialog dialog;
    //内网更新
    Runnable checkSelfUpdateInt = new Runnable() {
        @Override
        public void run() {
            // TODO 这里写上传逻辑
            try {
                int newVerCode = intUpdateManager.getServerVerCode();
                int oldVerCode = intUpdateManager.getLocalVerCode();
                // 如果版本不同则提示更
                if (newVerCode > oldVerCode) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (ActivityUtils.isRunning(mActivity)) {
                                if (System.currentTimeMillis() - App.lastUpdateTime >  1000) {
                                    App.lastUpdateTime = System.currentTimeMillis();
                                    dialog = CustomDialog.createUpdateDialog(TestActivity.this,
                                            new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    dialog.dismiss();
                                                    dialog.setCancelable(true);
                                                    intUpdateManager.showDownloadDialog();
                                                }
                                            });
                                    dialog.show();
                                }
                            }
                        }
                    });
                }
            } catch (Exception e) {
            }
        }
    };
}
