package tw.tib.financisto.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import tw.tib.financisto.R;
import tw.tib.financisto.adapter.BlotterListAdapter;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.Category;
import tw.tib.financisto.model.CategoryTree;
import tw.tib.financisto.model.CategoryTreeNavigator;
import tw.tib.financisto.utils.MenuItemInfo;
import tw.tib.financisto.utils.MyPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CategorySelectorActivity extends AbstractListActivity<Cursor> {

    public static final String SELECTED_CATEGORY_ID = "SELECTED_CATEGORY_ID";
    public static final String SELECTED_ACCOUNT_ID = "SELECTED_ACCOUNT_ID";
    public static final String EXCLUDED_SUB_TREE_ID = "EXCLUDED_SUB_TREE_ID";
    public static final String INCLUDE_SPLIT_CATEGORY = "INCLUDE_SPLIT_CATEGORY";

    public static final long NO_SELECTED_ACCOUNT = Long.MIN_VALUE;

    private int incomeColor;
    private int expenseColor;

    private CategoryTreeNavigator navigator;
    private Map<Long, String> attributes;

    private Button bBack;

    private boolean isShowRecentlyUsedCategory = false;

    public CategorySelectorActivity() {
        super(R.layout.category_selector);
        enablePin = false;
    }

    @Override
    protected void internalOnCreate(Bundle savedInstanceState) {
        Resources resources = getResources();
        this.incomeColor = resources.getColor(R.color.category_type_income);
        this.expenseColor = resources.getColor(R.color.category_type_expense);

        bBack = findViewById(R.id.bBack);
        bBack.setOnClickListener(view -> {
            if (navigator != null && navigator.goBack()) {
                setListAdapter(createAdapter(null));
            }
        });
        Button bSelect = findViewById(R.id.bSelect);
        bSelect.setOnClickListener(view -> confirmSelection());

        isShowRecentlyUsedCategory = MyPreferences.isShowRecentlyUsedCategory(this);
        if (isShowRecentlyUsedCategory) {
            View v = findViewById(R.id.suggestedCategoriesBarView);
            if (v != null) v.setVisibility(View.VISIBLE);
        }
    }

    private void confirmSelection() {
        if (navigator != null) {
            Intent data = new Intent();
            data.putExtra(SELECTED_CATEGORY_ID, navigator.selectedCategoryId);
            setResult(RESULT_OK, data);
        }
        finish();
    }

    @Override
    protected List<MenuItemInfo> createContextMenus(long id) {
        return Collections.emptyList();
    }

    @Override
    protected Cursor loadInBackground() {
        long excTreeId = -1;
        Intent intent = getIntent();

        if (isShowRecentlyUsedCategory) {
            var executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                var suggestedCategories = loadSuggestedCategories(intent);
                runOnUiThread(() -> fillSuggestedCategories(suggestedCategories));
            });
        }

        if (intent != null) {
            excTreeId = intent.getLongExtra(EXCLUDED_SUB_TREE_ID, -1);
        }
        navigator = new CategoryTreeNavigator(db, excTreeId);
        if (MyPreferences.isSeparateIncomeExpense(this)) {
            navigator.separateIncomeAndExpense();
        }
        attributes = db.getAllAttributesMap();

        if (intent != null) {
            boolean includeSplit = intent.getBooleanExtra(INCLUDE_SPLIT_CATEGORY, false);
            if (includeSplit) {
                navigator.addSplitCategoryToTheTop();
            }
            navigator.selectCategory(intent.getLongExtra(SELECTED_CATEGORY_ID, 0));
        }

        return null;
    }

    private List<Category> loadSuggestedCategories(Intent intent) {
        if (intent == null) {
            return null;
        }
        long selectedAccountId = intent.getLongExtra(SELECTED_ACCOUNT_ID, NO_SELECTED_ACCOUNT);
        if (selectedAccountId == NO_SELECTED_ACCOUNT) {
            return null;
        }

        var allTransactions = db.getTransactionsForAccount(selectedAccountId);
        if (allTransactions.isEmpty()) {
            return null;
        }

        var intervalEndMillis = allTransactions.get(0).dateTime;
        var intervalStartMillis = intervalEndMillis - TimeUnit.DAYS.toMillis(30);
        var transactionsProcessed = 0;
        var recentCategories = new HashMap<Category, Integer>();

        for (var t: allTransactions) {
            var c = t.category;
            if (!recentCategories.containsKey(c)) {
                recentCategories.put(c, 1);
            } else {
                recentCategories.put(c, recentCategories.get(c) + 1);
            }

            if (transactionsProcessed++ >= 30 && t.dateTime < intervalStartMillis) {
                break;
            }
        }

        var topCategories = new ArrayList<>(recentCategories.entrySet());
        Collections.sort(topCategories, (o1, o2) -> o2.getValue() - o1.getValue());

        var suggestedCategories = new ArrayList<Category>();
        for (var e: topCategories) {
            suggestedCategories.add(e.getKey());
            if (suggestedCategories.size() >= 5) {
                break;
            }
        }
        return suggestedCategories;
    }

    private void fillSuggestedCategories(List<Category> suggestedCategories) {
        var container = (LinearLayout)findViewById(R.id.suggestedCategoriesBar);
        Button placeholder = findViewById(R.id.suggestedCategoriesBarLoadingPlaceholder);

        if (suggestedCategories == null || suggestedCategories.isEmpty()) {
            placeholder.setText(R.string.no_suggestion);
            return;
        }

        placeholder.setVisibility(View.GONE);

        for (var c: suggestedCategories) {
            var v = buildViewForCategory(c);
            v.setOnClickListener(cv -> {
                while (navigator.canGoBack()) {
                    navigator.goBack();
                }
                navigator.selectCategory(c.id);
                confirmSelection();
            });
            container.addView(v);
        }
    }

    private View buildViewForCategory(Category c) {
        var res = new Button(this);
        res.setText(c.title);
        return res;
    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        if (navigator == null) {
            return null;
        }
        if (bBack != null) {
            bBack.setEnabled(navigator.canGoBack());
        }
        return new CategoryAdapter(navigator.categories);
    }

    @Override
    protected void deleteItem(View v, int position, long id) {
    }

    @Override
    protected void editItem(View v, int position, long id) {
    }

    @Override
    protected void viewItem(View v, int position, long id) {
        if (navigator.navigateTo(id)) {
            setListAdapter(createAdapter(null));
        } else {
            if (MyPreferences.isAutoSelectChildCategory(this)) {
                confirmSelection();
            }
        }
    }

    public static boolean pickCategory(Activity activity, boolean forceHierSelector, long selectedId, Account selectedAccount, long excludingTreeId, boolean includeSplit) {
        if (forceHierSelector || MyPreferences.isUseHierarchicalCategorySelector(activity)) {
            Intent intent = new Intent(activity, CategorySelectorActivity.class);
            intent.putExtra(CategorySelectorActivity.SELECTED_CATEGORY_ID, selectedId);
            intent.putExtra(CategorySelectorActivity.SELECTED_ACCOUNT_ID, selectedAccount == null ? NO_SELECTED_ACCOUNT : selectedAccount.getId());
            intent.putExtra(CategorySelectorActivity.EXCLUDED_SUB_TREE_ID, excludingTreeId);
            intent.putExtra(CategorySelectorActivity.INCLUDE_SPLIT_CATEGORY, includeSplit);
            activity.startActivityForResult(intent, R.id.category_pick);
            return true;
        }
        return false;
    }

    private class CategoryAdapter extends BaseAdapter {

        private final CategoryTree<Category> categories;

        private CategoryAdapter(CategoryTree<Category> categories) {
            this.categories = categories;
        }

        @Override
        public int getCount() {
            return categories.size();
        }

        @Override
        public Category getItem(int i) {
            return categories.getAt(i);
        }

        @Override
        public long getItemId(int i) {
            return getItem(i).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            BlotterListAdapter.BlotterViewHolder v;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.blotter_list_item, parent, false);
                v = new BlotterListAdapter.BlotterViewHolder(convertView);
                convertView.setTag(v);
            } else {
                v = (BlotterListAdapter.BlotterViewHolder)convertView.getTag();
            }
            Category c = getItem(position);
            if (c.id == CategoryTreeNavigator.INCOME_CATEGORY_ID) {
                v.centerView.setText(getString(R.string.income));                
            } else if (c.id == CategoryTreeNavigator.EXPENSE_CATEGORY_ID) {
                v.centerView.setText(getString(R.string.expense));
            } else {
                v.centerView.setText(c.title);
            }
            v.bottomView.setText(c.tag);
            v.indicator.setBackgroundColor(c.isIncome() ? incomeColor : expenseColor);
            v.rightCenterView.setVisibility(View.INVISIBLE);
            v.iconView.setVisibility(View.INVISIBLE);
            if (attributes != null && attributes.containsKey(c.id)) {
                v.rightView.setText(attributes.get(c.id));
                v.rightView.setVisibility(View.VISIBLE);
            } else {
                v.rightView.setVisibility(View.GONE);
            }
            v.topView.setVisibility(View.INVISIBLE);
            if (navigator.isSelected(c.id)) {
                v.layout.setBackgroundResource(R.drawable.list_selector_background_focus);
            } else {
                v.layout.setBackgroundResource(0);
            }
            v.top2View.setVisibility(View.INVISIBLE);
            return convertView;
        }

    }
    

}
