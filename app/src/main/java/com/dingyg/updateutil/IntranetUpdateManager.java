package com.dingyg.updateutil;

/**
 * Created by DingYG on 2017-10-11.
 * 内网更新
 */

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;

import com.github.dfqin.grantor.PermissionListener;
import com.github.dfqin.grantor.PermissionsUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class IntranetUpdateManager {
    private Activity mContext;
    private String ServiceAddress = "";
    private String apkName = "";
    private Dialog downloadDialog; // 下载进度对话
    private ProgressBar mProgressBar; // 进度
    private Boolean interceptFlag = false; // 标记用户是否在下载过程中取消下载
    private Thread downloadApkThread = null; // 下载线程

    private String apkUrl;
    private String savePath; // 下载的apk存放的路
    private String saveFileName; // 下载的apt文件
    private int progress = 0; // 下载进度
    private final int DOWNLOAD_ING = 1; // 标记正在下载
    private final int DOWNLOAD_OVER = 2; // 标记下载完成

    private Dialog cusDialog;
    @SuppressLint("HandlerLeak")
    private Handler mhandler = new Handler() { // 更新UI的handler

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case DOWNLOAD_ING:
                    // 更新进度
                    mProgressBar.setProgress(progress);
                    break;
                case DOWNLOAD_OVER:
                    downloadDialog.dismiss();
                    installApk();
                    // 安装
                    break;
                default:
                    break;
            }
        }
    };

    public IntranetUpdateManager(Activity context,String ServiceAddress, String apkname) {
        if(apkname==null || apkname.equals("")){
            return;
        }
        this.apkName = apkname + ".apk";
        this.mContext = context;
        this.ServiceAddress = ServiceAddress.toLowerCase();
        if (!ServiceAddress.contains("http")) {
            ServiceAddress = "http://" + ServiceAddress;
        }
        try {
            apkUrl = ServiceAddress + "/update/"+ apkName;
            savePath = "/sdcard/SFAPK/"; // 下载的apk存放的路
            saveFileName = savePath + apkName; // 下载的apt文件
        } catch (Exception e) {
            return;
        }
    }

    /**
     * 获取本地程序版本
     *
     * @return
     */
    public int getLocalVerCode() {
        int oldVerCode = 0;
        PackageManager manager = mContext.getPackageManager();

        try {
            PackageInfo info = manager.getPackageInfo(
                    mContext.getPackageName(), 0);
            oldVerCode = info.versionCode;
        } catch (Exception e) {
            oldVerCode = -1;
        }

        return oldVerCode;
    }

    /**
     * 获取本地程序版本用于显示在设置页面
     *
     * @return
     */
    public String getLocalVerName() {
        String oldVerCode = "";
        PackageManager manager = mContext.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(
                    mContext.getPackageName(), 0);
            oldVerCode = info.versionName;
        } catch (Exception e) {
            oldVerCode = "-1";
        }

        return oldVerCode;
    }

    /**
     * 获取服务版本
     *
     * @return
     */
    public int getServerVerCode() {
        int newVerCode = 0;
        try {
            this.ServiceAddress = ServiceAddress.toLowerCase();
            if (!ServiceAddress.contains("http")) {
                ServiceAddress = "http://" + ServiceAddress;
            }
            String url = ServiceAddress + "/update/version.txt";
            String verjson = getUrlContent(url);
            JSONArray array = new JSONArray(verjson);
            if (array.length() > 0) {
                JSONObject obj = array.getJSONObject(0);
                newVerCode = Integer.parseInt(obj.getString("vercode"));
            }
        } catch (Exception e) {
            newVerCode = -1;
        }
        return newVerCode;
    }

    /*
     * 弹出下载进度对话
     */
    public void showDownloadDialog() {
        PermissionsUtil.requestPermission(mContext, new PermissionListener() {
                    @Override
                    public void permissionGranted(@NonNull String[] permissions) {
                        Builder builder = new Builder(mContext);
                        builder.setTitle("软件更新");

                        final LayoutInflater inflater = LayoutInflater.from(mContext);
                        View v = inflater.inflate(R.layout.progress, null);
                        mProgressBar = (ProgressBar) v.findViewById(R.id.updateProgress);
                        builder.setView(v);
                        builder.setCancelable(false);
                        builder.setNegativeButton("取消", new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                downloadDialog.dismiss();
                                interceptFlag = true;
                            }
                        });
                        downloadDialog = builder.create();

                        downloadDialog.show();

                        downloadLatestVersionApk();
                    }

                    @Override
                    public void permissionDenied(@NonNull String[] permissions) {
                    }
                },
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    /*
     * 下载新的apk文件
     */
    private void downloadLatestVersionApk() {
        downloadApkThread = new Thread(downloadApkRunnable);
        downloadApkThread.start();
    }

    // apk文件下载线程
    private Runnable downloadApkRunnable = new Runnable() {

        @Override
        public void run() {
            try {
                URL url = new URL(apkUrl);
                HttpURLConnection conn = (HttpURLConnection) url
                        .openConnection();
                conn.connect();
                int length = conn.getContentLength();

                InputStream is = conn.getInputStream();
                File file = new File(savePath);
                if (!file.exists()) {
                    file.mkdir();
                }
                File apkFile = new File(saveFileName);
                FileOutputStream out = new FileOutputStream(apkFile);
                int count = 0;
                int readnum = 0;
                byte[] buffer = new byte[1024 * 5];
                do {
                    readnum = is.read(buffer);
                    count += readnum;
                    progress = (int) (((float) count / length) * 100);

                    mhandler.sendEmptyMessage(DOWNLOAD_ING);
                    if (readnum <= 0) {

                        // 下载结束
                        mhandler.sendEmptyMessage(DOWNLOAD_OVER);
                        break;
                    }
                    out.write(buffer, 0, readnum);
                } while (!interceptFlag);

                is.close();
                out.close();
            } catch (MalformedURLException e) {
                downloadDialog.dismiss();
                Looper.prepare();  // 此处获取到当前线程的Looper，并且prepare()
                showHint("请求服务器apk文件错误，请联系管理员！");
                Looper.loop();
            } catch (IOException e) {
                downloadDialog.dismiss();
                if (e.toString().contains("FileNotFoundException")) {
                    Looper.prepare();  // 此处获取到当前线程的Looper，并且prepare()
                    showHint("未检测到服务器中存在\n【"+ apkName + "】\n文件，请联系管理员！");
                    Looper.loop();
                } else {
                    Looper.prepare();  // 此处获取到当前线程的Looper，并且prepare()
                    showHint("请求服务器apk文件错误，请联系管理员！");
                    Looper.loop();
                }
            }
        }
    };

    private void showHint(final String hintText) {
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (ActivityUtils.isRunning(mContext)) {
                    cusDialog = CustomDialog.createCancleDialog(mContext,
                            hintText, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    /**返回**/
                                    cusDialog.dismiss();
                                }
                            });
                    cusDialog.show();
                }
            }
        };
        handler.sendEmptyMessage(1);
    }

    /*
     * 安装下载的apk文件
     */
    private void installApk() {
        final File file = new File(saveFileName);
        if (!file.exists()) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // 7.0+以上版本
                Uri apkUri = FileProvider.getUriForFile(mContext, "com.dingyg.updateutil.myfileprovider", file);  //包名.fileprovider
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            } else {
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            }
            mContext.startActivity(intent);
        } catch (Exception e) {
            if (ActivityUtils.isRunning(mContext)) {
                cusDialog = CustomDialog.createCancleDialog(mContext,
                        "因未知原因导致暂不能自动安装，请到【SFAPK】文件夹下点击" + apkName + "进行手动安装！", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                /**返回**/
                                cusDialog.dismiss();
                            }
                        });
                cusDialog.show();
            }
        }
    }

    /**
     * 获取指定网页内容
     *
     * @param url
     * @return
     */
    public static String getUrlContent(String url) {
        String sResult = "";
        try {
            URL myurl = new URL(url);
            // 获得连接
            HttpURLConnection conn = (HttpURLConnection) myurl.openConnection();
            conn.setConnectTimeout(6000);// 设置超时
            conn.setDoInput(true);
            conn.setUseCaches(false);// 不缓存
            conn.connect();
            InputStream is = conn.getInputStream();// 获得的数据流
            BufferedReader br = new BufferedReader(new InputStreamReader(is));// 获得输入流的包装�?
            String str = null;//
            StringBuffer sb = new StringBuffer();// 字符串数据的拼接
            while ((str = br.readLine()) != null) {// 做判断是不是读完�?
                sb.append(str);// 若没读完，则拼接
            }
            is.close();
            br.close();
            conn.disconnect();
            sResult = sb.toString();
        } catch (Exception e) {
            sResult = "";
        }
        return sResult;
    }
}

