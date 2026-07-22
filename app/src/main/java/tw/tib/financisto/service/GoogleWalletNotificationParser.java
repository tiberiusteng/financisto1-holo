package tw.tib.financisto.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Google Wallet tap-to-pay notifications into a payment.
 *
 * Depending on the Wallet version and locale, the amount/card line can be either
 * in the notification title or in the text, with the merchant name in the other
 * field, e.g.:
 *   title="$4.50 with Visa •••• 1234", text="STARBUCKS"
 *   title="McDonald's",                text="12,34 € · Mastercard •• 4321"
 *
 * This class is intentionally free of Android dependencies so it can be unit
 * tested on the JVM.
 */
public class GoogleWalletNotificationParser {

    /** Card reference like "•••• 1234", "••1234", "**** 1234", "xx1234" */
    private static final Pattern CARD_PATTERN =
            Pattern.compile("(?:[•∙●✱*]+|[xX]{2,})\\s*(\\d{4})\\b");

    /** Common ISO 4217 codes — an explicit list avoids treating any 3-letter merchant word as a currency */
    private static final String CURRENCY_ISO =
            "USD|EUR|GBP|UAH|RUB|PLN|CZK|JPY|CNY|INR|TRY|KRW|BRL|CAD|AUD|CHF|SEK|NOK|DKK|"
            + "HUF|RON|BGN|ILS|AED|SAR|KZT|GEL|MDL|HKD|SGD|NZD|ZAR|MXN|THB|BYN|GBX";

    private static final String CURRENCY_MARKER =
            "US\\$|R\\$|\\$|€|£|₴|₽|¥|₹|₺|₩|zł|Kč|грн\\.?|руб\\.?|(?<![A-Z])(?:" + CURRENCY_ISO + ")(?![A-Z])";

    private static final String NUMBER =
            "\\d{1,3}(?:[ \\u00A0\\u202F.,]?\\d{3})*(?:[.,]\\d{1,2})?";

    /** An amount with a currency marker before or after it */
    private static final Pattern MONEY_PATTERN = Pattern.compile(
            "(?:(" + CURRENCY_MARKER + ")\\s?)?(" + NUMBER + ")(?:\\s?(" + CURRENCY_MARKER + "))?");

    /** "$5.00 at Starbucks with Visa ••1234" — merchant embedded in the payment line */
    private static final Pattern MERCHANT_IN_PAYMENT_LINE =
            Pattern.compile("\\bat\\s+(.+?)(?=\\s+with\\b|\\s*$)", Pattern.CASE_INSENSITIVE);

