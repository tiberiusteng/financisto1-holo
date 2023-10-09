package tw.tib.financisto.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import tw.tib.financisto.R;
import tw.tib.financisto.utils.MyPreferences;

public class TemplatesListActivity extends AppCompatActivity {
	public TemplatesListActivity() {
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
					.add(R.id.fragment_container_view, TemplatesListFragment.class, args)
					.commit();
		}
	}
}
