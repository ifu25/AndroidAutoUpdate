package cc.wco.autoupdatedemo;

import android.app.Application;

import cc.wco.autoupdate.AutoUpdateUtil;

public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AutoUpdateUtil.Builder builder = new AutoUpdateUtil.Builder()
                .setBaseUrl("https://app.lttc.cn/auto-demo.json")
                .setIgnoreThisVersion(true)
                .setShowType(AutoUpdateUtil.Builder.TYPE_DIALOG_WITH_PROGRESS)
                .setIconRes(R.mipmap.ic_launcher)
                .showLog(true)
                .setRequestMethod(AutoUpdateUtil.Builder.METHOD_GET)
                .build();
        AutoUpdateUtil.init(builder);
    }
}