    /**
     * The card/payment method the notification says the payment was made "with".
     * Wallet uses the English word "with" even in localized notifications, e.g.
     * "UAH192.18 with Сільпо" or "UAH198.00 with Visa PrivatBank ••6810".
     * A couple of localized connectors are matched defensively.
     */
    private static final Pattern CARD_LABEL_PATTERN = Pattern.compile(
            "\\b(?:with|через|карткою|з карткою|con|mit|avec)\\s+(.+?)\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Map<String, String> CURRENCY_BY_SYMBOL = new HashMap<>();
    static {
        CURRENCY_BY_SYMBOL.put("$", "USD");
        CURRENCY_BY_SYMBOL.put("US$", "USD");
        CURRENCY_BY_SYMBOL.put("€", "EUR");
        CURRENCY_BY_SYMBOL.put("£", "GBP");
        CURRENCY_BY_SYMBOL.put("₴", "UAH");
        CURRENCY_BY_SYMBOL.put("грн", "UAH");
        CURRENCY_BY_SYMBOL.put("грн.", "UAH");
        CURRENCY_BY_SYMBOL.put("₽", "RUB");
        CURRENCY_BY_SYMBOL.put("руб", "RUB");
        CURRENCY_BY_SYMBOL.put("руб.", "RUB");
        CURRENCY_BY_SYMBOL.put("¥", "JPY");
        CURRENCY_BY_SYMBOL.put("₹", "INR");
        CURRENCY_BY_SYMBOL.put("₺", "TRY");
        CURRENCY_BY_SYMBOL.put("₩", "KRW");
        CURRENCY_BY_SYMBOL.put("zł", "PLN");
        CURRENCY_BY_SYMBOL.put("Kč", "CZK");
        CURRENCY_BY_SYMBOL.put("R$", "BRL");
    }

    public static class ParsedPayment {
        public BigDecimal amount;
        /** ISO 4217 code, or null when the notification had no recognizable currency */
        public String currency;
        /** The card label as shown in Wallet, e.g. "Сільпо" or "Visa PrivatBank ••6810", or null */
        public String cardLabel;
        /** Last 4 digits of the card, when the label contains them, else null */
        public String cardLast4;
        public String merchant;
    }

    /**
     * @return parsed payment, or null if the notification is not a payment
     *         (e.g. "card added", loyalty pass updates etc.)
     */
    public static ParsedPayment parse(String title, String text) {
        title = title == null ? "" : title.trim();
        text = text == null ? "" : text.trim();

        Money titleMoney = findMoney(title);
        Money textMoney = findMoney(text);

        String paymentLine;
        String merchantLine;
        Money money;
        if (titleMoney != null && textMoney != null) {
            // both fields look like an amount (e.g. a merchant name with digits) —
            // prefer the field that carries the payment: the "with <card>" connector
            // is the strongest signal, then a card mask
            int titleScore = paymentLineScore(title);
            int textScore = paymentLineScore(text);
            if (textScore > titleScore) {
                titleMoney = null;
            } else if (titleScore > textScore) {
                textMoney = null;
            }
        }
        if (titleMoney != null) {
            money = titleMoney;
            paymentLine = title;
            merchantLine = text;
        } else if (textMoney != null) {
            money = textMoney;
            paymentLine = text;
            merchantLine = title;
        } else {
            return null;
        }

        ParsedPayment payment = new ParsedPayment();
        payment.amount = money.amount;
        payment.currency = money.currency;
        payment.cardLabel = extractCardLabel(paymentLine);
        payment.cardLast4 = findCardLast4(payment.cardLabel != null ? payment.cardLabel : "", title, text);
        payment.merchant = extractMerchant(merchantLine, paymentLine);
        return payment;
    }

    /** Higher score = more likely to be the payment line rather than the merchant line */
    private static int paymentLineScore(String s) {
        int score = 0;
        if (CARD_LABEL_PATTERN.matcher(s).find()) {
            score += 2;
        }
        if (CARD_PATTERN.matcher(s).find()) {
            score += 1;
        }
        return score;
    }

    private static String extractCardLabel(String paymentLine) {
        Matcher m = CARD_LABEL_PATTERN.matcher(paymentLine);
        if (m.find()) {
            String label = m.group(1).trim();
            // drop a trailing separator dot Wallet sometimes appends, keep card-mask dots
            label = label.replaceAll("\\s*[·・]\\s*$", "").trim();
            if (!label.isEmpty()) {
                return label;
            }
        }
        return null;
    }

    private static class Money {
        BigDecimal amount;
        String currency;
    }

    private static Money findMoney(String s) {
        if (s.isEmpty()) {
            return null;
        }
        Matcher m = MONEY_PATTERN.matcher(s);
        while (m.find()) {
            String marker = m.group(1) != null ? m.group(1) : m.group(3);
            // a bare number (e.g. card digits) is not an amount
            if (marker == null) {
                continue;
            }
            try {
                Money money = new Money();
                money.amount = SmsTransactionProcessor.toBigDecimal(m.group(2));
                money.currency = toCurrencyCode(marker);
                return money;
            } catch (NumberFormatException ignored) {
                // keep looking
            }
        }
        return null;
    }

    private static String toCurrencyCode(String marker) {
        String code = CURRENCY_BY_SYMBOL.get(marker);
        if (code != null) {
            return code;
        }
        if (marker.matches("[A-Z]{3}")) {
            return marker;
        }
        return null;
    }

    private static String findCardLast4(String... candidates) {
        for (String s : candidates) {
            if (s == null) {
                continue;
            }
            Matcher m = CARD_PATTERN.matcher(s);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }

    private static String extractMerchant(String merchantLine, String paymentLine) {
        String merchant = merchantLine.trim();
        if (merchant.regionMatches(true, 0, "at ", 0, 3)) {
            merchant = merchant.substring(3).trim();
        }
        if (merchant.isEmpty()
                || merchant.equalsIgnoreCase("Google Wallet")
                || merchant.equalsIgnoreCase("Google Pay")
                || merchant.equalsIgnoreCase("Гаманець Google")) {
            // no separate merchant field, it may be embedded in the payment line
            Matcher m = MERCHANT_IN_PAYMENT_LINE.matcher(paymentLine);
            if (m.find()) {
                return m.group(1).trim();
            }
            return null;
        }
        return merchant;
    }
}
