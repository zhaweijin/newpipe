/**
 * @Title UserInfoUtil.java
 * @Package com.hiveview.user.utils
 * @author 郭松胜
 * @date 2014-6-30 下午5:20:56
 * @Description TODO
 * @version V1.0
 */
package org.schabi.newpipe.device;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.hiveview.manager.SystemInfoManager;


/**
 * @ClassName: UserInfoUtil
 * @Description: TODO // 获取设备版本 version =
 *               cursor.getInt(cursor.getColumnIndex("versionCode"));
 * @author: guosongsheng
 * @date 2014-6-30 下午5:20:56
 */
public class DeviceInfoManager {

	private static final String TAG = "DeviceInfoManager";

	private static DeviceInfoManager sManager;
	/**
	 * 设备名称
	 */
	private String deviceName = "";
	/**
	 * 硬件版本
	 */
	private String hardwareVersion = "";
	/**
	 * 软件版本
	 */
	private String softwareVersion = "";
	/**
	 * 无线mac地址
	 */
	private String wlanMac = "";
	/**
	 * @Fields mac:TODO mac地址
	 */
	private String mac = "";
	/**
	 * @Fields sn:TODO 产品序列号
	 */
	private String sn = "";
	/**
	 * @Fields romVersion:TODO rom版本
	 */
	private final String romVersion = "";
	/**
	 * @Fields model:TODO 设备类型
	 */
	private String model = "";
	/**
	 * 设备ID
	 */
	private String androidId = "";
	/**
	 * 设备版本
	 */
	private String versionCode = "";
	/**
	 * 设备码
	 */
	private String deviceCode = "";
	/**
	 * Launcher版本名称
	 */
	private String launcherVersionName = "";
	/**
	 * 区分国内外盒子
	 */
	private int sofewareInfo = 0;

	/* 设备的终端信息 [1:盒子 2:电视] */
	private DeviceType deviceType = DeviceType.DOMY_BOX;
	/**
	 * @Fields URI_DEVICES_INFO:TODO 获取设备信息URI
	 */
	private static final Uri URI_DEVICES_INFO = Uri
			.parse("content://HiveViewAuthoritiesDataProvider/TABLE_DEVICE_INFO");

	private DeviceInfoManager() {

	}

	public synchronized static DeviceInfoManager getInstance() {
		if (sManager == null) {
			sManager = new DeviceInfoManager();
		}
		return sManager;
	}

	/**
	 * 初始化设备信息
	 * 
	 * @Title: UserInfoUtil
	 * @author:guosongsheng
	 * @Description: TODO
	 * @param context
	 */
	public void initDevices(Context context) throws Exception {
		if (context == null) {
			throw new Exception("context is null");
		}
		// 先从Dataprovider中获取设备信息，如果为获取到则从DeviceInfo.jar中获取
		// lcp暂时注释掉:解决mac为空问题
		// if (!this.initDeviveInfoFromDataprovider(context)) {
		this.initDeviveInfoFromDeviceInfoJar(context);
		// }
		StringBuilder sb = new StringBuilder();
		sb.append(" deviceName:" + this.deviceName);
		sb.append(" hardwareVersion:" + this.hardwareVersion);
		sb.append(" softwareVersion:" + this.softwareVersion);
		sb.append(" wlanMac:" + this.wlanMac);
		sb.append(" mac:" + this.mac);
		sb.append(" sn:" + this.sn);
		sb.append(" model:" + this.model);
		sb.append(" androidId:" + this.androidId);
		sb.append(" versionCode:" + this.versionCode);
		sb.append(" deviceType:" + this.deviceType);
		sb.append(" deviceCode:" + this.deviceCode);
		sb.append(" launcherVersionName:" + this.launcherVersionName);
		Log.i(TAG, "DeviceInfo[" + sb.toString() + "]");
	}

	// 区分国内海外 -1出错，0表示国内盒子 1表示国内电视版本 2表示海外盒子
	public int getSofewareInfo() {
		return this.sofewareInfo;
	}

