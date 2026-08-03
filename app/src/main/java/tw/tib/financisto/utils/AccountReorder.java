package tw.tib.financisto.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import tw.tib.financisto.model.Account;

public class AccountReorder {

    /** Mirrors the list order of MyEntityManager.getAllAccounts() under SORT_ORDER_ASC. */
    public static final Comparator<Account> BY_ACTIVE_THEN_SORT_ORDER =
            Comparator.<Account>comparingInt(a -> a.isActive ? 0 : 1)
                    .thenComparingInt(a -> a.sortOrder)
                    .thenComparing(a -> a.title);

    /** Moves the item at from to to, shifting the rest — matches ItemTouchHelper semantics. */
    public static <T> void move(List<T> items, int from, int to) {
        if (from == to) {
            return;
        }
        T item = items.remove(from);
        items.add(to, item);
    }

    /** True if any account's position differs from its stored sortOrder ordering. */
    public static boolean isReordered(List<Account> accounts) {
        List<Account> sorted = new ArrayList<>(accounts);
        sorted.sort(BY_ACTIVE_THEN_SORT_ORDER);
        for (int i = 0; i < accounts.size(); i++) {
            // Account.equals() compares by id, which is unreliable here: two distinct new
            // accounts both default to id == -1. Reference identity is what we actually mean
            // by "this account's position didn't change".
            if (accounts.get(i) != sorted.get(i)) {
                return true;
            }
        }
        return false;
    }
}
