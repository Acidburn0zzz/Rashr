package de.mkrtchyan.recoverytools;

/**
 * Copyright (c) 2014 Ashot Mkrtchyan
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights 
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
 * copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.devspark.appmsg.AppMsg;
import com.google.ads.AdView;

import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.Toolbox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;

import de.mkrtchyan.utils.Common;
import de.mkrtchyan.utils.Downloader;
import de.mkrtchyan.utils.FileChooserDialog;
import de.mkrtchyan.utils.Notifyer;

public class Rashr extends ActionBarActivity {

    private static final String TAG = "Rashr";

    /**
     * Declaring setting names
     */
    public static final String PREF_NAME = "rashr";
    public static final String PREF_KEY_RECOVERY_HISTORY = "last_recovery_history_";
    public static final String PREF_KEY_KERNEL_HISTORY = "last_kernel_history_";
    private static final String PREF_STYLE = "style";
    private static final String PREF_KEY_ADS = "show_ads";
    private static final String PREF_KEY_CUR_VER = "current_version";
    private static final String PREF_KEY_FIRST_RUN = "first_run";

    /**
     * Web Address for download Recovery and Kernel IMGs
     */
    private static final String RECOVERY_HOST_URL = "http://dslnexus.de/Android/recoveries";
//    private static final String KERNEL_HOST_URL = "http://dslnexus.de/Android/kernel";

    /**
     * Used paths and files
     */
    private static final File PathToSd = Environment.getExternalStorageDirectory();
    private static final File PathToRashr = new File(PathToSd, "Rashr");
    private static final File PathToRecoveries = new File(PathToRashr, "recoveries");
    private static final File PathToKernel = new File(PathToRashr, "kernel");
    public static final File PathToCWM = new File(PathToRecoveries, "clockworkmod");
    public static final File PathToTWRP = new File(PathToRecoveries, "twrp");
    public static final File PathToPhilz = new File(PathToRecoveries, "philz");
    public static final File PathToRecoveryBackups = new File(PathToRashr, "recovery-backups");
    public static final File PathToKernelBackups = new File(PathToRashr, "kernel-backups");
    public static final File PathToUtils = new File(PathToRashr, "utils");
    public static final File LastLog = new File("/cache/recovery/last_log");
    public final File Folder[] = {PathToRashr, PathToRecoveries, PathToKernel, PathToCWM,
            PathToTWRP, PathToPhilz, PathToRecoveryBackups, PathToKernelBackups, PathToUtils};
    private final ActionBarActivity mActivity = this;
    private final Context mContext = this;

    /**
     * Declaring needed objects
     */
    private final Notifyer mNotifyer = new Notifyer(mContext);
    private final int APPCOMPAT_DARK = R.style.MyDark;
    private final int APPCOMPAT_LIGHT = R.style.MyLight;
    private final int APPCOMPAT_LIGHT_DARK_BAR = R.style.MyLightDarkBar;
    private File RecoveryCollectionFile;
    private File fRECOVERY, fKERNEL;
    private Shell mShell;
    private Toolbox mToolbox;
    private Device mDevice;
    private DrawerLayout mRashrLayout = null;
    private SwipeRefreshLayout mSwipeUpdater = null;
    private FileChooserDialog fcFlashOtherRecovery = null;
    private final Runnable rRecoveryFlasher = new Runnable() {
        @Override
        public void run() {
            if (fRECOVERY != null) {
                if (fRECOVERY.exists() && fRECOVERY.getName().endsWith(mDevice.getRecoveryExt())) {
                    if (!mDevice.isFOTAFlashed() && !mDevice.isRecoveryOverRecovery()) {
                        /** Flash not need to be handled specially */
                        rFlashRecovery.run();
                    } else {
                        /** Flashing needs to be handled specially (not standard flash method)*/
                        if (mDevice.isFOTAFlashed()) {
                            /** Show instructions if FOTAKernel will be flashed */
                            mNotifyer.createAlertDialog(R.string.warning, R.string.fota, rFlashRecovery).show();
                        } else {
                            /** Get user input if user want to install over recovery now */
                            showOverRecoveryInstructions();
                        }
                    }
                }
            }
            fcFlashOtherRecovery = null;
        }
    };
    private FileChooserDialog fcFlashOtherKernel = null;
    private final Runnable rKernelFlasher = new Runnable() {
        @Override
        public void run() {
            if (fKERNEL != null) {
                if (fKERNEL.exists()) {
                    if (fKERNEL.getName().endsWith(mDevice.getKernelExt())) {
                        rFlashKernel.run();
                    }
                }
            }
            fcFlashOtherKernel = null;
        }
    };
    private boolean version_changed = false;
    private boolean keepAppOpen = true;
    //	"Methods" need a input from user (AlertDialog) or at the end of AsyncTasks
    private final Runnable rFlashRecovery = new Runnable() {
        @Override
        public void run() {
            FlashUtil flashUtil = new FlashUtil(mShell, mContext, mDevice, fRECOVERY,
                    FlashUtil.JOB_FLASH_RECOVERY);
            flashUtil.setKeepAppOpen(keepAppOpen);
            flashUtil.execute();
            FlashUtils_ERRORS.addAll(flashUtil.getERRORS());
        }
    };
    private final Runnable rFlashKernel = new Runnable() {
        @Override
        public void run() {
            FlashUtil flashUtil = new FlashUtil(mShell, mContext, mDevice, fKERNEL,
                    FlashUtil.JOB_FLASH_KERNEL);
            flashUtil.setKeepAppOpen(keepAppOpen);
            flashUtil.execute();
            FlashUtils_ERRORS.addAll(flashUtil.getERRORS());
        }
    };

    private final ArrayList<String> ERRORS = new ArrayList<String>();
    private final ArrayList<String> FlashUtils_ERRORS = new ArrayList<String>();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RecoveryCollectionFile = new File(getFilesDir(), "IMG_SUMS");

        if (Common.getIntegerPref(mContext, PREF_NAME, PREF_STYLE) == 0) {
            /**
             * If theme setting not set (usually on first start)
             * using AppCompat_Light_Dark_Bar theme
             */
            setTheme(APPCOMPAT_LIGHT_DARK_BAR);
        } else {
            /** Using predefined theme */
            setTheme(Common.getIntegerPref(mContext, PREF_NAME, PREF_STYLE));
        }

        setContentView(R.layout.loading_layout);

        final TextView tvLoading = (TextView) findViewById(R.id.tvLoading);

        final Thread StartThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    /** Try to get root access */
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvLoading.setText(R.string.getting_root);
                        }
                    });
                    mShell = Shell.startRootShell(mContext);
                } catch (IOException e) {

                    ERRORS.add(e.toString());
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            findViewById(R.id.pbLoading).setVisibility(View.INVISIBLE);
                            tvLoading.setTextColor(Color.RED);
                            tvLoading.setText(R.string.no_root);
                        }
                    });
                    return;
                }

                try {
                    unpackFiles();
                } catch (IOException e) {
                    ERRORS.add(e.toString());
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvLoading.setText(R.string.failed_unpack_files);
                            tvLoading.setTextColor(Color.RED);
                        }
                    });
                    return;
                }

                mToolbox = new Toolbox(mShell);
                try {
                    File LogCopy = new File(mContext.getFilesDir(), LastLog.getName() + ".txt");
                    mToolbox.setFilePermissions(LastLog, "644");
                    mToolbox.copyFile(LastLog, LogCopy, false, false);
                } catch (Exception e) {
                    ERRORS.add(e.toString());
                }
                mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvLoading.setText(R.string.reading_device);
                        }
                    });
                mDevice = new Device(mContext);
                /** Creating needed folder and unpacking files */
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvLoading.setText(R.string.loading_data);
                    }
                });
                for (File i : Folder) {
                    if (!i.exists()) {
                        i.mkdir();
                    }
                }

                /** If device is not supported, you can report it now or close the App */
                if (!mDevice.isRecoverySupported() && !mDevice.isKernelSupported()) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showDeviceNotSupportedDialog();
                        }
                    });
                } else {
                    if (!Common.getBooleanPref(mContext, PREF_NAME, PREF_KEY_FIRST_RUN)) {
                        /** Setting first start configuration */
                        Common.setIntegerPref(mContext, PREF_NAME, PREF_STYLE, APPCOMPAT_LIGHT_DARK_BAR);
                        Common.setBooleanPref(mContext, PREF_NAME, PREF_KEY_ADS, true);
                        Common.setBooleanPref(mContext, Shell.PREF_NAME, Shell.PREF_LOG, true);
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showUsageWarning();
                            }
                        });
                    } else {
                        /** Checking if version has changed */
                        try {
                            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                            final int previous_version = Common.getIntegerPref(mContext, PREF_NAME, PREF_KEY_CUR_VER);
                            final int current_version = pInfo.versionCode;
                            version_changed = current_version > previous_version;
                            Common.setIntegerPref(mContext, PREF_NAME, PREF_KEY_CUR_VER, current_version);
                        } catch (Exception e) {
                            ERRORS.add(e.toString());
                            version_changed = true;
                        }
                        if (version_changed) {
                            Common.setBooleanPref(mContext, PREF_NAME, PREF_KEY_ADS, true);
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Notifyer.showAppRateDialog(mContext);
                                    showChangelog();
                                }
                            });
                        }
                    }
                }
                try {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                                optimizeLayout();
                            mDevice.downloadUtils(mContext);
                        }
                    });
                } catch (NullPointerException e) {
                    ERRORS.add(e.toString());
                    try {
                        tvLoading.setText(R.string.failed_setup_layout);
                        tvLoading.setTextColor(Color.RED);
                    } catch (RuntimeException ex) {
                        ERRORS.add(ex.toString());
                        ex.printStackTrace();

                    }
                }

