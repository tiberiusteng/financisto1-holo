/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package tw.tib.financisto.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import tw.tib.financisto.R;
import tw.tib.financisto.utils.PinProtection;

public abstract class AbstractImportActivity extends AppCompatActivity {

    public static final int IMPORT_FILENAME_REQUESTCODE = 0xff;

    private final int layoutId;
    protected ImageButton bBrowse;
    protected EditText edFilename;
    protected Uri importFileUri;

    public AbstractImportActivity(int layoutId) {
        this.layoutId = layoutId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layoutId);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            var lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.topMargin = insets.top;
            lp.bottomMargin = insets.bottom;
            v.setLayoutParams(lp);
            return WindowInsetsCompat.CONSUMED;
        });

        bBrowse = findViewById(R.id.btn_browse);
        bBrowse.setOnClickListener(v -> openFile());
        edFilename = findViewById(R.id.edFilename);

        internalOnCreate();
    }

    protected void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        intent.setType("*/*");
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, importFileUri);

        try {
            startActivityForResult(intent, IMPORT_FILENAME_REQUESTCODE);
        } catch (ActivityNotFoundException e) {
            // No compatible file manager was found.
            Toast.makeText(this, R.string.no_filemanager_installed, Toast.LENGTH_SHORT).show();
        }

    }

    protected abstract void internalOnCreate();

    protected abstract void updateResultIntentFromUi(Intent data);

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMPORT_FILENAME_REQUESTCODE) {
            if (resultCode == RESULT_OK && data != null) {
                importFileUri = data.getData();
                getContentResolver().takePersistableUriPermission(importFileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (importFileUri != null) {
                    String filePath = importFileUri.getPath();
                    if (filePath != null) {
                        edFilename.setText(filePath.substring(filePath.lastIndexOf("/") + 1));
                        savePreferences();
                    }
                }
            }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        PinProtection.lock(this);
        savePreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PinProtection.unlock(this);
        restorePreferences();
    }

    protected abstract void savePreferences();

    protected abstract void restorePreferences();

}
