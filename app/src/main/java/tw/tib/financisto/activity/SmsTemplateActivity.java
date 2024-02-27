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

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import tw.tib.financisto.R;
import tw.tib.financisto.adapter.MyEntityAdapter;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.db.DatabaseHelper.SmsTemplateColumns;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.SmsTemplate;
import tw.tib.financisto.service.SmsTransactionProcessor;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.Utils;

import java.util.ArrayList;

public class SmsTemplateActivity extends AbstractActivity {
    private static final String TAG = "SmsTemplateActivity";

    private DatabaseAdapter db;

    private EditText smsNumber;
    private EditText templateTxt;
    private EditText noteTxt;
    private EditText exampleTxt;
    private TextView parseResult;
    private Spinner accountSpinner;
    private Spinner toAccountSpinner;
    private ToggleButton toggleIncome;
    private ArrayList<Account> accounts;
    private ArrayList<Account> toAccounts;
    private long categoryId = -1;
    private SmsTemplate smsTemplate = new SmsTemplate();
    private CategorySelector<SmsTemplateActivity> categorySelector;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smstemplate);

        db = new DatabaseAdapter(this);
        db.open();

        smsNumber = findViewById(R.id.sms_number);
        initTitleAndDynamicDescription();
        templateTxt = findViewById(R.id.sms_template);
        noteTxt = findViewById(R.id.sms_note);
        initAccounts();
        toggleIncome = findViewById(R.id.toggle);

        parseResult = findViewById(R.id.parse_result);

        Button bOK = findViewById(R.id.bOK);
        bOK.setOnClickListener(arg0 -> {
            updateSmsTemplateFromUI();
            if (Utils.checkEditText(smsNumber, "sms number", true, 30)
                && Utils.checkEditText(templateTxt, "sms template", true, 160)) {
                long id = db.saveOrUpdate(smsTemplate);
                Intent intent = new Intent();
                intent.putExtra(SmsTemplateColumns._id.name(), id);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        Button bCancel = findViewById(R.id.bCancel);
        bCancel.setOnClickListener(arg0 -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        initExampleField();

        fillByCallerData();
        initCategorySelector();
    }

    private void initTitleAndDynamicDescription() {
        TextView templateTitle = findViewById(R.id.sms_tpl_title);
        TextView templateDesc = findViewById(R.id.sms_tpl_desc);
        templateDesc.setOnClickListener(v -> templateDesc.setVisibility(View.GONE));
        templateTitle.setOnClickListener(v -> templateDesc.setVisibility( templateDesc.getVisibility() == View.GONE ? View.VISIBLE : View.GONE));

        TextView noteTitle = findViewById(R.id.sms_note_title);
        TextView noteDesc = findViewById(R.id.sms_note_desc);
        noteDesc.setOnClickListener(v -> noteDesc.setVisibility(View.GONE));
        noteTitle.setOnClickListener(v -> noteDesc.setVisibility(noteDesc.getVisibility() == View.GONE ? View.VISIBLE : View.GONE));
    }

    private void initExampleField() {
        exampleTxt = findViewById(R.id.sms_example);
        exampleTxt.setOnFocusChangeListener((v, hasFocus) -> exampleTxt.setAlpha(1F));
        exampleTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateExampleAndHighlight(templateTxt.getText().toString(), s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        templateTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateExampleAndHighlight(s.toString(), exampleTxt.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void initCategorySelector() {
        if (categoryId == -1) {
            categorySelector = new CategorySelector<>(this, db, x);
            categorySelector.setEmptyResId(R.string.no_category);
            categorySelector.doNotShowSplitCategory();
            categorySelector.fetchCategories(false);
            categorySelector.createNode(findViewById(R.id.list2), CategorySelector.SelectorType.FILTER);
            
            if (smsTemplate != null) {
                categorySelector.selectCategory(smsTemplate.categoryId, false);
            }
        }
    }

    @Override
    protected void onClick(View v, int id) {
        categorySelector.onClick(id);
    }

    private void initAccounts() {
        accounts = new ArrayList<>();
        Account emptyItem = new Account();
        emptyItem.id = -1;
        emptyItem.title = getString(R.string.no_account);
        accounts.add(emptyItem);
        accounts.addAll(db.getAllAccountsList());

        toAccounts = new ArrayList<>();
        Account notTransfer = new Account();
        notTransfer.id = -1;
        notTransfer.title = getString(R.string.tpl_not_transfer);
        toAccounts.add(notTransfer);
        toAccounts.addAll(db.getAllAccountsList());

        ArrayAdapter<Account> accountsAdapter = new MyEntityAdapter<>(this, android.R.layout.simple_spinner_item, android.R.id.text1, accounts);
        accountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accountSpinner = findViewById(R.id.spinnerAccount);
        accountSpinner.setAdapter(accountsAdapter);

        ArrayAdapter<Account> toAccountsAdapter = new MyEntityAdapter<>(this, android.R.layout.simple_spinner_item, android.R.id.text1, toAccounts);
        toAccountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        toAccountSpinner = findViewById(R.id.spinnerTransferAccount);
        toAccountSpinner.setAdapter(toAccountsAdapter);
    }

    private void updateSmsTemplateFromUI() {
        smsTemplate.title = smsNumber.getText().toString();
        smsTemplate.template = templateTxt.getText().toString();
        smsTemplate.note = noteTxt.getText().toString();
        smsTemplate.categoryId = categorySelector == null ? categoryId : categorySelector.getSelectedCategoryId();
        smsTemplate.isIncome = toggleIncome.isChecked();
        smsTemplate.accountId = accountSpinner.getSelectedItemId();
        smsTemplate.toAccountId = toAccountSpinner.getSelectedItemId();
    }

    private void fillByCallerData() {
        final Intent intent = getIntent();
        if (intent != null) {
            long id = intent.getLongExtra(SmsTemplateColumns._id.name(), -1);
            categoryId = intent.getLongExtra(SmsTemplateColumns.category_id.name(), -1);
            if (id != -1) {
                smsTemplate = db.load(SmsTemplate.class, id);
                editSmsTemplate();
            }
        }
    }

    private void editSmsTemplate() {
        smsNumber.setText(smsTemplate.title);
        templateTxt.setText(smsTemplate.template);
        noteTxt.setText(smsTemplate.note);
        selectedAccount(smsTemplate.accountId);
        selectedToAccount(smsTemplate.toAccountId);
        toggleIncome.setChecked(smsTemplate.isIncome);
    }

    private void selectedAccount(long selectedAccountId) {
        for (int i=0; i<accounts.size(); i++) {
            Account a = accounts.get(i);
            if (a.id == selectedAccountId) {
                accountSpinner.setSelection(i);
                break;
            }
        }
    }

    private void selectedToAccount(long selectedAccountId) {
        for (int i=0; i<toAccounts.size(); i++) {
            Account a = toAccounts.get(i);
            if (a.id == selectedAccountId) {
                toAccountSpinner.setSelection(i);
                break;
            }
        }
    }

    @Override
    public void onSelectedId(int id, long selectedId) {
        categorySelector.onSelectedId(id, selectedId);
        switch (id) {
            case R.id.category:
                categoryId = categorySelector.getSelectedCategoryId();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        categorySelector.onActivityResult(requestCode, resultCode, data);
    }

    private void validateExampleAndHighlight(String template, String example) {
        if (Utils.isNotEmpty(template) && template.length() > 4 && Utils.isNotEmpty(example)) {
            final Resources resources = SmsTemplateActivity.this.getResources();
            final String[] matches = SmsTransactionProcessor.findTemplateMatches(template, example);
            if (matches == null) {
                exampleTxt.setBackgroundColor(resources.getColor(R.color.negative_amount));
                parseResult.setText("");
            } else {
                StringBuilder sb = new StringBuilder();
                exampleTxt.setBackgroundColor(resources.getColor(R.color.cleared_transaction_color));

                // dump match result to help debugging
                // no styling yet, could be improved
                for (SmsTransactionProcessor.Placeholder p : SmsTransactionProcessor.Placeholder.values()) {
                    sb.append(p.name()); sb.append(": ");
                    if (matches[p.ordinal()] == null) {
                        sb.append(getString(R.string.tpl_parse_not_found));
                    }
                    else if (p == SmsTransactionProcessor.Placeholder.PRICE) {
                        // price will be converted to big decimal
                        // show converted result instead of raw input
                        try {
                            sb.append(SmsTransactionProcessor.toBigDecimal(matches[p.ordinal()]));
                        } catch (Exception e) {
                            sb.append(getString(R.string.tpl_failed_to_parse, matches[p.ordinal()]));
                        }
                    }
                    else {
                        sb.append(matches[p.ordinal()]);
                    }
                    sb.append("\n");
                }
                parseResult.setText(sb);
            }
        }
    }
}
