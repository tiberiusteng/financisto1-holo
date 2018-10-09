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
package ru.orangesoftware.financisto.activity;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.PinProtection;
import ru.orangesoftware.financisto.view.PinView;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;

public class PinActivity extends Activity implements PinView.PinListener {
	
	public static final String SUCCESS = "PIN_SUCCESS";

	private FingerprintManager fingerprintManager;
	private CancellationSignal cancelFingerprintCheck;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String pin = MyPreferences.getPin(this);

		if (pin == null) {
			onSuccess(null);
		} else {
			PinView v = new PinView(this, this, pin, R.layout.lock);

			if (Build.VERSION.SDK_INT >= 23) {
				class FingerprintCallback extends FingerprintManager.AuthenticationCallback {
					private PinView.PinListener listener;
					public FingerprintCallback(PinView.PinListener pListener) {
						this.listener = pListener;
					}
					@Override
					public void onAuthenticationSucceeded(
							FingerprintManager.AuthenticationResult result)
					{
						this.listener.onSuccess(null);
					}
				}

				fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
				cancelFingerprintCheck = new CancellationSignal();

				if ((checkSelfPermission(Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED) &&
						fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints()) {
					fingerprintManager.authenticate(null, cancelFingerprintCheck, 0, new FingerprintCallback(this), null);
				}
			}

			setContentView(v.getView());
		}
	}

	@Override
	public void onConfirm(String pinBase64) {		
	}

	@Override
	public void onSuccess(String pinBase64) {
		if (cancelFingerprintCheck != null) cancelFingerprintCheck.cancel();
        PinProtection.pinUnlock(this);
		Intent data = new Intent();
		data.putExtra(SUCCESS, true);
		setResult(RESULT_OK, data);
		finish();
	}

	@Override
	public void onBackPressed() {
		if (cancelFingerprintCheck != null) cancelFingerprintCheck.cancel();
        moveTaskToBack(true);
	}

}