//                mActivity.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Dialog dialog = new Dialog(mContext);
//                        ListView listView = new ListView(mContext);
//                        dialog.setContentView(listView);
//                        listView.setAdapter(new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, ERRORS));
//                        dialog.show();
//                    }
//                });
            }
        });
        StartThread.start();
    }

    /**
     * Buttons on Rashr Layout
     */
    public void bFlashRecovery(View view) {
        showFlashRecoveryDialog();
    }

    public void bFlashKernel(View view) {
        showFlashKernelDialog();
    }

    public void bClearCache(View view) {
        Common.deleteFolder(PathToCWM, false);
        Common.deleteFolder(PathToTWRP, false);
        Common.deleteFolder(PathToPhilz, false);
    }

    public void bRebooter(View view) {
        showPopup(R.menu.rebooter_menu, view);
    }

    /**
     * Buttons on FlashRecovery and FlashKernel Dialog
     */
    public void FlashSupportedRecovery(View view) {
        fRECOVERY = null;
        final File path;
        ArrayList<String> Versions;
        if (!mDevice.downloadUtils(mContext)) {
            /**
             * If there files be needed to flash download it and listing device specified recovery
             * file for example recovery-clockwork-touch-6.0.3.1-grouper.img(read out from IMG_SUMS)
             */
            String SYSTEM = view.getTag().toString();
            if (SYSTEM.equals("clockwork")) {
                Versions = mDevice.getCWMVersions();
                path = PathToCWM;
            } else if (SYSTEM.equals("twrp")) {
                Versions = mDevice.getTWRPVersions();
                path = PathToTWRP;
            } else if (SYSTEM.equals("philz")) {
                Versions = mDevice.getPHILZVersions();
                path = PathToPhilz;
            } else {
                return;
            }

            final Dialog RecoveriesDialog = new Dialog(mContext);
            RecoveriesDialog.setTitle(SYSTEM);
            ListView VersionList = new ListView(mContext);
            RecoveriesDialog.setContentView(VersionList);
            VersionList.setAdapter(new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, Versions));
            RecoveriesDialog.show();
            VersionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                    RecoveriesDialog.dismiss();
                    final String fileName = ((TextView) view).getText().toString();

                    if (fileName != null) {
                        fRECOVERY = new File(path, fileName);

                        if (!fRECOVERY.exists()) {
                            Downloader RecoveryDownloader = new Downloader(mContext, RECOVERY_HOST_URL,
                                    fRECOVERY, rRecoveryFlasher);
                            RecoveryDownloader.setRetry(true);
                            RecoveryDownloader.setAskBeforeDownload(true);
                            RecoveryDownloader.setChecksumFile(RecoveryCollectionFile);
                            RecoveryDownloader.ask();
                        } else {
                            rRecoveryFlasher.run();
                        }
                    }
                }
            });
        }
    }

    /**
     * Flash Recovery from storage (separate downloaded)
     */
    public void bFlashOtherRecovery(View view) {
        fRECOVERY = null;
        try {
            fcFlashOtherRecovery = new FileChooserDialog(view.getContext(), PathToSd, new Runnable() {
                @Override
                public void run() {
                    fRECOVERY = fcFlashOtherRecovery.getSelectedFile();
                    rRecoveryFlasher.run();
                }
            });
            fcFlashOtherRecovery.setTitle(R.string.pick_file);
            String AllowedEXT[] = {mDevice.getRecoveryExt()};
            fcFlashOtherRecovery.setAllowedEXT(AllowedEXT);
            fcFlashOtherRecovery.setBrowseUpEnabled(true);
            fcFlashOtherRecovery.setWarn(true);
            fcFlashOtherRecovery.show();
        } catch (NullPointerException e) {
            ERRORS.add(e.toString());
        }
    }

    /**
     * Flash Kernel from storage (separate downloaded)
     */
    public void bFlashOtherKernel(View view) {
        fKERNEL = null;
        try {
            fcFlashOtherKernel = new FileChooserDialog(view.getContext(), PathToSd, new Runnable() {
                @Override
                public void run() {
                    fKERNEL = fcFlashOtherKernel.getSelectedFile();
                    rKernelFlasher.run();
                }
            });
            fcFlashOtherKernel.setTitle(R.string.pick_file);
            String AllowedEXT[] = {mDevice.getKernelExt()};
            fcFlashOtherKernel.setAllowedEXT(AllowedEXT);
            fcFlashOtherKernel.setBrowseUpEnabled(true);
            fcFlashOtherKernel.setWarn(true);
            fcFlashOtherKernel.show();
        } catch (NullPointerException e) {
            ERRORS.add(e.toString());
        }
    }

    public void showFlashHistory(View view) {
        final boolean RecoveryHistory = view.getTag().toString().equals("recovery");
        String PREF_KEY;
        if (RecoveryHistory) {
            PREF_KEY = PREF_KEY_RECOVERY_HISTORY;
        } else {
            PREF_KEY = PREF_KEY_KERNEL_HISTORY;
        }
        final ArrayList<File> HistoryFiles = new ArrayList<File>();
        final ArrayList<String> HistoryFileNames = new ArrayList<String>();
        final Dialog HistoryDialog = new Dialog(mContext);
        HistoryDialog.setTitle(R.string.sHistory);
        ListView HistoryList = new ListView(mContext);
        File tmp;
        for (int i = 0; i < 5; i++) {
            tmp = new File(Common.getStringPref(mContext, PREF_NAME, PREF_KEY + String.valueOf(i)));
            if (!tmp.exists())
                Common.setStringPref(mContext, PREF_NAME, PREF_KEY + String.valueOf(i), "");
            else {
                HistoryFiles.add(tmp);
                HistoryFileNames.add(tmp.getName());
            }
        }
        HistoryList.setAdapter(new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, HistoryFileNames));
        HistoryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                if (HistoryFiles.get(arg2).exists()) {
                    if (RecoveryHistory) {
                        fRECOVERY = HistoryFiles.get(arg2);
                        rRecoveryFlasher.run();
                    } else {
                        fKERNEL = HistoryFiles.get(arg2);
                        rKernelFlasher.run();
                    }
                    HistoryDialog.dismiss();
                } else {
                    AppMsg
                            .makeText(mActivity, R.string.no_choosed, AppMsg.STYLE_ALERT)
                            .show();
                }
            }
        });
        HistoryDialog.setContentView(HistoryList);
        if (HistoryFileNames.toArray().length > 0) {
            HistoryDialog.show();
        } else {
            AppMsg
                    .makeText(mActivity, R.string.no_history, AppMsg.STYLE_ALERT)
                    .show();
        }

    }

    /**
     * RadioButtons on FlashAs Layout
     */
    public void FlashAsOpt(View view) {
        if (view.getTag().toString().equals("recovery")) {
            RadioButton optAsKernel = (RadioButton) findViewById(R.id.optAsKernel);
            optAsKernel.setChecked(false);
        } else {
            RadioButton optAsRecovery = (RadioButton) findViewById(R.id.optAsRecovery);
            optAsRecovery.setChecked(false);
        }
        findViewById(R.id.bFlashAs).setEnabled(true);
    }

    public void flashAs(View view) {
        String path;
        keepAppOpen = false;
        if ((path = getIntent().getData().getPath()) != null) {
            final File IMG = new File(path);
            if (IMG.exists()) {
                RadioButton optAsRecovery = (RadioButton) findViewById(R.id.optAsRecovery);
                if (optAsRecovery.isChecked()) {
                    fRECOVERY = IMG;
                    rRecoveryFlasher.run();
                } else {
                    fKERNEL = IMG;
                    rKernelFlasher.run();
                }
            } else {
                exit();
            }
        } else {
            exit();
        }
    }

    /**
     * Buttons from MenuDrawer
     */
    public void bOpenRecoveryScriptManager(View view) {
        startActivity(new Intent(this, RecoveryScriptManager.class));
    }

    public void bDonate(View view) {
        startActivity(new Intent(view.getContext(), DonationsActivity.class));
    }

    public void bXDA(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://forum.xda-developers.com/showthread.php?t=2334554")));
    }

    public void bExit(View view) {
        exit();
    }

    public void cbLog(View view) {
        CheckBox cbLog = (CheckBox) view;
        Common.setBooleanPref(mContext, Shell.PREF_NAME, Shell.PREF_LOG,
                !Common.getBooleanPref(mContext, Shell.PREF_NAME, Shell.PREF_LOG));
        cbLog.setChecked(Common.getBooleanPref(mContext, Shell.PREF_NAME, Shell.PREF_LOG));
        if (cbLog.isChecked()) {
            findViewById(R.id.bShowLogs).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.bShowLogs).setVisibility(View.INVISIBLE);
        }
    }

    public void bReport(View view) {
        report(true);
    }

    public void bShowLogs(View view) {
        Common.showLogs(view.getContext());
    }

    public void cbShowAds(View view) {
        Common.setBooleanPref(mContext, PREF_NAME, PREF_KEY_ADS,
                !Common.getBooleanPref(mContext, PREF_NAME, PREF_KEY_ADS));
        ((CheckBox) view).setChecked(Common.getBooleanPref(mContext, PREF_NAME, PREF_KEY_ADS));
        optimizeLayout();
    }

    /**
     * Buttons on BackupDrawer
     */
    public void bCreateBackup(View view) {
        createBackup(view.getTag().equals("recovery"));
    }

    public void createBackup(final boolean RecoveryBackup) {
        String prefix;
        String CurrentName;
        String EXT;
        if (RecoveryBackup) {
            prefix = "recovery";
            EXT = mDevice.getRecoveryExt();
            CurrentName = mDevice.getRecoveryVersion();
        } else {
            prefix = "kernel";
            EXT = mDevice.getKernelExt();
            CurrentName = mDevice.getKernelVersion();
        }
        final Dialog dialog = new Dialog(mContext);
        dialog.setTitle(R.string.setname);
        dialog.setContentView(R.layout.dialog_input);
        final Button bGoBackup = (Button) dialog.findViewById(R.id.bGoBackup);
        final EditText etFileName = (EditText) dialog.findViewById(R.id.etFileName);
        final CheckBox optName = (CheckBox) dialog.findViewById(R.id.cbOptInput);
        final String NameHint = prefix + "-from-" + Calendar.getInstance().get(Calendar.DATE)
                + "-" + Calendar.getInstance().get(Calendar.MONTH)
                + "-" + Calendar.getInstance().get(Calendar.YEAR)
                + "-" + Calendar.getInstance().get(Calendar.HOUR)
                + ":" + Calendar.getInstance().get(Calendar.MINUTE) + EXT;
        optName.setText(CurrentName);
        optName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etFileName.setEnabled(!optName.isChecked());
            }
        });

        etFileName.setHint(NameHint);
        bGoBackup.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String EXT;
                File Path;
                final int JOB;
                if (RecoveryBackup) {
                    EXT = mDevice.getRecoveryExt();
                    Path = PathToRecoveryBackups;
                    JOB = FlashUtil.JOB_BACKUP_RECOVERY;
                } else {
                    EXT = mDevice.getKernelExt();
                    Path = PathToKernelBackups;
                    JOB = FlashUtil.JOB_BACKUP_KERNEL;
                }

                String Name = "";
                if (optName.isChecked()) {
                    Name = optName.getText().toString() + EXT;
                } else {
                    if (etFileName.getText() != null && !etFileName.getText().toString().equals("")) {
                        Name = etFileName.getText().toString();
                    }

                    if (Name.equals("")) {
                        Name = String.valueOf(etFileName.getHint());
                    }

                    if (!Name.endsWith(EXT)) {
                        Name = Name + EXT;
                    }
                }

                final File fBACKUP = new File(Path, Name);
                if (fBACKUP.exists()) {
                    AppMsg
                            .makeText(mActivity, R.string.backupalready, AppMsg.STYLE_ALERT)
                            .show();
                } else {
                    FlashUtil BackupCreator = new FlashUtil(mShell, mContext, mDevice, fBACKUP,
                            JOB);
                    BackupCreator.setRunAtEnd(new Runnable() {
                        @Override
                        public void run() {
                            loadBackups();
                        }
                    });
                    BackupCreator.execute();
                    FlashUtils_ERRORS.addAll(BackupCreator.getERRORS());
                }
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void showFlashRecoveryDialog() {
        final Dialog FlashRecoveryDialog = new Dialog(mContext);
        LayoutInflater inflater = getLayoutInflater();
        FlashRecoveryDialog.setTitle(R.string.flash_options);
        try {
            final ScrollView RecoveryLayout = (ScrollView) inflater.inflate(R.layout.recovery_dialog, null);
            LinearLayout layout = (LinearLayout) RecoveryLayout.getChildAt(0);

            FlashRecoveryDialog.setContentView(RecoveryLayout);
            if (!mDevice.isCwmSupported()) {
                layout.removeView(FlashRecoveryDialog.findViewById(R.id.bCWM));
            }
            if (!mDevice.isTwrpSupported()) {
                layout.removeView(FlashRecoveryDialog.findViewById(R.id.bTWRP));
            }
            if (!mDevice.isPhilzSupported()) {
                layout.removeView(FlashRecoveryDialog.findViewById(R.id.bPHILZ));
            }

            FlashRecoveryDialog.show();
        } catch (NullPointerException e) {
            ERRORS.add(e.toString());
            Notifyer.showExceptionToast(mContext, TAG, e);
        }
    }

    private void showFlashKernelDialog() {
        final AlertDialog.Builder FlashKernelDialog = new AlertDialog.Builder(mContext);
        LayoutInflater inflater = getLayoutInflater();
        FlashKernelDialog.setTitle(R.string.flash_options);
        FlashKernelDialog.setView(inflater.inflate(R.layout.kernel_dialog, null));
        FlashKernelDialog.show();
    }

    public void report(final boolean isCancelable) {
        final Dialog reportDialog = mNotifyer.createDialog(R.string.commentar, R.layout.dialog_comment, false, true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                /** Creates a report Email including a Comment and important device infos */
                final Button bGo = (Button) reportDialog.findViewById(R.id.bGo);
                bGo.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        if (!Common.getBooleanPref(mContext, PREF_NAME, PREF_KEY_ADS))
                            AppMsg.makeText(mActivity, R.string.please_ads, AppMsg.STYLE_ALERT).show();
                        AppMsg.makeText(mActivity, R.string.donate_to_support, AppMsg.STYLE_ALERT).show();
                        try {
                            ArrayList<File> files = new ArrayList<File>();
                            File TestResults = new File(mContext.getFilesDir(), "results.txt");
                            try {
                                if (TestResults.exists()) {
                                    if (TestResults.delete()) {
                                        FileOutputStream fos = openFileOutput(TestResults.getName(), Context.MODE_PRIVATE);
                                        fos.write(("Rashr:\n\n" + mShell.execCommand("ls -lR " + PathToRashr.getAbsolutePath()) +
                                                "\nCache Tree:\n" + mShell.execCommand("ls -lR /cache") + "\n" +
                                                "\nMTD result:\n" + mShell.execCommand("cat /proc/mtd") + "\n" +
                                                "\nDevice Tree:\n\n" + mShell.execCommand("ls -lR /dev")).getBytes());
                                    }
                                    files.add(TestResults);
                                }
                            } catch (FileNotFoundException e) {
                                ERRORS.add(e.toString());
                            }
                            if (getPackageManager() != null) {
                                PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                                EditText text = (EditText) reportDialog.findViewById(R.id.etComment);
                                String comment = "";
                                if (text.getText() != null)
                                    comment = text.getText().toString();
                                Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                                intent.setType("text/plain");
                                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"ashotmkrtchyan1995@gmail.com"});
                                intent.putExtra(Intent.EXTRA_SUBJECT, "Rashr " + pInfo.versionName + " report");
                                String message = "Package Infos:" +
                                        "\n\nName: " + pInfo.packageName +
                                        "\nVersion Code: " + pInfo.versionCode ;
                                message +=
                                        "\n\n\nProduct Info: " +
                                        "\n\nManufacture: " + android.os.Build.MANUFACTURER +
                                        "\nDevice: " + Build.DEVICE + " (" + mDevice.getDeviceName() + ")" +
                                        "\nBoard: " + Build.BOARD +
                                        "\nBrand: " + Build.BRAND +
                                        "\nModel: " + Build.MODEL +
                                        "\nFingerprint: " + Build.FINGERPRINT +
                                        "\nAndroid SDK Level: " + Build.VERSION.CODENAME + " (" + Build.VERSION.SDK_INT + ")";

                                if (mDevice.isRecoverySupported()) {
                                    message += "\n\nRecovery Path: " + mDevice.getRecoveryPath() +
                                            "\nRecovery Version: " + mDevice.getRecoveryVersion() +
                                            "\nRecovery MTD: " + mDevice.isRecoveryMTD() +
                                            "\nRecovery DD: " + mDevice.isRecoveryDD() +
                                            "\nCWM: " + mDevice.isCwmSupported() +
                                            "\nTWRP: " + mDevice.isTwrpSupported() +
                                            "\nPHILZ: " + mDevice.isPhilzSupported();
                                }
                                if (mDevice.isKernelSupported()) {
                                    message += "\n\nKernel Path: " + mDevice.getKernelPath() +
                                            "\nKernel Version: " + mDevice.getKernelVersion() +
                                            "\nKernel MTD: " + mDevice.isKernelMTD() +
                                            "\nKernel DD: " + mDevice.isKernelDD();
                                }
                                if (!comment.equals("")) {
                                    message +=
                                            "\n\n\n===========COMMENT==========\n"
                                                            + comment +
                                                "\n=========COMMENT END========\n" ;
                                }
                                message +=
                                        "\n===========PREFS==========\n"
                                                + getAllPrefs() +
                                        "\n=========PREFS END========\n";

                                if (ERRORS.size() > 0) {
                                    message += "Rashr ERRORS:\n";
                                    for (String error : ERRORS) {
                                        message += error + "\n";
                                    }
                                }

                                if (mDevice.getERRORS().size() > 0) {
                                    message += "Device ERRORS:\n";
                                    for (String error : mDevice.getERRORS()) {
                                        message += error + "\n";
                                    }
                                }

                                if (FlashUtils_ERRORS.size() > 0) {
                                    message += "FlashUtils ERRORS:\n";
                                    for (String error : FlashUtils_ERRORS) {
                                        message += error + "\n";
                                    }
                                }

                                intent.putExtra(Intent.EXTRA_TEXT, message);
                                File CommandLogs = new File(mContext.getFilesDir(), Shell.Logs);
                                if (CommandLogs.exists()) {
                                    files.add(CommandLogs);
                                }
                                files.add(new File(getFilesDir(), "last_log.txt"));
                                ArrayList<Uri> uris = new ArrayList<Uri>();
                                for (File file : files) {
                                    mShell.execCommand("cp " + file.getAbsolutePath() + " " + new File(mContext.getFilesDir(), file.getName()).getAbsolutePath());
                                    file = new File(mContext.getFilesDir(), file.getName());
                                    mToolbox.setFilePermissions(file, "644");
                                    uris.add(Uri.fromFile(file));
                                }
                                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                                startActivity(Intent.createChooser(intent, "Send over Gmail"));
                                reportDialog.dismiss();
                            }
                        } catch (Exception e) {
                            ERRORS.add(e.toString());
                            reportDialog.dismiss();
                            Notifyer.showExceptionToast(mContext, TAG, e);
                        }
                    }
                });
            }
        }).start();
        reportDialog.setCancelable(isCancelable);
        reportDialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mRashrLayout.isDrawerOpen(Gravity.LEFT)) {
                    mRashrLayout.closeDrawer(Gravity.LEFT);
                } else {
                    mRashrLayout.openDrawer(Gravity.LEFT);
                }
                if (mRashrLayout.isDrawerOpen(Gravity.RIGHT)) {
                    mRashrLayout.closeDrawer(Gravity.LEFT);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showChangelog() {
        if (version_changed) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
            dialog.setTitle(R.string.changelog);
            WebView changes = new WebView(mContext);
            changes.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            changes.setWebViewClient(new WebViewClient());
            changes.loadUrl("http://forum.xda-developers.com/showpost.php?p=42839595&postcount=2");
            changes.clearCache(true);
            dialog.setView(changes);
            dialog.show();
        }
    }

    private void showUsageWarning() {
        if (mDevice.isRecoverySupported() || mDevice.isKernelSupported()) {
            final AlertDialog.Builder WarningDialog = new AlertDialog.Builder(mContext);
            WarningDialog.setTitle(R.string.warning);
            WarningDialog.setMessage(String.format(getString(R.string.bak_warning),
                    PathToRecoveryBackups + " & " + PathToKernelBackups));
            WarningDialog.setPositiveButton(R.string.backup, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (mDevice.isRecoverySupported()) {
                        createBackup(true);
                    }
                    if (mDevice.isKernelSupported()) {
                        createBackup(false);
                    }
                    Common.setBooleanPref(mContext, PREF_NAME, PREF_KEY_FIRST_RUN, true);
                }
            });
            WarningDialog.setNegativeButton(R.string.risk, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Common.setBooleanPref(mContext, PREF_NAME, PREF_KEY_FIRST_RUN, true);
                }
            });
            WarningDialog.setCancelable(false);
            WarningDialog.show();
        }
    }

    public void showPopup(int Menu, final View v) {
        PopupMenu popup = new PopupMenu(mContext, v);
        popup.getMenuInflater().inflate(Menu, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                try {
                    final String text = ((TextView) v).getText().toString();

                    final Dialog dialog = new Dialog(mContext);
                    dialog.setTitle(R.string.setname);
                    dialog.setContentView(R.layout.dialog_input);
                    final Button bGo = (Button) dialog.findViewById(R.id.bGoBackup);
                    final EditText etFileName = (EditText) dialog.findViewById(R.id.etFileName);
                    final File Backup = new File(PathToRecoveryBackups, text);

                    switch (menuItem.getItemId()) {
                        case R.id.iReboot:
                            mToolbox.reboot(Toolbox.REBOOT_REBOOT);
                            return true;
                        case R.id.iRebootRecovery:
                            mToolbox.reboot(Toolbox.REBOOT_RECOVERY);
                            return true;
                        case R.id.iRebootBootloader:
                            mToolbox.reboot(Toolbox.REBOOT_BOOTLOADER);
                            return true;
                        case R.id.iRestoreRecovery:
                            FlashUtil RestoreRecoveryUtil = new FlashUtil(mShell, mContext, mDevice,
                                    new File(PathToRecoveryBackups, text), FlashUtil.JOB_RESTORE_RECOVERY);
                            RestoreRecoveryUtil.execute();
                            FlashUtils_ERRORS.addAll(RestoreRecoveryUtil.getERRORS());
                            return true;
                        case R.id.iRenameRecovery:
                            etFileName.setHint(text);
                            bGo.setOnClickListener(new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {

                                    String Name;
                                    if (etFileName.getText() != null
                                            && !etFileName.getText().toString().equals("")) {
                                        Name = etFileName.getText().toString();
                                    } else {
                                        Name = String.valueOf(etFileName.getHint());
                                    }

                                    if (!Name.endsWith(mDevice.getRecoveryExt())) {
                                        Name = Name + mDevice.getRecoveryExt();
                                    }

                                    final File renamedBackup = new File(PathToRecoveryBackups, Name);

                                    if (renamedBackup.exists()) {
                                        AppMsg
                                                .makeText(mActivity, R.string.backupalready, AppMsg.STYLE_ALERT)
                                                .show();
                                    } else {
                                        if (Backup.renameTo(renamedBackup)) {
                                            loadBackups();
                                        }
                                    }
                                    dialog.dismiss();
                                }
                            });
                            dialog.show();
                            return true;
                        case R.id.iDeleteRecoveryBackup:
                            if (((TextView) v).getText() != null) {
                                if (new File(PathToRecoveryBackups, text).delete()) {
                                    AppMsg.makeText(mActivity, String.format(mContext.getString(R.string.bak_deleted),
                                            ((TextView) v).getText()), AppMsg.STYLE_INFO).show();
                                    loadBackups();
                                }
                            }
                            return true;
                        case R.id.iRestoreKernel:
                            FlashUtil RestoreKernelUtil = new FlashUtil(mShell, mContext, mDevice, new File(PathToKernelBackups, text),
                                    FlashUtil.JOB_RESTORE_KERNEL);
                            RestoreKernelUtil.execute();
                            FlashUtils_ERRORS.addAll(RestoreKernelUtil.getERRORS());
                            return true;
                        case R.id.iRenameKernel:
                            etFileName.setHint(text);
                            bGo.setOnClickListener(new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {

                                    String Name;
                                    if (etFileName.getText() != null && !etFileName.getText().toString().equals("")) {
                                        Name = etFileName.getText().toString();
                                    } else {
                                        Name = String.valueOf(etFileName.getHint());
                                    }

                                    if (!Name.endsWith(mDevice.getKernelExt())) {
                                        Name = Name + mDevice.getKernelExt();
                                    }

                                    final File renamedBackup = new File(PathToKernelBackups, Name);

                                    if (renamedBackup.exists()) {
                                        AppMsg
                                                .makeText(mActivity, R.string.backupalready, AppMsg.STYLE_ALERT)
                                                .show();
                                    } else {
                                        if (Backup.renameTo(renamedBackup)) {
                                            loadBackups();
                                        }
                                    }
                                    dialog.dismiss();

                                }
                            });
                            dialog.show();
                            return true;
                        case R.id.iDeleteKernelBackup:
                            if (((TextView) v).getText() != null) {
                                if (new File(PathToKernelBackups, text).delete()) {
                                    AppMsg
                                            .makeText(mActivity, String.format(mContext.getString(R.string.bak_deleted),
                                            ((TextView) v).getText()), AppMsg.STYLE_INFO)
                                            .show();
                                    loadBackups();
                                }
                            }
                            return true;
                        default:
                            return false;
                    }
                } catch (Exception e) {
                    ERRORS.add(e.toString());
                    return false;
                }
            }
        });
        popup.show();
    }

    public void showOverRecoveryInstructions() {
        final AlertDialog.Builder Instructions = new AlertDialog.Builder(mContext);
        Instructions
                .setTitle(R.string.info)
                .setMessage(R.string.flash_over_recovery)
                .setPositiveButton(R.string.positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            mToolbox.reboot(Toolbox.REBOOT_RECOVERY);
                        } catch (Exception e) {
                            ERRORS.add(e.toString());
                            e.printStackTrace();
                        }
                    }
                })
                .setNeutralButton(R.string.instructions, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Dialog d = new Dialog(mContext);
                        d.setTitle(R.string.instructions);
                        TextView tv = new TextView(mContext);
                        tv.setTextSize(20);
                        tv.setText(R.string.instruction);
                        d.setContentView(tv);
                        d.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                Instructions.show();
                            }
                        });
                        d.show();
                    }
                })
                .show();
    }

    private void showDeviceNotSupportedDialog() {
        AlertDialog.Builder DeviceNotSupported = new AlertDialog.Builder(mContext);
        DeviceNotSupported.setTitle(R.string.warning);
        DeviceNotSupported.setMessage(R.string.not_supportded);
        DeviceNotSupported.setPositiveButton(R.string.report, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                report(false);
            }
        });
        DeviceNotSupported.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                exit();
            }
        });
        if (!LastLog.exists()) {
            DeviceNotSupported.setNeutralButton(R.string.sReboot, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try {
                        mToolbox.reboot(Toolbox.REBOOT_RECOVERY);
                    } catch (Exception e) {
                        ERRORS.add(e.toString());
                        e.printStackTrace();
                    }
                }
            });
        }
        DeviceNotSupported.setCancelable(BuildConfig.DEBUG);
        DeviceNotSupported.show();
    }

    private void unpackFiles() throws IOException {
        File flash_image = new File(getFilesDir(), "flash_image");
        Common.pushFileFromRAW(mContext, flash_image, R.raw.flash_image, version_changed);
        File dump_image = new File(getFilesDir(), "dump_image");
        Common.pushFileFromRAW(mContext, dump_image, R.raw.dump_image, version_changed);
        File busybox = new File(mContext.getFilesDir(), "busybox");
        Common.pushFileFromRAW(mContext, busybox, R.raw.busybox, version_changed);
        Common.pushFileFromRAW(mContext, RecoveryCollectionFile, R.raw.img_sums, version_changed);
        File PartLayoutsZip = new File(mContext.getFilesDir(), "partlayouts.zip");
        Common.pushFileFromRAW(mContext, PartLayoutsZip, R.raw.partlayouts, version_changed);
        File loki_patch = new File(mContext.getFilesDir(), "loki_patch");
        Common.pushFileFromRAW(mContext, loki_patch, R.raw.loki_patch, version_changed);
        File loki_flash = new File(mContext.getFilesDir(), "loki_flash");
        Common.pushFileFromRAW(mContext, loki_flash, R.raw.loki_flash, version_changed);
    }

    public void loadBackups() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mDevice.isRecoveryDD() || mDevice.isRecoveryMTD()) {
                    if (PathToRecoveryBackups.listFiles() != null) {
                        ArrayList<File> RecoveryBakFiles = new ArrayList<File>();
                        File FileList[] = PathToRecoveryBackups.listFiles();
                        if (FileList != null) {
                            RecoveryBakFiles.addAll(Arrays.asList(FileList));
                        }

                        ArrayList<String> RecoveryBakNames = new ArrayList<String>();

                        for (File backup : RecoveryBakFiles) {
                            RecoveryBakNames.add(backup.getName());
                        }

                        final ArrayAdapter<String> RecoveryBakAdapter = new ArrayAdapter<String>(mContext, R.layout.custom_list_item, RecoveryBakNames);

                        final ListView lvRecoveryBackups = (ListView) mActivity.findViewById(R.id.lvRecoveryBackups);
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                lvRecoveryBackups.setAdapter(RecoveryBakAdapter);
                            }
                        });
                        lvRecoveryBackups.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                            @Override
                            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                                    long arg3) {
                                showPopup(R.menu.bakmgr_recovery_menu, arg1);
                            }
                        });
                    }
                }
                if (mDevice.isKernelDD() || mDevice.isKernelMTD()) {
                    if (PathToKernelBackups.listFiles() != null) {
                        ArrayList<File> KernelBakList = new ArrayList<File>();
                        File FileList[] = PathToKernelBackups.listFiles();
                        if (FileList != null) {
                            KernelBakList.addAll(Arrays.asList(FileList));
                        }

                        ArrayList<String> KernelBakNames = new ArrayList<String>();

                        for (File backup : KernelBakList) {
                            KernelBakNames.add(backup.getName());
                        }

                        final ArrayAdapter<String> KernelBakAdapter = new ArrayAdapter<String>(mContext, R.layout.custom_list_item, KernelBakNames);

                        final ListView lvKernelBackups = (ListView) mActivity.findViewById(R.id.lvKernelBackups);

                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                lvKernelBackups.setAdapter(KernelBakAdapter);
                            }
                        });
                        lvKernelBackups.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                            @Override
                            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                                    long arg3) {
                                showPopup(R.menu.bakmgr_kernel_menu, arg1);
                            }
                        });
                    }
                }
            }
        }).start();

    }

    public void optimizeLayout() throws NullPointerException {

        if (mDevice.isRecoverySupported() || mDevice.isKernelSupported()) {
            /** If device is supported start setting up layout */
            if (getIntent().getAction().equals(Intent.ACTION_VIEW)) {
                startFlashAs();
            } else {
                setContentView(R.layout.rashr);
                mRashrLayout = (DrawerLayout) findViewById(R.id.RashrLayout);

                setupSwipeUpdater();

                setupDrawer();

                final TextView RecoveryVersion = (TextView) findViewById(R.id.tvVersion);
                RecoveryVersion.setText(mDevice.getRecoveryVersion() + "\n" + mDevice.getKernelVersion());

                AdView adView = (AdView) findViewById(R.id.adView);
                ViewGroup MainParent = (ViewGroup) adView.getParent();

                if (MainParent != null) {
                    if (!Common.getBooleanPref(mContext, PREF_NAME, PREF_KEY_ADS)) {
                        /** Removing ads if user has turned off */
                        MainParent.removeView(adView);
                    }
                    if (!mDevice.isKernelSupported()) {
                        /** If Kernel flashing is not supported remove flash options */
                        MainParent.removeView(findViewById(R.id.bFlashKernel));
                    }
                    if (!mDevice.isRecoverySupported()) {
                        /** If Recovery flashing is not supported remove flash options */
                        MainParent.removeView(findViewById(R.id.bFlashRecovery));
                    }
                }
            }
        }
    }

    public void startFlashAs() throws NullPointerException {
        /** Setting layout for flashing over external App for example File Browser */
        setContentView(R.layout.flash_as);
        RelativeLayout FlashAsLayout = (RelativeLayout) findViewById(R.layout.flash_as);
        String path;
        if ((path = getIntent().getData().getPath()) != null) {
            final File IMG = new File(path);
            if (IMG.exists()) {
                TextView tvFlashAs = (TextView) findViewById(R.id.tvFlashAs);
                tvFlashAs.setText(String.format(getString(R.string.flash_as), IMG.getName()));
            } else {
                exit();
            }
            RadioButton optAsRecovery = (RadioButton) findViewById(R.id.optAsRecovery);
            RadioButton optAsKernel = (RadioButton) findViewById(R.id.optAsKernel);

            if (!mDevice.isRecoverySupported()) {
                FlashAsLayout.removeView(optAsRecovery);
                optAsKernel.setChecked(true);
            }
            if (!mDevice.isKernelSupported()) {
                FlashAsLayout.removeView((optAsKernel));
            }
        } else {
            exit();
        }
    }

    public void setupSwipeUpdater() {
        mSwipeUpdater = (SwipeRefreshLayout) findViewById(R.id.swipe_updater);
        mSwipeUpdater.setColorScheme(R.color.custom_green,
                R.color.custom_yellow,
                R.color.custom_green,
                android.R.color.darker_gray);
        mSwipeUpdater.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                final int img_count =
                        mDevice.getCWMVersions().size() + mDevice.getTWRPVersions().size()
                                + mDevice.getPHILZVersions().size();
                Downloader updater = new Downloader(mContext, "http://dslnexus.de/Android/",
                        "img_sums", RecoveryCollectionFile, new Runnable() {
                    @Override
                    public void run() {
                        mDevice.loadRecoveryList();
                    }
                }
                );
                updater.setOverrideFile(true);
                updater.setRetry(true);
                updater.setHidden(true);

                AppMsg
                        .makeText(mActivity, R.string.refresh_list, AppMsg.STYLE_INFO)
                        .show();
                updater.setAfterDownload(new Runnable() {
                    @Override
                    public void run() {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                mDevice.loadRecoveryList();
                                mSwipeUpdater.setRefreshing(false);
                                final int new_img_count =
                                        img_count - (mDevice.getCWMVersions().size()
                                                + mDevice.getTWRPVersions().size()
                                                + mDevice.getPHILZVersions().size());
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        AppMsg.Style style;
                                        if (new_img_count > 0) {
                                            style = AppMsg.STYLE_INFO;
                                        } else {
                                            style = AppMsg.STYLE_CONFIRM;
                                        }
                                        AppMsg
                                                .makeText(mActivity, String.format(getString(R.string.new_imgs_loaded), new_img_count), style)
                                                .show();
                                    }
                                });
                            }
                        }).start();
                    }
                });
                updater.execute();
            }
        });
    }

    public void setupDrawer() {
        LayoutInflater layoutInflater = getLayoutInflater();

        mRashrLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(mActivity,
                mRashrLayout, R.drawable.ic_navigation_drawer, R.string.settings, R.string.app_name);
        mRashrLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        setupMenuDrawer(layoutInflater);
        setupBackupDrawer(layoutInflater);
    }

    public void setupMenuDrawer(LayoutInflater layoutInflater) throws NullPointerException {
        DrawerLayout mMenuDrawer =
                (DrawerLayout) layoutInflater.inflate(R.layout.menu_drawer, mRashrLayout, true);
        if (mMenuDrawer != null) {
            Spinner spStyle = (Spinner) mMenuDrawer.findViewById(R.id.spStyle);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(mActivity,
                    R.layout.custom_list_item, getResources().getStringArray(R.array.styles));
            adapter.setDropDownViewResource(R.layout.custom_list_item);

            spStyle.setAdapter(adapter);
            spStyle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    switch (position) {
                        case 1:
                            Common.setIntegerPref(mContext, PREF_NAME, PREF_STYLE, APPCOMPAT_LIGHT_DARK_BAR);
                            restartActivity();
                            break;
                        case 2:
                            Common.setIntegerPref(mContext, PREF_NAME, PREF_STYLE, APPCOMPAT_LIGHT);
                            restartActivity();
                            break;
                        case 3:
                            Common.setIntegerPref(mContext, PREF_NAME, PREF_STYLE, APPCOMPAT_DARK);
                            restartActivity();
                            break;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            CheckBox cbShowAds = (CheckBox) mMenuDrawer.findViewById(R.id.cbShowAds);
            CheckBox cbLog = (CheckBox) mMenuDrawer.findViewById(R.id.cbLog);

            cbShowAds.setChecked(Common.getBooleanPref(mContext, PREF_NAME, PREF_KEY_ADS));
            cbLog.setChecked(Common.getBooleanPref(mContext, Shell.PREF_NAME, Shell.PREF_LOG));
            if (cbLog.isChecked()) {
                findViewById(R.id.bShowLogs).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.bShowLogs).setVisibility(View.INVISIBLE);
            }
            cbShowAds.setChecked(Common.getBooleanPref(mContext, PREF_NAME, PREF_KEY_ADS));

        }
    }

    public void setupBackupDrawer(LayoutInflater layoutInflater) throws NullPointerException {
        DrawerLayout mBackupDrawer =
                (DrawerLayout) layoutInflater.inflate(R.layout.backup_drawer, mRashrLayout, true);
        if (mBackupDrawer != null) {
            ViewGroup BackupDrawerParent = (ViewGroup) mBackupDrawer.getParent();
            if (mDevice.isRecoveryOverRecovery()) {
                BackupDrawerParent.removeView(mBackupDrawer);
            } else {
                View createKernelBackup = findViewById(R.id.bCreateKernelBackup);
                View kernelBackups = findViewById(R.id.lvKernelBackups);
                View createRecoveryBackup = findViewById(R.id.bCreateRecoveryBackup);
                View recoveryBackups = findViewById(R.id.lvRecoveryBackups);
                if (!mDevice.isKernelSupported()) {
                    /** If Kernel flashing is not supported remove backup views */
                    ((ViewGroup) createKernelBackup.getParent()).removeView(createKernelBackup);
                    ((ViewGroup) kernelBackups.getParent()).removeView(kernelBackups);
                }
                if (!mDevice.isRecoverySupported()) {
                    /** If Recovery flashing is not supported remove backup views */
                    ((ViewGroup) createRecoveryBackup.getParent()).removeView(createRecoveryBackup);
                    ((ViewGroup) recoveryBackups.getParent()).removeView(recoveryBackups);
                }
            }
            loadBackups();
        }
    }

    private void restartActivity() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    public void exit() {
        finish();
        System.exit(0);
    }

    /**
     * @return All Preferences as String
     */
    public String getAllPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String Prefs = "";
        Map<String,?> prefsMap = prefs.getAll();
        for (Map.Entry<String, ?> entry : prefsMap.entrySet()) {
            /**
             * Skip following Prefs (PREF_KEY_KERNEL_HISTORY, PREF_KEY_RECOVERY_HISTORY ...)
             */
            try {
                if (!entry.getKey().contains(PREF_KEY_KERNEL_HISTORY)
                        && !entry.getKey().contains(PREF_KEY_RECOVERY_HISTORY)
                        && !entry.getKey().contains(FlashUtil.PREF_KEY_FLASH_KERNEL_COUNTER)
                        && !entry.getKey().contains(FlashUtil.PREF_KEY_FLASH_RECOVERY_COUNTER)) {
                    Prefs += entry.getKey() + ": " + entry.getValue().toString() + "\n";
                }
            } catch (NullPointerException e) {
                ERRORS.add(e.toString());
            }
        }
        return Prefs;
    }
}