package com.aof.mcinabox.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.Toast;

import com.aof.mcinabox.R;
import com.aof.mcinabox.gamecontroller.ckb.achieve.CkbManager;
import com.aof.mcinabox.gamecontroller.definitions.manifest.AppManifest;
import com.aof.mcinabox.launcher.gamedir.GamedirManager;
import com.aof.mcinabox.launcher.lang.LangManager;
import com.aof.mcinabox.launcher.runtime.RuntimeManager;
import com.aof.mcinabox.launcher.setting.SettingManager;
import com.aof.mcinabox.launcher.setting.support.SettingJson;
import com.aof.mcinabox.launcher.theme.ThemeManager;
import com.aof.mcinabox.launcher.tipper.TipperManager;
import com.aof.mcinabox.launcher.uis.BaseUI;
import com.aof.mcinabox.launcher.uis.achieve.UiManager;
import com.aof.mcinabox.utils.BoatUtils;
import com.aof.mcinabox.utils.FileTool;
import com.aof.mcinabox.utils.dialog.DialogUtils;
import com.aof.mcinabox.utils.dialog.support.TaskDialog;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class OldMainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";

    public static final int LAUNCHER_IMPT_RTPACK = 127;
    public static WeakReference<OldMainActivity> CURRENT_ACTIVITY;
    public Timer mTimer;
    public UiManager mUiManager;
    public TipperManager mTipperManager;
    public SettingManager mSettingManager;
    public ThemeManager mThemeManager;
    private static final int REFRESH_DELAY = 0; //ms
    private static final int REFRESH_PERIOD = 1000; //ms
    public static SettingJson Setting;
    private boolean enableSettingChecker = false;

    public void releaseGameFiles() throws IOException {
        Log.i(TAG, "Releasing Game Files");
        File appFile = new File(Objects.requireNonNull(this.getExternalFilesDir("mcinabox")).getAbsolutePath());
        System.out.println(Arrays.toString(getAssets().list("gamedir")));
        FileTool.copyAssets("runtime", appFile + "/runtime");
        RuntimeManager.clearRuntime(this);
        RuntimeManager.installRuntimeFromPath(this, appFile + "/runtime/runtime.tar.xz");

        final TaskDialog mDialog = DialogUtils.createTaskDialog(this, "", "", false);
        mDialog.show();
        new Thread() {
            @Override
            public void run() {
                OldMainActivity.CURRENT_ACTIVITY.get().runOnUiThread(() -> mDialog.setTotalTaskName("正在释放游戏文件，请稍后"));
                FileTool.copyAssets("gamedir", appFile + "/gamedir");
                mDialog.dismiss();
                FileTool.copyAssets("keyboards", appFile + "/keyboards");

//                File file = new File(AppManifest.MCINABOX_KEYBOARD + "/" + "key.json");
//                if (!mManager.loadKeyboard(fileName)) {
//                    DialogUtils.createSingleChoiceDialog(mContext, mContext.getString(R.string.title_error), mContext.getString(R.string.tips_failed_to_import_keyboard_layout), mContext.getString(R.string.title_ok), null);
//                } else {
//                    Toast.makeText(mContext, mContext.getString(R.string.tips_successed_to_import_keyboard_layout), Toast.LENGTH_SHORT).show();
//                }
                refreshLauncher();
                saveLauncherSettingToFile(Setting);
                Looper.prepare();
                Toast.makeText(OldMainActivity.CURRENT_ACTIVITY.get(), "游戏安装成功", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }.start();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_old_main);
        //静态对象
        CURRENT_ACTIVITY = new WeakReference<>(this);
        //使用语言管理器切换语言
        if (!new LangManager(this).fitSystemLang()) {
            return;
        }
        //初始化配置管理器
        mSettingManager = new SettingManager(this);
        boolean isFirst = mSettingManager.isFirstStart();
        //检查配置文件
        if (Setting == null) {
            Setting = checkLauncherSettingFile();
        }
        //初始化清单
        AppManifest.initManifest(this, Setting.getGamedir());
        Setting.setGameDir(GamedirManager.PRIVATE_GAMEDIR);

        //检查目录
        CheckMcinaBoxDir();
        //初始化主题管理器
        mThemeManager = new ThemeManager(this);
        //初始化消息管理器
        mTipperManager = new TipperManager(this);
        //初始化界面管理器
        mUiManager = new UiManager(this, Setting);
        //Life Circle
        mUiManager.onCreate();
        //设置导航栏颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.WHITE);
        }

        Button toolbarButtonNewUi = findViewById(R.id.toolbar_button_new_ui);
        toolbarButtonNewUi.setOnClickListener(v -> {
            Intent i = new Intent(OldMainActivity.this, MainActivity.class);
            startActivity(i);
        });

        //是否初次启动
        if (isFirst) {
            Setting.getConfigurations().setMaxMemory(1024);
            Setting.getConfigurations().setNotCheckGame(true);
            System.out.println("First Start");
            try {
                releaseGameFiles();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //执行自动刷新
        mTimer = new Timer();
        this.mTimer.schedule(createRefreshTimerTask(), REFRESH_DELAY, REFRESH_PERIOD);
        //启用检查
        switchSettingChecker(true);
        //添加无媒体文件标签
        setMCinaBoxNoMedia();
    }

    /**
     * 【重写返回键】
     **/
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            backFromHere();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 【界面切换】
     **/
    public void switchUIs(BaseUI ui, String position) {
        mUiManager.switchUIs(ui, position);
    }

    /**
     * 【设定返回键的执行逻辑和顶部指示器】
     **/
    public void backFromHere() {
        mUiManager.backFromHere();
    }

    /**
     * 【保存启动器配置到配置文件】
     **/
    private void saveLauncherSettingToFile(SettingJson settingJson) {
        mSettingManager.saveSettingToFile();
    }

    /**
     * 【检查MCinaBox的目录结构是否正常】
     **/
    private void updateSettingFromUis() {
        mUiManager.saveConfigToSetting();
    }

    /**
     * 【检查MCinaBox的目录结构是否正常】
     **/
    private void CheckMcinaBoxDir() {
        for (String path : AppManifest.getAllPath()) {
            FileTool.checkFilePath(new File(path), true);
        }
    }

    /**
     * 【检查启动器模板】
     **/
    private SettingJson checkLauncherSettingFile() {
        return mSettingManager.getSettingFromFile();
    }

    /**
     * 【刷新启动器设置】
     **/
    public void refreshLauncher() {
        mUiManager.refreshUis();
    }

    /**
     * 【给Minecraft目录设置无媒体标签】
     **/
    private void setMCinaBoxNoMedia() {
        File file = new File(AppManifest.MINECRAFT_HOME + "/.nomedia");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 【移除缓存文件夹】
     **/
    private void removeTmpFloder() {
        FileTool.deleteDir(AppManifest.MCINABOX_TEMP);
        FileTool.makeFolder(AppManifest.MCINABOX_TEMP);
    }

    @Override
    public void onStop() {
        super.onStop();
        mUiManager.onStop();
        saveLauncherSettingToFile(Setting);
        // recover Timer Task.
        mTimer.cancel();
        //首先要关闭SettingManager的自动检查
        switchSettingChecker(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 重新创建缓存文件夹
        removeTmpFloder();
        switchSettingChecker(false);
    }

    @Override
    public void onRestart() {
        super.onRestart();
        mUiManager.onRestart();
    }

    private TimerTask createRefreshTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    Log.d(TAG, "run: Updating settings.");
                    refreshLauncher();
                    updateSettingFromUis();
                });
            }
        };
    }

    public void restarter() {
        //重启Activity
        Intent i = new Intent(this, OldMainActivity.class);
        this.startActivity(i);
        //结束当前Activity
        finish();
    }

    private void switchSettingChecker(boolean enable) {
        if (mSettingManager != null) {
            if (enable && !enableSettingChecker) {
                mSettingManager.startChecking();
                enableSettingChecker = true;
            } else if (!enable && enableSettingChecker) {
                mSettingManager.stopChecking();
                enableSettingChecker = false;
            }
        }
    }
}