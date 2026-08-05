package tw.tib.financisto.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import tw.tib.financisto.R;
import tw.tib.financisto.adapter.AccountReorderAdapter;
import tw.tib.financisto.adapter.dragndrop.SimpleItemTouchHelperCallback;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.utils.AccountReorder;

public class AccountReorderDialog {

    private final Context context;
    private final DatabaseAdapter db;
    private final Runnable onSaved;

    public AccountReorderDialog(Context context, DatabaseAdapter db, Runnable onSaved) {
        this.context = context;
        this.db = db;
        this.onSaved = onSaved;
    }

    public AlertDialog show() {
        List<Account> accounts = db.getAllAccountsListWithClosed();
        accounts.sort(AccountReorder.BY_ACTIVE_THEN_SORT_ORDER);

        View v = LayoutInflater.from(context).inflate(R.layout.account_reorder_dialog, null);
        RecyclerView list = v.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(context));

        AccountReorderAdapter adapter = new AccountReorderAdapter(context, accounts);
        list.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter, false);
        new ItemTouchHelper(callback).attachToRecyclerView(list);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.reorder_accounts)
                .setView(v)
                .setPositiveButton(R.string.ok, (d, which) -> {
                    if (AccountReorder.isReordered(accounts)) {
                        db.updateAccountsSortOrder(adapter.getAccountIds());
                        if (onSaved != null) {
                            onSaved.run();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.show();
        return dialog;
    }
}
