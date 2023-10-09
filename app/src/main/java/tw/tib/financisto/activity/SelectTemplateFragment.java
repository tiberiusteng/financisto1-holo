/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package tw.tib.financisto.activity;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import tw.tib.financisto.R;
import tw.tib.financisto.adapter.TemplateListAdapter;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BuildCompat;

public class SelectTemplateFragment extends TemplatesListFragment {

    public static final String TEMPLATE_ID = "template_id";
    public static final String MULTIPLIER = "multiplier";
    public static final String EDIT_AFTER_CREATION = "edit_after_creation";

    private TextView multiplierText;
    private int multiplier = 1;

    public SelectTemplateFragment() {
        super(R.layout.templates);
    }

    @Override
    @BuildCompat.PrereleaseSdkCheck
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        internalOnCreateTemplates();

        getListView().setOnItemLongClickListener((parent, view1, position, id) -> {
            returnResult(id, true);
            return true;
        });

        Button b = (Button) view.findViewById(R.id.bEditTemplates);
        b.setOnClickListener(arg0 -> {
            getActivity().setResult(RESULT_CANCELED);
            getActivity().finish();
            Intent intent = new Intent(getContext(), TemplatesListActivity.class);
            startActivity(intent);
        });
        b = (Button) view.findViewById(R.id.bCancel);
        b.setOnClickListener(arg0 -> {
            getActivity().setResult(RESULT_CANCELED);
            getActivity().finish();
        });
        multiplierText = (TextView) view.findViewById(R.id.multiplier);
        ImageButton ib = (ImageButton) view.findViewById(R.id.bPlus);
        ib.setOnClickListener(arg0 -> incrementMultiplier());
        ib = (ImageButton) view.findViewById(R.id.bMinus);
        ib.setOnClickListener(arg0 -> decrementMultiplier());
    }

    protected void incrementMultiplier() {
        ++multiplier;
        multiplierText.setText("x"+multiplier);
    }

    protected void decrementMultiplier() {
        --multiplier;
        if (multiplier < 1) {
            multiplier = 1;
        }
        multiplierText.setText("x"+multiplier);
    }

    @Override
    public void registerForContextMenu(View view) {
    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        return new TemplateListAdapter(getContext(), db, cursor);
    }

    @Override
    protected void onItemClick(View v, int position, long id) {
        returnResult(id, false);
    }

    @Override
    protected void viewItem(View v, int position, long id) {
        returnResult(id, false);
    }

    @Override
    public void editItem(View v, int position, long id) {
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        // do nothing
    }

    void returnResult(long id, boolean edit) {
        Intent intent = new Intent();
        intent.putExtra(TEMPLATE_ID, id);
        intent.putExtra(MULTIPLIER, multiplier);
        if (edit) intent.putExtra(EDIT_AFTER_CREATION, true);
        getActivity().setResult(RESULT_OK, intent);
        getActivity().finish();
    }

}
