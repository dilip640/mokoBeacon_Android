package com.moko.beacon.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.moko.beacon.entity.BeaconInfo;
import com.moko.support.entity.DeviceInfo;

import java.text.DecimalFormat;

/**
 * @Date 2017/12/7 0007
 * @Author wenzheng.liu
 * @Description
 * @ClassPath com.moko.beacon.utils.Utils
 */
public class Utils {
    /**
     * A - 发射端和接收端相隔1米时的信号强度
     */
    private static final double n_Value = 2.0;/** n - 环境衰减因子*/


    /**
     * @Date 2017/12/11 0011
     * @Author wenzheng.liu
     * @Description 根据Rssi获得返回的距离, 返回数据单位为m
     */
    public static double getDistance(int rssi, int acc) {
        int iRssi = Math.abs(rssi);
        double power = (iRssi - acc) / (10 * n_Value);
        return Math.pow(10, power);
    }

    public static String getVersionInfo(Context context) {
        // 获取packagemanager的实例
        PackageManager packageManager = context.getPackageManager();
        // getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = null;
        try {
            packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packInfo != null) {
            String version = packInfo.versionName;
            return String.format("%s", version);
        }
        return "";
    }
}
