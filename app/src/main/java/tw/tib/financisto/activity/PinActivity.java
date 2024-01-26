/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package tw.tib.financisto.activity;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK;
import static androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

import tw.tib.financisto.R;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.PinProtection;
import tw.tib.financisto.view.PinView;

public class PinActivity extends AppCompatActivity implements PinView.PinListener {

    public static final String SUCCESS = "PIN_SUCCESS";

    private final Handler handler = new Handler();

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String pin = MyPreferences.getPin(this);
        BiometricManager biometricManager = BiometricManager.from(this);

        if (pin == null) {
            onSuccess(null);
        } else if (biometricManager.canAuthenticate(BIOMETRIC_WEAK) == BIOMETRIC_SUCCESS
                && MyPreferences.isPinLockUseFingerprint(this))
        {
            setContentView(R.layout.lock_fingerprint);
            findViewById(R.id.try_biometric_again).setOnClickListener(v -> askForFingerprint());
            askForFingerprint();
        } else {
            usePinLock();
        }

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });
    }

    private void usePinLock() {
        String pin = MyPreferences.getPin(this);
        PinView v = new PinView(this, this, pin, R.layout.lock);
        setContentView(v.getView());
    }

    private void askForFingerprint() {
        View usePinButton = findViewById(R.id.use_pin);
        View tryBiometricAgain = findViewById(R.id.try_biometric_again);
        tryBiometricAgain.setVisibility(View.INVISIBLE);
        if (MyPreferences.isUseFingerprintFallbackToPinEnabled(this)) {
            usePinButton.setOnClickListener(v -> {
                usePinLock();
            });
        } else {
            usePinButton.setVisibility(View.GONE);
        }
        Executor executor = ContextCompat.getMainExecutor(this);

        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                setFingerprintStatus(R.string.fingerprint_auth_failed, R.drawable.ic_error_black_48dp, R.color.material_orange);
                tryBiometricAgain.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                setFingerprintStatus(R.string.fingerprint_auth_success, R.drawable.ic_check_circle_black_48dp, R.color.material_teal);
                handler.postDelayed(() -> onSuccess(null), 200);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                setFingerprintStatus(R.string.fingerprint_auth_failed, R.drawable.ic_error_black_48dp, R.color.material_orange);
                tryBiometricAgain.setVisibility(View.VISIBLE);
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.fingerprint_description))
                .setNegativeButtonText(getString(R.string.cancel))
                .setAllowedAuthenticators(BIOMETRIC_WEAK)
                .setConfirmationRequired(false)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void setFingerprintStatus(int messageResId, int iconResId, int colorResId) {
        TextView status = findViewById(R.id.fingerprint_status);
        ImageView icon = findViewById(R.id.fingerprint_icon);
        int color = getResources().getColor(colorResId);
        status.setText(messageResId);
        status.setTextColor(color);
        icon.setImageResource(iconResId);
        icon.setColorFilter(color);
    }

    @Override
    public void onConfirm(String pinBase64) {
    }

    @Override
    public void onSuccess(String pinBase64) {
        PinProtection.pinUnlock(this);
        Intent data = new Intent();
        data.putExtra(SUCCESS, true);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= 33) {
            super.onBackPressed();
        }
        else {
            moveTaskToBack(true);
        }
    }

}
