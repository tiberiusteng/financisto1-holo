package tw.tib.financisto.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import tw.tib.financisto.service.SmsTransactionProcessor.Placeholder;

/**
 * Runs on a device/emulator on purpose: Android's java.util.regex is ICU-backed and its
 * character classes are always Unicode, which differs from a desktop JVM. A desktop unit
 * test would give the wrong answer about what these placeholders capture.
 *
 * What matters here is not whether the template matches, but what it captures — a wrong
 * capture is worse than no match, because the account lookup then silently fails.
 */
@RunWith(AndroidJUnit4.class)
public class PlaceholderCaptureTest {

    private static final String TEMPLATE = "transfer {{p}} to {{x}} done";

    private static String captureTransferTo(String accountTitle) {
        String[] match = SmsTransactionProcessor.findTemplateMatches(
                TEMPLATE, "transfer 100 to " + accountTitle + " done");
        assertNotNull("template did not match for: " + accountTitle, match);
        return match[Placeholder.TRANSFER_TO_ACCOUNT_NAME.ordinal()];
    }

    @Test
    public void capturesAsciiAccountTitle() {
        assertEquals("LineBank", captureTransferTo("LineBank"));
    }

    @Test
    public void capturesCjkAccountTitle() {
        assertEquals("身上現金", captureTransferTo("身上現金"));
        assertEquals("中信信用卡", captureTransferTo("中信信用卡"));
        assertEquals("Richart帳戶", captureTransferTo("Richart帳戶"));
    }

    /**
     * A hyphen is not a word character in any Unicode mode, so with (\w+?) the whole
     * template fails to match and no transaction is created at all. This has nothing to
     * do with the script the title is written in — plain ASCII titles break the same way.
     */
    @Test
    public void capturesAccountTitleWithHyphen() {
        assertEquals("Visa-Gold", captureTransferTo("Visa-Gold"));
        assertEquals("郵局-老婆", captureTransferTo("郵局-老婆"));
        assertEquals("富邦銀行-老婆", captureTransferTo("富邦銀行-老婆"));
    }

    /** Same for parentheses, which are punctuation rather than word characters. */
    @Test
    public void capturesAccountTitleWithParentheses() {
        assertEquals("Cash(Joint)", captureTransferTo("Cash(Joint)"));
        assertEquals("(存款)", captureTransferTo("(存款)"));
        assertEquals("(旅遊儲蓄金)", captureTransferTo("(旅遊儲蓄金)"));
    }

    /**
     * The end-of-line rule is decided by the placeholder's pattern, not by {{e}} specifically:
     * {{x}} / {{c}} / {{r}} are (\S+?) and used to capture one character at the end of a template
     * too. With the rule they capture the whole last word of the line. Because \S cannot cross a
     * space, a value followed by more words on the same line makes the template not match at
     * all — better than the silent one-character account name that failed the lookup anyway.
     */
    @Test
    public void transferToAccountAtTemplateEndCapturesTheLastWord() {
        String[] match = SmsTransactionProcessor.findTemplateMatches(
                "transfer {{p}} to {{x}}", "transfer 100 to Visa-Gold\nthank you");
        assertNotNull("template did not match", match);
        assertEquals("Visa-Gold", match[Placeholder.TRANSFER_TO_ACCOUNT_NAME.ordinal()]);
        assertNull(SmsTransactionProcessor.findTemplateMatches(
                "transfer {{p}} to {{x}}", "transfer 100 to Visa Gold done"));
    }

    // --- {{e}} (payee / merchant) ---

    /** The usual shape of a card notification: the merchant sits between two fixed labels. */
    private static final String MERCHANT_TEMPLATE =
            "信用卡消費通知 消費金額：{{p}}元{{*}}末四碼{{a}}{{*}}商店名稱：{{e}}\n授權碼：";

    private static String captureMerchant(String merchant) {
        String[] match = SmsTransactionProcessor.findTemplateMatches(MERCHANT_TEMPLATE,
                "信用卡消費通知 消費金額：4000元\n卡　　號：末四碼1234\n"
                        + "授權時間：2026/08/31 10:00\n商店名稱：" + merchant + "\n授權碼：000123");
        assertNotNull("template did not match for merchant: " + merchant, match);
        return match[Placeholder.PAYEE.ordinal()];
    }

    @Test
    public void capturesMerchantWithoutSpace() {
        assertEquals("甲壽保費", captureMerchant("甲壽保費"));
        assertEquals("SHOPFAST", captureMerchant("SHOPFAST"));
        assertEquals("丁購物股份有限公司", captureMerchant("丁購物股份有限公司"));
    }

