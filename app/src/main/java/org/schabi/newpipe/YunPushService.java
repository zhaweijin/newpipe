package org.schabi.newpipe;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.hiveview.manager.IPmInstallObserver;
import com.hiveview.manager.PmInstallManager;
import com.hiveview.manager.SystemInfoManager;

import org.reactivestreams.Subscriber;
import org.schabi.newpipe.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by carter on 6/26/17.
 */

public class YunPushService extends Service{

    private String tag = "YunPushService";
    private InstalledReceiver installedReceiver;

    private final String YUNPUSH_10PLUS = "Hiveview_Domybox_InputManager_1.0+_sign.apk";
    private final String YUNPUSH_905 = "Hiveview_Domybox_InputManager_905_sign.apk";
    private final String YUNPUSH_10TONGWEI = "Hiveview_Domybox_InputManager_1.0_sign.apk";

    private String yunPush_apk_path;
	private final String packageNameApk="com.hiveview.inputmanager";

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        if(intent==null)
            return;
        regeditBrocast();

        if(!Utils.isPkgInstalled(this,packageNameApk)){
            copyInstallFile();
        }

        Utils.print(tag,"end");

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }



    private void installAPK(String path){
        try{
            Utils.print(tag,"start install");
            Utils.print(tag,"path:"+path);
            this.yunPush_apk_path = path;
            PmInstallManager pmInstallManager = PmInstallManager.getPmInstallManager(YunPushService.this);
            pmInstallManager.pmInstall(path, new IPmInstallObserver() {
                @Override
                public void packageInstalled(String s, int i) throws RemoteException {
                    Utils.print(tag,"baidu install finished");
                }

                @Override
                public IBinder asBinder() {
                    return null;
                }
            },0);

        }catch (Exception e){
            e.printStackTrace();
        }
    }


     private void copyInstallFile(){
         Utils.print(tag,"copyInstallFile");
         Observable<String> observable=Observable.create(new ObservableOnSubscribe<String>() {
             @Override
             public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                 String filepath = copyYunPushSignApp();
                 emitter.onNext(filepath);
             }
         });
         Observer<String> observer=new Observer<String>() {
             @Override
             public void onSubscribe(Disposable d) {

             }

             @Override
             public void onNext(String s) {
                 Utils.print(tag,">>>>>"+s);
                         installAPK(s);
             }

             @Override
             public void onError(Throwable e) {

             }

             @Override
             public void onComplete() {

             }
         };
         observable.subscribeOn(Schedulers.io())
                 .observeOn(AndroidSchedulers.mainThread())
                 .subscribe(observer);
     }


    /**
     * 拷贝文件
     * @param in
     * @param out
     * @throws IOException
     */
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }




    private void regeditBrocast(){
        installedReceiver = new InstalledReceiver();
        IntentFilter filter = new IntentFilter();

        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addDataScheme("package");

        registerReceiver(installedReceiver, filter);
    }

    private void unRegeditBrocast(){
        if(installedReceiver != null) {
            unregisterReceiver(installedReceiver);
        }
    }


    public class InstalledReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED")) {     // install
                String packageName = intent.getDataString();
                Utils.print(tag, "installed :" + packageName);
                if(packageName.contains(packageNameApk)){
                    try{
                        if(!yunPush_apk_path.equals(""))
                            new File(yunPush_apk_path).delete();
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Utils.print(tag,"onDestroy");
        unRegeditBrocast();
    }



    /**
     * 获取版本号
     *
     * @return 当前应用的版本号
     */
    public int getVersionCode(String packageName) {
        try {
            PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(packageName, 0);
            int versionCode = info.versionCode;
            return versionCode;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }



    /**
     * 拷贝asset 文件
     * @return
     */
    private String copyYunPushSignApp() {
        Utils.print(tag,"copyYunPushSignApp");
        AssetManager assetManager = getAssets();
        InputStream in = null;
        OutputStream out = null;
        String filepath="";
        try {
            SystemInfoManager manager = SystemInfoManager.getSystemInfoManager();
            String version = manager.getFirmwareVersion();
            String[] vers = version.split("\\.");
            String filename;
            if(Integer.parseInt(vers[1])==40){ //1.0+
                filename = YUNPUSH_10PLUS;
            }else if(Integer.parseInt(vers[1])==30 || Integer.parseInt(vers[1])==31){//1.0tw or 1.0cw
                filename = YUNPUSH_10TONGWEI;
            }else if(Integer.parseInt(vers[1])==72 || Integer.parseInt(vers[1])==74 ||
                    Integer.parseInt(vers[1])==76 || Integer.parseInt(vers[1])==78 || Integer.parseInt(vers[1])==70){
                //1.0x,1.0S,3.0x,3.0vc,4.0s
                filename = YUNPUSH_905;
            }else {
                return "";
            }
            Utils.print(tag,"filename=="+filename);
            filepath = Environment.getExternalStorageDirectory().getPath()+"/" +filename;
            if(new File(filepath).exists()){
                Utils.print(tag,filepath+" file exist");
                return filepath;
            }

            in = assetManager.open(filename);
            File outFile = new File(Environment.getExternalStorageDirectory(), filename);
            out = new FileOutputStream(outFile);
            copyFile(in, out);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
        Utils.print(tag,"copy finish=="+filepath);
        return filepath;
    }


}
