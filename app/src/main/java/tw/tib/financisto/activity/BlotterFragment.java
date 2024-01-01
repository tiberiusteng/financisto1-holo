package tw.tib.financisto.activity;

import static android.app.Activity.RESULT_FIRST_USER;
import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import greendroid.widget.QuickActionGrid;
import greendroid.widget.QuickActionWidget;
import tw.tib.financisto.R;
import tw.tib.financisto.adapter.BlotterListAdapter;
import tw.tib.financisto.adapter.TransactionsListAdapter;
import tw.tib.financisto.blotter.AccountTotalCalculationTask;
import tw.tib.financisto.blotter.BlotterFilter;
import tw.tib.financisto.blotter.BlotterTotalCalculationTask;
import tw.tib.financisto.blotter.TotalCalculationTask;
import tw.tib.financisto.dialog.TransactionInfoDialog;
import tw.tib.financisto.filter.Criteria;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.AccountType;
import tw.tib.financisto.model.Budget;
import tw.tib.financisto.model.Transaction;
import tw.tib.financisto.utils.IntegrityCheckRunningBalance;
import tw.tib.financisto.utils.MenuItemInfo;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.PinProtection;
import tw.tib.financisto.view.NodeInflater;

public class BlotterFragment extends AbstractListFragment implements BlotterOperations.BlotterOperationsCallback {
    public static final String SAVE_FILTER = "saveFilter";
    public static final String EXTRA_FILTER_ACCOUNTS = "filterAccounts";

    private static final int NEW_TRANSACTION_REQUEST = 1;
    private static final int NEW_TRANSFER_REQUEST = 3;
    private static final int NEW_TRANSACTION_FROM_TEMPLATE_REQUEST = 5;
    private static final int MONTHLY_VIEW_REQUEST = 6;
    private static final int BILL_PREVIEW_REQUEST = 7;

    protected static final int FILTER_REQUEST = 6;
    private static final int MENU_DUPLICATE = MENU_ADD + 1;
    private static final int MENU_SAVE_AS_TEMPLATE = MENU_ADD + 2;

    protected TextView totalText;
    protected TextView emptyText;
    protected ProgressBar progressBar;

    protected ImageButton bFilter;
    protected ImageButton bTransfer;
    protected ImageButton bTemplate;
    protected ImageButton bSearch;
//    protected ImageButton bMenu;

    protected QuickActionGrid transactionActionGrid;
    protected QuickActionGrid addButtonActionGrid;

    private TotalCalculationTask calculationTask;

    protected boolean saveFilter;
    protected WhereFilter blotterFilter = WhereFilter.empty();

    protected boolean isAccountBlotter = false;
    protected boolean showAllBlotterButtons = true;
    protected boolean isQuickMenuEnabledForTransaction = false;
    protected boolean isQuickMenuShowAdditionalTransactionStatus = false;

    protected OnBackPressedCallback backCallback;

    private static final Pattern amountSearchPattern = Pattern.compile("^([<>])?(\\d+(?:\\.\\d+)?)(?:~(\\d+(?:\\.\\d+)?))?$");

    public BlotterFragment(int layoutId) {
        super(layoutId);
    }

    public BlotterFragment() {
        super(R.layout.blotter);
    }

    public BlotterFragment(boolean saveFilter) {
        super(R.layout.blotter);
        this.saveFilter = saveFilter;
    }

    protected void calculateTotals() {
        if (calculationTask != null) {
            calculationTask.stop();
            calculationTask.cancel(true);
        }
        calculationTask = createTotalCalculationTask();
        calculationTask.execute();
    }

    protected TotalCalculationTask createTotalCalculationTask() {
        WhereFilter filter = WhereFilter.copyOf(blotterFilter);
        if (filter.getAccountId() > 0) {
            return new AccountTotalCalculationTask(getContext(), db, filter, totalText);
        } else {
            return new BlotterTotalCalculationTask(getContext(), db, filter, totalText);
        }
    }

