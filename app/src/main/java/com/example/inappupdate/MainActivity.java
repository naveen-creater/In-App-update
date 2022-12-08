package com.example.inappupdate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.ActivityResult;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

import static com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE;
import static com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE;

public class MainActivity extends AppCompatActivity {

    private static final int IN_APP_REQUEST_CODE = 530;
    private AppUpdateManager appUpdateManager = null;
    private int updateType = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        inAppUpdate(FLEXIBLE);  //optional update
        inAppUpdate(IMMEDIATE); // manditary update
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == IN_APP_REQUEST_CODE){
            if (resultCode == RESULT_OK) {
                System.out.println("InApp: Update sucess: " + resultCode);
            }
            if (resultCode != RESULT_OK) {
                System.out.println("InApp:Update flow failed! Result code: " + resultCode);
                // If the update is cancelled or fails,
                // you can request to start the update again.
            }

            if (resultCode == RESULT_CANCELED) {
                System.out.println("InApp:Update Cancelled by User: " + resultCode);
            }

            if (resultCode == ActivityResult.RESULT_IN_APP_UPDATE_FAILED) {
                System.out.println("InApp:Some other error prevented either the user from providing consent or the update from proceeding.: " + resultCode);
            }

        }
    }

    private void inAppUpdate(final int updateType){
        appUpdateManager = AppUpdateManagerFactory.create(this);

        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.progress_horizondal);

        final ProgressBar progressBar = (ProgressBar) dialog.findViewById(R.id.progressbarPredict);

        final InstallStateUpdatedListener listener = new InstallStateUpdatedListener() {
            @Override
            public void onStateUpdate(@NonNull InstallState state) {
                // (Optional) Provide a download progress bar.
                if (state.installStatus() == InstallStatus.DOWNLOADING) {
                    long bytesDownloaded = state.bytesDownloaded();
                    long totalBytesToDownload = state.totalBytesToDownload();
                    // Implement progress bar.
                    progressBar.setMax((int)totalBytesToDownload);

                    System.out.println("InApp:Download in progresse!!: "+ bytesDownloaded+"    "+totalBytesToDownload);



                    progressBar.setProgress((int)bytesDownloaded);


                    dialog.show();

                    Window window = dialog.getWindow();
                    window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                }
                // Log state or install the update.

                if (state.installStatus() == InstallStatus.DOWNLOADED) {
                    // After the update is downloaded, show a notification
                    // and request user confirmation to restart the app.
                    dialog.dismiss();
                    popupSnackbarForCompleteUpdate(appUpdateManager);
                    System.out.println("InApp:Download is done!!");
                }
            }
        };

        appUpdateInfoTask.addOnSuccessListener(new OnSuccessListener<AppUpdateInfo>() {
            @Override
            public void onSuccess(AppUpdateInfo appUpdateInfo) {

                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        // This example applies an immediate update. To apply a flexible update
                        // instead, pass in AppUpdateType.FLEXIBLE
                        && appUpdateInfo.isUpdateTypeAllowed(updateType)) {
                    // Request the update.

                    System.out.println("InApp:Update Available!!");

                    try {
                        if(updateType == IMMEDIATE){
                            appUpdateManager.startUpdateFlowForResult(appUpdateInfo, IMMEDIATE,
                                    MainActivity.this, IN_APP_REQUEST_CODE);
                        }else if(updateType == FLEXIBLE){
                            appUpdateManager.registerListener(listener);

                            appUpdateManager.startUpdateFlowForResult(appUpdateInfo, updateType,
                                    MainActivity.this, IN_APP_REQUEST_CODE);

                        }

                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
    }

    private void popupSnackbarForCompleteUpdate(final AppUpdateManager appUpdateManager) {
        /*TextView textView = new TextView(this);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        Snackbar snackbar =
                Snackbar.make(
                        textView,
                        "An update has just been downloaded.",
                        Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("RESTART", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                appUpdateManager.completeUpdate();
            }
        });
        snackbar.setActionTextColor(Color.YELLOW);
        snackbar.show();*/


//        DialogMSF.alertDialog(this, 0,getString(DIALOG_APP_NAME),"An update has just been downloaded. Please RESTART!", getString(DIALOG_OK),
//                true, new DialogMSF.DialogListenerMSF() {
//
//                    @Override
//                    public void alertDialogAction(DialogMSF.Action action, Object... data) {
//                        if(action == DialogMSF.Action.OK) {
//                            appUpdateManager.completeUpdate();
//                        }else{
//                            appUpdateManager.completeUpdate();
//                        }
//                    }
//                }, isDark());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(updateType == FLEXIBLE){
            appUpdateManager
                    .getAppUpdateInfo()
                    .addOnSuccessListener(new OnSuccessListener<AppUpdateInfo>() {
                        @Override
                        public void onSuccess(AppUpdateInfo appUpdateInfo) {
                            // If the update is downloaded but not installed,
                            // notify the user to complete the update.
                            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                                System.out.println("InApp: OnResume popup for after download flexmode");
                                popupSnackbarForCompleteUpdate(appUpdateManager);
                            }
                        }
                    });
        }

        if(updateType == IMMEDIATE){
            appUpdateManager
                    .getAppUpdateInfo()
                    .addOnSuccessListener(
                            new OnSuccessListener<AppUpdateInfo>() {
                                @Override
                                public void onSuccess(AppUpdateInfo appUpdateInfo) {
                                    if (appUpdateInfo.updateAvailability()
                                            == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                                        // If an in-app update is already running, resume the update.
                                        try {
                                            appUpdateManager.startUpdateFlowForResult(
                                                    appUpdateInfo,
                                                    IMMEDIATE,
                                                    MainActivity.this,
                                                    IN_APP_REQUEST_CODE);
                                        } catch (IntentSender.SendIntentException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });
        }

    }
}