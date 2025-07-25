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
package tw.tib.financisto.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.BlurMaskFilter;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import tw.tib.financisto.R;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.AccountType;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.Total;
import tw.tib.financisto.model.TotalError;
import tw.tib.financisto.rates.ExchangeRate;
import tw.tib.financisto.rates.ExchangeRateProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Utils {

    public static final BigDecimal HUNDRED = new BigDecimal(100);
    public static final String TRANSFER_DELIMITER = " \u00BB ";

    private static final int zeroColor = Resources.getSystem().getColor(android.R.color.secondary_text_dark);

    private Context context = null;

    private boolean isShowAccountBalanceOnSelector = false;

    public int positiveColor = 0;
    public int negativeColor = 0;
    public int transferColor = 0;
    public int futureColor = 0;
    public int splitColor = 0;

    public Utils(Context context) {
        if (context != null) {
            Resources r = context.getResources();
            this.positiveColor = r.getColor(R.color.positive_amount);
            this.negativeColor = r.getColor(R.color.negative_amount);
            this.transferColor = r.getColor(R.color.transfer_color);
            this.futureColor = r.getColor(R.color.future_color);
            this.splitColor = r.getColor(R.color.split_color);
            this.context = context;

            this.isShowAccountBalanceOnSelector = MyPreferences.isShowAccountBalanceOnSelector(context);
        }
    }

    public static String formatRateDate(Context context, long date) {
        return android.text.format.DateUtils.formatDateTime(context, date,
                android.text.format.DateUtils.FORMAT_SHOW_DATE | android.text.format.DateUtils.FORMAT_ABBREV_MONTH);
    }

    public static String amountToString(Currency c, long amount) {
        return amountToString(c, amount, false);
    }

    public static String amountToString(Currency c, BigDecimal amount) {
        StringBuilder sb = new StringBuilder();
        return amountToString(sb, c, amount, false).toString();
    }

    public static StringBuilder amountToString(StringBuilder sb, Currency c, long amount) {
        return amountToString(sb, c, amount, false);
    }

    public static String amountToString(Currency c, long amount, boolean addPlus) {
        StringBuilder sb = new StringBuilder();
        return amountToString(sb, c, amount, addPlus).toString();
    }

    public static StringBuilder amountToString(StringBuilder sb, Currency c, long amount, boolean addPlus) {
        return amountToString(sb, c, new BigDecimal(amount), addPlus);
    }

    public static StringBuilder amountToString(StringBuilder sb, Currency c, BigDecimal amount, boolean addPlus) {
        var sb2 = new StringBuilder();
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            if (addPlus) {
                sb2.append("+");
            }
        }
        if (c == null) {
            c = Currency.EMPTY;
        }
        String s = c.getFormat().format(amount.divide(HUNDRED));
        if (s.endsWith(".")) {
            s = s.substring(0, s.length()-1);
        }
        sb2.append(s);
        if (isNotEmpty(c.symbol)) {
            if (c.symbolFormat != null) {
                c.symbolFormat.appendSymbol(sb2, c.symbol);
            } else {
                sb2.append(" ").append(c.symbol);
            }
        }
        sb.append(sb2);
        return sb;
    }

    public static boolean checkEditText(EditText editText, String name, boolean required, int length) {
        String text = text(editText);
        if (isEmpty(text) && required) {
            editText.setError("Please specify the "+name+"..");
            return false;
        }
        if (text != null && text.length() > length) {
            editText.setError("Length of the "+name+" must not be more than "+length+" chars..");
            return false;
        }
        return true;
    }

    public static String text(EditText text) {
        String s = text.getText().toString().trim();
        return s.length() > 0 ? s : null;
    }

    public void setAmountText(TextView view, Currency c, long amount, boolean addPlus) {
        setAmountText(new StringBuilder(), view, c, amount, addPlus);
    }

    public void setAmountText(StringBuilder sb, TextView view, Currency c, long amount, boolean addPlus) {
        view.setText(amountToString(sb, c, amount, addPlus).toString());
        view.setTextColor(amount == 0 ? zeroColor : (amount > 0 ? positiveColor : negativeColor));
    }

    public void setAmountText(StringBuilder sb, TextView view, Currency originalCurrency, long originalAmount, Currency currency, long amount, boolean addPlus) {
        amountToString(sb, originalCurrency, originalAmount, addPlus);
        sb.append(" (");
        amountToString(sb, currency, amount, addPlus);
        sb.append(")");
        view.setText(sb.toString());
        view.setTextColor(amount == 0 ? zeroColor : (amount > 0 ? positiveColor : negativeColor));
    }

    public int getAmountColor(long amount) {
        return amount == 0 ? zeroColor : (amount > 0 ? positiveColor : negativeColor);
    }

    public static TextAppearanceSpan getAmountSpan(Context context, long amount) {
        return new TextAppearanceSpan(context,
                amount == 0
                        ? R.style.TextAppearance_ZeroAmount
                        : (amount > 0 ? R.style.TextAppearance_PositiveAmount : R.style.TextAppearance_NegativeAmount));
    }

    public static int moveCursor(Cursor cursor, String idColumnName, long id) {
        int pos = cursor.getColumnIndexOrThrow(idColumnName);
        if (cursor.moveToFirst()) {
            do {
                if (cursor.getLong(pos) == id) {
                    return cursor.getPosition();
                }
            } while(cursor.moveToNext());
        }
        return -1;
    }

    public static String locationToText(String provider, double latitude, double longitude, float accuracy, String resolvedAddress) {
        StringBuilder sb = new StringBuilder();
        sb.append(provider).append(" (");
        if (resolvedAddress != null) {
            sb.append(resolvedAddress);
        } else {
            sb.append("Lat: ").append(Location.convert(latitude, Location.FORMAT_DEGREES)).append(", ");
            sb.append("Lon: ").append(Location.convert(longitude, Location.FORMAT_DEGREES));
            if (accuracy > 0) {
                sb.append(", ");
                sb.append("±").append(String.format("%.2f", accuracy)).append("m");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    public static void setEnabled(ViewGroup layout, boolean enabled) {
        int count = layout.getChildCount();
        for (int i=0; i<count; i++) {
            layout.getChildAt(i).setEnabled(enabled);
        }
    }

    public static boolean isNotEmpty(String s) {
        return s != null && s.length() > 0;
    }

    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static boolean isEmpty(EditText e) {
        return isEmpty(text(e));
    }

    public static PackageInfo getPackageInfo(Context context) throws NameNotFoundException {
        PackageManager manager = context.getPackageManager();
        return manager.getPackageInfo(context.getPackageName(), 0);
    }

    public void setTransferTitleText(TextView textView, Account fromAccount, Account toAccount) {
        setTransferTitleText(textView, fromAccount.title, toAccount.title);
    }

    public void setTransferTitleText(TextView textView, String fromAccountTitle, String toAccountTitle) {
        textView.setText(getTransferTitleText(fromAccountTitle, toAccountTitle));
        setTransferTextColor(textView);
    }

    public String getTransferTitleText(Account fromAccount, Account toAccount) {
        return getTransferTitleText(fromAccount.title, toAccount.title);
    }

    public String getTransferTitleText(String fromAccountTitle, String toAccountTitle) {
        var sb = new StringBuilder();
        sb.append(fromAccountTitle).append(TRANSFER_DELIMITER).append(toAccountTitle);
        return sb.toString();
    }

    public void setFutureTextColor(TextView textView) {
        textView.setTextColor(futureColor);
    }

    public void setTransferAmountText(TextView textView, Currency fromCurrency, long fromAmount, Currency toCurrency, long toAmount) {
        textView.setText(getTransferAmountText(fromCurrency, fromAmount, toCurrency, toAmount));
        textView.setTextColor(zeroColor);
    }

    public String getTransferAmountText(Currency fromCurrency, long fromAmount, Currency toCurrency, long toAmount) {
        var sb = new StringBuilder();
        if (sameCurrency(fromCurrency, toCurrency)) {
            Utils.amountToString(sb, toCurrency, toAmount);
        } else {
            Utils.amountToString(sb, fromCurrency, Math.abs(fromAmount)).append(TRANSFER_DELIMITER);
            Utils.amountToString(sb, toCurrency, toAmount);
        }
        return sb.toString();
    }

    public static boolean sameCurrency(Currency fromCurrency, Currency toCurrency) {
        return fromCurrency.id == toCurrency.id;
    }

    public void setTransferBalanceText(TextView textView, Currency fromCurrency, long fromBalance, Currency toCurrency, long toBalance) {
        var sb = new StringBuilder();
        Utils.amountToString(sb, fromCurrency, fromBalance, false).append(TRANSFER_DELIMITER);
        Utils.amountToString(sb, toCurrency, toBalance, false);
        textView.setText(sb.toString());
    }

    public void setTransferTextColor(TextView textView) {
        textView.setTextColor(transferColor);
    }

    public void setNegativeColor(TextView textView) {
        textView.setTextColor(negativeColor);
    }

    public void setPositiveColor(TextView textView) {
        textView.setTextColor(positiveColor);
    }

    public void setTotal(TextView totalText, Total total) {
        if (total.isError()) {
            setTotalError(totalText);
        } else {
            setAmountText(totalText, total);
            totalText.setError(null);
        }
    }

    public void setAmountText(TextView totalText, Total total) {
        if (total.showAmount) {
            setAmountTextWithTwoAmounts(totalText, total.currency, total.amount, total.balance);
        } else if (total.showIncomeExpense) {
            setAmountTextWithTwoAmounts(totalText, total.currency, total.income, total.expenses);
        } else {
            setAmountText(totalText, total.currency, total.balance, false);
        }
    }

    private void setAmountTextWithTwoAmounts(TextView textView, Currency c, long amount1, long amount2) {
        if (context == null) return;
        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append(Utils.amountToString(c, amount1, false));
        int x = sb.length();
        sb.setSpan(getAmountSpan(context, amount1), 0, x, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        sb.append(" | ");
        sb.append(Utils.amountToString(c, amount2, false));
        sb.setSpan(getAmountSpan(context, amount2), x+3, sb.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        textView.setText(sb, TextView.BufferType.NORMAL);
    }

    private void setTotalError(TextView totalText) {
        totalText.setText(R.string.not_available);
        totalText.setTextColor(context.getResources().getColor(R.color.holo_red_light));
        Drawable dr = context.getResources().getDrawable(R.drawable.total_error);
        dr.setBounds(0, 0, dr.getIntrinsicWidth(), dr.getIntrinsicHeight());
        totalText.setError(totalText.getText(), dr);
    }

    public static void openSoftKeyboard(EditText textEdit, Context context) { // https://stackoverflow.com/a/8080621/365675
        textEdit.requestFocusFromTouch();
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(textEdit, InputMethodManager.SHOW_IMPLICIT);
    }

    public static void closeSoftKeyboard(View view, Context context) {
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

    }

    public void setAccountTitleBalance(
            Account a, TextView accountText, TextView accountBalanceText, TextView accountLimitText
    ) {
        if (isShowAccountBalanceOnSelector) {
            long amount = a.totalAmount;
            AccountType type = AccountType.valueOf(a.type);
            if (type == AccountType.CREDIT_CARD && a.limitAmount != 0) {
                long limitAmount = Math.abs(a.limitAmount);
                long balance = limitAmount + amount;
                accountLimitText.setVisibility(View.VISIBLE);
                setAmountText(accountBalanceText, a.currency, amount, false);
                setAmountText(accountLimitText, a.currency, balance, false);
            } else {
                accountLimitText.setVisibility(View.GONE);
                setAmountText(accountBalanceText, a.currency, amount, false);
            }
            accountBalanceText.setVisibility(View.VISIBLE);
        }
        accountText.setText(a.title);
    }

    public Total calculateTotalInCurrency(Total[] totals, ExchangeRateProvider rates, Currency inCurrency) {
        BigDecimal total = BigDecimal.ZERO;
        for (Total item : totals) {
            if (item.currency.id == inCurrency.id) {
                total = total.add(BigDecimal.valueOf(item.balance));
            }
            else {
                ExchangeRate rate = rates.getRate(item.currency, inCurrency);
                if (rate == ExchangeRate.NA) {
                    return new Total(inCurrency, TotalError.lastRateError(item.currency));
                } else {
                    total = total.add(BigDecimal.valueOf(rate.rate * item.balance));
                }
            }
        }
        Total result = new Total(inCurrency);
        result.balance = total.longValue();
        return result;
    }

    public static void applyBlur(TextView textView) {
        float radius = textView.getTextSize() / 3.0f;
        BlurMaskFilter filter = new BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL);
        textView.getPaint().setMaskFilter(filter);
    }

    public static long roundAmount(Context context, Currency currency, long amount) {
        if (MyPreferences.isRoundUpAmount(context) && currency != null) {
            long sign, absAmount;
            if (amount < 0) {
                sign = -1;
                absAmount = amount * -1;
            }
            else {
                sign = 1;
                absAmount = amount;
            }
            BigDecimal bd = new BigDecimal(absAmount).setScale(2, RoundingMode.UNNECESSARY);
            BigDecimal hundred = new BigDecimal(100);

            bd = bd.divide(hundred, RoundingMode.UNNECESSARY);
            bd = bd.setScale(currency.decimals, RoundingMode.HALF_UP);
            bd = bd.multiply(hundred);

            return bd.longValue() * sign;
        }
        return amount;
    }
}
