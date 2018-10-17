package cc.wco.autoupdate;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import cc.wco.autoupdate.interfaces.ForceExitCallBack;
import cc.wco.autoupdate.model.IUpdateEntity;
import cc.wco.autoupdate.model.UpdateEntity;
import cc.wco.autoupdate.utils.DownloadReceiver;
import cc.wco.autoupdate.utils.DownloadService;
import cc.wco.autoupdate.utils.FileUtils;
import cc.wco.autoupdate.utils.JSONHelper;
import cc.wco.autoupdate.utils.NetWorkUtils;
import cc.wco.autoupdate.view.ProgressView;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.os.Build.VERSION_CODES.M;

/**
 * Created by cretin on 2017/3/13.
 */
public class AutoUpdateUtil {
    private static String TAG = "ifu25:AutoUpdateLib";
    private static Context mContext;
    private static AutoUpdateUtil mAutoUpdateUtil;
    //广播接受者
    private static MyDownloadReceiver mDownloadReceiver;
    //下载服务intent
    private static Intent downloadServiceIntent;
    //权限请求码
    private static int PERMISSON_REQUEST_CODE = 2;
    //定义一个展示下载进度的进度条
    private static ProgressDialog progressDialog;
    //强制退出回调
    private static ForceExitCallBack forceCallBack;
    //检查更新的url
    private static String checkUrl;
    //展示下载进度的方式 对话框模式 通知栏进度条模式
    private static int showType = Builder.TYPE_DIALOG;
    //是否展示忽略此版本的选项 默认开启
    private static boolean canIgnoreThisVersion = true;
    //没有更新时的Toast提示，如果为空则不提示
    private static String noUpdateMsg = "";
    //app图标
    private static int iconRes;
    //appName
    private static String appName;
    //是否开启日志输出
    private static boolean showLog = true;
    //自定义Bean类
    private static Object customUpdateEntity;
    //设置请求方式
    private static int requestMethod = Builder.METHOD_POST;

    //自定义对话框的所有控件的引用
    private static AlertDialog showAndDownDialog;
    private static AlertDialog showAndBackDownDialog;

    //绿色可爱型
    private static TextView showAndDownTvMsg;
    private static TextView showAndDownTvBtn1;
    private static TextView showAndDownTvBtn2;
    private static TextView showAndDownTvTitle;
    private static LinearLayout showAndDownLlProgress;
    private static ImageView showAndDownIvClose;
    private static ProgressView showAndDownUpdateProView;

    //前台展示后台下载
    private static TextView showAndBackDownMsg;
    private static ImageView showAndBackDownClose;
    private static TextView showAndBackDownUpdate;

    //私有化构造方法
    private AutoUpdateUtil() {

    }

    /**
     * 初始化url
     *
     * @param url
     */
    public static void init(String url) {
        checkUrl = url;
    }

    /**
     * 初始化url
     *
     * @param builder
     */
    public static void init(Builder builder) {
        checkUrl = builder.baseUrl;
        showType = builder.showType;
        canIgnoreThisVersion = builder.canIgnoreThisVersion;
        iconRes = builder.iconRes;
        showLog = builder.showLog;
        requestMethod = builder.requestMethod;
        customUpdateEntity = builder.customUpdateEntity;
    }

    /**
     * getInstance()
     *
     * @param context
     * @return
     */
    public static AutoUpdateUtil getInstance(Context context) {
        if (mAutoUpdateUtil == null) {
            mContext = context;
            mAutoUpdateUtil = new AutoUpdateUtil();
            mDownloadReceiver = new MyDownloadReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.MY_RECEIVER");
            context.registerReceiver(mDownloadReceiver, filter);
            Log.d(TAG, "已注册自动更新任务广播");
        }

        return mAutoUpdateUtil;
    }

    /**
     * 检查更新
     */
    public void check(String noUpdateMsg, ForceExitCallBack forceCallBack) {
        AutoUpdateUtil.noUpdateMsg = noUpdateMsg;
        AutoUpdateUtil.forceCallBack = forceCallBack;
        if (TextUtils.isEmpty(checkUrl)) {
            throw new RuntimeException("checkUrl is null. You must call init before using the cretin checking library.");
        } else {
            new DownDataAsyncTask().execute();
        }
    }

    /**
     * 检查更新
     */
    public void check() {
        check("", null);
    }

    /**
     * 检查更新
     */
    public void check(String noUpdateMsg) {
        check(noUpdateMsg, null);
    }

