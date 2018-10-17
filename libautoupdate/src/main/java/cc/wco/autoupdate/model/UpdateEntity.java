package cc.wco.autoupdate.model;

/**
 * Created by ifu25 on 2018/08/25
 */
public class UpdateEntity implements IUpdateEntity {

    @Override
    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    @Override
    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    @Override
    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    @Override
    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    @Override
    public String getVersionDate() {
        return versionDate;
    }

    public void setVersionDate(String versionDate) {
        this.versionDate = versionDate;
    }

    @Override
    public String getApkSize() {
        return apkSize;
    }

    public void setApkSize(String apkSize) {
        this.apkSize = apkSize;
    }

    @Override
    public String getApkMd5() {
        return apkMd5;
    }

    public void setApkMd5(String apkMd5) {
        this.apkMd5 = apkMd5;
    }

    @Override
    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    @Override
    public String getUpdateLog() {
        return updateLog;
    }

    public void setUpdateLog(String updateLog) {
        this.updateLog = updateLog;
    }

    @Override
    public int getIsForceUpdate() {
        return isForceUpdate;
    }

    public void setIsForceUpdate(int isForceUpdate) {
        this.isForceUpdate = isForceUpdate;
    }

    @Override
    public int getPreBaselineCode() {
        return preBaselineCode;
    }

    public void setPreBaselineCode(int preBaselineCode) {
        this.preBaselineCode = preBaselineCode;
    }

    @Override
    public String getHasAffectCodes() {
        return hasAffectCodes;
    }

    public void setHasAffectCodes(String hasAffectCodes) {
        this.hasAffectCodes = hasAffectCodes;
    }

    /**
     * APP 代号
     */
    public String appCode="";

    /**
     * APP 名称
     */
    public String appName="";

    /**
     * 版本号
     */
    public int versionCode=0;

    /**
     * 版本名称
     */
    public String versionName="";

    /**
     * 版本日期
     * @return
     */
    public String versionDate="";

    /**
     * 安装包大小，单位字节
     */
    public String apkSize="";

    /**
     * 安装包 MD5 值
     */
    public String apkMd5="";

    /**
     * 新安装包下载地址
     */
    public String downloadUrl="";

    /**
     * 更新日志
     */
    public String updateLog="";

    /**
     * 是否强制更新，0：不强制更新 1：hasAffectCodes拥有字段强制更新 2：所有版本强制更新
     */
    public int isForceUpdate = 0;

    /**
     * 上一个版本版本号
     */
    public int preBaselineCode = 0;

    /**
     * 受影响的版本号，如果开启强制更新，那么这个字段包含的所有版本都会被强制更新。格式：2|3|4
     */
    public String hasAffectCodes = "";
}
