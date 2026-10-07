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
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;

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

	private boolean supportAliases;
	private boolean supportColor;

	protected MyEntityActivity(Class<T> clazz) {
		this(clazz, false, false);
	}

	protected MyEntityActivity(Class<T> clazz, boolean supportAliases) {
		this(clazz, supportAliases, false);
	}

	protected MyEntityActivity(Class<T> clazz, boolean supportAliases, boolean supportColor) {
		try {
			this.clazz = clazz;
			this.supportAliases = supportAliases;
			this.supportColor = supportColor;
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

		View editActive = findViewById(R.id.editActive);
		View create = findViewById(R.id.create);

		EditText color = findViewById(R.id.color);
		View colorPreview = findViewById(R.id.color_preview);
		ImageButton palette = findViewById(R.id.palette);

		editActive.setVisibility(View.GONE);
		create.setVisibility(View.VISIBLE);

		if (!supportAliases) {
			findViewById(R.id.aliases_block).setVisibility(View.GONE);
		}

		if (!supportColor) {
			findViewById(R.id.color_block).setVisibility(View.GONE);
		}
		else {
			color.addTextChangedListener(new TextWatcher() {
				@Override
				public void afterTextChanged(Editable s) {
					int color = 0;
					try {
						color = Color.parseColor(s.toString());
					} catch (Exception e) {
						// pass
					}
					colorPreview.setBackground(new ColorDrawable(color));
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {

				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {

				}
			});

			palette.setOnClickListener(v -> {
				String[] colors = {
					"#3d414c", "#7b0e0e", "#7b450e", "#7b7b0e", "#457b0e", "#0e7b0e", "#0e7b45",
					"#0e7b7b", "#0e457b", "#0e0e7b", "#440e7b", "#7b0e7b", "#7b0e44"
				};
				var adapter = new ArrayAdapter<>(this, R.layout.select_entry_color_row, colors)
				{
					@Override
					public View getView(int position, View convertView,	ViewGroup parent) {
						View v;
						final var inflater = LayoutInflater.from(getContext());
						if (convertView == null) {
							convertView = inflater.inflate(R.layout.select_entry_color_row, parent, false);
							v = convertView.findViewById(R.id.color_patch);
							convertView.setTag(v);
						}
						else {
							v = (View) convertView.getTag();
						}
						v.setBackground(new ColorDrawable(Color.parseColor(colors[position])));
						return convertView;
					}
				};
				var builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.select_color)
						.setAdapter(adapter, (dialog, which) -> {
							color.setText(colors[which]);
							dialog.cancel();
						})
						.create()
						.show();
			});
		}

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
			preUpdateEntity(entity);
			long id = db.saveOrUpdate(entity);
			updateEntity(entity);
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
				editActive.setVisibility(View.VISIBLE);
				create.setVisibility(View.GONE);
				entity = db.load(clazz, id);
				editEntity();
			}
		}

	}

	protected String getColor(T entity) {
		return null;
	}

	protected String getAliases(T entity) {
		return null;
	}

	protected void preUpdateEntity(T entity) {
		// do nothing
	}

	protected void updateEntity(T entity) {
		// do nothing
	}

	private void editEntity() {
		EditText title = findViewById(R.id.title);
		title.setText(entity.title);

		if (supportAliases) {
			EditText aliases = findViewById(R.id.aliases);
			aliases.setText(getAliases(entity));
		}
		if (supportColor) {
			EditText color = findViewById(R.id.color);
			color.setText(getColor(entity));
		}

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
