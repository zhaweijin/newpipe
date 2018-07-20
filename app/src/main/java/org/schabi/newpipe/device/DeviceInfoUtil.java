package org.schabi.newpipe.device;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;

import com.hiveview.manager.SystemInfoManager;

import org.schabi.newpipe.App;

import java.util.HashMap;
import java.util.Map;

/**
 * @author aibitao
 * @date 2015-12-29 下午6:08:48
 * @description
 */
public class DeviceInfoUtil {
    private static SystemInfoManager sm = SystemInfoManager.getSystemInfoManager();

    static {
        initDeviceInfo();
    }

    public static String getModel() {
        if (TextUtils.isEmpty(Model)) {
            try {
                Model = sm.getProductModel() == null ? "" : sm.getProductModel();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return Model;
    }

    public static String getMac() {
        if (TextUtils.isEmpty(Mac)) {
            try {
                Mac = sm.getMacInfo() != null ? sm.getMacInfo() : "";
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return Mac;
    }

    public static String getSN() {
        if (TextUtils.isEmpty(SN)) {
            try {
                SN = sm.getSnInfo() != null ? sm.getSnInfo() : "";
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return SN;
    }

    public static String getAndroidId(Context ctx) {
        String AndroidId = Secure.getString(ctx.getContentResolver(),
                Secure.ANDROID_ID);
        if (AndroidId == null) {
            AndroidId = "";
        }
        return AndroidId;
    }

    public static String getSoftwareVersion() {
        if (TextUtils.isEmpty(SoftwareVersion)) {
            try {
                SoftwareVersion = sm.getFirmwareVersion() == null ? "" : sm.getFirmwareVersion();//获取rom版本号
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return SoftwareVersion;
    }

    public static String getWlanMac() {
        if (TextUtils.isEmpty(WlanMac)) {
            try {
                WlanMac = sm.getWMacInfo() == null ? "" : sm.getWMacInfo();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return WlanMac;
    }

    public static String getDeviceVersion() {
        if (TextUtils.isEmpty(DeviceVersion)) {
            try {
                DeviceVersion = sm.getHWVersion() == null ? "" : sm.getHWVersion();//硬件版本；
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return DeviceVersion;
    }

    private static String Model = "";
    private static String Mac = "";
    private static String SN = "";
    private static String AndroidId = "";
    /**
     * rom版本
     */
    private static String SoftwareVersion = "";
    private static String WlanMac = "";
    /**
     * 硬件版本
     */
    private static String DeviceVersion = "";

    public static int getRomType() {
        if (romType < 0) {
            try {
                romType = sm.getSofewareInfo();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return romType;
    }

    /**
     * romType -1错误 0 国内盒子；1：国内电视；2：海外盒子版本；
     */
    private static int romType = -2;

    public static void initDeviceInfo() {
        try {
            if (null == sm) {
                sm = SystemInfoManager.getSystemInfoManager();
            }
            Model = sm.getProductModel() == null ? "" : sm.getProductModel();
            Mac = sm.getMacInfo() != null ? sm.getMacInfo() : "";
            SN = sm.getSnInfo() != null ? sm.getSnInfo() : "";
            SoftwareVersion = sm.getFirmwareVersion() == null ? "" : sm.getFirmwareVersion();//获取rom版本号
            WlanMac = sm.getWMacInfo() == null ? "" : sm.getWMacInfo();
            DeviceVersion = sm.getHWVersion() == null ? "" : sm.getHWVersion();//硬件版本；
            romType = sm.getSofewareInfo();
            Log.d("DeviceInfoUtil", "romType=" + romType);
        } catch (RemoteException e) {
            e.printStackTrace();
            Log.e("DeviceInfoUtil", "device info can not be initial. error:" + e.getMessage());
            Model = "";
            Mac = "";
            SN = "";
            AndroidId = "";
            SoftwareVersion = "";
            WlanMac = "";
            DeviceVersion = "";
            romType = -2;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("DeviceInfoUtil", "--device info can not be initial. error:" + e.getMessage());
        }
    }

    public static String getApkVersion(Context context) {
        PackageManager pm = context.getPackageManager();
        PackageInfo info = null;
        String appVersionName = "";
        try {
            info = pm.getPackageInfo(context.getPackageName(), 0);
            appVersionName = info.versionName; // 版本名
            int currentVersionCode = info.versionCode; // 版本号

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return appVersionName;
    }


    public static Map<String, String> getHeaders() {
        initDeviceInfo();
        String userArgentStr =
                "MODEL= " + Model +
                        "&&MAC= " + Mac +
                        "&&SN= " + SN +
                        "&&VERSION = " + SoftwareVersion +
                        "&&APPNAME = " + App.getInstance().getPackageName();
        Log.d("header", userArgentStr);
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("User-Agent", userArgentStr);
        return headers;
    }
}
