package tw.tib.financisto.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import tw.tib.financisto.R;
import tw.tib.financisto.adapter.BlotterListAdapter;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.utils.EnumUtils;
import tw.tib.financisto.utils.LocalizableEnum;

import java.util.Arrays;

public class MassOpFragment extends BlotterFragment {

    public MassOpFragment() {
        super(R.layout.blotter_mass_op);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        bFilter = view.findViewById(R.id.bFilter);
        bFilter.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), BlotterFilterActivity.class);
            blotterFilter.toIntent(intent);
            startActivityForResult(intent, FILTER_REQUEST);
        });

        ImageButton bCheckAll = view.findViewById(R.id.bCheckAll);
        bCheckAll.setOnClickListener(arg0 -> {
            var adapter = (BlotterListAdapter) getListAdapter();
            if (adapter != null) adapter.checkAll();
        });

        ImageButton bUncheckAll = view.findViewById(R.id.bUncheckAll);
        bUncheckAll.setOnClickListener(arg0 -> {
            var adapter = (BlotterListAdapter)getListAdapter();
            if (adapter != null) adapter.uncheckAll();
        });

        final MassOp[] operations = MassOp.values();
        final Spinner spOperation = view.findViewById(R.id.spOperation);
        Button proceed = view.findViewById(R.id.proceed);
        proceed.setOnClickListener(v -> {
            MassOp op = operations[spOperation.getSelectedItemPosition()];
            applyMassOp(op);
        });
        Bundle args = getArguments();
        if (args != null) {
            blotterFilter = WhereFilter.fromBundle(args);
            applyFilter();
        }
        spOperation.setPrompt(getString(R.string.mass_operations));
        spOperation.setAdapter(new SpinnerAdapter() {
            private LayoutInflater inflater;

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                return getView(position, convertView, parent);
            }

            @Override
            public void registerDataSetObserver(DataSetObserver observer) {

            }

            @Override
            public void unregisterDataSetObserver(DataSetObserver observer) {

            }

            @Override
            public int getCount() {
                return operations.length;
            }

            @Override
            public Object getItem(int position) {
                return operations[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return false;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                Context context = getContext();
                if (inflater == null) inflater = LayoutInflater.from(context);
                View view = inflater.inflate(R.layout.mass_op_action_item, parent, false);
                TextView indicator = view.findViewById(R.id.indicator);
                TextView title = view.findViewById(R.id.center);
                title.setText(context.getString(operations[position].getTitleId()));
                indicator.setBackgroundColor(ContextCompat.getColor(context, operations[position].getColor()));
                return view;
            }

            @Override
            public int getItemViewType(int position) {
                return 0;
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        });
        prepareTransactionActionGrid();

        emptyText = view.findViewById(android.R.id.empty);
        progressBar = view.findViewById(android.R.id.progress);
    }

    protected void applyMassOp(final MassOp op) {
        BlotterListAdapter adapter = (BlotterListAdapter)getListAdapter();
        int count = 0;

        if (adapter != null) {
            count = adapter.getCheckedCount();
        }

        if (count > 0) {
            new AlertDialog.Builder(getContext())
                    .setMessage(getString(R.string.apply_mass_op, getString(op.getTitleId()), count))
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            long[] ids = adapter.getAllCheckedIds();
                            Log.d("Financisto", "Will apply "+op+" on "+Arrays.toString(ids));
                            op.apply(db, ids);
                            adapter.uncheckAll();
                            recreateCursor();
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        } else {
            Toast.makeText(getContext(), R.string.apply_mass_op_zero_count, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void applyFilter() {
        updateFilterImage();
    }

    @Override
    protected void calculateTotals(WhereFilter filter) {
        // do nothing
    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        if (cursor.getCount() == 0) {
            emptyText.setVisibility(View.VISIBLE);
        }
        progressBar.setVisibility(View.GONE);
        return new BlotterListAdapter(getContext(), db, R.layout.blotter_mass_op_list_item, cursor, true);
    }

    private enum MassOp implements LocalizableEnum{
        PENDING(R.string.mass_operations_mark_pending_all) {
            @Override
            public void apply(DatabaseAdapter db, long[] ids) {
                db.markPendingSelectedTransactions(ids);
            }
            @Override
            public int getColor() {
                return R.color.pending_transaction_color;
            }
        },
        RESTORED(R.string.mass_operations_mark_restored_all) {
            @Override
            public void apply(DatabaseAdapter db, long[] ids) {
                db.markRestoredSelectedTransactions(ids);
            }
            @Override
            public int getColor() {
                return R.color.restored_transaction_color;
            }
        },
        UNRECONCILED(R.string.mass_operations_mark_unreconciled_all) {
            @Override
            public void apply(DatabaseAdapter db, long[] ids) {
                db.markUnreconciledSelectedTransactions(ids);
            }
            @Override
            public int getColor() {
                return R.color.unreconciled_transaction_color;
            }
        },
        CLEAR(R.string.mass_operations_clear_all){
            @Override
            public void apply(DatabaseAdapter db, long[] ids) {
                db.clearSelectedTransactions(ids);
            }
            @Override
            public int getColor() {
                return R.color.cleared_transaction_color;
            }
        },
        RECONCILE(R.string.mass_operations_reconcile){
            @Override
            public void apply(DatabaseAdapter db, long[] ids) {
                db.reconcileSelectedTransactions(ids);
            }
            @Override
            public int getColor() {
                return R.color.reconciled_transaction_color;
            }
        },
        DELETE(R.string.mass_operations_delete){
            @Override
            public void apply(DatabaseAdapter db, long[] ids) {
                db.deleteSelectedTransactions(ids);
                db.rebuildRunningBalances();
            }
            @Override
            public int getColor() {
                return R.color.holo_red_dark;
            }
        };

        private final int titleId;

        MassOp(int titleId) {
            this.titleId = titleId;
        }

        public abstract int getColor();
        public abstract void apply(DatabaseAdapter db, long[] ids);

        @Override
        public int getTitleId() {
            return titleId;
        }
    }

}