	private void initDeviveInfoFromDeviceInfoJar(Context context)
			throws RemoteException {
		if (context == null) {
			return;
		}
		SystemInfoManager manager = SystemInfoManager.getSystemInfoManager();
		this.mac = manager.getMacInfo();
		if (this.mac == null) {
			this.mac = "";
		}
		this.sn = manager.getSnInfo();
		if (this.sn == null) {
			this.sn = "";
		}
		this.sofewareInfo = manager.getSofewareInfo();
		Log.d(TAG, "sofewareInfo == " + sofewareInfo);
		this.model = manager.getProductModel();
		if (this.model == null) {
			this.model = "";
		}
		this.softwareVersion = manager.getFirmwareVersion();
		if (this.softwareVersion == null) {
			this.softwareVersion = "";
		}
		// Device device = DeviceFactory.getInstance().getDevice();
		DeviceScreen device = new DeviceScreen();
		if (device != null) {
			// device.initDeviceInfo(context);
			// device = DeviceFactory.getInstance().getDevice();//
			// 在device初始化后要重新获取一次device
			// 获取设备名称
			// this.deviceName = device.getDeviceName();
			this.deviceName = device.getHardwareVersion();
			if (this.deviceName == null) {
				this.deviceName = "";
			}
			Log.d(TAG, "DeviceInfoJar---deviceName:" + this.deviceName);
			// 获取硬件版本
			this.hardwareVersion = device.getHardwareVersion();
			if (this.hardwareVersion == null) {
				this.hardwareVersion = "";
			}
			Log.d(TAG, "DeviceInfoJar---hardwareVersion:"
					+ this.hardwareVersion);
			// 获取软件版本
			// this.softwareVersion = device.getSoftwareVersion();
			Log.d(TAG, "DeviceInfoJar---softwareVersion:"
					+ this.softwareVersion);
			// 获取无线mac地址
			this.wlanMac = device.getWlanMac();
			if (this.wlanMac == null) {
				this.wlanMac = "";
			}
			Log.d(TAG, "DeviceInfoJar---wlanMac:" + this.wlanMac);
			// 获取有线mac地址
			// this.mac = device.getMac();
			Log.d(TAG, "DeviceInfoJar---mac:" + this.mac);
			// 获取sn
			// this.sn = device.getSN();
			Log.d(TAG, "DeviceInfoJar---sn:" + this.sn);
			// 获取设备类型
			// this.model = device.getModel();
			this.initUserType();
			Log.d(TAG, "DeviceInfoJar---model:" + this.model);
			// 获取设备ID
			this.androidId = device.getAndroidId(context);
			if (this.androidId == null) {
				this.androidId = "";
			}
			Log.d(TAG, "DeviceInfoJar--androidId:" + this.androidId);
			// 获取设备版本
			// this.versionCode = device.getVersionCode() + "";
			this.versionCode = device.getSoftwareVersion();
			if (this.versionCode == null) {
				this.versionCode = "";
			}
			Log.d(TAG, "DeviceInfoJar---versionCode:" + this.versionCode);
			// 获取设备码
			// this.getDeviceCode(device);
			// 获取launcher版本名称
			this.initLauncherVersionName(context);
			Log.d(TAG, "DeviceInfoJar--launcherVersionName:"
					+ this.launcherVersionName);
		}
	}

	/**
	 * 设备码,需要访问访问网络所以放到了子线程中
	 * 
	 * @Title: DeviceInfoActivity
	 * @Description: TODO
	 */
	// private void getDeviceCode(final Device device) {
	// new Thread() {
	// @Override
	// public void run() {
	// try {
	// DeviceInfoManager.this.deviceCode = device.getDeviceCode();
	// Log.d(TAG, "DeviceInfoJar--deviceCode:" +
	// DeviceInfoManager.this.deviceCode);
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// };
	// }.start();
	// }

	/**
	 * 从dataprovider开放的数据中获取设备信息
	 * 
	 * @Title: UserInfoUtil
	 * @author:郭松胜
	 * @Description: TODO
	 * @param context
	 */
	private boolean initDeviveInfoFromDataprovider(Context context) {
		if (context == null) {
			return false;
		}
		boolean isSuccess = false;
		Cursor cursor = null;
		try {
			ContentResolver resolver = context.getContentResolver();
			cursor = resolver.query(URI_DEVICES_INFO, null, null, null, null);
			if (cursor == null || cursor.getCount() <= 0) {
				Log.d(TAG, "---" + cursor.getCount());
				return isSuccess;
			}
			cursor.moveToFirst();
			do {
				// 获取设备名称
				this.deviceName = cursor.getString(cursor
						.getColumnIndex("deviceName"));
				if (this.deviceName == null) {
					this.deviceName = "";
				}
				Log.d(TAG, "Dataprovider---deviceName:" + this.deviceName);

				// 获取硬件版本
				this.hardwareVersion = cursor.getString(cursor
						.getColumnIndex("hardwareVersion"));
				if (this.hardwareVersion == null) {
					this.hardwareVersion = "";
				}
				Log.d(TAG, "Dataprovider---hardwareVersion:"
						+ this.hardwareVersion + "isnull"
						+ (this.hardwareVersion == null));

				// 获取软件版本
				this.softwareVersion = cursor.getString(cursor
						.getColumnIndex("softwareVersion"));
				if (this.softwareVersion == null) {
					this.softwareVersion = "";
				}
				Log.d(TAG, "Dataprovider---softwareVersion:"
						+ this.softwareVersion);

				// 获取无线mac地址
				this.wlanMac = cursor.getString(cursor
						.getColumnIndex("wlanMac"));
				if (this.wlanMac == null) {
					this.wlanMac = "";
				}
				Log.d(TAG, "Dataprovider---wlanMac:" + this.wlanMac
						+ "isnull" + (this.wlanMac == null));

				// 获取有线mac地址
				this.mac = cursor.getString(cursor.getColumnIndex("mac"));
				if (this.mac == null) {
					this.mac = "";
				}
				Log.d(TAG, "Dataprovider---mac:" + this.mac);

				// 获取sn
				this.sn = cursor.getString(cursor.getColumnIndex("sn"));
				if (this.sn == null) {
					this.sn = "";
				}
				Log.d(TAG, "Dataprovider---sn:" + this.sn);

				// 获取设备类型
				this.model = cursor.getString(cursor.getColumnIndex("model"));
				if (this.model == null) {
					this.model = "";
				}
				Log.d(TAG, "Dataprovider---model:" + this.model);

				this.initUserType();

				// 获取设备ID
				this.androidId = cursor.getString(cursor
						.getColumnIndex("androidId"));
				if (this.androidId == null) {
					this.androidId = "";
				}
				Log.d(TAG, "Dataprovider--androidId:" + this.androidId);

				// 获取设备版本
				this.versionCode = cursor.getString(cursor
						.getColumnIndex("versionCode"));
				if (this.versionCode == null) {
					this.versionCode = "";
				}
				Log.d(TAG, "Dataprovider---versionCode:" + this.versionCode);

				// 获取设备码
				this.deviceCode = cursor.getString(cursor
						.getColumnIndex("deviceCode"));
				if (this.deviceCode == null) {
					this.deviceCode = "";
				}
				Log.d(TAG, "Dataprovider--deviceCode:" + this.deviceCode);

				// 获取launcher版本名称
				this.initLauncherVersionName(context);
				Log.d(TAG, "Dataprovider--launcherVersionName:"
						+ this.launcherVersionName);
				isSuccess = true;
			} while (cursor.moveToNext());
		} catch (Exception e) {
			Log.e(TAG, "fail to query device info from dataprovider!!!");
			isSuccess = false;
		} finally {
			if (null != cursor && !cursor.isClosed()) {
				cursor.close();
			}
		}
		return isSuccess;
	}