    /**
     * 检查更新
     */
    public void check(ForceExitCallBack forceCallBack) {
        check("", forceCallBack);
    }

    /**
     * 取消广播的注册
     */
    public void destroy() {
        //不要忘了这一步
        if (mContext != null && downloadServiceIntent != null) {
            mContext.stopService(downloadServiceIntent);
        }
        if (mContext != null && mDownloadReceiver != null) {
            mContext.unregisterReceiver(mDownloadReceiver);
            Log.d(TAG, "已卸载自动更新任务广播");
        }

        //清空变量，防止退出程序后变量未清空，下次进入程序不能正常初始化
        mAutoUpdateUtil = null;
        mDownloadReceiver = null;
    }

    /**
     * 异步任务下载数据
     */
    class DownDataAsyncTask extends AsyncTask<String, Void, UpdateEntity> {

        @Override
        protected UpdateEntity doInBackground(String... params) {
            HttpURLConnection httpURLConnection = null;
            InputStream is = null;
            StringBuilder sb = new StringBuilder();
            try {
                //准备请求的网络地址
                URL url = new URL(checkUrl);
                //调用openConnection得到网络连接，网络连接处于就绪状态
                httpURLConnection = (HttpURLConnection) url.openConnection();
                //设置网络连接超时时间5S
                httpURLConnection.setConnectTimeout(5 * 1000);
                //设置读取超时时间
                httpURLConnection.setReadTimeout(5 * 1000);
                if (requestMethod == Builder.METHOD_POST) {
                    httpURLConnection.setRequestMethod("POST");
                } else {
                    httpURLConnection.setRequestMethod("GET");
                }
                httpURLConnection.connect();
                //if连接请求码成功
                if (httpURLConnection.getResponseCode() == httpURLConnection.HTTP_OK) {
                    is = httpURLConnection.getInputStream();
                    byte[] bytes = new byte[1024];
                    int i = 0;
                    while ((i = is.read(bytes)) != -1) {
                        sb.append(new String(bytes, 0, i, "utf-8"));
                    }
                    is.close();
                }
                if (showLog) {
                    if (TextUtils.isEmpty(sb.toString())) {
                        Log.e(TAG, "自动更新library返回的数据为空，请检查请求方法是否设置正确，默认为post请求，再检查地址是否输入有误");
                    } else {
                        Log.d(TAG, "自动更新数据：" + sb.toString());
                    }
                }

                //自定义更新对象不为空时用自定义的对象
                if (customUpdateEntity != null) {
                    if (customUpdateEntity instanceof IUpdateEntity) {
                        IUpdateEntity o = (IUpdateEntity) JSONHelper.parseObject(sb.toString(), customUpdateEntity.getClass()); //反序列化
                        UpdateEntity updateEntity = new UpdateEntity();
                        updateEntity.setAppCode(o.getAppCode());
                        updateEntity.setAppName(o.getAppName());
                        updateEntity.setVersionCode(o.getVersionCode());
                        updateEntity.setVersionName(o.getVersionName());
                        updateEntity.setVersionDate(o.getVersionDate());
                        updateEntity.setApkSize(o.getApkSize());
                        updateEntity.setApkMd5(o.getApkMd5());
                        updateEntity.setDownloadUrl(o.getDownloadUrl());
                        updateEntity.setUpdateLog(o.getUpdateLog());
                        updateEntity.setIsForceUpdate(o.getIsForceUpdate());
                        updateEntity.setPreBaselineCode(o.getPreBaselineCode());
                        updateEntity.setHasAffectCodes(o.getHasAffectCodes());
                        return updateEntity;
                    } else {
                        throw new RuntimeException("自定义升级实体类 " + customUpdateEntity.getClass().getName() + " 未实现 IUpdateEntity 接口");
                    }
                }
                return JSONHelper.parseObject(sb.toString(), UpdateEntity.class);//反序列化
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                throw new RuntimeException("自动升级解决 json 出错，请按照 IUpdateEntity 所需参数返回数据，json必须包含 IUpdateEntity 所需全部字段");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(UpdateEntity data) {
            super.onPostExecute(data);
            if (data == null) return;

            if (data.isForceUpdate == 2) {
                //所有旧版本强制更新
                showUpdateDialog(data, true, false);
            } else if (data.isForceUpdate == 1) {
                //hasAffectCodes提及的版本强制更新
                if (data.versionCode > getVersionCode(mContext)) {
                    //有更新
                    String[] hasAffectCodes = data.hasAffectCodes.split("\\|");
                    if (Arrays.asList(hasAffectCodes).contains(getVersionCode(mContext) + "")) {
                        //被列入强制更新 不可忽略此版本
                        showUpdateDialog(data, true, false);
                    } else {
                        String dataVersion = data.versionName;
                        if (!TextUtils.isEmpty(dataVersion)) {
                            List listCodes = loadArray();
                            if (!listCodes.contains(dataVersion)) {
                                //没有设置为已忽略
                                showUpdateDialog(data, false, true);
                            }
                        }
                    }
                } else {
                    if (!TextUtils.isEmpty(noUpdateMsg)) {
                        Toast.makeText(mContext, noUpdateMsg, Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (data.isForceUpdate == 0) {
                if (data.versionCode > getVersionCode(mContext)) {
                    showUpdateDialog(data, false, true);
                } else {
                    if (!TextUtils.isEmpty(noUpdateMsg)) {
                        Toast.makeText(mContext, noUpdateMsg, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    /**
     * 显示更新对话框
     *
     * @param data
     */
    private void showUpdateDialog(final UpdateEntity data, final boolean isForceUpdate, boolean showIgnore) {
        //更新日志增加版本号
        if (TextUtils.isEmpty(data.updateLog)) {
            data.setUpdateLog("最新版本：" + data.getVersionName() + "\n\n新版本，欢迎更新");
        } else {
            data.setUpdateLog("最新版本：" + data.getVersionName() + "\n\n" + data.updateLog);
        }

        if (showType == Builder.TYPE_DIALOG || showType == Builder.TYPE_NITIFICATION) {
            //简约式对话框展示对话信息的方式
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            AlertDialog alertDialog = builder.create();
            String updateLog = data.updateLog;
            String versionName = data.versionName;
            alertDialog.setTitle("新版本：" + versionName);
            alertDialog.setMessage(updateLog);
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (isForceUpdate) {
                        if (forceCallBack != null)
                            forceCallBack.exit();
                    }
                }
            });
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "更新", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startUpdate(data);
                }
            });
            if (canIgnoreThisVersion && showIgnore) {
                final String finalVersionName = versionName;
                alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "忽略此版本", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //忽略此版本
                        List listCodes = loadArray();
                        if (listCodes != null) {
                            listCodes.add(finalVersionName);
                        } else {
                            listCodes = new ArrayList();
                            listCodes.add(finalVersionName);
                        }
                        saveArray(listCodes);
                        Toast.makeText(mContext, "此版本已忽略", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            if (isForceUpdate) {
                alertDialog.setCancelable(false);
            }
            alertDialog.show();
            ((TextView) alertDialog.findViewById(android.R.id.message)).setLineSpacing(5, 1);
            Button btnPositive = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            Button btnNegative = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            btnNegative.setTextColor(Color.parseColor("#16b2f5"));
            if (canIgnoreThisVersion && showIgnore) {
                Button btnNeutral = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
                btnNeutral.setTextColor(Color.parseColor("#16b2f5"));
            }
            btnPositive.setTextColor(Color.parseColor("#16b2f5"));
        } else if (showType == Builder.TYPE_DIALOG_WITH_PROGRESS) {
            //在一个对话框中展示信息和下载进度
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext, cc.wco.autoupdate.R.style.dialog);
            View view = View.inflate(mContext, cc.wco.autoupdate.R.layout.download_dialog, null);
            builder.setView(view);
            showAndDownTvBtn1 = (TextView) view.findViewById(cc.wco.autoupdate.R.id.tv_btn1);
            showAndDownTvBtn2 = (TextView) view.findViewById(cc.wco.autoupdate.R.id.tv_btn2);
            showAndDownTvTitle = (TextView) view.findViewById(cc.wco.autoupdate.R.id.tv_title);
            showAndDownTvMsg = (TextView) view.findViewById(cc.wco.autoupdate.R.id.tv_msg);
            showAndDownIvClose = (ImageView) view.findViewById(cc.wco.autoupdate.R.id.iv_close);
            showAndDownLlProgress = (LinearLayout) view.findViewById(cc.wco.autoupdate.R.id.ll_progress);
            showAndDownUpdateProView = (ProgressView) showAndDownLlProgress.findViewById(cc.wco.autoupdate.R.id.progressView);
            String updateLog = data.updateLog;
            showAndDownTvMsg.setText(updateLog);
            builder.setCancelable(false);
            showAndDownDialog = builder.show();
            showAndDownTvBtn2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String btnStr = showAndDownTvBtn2.getText().toString();
                    if (btnStr.equals("立即更新")) {
                        //检查权限，没有权限弹出提示并请求
                        if (!checkPermission()) {
                            Toast.makeText(mContext, "自动更新需要存储权限！", Toast.LENGTH_SHORT).show();
                            requestPermission();
                            return;
                        }
                        //点更新
                        showAndDownTvMsg.setVisibility(View.GONE);
                        showAndDownLlProgress.setVisibility(View.VISIBLE);
                        showAndDownTvTitle.setText("正在更新...");
                        showAndDownTvBtn2.setText("取消更新");
                        showAndDownTvBtn1.setText("隐藏窗口");
                        showAndDownIvClose.setVisibility(View.GONE);
                        startUpdate(data);
                    } else {
                        //点取消更新
                        showAndDownDialog.dismiss();
                        //取消更新 ？
                        destroy();
                    }
                }
            });

            showAndDownTvBtn1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String btnStr = showAndDownTvBtn1.getText().toString();
                    if (btnStr.equals("下次再说") || btnStr.equals("退出")) {
                        //点下次再说
                        if (isForceUpdate) {
                            if (forceCallBack != null)
                                forceCallBack.exit();
                        } else {
                            showAndDownDialog.dismiss();
                        }
                    } else if (btnStr.equals("隐藏窗口")) {
                        //点隐藏窗口
                        showAndDownDialog.dismiss();
                    }
                }
            });

            showAndDownIvClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //关闭按钮
                    showAndDownDialog.dismiss();
                    if (isForceUpdate) {
                        if (forceCallBack != null)
                            forceCallBack.exit();
                    }
                }
            });

