package org.schabi.newpipe.download;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.hiveview.manager.IPmInstallObserver;
import com.hiveview.manager.PmInstallManager;

import org.schabi.newpipe.App;
import org.schabi.newpipe.database.InstallApkEntity;
import org.schabi.newpipe.device.DeviceInfoUtil;
import org.schabi.newpipe.util.Constants;
import org.schabi.newpipe.util.DownloadUtils;
import org.schabi.newpipe.util.Utils;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * package_name:org.schabi.newpipe.download
 * author: 李文烙
 * date: 2018/7/3
 * company: hiveview
 * annotation:
 * desc:静默接口安装数据下载类：baidu输入法
 */

public class DownloadService extends IntentService {
    private static Context mContext;
    private static final String TAG = "DownloadService";

    //保存的包名
    private static final String SAVE_APP_NAME = "baidu.apk";
    //网上下载地址，测试时使用
    private static String downLoadUrl = "http://count.liqucn.com/d.php?id=10041&urlos=android-tv&from_type=web"; //测试地址
    //接口必须数据
    private static String DUI_VERSION = "3.1.5";
    //海外OTA接口
    private static String apkUrlImpl = "http://api.otaupdate.global.domybox.com/otaupdate/directupdate.json";


    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public DownloadService() {
        super("DownService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        mContext = this;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.i(TAG, "HandleIntent");
        //是否安装输入法，未安装情况下，从OTA下载，已经安装的情况下，直接切换
        if (!Utils.isPkgInstalled(App.getInstance(), "com.baidu.input_baidutv")) {
            Log.i(TAG, "download-->url");
            httpGetDownloadUrl();
        } else {
//            switchInputMethod();
        }
    }

    /**
     * 从OTA接口获取下载地址 ,flag标志位为test时，为测试标志
     *
     * @return 下载地址
     */
    public String httpGetDownloadUrl() {
        try {
            Log.v(TAG, "start get");
            OkHttpClient client = new OkHttpClient();//创建okhttp实例
            FormBody body = new FormBody.Builder()
//                    .add("bundleId", Constants.BAIDU_PACKAGE_NAME)
                    .add("bundleId", Constants.BAIDU_PACKAGE_NAME)
                    .build();
            Request request = new Request.Builder().post(body).url(apkUrlImpl).build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                //请求失败时调用
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.i(TAG, "onFailure: " + e);
                }

                //请求成功时调用
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String result = response.body().string();
                        Log.i(TAG, result);
                        String subJson = result.substring(result.indexOf('['), result.indexOf(']') + 1);
                        if (subJson.length() > 2) {
                            List<InstallApkEntity> installApkEntities = JSONObject.parseArray(subJson, InstallApkEntity.class);
                            if (installApkEntities.size() > 0) {
                                for (InstallApkEntity entity : installApkEntities) {
                                    Log.v(TAG, "entitiy:" + entity.getBundleId());
                                    if (entity!=null && entity.getBundleId()!=null)
                                    if (entity.getBundleId().equals(Constants.BAIDU_PACKAGE_NAME)) {
                                        downLoadUrl = entity.getVersionUrl();
                                        if(downLoadUrl.contains("http://")){
                                            new Thread() {
                                                public void run() {
                                                    //丢到主线程去跑,不然会导致hanler与下载任务处于同一线程，导致不能创建loop
                                                    new Handler(Looper.getMainLooper()).post(runnable);
                                                }
                                            }.start();
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
        return downLoadUrl;
    }

    /**
     * 执行下载任务，IntentService原本就是工作线程了，如果直接执行downloadApk
     * 会导致handle与自身同处一个线程内，造成程序崩溃
     * 故应该将该任务与主线程的handle进行绑定
     */
    final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            downloadApk(mContext, downLoadUrl);
        }
    };

    /**
     * 下载Apk, 并设置Apk地址,
     * 默认位置: /storage/emulated/0/inputmethod/
     *
     * @param context     上下文
     * @param downLoadUrl 下载地址
     */
    @SuppressWarnings("unused")
    public void downloadApk(
            Context context,
            String downLoadUrl) {
        Log.v(TAG, "downloadUrl-->" + downLoadUrl);
        Log.v(TAG, Environment.getExternalStorageDirectory().getPath() + "/inputmethod/");
        if (!downLoadUrl.isEmpty())
            DownloadUtils.getInstance().download(downLoadUrl, Environment.getExternalStorageDirectory().getPath() + "/inputmethod/", SAVE_APP_NAME, new DownloadUtils.OnDownloadListener() {
                @Override
                public void onDownloadSuccess(String path) {
                    Toast.makeText(context, "下载成功", Toast.LENGTH_SHORT).show();
                    installAPK(Environment.getExternalStorageDirectory().getPath() + "/inputmethod/" + SAVE_APP_NAME);
                }

                @Override
                public void onDownloading(int progress) {
                    Log.i(TAG, "下载中:" + progress + "%");
                }

                @Override
                public void onDownloadFailed() {
                    Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show();
                }
            });
    }


    /**
     * 安装下载完成的APK
     *
     * @param filePath 安装路径
     */
    private void installAPK(String filePath) {
        Log.i(TAG, "start install");
        PmInstallManager pmInstallManager = PmInstallManager.getPmInstallManager(mContext);
        try {
            pmInstallManager.pmInstall(filePath, new IPmInstallObserver() {
                @Override
                public void packageInstalled(String s, int i) throws RemoteException {
                    Log.i(TAG, "baidu install finished");
                    if (Utils.isPkgInstalled(mContext, "com.baidu.input_baidutv")) {

                    }
//                        switchInputMethod();
                }

                @Override
                public IBinder asBinder() {
                    return null;
                }
            }, 0);

        } catch (Exception e) {
            e.printStackTrace();
            if (Utils.isPkgInstalled(mContext, "com.baidu.input_baidutv")) {

            }
//                switchInputMethod();

        }
    }

    /**
     * 切换输入法
     */
    private void switchInputMethod() {
        if (Utils.isPkgInstalled(mContext, "com.hiveview.inputmanager")) {
            String ACTION_INPUT_METHOD_CLASS_NAME = "input_method_class_name";
            String switchClassName = "com.baidu.input_baidutv/.ImeService";  //待切换的输入法名称
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction("com.hiveview.inputmethod.switch");
            intent.putExtra(ACTION_INPUT_METHOD_CLASS_NAME, switchClassName);
            startActivity(intent);
        }
    }


}
