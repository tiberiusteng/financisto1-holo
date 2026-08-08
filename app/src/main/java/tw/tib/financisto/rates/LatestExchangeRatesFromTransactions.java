package tw.tib.financisto.rates;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.Date;
import java.util.List;

import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Currency;

public class LatestExchangeRatesFromTransactions implements ExchangeRateProvider {
    private static final String TAG = "LatestXRfromTX";
    protected Context context;

    private static final String SQL =
        "select datetime, -1 * cast(to_amount as real) / from_amount as rate from v_all_transactions " +
        "   where from_account_currency_id = ? and to_account_currency_id = ? " +
        "union " +
        "select datetime, -1 * cast(from_amount as real) / to_amount as rate from v_all_transactions " +
        "   where from_account_currency_id = ? and to_account_currency_id = ? " +
        "union " +
        "select datetime, cast(original_from_amount as real) / cast(from_amount as real) as rate from v_all_transactions " +
        "   where from_account_currency_id = ? and original_currency_id = ? " +
        "union " +
        "select datetime, cast(from_amount as real) / cast(original_from_amount as real) as rate from v_all_transactions " +
        "   where from_account_currency_id = ? and original_currency_id = ? " +
        "order by datetime desc limit 1";

    public LatestExchangeRatesFromTransactions(Context context) {
        this.context = context;
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency) {
        SQLiteDatabase db = new DatabaseAdapter(context).db();
        String f = Long.toString(fromCurrency.id), t = Long.toString(toCurrency.id);
        long t0 = System.nanoTime();
        try (Cursor c = db.rawQuery(SQL, new String[]{f, t, t, f, f, t, t, f})) {
            if (c.moveToFirst()) {
                long t1 = System.nanoTime();
                ExchangeRate r = new ExchangeRate();
                r.date = c.getLong(0);
                r.rate = c.getDouble(1);
                r.fromCurrencyId = fromCurrency.id;
                r.toCurrencyId = toCurrency.id;
                Log.d(TAG, "getRate " + fromCurrency.name + "->" + toCurrency.name + " " +
                        r.rate + " " + (new Date(r.date)) + " " + (t1 - t0) + " ns");
                return r;
            }
            return ExchangeRate.NA;
        }
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency, long atTime) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ExchangeRate> getRates(Currency homeCurrency, List<Currency> currencies) {
        throw new UnsupportedOperationException();
    }
}