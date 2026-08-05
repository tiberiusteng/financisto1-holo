package tw.tib.financisto.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import tw.tib.financisto.R;
import tw.tib.financisto.adapter.dragndrop.ItemTouchHelperAdapter;
import tw.tib.financisto.adapter.dragndrop.ItemTouchHelperViewHolder;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.utils.AccountReorder;

public class AccountReorderAdapter extends RecyclerView.Adapter<AccountReorderAdapter.ViewHolder>
        implements ItemTouchHelperAdapter {

    private final Context context;
    private final List<Account> accounts;

    public AccountReorderAdapter(Context context, List<Account> accounts) {
        this.context = context;
        this.accounts = accounts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.account_reorder_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Account a = accounts.get(position);
        holder.title.setText(a.title);
        AccountRecyclerAdapter.setAccountIcon(holder.icon, holder.iconText, a);

        float alpha = a.isActive ? 1.0f : 0.5f;
        holder.title.setAlpha(alpha);
        holder.icon.setAlpha(alpha);
        holder.iconText.setAlpha(alpha);
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        AccountReorder.move(accounts, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemDismiss(int position, int direction) {

    }

    public List<Long> getAccountIds() {
        List<Long> ids = new ArrayList<>(accounts.size());
        for (Account a : accounts) {
            ids.add(a.id);
        }
        return ids;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {
        final ImageView icon;
        final TextView iconText;
        final TextView title;

        public ViewHolder(View v) {
            super(v);
            icon = v.findViewById(R.id.icon);
            iconText = v.findViewById(R.id.icon_text);
            title = v.findViewById(R.id.title);
        }

        @Override
        public void onItemSelected() {
            itemView.setAlpha(0.7f);
        }

        @Override
        public void onItemClear() {
            itemView.setAlpha(1.0f);
        }
    }
}