    /**
     * A space is not punctuation but it is not \S either, so with (\S+?) the whole
     * template fails to match and the notification is dropped without a trace. English
     * merchant names routinely contain spaces — this is the common case, not an edge one.
     */
    @Test
    public void capturesMerchantWithSpace() {
        assertEquals("ALPHA THEATRES", captureMerchant("ALPHA THEATRES"));
        assertEquals("SHOPFAST TW", captureMerchant("SHOPFAST TW"));
        assertEquals("A1 MART 城中店", captureMerchant("A1 MART 城中店"));
    }

    /** Non-greedy plus a closing anchor: capture stops at the first anchor, nothing after it. */
    @Test
    public void stopsAtTheFirstAnchor() {
        assertEquals("A B", captureMerchant("A B"));
        String[] match = SmsTransactionProcessor.findTemplateMatches(
                "金額NT${{p}}元{{*}}在{{e}} 刷卡。",
                "丙銀行 【刷卡通知】金額NT$205元 \n"
                        + "卡號末四碼5678於 2026/08/17 13:57在SHOPFAST TW 刷卡。立即查看消費明細");
        assertNotNull("template did not match", match);
        assertEquals("SHOPFAST TW", match[Placeholder.PAYEE.ordinal()]);
    }

    /**
     * A template that ends with {{e}} has no closing anchor. A lazy capture used to settle for
     * a single character there ("SHOPFAST TW" -> "S"); it now runs to the end of the line, and
     * the next line is not pulled in. A message that ends right after the payee works too.
     */
    @Test
    public void payeeAtTemplateEndCapturesToEndOfLine() {
        String[] match = SmsTransactionProcessor.findTemplateMatches(
                "金額NT${{p}}元{{*}}在{{e}}",
                "丙銀行 【刷卡通知】金額NT$205元 \n卡號末四碼5678於 2026/08/17 13:57在SHOPFAST TW\n立即查看消費明細");
        assertNotNull("template did not match", match);
        assertEquals("SHOPFAST TW", match[Placeholder.PAYEE.ordinal()]);
        match = SmsTransactionProcessor.findTemplateMatches("金額{{p}}元在{{e}}", "丙銀行 金額205元在NeoShop");
        assertNotNull("template did not match at end of message", match);
        assertEquals("NeoShop", match[Placeholder.PAYEE.ordinal()]);
    }

    /**
     * {{e}} followed only by a trailing {{*}} (to ignore whatever comes after) has no anchor
     * either: same treatment, the payee runs to the end of its line and {{*}} takes the rest.
     * {{e}}{{*}} followed by fixed text is left alone — the wildcard then has to reach that text.
     */
    @Test
    public void payeeBeforeTrailingWildcardCapturesToEndOfLine() {
        String[] match = SmsTransactionProcessor.findTemplateMatches(
                "金額NT${{p}}元{{*}}在{{e}}{{*}}",
                "丙銀行 【刷卡通知】金額NT$205元 \n卡號末四碼5678於 2026/08/17 13:57在SHOPFAST TW\n立即查看消費明細");
        assertNotNull("template did not match", match);
        assertEquals("SHOPFAST TW", match[Placeholder.PAYEE.ordinal()]);
    }

    /**
     * The closing anchor occurs more than once on the line — "merchant, disclaimer, more,
     * thanks" is the usual shape of a card notification here. A lazy capture stops at the
     * first anchor and yields the merchant; a greedy one runs to the last anchor and the
     * whole disclaimer becomes the payee. Adding more fixed text after {{e}} does not help
     * when the anchor itself is what repeats.
     */
    @Test
    public void payeeStopsAtTheFirstAnchorEvenWithALongTail() {
        String[] match = SmsTransactionProcessor.findTemplateMatches(
                "丙銀行 {{*}}末四碼{{a}}{{*}}台幣{{p}}元，商店名稱:{{e}}，",
                "丙銀行 丙銀行信用卡末四碼4321刷卡通知1150819_20:57金額台幣1,838元，商店名稱:測試商行，"
                        + "實際商店名稱請以信用卡帳單列示為準，實際請款金額以帳單所列為準如有疑問請撥打卡片背面服務專線，謝謝！");
        assertNotNull("template did not match", match);
        assertEquals("測試商行", match[Placeholder.PAYEE.ordinal()]);
    }

    /**
     * {{e}} never crosses a line. That is why it uses ([^\r\n]+?) rather than (.+?): the
     * pattern is compiled with DOTALL, so (.+?) would swallow the line breaks and capture a
     * multi-line payee whenever the closing anchor only appears further down. If the anchor
     * is not on the same line the template should simply not match — better no transaction
     * than a payee spanning two lines.
     */
    @Test
    public void payeeNeverCrossesLines() {
        assertNull(SmsTransactionProcessor.findTemplateMatches(
                "消費金額：{{p}}元\n商店名稱：{{e}}授權碼：",
                "消費金額：4000元\n商店名稱：甲壽保費\n授權碼：000123"));
    }
}
