package ru.orangesoftware.financisto.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
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

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.MyPreferences;

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

        disableToggleIfGranted(Manifest.permission.CAMERA, toggleCamera, toggleCameraWrap);
        disableToggleIfGranted(Manifest.permission.RECEIVE_SMS, toggleSms, toggleSmsWrap);

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

    private void requestPermission(String permission, CompoundButton toggleButton) {
        toggleButton.setChecked(false);
        ActivityCompat.requestPermissions(this, new String[]{permission}, 0);
    }

    private boolean isGranted(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        checkPermissions();
    }

}