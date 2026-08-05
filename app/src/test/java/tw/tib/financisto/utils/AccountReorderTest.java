package tw.tib.financisto.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tw.tib.financisto.model.Account;

public class AccountReorderTest {

    /* ===============================================
     * move
     * =============================================== */

    @Test
    public void move_down() {
        List<String> items = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        AccountReorder.move(items, 0, 2);
        assertEquals(Arrays.asList("b", "c", "a", "d"), items);
    }

    @Test
    public void move_up() {
        List<String> items = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        AccountReorder.move(items, 2, 0);
        assertEquals(Arrays.asList("c", "a", "b", "d"), items);
    }

    @Test
    public void move_adjacent() {
        List<String> items = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        AccountReorder.move(items, 1, 2);
        assertEquals(Arrays.asList("a", "c", "b", "d"), items);
    }

    @Test
    public void move_sameIndex_isNoop() {
        List<String> items = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        AccountReorder.move(items, 1, 1);
        assertEquals(Arrays.asList("a", "b", "c", "d"), items);
    }

    @Test
    public void move_firstToLast() {
        List<String> items = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        AccountReorder.move(items, 0, 3);
        assertEquals(Arrays.asList("b", "c", "d", "a"), items);
    }

    @Test
    public void move_lastToFirst() {
        List<String> items = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        AccountReorder.move(items, 3, 0);
        assertEquals(Arrays.asList("d", "a", "b", "c"), items);
    }

    /* ===============================================
     * BY_ACTIVE_THEN_SORT_ORDER
     * =============================================== */

    private static long nextId = 1;

    // Account.equals() compares by id, so tests must give each account a distinct id —
    // otherwise every freshly-constructed Account (id == -1 by default) would compare
    // equal to every other one, and assertEquals on a List<Account> would pass trivially
    // regardless of actual order.
    private static Account account(String title, boolean isActive, int sortOrder) {
        Account a = new Account();
        a.id = nextId++;
        a.title = title;
        a.isActive = isActive;
        a.sortOrder = sortOrder;
        return a;
    }

    @Test
    public void byActiveThenSortOrder_activeBeforeInactive() {
        Account active = account("Zzz", true, 5);
        Account inactive = account("Aaa", false, 1);
        List<Account> accounts = new ArrayList<>(Arrays.asList(inactive, active));
        accounts.sort(AccountReorder.BY_ACTIVE_THEN_SORT_ORDER);
        assertEquals(Arrays.asList(active, inactive), accounts);
    }

    @Test
    public void byActiveThenSortOrder_ascendingWithinGroup() {
        Account first = account("B", true, 1);
        Account second = account("A", true, 2);
        List<Account> accounts = new ArrayList<>(Arrays.asList(second, first));
        accounts.sort(AccountReorder.BY_ACTIVE_THEN_SORT_ORDER);
        assertEquals(Arrays.asList(first, second), accounts);
    }

    @Test
    public void byActiveThenSortOrder_titleTiebreakerWhenSortOrdersCollide() {
        Account b = account("Bank", true, 0);
        Account a = account("Alpha", true, 0);
        List<Account> accounts = new ArrayList<>(Arrays.asList(b, a));
        accounts.sort(AccountReorder.BY_ACTIVE_THEN_SORT_ORDER);
        assertEquals(Arrays.asList(a, b), accounts);
    }

    @Test
    public void byActiveThenSortOrder_stableForEqualKeys() {
        Account a1 = account("Same", true, 0);
        Account a2 = account("Same", true, 0);
        List<Account> accounts = new ArrayList<>(Arrays.asList(a1, a2));
        accounts.sort(AccountReorder.BY_ACTIVE_THEN_SORT_ORDER);
        assertEquals(Arrays.asList(a1, a2), accounts);
    }

    /* ===============================================
     * isReordered
     * =============================================== */

    @Test
    public void isReordered_falseForFreshlySortedList() {
        List<Account> accounts = Arrays.asList(
                account("A", true, 1),
                account("B", true, 2),
                account("C", true, 3)
        );
        assertFalse(AccountReorder.isReordered(accounts));
    }

    @Test
    public void isReordered_falseForAllZerosInTitleOrder() {
        List<Account> accounts = Arrays.asList(
                account("A", true, 0),
                account("B", true, 0),
                account("C", true, 0)
        );
        assertFalse(AccountReorder.isReordered(accounts));
    }

    @Test
    public void isReordered_trueAfterMove() {
        List<Account> accounts = new ArrayList<>(Arrays.asList(
                account("A", true, 1),
                account("B", true, 2),
                account("C", true, 3)
        ));
        AccountReorder.move(accounts, 0, 2);
        assertTrue(AccountReorder.isReordered(accounts));
    }

}
