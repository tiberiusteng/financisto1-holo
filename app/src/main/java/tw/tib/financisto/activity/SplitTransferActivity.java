package tw.tib.financisto.activity;

import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseHelper;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.Category;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.TransactionUtils;
import tw.tib.financisto.utils.Utils;
import tw.tib.financisto.widget.RateLayoutView;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 4/21/11 7:17 PM
 */
public class SplitTransferActivity extends AbstractSplitActivity implements CategorySelector.CategorySelectorListener {
    private static final String TAG = "SplitTransferActivity";

    private RateLayoutView rateView;

    protected TextView accountText;
    protected Cursor accountCursor;
    protected ListAdapter accountAdapter;

    private TextView accountBalanceText;
    private TextView accountLimitText;

    protected Utils u;

    private boolean isShowCategoryInTransfer;
    private CategorySelector<SplitTransferActivity> categorySelector;

    public SplitTransferActivity() {
        super(R.layout.split_fixed);
    }

    @Override
    protected void createUI(LinearLayout layout) {

        if (MyPreferences.isShowAccountBalanceOnSelector()) {
            accountText = x.addListNodeAccount(layout, R.id.account, R.string.account, R.string.select_account);
            View v = ((View) accountText.getTag());
            accountBalanceText = v.findViewById(R.id.balance);
            accountBalanceText.setVisibility(View.INVISIBLE);
            accountLimitText = v.findViewById(R.id.limit);
            accountLimitText.setVisibility(View.GONE);
        }
        else {
            accountText = x.addListNode(layout, R.id.account, R.string.account, R.string.select_account);
        }

        isShowCategoryInTransfer = MyPreferences.isShowCategoryInTransferScreen();
        categorySelector = new CategorySelector<>(this, db, x);
        if (isShowCategoryInTransfer) {
            categorySelector.createNode(layout, CategorySelector.SelectorType.SPLIT);
        }
        else {
            categorySelector.createDummyNode();
        }
        rateView = new RateLayoutView(this, x, layout);
        rateView.createSwitchableTransferUI();
        rateView.setAmountFromChangeListener((oldAmount, newAmount) -> setUnsplitAmount(split.unsplitAmount - newAmount));
        categorySelector.createAttributesLayout(layout);

        u = new Utils(this);
    }

    @Override
    protected void updateUIforPreventEditing() {
        boolean enabled = !isPreventEditing();
        categorySelector.setEnabled(enabled);
        if (accountText.getTag() instanceof View v) v.setEnabled(enabled);
        rateView.setEnabled(enabled);
    }

    @Override
    protected void fetchData() {
        accountCursor = db.getAllActiveAccounts();
        startManagingCursor(accountCursor);

        if (MyPreferences.isShowAccountBalanceOnSelector()) {
            accountAdapter = TransactionUtils.createAccountBalanceAdapter(this, accountCursor);
        }
        else {
            accountAdapter = TransactionUtils.createAccountAdapter(this, accountCursor);
        }

        if (MyPreferences.isShowCategoryInTransferScreen()) {
            categorySelector.setListener(this);
            categorySelector.doNotShowSplitCategory();
            categorySelector.fetchCategories(false);
        }
    }

    @Override
    protected void updateUI() {
        super.updateUI();
        Log.d(TAG, "updateUI splitParentAccountId=" + splitParentAccountId + ", split.fromAccountId=" + split.fromAccountId + ", "
                + " split.toAccountId=" + split.toAccountId);
        if (splitParentAccountId == split.fromAccountId) {
            selectFromAccount(split.fromAccountId);
            selectToAccount(split.toAccountId);
            setFromAmount(split.fromAmount);
            setToAmount(split.toAmount);
        }
        else {
            selectFromAccount(split.toAccountId);
            selectToAccount(split.fromAccountId);
            setFromAmount(split.toAmount);
            setToAmount(split.fromAmount);
        }
        categorySelector.selectCategory(split.categoryId);
    }

    @Override
    protected boolean updateFromUI() {
        super.updateFromUI();
        Log.d(TAG, "updateFromUI before work split.fromAccountId=" + split.fromAccountId + ", "
                + "split.toAccountId=" + split.toAccountId);
        long fromAmount = rateView.getFromAmount();
        long toAmount = rateView.getToAmount();
        if (fromAmount < 0) {
            split.fromAmount = fromAmount;
            split.toAmount = toAmount;
        }
        else {
            split.fromAmount = toAmount;
            split.toAmount = fromAmount;
            split.fromAccountId = split.toAccountId;
            split.toAccountId = splitParentAccountId;
        }
        if (split.fromAccountId == split.toAccountId) {
            Toast.makeText(this, R.string.select_to_account_differ_from_to_account, Toast.LENGTH_SHORT).show();
            return false;
        }
        Log.d(TAG, "updateFromUI fromAccountId=" + split.fromAccountId + ", fromAmount=" + split.fromAmount + ", "
                + "toAccountId=" + split.toAccountId + ", toAmount=" + split.toAmount);
        return true;
    }

    private void selectFromAccount(long accountId) {
        if (accountId > 0) {
            Account account = db.getAccount(accountId);
            Log.d(TAG, "selectCurrencyFrom " + account.currency);
            rateView.selectCurrencyFrom(account.currency);
        }
    }

    private void selectToAccount(long accountId) {
        if (accountId > 0) {
            Account account = db.getAccount(accountId);
            Log.d(TAG, "selectCurrencyTo " + account.currency);
            rateView.selectCurrencyTo(account.currency);
            accountText.setText(account.title);
            split.toAccountId = accountId;
            u.setAccountTitleBalance(account, accountText, accountBalanceText, accountLimitText);
        }
    }

    private void setFromAmount(long amount) {
        rateView.setFromAmount(amount);
    }

    private void setToAmount(long amount) {
        rateView.setToAmount(amount);
    }

    @Override
    protected void onClick(View v, int id) {
        super.onClick(v, id);
        if (id == R.id.account) {
            x.select(this, R.id.account, R.string.account_to, accountCursor, accountAdapter,
                    DatabaseHelper.AccountColumns.ID, split.toAccountId);
        }
        categorySelector.onClick(id);
    }

    @Override
    public void onSelectedId(int id, long selectedId) {
        super.onSelectedId(id, selectedId);
        switch(id) {
            case R.id.account:
                selectToAccount(selectedId);
                break;
        }
        categorySelector.onSelectedId(id, selectedId);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        categorySelector.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCategorySelected(Category category, boolean selectLast) {
        split.categoryId = category.id;
        categorySelector.addAttributes(split);
    }
}
