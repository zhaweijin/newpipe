package org.schabi.newpipe.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.regex.Pattern;

/**
 * package_name:org.schabi.newpipe.util
 * author: 李文烙
 * date: 2018/7/5
 * company: hiveview
 * annotation
 */

public class Utils {
    private final static String tag = "Utils";

    /**
     * 打印测试
     *
     * @param tag
     * @param value
     */
    public static void print(String tag, String value) {
        Log.v(tag, value);
    }

    /**
     * 获取字符长度,中文占2个，字母占2个
     *
     * @param str
     */
    public static int getStringLength(String str) {
        Utils.print(tag, "length=" + str.length());
        int length = 0;
        for (int i = 0; i < str.length(); i++) {
            String bb = str.substring(i, i + 1);
            // 生成一个Pattern,同时编译一个正则表达式,其中的u4E00("一"的unicode编码)-\u9FA5("龥"的unicode编码)
            boolean cc = Pattern.matches("[\u4E00-\u9FA5]", bb);
            if (!cc) {
                Log.v(tag, "en"); //字母
                length = length + 1;
            } else {
                Log.v(tag, "zh");//中文
                length = length + 2;
            }
        }
        return length;

    }

    /**
     * 判断包是否存在
     *
     * @param context
     * @param packageName
     * @return
     */
    public static boolean isPkgInstalled(Context context, String packageName) {
        if (packageName == null || "".equals(packageName))
            return false;
        android.content.pm.ApplicationInfo info = null;
        try {
            info = context.getPackageManager().getApplicationInfo(packageName, 0);
            return info != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;

        }
    }
}