            if (isForceUpdate) {
                //强制更新
                showAndDownTvBtn1.setText("退出");
            }
        } else if (showType == Builder.TYPE_DIALOG_WITH_BACK_DOWN) {
            //前台展示 后台下载
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext, cc.wco.autoupdate.R.style.dialog);
            View view = View.inflate(mContext, cc.wco.autoupdate.R.layout.download_dialog_super, null);
            builder.setView(view);
            showAndBackDownMsg = (TextView) view.findViewById(cc.wco.autoupdate.R.id.tv_content);
            showAndBackDownClose = (ImageView) view.findViewById(cc.wco.autoupdate.R.id.iv_close);
            showAndBackDownUpdate = (TextView) view.findViewById(cc.wco.autoupdate.R.id.tv_update);
            String updateLog = data.updateLog;
            showAndBackDownMsg.setText(updateLog);
            showAndBackDownDialog = builder.show();

            showAndBackDownUpdate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //点更新
                    startUpdate(data);
                    showAndBackDownDialog.dismiss();
                }
            });

            showAndBackDownClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAndBackDownDialog.dismiss();
                    if (isForceUpdate) {
                        if (forceCallBack != null)
                            forceCallBack.exit();
                    }
                }
            });
        }
    }

    /**
     * 开始更新操作
     */
    public void startUpdate(UpdateEntity data) {
        update(data);
    }

    @TargetApi(M)
    private static boolean checkPermission() {
        boolean isGranted = ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        return isGranted;
    }

    @TargetApi(M)
    private static void requestPermission() {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 第一次请求权限时，用户如果拒绝，下一次请求shouldShowRequestPermissionRationale()返回true
            // 向用户解释为什么需要这个权限
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(mContext)
                        .setMessage("自动更新需要获取存储权限，请同意！")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //申请存储权限
                                ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSON_REQUEST_CODE);
                            }
                        }).show();
            } else {
                //申请存储权限
                ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSON_REQUEST_CODE);
            }
        }
    }

    /**
     * 执行自动更更新
     *
     * @param data
     */
    private static void update(final UpdateEntity data) {
        if (TextUtils.isEmpty(data.downloadUrl)) {
            Toast.makeText(mContext, "APP下载路径未配置", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(mContext, "没有挂载的SD卡", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            final String fileName = filePath + "/" + getPackgeName(mContext) + "-update.apk"; //如：/storage/emulated/0/com.willcoo.autoupdate-update.apk
            final File file = new File(fileName);

            //如果文件存在并且MD5一致则直接安装，不一致则删除重新下载
            if (file.exists()) {
                if (FileUtils.getFileMD5(file).toUpperCase().equals(data.getApkMd5().toUpperCase())) {
                    installApkFile(mContext, file);
                    showAndDownDialog.dismiss();
                    return;
                } else {
                    // 删除之前遗留的旧文件
                    file.delete();
                }
            }

            //下载文件，非 wifi 弹出提示
            if (!NetWorkUtils.getCurrentNetType(mContext).equals("wifi")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("提示");
                builder.setMessage("当前未连接WIFI，是否继续下载？");
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        createFileAndDownload(file, data.getDownloadUrl());
                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showAndDownLlProgress.setVisibility(View.GONE);
                        showAndDownTvMsg.setVisibility(View.VISIBLE);
                        showAndDownTvBtn2.setText("立即更新");
                        showAndDownTvBtn1.setText("下次再说");
                        showAndDownTvTitle.setText("发现新版本...");
                        showAndDownIvClose.setVisibility(View.VISIBLE);
                    }
                });
                builder.show();
            } else {
                createFileAndDownload(file, data.getDownloadUrl());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建文件并下载文件
     *
     * @param file
     * @param downurl
     */
    private static void createFileAndDownload(File file, String downurl) {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try {
            // 创建文件
            if (!file.createNewFile()) {
                Toast.makeText(mContext, "文件创建失败", Toast.LENGTH_SHORT).show();
                return;
            }

            //文件创建成功，启动服务下载升级文件
            downloadServiceIntent = new Intent(mContext, DownloadService.class);
            downloadServiceIntent.putExtra("downUrl", downurl);
            downloadServiceIntent.putExtra("fileName", file.getPath());
            downloadServiceIntent.putExtra("appName", "自动更新");
            downloadServiceIntent.putExtra("type", showType);
            if (iconRes != 0) downloadServiceIntent.putExtra("icRes", iconRes);
            mContext.startService(downloadServiceIntent);

            //显示dialog
            if (showType == Builder.TYPE_DIALOG) {
                progressDialog = new ProgressDialog(mContext);
                if (iconRes != 0) {
                    progressDialog.setIcon(iconRes);
                } else {
                    progressDialog.setIcon(cc.wco.autoupdate.R.mipmap.ic_launcher1);
                }
                progressDialog.setTitle("正在更新...");
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL); //设置进度条对话框，样式（水平，旋转）
                progressDialog.setMax(100); //进度最大值
                progressDialog.setCancelable(false);
                progressDialog.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 广播接收器
     *
     * @author user
     */
    private static class MyDownloadReceiver extends DownloadReceiver {
        @Override
        protected void downloadComplete() {
            if (progressDialog != null)
                progressDialog.dismiss();
            if (showAndDownDialog != null)
                showAndDownDialog.dismiss();
            try {
                if (mContext != null && downloadServiceIntent != null)
                    mContext.stopService(downloadServiceIntent);
                if (mContext != null && mDownloadReceiver != null)
                    mContext.unregisterReceiver(mDownloadReceiver);
            } catch (Exception e) {
            }
        }

        @Override
        protected void downloading(int progress) {
            if (showType == Builder.TYPE_DIALOG) {
                if (progressDialog != null)
                    progressDialog.setProgress(progress);
            } else if (showType == Builder.TYPE_DIALOG_WITH_PROGRESS) {
                if (showAndDownUpdateProView != null)
                    showAndDownUpdateProView.setProgress(progress);
            }
        }

        @Override
        protected void downloadFail(String e) {
            if (progressDialog != null)
                progressDialog.dismiss();
            Toast.makeText(mContext, "下载失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 安装app
     *
     * @param context
     * @param file
     */
    public static void installApkFile(Context context, File file) {
        // Android 8.0 未知来源权限检查
        if (Build.VERSION.SDK_INT >= 26) {
            boolean isAllowInstallAPK = context.getPackageManager().canRequestPackageInstalls();
            if (!isAllowInstallAPK) {
                // 此权限不是危险权限不用/能动态申请，经测试9.0模拟器/小米8（android 8.1）/华为荣耀V10（android 8.0）都会自动弹出授权页面
                // 如遇到特殊情况可用以下方式主动提示用户授权
                //Intent i=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+context.getPackageName()));
                //context.startActivity(i);
                //return;
            }
        }

        Intent intent1 = new Intent(Intent.ACTION_VIEW);
        // Arndroid 7.0 文件共享权限处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent1.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            intent1.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            intent1.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        if (context.getPackageManager().queryIntentActivities(intent1, 0).size() > 0) {
            context.startActivity(intent1);
        }
    }

    /**
     * 获得apkPackgeName
     *
     * @param context
     * @return
     */
    public static String getPackgeName(Context context) {
        String packName = "";
        PackageInfo packInfo = getPackInfo(context);
        if (packInfo != null) {
            packName = packInfo.packageName;
        }
        return packName;
    }

    /**
     * 获取版本名称
     *
     * @param context
     * @return
     */
    private static String getVersionName(Context context) {
        String versionName = "";
        PackageInfo packInfo = getPackInfo(context);
        if (packInfo != null) {
            versionName = packInfo.versionName;
        }
        return versionName;
    }

    /**
     * 获得apk版本号
     *
     * @param context
     * @return
     */
    public static int getVersionCode(Context context) {
        int versionCode = 0;
        PackageInfo packInfo = getPackInfo(context);
        if (packInfo != null) {
            versionCode = packInfo.versionCode;
        }
        return versionCode;
    }

    /**
     * 获得apkinfo
     *
     * @param context
     * @return
     */
    public static PackageInfo getPackInfo(Context context) {
        // 获取packagemanager的实例
        PackageManager packageManager = context.getPackageManager();
        // getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = null;
        try {
            packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return packInfo;
    }

    //建造者模式
    public static final class Builder {
        private String baseUrl;
        private int showType = TYPE_DIALOG;
        //是否显示忽略此版本 true 是 false 否
        private boolean canIgnoreThisVersion = true;
        //在通知栏显示进度
        public static final int TYPE_NITIFICATION = 1;
        //对话框显示进度
        public static final int TYPE_DIALOG = 2;
        //对话框展示提示和下载进度
        public static final int TYPE_DIALOG_WITH_PROGRESS = 3;
        //对话框展示提示后台下载
        public static final int TYPE_DIALOG_WITH_BACK_DOWN = 4;
        //POST方法
        public static final int METHOD_POST = 3;
        //GET方法
        public static final int METHOD_GET = 4;
        //显示的app资源图
        private int iconRes;
        //显示的app名
        private String appName;
        //显示log日志
        private boolean showLog;
        //设置请求方式
        private int requestMethod = METHOD_POST;
        //自定义Bean类
        private Object customUpdateEntity;

        public final AutoUpdateUtil.Builder setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public final AutoUpdateUtil.Builder setUpdateEntity(Object updateEntity) {
            this.customUpdateEntity = updateEntity;
            return this;
        }

        public final AutoUpdateUtil.Builder showLog(boolean showLog) {
            this.showLog = showLog;
            return this;
        }

        public final AutoUpdateUtil.Builder setRequestMethod(int requestMethod) {
            this.requestMethod = requestMethod;
            return this;
        }

        public final AutoUpdateUtil.Builder setShowType(int showType) {
            this.showType = showType;
            return this;
        }

        public final AutoUpdateUtil.Builder setIconRes(int iconRes) {
            this.iconRes = iconRes;
            return this;
        }

        public final AutoUpdateUtil.Builder setAppName(String appName) {
            this.appName = appName;
            return this;
        }

        public final AutoUpdateUtil.Builder setIgnoreThisVersion(boolean canIgnoreThisVersion) {
            this.canIgnoreThisVersion = canIgnoreThisVersion;
            return this;
        }

        public final Builder build() {
            return this;
        }

    }

    public boolean saveArray(List<String> list) {
        SharedPreferences sp = mContext.getSharedPreferences("ingoreList", mContext.MODE_PRIVATE);
        SharedPreferences.Editor mEdit1 = sp.edit();
        mEdit1.putInt("Status_size", list.size());

        for (int i = 0; i < list.size(); i++) {
            mEdit1.remove("Status_" + i);
            mEdit1.putString("Status_" + i, list.get(i));
        }
        return mEdit1.commit();
    }

    public List loadArray() {
        List<String> list = new ArrayList<>();
        SharedPreferences mSharedPreference1 = mContext.getSharedPreferences("ingoreList", mContext.MODE_PRIVATE);
        list.clear();
        int size = mSharedPreference1.getInt("Status_size", 0);
        for (int i = 0; i < size; i++) {
            list.add(mSharedPreference1.getString("Status_" + i, null));
        }
        return list;
    }
}
