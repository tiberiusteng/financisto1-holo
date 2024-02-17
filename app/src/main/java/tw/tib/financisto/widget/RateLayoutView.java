package tw.tib.financisto.widget;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.sql.Date;
import java.util.concurrent.Executors;

import tw.tib.financisto.R;
import tw.tib.financisto.activity.AbstractActivity;
import tw.tib.financisto.activity.ActivityLayout;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.rates.ExchangeRate;
import tw.tib.financisto.rates.ExchangeRateProvider;
import tw.tib.financisto.utils.MyPreferences;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 6/24/11 6:45 PM
 */
public class RateLayoutView implements RateNodeOwner {
    private static final String TAG = "RateLayoutView";

    private final AbstractActivity activity;
    private final ActivityLayout x;
    private final LinearLayout layout;

    private AmountInput amountInputFrom;
    private AmountInput amountInputTo;

    private RateNode rateNode;
    private View amountInputFromNode;
    private View amountInputToNode;
    private int amountFromTitleId;
    private int amountToTitleId;

    private AmountInput.OnAmountChangedListener amountFromChangeListener;
    private AmountInput.OnAmountChangedListener amountToChangeListener;

    private Currency currencyFrom;
    private Currency currencyTo;

    public RateLayoutView(AbstractActivity activity, ActivityLayout x, LinearLayout layout) {
        this.activity = activity;
        this.x = x;
        this.layout = layout;
    }

    public void setAmountFromChangeListener(AmountInput.OnAmountChangedListener amountFromChangeListener) {
        this.amountFromChangeListener = amountFromChangeListener;
    }

    public void setAmountToChangeListener(AmountInput.OnAmountChangedListener amountToChangeListener) {
        this.amountToChangeListener = amountToChangeListener;
    }

