package tw.tib.financisto.activity;

import static android.app.Activity.RESULT_FIRST_USER;
import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import tw.tib.financisto.R;
import tw.tib.financisto.adapter.BudgetListAdapter;
import tw.tib.financisto.blotter.BlotterFilter;
import tw.tib.financisto.datetime.PeriodType;
import tw.tib.financisto.db.MyEntityManager;
import tw.tib.financisto.filter.Criteria;
import tw.tib.financisto.filter.DateTimeCriteria;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.db.BudgetsTotalCalculator;
import tw.tib.financisto.model.Budget;
import tw.tib.financisto.model.Total;
import tw.tib.financisto.utils.RecurUtils;
import tw.tib.financisto.utils.Utils;

public class BudgetListFragment extends AbstractListFragment<ArrayList<Budget>> {
    private static final String TAG = "BudgetListFragment";

    private static final int NEW_BUDGET_REQUEST = 1;
    private static final int EDIT_BUDGET_REQUEST = 2;
    private static final int VIEW_BUDGET_REQUEST = 3;
    private static final int FILTER_BUDGET_REQUEST = 4;

    private static final String PREF_SORT_ORDER = "sort_order";

    private ImageButton bFilter;
    private ImageButton bSortOrder;

    private WhereFilter filter = WhereFilter.empty();

    public BudgetListFragment() {
        super(R.layout.budget_list);
    }

    private Handler handler;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView totalText = view.findViewById(R.id.total);
        totalText.setOnClickListener(v -> showTotals());

