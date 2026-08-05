package tw.tib.financisto.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
}