	private void initUserType() {
		if (this.model != null
				&& (this.model.startsWith("n320") || this.model
						.startsWith("D55s"))) {
			this.deviceType = DeviceType.ClOUD_SCREEN;
		} else {
			this.deviceType = DeviceType.DOMY_BOX;
		}
	}

	/**
	 * 获取mac
	 * 
	 * @return
	 */
	public String getMac() {
		return this.mac;
	}

	/**
	 * 获取sn
	 * 
	 * @return
	 */
	public String getSn() {
		return this.sn;
	}

	/**
	 * 获取rom版本号
	 * 
	 * @return
	 */
	public String getRomVersion() {
		return this.romVersion;
	}

	public String getAndroidId() {
		return this.androidId;
	}

	public void setAndroidId(String androidId) {
		this.androidId = androidId;
	}

	public String getDeviceName() {
		return this.deviceName;
	}

	public String getHardwareVersion() {
		return this.hardwareVersion;
	}

	public String getSoftwareVersion() {
		return this.softwareVersion;
	}

	public String getWlanMac() {
		return this.wlanMac;
	}

	public String getModel() {
		return this.model;
	}

	public String getVersionCode() {
		return this.versionCode;
	}

	public String getDeviceCode() {
		return this.deviceCode;
	}

	public DeviceType getDeviceType() {
		return this.deviceType;
	}

	public String getLauncherVersionName() {
		return this.launcherVersionName;
	}

	/**
	 * 获取Launcher版本号
	 * 
	 * @param context
	 * @return
	 */
	private void initLauncherVersionName(Context context) {
		if (TextUtils.isEmpty(this.launcherVersionName)) {
			try {
				PackageManager manager = context.getPackageManager();// 得到包管理对象
				PackageInfo info = null;
				if (this.checkPackage(manager,
						"com.hiveview.cloudscreen.launcher")) {
					info = manager.getPackageInfo(
							"com.hiveview.cloudscreen.launcher", 0);// 获取指定包的信息
				} else {
					info = manager.getPackageInfo("com.hiveview.tv", 0);// 获取指定包的信息
				}
				this.launcherVersionName = info.versionName;// 获取版本
			} catch (NameNotFoundException e) {
				Log.e(TAG, "initLauncherVersionName NameNotFoundException");
				this.launcherVersionName = "unkonwn";
			}
		}
	}

	public boolean isBox() {
		return this.deviceType == DeviceType.DOMY_BOX;
	}

	public boolean isCloudScreen() {
		return this.deviceType == DeviceType.ClOUD_SCREEN;
	}

	private enum DeviceType {
		DOMY_BOX, ClOUD_SCREEN;
	}

	public boolean checkPackage(PackageManager manager, String packageName) {
		if (packageName == null || "".equals(packageName)) {
			return false;
		}
		try {
			manager.getApplicationInfo(packageName,
					PackageManager.GET_UNINSTALLED_PACKAGES);
			return true;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}

}