        bFilter = view.findViewById(R.id.bFilter);
        bFilter.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), DateFilterActivity.class);
            filter.toIntent(intent);
            startActivityForResult(intent, FILTER_BUDGET_REQUEST);
        });

        bSortOrder = view.findViewById(R.id.bSortOrder);
        bSortOrder.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setSingleChoiceItems(
                            new ArrayAdapter<>(getContext(),
                                    android.R.layout.simple_list_item_activated_1,
                                    android.R.id.text1,
                                    getResources().getStringArray(R.array.budget_sort_order)),
                            getActivity().getSharedPreferences(TAG, MODE_PRIVATE).getInt(PREF_SORT_ORDER, 0),
                            (dialog, which) -> {
                                dialog.cancel();
                                getActivity().getSharedPreferences(TAG, MODE_PRIVATE).edit().putInt(PREF_SORT_ORDER, which).apply();
                                recreateCursor();
                            })
                    .setTitle(getString(R.string.sort_order))
                    .show();
        });

        if (filter.isEmpty()) {
            filter = WhereFilter.fromSharedPreferences(getContext().getSharedPreferences(this.getClass().getName(), 0));
        }
        if (filter.isEmpty()) {
            filter.put(new DateTimeCriteria(PeriodType.THIS_MONTH));
        }

        handler = new Handler();

        applyFilter();
    }

    private void showTotals() {
        Intent intent = new Intent(getContext(), BudgetListTotalsDetailsActivity.class);
        filter.toIntent(intent);
        startActivityForResult(intent, -1);
    }

    private void saveFilter() {
        SharedPreferences preferences = getContext().getSharedPreferences(this.getClass().getName(), 0);
        filter.toSharedPreferences(preferences);
        applyFilter();
        recreateCursor();
    }

    private void applyFilter() {
        FilterState.updateFilterColor(getContext(), filter, bFilter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILTER_BUDGET_REQUEST) {
            if (resultCode == RESULT_FIRST_USER) {
                filter.clear();
            } else if (resultCode == RESULT_OK) {
                String periodType = data.getStringExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_TYPE);
                PeriodType p = PeriodType.valueOf(periodType);
                if (PeriodType.CUSTOM == p) {
                    long periodFrom = data.getLongExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_FROM, 0);
                    long periodTo = data.getLongExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_TO, 0);
                    filter.put(new DateTimeCriteria(periodFrom, periodTo));
                } else {
                    filter.put(new DateTimeCriteria(p));
                }
            }
            saveFilter();
        }
        recreateCursor();
    }

    @Override
    protected ListAdapter createAdapter(ArrayList<Budget> budgets) {
        calculateTotals(budgets);
        return new BudgetListAdapter(getContext(), budgets);
    }

    @Override
    protected ArrayList<Budget> loadInBackground() {
        int sortOrder = getActivity().getSharedPreferences(TAG, MODE_PRIVATE).getInt(PREF_SORT_ORDER, 0);
        filter.recalculatePeriod();
        return db.getAllBudgets(filter, MyEntityManager.BudgetSortOrder.values()[sortOrder]);
    }

    private BudgetListFragment.BudgetTotalsCalculationTask totalCalculationTask;

    private void calculateTotals(ArrayList<Budget> budgets) {
        if (totalCalculationTask != null) {
            totalCalculationTask.stop();
            totalCalculationTask.cancel(true);
        }
        TextView totalText = getView().findViewById(R.id.total);
        totalCalculationTask = new BudgetListFragment.BudgetTotalsCalculationTask(totalText, budgets);
        totalCalculationTask.execute((Void[]) null);
    }

    @Override
    protected void addItem() {
        Intent intent = new Intent(getContext(), BudgetActivity.class);
        startActivityForResult(intent, NEW_BUDGET_REQUEST);
    }

    @Override
    protected void deleteItem(View v, int position, final long id) {
        final Budget b = db.load(Budget.class, id);
        if (b.parentBudgetId > 0) {
            new AlertDialog.Builder(getContext())
                    .setMessage(R.string.delete_budget_recurring_select)
                    .setPositiveButton(R.string.delete_budget_one_entry, (arg0, arg1) -> {
                        db.deleteBudgetOneEntry(id);
                        recreateCursor();
                    })
                    .setNeutralButton(R.string.delete_budget_all_entries, (arg0, arg1) -> {
                        db.deleteBudget(b.parentBudgetId);
                        recreateCursor();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            RecurUtils.Recur recur = RecurUtils.createFromExtraString(b.recur);
            new AlertDialog.Builder(getContext())
                    .setMessage(recur.interval == RecurUtils.RecurInterval.NO_RECUR ? R.string.delete_budget_confirm : R.string.delete_budget_recurring_confirm)
                    .setPositiveButton(R.string.yes, (arg0, arg1) -> {
                        db.deleteBudget(id);
                        recreateCursor();
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        }
    }

    @Override
    public void editItem(View v, int position, long id) {
        Budget b = db.load(Budget.class, id);
        RecurUtils.Recur recur = b.getRecur();
        if (recur.interval != RecurUtils.RecurInterval.NO_RECUR) {
            Toast t = Toast.makeText(getContext(), R.string.edit_recurring_budget, Toast.LENGTH_LONG);
            t.show();
        }
        Intent intent = new Intent(getContext(), BudgetActivity.class);
        intent.putExtra(BudgetActivity.BUDGET_ID_EXTRA, b.parentBudgetId > 0 ? b.parentBudgetId : id);
        startActivityForResult(intent, EDIT_BUDGET_REQUEST);
    }

    @Override
    protected void viewItem(View v, int position, long id) {
        Budget b = db.load(Budget.class, id);
        Intent intent = new Intent(getContext(), BudgetBlotterActivity.class);
        Criteria.eq(BlotterFilter.BUDGET_ID, String.valueOf(id))
                .toIntent(b.title, intent);
        startActivityForResult(intent, VIEW_BUDGET_REQUEST);
    }

    public class BudgetTotalsCalculationTask extends AsyncTask<Void, Total, Total> {

        private volatile boolean isRunning = true;

        private final TextView totalText;
        private ArrayList<Budget> budgets;

        public BudgetTotalsCalculationTask(TextView totalText, ArrayList<Budget> budgets) {
            this.budgets = budgets;
            this.totalText = totalText;
        }

        @Override
        protected Total doInBackground(Void... params) {
            try {
                BudgetsTotalCalculator c = new BudgetsTotalCalculator(db, budgets);
                c.updateBudgets(handler);
                return c.calculateTotalInHomeCurrency();
            } catch (Exception ex) {
                Log.e("BudgetTotals", "Unexpected error", ex);
                return Total.ZERO;
            }

        }

        @Override
        protected void onPostExecute(Total result) {
            if (isRunning && adapter != null) {
                Utils u = new Utils(getActivity());
                u.setTotal(totalText, result);
                ((BudgetListAdapter) adapter).notifyDataSetChanged();
            }
        }

        public void stop() {
            isRunning = false;
        }

    }
}
