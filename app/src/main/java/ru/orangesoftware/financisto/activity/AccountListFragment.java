package ru.orangesoftware.financisto.activity;

import static ru.orangesoftware.financisto.utils.MyPreferences.isQuickMenuEnabledForAccount;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import greendroid.widget.QuickActionGrid;
import greendroid.widget.QuickActionWidget;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.AccountListAdapter2;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.TotalCalculationTask;
import ru.orangesoftware.financisto.bus.GreenRobotBus_;
import ru.orangesoftware.financisto.bus.SwitchToMenuTabEvent;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.dialog.AccountInfoDialog;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.utils.IntegrityCheckAutobackup;
import ru.orangesoftware.financisto.utils.MenuItemInfo;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.view.NodeInflater;

public class AccountListFragment extends AbstractListFragment {

    private static final int NEW_ACCOUNT_REQUEST = 1;

    public static final int EDIT_ACCOUNT_REQUEST = 2;
    private static final int VIEW_ACCOUNT_REQUEST = 3;
    private static final int PURGE_ACCOUNT_REQUEST = 4;

    private QuickActionWidget accountActionGrid;

    public AccountListFragment() {
        super(R.layout.account_list);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUi();
        setupMenuButton();
        calculateTotals();
        integrityCheck();
    }

    private void setupUi() {
        getView().findViewById(R.id.integrity_error).setOnClickListener(v -> v.setVisibility(View.GONE));
        getListView().setOnItemLongClickListener((parent, view, position, id) -> {
            selectedId = id;
            prepareAccountActionGrid();
            accountActionGrid.show(view);
            return true;
        });
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
                MenuListItem.MENU_BACKUP.call(getActivity());
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
        accountActionGrid.setOnQuickActionClickListener(accountActionListener);
    }

    private QuickActionWidget.OnQuickActionClickListener accountActionListener = new QuickActionWidget.OnQuickActionClickListener() {
        public void onQuickActionClicked(QuickActionWidget widget, int position) {
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
            }
        }

    };

    private void addTransaction(long accountId, Class<? extends AbstractTransactionActivity> clazz) {
        Intent intent = new Intent(getContext(), clazz);
        intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, accountId);
        startActivityForResult(intent, VIEW_ACCOUNT_REQUEST);
    }

    @Override
    public void recreateCursor() {
        super.recreateCursor();
        calculateTotals();
    }

    private AccountListActivity.AccountTotalsCalculationTask totalCalculationTask;

    private void calculateTotals() {
        if (totalCalculationTask != null) {
            totalCalculationTask.stop();
            totalCalculationTask.cancel(true);
        }
        TextView totalText = getView().findViewById(R.id.total);
        totalText.setOnClickListener(view -> showTotals());
        totalCalculationTask = new AccountListActivity.AccountTotalsCalculationTask(getContext(), db, totalText);
        totalCalculationTask.execute();
    }

    private void showTotals() {
        Intent intent = new Intent(getContext(), AccountListTotalsDetailsActivity.class);
        startActivityForResult(intent, -1);
    }

    public static class AccountTotalsCalculationTask extends TotalCalculationTask {

        private final DatabaseAdapter db;

        AccountTotalsCalculationTask(Context context, DatabaseAdapter db, TextView totalText) {
            super(context, totalText);
            this.db = db;
        }

        @Override
        public Total getTotalInHomeCurrency() {
            return db.getAccountsTotalInHomeCurrency();
        }

        @Override
        public Total[] getTotals() {
            return new Total[0];
        }

    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        long t1 = System.currentTimeMillis();
        ListAdapter a = new AccountListAdapter2(getContext(), cursor);
        Log.d(this.getTag(), "createAdapter: " + (System.currentTimeMillis() - t1) + " ms");
        return a;
    }

    @Override
    protected Cursor createCursor() {
        Cursor c;
        long t1 = System.currentTimeMillis();
        if (MyPreferences.isHideClosedAccounts(getContext())) {
            c = db.getAllActiveAccounts();
        } else {
            c = db.getAllAccounts();
        }
        c.getCount();
        Log.d(this.getTag(), "createCursor: " + (System.currentTimeMillis() - t1) + " ms");
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

    private long selectedId = -1;

    private void showAccountInfo(long id) {
        NodeInflater nodeInflater = new NodeInflater(inflater);
        AccountInfoDialog accountInfoDialog = new AccountInfoDialog(getContext(), id, db, nodeInflater);
        accountInfoDialog.show();
    }


    @Override
    protected void onItemClick(View v, int position, long id) {
        if (isQuickMenuEnabledForAccount(getContext())) {
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
        new IntegrityCheckTask(getActivity()).execute(new IntegrityCheckAutobackup(getContext(), TimeUnit.DAYS.toMillis(7)));
    }
}
