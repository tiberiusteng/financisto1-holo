package tw.tib.financisto.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.math.BigDecimal;

public class IntentTransactionProcessorTest {

    @Test
    public void withSignOf_followsTheAccountAmount() {
        BigDecimal expense = new BigDecimal("-25.40");
        BigDecimal income = new BigDecimal("25.40");

        assertEquals(new BigDecimal("-21.70"),
                IntentTransactionProcessor.withSignOf(new BigDecimal("21.70"), expense));
        assertEquals(new BigDecimal("-21.70"),
                IntentTransactionProcessor.withSignOf(new BigDecimal("-21.70"), expense));
        assertEquals(new BigDecimal("21.70"),
                IntentTransactionProcessor.withSignOf(new BigDecimal("-21.70"), income));
    }

    @Test
    public void withSignOf_keepsItsOwnSignWithoutAnAccountAmount() {
        assertEquals(new BigDecimal("-21.70"),
                IntentTransactionProcessor.withSignOf(new BigDecimal("-21.70"), null));
        assertEquals(new BigDecimal("21.70"),
                IntentTransactionProcessor.withSignOf(new BigDecimal("21.70"), BigDecimal.ZERO));
    }

    @Test
    public void parseAmount_acceptsTheAmountFormats() {
        assertEquals(new BigDecimal("-16.25"), IntentTransactionProcessor.parseAmount("-16.25"));
        assertEquals(0, new BigDecimal("4500").compareTo(IntentTransactionProcessor.parseAmount("4,500")));
        assertEquals(0, new BigDecimal("1.23").compareTo(IntentTransactionProcessor.parseAmount("1.23")));
    }

    @Test
    public void parseAmount_rejectsEmptyZeroAndGarbage() {
        assertNull(IntentTransactionProcessor.parseAmount(""));
        assertNull(IntentTransactionProcessor.parseAmount("0"));
        assertNull(IntentTransactionProcessor.parseAmount("0.00"));
        assertNull(IntentTransactionProcessor.parseAmount("abc"));
    }

    @Test
    public void toMinorUnits_twoDecimals() {
        assertEquals(-1625L, IntentTransactionProcessor.toMinorUnits(new BigDecimal("-16.25"), 2));
        assertEquals(3500L, IntentTransactionProcessor.toMinorUnits(new BigDecimal("35"), 2));
    }

    @Test
    public void toMinorUnits_threeDecimals() {
        // e.g. KWD: Currency.getScale() is 3, so 1.500 is stored as 1500, not 150
        assertEquals(1500L, IntentTransactionProcessor.toMinorUnits(new BigDecimal("1.500"), 3));
        assertEquals(-1234L, IntentTransactionProcessor.toMinorUnits(new BigDecimal("-1.234"), 3));
    }
}
