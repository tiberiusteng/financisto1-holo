package tw.tib.financisto.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import greendroid.widget.QuickActionGrid;
import greendroid.widget.QuickActionWidget;
import tw.tib.financisto.R;
import tw.tib.financisto.adapter.AccountListAdapter;
import tw.tib.financisto.blotter.BlotterFilter;
import tw.tib.financisto.blotter.TotalCalculationTask;
import tw.tib.financisto.bus.SwitchToMenuTabEvent;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.dialog.AccountInfoDialog;
import tw.tib.financisto.filter.Criteria;
import tw.tib.financisto.bus.GreenRobotBus_;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.Total;
import tw.tib.financisto.utils.IntegrityCheckAutobackup;
import tw.tib.financisto.utils.MenuItemInfo;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.PinProtection;
import tw.tib.financisto.view.NodeInflater;

public class AccountListFragment extends AbstractListFragment<Cursor> {
    private static final String TAG = "AccountListFragment";

    private static final String FILTER_PERF = "filter";

    private static final int NEW_ACCOUNT_REQUEST = 1;

    public static final int EDIT_ACCOUNT_REQUEST = 2;
    private static final int VIEW_ACCOUNT_REQUEST = 3;
    private static final int PURGE_ACCOUNT_REQUEST = 4;

    private QuickActionWidget accountActionGrid;
    private TextView emptyText;
    private ProgressBar progressBar;
    private ImageButton bSearch;
    private String filter;

    private long selectedId = -1;

    public AccountListFragment() {
        super(R.layout.account_list);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUi(view);
        setupMenuButton();
        calculateTotals();
        integrityCheck();
    }

