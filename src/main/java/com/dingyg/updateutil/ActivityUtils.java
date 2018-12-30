package com.dingyg.updateutil;

import android.app.Activity;

/**
 * 作者：DC-DingYG on 2018-04-27 9:03
 * 邮箱：dingyg012655@126.com
 */
public class ActivityUtils {
    //检测Activity在运行
    public static boolean isRunning(Activity activity){
        if(activity==null||activity.isDestroyed()||activity.isFinishing()){
            return false;
        }else{
            return true;
        }
    }
}
