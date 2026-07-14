package tw.tib.financisto.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import tw.tib.financisto.R;
import tw.tib.financisto.datetime.DateUtils;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.Transaction;
import tw.tib.financisto.model.TransactionStatus;
import tw.tib.financisto.utils.CurrencyCache;
import tw.tib.financisto.utils.EnumUtils;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.Utils;

import static tw.tib.financisto.utils.Utils.text;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 4/21/11 7:17 PM
 */
public abstract class AbstractSplitActivity extends AbstractActivity {

    private static final TransactionStatus[] statuses = TransactionStatus.values();
    protected Calendar dateTime;
    protected ImageButton status;
    protected Button dateText;
    protected Button timeText;
    protected DateFormat df;
    protected DateFormat tf;
    protected TextView editDisabled;

    protected EditText noteText;
    protected TextView unsplitAmountText;

    protected Account fromAccount;
    protected Currency originalCurrency;
    protected Utils utils;
    protected Transaction split;
    protected long splitParentAccountId;

    protected LocationSelector<AbstractSplitActivity> locationSelector;
    protected ProjectSelector<AbstractSplitActivity> projectSelector;

    private final int layoutId;

    protected AbstractSplitActivity(int layoutId) {
        this.layoutId = layoutId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layoutId);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.split_fixed), (vi, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            var lp = (ViewGroup.MarginLayoutParams) vi.getLayoutParams();
            lp.topMargin = insets.top;
            lp.bottomMargin = insets.bottom;
            vi.setLayoutParams(lp);
            return WindowInsetsCompat.CONSUMED;
        });

        // todo.mb: check selector here
        locationSelector = new LocationSelector<>(this, db, x);
        projectSelector = new ProjectSelector<>(this, db, x);

        utils  = new Utils(this);
        Intent intent = getIntent();
        split = Transaction.fromIntentAsSplit(intent);
        splitParentAccountId = intent.getLongExtra(TransactionActivity.SPLIT_PARENT_ACCOUNT, 0);
        if (split.fromAccountId > 0) {
            fromAccount = db.getAccount(split.fromAccountId);
        }
        if (split.originalCurrencyId > 0) {
            originalCurrency = CurrencyCache.getCurrency(split.originalCurrencyId);
        }

        status = findViewById(R.id.status);
        status.setOnClickListener(v -> {
            ArrayAdapter<String> adapter = EnumUtils.createDropDownAdapter(AbstractSplitActivity.this, statuses);
            x.selectPosition(AbstractSplitActivity.this, R.id.status, R.string.transaction_status, adapter, split.status.ordinal());
        });

        df = DateUtils.getLongDateFormat(this);
        tf = DateUtils.getTimeFormat(this);
        dateTime = Calendar.getInstance();
        dateTime.setTime(new Date(split.dateTime));
        dateText = findViewById(R.id.date);
        dateText.setText(df.format(dateTime.getTime()));
        timeText = findViewById(R.id.time);
        timeText.setText(tf.format(dateTime.getTime()));

        LinearLayout layout = findViewById(R.id.list);

        editDisabled = findViewById(R.id.edit_disabled);

        createUI(layout);
        createCommonUI(layout);
        // ensure views are created before start loading
        // such that loading would never finish before view available
        fetchData();
        projectSelector.fetchEntities();
        locationSelector.fetchEntities();
        updateUI();
        selectStatus(split.status);
    }

    private void createCommonUI(LinearLayout layout) {
        unsplitAmountText = x.addInfoNode(layout, R.id.add_split, R.string.unsplit_amount, "0");

        int locationOrder = MyPreferences.getLocationOrder();
        int noteOrder = MyPreferences.getNoteOrder();
        int projectOrder = MyPreferences.getProjectOrder();

        for (int i = 0; i < 6; i++) {
            if (i == noteOrder) {
                noteText = new EditText(this);
                x.addEditNode(layout, R.string.note, noteText);
            }
            if (i == projectOrder) {
                projectSelector.createNode(layout);
            }
            if (i == locationOrder) {
                locationSelector.createNode(layout);
            }
        }

        Button bSave = findViewById(R.id.bSave);
        bSave.setOnClickListener(arg0 -> saveAndFinish());

        Button bCancel = findViewById(R.id.bCancel);
        bCancel.setOnClickListener(arg0 -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }

    protected void updateCommonUIforPreventEditing() {
        boolean enabled = !isPreventEditing();
        if (noteText != null) noteText.setEnabled(enabled);
        if (projectSelector != null) projectSelector.setEnabled(enabled);
        if (locationSelector != null) locationSelector.setEnabled(enabled);

        if (enabled) {
            editDisabled.setVisibility(View.GONE);
        }
        else {
            editDisabled.setVisibility(View.VISIBLE);
        }

        updateUIforPreventEditing();
    }

    protected void updateUIforPreventEditing() {}

    protected abstract void fetchData();

    protected abstract void createUI(LinearLayout layout);

    @Override
    protected void onClick(View v, int id) {
        locationSelector.onClick(id);
        projectSelector.onClick(id);
    }

    @Override
    public void onSelectedPos(int id, int selectedPos) {
        locationSelector.onSelectedPos(id, selectedPos);
        projectSelector.onSelectedPos(id, selectedPos);
        if (id ==  R.id.status) {
            selectStatus(statuses[selectedPos]);
        }
    }

    @Override
    public void onSelectedId(int id, long selectedId) {
        locationSelector.onSelectedId(id, selectedId);
        projectSelector.onSelectedId(id, selectedId);
    }

    private void selectStatus(TransactionStatus transactionStatus) {
        split.status = transactionStatus;
        status.setImageResource(transactionStatus.iconId);
        updateCommonUIforPreventEditing();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        locationSelector.onActivityResult(requestCode, resultCode, data);
        projectSelector.onActivityResult(requestCode, resultCode, data);
    }

    private void saveAndFinish() {
        Intent data = new Intent();
        if (updateFromUI()) {
            split.toIntentAsSplit(data);
            setResult(Activity.RESULT_OK, data);
            finish();
        }
    }

    protected boolean updateFromUI() {
        split.note = text(noteText);
        split.locationId = locationSelector.getSelectedEntityId();
        split.projectId = projectSelector.getSelectedEntityId();
        return true;
    }

    protected void updateUI() {
        locationSelector.selectEntity(split.locationId);
        projectSelector.selectEntity(split.projectId);
        setNote(split.note);
    }

    private void setNote(String note) {
        noteText.setText(note);
    }

    protected void setUnsplitAmount(long amount) {
        Currency currency = getCurrency();
        utils.setAmountText(unsplitAmountText, currency, amount, false);
    }

    protected Currency getCurrency() {
        return originalCurrency != null ? originalCurrency : (fromAccount != null ? fromAccount.currency : Currency.defaultCurrency());
    }

    protected boolean isPreventEditing() {
        return MyPreferences.isPreventEditClearedReconciledTransactions() &&
                (split.status == TransactionStatus.CL ||
                 split.status == TransactionStatus.RC);
    }

    @Override
    protected boolean shouldLock() {
        return MyPreferences.isPinProtectedNewTransaction();
    }

    @Override
    protected void onDestroy() {
        if (locationSelector != null) locationSelector.onDestroy();
        if (projectSelector != null) projectSelector.onDestroy();
        super.onDestroy();
    }
}
