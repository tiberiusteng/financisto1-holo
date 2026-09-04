package tw.tib.financisto.activity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import tw.tib.financisto.blotter.BlotterFilter;
import tw.tib.financisto.filter.WhereFilter;

/**
 * Telling a filter the user asked for apart from one navigation brought along.
 *
 * Opening an account from the account list lands on a blotter that shows only that
 * account, so the account criterion is part of the navigation rather than something the
 * user picked. Picking an account from the transactions screen through the filter UI is
 * the opposite: that is the user's intent.
 */
public class NavigationOnlyFilterTest {

    private static WhereFilter accountOnly() {
        return WhereFilter.empty().eq(BlotterFilter.FROM_ACCOUNT_ID, "5");
    }

    @Test
    public void accountBlotterWithOnlyTheAccountIsNavigationOnly() {
        assertTrue(BlotterFragment.isNavigationOnlyFilter(true, accountOnly()));
    }

    @Test
    public void accountBlotterWithAnExtraCriterionIsUserIntent() {
        WhereFilter f = accountOnly().eq(BlotterFilter.CATEGORY_ID, "7");
        assertFalse("the user added a category on top of the account blotter",
                BlotterFragment.isNavigationOnlyFilter(true, f));
    }

    @Test
    public void accountBlotterWithAStatusCriterionIsUserIntent() {
        WhereFilter f = accountOnly().eq(BlotterFilter.STATUS, "PN");
        assertFalse(BlotterFragment.isNavigationOnlyFilter(true, f));
    }

    /** Same filter contents, but reached through the filter screen: that is user intent. */
    @Test
    public void accountPickedInTheFilterScreenIsUserIntent() {
        assertFalse(BlotterFragment.isNavigationOnlyFilter(false, accountOnly()));
    }

    @Test
    public void emptyFilterIsNotNavigationOnly() {
        assertFalse(BlotterFragment.isNavigationOnlyFilter(true, WhereFilter.empty()));
    }
}
