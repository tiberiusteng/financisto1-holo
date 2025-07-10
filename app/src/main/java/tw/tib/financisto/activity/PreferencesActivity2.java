package tw.tib.financisto.activity;

import static java.lang.String.format;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import tw.tib.financisto.R;
import tw.tib.financisto.utils.MyPreferences;

public class PreferencesActivity2 extends AppCompatActivity {
    public PreferencesActivity2() {
        super(R.layout.fragment_container);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        var toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setVisibility(View.VISIBLE);
            setSupportActionBar(toolbar);

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                if (v.getPaddingTop() == 0) {
                    var lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    lp.height += insets.top;
                    v.setPadding(0, insets.top, 0, 0);
                    v.setLayoutParams(lp);
                }
                return WindowInsetsCompat.CONSUMED;
            });
        }

        if (MyPreferences.isSecureWindow(this)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        Intent intent = getIntent();
        Bundle args = null;
        if (intent != null) {
            args = intent.getExtras();
        }
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.fragment_container_view, PreferenceFragment.class, args)
                    .commit();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
