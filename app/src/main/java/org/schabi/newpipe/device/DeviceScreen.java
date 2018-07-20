package org.schabi.newpipe.device;

import android.content.Context;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;

import com.hiveview.manager.SystemInfoManager;

public class DeviceScreen {

    private SystemInfoManager systemInfoManager = null;

    protected String wlanMacNode = "sys/class/net/wlan0/address";

    public String getSN() {
        if (this.systemInfoManager == null) {
            this.systemInfoManager = SystemInfoManager.getSystemInfoManager();
        }
        try {
            String sn = this.systemInfoManager.getSnInfo();
            return sn;
        } catch (RemoteException e) {
            // e.printStackTrace();
            Log.d("TestLog","getSn_exception");
        }
        return "";
    }

    public String getMac() {
        if (this.systemInfoManager == null) {
            this.systemInfoManager = SystemInfoManager.getSystemInfoManager();
        }
        try {
            String mac = this.systemInfoManager.getMacInfo();
            return mac;
        } catch (RemoteException e) {
            // e.printStackTrace();
            Log.d("TestLog","getMac_exception");
        }
        return "";
    }

    public String getWlanMac() {
        if (this.systemInfoManager == null) {
            this.systemInfoManager = SystemInfoManager.getSystemInfoManager();
        }
        try {
            String wlanmac = this.systemInfoManager.getWMacInfo();
            return wlanmac;
        } catch (RemoteException e) {
            // e.printStackTrace();
        }
        return "";
    }

    public String getHardwareVersion() {
        if (this.systemInfoManager == null) {
            this.systemInfoManager = SystemInfoManager.getSystemInfoManager();
        }
        try {
            String hardware = this.systemInfoManager.getHWVersion();
            return hardware;
        } catch (RemoteException e) {
            // e.printStackTrace();
            Log.d("TestLog","getHardwareVersion_exception");
        }
        return "";
    }

    public String getSoftwareVersion() {
        if (this.systemInfoManager == null) {
            this.systemInfoManager = SystemInfoManager.getSystemInfoManager();
        }
        try {
            String softwareVersion = this.systemInfoManager.getFirmwareVersion();
            return softwareVersion;
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
             e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取电视的model
     * @Title: DeviceFactory
     * @author:郭松胜
     * @Description: TODO
     * @return
     */
    public String getModel() {
        try {
            if (this.systemInfoManager == null) {
                this.systemInfoManager = SystemInfoManager.getSystemInfoManager();
            }
            String model = this.systemInfoManager.getProductModel();
            return model;
        } catch (RemoteException e) {
            Log.d("TestLog","getModel_exception");
        }
        return "";
    }

//    public String getAndroidId(Context context) {
//        if (this.systemInfoManager == null) {
//            this.systemInfoManager = SystemInfoManager.getSystemInfoManager();
//        }
//        try {
//            String androidId = this.systemInfoManager.getAndroidVersion();
//            return androidId;
//        } catch (RemoteException e) {
//            // TODO Auto-generated catch block
//            // e.printStackTrace();
//        }
//        return "";
//    }
    
    /**
     * 获取设备ID
     */
    public String getAndroidId(Context context) {
    	 String AndroidId = Settings.Secure.getString(context.getContentResolver(),
                 Settings.Secure.ANDROID_ID);
         if (AndroidId == null) {
             AndroidId = "";
         }
         return AndroidId;

    }
}
