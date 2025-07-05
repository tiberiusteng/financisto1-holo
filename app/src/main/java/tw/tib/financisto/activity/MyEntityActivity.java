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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.db.DatabaseHelper;
import tw.tib.financisto.model.MyEntity;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.PinProtection;

public abstract class MyEntityActivity<T extends MyEntity> extends Activity {

	public static final String ENTITY_ID_EXTRA = "entityId";

	private final Class<T> clazz;

	private DatabaseAdapter db;

	private T entity;

	protected MyEntityActivity(Class<T> clazz) {
		try {
			this.clazz = clazz;
			this.entity = clazz.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(MyPreferences.switchLocale(base));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_entity);

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.edit_entity), (v, windowInsets) -> {
			Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
					| WindowInsetsCompat.Type.statusBars()
					| WindowInsetsCompat.Type.captionBar());
			var lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
			lp.topMargin = insets.top;
			lp.bottomMargin = insets.bottom;
			v.setLayoutParams(lp);
			return WindowInsetsCompat.CONSUMED;
		});

		CheckBox activityCheckBox = findViewById(R.id.isActive);
		activityCheckBox.setChecked(true);

		db = new DatabaseAdapter(this);
		db.open();

		Button bOK = findViewById(R.id.bOK);
		bOK.setOnClickListener(arg0 -> {
			EditText title = findViewById(R.id.title);
			entity.title = title.getText().toString();
			entity.isActive = activityCheckBox.isChecked();
			updateEntity(entity);
			long id = db.saveOrUpdate(entity);
			Intent intent = new Intent();
			intent.putExtra(DatabaseHelper.EntityColumns.ID, id);
			setResult(RESULT_OK, intent);
			finish();
		});

		Button bCancel = findViewById(R.id.bCancel);
		bCancel.setOnClickListener(arg0 -> {
			setResult(RESULT_CANCELED);
			finish();
		});

		Intent intent = getIntent();
		if (intent != null) {
			long id = intent.getLongExtra(ENTITY_ID_EXTRA, -1);
			if (id != -1) {
				View editActive = findViewById(R.id.editActive);
				View create = findViewById(R.id.create);
				editActive.setVisibility(View.VISIBLE);
				create.setVisibility(View.GONE);
				entity = db.load(clazz, id);
				editEntity();
			}
		}

	}

	protected void updateEntity(T entity) {
		// do nothing
	}

	private void editEntity() {
		EditText title = findViewById(R.id.title);
		title.setText(entity.title);

		CheckBox activityCheckBox = findViewById(R.id.isActive);
		activityCheckBox.setChecked(entity.isActive);
	}

	@Override
	protected void onPause() {
		super.onPause();
		PinProtection.lock(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		PinProtection.unlock(this);
	}
}
