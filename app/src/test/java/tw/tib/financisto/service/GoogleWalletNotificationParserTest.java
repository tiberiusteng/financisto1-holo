package tw.tib.financisto.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.math.BigDecimal;

import tw.tib.financisto.service.GoogleWalletNotificationParser.ParsedPayment;

public class GoogleWalletNotificationParserTest {

    @Test
    public void amountInTitle_merchantInText() {
        ParsedPayment p = GoogleWalletNotificationParser.parse(
                "$4.50 with Visa •••• 1234", "STARBUCKS");
        assertNotNull(p);
        assertEquals(new BigDecimal("4.50"), p.amount);
        assertEquals("USD", p.currency);
        assertEquals("1234", p.cardLast4);
        assertEquals("STARBUCKS", p.merchant);
        assertEquals("Visa •••• 1234", p.cardLabel);
    }

    // Real Google Wallet format observed on a UA device:
    // title = merchant, text = "<CUR><amount> with <card>"

    @Test
    public void realFormat_namedCard() {
        ParsedPayment p = GoogleWalletNotificationParser.parse(
                "VM33", "UAH192.18 with Сільпо");
        assertNotNull(p);
        assertEquals(new BigDecimal("192.18"), p.amount);
        assertEquals("UAH", p.currency);
        assertEquals("VM33", p.merchant);
        assertEquals("Сільпо", p.cardLabel);
        assertNull(p.cardLast4);
    }

    @Test
    public void realFormat_namedCardWithMerchant() {
        ParsedPayment p = GoogleWalletNotificationParser.parse(
                "SILPO", "UAH189.95 with Сільпо");
        assertNotNull(p);
        assertEquals(new BigDecimal("189.95"), p.amount);
        assertEquals("UAH", p.currency);
        assertEquals("SILPO", p.merchant);
        assertEquals("Сільпо", p.cardLabel);
    }

    @Test
    public void realFormat_cardWithLast4() {
        ParsedPayment p = GoogleWalletNotificationParser.parse(
                "SILPO", "UAH198.00 with Visa PrivatBank ••6810");
        assertNotNull(p);
        assertEquals(new BigDecimal("198.00"), p.amount);
        assertEquals("UAH", p.currency);
        assertEquals("SILPO", p.merchant);
        assertEquals("Visa PrivatBank ••6810", p.cardLabel);
        assertEquals("6810", p.cardLast4);
    }

    @Test
    public void merchantInTitle_amountInText() {
        ParsedPayment p = GoogleWalletNotificationParser.parse(
                "McDonald's", "12,34 € · Mastercard •• 4321");
        assertNotNull(p);
        assertEquals(new BigDecimal("12.34"), p.amount);
        assertEquals("EUR", p.currency);
        assertEquals("4321", p.cardLast4);
        assertEquals("McDonald's", p.merchant);
    }

    @Test
    public void ukrainianHryvnia() {
        ParsedPayment p = GoogleWalletNotificationParser.parse(
                "123,45 грн через Visa •••• 9876", "СІЛЬПО");
        assertNotNull(p);
        assertEquals(new BigDecimal("123.45"), p.amount);
        assertEquals("UAH", p.currency);
        assertEquals("9876", p.cardLast4);
        assertEquals("СІЛЬПО", p.merchant);
    }

    @Test
    public void hryvniaSymbol() {
        ParsedPayment p = GoogleWalletNotificationParser.parse(
                "₴1 234,56 · Mastercard •••• 1111", "АТБ-МАРКЕТ");
        assertNotNull(p);
        assertEquals(new BigDecimal("1234.56"), p.amount);
        assertEquals("UAH", p.currency);
        assertEquals("1111", p.cardLast4);
        assertEquals("АТБ-МАРКЕТ", p.merchant);
    }

    @Test
    public void isoCurrencyCode() {
        ParsedPayment p = GoogleWalletNotificationParser.parse(
                "25.00 PLN with Visa •••• 2222", "Żabka");
        assertNotNull(p);
        assertEquals(new BigDecimal("25.00"), p.amount);
        assertEquals("PLN", p.currency);
        assertEquals("2222", p.cardLast4);
    }

    @Test
    public void merchantEmbeddedInPaymentLine() {
        ParsedPayment p = GoogleWalletNotificationParser.parse(
                "$5.00 at Starbucks with Visa •••• 1234", "");
        assertNotNull(p);
        assertEquals(new BigDecimal("5.00"), p.amount);
        assertEquals("Starbucks", p.merchant);
        assertEquals("1234", p.cardLast4);
    }

    @Test
    public void appNameIsNotMerchant() {
        ParsedPayment p = GoogleWalletNotificationParser.parse(
                "Google Wallet", "$3.00 with Visa •••• 1234");
        assertNotNull(p);
        assertEquals(new BigDecimal("3.00"), p.amount);
        assertNull(p.merchant);
    }

    @Test
    public void nonPaymentNotificationIsIgnored() {
        assertNull(GoogleWalletNotificationParser.parse(
                "Google Wallet", "Card was added to your device"));
    }

    @Test
    public void bareCardDigitsAreNotAnAmount() {
        assertNull(GoogleWalletNotificationParser.parse(
                "Visa •••• 1234", "Contactless setup complete"));
    }

    @Test
    public void merchantWithDigitsInTitle_paymentInText() {
        ParsedPayment p = GoogleWalletNotificationParser.parse(
                "WOG 555", "75.00 UAH with Visa •••• 3333");
        assertNotNull(p);
        assertEquals(new BigDecimal("75.00"), p.amount);
        assertEquals("UAH", p.currency);
        assertEquals("3333", p.cardLast4);
        assertEquals("WOG 555", p.merchant);
    }

    @Test
    public void noCardDigitsStillParses() {
        ParsedPayment p = GoogleWalletNotificationParser.parse(
                "$7.25 with Visa", "TARGET");
        assertNotNull(p);
        assertEquals(new BigDecimal("7.25"), p.amount);
        assertNull(p.cardLast4);
        assertEquals("TARGET", p.merchant);
    }
}
