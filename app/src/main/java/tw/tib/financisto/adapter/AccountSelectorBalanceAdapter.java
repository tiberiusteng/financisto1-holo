package tw.tib.financisto.adapter;

import tw.tib.financisto.R;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import tw.tib.financisto.model.Account;
import tw.tib.financisto.utils.Utils;
import tw.tib.orb.EntityManager;

public class AccountSelectorBalanceAdapter extends ResourceCursorAdapter {
    private Utils u;

    public AccountSelectorBalanceAdapter(Context context, int layout, Cursor c) {
        super(context, layout, c);
        u = new Utils(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = super.newView(context, cursor, parent);
        view.setTag(new ViewHolder(view));
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        var vh = (ViewHolder) view.getTag();
        Account a = EntityManager.loadFromCursor(cursor, Account.class);
        u.setAccountTitleBalance(a, vh.title, vh.balance, vh.limit);
    }

    static class ViewHolder {
        public TextView title;
        public TextView balance;
        public TextView limit;

        public ViewHolder(View v) {
            title = v.findViewById(R.id.data);
            balance = v.findViewById(R.id.balance);
            limit = v.findViewById(R.id.limit);

            v.setTag(this);
        }
    }
}
