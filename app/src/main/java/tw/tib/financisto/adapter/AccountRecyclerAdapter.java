package tw.tib.financisto.adapter;

import static java.lang.String.format;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.Date;

import tw.tib.financisto.R;
import tw.tib.financisto.adapter.dragndrop.ItemTouchHelperAdapter;
import tw.tib.financisto.adapter.dragndrop.ItemTouchHelperViewHolder;
import tw.tib.financisto.datetime.DateUtils;
import tw.tib.financisto.db.DatabaseHelper;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.AccountType;
import tw.tib.financisto.model.CardIssuer;
import tw.tib.financisto.model.ElectronicPaymentType;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.Utils;
import tw.tib.orb.EntityManager;

public class AccountRecyclerAdapter extends RecyclerView.Adapter<AccountRecyclerAdapter.ViewHolder>
        implements ItemTouchHelperAdapter
{
    public static final String TAG = "AccountRecyclerAdapter";

    protected final Context context;
    protected final Cursor cursor;

    private long created = 0;
    private final Utils u;
    private DateFormat df;
    private MyPreferences.AccountListDateType accountListDateType;
    private View.OnClickListener onClickListener = null;
    private View.OnLongClickListener onLongClickListener = null;

    public AccountRecyclerAdapter(Context context, Cursor c, View.OnClickListener onClickListener,
                                  View.OnLongClickListener onLongClickListener) {
        this.u = new Utils(context);
        this.df = DateUtils.getShortDateFormat(context);
        this.accountListDateType = MyPreferences.getAccountListDateType(context);
        this.context = context;
        this.cursor = c;
        this.onClickListener = onClickListener;
        this.onLongClickListener = onLongClickListener;
    }

    @Override
    public long getItemId(int position) {
        cursor.moveToPosition(position);
        return cursor.getLong(DatabaseHelper.BlotterColumns._id.ordinal());
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    @NonNull
    @Override
    public AccountRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        created++;
        Log.d(TAG, "onCreateViewHolder " + created);
        View view = LayoutInflater.from(context).inflate(R.layout.account_list_item, parent, false);
        view.setOnClickListener(this.onClickListener);
        view.setOnLongClickListener(this.onLongClickListener);

        return new AccountRecyclerAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder v, int position) {
        v.used++;
        Log.d(TAG, "onBindViewHolder used " + v.used);
        long t0 = System.nanoTime();

        cursor.moveToPosition(position);

        Account a = EntityManager.loadFromCursor(cursor, Account.class);

        v.view.setTag(R.id.account, a.getId());
        v.center.setText(a.title);

        AccountType type = AccountType.valueOf(a.type);

        if (!Utils.isEmpty(a.icon)) {
            v.icon.setVisibility(View.INVISIBLE);
            v.iconText.setVisibility(View.VISIBLE);
            v.iconText.setText(a.icon);
        }
        else {
            v.icon.setVisibility(View.VISIBLE);
            v.iconText.setVisibility(View.INVISIBLE);

            if (type.isCard && a.cardIssuer != null) {
                CardIssuer cardIssuer = CardIssuer.valueOf(a.cardIssuer);
                v.icon.setImageResource(cardIssuer.iconId);
            } else if (type.isElectronic && a.cardIssuer != null) {
                ElectronicPaymentType electronicPaymentType = ElectronicPaymentType.valueOf(a.cardIssuer);
                v.icon.setImageResource(electronicPaymentType.iconId);
            } else {
                v.icon.setImageResource(type.iconId);
            }
        }

        if (a.isActive) {
            v.icon.getDrawable().mutate().setAlpha(0xFF);
            v.iconText.setAlpha(1.0f);
            v.activeIcon.setVisibility(View.INVISIBLE);
        } else {
            v.icon.getDrawable().mutate().setAlpha(0x77);
            v.iconText.setAlpha(0.5f);
            v.activeIcon.setVisibility(View.VISIBLE);
        }

        StringBuilder sb = new StringBuilder();
        if (!Utils.isEmpty(a.issuer)) {
            sb.append(a.issuer);
        }
        if (!Utils.isEmpty(a.number)) {
            sb.append(" #").append(a.number);
        }
        if (sb.length() == 0) {
            sb.append(context.getString(type.titleId));
        }
        v.top.setText(sb.toString());

        switch (accountListDateType) {
            case LAST_TX:
                v.bottom.setText(df.format(new Date(a.lastTransactionDate)));
                break;
            case ACCOUNT_CREATION:
                if (a.creationDate != 0) {
                    v.bottom.setText(df.format(new Date(a.creationDate)));
                }
                else {
                    v.bottom.setText("");
                }
                break;
            case ACCOUNT_UPDATE:
                if (a.updatedOn != 0) {
                    v.bottom.setText(df.format(new Date(a.updatedOn)));
                }
                else {
                    v.bottom.setText("");
                }
                break;
            default:
            case HIDDEN:
                v.bottom.setVisibility(View.GONE);
        }

        long amount = a.totalAmount;
        if (type == AccountType.CREDIT_CARD && a.limitAmount != 0) {
            long limitAmount = Math.abs(a.limitAmount);
            long balance = limitAmount + amount;
            long balancePercentage = 10000*balance/limitAmount;
            u.setAmountText(v.rightCenter, a.currency, amount, false);
            u.setAmountText(v.right, a.currency, balance, false);
            v.right.setVisibility(View.VISIBLE);
            v.progress.setMax(10000);
            v.progress.setProgress((int)balancePercentage);
            v.progress.setVisibility(View.VISIBLE);
        } else {
            u.setAmountText(v.rightCenter, a.currency, amount, false);
            v.right.setVisibility(View.GONE);
            v.progress.setVisibility(View.GONE);
        }

        try {
            if (!Utils.isEmpty(a.accentColor)) {
                int color = Color.parseColor(a.accentColor);
                v.accent.setVisibility(View.VISIBLE);
                v.accent.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{color, 0}));
            }
            else {
                v.accent.setVisibility(View.INVISIBLE);
            }
        } catch (Exception e) {
            v.accent.setVisibility(View.INVISIBLE);
        }

        long t1 = System.nanoTime();
        Log.d(TAG, "onBindViewHolder " + (t1 - t0) / 1000 + " us");
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {

    }

    @Override
    public void onItemDismiss(int position, int direction) {

    }

    public static class ViewHolder extends RecyclerView.ViewHolder
            implements ItemTouchHelperViewHolder
    {
        public long used = 0;

        public final View view;
        public final TextView center;
        public final ImageView icon;
        public final TextView iconText;
        public final View accent;
        public final ImageView activeIcon;
        public final TextView top;
        public final TextView bottom;
        public final TextView rightCenter;
        public final TextView right;
        public final ProgressBar progress;

        public ViewHolder(View v) {
            super(v);
            view = v;
            accent = v.findViewById(R.id.accent);
            center = v.findViewById(R.id.center);
            icon = v.findViewById(R.id.icon);
            iconText = v.findViewById(R.id.icon_text);
            activeIcon = v.findViewById(R.id.active_icon);
            top = v.findViewById(R.id.top);
            bottom = v.findViewById(R.id.bottom);
            rightCenter = v.findViewById(R.id.right_center);
            right = v.findViewById(R.id.right);
            progress = v.findViewById(R.id.progress);
            progress.setVisibility(View.GONE);
        }

        @Override
        public void onItemSelected() {

        }

        @Override
        public void onItemClear() {

        }
    }
}
