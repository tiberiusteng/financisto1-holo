package tw.tib.financisto.utils;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK;

import android.content.Context;

import androidx.biometric.BiometricManager;

import tw.tib.financisto.R;

public class FingerprintUtils {

    public static boolean fingerprintUnavailable(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        return biometricManager.canAuthenticate(BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static String reasonWhyFingerprintUnavailable(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);

        switch (biometricManager.canAuthenticate(BIOMETRIC_WEAK)) {
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                return context.getString(R.string.fingerprint_unavailable_hardware);
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                return context.getString(R.string.fingerprint_unavailable_enrolled_fingerprints);
            default:
                return context.getString(R.string.fingerprint_unavailable_unknown);
        }
    }

}
