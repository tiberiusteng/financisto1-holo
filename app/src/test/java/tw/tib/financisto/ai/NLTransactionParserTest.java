package tw.tib.financisto.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NLTransactionParserTest {

    @Test
    public void testParseSimpleExpense() {
        ParsedTransactionResult result = NLTransactionParser.parse("全家 85 便當 現金", null);
        assertEquals(8500L, result.amount);
        assertTrue(result.isExpense);
        assertEquals("全家", result.payeeName);
    }

    @Test
    public void testParseCoffeeCard() {
        ParsedTransactionResult result = NLTransactionParser.parse("星巴克 165 拿鐵 信用卡", null);
        assertEquals(16500L, result.amount);
        assertTrue(result.isExpense);
        assertEquals("星巴克", result.payeeName);
    }

    @Test
    public void testParseIncome() {
        ParsedTransactionResult result = NLTransactionParser.parse("收到薪水 50000 國泰世華", null);
        assertEquals(5000000L, result.amount);
        assertFalse(result.isExpense);
    }

    @Test
    public void testParseGasStation() {
        ParsedTransactionResult result = NLTransactionParser.parse("昨天晚上中油加油 1000 刷卡", null);
        assertEquals(100000L, result.amount);
        assertTrue(result.isExpense);
        assertEquals("中油", result.payeeName);
    }
}
