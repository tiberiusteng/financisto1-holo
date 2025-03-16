/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package tw.tib.financisto.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import tw.tib.financisto.R;
import tw.tib.financisto.activity.AccountActivity;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.*;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.AccountType;
import tw.tib.financisto.model.CardIssuer;
import tw.tib.financisto.utils.Utils;
import tw.tib.financisto.view.NodeInflater;

import static tw.tib.financisto.utils.Utils.isNotEmpty;

public class AccountInfoDialog {

    private final Context context;
    private final long accountId;
    private final DatabaseAdapter db;
    private final NodeInflater inflater;
    private final LayoutInflater layoutInflater;
    private final Utils u;

    public AccountInfoDialog(Context context, long accountId,
                             DatabaseAdapter db, NodeInflater inflater) {
        this.context = context;
        this.accountId = accountId;
        this.db = db;
        this.inflater = inflater;
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.u = new Utils(context);
    }

    public void show() {
        Account a = db.getAccount(accountId);
        if (a == null) {
            Toast t = Toast.makeText(context, R.string.no_account, Toast.LENGTH_LONG);
            t.show();
            return;
        }

        View v = layoutInflater.inflate(R.layout.info_dialog, null);
        LinearLayout layout = (LinearLayout) v.findViewById(R.id.list);

        View titleView = createTitleView(a);
        createNodes(a, layout);

        showDialog(v, titleView);
    }

    private View createTitleView(Account a) {
        View titleView = layoutInflater.inflate(R.layout.info_dialog_title, null);
        TextView titleLabel = (TextView) titleView.findViewById(R.id.label);
        TextView titleData = (TextView) titleView.findViewById(R.id.data);
        ImageView titleIcon = (ImageView) titleView.findViewById(R.id.icon);

        titleLabel.setText(a.title);

        AccountType type = AccountType.valueOf(a.type);
        titleData.setText(type.titleId);
        titleIcon.setImageResource(type.iconId);

        return titleView;
    }

    private void createNodes(Account a, LinearLayout layout) {
        AccountType type = AccountType.valueOf(a.type);
        if (type.isCard) {
            CardIssuer issuer = CardIssuer.DEFAULT;
            if (a.cardIssuer != null) {
                issuer = CardIssuer.valueOf(a.cardIssuer);
            }
            add(layout, R.string.issuer, issuerTitle(a), issuer);
        }
        add(layout, R.string.currency, a.currency.title);

        if (type.isCreditCard && a.limitAmount != 0) {
            long limitAmount = Math.abs(a.limitAmount);
            long balance = limitAmount + a.totalAmount;
            TextView amountView = add(layout, R.string.amount, "");
            u.setAmountText(amountView, a.currency, a.totalAmount, true);
            TextView limitAmountView = add(layout, R.string.balance, "");
            u.setAmountText(limitAmountView, a.currency, balance, true);
        } else {
            TextView amountView = add(layout, R.string.balance, "");
            u.setAmountText(amountView, a.currency, a.totalAmount, true);
        }
        add(layout, R.string.note, a.note);
    }

    private String issuerTitle(Account a) {
        return (isNotEmpty(a.issuer) ? a.issuer : "")+" "+(isNotEmpty(a.number) ? "#"+a.number : "");
    }

    private void showDialog(final View v, View titleView) {
        final Dialog d = new AlertDialog.Builder(context)
                .setCustomTitle(titleView)
                .setView(v)
                .create();
        d.setCanceledOnTouchOutside(true);

        Button bEdit = (Button) v.findViewById(R.id.bEdit);
        bEdit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                d.dismiss();
                Intent intent = new Intent(context, AccountActivity.class);
                intent.putExtra(AccountActivity.ACCOUNT_ID_EXTRA, accountId);
                context.startActivity(intent);
            }
        });

        Button bClose = (Button) v.findViewById(R.id.bClose);
        bClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                d.dismiss();
            }
        });

        d.show();
    }

    private void add(LinearLayout layout, int labelId, String data, CardIssuer cardIssuer) {
        inflater.new Builder(layout, R.layout.select_entry_simple_icon)
                .withIcon(cardIssuer.iconId).withLabel(labelId).withData(data).create();
    }

    private TextView add(LinearLayout layout, int labelId, String data) {
        View v = inflater.new Builder(layout, R.layout.select_entry_simple).withLabel(labelId)
                .withData(data).create();
        return (TextView)v.findViewById(R.id.data);
    }

    private LinearLayout add(LinearLayout layout, String label, String data) {
        return (LinearLayout) inflater.new Builder(layout, R.layout.select_entry_simple).withLabel(label)
                .withData(data).create();
    }

}
