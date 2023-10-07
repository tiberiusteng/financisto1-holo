package ru.orangesoftware.financisto.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.MyPreferences;

public class SplitsBlotterActivity extends AppCompatActivity {
	public SplitsBlotterActivity() {
		super(R.layout.fragment_container);
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(MyPreferences.switchLocale(base));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		Bundle args = null;
		if (intent != null) {
			args = intent.getExtras();
		}
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.setReorderingAllowed(true)
					.add(R.id.fragment_container_view, SplitsBlotterFragment.class, args)
					.commit();
		}
	}
}
