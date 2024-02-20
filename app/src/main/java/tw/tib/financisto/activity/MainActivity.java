package tw.tib.financisto.activity;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;

import tw.tib.financisto.R;
import tw.tib.financisto.activity.MenuListFragment_;
import tw.tib.financisto.bus.GreenRobotBus;
import tw.tib.financisto.bus.RefreshCurrentTab;
import tw.tib.financisto.bus.SwitchToMenuTabEvent;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.db.DatabaseHelper;
import tw.tib.financisto.dialog.WebViewDialog;
import tw.tib.financisto.bus.GreenRobotBus_;
import tw.tib.financisto.utils.CurrencyCache;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.PinProtection;

public class MainActivity extends AppCompatActivity {
    private GreenRobotBus greenRobotBus;
    private Fragment fragments[];
    HashMap<String, TabLayout.Tab> tabs;
    private TabLayout tabLayout;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (MyPreferences.isSecureWindow(this)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        greenRobotBus = GreenRobotBus_.getInstance_(this);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.main2);

        initialLoad();

        tabLayout = findViewById(R.id.tabs);
        ViewPager2 viewPager = findViewById(R.id.viewpager);

        viewPager.setUserInputEnabled(false);

        fragments = new Fragment[]{
                new AccountListFragment(),
                new BlotterFragment(true),
                new BudgetListFragment(),
                new ReportsListFragment(),
                new MenuListFragment_()
        };
        tabs = new HashMap<>();

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return fragments[position];
            }

            @Override
            public int getItemCount() {
                return fragments.length;
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                refreshCurrentTab();
            }
        });

        new TabLayoutMediator(tabLayout, viewPager, true, false,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab/*.setText(getString(R.string.accounts))*/
                                    .setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_tab_accounts, getTheme()));
                            tabs.put("accounts", tab);
                            break;
                        case 1:
                            tab/*.setText(getString(R.string.blotter))*/
                                    .setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_tab_blotter, getTheme()));
                            tabs.put("blotter", tab);
                            break;
                        case 2:
                            tab/*.setText(getString(R.string.budgets))*/
                                    .setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_tab_budgets, getTheme()));
                            tabs.put("budgets", tab);
                            break;
                        case 3:
                            tab/*.setText(getString(R.string.reports))*/
                                    .setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_tab_reports, getTheme()));
                            tabs.put("reports", tab);
                            break;
                        case 4:
                            tab/*.setText(getString(R.string.menu))*/
                                    .setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_tab_menu, getTheme()));
                            tabs.put("menu", tab);
                            break;
                    }
                }).attach();

        viewPager.setCurrentItem(MyPreferences.getStartupScreen(this).ordinal(), false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        greenRobotBus.register(this);
        PinProtection.unlock(this);
        if (PinProtection.isUnlocked()) {
            WebViewDialog.checkVersionAndShowWhatsNewIfNeeded(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        greenRobotBus.unregister(this);
        PinProtection.lock(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PinProtection.immediateLock(this);
    }

    private void initialLoad() {
        long t3, t2, t1, t0 = System.currentTimeMillis();
        DatabaseAdapter db = new DatabaseAdapter(this);
        db.open();
        try {
            SQLiteDatabase x = db.db();
            x.beginTransaction();
            t1 = System.currentTimeMillis();
            try {
                updateFieldInTable(x, DatabaseHelper.CATEGORY_TABLE, 0, "title", getString(R.string.no_category));
                updateFieldInTable(x, DatabaseHelper.CATEGORY_TABLE, -1, "title", getString(R.string.split));
                updateFieldInTable(x, DatabaseHelper.PROJECT_TABLE, 0, "title", getString(R.string.no_project));
                updateFieldInTable(x, DatabaseHelper.LOCATIONS_TABLE, 0, "title", getString(R.string.current_location));
                x.setTransactionSuccessful();
            } finally {
                x.endTransaction();
            }
            t2 = System.currentTimeMillis();
            if (MyPreferences.shouldUpdateHomeCurrency(this)) {
                db.setDefaultHomeCurrency();
            }
            CurrencyCache.initialize(db);
            t3 = System.currentTimeMillis();
            if (MyPreferences.shouldRebuildRunningBalance(this)) {
                db.rebuildRunningBalances();
            }
            if (MyPreferences.shouldUpdateAccountsLastTransactionDate(this)) {
                db.updateAccountsLastTransactionDate();
            }
        } finally {
            db.close();
        }
        long t4 = System.currentTimeMillis();
        Log.d(getLocalClassName(), "Load time = " + (t4 - t0) + "ms = " + (t2 - t1) + "ms+" + (t3 - t2) + "ms+" + (t4 - t3) + "ms");
    }

    public void refreshCurrentTab() {
        if (fragments[tabLayout.getSelectedTabPosition()] instanceof RefreshSupportedActivity) {
            Fragment f = fragments[tabLayout.getSelectedTabPosition()];
            if (f.isAdded()) {
                RefreshSupportedActivity r = (RefreshSupportedActivity) f;
                r.recreateCursor();
                r.integrityCheck();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSwitchToMenuTab(SwitchToMenuTabEvent event) {
        TabLayout.Tab tab = tabs.get("menu");
        if (tab != null) {
            tabLayout.selectTab(tab);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshCurrentTab(RefreshCurrentTab e) {
        refreshCurrentTab();
    }

    private void updateFieldInTable(SQLiteDatabase db, String table, long id, String field, String value) {
        db.execSQL("update " + table + " set " + field + "=? where _id=?", new Object[]{value, id});
    }
}
