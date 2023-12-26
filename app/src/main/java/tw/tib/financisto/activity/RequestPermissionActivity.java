package tw.tib.financisto.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.SwitchCompat;

import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.ViewById;

import tw.tib.financisto.R;
import tw.tib.financisto.utils.MyPreferences;

@EActivity(R.layout.activity_request_permissions)
public class RequestPermissionActivity extends Activity {

    @Extra("requestedPermission")
    String requestedPermission;

    @ViewById(R.id.toggleCameraWrap)
    ViewGroup toggleCameraWrap;

    @ViewById(R.id.toggleCamera)
    SwitchCompat toggleCamera;

    @ViewById(R.id.toggleSmsWrap)
    ViewGroup toggleSmsWrap;

    @ViewById(R.id.toggleSms)
    SwitchCompat toggleSms;

    @ViewById(R.id.toggleNotificationWrap)
    ViewGroup toggleNotificationWrap;

    @ViewById(R.id.toggleNotification)
    SwitchCompat toggleNotification;

    @ViewById(R.id.toggleNotificationListenerWrap)
    ViewGroup toggleNotificationListenerWrap;

    @ViewById(R.id.toggleNotificationListener)
    SwitchCompat toggleNotificationListener;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @AfterViews
    public void initViews() {
        checkPermissions();
    }

    private void checkPermissions() {
        // using scoped storage, write external storage permission is not needed

        // camera is not used, sms permission not obtainable with google play install
        //disableToggleIfGranted(Manifest.permission.CAMERA, toggleCamera, toggleCameraWrap);
        //disableToggleIfGranted(Manifest.permission.RECEIVE_SMS, toggleSms, toggleSmsWrap);
        toggleCameraWrap.setVisibility(View.GONE);
        toggleSmsWrap.setVisibility(View.GONE);

        disableToggleIfGranted(Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE, toggleNotificationListener, toggleNotificationListenerWrap);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            disableToggleIfGranted(Manifest.permission.POST_NOTIFICATIONS, toggleNotification, toggleNotificationWrap);
        }
        else {
            toggleNotificationWrap.setVisibility(View.GONE);
        }
    }

    private void disableToggleIfGranted(String permission, CompoundButton toggleButton, ViewGroup wrapLayout) {
        if (isGranted(permission)) {
            toggleButton.setChecked(true);
            toggleButton.setEnabled(false);
            wrapLayout.setBackgroundResource(0);
        } else if (permission.equals(requestedPermission)) {
            wrapLayout.setBackgroundResource(R.drawable.highlight_border);
        }
    }

    @Click(R.id.toggleCamera)
    public void onGrantCamera() {
        requestPermission(Manifest.permission.CAMERA, toggleCamera);
    }

    @Click(R.id.toggleSms)
    public void onGrantSms() {
        requestPermission(Manifest.permission.RECEIVE_SMS, toggleSms);
    }

    @Click(R.id.toggleNotification)
    public void onGrantNotification() {
        requestPermission(Manifest.permission.POST_NOTIFICATIONS, toggleNotification);
    }

    @Click(R.id.toggleNotificationListener)
    public void onGrantNotificationListener() {
        requestPermission(Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE, toggleNotificationListener);
    }

    private void requestPermission(String permission, CompoundButton toggleButton) {
        toggleButton.setChecked(false);
        if (permission.equals(Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE)) {
            startActivityForResult(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), 0);
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 0);
        }
    }

    private boolean isGranted(String permission) {
        if (permission.equals(Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE)) {
            return NotificationManagerCompat.getEnabledListenerPackages(this).contains(getPackageName());
        }

        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        checkPermissions();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        checkPermissions();
    }
}