    private void createUI(int fromAmountTitleId, int toAmountTitleId) {
        //amount from
        amountInputFrom = AmountInput_.build(activity);
        amountInputFrom.setOwner(activity);
        amountInputFrom.setExpense();
        amountFromTitleId = fromAmountTitleId;
        amountInputFromNode = x.addEditNode(layout, fromAmountTitleId, amountInputFrom);
        //amount to & rate
        amountInputTo = AmountInput_.build(activity);
        amountInputTo.setOwner(activity);
        amountInputTo.setIncome();
        amountToTitleId = toAmountTitleId;
        amountInputToNode = x.addEditNode(layout, toAmountTitleId, amountInputTo);
        amountInputTo.setOnAmountChangedListener((oldAmount, newAmount) -> {
            checkIfRateConsistent();
            if (amountToChangeListener != null) {
                amountToChangeListener.onAmountChanged(oldAmount, newAmount);
            }
        });
        amountInputTo.setOnRequestAssignListener(() -> {
            double r = rateNode.getRate();
            long amountFrom = amountInputFrom.getAmount();
            long amountTo = (long)Math.floor(r*amountFrom);
            amountInputTo.disableAmountChangedListener();
            amountInputTo.setAmount(amountTo);
            amountInputTo.enableAmountChangedListener();
            rateNode.markRateConsistent();
        });
        amountInputFrom.setOnAmountChangedListener((oldAmount, newAmount) -> {
            checkIfRateConsistent();
            if (amountInputFrom.isIncomeExpenseEnabled()) {
                if (amountInputFrom.isExpense()) {
                    amountInputTo.setExpense();
                } else {
                    amountInputTo.setIncome();
                }
            }
            if (amountFromChangeListener != null) {
                amountFromChangeListener.onAmountChanged(oldAmount, newAmount);
            }
        });
        amountInputFrom.setOnRequestAssignListener(() -> {
            double r = rateNode.getRate();
            long amountTo = amountInputTo.getAmount();
            long amountFrom = (long)Math.floor((1.0/r)*amountTo);
            amountInputFrom.disableAmountChangedListener();
            amountInputFrom.setAmount(amountFrom);
            amountInputFrom.enableAmountChangedListener();
            rateNode.markRateConsistent();
        });
        AbstractActivity.setVisibility(amountInputToNode, View.GONE);
        rateNode = new RateNode(this, activity, x, layout);
        AbstractActivity.setVisibility(rateNode.rateInfoNode, View.GONE);

        if (MyPreferences.isSetFocusOnAmountField(activity)) {
            amountInputFrom.requestFocusFromTouch();
            activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    public void createTransferUI() {
        createUI(R.string.amount_from, R.string.amount_to);
        amountInputFrom.disableIncomeExpenseButton();
        amountInputTo.disableIncomeExpenseButton();
    }

    public void createTransactionUI() {
        createUI(R.string.amount, R.string.amount);
        amountInputTo.disableIncomeExpenseButton();
    }

    public void setIncome() {
        amountInputFrom.setIncome();
        amountInputTo.setIncome();
    }

    public void setExpense() {
        amountInputFrom.setExpense();
        amountInputTo.setExpense();
    }

    public void selectCurrencyFrom(Currency currency) {
        if (currencyFrom != null && currencyFrom.id == currency.id) return;
        currencyFrom = currency;
        amountInputFrom.setCurrency(currencyFrom);
        updateTitle(amountInputFromNode, amountFromTitleId, currencyFrom);
        checkNeedRate();
    }

    public void selectCurrencyTo(Currency currency) {
        if (currencyTo != null && currencyTo.id == currency.id) return;
        currencyTo = currency;
        amountInputTo.setCurrency(currencyTo);
        updateTitle(amountInputToNode, amountToTitleId, currencyTo);
        checkNeedRate();
    }

    private void updateTitle(View node, int titleId, Currency currency) {
        TextView title = node.findViewById(R.id.label);
        if (currency != null && currency.id > 0) {
            title.setText(activity.getString(titleId)+" ("+currency.name+")");
        } else {
            title.setText(activity.getString(titleId));
        }
    }

    private void checkNeedRate() {
        if (isDifferentCurrencies()) {
            amountInputFrom.showAssignButton();
            AbstractActivity.setVisibility(rateNode.rateInfoNode, View.VISIBLE);
            AbstractActivity.setVisibility(amountInputToNode, View.VISIBLE);
            amountInputFrom.showRateInfo();
            amountInputTo.showRateInfo();
            getLatestRate();
        } else {
            amountInputFrom.hideAssignButton();
            amountInputFrom.hideRateInfo();
            AbstractActivity.setVisibility(rateNode.rateInfoNode, View.GONE);
            AbstractActivity.setVisibility(amountInputToNode, View.GONE);
        }
    }

    private void getLatestRate() {
        Executors.newSingleThreadExecutor().execute(() -> {
            ExchangeRateProvider latestExchangeRates = new DatabaseAdapter(activity).getLatestRates();
            //ExchangeRateProvider latestExchangeRates = new LatestExchangeRatesFromTransactions(activity);
            ExchangeRate exchangeRate = latestExchangeRates.getRate(currencyFrom, currencyTo);
            Log.d(TAG, "getLatestRate " + currencyFrom.name + "->" + currencyTo.name + " " +
                    exchangeRate.rate + " " + exchangeRate.is_flip + " " + (new Date(exchangeRate.date)));
            if (exchangeRate != ExchangeRate.NA) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    rateNode.setRate(exchangeRate);
                    checkIfRateConsistent();
                });
            }
        });
    }

    private void calculateRate() {
        long amountFrom = amountInputFrom.getAmount();
        long amountTo = amountInputTo.getAmount();
        float r = 1.0f*amountTo/amountFrom;
        if (!Float.isNaN(r)) {
            rateNode.setRate(r);
        }
    }

    public long getFromAmount() {
        return amountInputFrom.getAmount();
    }

    public long getToAmount() {
        if (isDifferentCurrencies()) {
            return amountInputTo.getAmount();
        } else {
            return -amountInputFrom.getAmount();
        }
    }

    private boolean isDifferentCurrencies() {
        return currencyFrom != null && currencyTo != null && currencyFrom.id != currencyTo.id;
    }

    public void clearFromAmount() {
        amountInputFrom.clearAmount();
    }

    public void setFromAmount(long fromAmount) {
        amountInputFrom.setAmount(fromAmount);
        calculateRate();
        amountInputFrom.primary.selectAll();
    }

    public void setToAmount(long toAmount) {
        amountInputTo.setAmount(toAmount);
        calculateRate();
    }

    private void updateToAmountFromRate() {
        double r = rateNode.getRate();
        long amountFrom = amountInputFrom.getAmount();
        long amountTo = (long)Math.floor(r*amountFrom);
        if (amountInputTo.getAmount() == 0) {
            amountInputTo.disableAmountChangedListener();
            amountInputTo.setAmount(amountTo);
            amountInputTo.enableAmountChangedListener();
        }
        else {
            // if there is input, don't overwrite it
            checkIfRateConsistent();
        }
    }

    public void openFromAmountCalculator() {
        amountInputFrom.openCalculator();
    }

    @Override
    public void onBeforeRateDownload() {
        amountInputFrom.setEnabled(false);
        amountInputTo.setEnabled(false);
        rateNode.disableAll();
    }

    @Override
    public Currency getCurrencyFrom() {
        return currencyFrom;
    }

    public long getCurrencyFromId() {
        return currencyFrom != null ? currencyFrom.id : 0;
    }

    @Override
    public Currency getCurrencyTo() {
        return currencyTo;
    }

    public long getCurrencyToId() {
        return currencyTo != null ? currencyTo.id : 0;
    }

    @Override
    public void onAfterRateDownload() {
        amountInputFrom.setEnabled(true);
        amountInputTo.setEnabled(true);
        rateNode.enableAll();
    }

    @Override
    public void onSuccessfulRateDownload() {
        updateToAmountFromRate();
    }

    @Override
    public void onRateChanged() {
        checkIfRateConsistent();
    }

    protected void checkIfRateConsistent() {
        long amountFrom = amountInputFrom.getAmount();
        long amountTo = amountInputTo.getAmount();
        if (amountFrom != 0 && amountTo != 0) {
            double rate = 1.0d * amountTo / amountFrom;
            String rateInfo = rateNode.checkIfRateConsistent(rate);
            amountInputFrom.setRateInfo(activity.getString(R.string.rate_info, currencyFrom.name, rateInfo, currencyTo.name));
            String inverseRateInfo = rateNode.formatRate(1.0d / rate);
            amountInputTo.setRateInfo(activity.getString(R.string.rate_info, currencyTo.name, inverseRateInfo, currencyFrom.name));
        }
        else {
            amountInputFrom.setRateInfo("");
            amountInputTo.setRateInfo("");
        }
    }

    @Override
    public void onRequestAssign() {
        calculateRate();
    }

    @Override
    public Activity getActivity() {
        return activity;
    }

    public void selectSameCurrency(Currency currency) {
        selectCurrencyFrom(currency);
        selectCurrencyTo(currency);
    }

}
