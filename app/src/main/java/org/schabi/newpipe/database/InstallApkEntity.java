package org.schabi.newpipe.database;

/**
 * package_name:org.schabi.newpipe.database
 * author: 李文烙
 * date: 2018/7/3
 * company: hiveview
 * annotation
 */

public class InstallApkEntity {
    private int id;
    private String appName;
    private String bundleId;
    private String developer;
    private String md5;
    private String size;
    private String versionDesc;
    private String versionNo;
    private String versionUrl;
    private String status;

    public InstallApkEntity() {
    }

    public InstallApkEntity(int id, String appName, String bundleId, String developer, String md5, String size, String versionDesc, String versionNo, String versionUrl, String status) {
        this.id = id;
        this.appName = appName;
        this.bundleId = bundleId;
        this.developer = developer;
        this.md5 = md5;
        this.size = size;
        this.versionDesc = versionDesc;
        this.versionNo = versionNo;
        this.versionUrl = versionUrl;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getBundleId() {
        return bundleId;
    }

    public void setBundleId(String bundleId) {
        this.bundleId = bundleId;
    }

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getVersionDesc() {
        return versionDesc;
    }

    public void setVersionDesc(String versionDesc) {
        this.versionDesc = versionDesc;
    }

    public String getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(String versionNo) {
        this.versionNo = versionNo;
    }

    public String getVersionUrl() {
        return versionUrl;
    }

    public void setVersionUrl(String versionUrl) {
        this.versionUrl = versionUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
