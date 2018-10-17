package cc.wco.autoupdate.model;

/**
 * Created by ifu25 on 2018/8/25
 */

public interface IUpdateEntity {
    /**
     * 获取 APP 代号
     */
    String getAppCode();

    /**
     * 获取 APP 名称
     */
    String getAppName();

    /**
     * 获取版本号
     */
    int getVersionCode();

    /**
     * 获取版本名称
     */
    String getVersionName();

    /**
     * 获取版本日期
     * @return
     */
    String getVersionDate();

    /**
     * 安装包大小，单位字节
     */
    String getApkSize();

    /**
     * 安装包 MD5 值
     */
    String getApkMd5();

    /**
     * 新安装包的下载地址
     */
    String getDownloadUrl();

    /**
     * 更新日志
     */
    String getUpdateLog();

    /**
     * 是否强制更新，0：不强制更新 1：hasAffectCodes拥有字段强制更新 2：所有版本强制更新
     */
    int getIsForceUpdate();

    /**
     * 上一个版本版本号
     */
    int getPreBaselineCode();

    /**
     * 受影响的版本号，如果开启强制更新，那么这个字段包含的所有版本都会被强制更新。格式：2|3|4
     */
    String getHasAffectCodes();
}