    private void setupUi(View view) {
        EditText searchText = view.findViewById(R.id.search_text);
        FrameLayout searchLayout = view.findViewById(R.id.search_text_frame);
        ImageButton clearButton = view.findViewById(R.id.search_text_clear);

        view.findViewById(R.id.integrity_error).setOnClickListener(v -> v.setVisibility(View.GONE));
        getListView().setOnItemLongClickListener((parent, child, position, id) -> {
            selectedId = id;
            prepareAccountActionGrid();
            accountActionGrid.show(child);
            return true;
        });
        emptyText = view.findViewById(android.R.id.empty);
        progressBar = view.findViewById(android.R.id.progress);

        bSearch = view.findViewById(R.id.bSearch);
        if (bSearch != null) {
            loadFilter();

            if (!filter.isEmpty()) {
                searchLayout.setVisibility(View.VISIBLE);
                clearButton.setVisibility(View.VISIBLE);
                searchText.setText(filter);
                bSearch.setColorFilter(new LightingColorFilter(Color.BLACK, getResources().getColor(R.color.holo_blue_dark)));
            }

            bSearch.setOnClickListener(method -> {
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

                searchText.setOnFocusChangeListener((v, b) -> {
                    if (!v.hasFocus()) {
                        imm.hideSoftInputFromWindow(searchLayout.getWindowToken(), 0);
                    }
                });

                clearButton.setOnClickListener(v -> {
                    searchText.setText("");
                });

                if (searchLayout.getVisibility() == View.VISIBLE) {
                    imm.hideSoftInputFromWindow(searchLayout.getWindowToken(), 0);
                    searchLayout.setVisibility(View.GONE);
                    return;
                }

                searchLayout.setVisibility(View.VISIBLE);
                searchText.requestFocusFromTouch();
                imm.showSoftInput(searchText, InputMethodManager.SHOW_IMPLICIT);

                searchText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        ImageButton clearButton = view.findViewById(R.id.search_text_clear);
                        String text = editable.toString();
                        filter = text;

                        if (!text.isEmpty()) {
                            clearButton.setVisibility(View.VISIBLE);
                            bSearch.setColorFilter(new LightingColorFilter(Color.BLACK, getResources().getColor(R.color.holo_blue_dark)));
                        } else {
                            clearButton.setVisibility(View.GONE);
                            bSearch.setColorFilter(null);
                        }

                        recreateCursor();
                        saveFilter();
                    }
                });
            });
        }
    }

    private void setupMenuButton() {
        final ImageButton bMenu = getView().findViewById(R.id.bMenu);
        if (MyPreferences.isShowMenuButtonOnAccountsScreen(getContext())) {
            bMenu.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(getActivity(), bMenu);
                MenuInflater inflater = getActivity().getMenuInflater();
                inflater.inflate(R.menu.account_list_menu, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(item -> {
                    handlePopupMenu(item.getItemId());
                    return true;
                });
                popupMenu.show();
            });
        } else {
            bMenu.setVisibility(View.GONE);
        }
    }

    private void handlePopupMenu(int id) {
        switch (id) {
            case R.id.backup:
                MenuListItem.MENU_BACKUP.call(this);
                break;
            case R.id.go_to_menu:
                GreenRobotBus_.getInstance_(getActivity()).post(new SwitchToMenuTabEvent());
                break;
        }
    }

    protected void prepareAccountActionGrid() {
        Account a = db.getAccount(selectedId);
        accountActionGrid = new QuickActionGrid(getContext());
        accountActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.ic_action_info, R.string.info));
        accountActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.ic_action_list, R.string.blotter));
        accountActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.ic_action_edit, R.string.edit));
        accountActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.ic_action_add, R.string.transaction));
        accountActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.ic_action_transfer, R.string.transfer));
        accountActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.ic_action_tick, R.string.balance));
        accountActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.ic_action_flash, R.string.delete_old_transactions));
        if (a.isActive) {
            accountActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.ic_action_lock_closed, R.string.close_account));
        } else {
            accountActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.ic_action_lock_open, R.string.reopen_account));
        }
        accountActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.ic_action_trash, R.string.delete_account));
        if (MyPreferences.isShowTransferCurrentBalance(getContext())) {
            accountActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.share_windows_32dp, R.string.transfer_current_balance));
        }
        accountActionGrid.setOnQuickActionClickListener(accountActionListener);
    }

    private QuickActionWidget.OnQuickActionClickListener accountActionListener = (widget, position, action) -> {
        switch (position) {
            case 0:
                showAccountInfo(selectedId);
                break;
            case 1:
                showAccountTransactions(selectedId);
                break;
            case 2:
                editAccount(selectedId);
                break;
            case 3:
                addTransaction(selectedId, TransactionActivity.class);
                break;
            case 4:
                addTransaction(selectedId, TransferActivity.class);
                break;
            case 5:
                updateAccountBalance(selectedId);
                break;
            case 6:
                purgeAccount();
                break;
            case 7:
                closeOrOpenAccount();
                break;
            case 8:
                deleteAccount();
                break;
            case 9:
                transferCurrentBalance(selectedId);
                break;
        }
    };

    private void addTransaction(long accountId, Class<? extends AbstractTransactionActivity> clazz) {
        Intent intent = new Intent(getContext(), clazz);
        intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, accountId);
        startActivityForResult(intent, VIEW_ACCOUNT_REQUEST);
    }

    private void transferCurrentBalance(long accountId) {
        Account a = db.getAccount(accountId);
        if (a != null) {
            Intent intent = new Intent(getContext(), TransferActivity.class);
            intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, accountId);
            intent.putExtra(TransferActivity.AMOUNT_EXTRA, a.totalAmount);
            startActivityForResult(intent, VIEW_ACCOUNT_REQUEST);
        }
    }

    @Override
    public void recreateCursor() {
        Log.d(this.getClass().getSimpleName(), "recreateCursor");
        super.recreateCursor();
        calculateTotals();
    }

    private AccountTotalsCalculationTask totalCalculationTask;

    private void calculateTotals() {
        if (totalCalculationTask != null) {
            totalCalculationTask.stop();
            totalCalculationTask.cancel(true);
        }
        TextView totalText = getView().findViewById(R.id.total);
        totalText.setOnClickListener(view -> showTotals());
        totalCalculationTask = new AccountTotalsCalculationTask(getContext(), db, totalText, filter);
        totalCalculationTask.execute();
    }

    private void showTotals() {
        Intent intent = new Intent(getContext(), AccountListTotalsDetailsActivity.class);
        intent.putExtra(AccountListTotalsDetailsActivity.FILTER, filter);
        startActivityForResult(intent, -1);
    }

    public static class AccountTotalsCalculationTask extends TotalCalculationTask {

        private final DatabaseAdapter db;
        private String filter;

        AccountTotalsCalculationTask(Context context, DatabaseAdapter db, TextView totalText, String filter) {
            super(context, db, totalText);
            this.db = db;
            this.filter = filter;
        }

        @Override
        public Total getTotalInHomeCurrency() {
            return db.getAccountsTotalInHomeCurrencyWithFilter(filter);
        }

        @Override
        public Total[] getTotals() {
            return new Total[0];
        }

    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        long t1 = System.currentTimeMillis();
        ListAdapter a = new AccountListAdapter(getContext(), cursor);
        if (a.getCount() == 0) {
            emptyText.setVisibility(View.VISIBLE);
        }
        progressBar.setVisibility(View.GONE);
        Log.d(this.getClass().getSimpleName(), "createAdapter: " + (System.currentTimeMillis() - t1) + " ms");
        return a;
    }

    @Override
    protected Cursor loadInBackground() {
        Cursor c;

        new Handler(Looper.getMainLooper()).post(()-> {
            emptyText.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        });

        Log.d(this.getClass().getSimpleName(), "createCursor start");
        long t1 = System.currentTimeMillis();
        if (MyPreferences.isHideClosedAccounts(context)) {
            c = db.getAllActiveAccountsWithFilter(filter);
        } else {
            c = db.getAllAccountsWithFilter(filter);
        }
        c.getCount();
        Log.d(getClass().getSimpleName(), "createCursor: " + (System.currentTimeMillis() - t1) + " ms");
        return c;
    }

    protected List<MenuItemInfo> createContextMenus(long id) {
        return new ArrayList<>();
    }

    @Override
    public boolean onPopupItemSelected(int itemId, View view, int position, long id) {
        // do nothing
        return true;
    }

    private boolean updateAccountBalance(long id) {
        Account a = db.getAccount(id);
        if (a != null) {
            Intent intent = new Intent(getContext(), TransactionActivity.class);
            intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, a.id);
            intent.putExtra(TransactionActivity.CURRENT_BALANCE_EXTRA, a.totalAmount);
            startActivityForResult(intent, 0);
            return true;
        }
        return false;
    }

    @Override
    protected void addItem() {
        Intent intent = new Intent(getContext(), AccountActivity.class);
        startActivityForResult(intent, NEW_ACCOUNT_REQUEST);
    }

    @Override
    protected void deleteItem(View v, int position, final long id) {
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.delete_account_confirm)
                .setPositiveButton(R.string.yes, (arg0, arg1) -> {
                    db.deleteAccount(id);
                    recreateCursor();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    public void editItem(View v, int position, long id) {
        editAccount(id);
    }

    private void editAccount(long id) {
        Intent intent = new Intent(getContext(), AccountActivity.class);
        intent.putExtra(AccountActivity.ACCOUNT_ID_EXTRA, id);
        startActivityForResult(intent, EDIT_ACCOUNT_REQUEST);
    }

    private void showAccountInfo(long id) {
        NodeInflater nodeInflater = new NodeInflater(inflater);
        AccountInfoDialog accountInfoDialog = new AccountInfoDialog(getContext(), id, db, nodeInflater);
        accountInfoDialog.show();
    }


    @Override
    protected void onItemClick(View v, int position, long id) {
        if (MyPreferences.isQuickMenuEnabledForAccount(getContext())) {
            selectedId = id;
            prepareAccountActionGrid();
            accountActionGrid.show(v);
        } else {
            showAccountTransactions(id);
        }
    }

    @Override
    protected void viewItem(View v, int position, long id) {
        showAccountTransactions(id);
    }

    private void showAccountTransactions(long id) {
        Account account = db.getAccount(id);
        if (account != null) {
            Intent intent = new Intent(getContext(), BlotterActivity.class);
            Criteria.eq(BlotterFilter.FROM_ACCOUNT_ID, String.valueOf(id))
                    .toIntent(account.title, intent);
            intent.putExtra(BlotterFilterActivity.IS_ACCOUNT_FILTER, true);
            startActivityForResult(intent, VIEW_ACCOUNT_REQUEST);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VIEW_ACCOUNT_REQUEST || requestCode == PURGE_ACCOUNT_REQUEST) {
            recreateCursor();
        }
    }

    private void purgeAccount() {
        Intent intent = new Intent(getContext(), PurgeAccountActivity.class);
        intent.putExtra(PurgeAccountActivity.ACCOUNT_ID, selectedId);
        startActivityForResult(intent, PURGE_ACCOUNT_REQUEST);
    }

    private void closeOrOpenAccount() {
        Account a = db.getAccount(selectedId);
        if (a.isActive) {
            new AlertDialog.Builder(getContext())
                    .setMessage(R.string.close_account_confirm)
                    .setPositiveButton(R.string.yes, (arg0, arg1) -> flipAccountActive(a))
                    .setNegativeButton(R.string.no, null)
                    .show();
        } else {
            flipAccountActive(a);
        }
    }

    private void flipAccountActive(Account a) {
        a.isActive = !a.isActive;
        db.saveAccount(a);
        recreateCursor();
    }

    private void deleteAccount() {
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.delete_account_confirm)
                .setPositiveButton(R.string.yes, (arg0, arg1) -> {
                    db.deleteAccount(selectedId);
                    recreateCursor();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    public void integrityCheck() {
        new IntegrityCheckTask(this).execute(new IntegrityCheckAutobackup(getContext(), TimeUnit.DAYS.toMillis(7)));
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(this.getClass().getSimpleName(), "onResume");

        if (PinProtection.isUnlocked()) {
            Log.d(this.getClass().getSimpleName(), "onResume isUnlocked, show list");
            getListView().setVisibility(View.VISIBLE);
        }
        else {
            // still locked, don't show account list balances
            Log.d(this.getClass().getSimpleName(), "onResume NOT isUnlocked, hide list");
            getListView().setVisibility(View.INVISIBLE);
        }
    }

    private void loadFilter() {
        SharedPreferences preferences = getContext().getSharedPreferences(TAG, 0);
        filter = preferences.getString(FILTER_PERF, "");
    }

    private void saveFilter() {
        SharedPreferences preferences = getContext().getSharedPreferences(TAG, 0);
        SharedPreferences.Editor e = preferences.edit();
        e.putString(FILTER_PERF, filter);
        e.apply();
    }
}