    @Override
    public void recreateCursor() {
        super.recreateCursor();
        calculateTotals();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater layoutInflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(layoutInflater, container, savedInstanceState);
        inflater = new NodeInflater(layoutInflater);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        integrityCheck();

        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                FrameLayout searchLayout = view.findViewById(R.id.search_text_frame);
                if (searchLayout != null && searchLayout.getVisibility() == View.VISIBLE) {
                    searchLayout.setVisibility(View.GONE);
                    this.setEnabled(false);
                }
            }
        };

        getActivity().getOnBackPressedDispatcher().addCallback(backCallback);

        showAllBlotterButtons = !MyPreferences.isCollapseBlotterButtons(getContext());

        isQuickMenuEnabledForTransaction = MyPreferences.isQuickMenuEnabledForTransaction(getContext());
        isQuickMenuShowAdditionalTransactionStatus = MyPreferences.isQuickMenuShowAdditionalTransactionStatus(getContext());

        if (showAllBlotterButtons) {
            bTransfer = view.findViewById(R.id.bTransfer);
            if (bTransfer != null) {
                bTransfer.setVisibility(View.VISIBLE);
                bTransfer.setOnClickListener(arg0 -> addItem(NEW_TRANSFER_REQUEST, TransferActivity.class));
            }

            bTemplate = view.findViewById(R.id.bTemplate);
            if (bTemplate != null) {
                bTemplate.setVisibility(View.VISIBLE);
                bTemplate.setOnClickListener(v -> createFromTemplate());
            }
        }

        bFilter = view.findViewById(R.id.bFilter);
        if (bFilter != null) {
            bFilter.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), BlotterFilterActivity.class);
                blotterFilter.toIntent(intent);
                intent.putExtra(BlotterFilterActivity.IS_ACCOUNT_FILTER, isAccountBlotter && blotterFilter.getAccountId() > 0);
                startActivityForResult(intent, FILTER_REQUEST);
            });
        }

        totalText = view.findViewById(R.id.total);
        if (totalText != null) {
            totalText.setOnClickListener(v -> showTotals());
        }

        emptyText = view.findViewById(android.R.id.empty);
        progressBar = view.findViewById(android.R.id.progress);

        Bundle args = getArguments();
        if (args != null) {
            blotterFilter = WhereFilter.fromBundle(args);
            isAccountBlotter = args.getBoolean(BlotterFilterActivity.IS_ACCOUNT_FILTER, false);
        }
        if (savedInstanceState != null) {
            blotterFilter = WhereFilter.fromBundle(savedInstanceState);
        }
        if (saveFilter && blotterFilter.isEmpty()) {
            blotterFilter = WhereFilter.fromSharedPreferences(getContext().getSharedPreferences(this.getClass().getName(), 0));
        }

        bSearch = view.findViewById(R.id.bSearch);
        if (bSearch != null) {
            bSearch.setOnClickListener(method -> {
                EditText searchText = view.findViewById(R.id.search_text);
                FrameLayout searchLayout = view.findViewById(R.id.search_text_frame);
                ImageButton searchTextClearButton = view.findViewById(R.id.search_text_clear);
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

                searchText.setOnFocusChangeListener((v, b) -> {
                    if (!v.hasFocus()) {
                        imm.hideSoftInputFromWindow(searchLayout.getWindowToken(), 0);
                    }
                });

                searchTextClearButton.setOnClickListener(v -> {
                    searchText.setText("");
                });

                if (searchLayout.getVisibility() == View.VISIBLE) {
                    imm.hideSoftInputFromWindow(searchLayout.getWindowToken(), 0);
                    searchLayout.setVisibility(View.GONE);
                    backCallback.setEnabled(false);
                    return;
                }

                searchLayout.setVisibility(View.VISIBLE);
                searchText.requestFocusFromTouch();
                imm.showSoftInput(searchText, InputMethodManager.SHOW_IMPLICIT);
                backCallback.setEnabled(true);

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
                        blotterFilter.remove(BlotterFilter.NOTE);
                        while (blotterFilter.remove(BlotterFilter.FROM_AMOUNT) != null);
                        while (blotterFilter.remove(BlotterFilter.ORIGINAL_FROM_AMOUNT) != null);

                        if (!text.isEmpty()) {
                            Criteria amount = null;
                            Matcher m = amountSearchPattern.matcher(text);
                            if (m.matches()) {
                                if (m.group(1) == null && m.group(3) == null) {
                                    // 123.45
                                    String val = Double.toString(Math.floor(Double.parseDouble(m.group(2)) * 100));
                                    amount = Criteria.or(
                                            Criteria.or(
                                                    Criteria.eq(BlotterFilter.FROM_AMOUNT, val),
                                                    Criteria.eq(BlotterFilter.FROM_AMOUNT, "-" + val)),
                                            Criteria.or(
                                                    Criteria.eq(BlotterFilter.ORIGINAL_FROM_AMOUNT, val),
                                                    Criteria.eq(BlotterFilter.ORIGINAL_FROM_AMOUNT, "-" + val)));
                                }
                                else if (m.group(3) == null) {
                                    // >123.45, <123.45
                                    String val = Double.toString(Math.floor(Double.parseDouble(m.group(2)) * 100));
                                    if (m.group(1).equals("<")) {
                                        amount = Criteria.or(
                                                Criteria.or(
                                                        Criteria.and(
                                                                Criteria.lt(BlotterFilter.FROM_AMOUNT, val),
                                                                Criteria.gt(BlotterFilter.FROM_AMOUNT, "0")),
                                                        Criteria.and(
                                                                Criteria.gt(BlotterFilter.FROM_AMOUNT, "-" + val),
                                                                Criteria.lt(BlotterFilter.FROM_AMOUNT, "0"))
                                                ),
                                                Criteria.or(
                                                        Criteria.and(
                                                                Criteria.lt(BlotterFilter.ORIGINAL_FROM_AMOUNT, val),
                                                                Criteria.gt(BlotterFilter.ORIGINAL_FROM_AMOUNT, "0")),
                                                        Criteria.and(
                                                                Criteria.gt(BlotterFilter.ORIGINAL_FROM_AMOUNT, "-" + val),
                                                                Criteria.lt(BlotterFilter.ORIGINAL_FROM_AMOUNT, "0"))
                                                )
                                        );
                                    }
                                    else if (m.group(1).equals(">")) {
                                        amount = Criteria.or(
                                                Criteria.or(
                                                        Criteria.gt(BlotterFilter.FROM_AMOUNT, val),
                                                        Criteria.lt(BlotterFilter.FROM_AMOUNT, "-" + val)
                                                ),
                                                Criteria.or(
                                                        Criteria.gt(BlotterFilter.ORIGINAL_FROM_AMOUNT, val),
                                                        Criteria.lt(BlotterFilter.ORIGINAL_FROM_AMOUNT, "-" + val)
                                                )
                                        );
                                    }
                                }
                                else if (m.group(1) == null) {
                                    // 100~900
                                    String val2 = Double.toString(Math.floor(Double.parseDouble(m.group(2)) * 100));
                                    String val3 = Double.toString(Math.floor(Double.parseDouble(m.group(3)) * 100));
                                    amount = Criteria.or(
                                            Criteria.or(
                                                    Criteria.btw(BlotterFilter.FROM_AMOUNT, val2, val3),
                                                    Criteria.btw(BlotterFilter.FROM_AMOUNT, "-" + val3, "-" + val2)
                                            ),
                                            Criteria.or(
                                                    Criteria.btw(BlotterFilter.ORIGINAL_FROM_AMOUNT, val2, val3),
                                                    Criteria.btw(BlotterFilter.ORIGINAL_FROM_AMOUNT, "-" + val3, "-" + val2)
                                            )
                                    );
                                }
                            }
                            if (amount == null) {
                                blotterFilter.contains(BlotterFilter.NOTE, text);
                            }
                            else {
                                blotterFilter.eq(Criteria.or(
                                        amount,
                                        Criteria.like(BlotterFilter.NOTE,
                                                String.format("%%%s%%", text))
                                ));
                            }


                            clearButton.setVisibility(View.VISIBLE);
                        } else {
                            clearButton.setVisibility(View.GONE);
                        }

                        recreateCursor();
                        applyFilter();
                        saveFilter();
                    }
                });

                if (blotterFilter.get(BlotterFilter.NOTE) != null) {
                    String searchFilterText = blotterFilter.get(BlotterFilter.NOTE).getStringValue();
                    if (!searchFilterText.isEmpty()) {
                        searchFilterText = searchFilterText.substring(1, searchFilterText.length() - 1);
                        searchText.setText(searchFilterText);
                    }
                }
            });
        }

        applyFilter();
        calculateTotals();
        prepareTransactionActionGrid();
        prepareAddButtonActionGrid();

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                if (isAccountBlotter) {
                    long accountId = blotterFilter.getAccountId();
                    if (accountId != -1) {
                        // get account type
                        Account account = db.getAccount(accountId);
                        AccountType type = AccountType.valueOf(account.type);
                        if (type.isCreditCard) {
                            // Show menu for Credit Cards - bill
                            menuInflater.inflate(R.menu.ccard_blotter_menu, menu);
                        } else {
                            // Show menu for other accounts - monthly view
                            menuInflater.inflate(R.menu.blotter_menu, menu);
                        }
                    }
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                long accountId = blotterFilter.getAccountId();
                Intent intent = new Intent(getContext(), MonthlyViewActivity.class);
                intent.putExtra(MonthlyViewActivity.ACCOUNT_EXTRA, accountId);

                switch (menuItem.getItemId()) {
                    case R.id.opt_menu_month:
                        // call credit card bill activity sending account id
                        intent.putExtra(MonthlyViewActivity.BILL_PREVIEW_EXTRA, false);
                        startActivityForResult(intent, MONTHLY_VIEW_REQUEST);
                        return true;

                    case R.id.opt_menu_bill:
                        if (accountId != -1) {
                            Account account = db.getAccount(accountId);

                            // call credit card bill activity sending account id
                            if (account.paymentDay > 0 && account.closingDay > 0) {
                                intent.putExtra(MonthlyViewActivity.BILL_PREVIEW_EXTRA, true);
                                startActivityForResult(intent, BILL_PREVIEW_REQUEST);
                            } else {
                                // display message: need payment and closing day
                                AlertDialog.Builder dlgAlert = new AlertDialog.Builder(getContext());
                                dlgAlert.setMessage(R.string.statement_error);
                                dlgAlert.setTitle(R.string.ccard_statement);
                                dlgAlert.setPositiveButton(R.string.ok, null);
                                dlgAlert.setCancelable(true);
                                dlgAlert.create().show();
                            }
                        }
                        return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void showTotals() {
        Intent intent = new Intent(getContext(), BlotterTotalsDetailsActivity.class);
        blotterFilter.toIntent(intent);
        startActivityForResult(intent, -1);
    }

    protected void prepareTransactionActionGrid() {
        transactionActionGrid = new QuickActionGrid(getContext());
        transactionActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.ic_action_info, R.string.info));
        transactionActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.ic_action_edit, R.string.edit));
        transactionActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.ic_action_trash, R.string.delete));
        if (isQuickMenuShowAdditionalTransactionStatus) {
            transactionActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.ic_action_flash, R.string.transaction_status_restored));
            transactionActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.ic_action_info, R.string.transaction_status_pending));
            transactionActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.ic_action_gear, R.string.transaction_status_unreconciled));
        }
        transactionActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.ic_action_copy, R.string.duplicate));
        transactionActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.ic_action_tick, R.string.clear));
        transactionActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.ic_action_double_tick, R.string.reconcile));
        transactionActionGrid.setOnQuickActionClickListener(transactionActionListener);
    }

    private QuickActionWidget.OnQuickActionClickListener transactionActionListener = new QuickActionWidget.OnQuickActionClickListener() {
        public void onQuickActionClicked(QuickActionWidget widget, int position) {
            if (isQuickMenuShowAdditionalTransactionStatus) {
                switch (position) {
                    case 0:
                        showTransactionInfo(selectedId);
                        break;
                    case 1:
                        editTransaction(selectedId);
                        break;
                    case 2:
                        deleteTransaction(selectedId);
                        break;
                    case 3:
                        restoreTransaction(selectedId);
                        break;
                    case 4:
                        pendingTransaction(selectedId);
                        break;
                    case 5:
                        unreconcileTransaction(selectedId);
                        break;
                    case 6:
                        duplicateTransaction(selectedId, 1);
                        break;
                    case 7:
                        clearTransaction(selectedId);
                        break;
                    case 8:
                        reconcileTransaction(selectedId);
                        break;
                }
            }
            else {
                switch (position) {
                    case 0:
                        showTransactionInfo(selectedId);
                        break;
                    case 1:
                        editTransaction(selectedId);
                        break;
                    case 2:
                        deleteTransaction(selectedId);
                        break;
                    case 3:
                        duplicateTransaction(selectedId, 1);
                        break;
                    case 4:
                        clearTransaction(selectedId);
                        break;
                    case 5:
                        reconcileTransaction(selectedId);
                        break;
                }
            }
        }

    };

    private void prepareAddButtonActionGrid() {
        addButtonActionGrid = new QuickActionGrid(getContext());
        addButtonActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.actionbar_add_big, R.string.transaction));
        addButtonActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.ic_action_transfer, R.string.transfer));
        if (addTemplateToAddButton()) {
            addButtonActionGrid.addQuickAction(new MyQuickAction(getContext(), R.drawable.actionbar_tiles_large, R.string.template));
        } else {
            addButtonActionGrid.setNumColumns(2);
        }
        addButtonActionGrid.setOnQuickActionClickListener(addButtonActionListener);
    }

    protected boolean addTemplateToAddButton() {
        return true;
    }

    private QuickActionWidget.OnQuickActionClickListener addButtonActionListener = (widget, position) -> {
        switch (position) {
            case 0:
                addItem(NEW_TRANSACTION_REQUEST, TransactionActivity.class);
                break;
            case 1:
                addItem(NEW_TRANSFER_REQUEST, TransferActivity.class);
                break;
            case 2:
                createFromTemplate();
                break;
        }
    };

    private void restoreTransaction(long selectedId) {
        new BlotterOperations(getContext(), this, db, selectedId).restoreTransaction();
        recreateCursor();
    }

    private void pendingTransaction(long selectedId) {
        new BlotterOperations(getContext(), this, db, selectedId).pendingTransaction();
        recreateCursor();
    }

    private void unreconcileTransaction(long selectedId) {
        new BlotterOperations(getContext(), this, db, selectedId).unreconcileTransaction();
        recreateCursor();
    }

    private void clearTransaction(long selectedId) {
        new BlotterOperations(getContext(), this, db, selectedId).clearTransaction();
        recreateCursor();
    }

    private void reconcileTransaction(long selectedId) {
        new BlotterOperations(getContext(), this, db, selectedId).reconcileTransaction();
        recreateCursor();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        blotterFilter.toBundle(outState);
    }

    protected void createFromTemplate() {
        Intent intent = new Intent(getContext(), SelectTemplateActivity.class);
        startActivityForResult(intent, NEW_TRANSACTION_FROM_TEMPLATE_REQUEST);
    }

    @Override
    protected List<MenuItemInfo> createContextMenus(long id) {
        if (blotterFilter.isTemplate() || blotterFilter.isSchedule()) {
            return super.createContextMenus(id);
        } else {
            List<MenuItemInfo> menus = super.createContextMenus(id);
            menus.add(new MenuItemInfo(MENU_DUPLICATE, R.string.duplicate));
            menus.add(new MenuItemInfo(MENU_SAVE_AS_TEMPLATE, R.string.save_as_template));
            return menus;
        }
    }

    @Override
    public boolean onPopupItemSelected(int itemId, View view, int position, long id) {
        if (!super.onPopupItemSelected(itemId, view, position, id)) {
            switch (itemId) {
                case MENU_DUPLICATE:
                    duplicateTransaction(id, 1);
                    return true;
                case MENU_SAVE_AS_TEMPLATE:
                    new BlotterOperations(getContext(), this, db, id).duplicateAsTemplate();
                    Toast.makeText(getContext(), R.string.save_as_template_success, Toast.LENGTH_SHORT).show();
                    return true;
            }
        }
        return false;
    }

    private long duplicateTransaction(long id, int multiplier) {
        long newId = new BlotterOperations(getContext(), this, db, id).duplicateTransaction(multiplier);
        String toastText;
        if (multiplier > 1) {
            toastText = getString(R.string.duplicate_success_with_multiplier, multiplier);
        } else {
            toastText = getString(R.string.duplicate_success);
        }
        Toast.makeText(getContext(), toastText, Toast.LENGTH_LONG).show();
        recreateCursor();
        AccountWidget.updateWidgets(getContext());
        return newId;
    }

    @Override
    protected void addItem() {
        if (showAllBlotterButtons) {
            addItem(NEW_TRANSACTION_REQUEST, TransactionActivity.class);
        } else {
            addButtonActionGrid.show(bAdd);
        }
    }

    protected void addItem(int requestId, Class<? extends AbstractTransactionActivity> clazz) {
        Intent intent = new Intent(getContext(), clazz);
        long accountId = blotterFilter.getAccountId();
        if (accountId != -1) {
            intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, accountId);
        }
        else {
            long budgetId = blotterFilter.getBudgetId();
            if (budgetId != -1) {
                Budget budget = db.load(Budget.class, budgetId);
                if (budget.account != null) {
                    intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, budget.account.id);
                }
            }
        }

        intent.putExtra(TransactionActivity.TEMPLATE_EXTRA, blotterFilter.getIsTemplate());
        startActivityForResult(intent, requestId);
    }

    @Override
    protected Cursor createCursor() {
        Cursor c;
        WhereFilter blotterFilterCopy = WhereFilter.copyOf(blotterFilter);

        new Handler(Looper.getMainLooper()).post(()-> {
            emptyText.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        });

        long t1 = System.currentTimeMillis();
        if (db == null) {
            db = new DatabaseAdapter(getActivity());
            db.open();
        }
        long accountId = blotterFilterCopy.getAccountId();
        if (accountId != -1) {
            c = db.getBlotterForAccount(blotterFilterCopy);
        } else {
            c = db.getBlotter(blotterFilterCopy);
        }
        c.getCount();
        Log.d(getClass().getSimpleName(), "createCursor: " + (System.currentTimeMillis() - t1) + " ms");
        return c;
    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        ListAdapter a;
        long t1 = System.currentTimeMillis();
        long accountId = blotterFilter.getAccountId();
        if (accountId != -1) {
            a = new TransactionsListAdapter(getContext(), db, cursor);
        } else {
            a = new BlotterListAdapter(getContext(), db, cursor);
        }
        if (a.getCount() == 0) {
            emptyText.setVisibility(View.VISIBLE);
        }
        progressBar.setVisibility(View.GONE);
        Log.d(this.getTag(), "createAdapter: " + (System.currentTimeMillis() - t1) + " ms");
        return a;
    }

    @Override
    protected void deleteItem(View v, int position, final long id) {
        deleteTransaction(id);
    }

    private void deleteTransaction(long id) {
        new BlotterOperations(getContext(), this, db, id).deleteTransaction();
    }

    public void afterDeletingTransaction(long id) {
        recreateCursor();
        AccountWidget.updateWidgets(getContext());
    }

    @Override
    public void editItem(View v, int position, long id) {
        editTransaction(id);
    }

    private void editTransaction(long id) {
        new BlotterOperations(getContext(), this, db, id).editTransaction();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(getClass().getSimpleName(), "onActivityResult requestCode=" + requestCode + " resultCode=" + resultCode);

        if (requestCode == FILTER_REQUEST) {
            if (resultCode == RESULT_FIRST_USER) {
                blotterFilter.clear();
            } else if (resultCode == RESULT_OK) {
                blotterFilter = WhereFilter.fromIntent(data);
            }
            if (saveFilter) {
                saveFilter();
            }
            applyFilter();
        } else if (resultCode == RESULT_OK && requestCode == NEW_TRANSACTION_FROM_TEMPLATE_REQUEST) {
            createTransactionFromTemplate(data);
        }
        if (resultCode == RESULT_OK || resultCode == RESULT_FIRST_USER) {
            Log.d(getClass().getSimpleName(), "RESULT_OK || RESULT_FIRST_USER");
        }
        recreateCursor();
    }

    private void createTransactionFromTemplate(Intent data) {
        long templateId = data.getLongExtra(SelectTemplateFragment.TEMPLATE_ID, -1);
        int multiplier = data.getIntExtra(SelectTemplateFragment.MULTIPLIER, 1);
        boolean edit = data.getBooleanExtra(SelectTemplateFragment.EDIT_AFTER_CREATION, false);
        if (templateId > 0) {
            long id = duplicateTransaction(templateId, multiplier);
            Transaction t = db.getTransaction(id);
            if (t.fromAmount == 0 || edit) {
                new BlotterOperations(getContext(), this, db, id).asNewFromTemplate().editTransaction();
            }
        }
    }

    private void saveFilter() {
        SharedPreferences preferences = getContext().getSharedPreferences(this.getClass().getName(), 0);
        blotterFilter.toSharedPreferences(preferences);
    }

    protected void applyFilter() {
        long accountId = blotterFilter.getAccountId();
        if (accountId != -1) {
            Account a = db.getAccount(accountId);
            bAdd.setVisibility(a != null && a.isActive ? View.VISIBLE : View.GONE);
            if (showAllBlotterButtons) {
                bTransfer.setVisibility(a != null && a.isActive ? View.VISIBLE : View.GONE);
            }
        }
        String title = blotterFilter.getTitle();
        if (title != null && !title.isEmpty()) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(title);
                actionBar.setSubtitle(R.string.blotter);
            }
        }
        updateFilterImage();
    }

    protected void updateFilterImage() {
        FilterState.updateFilterColor(getContext(), blotterFilter, bFilter);
    }

    private NodeInflater inflater;
    private long selectedId = -1;

    @Override
    protected void onItemClick(View v, int position, long id) {
        if (isQuickMenuEnabledForTransaction) {
            selectedId = id;
            transactionActionGrid.show(v);
        } else {
            showTransactionInfo(id);
        }
    }

    @Override
    protected void viewItem(View v, int position, long id) {
        showTransactionInfo(id);
    }

    private void showTransactionInfo(long id) {
        TransactionInfoDialog transactionInfoView = new TransactionInfoDialog(getContext(), db, inflater);
        transactionInfoView.show(getContext(), this, id);
    }

    @Override
    public void integrityCheck() {
        new IntegrityCheckTask(getActivity()).execute(new IntegrityCheckRunningBalance(getContext(), db));
    }

    public boolean onBackPressed()
    {
        FrameLayout searchLayout = getView().findViewById(R.id.search_text_frame);
        if (searchLayout != null && searchLayout.getVisibility() == View.VISIBLE) {
            searchLayout.setVisibility(View.GONE);
            return true;
        } else {
            return false;
        }
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
}
