package tw.tib.financisto.rates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import tw.tib.financisto.http.HttpClientWrapper;
import tw.tib.financisto.model.Currency;

public class AllRatesTodayDownloaderTest {

    private static final String BASE_URL = AllRatesTodayDownloader.DEFAULT_BASE_URL;

    private static class FakeHttpClient extends HttpClientWrapper {
        final List<String> urls = new ArrayList<>();
        String response;
        Exception failure;

        FakeHttpClient() {
            super(null);
        }

        @Override
        public JSONObject getAsJson(String url) throws Exception {
            urls.add(url);
            if (failure != null) {
                throw failure;
            }
            return new JSONObject(response);
        }
    }

    private static class TestDownloader extends AllRatesTodayDownloader {
        Exception shownError;

        TestDownloader(HttpClientWrapper httpClient, String baseUrl) {
            super(httpClient, baseUrl, null);
        }

        @Override
        protected void showError(Exception e) {
            shownError = e;
        }
    }

    private FakeHttpClient http;
    private TestDownloader downloader;
    private Currency usd;
    private Currency eur;
    private Currency gbp;

    @Before
    public void setUp() {
        http = new FakeHttpClient();
        downloader = new TestDownloader(http, "");
        usd = currency(1, "USD");
        eur = currency(2, "EUR");
        gbp = currency(3, "GBP");
    }

    private static Currency currency(long id, String name) {
        Currency c = new Currency();
        c.id = id;
        c.name = name;
        c.updateExchangeRate = true;
        return c;
    }

    private static long localTime(int year, int month, int day, int hour) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month - 1, day, hour, 0, 0);
        return c.getTimeInMillis();
    }

    /* ===============================================
     * getRate at a date
     * =============================================== */

    @Test
    public void dated_rate_requests_that_date() {
        http.response = "{\"base\":\"USD\",\"date\":\"2024-03-01\",\"rates\":{\"EUR\":0.92481}}";

        ExchangeRate rate = downloader.getRate(usd, eur, localTime(2024, 3, 1, 15));

        assertEquals(BASE_URL + "/2024-03-01?base=USD&symbols=EUR", http.urls.get(0));
        assertTrue(rate.isOk());
        assertEquals(1, rate.fromCurrencyId);
        assertEquals(2, rate.toCurrencyId);
        assertEquals(0.92481, rate.rate, 0);
    }

    @Test
    public void dated_rate_keeps_the_date_of_the_table_returned() {
        // Saturday: the API answers with Friday's table
        http.response = "{\"base\":\"USD\",\"date\":\"2024-03-01\",\"rates\":{\"EUR\":0.92481}}";

        ExchangeRate rate = downloader.getRate(usd, eur, localTime(2024, 3, 2, 15));

        assertEquals(BASE_URL + "/2024-03-02?base=USD&symbols=EUR", http.urls.get(0));
        assertEquals(localTime(2024, 3, 1, 0), rate.date);
    }

    @Test
    public void dated_rate_reports_an_error_body() {
        http.response = "{\"error\":\"x\",\"message\":\"No published rates on or before 1990-03-02\"}";

        ExchangeRate rate = downloader.getRate(usd, eur, localTime(1990, 3, 2, 15));

        assertFalse(rate.isOk());
        assertEquals("No published rates on or before 1990-03-02", rate.error);
    }

    @Test
    public void dated_rate_reports_a_currency_outside_the_table() {
        http.response = "{\"base\":\"USD\",\"date\":\"2024-03-01\",\"rates\":{}}";

        ExchangeRate rate = downloader.getRate(usd, currency(4, "COP"), localTime(2024, 3, 1, 15));

        assertFalse(rate.isOk());
    }

    @Test
    public void dated_rate_reports_a_network_failure() {
        http.failure = new java.io.IOException("timeout");

        ExchangeRate rate = downloader.getRate(usd, eur, localTime(2024, 3, 1, 15));

        assertEquals("timeout", rate.error);
    }

    /* ===============================================
     * getRate, latest
     * =============================================== */

    @Test
    public void latest_rate_requests_latest() {
        http.response = "{\"base\":\"USD\",\"date\":\"2026-11-06\",\"rates\":{\"EUR\":0.89565607}}";

        ExchangeRate rate = downloader.getRate(usd, eur);

        assertEquals(BASE_URL + "/latest?base=USD&symbols=EUR", http.urls.get(0));
        assertEquals(0.89565607, rate.rate, 0);
        assertEquals(localTime(2026, 11, 6, 0), rate.date);
    }

    /* ===============================================
     * getRates
     * =============================================== */

    @Test
    public void all_rates_use_one_request_from_the_home_currency() {
        http.response = "{\"base\":\"USD\",\"date\":\"2026-11-06\",\"rates\":{\"EUR\":0.9,\"GBP\":0.75}}";

        List<ExchangeRate> rates = downloader.getRates(usd, Arrays.asList(eur, gbp));

        assertEquals(Arrays.asList(BASE_URL + "/latest?base=USD"), http.urls);
        assertEquals(2, rates.size());
        assertEquals(1, rates.get(0).fromCurrencyId);
        assertEquals(2, rates.get(0).toCurrencyId);
        assertEquals(0.9, rates.get(0).rate, 0);
        assertEquals(3, rates.get(1).toCurrencyId);
        assertEquals(0.75, rates.get(1).rate, 0);
    }

    @Test
    public void all_rates_skip_home_and_opted_out_currencies() {
        http.response = "{\"base\":\"USD\",\"date\":\"2026-11-06\",\"rates\":{\"EUR\":0.9,\"GBP\":0.75}}";
        eur.isDefault = true;
        gbp.updateExchangeRate = false;

        assertTrue(downloader.getRates(usd, Arrays.asList(eur, gbp)).isEmpty());
    }

    @Test
    public void all_rates_skip_a_currency_outside_the_table() {
        http.response = "{\"base\":\"USD\",\"date\":\"2026-11-06\",\"rates\":{\"EUR\":0.9}}";

        List<ExchangeRate> rates = downloader.getRates(usd, Arrays.asList(eur, currency(4, "COP")));

        assertEquals(1, rates.size());
        assertNull(downloader.shownError);
    }

    @Test
    public void all_rates_show_an_error_for_an_unknown_home_currency() {
        http.response = "{\"message\":\"Base currency COP is not in the ECB reference table\"}";

        assertNull(downloader.getRates(currency(4, "COP"), Arrays.asList(eur)));
        assertNotNull(downloader.shownError);
        assertEquals("Base currency COP is not in the ECB reference table", downloader.shownError.getMessage());
    }

    /* ===============================================
     * base URL
     * =============================================== */

    @Test
    public void configured_base_url_is_used() {
        http.response = "{\"date\":\"2024-03-01\",\"rates\":{\"EUR\":0.92481}}";

        new TestDownloader(http, " https://api.frankfurter.dev/v1/ ").getRate(usd, eur);

        assertEquals("https://api.frankfurter.dev/v1/latest?base=USD&symbols=EUR", http.urls.get(0));
    }

    @Test
    public void missing_base_url_falls_back_to_the_default() {
        http.response = "{\"date\":\"2024-03-01\",\"rates\":{\"EUR\":0.92481}}";

        new TestDownloader(http, null).getRate(usd, eur);

        assertEquals(BASE_URL + "/latest?base=USD&symbols=EUR", http.urls.get(0));
    }
}
