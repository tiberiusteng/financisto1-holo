# Plan — Drag-and-drop reordering for the accounts list

## Context

Account position in the accounts list is driven by `account.sort_order`, an `integer not null default 0`
column added by `app/src/main/assets/database/alter/20091014_2319_add_sort_order_to_account.sql`.

Today the **only** way to change it is to open each account one at a time and type a number into the
"Sort Order" field (`AccountActivity.sortOrderText`, `AccountActivity.java:84,126-129,249,301-302,465`).
Reordering N accounts means N edit-save round trips, and because the numbers are absolute rather than
relative, inserting an account between two others usually means renumbering everything by hand. It is
slow and error-prone.

This adds a drag-and-drop reorder dialog. **Storage and data type are unchanged** — it is purely a more
convenient way to write the same `sort_order` values. Manual numeric editing in `AccountActivity` stays
exactly as it is.

Confirmed by inspection: the sort order *is* stored in the database (`account.sort_order`), surfaced as
`Account.sortOrder` (`int`), and used for ordering in exactly one place —
`MyEntityManager.getAllAccounts()` (`MyEntityManager.java:253-290`).

### Half-built groundwork already in the repo

Someone previously started this and stopped. The plan deliberately does **not** revive most of it, to
keep the diff small, but it is worth knowing it exists:

| Artefact | State |
| --- | --- |
| `account_list_item.xml` `@+id/drag_handle` ImageView | present, `visibility="gone"`, unbound |
| `AccountRecyclerAdapter implements ItemTouchHelperAdapter` | present, `onItemMove`/`onItemDismiss` empty stubs |
| `AccountRecyclerFragment.java:376-378` ItemTouchHelper attach | commented out |
| `account_recyclerview.xml` `@+id/bReorder` button | present, `visibility="gone"`, zero Java references |

Decision: leave all four alone. Entry points are only the two requested (account editor + preferences).

### The alphabetical-sort interaction

`sort_accounts` (`MyPreferences.AccountSortOrder`, `MyPreferences.java:45-59,195-198`) selects which
column orders the list: `SORT_ORDER_ASC` (default), `SORT_ORDER_DESC`, `NAME`, `LAST_TRANSACTION_ASC`,
`LAST_TRANSACTION_DESC`. `MyEntityManager.getAllAccounts()` builds one ORDER BY:
`is_active DESC, <pref column> ASC|DESC, title ASC`.

So under `NAME`, `sort_order` is simply absent from the ORDER BY — the stored values are untouched and
the account editor still exposes them. The existing `bShowSortOrder` toggle
(`AccountRecyclerFragment.java:177-188`) displays the numbers regardless of which preference is active.

**The reorder dialog follows that same precedent: it never reads or writes `sort_accounts`.** It always
lists by sort order and always saves sort order. A user on alphabetical sorting keeps alphabetical
sorting, unchanged and unwarned.

---

## Approach

An `AlertDialog` containing a `RecyclerView` of all accounts with drag handles. On **OK**, every listed
account is renumbered `1..N` in a single transaction. On **Cancel**, nothing is written.

Renumbering (rather than the incremental shift used for SMS templates) is chosen because:

- It self-heals the common state where every account still has `sort_order = 0`.
- `EntityManager.moveItemByChangingOrder` requires `T extends SortableEntity`;
  `Account` does not implement it, and `SortableEntity.getSortOrder()` returns `long` while
  `Account.sortOrder` is `int`. `EntityManager.updateEntitySortOrder` does
  `f.field.set(obj, sortOrder)` with a `long` — that would throw `IllegalArgumentException` against an
  `int` field. Making `Account` sortable is a trap, not a shortcut.
- One transaction, no per-drag DB writes, trivially cancellable.

Numbering starts at **1, not 0**, because `EntityManager.saveOrUpdate` treats `sort_order <= 0` on
insert as "assign `max(sort_order) + 1`" (`EntityManager.java:147-177`). Keeping every value positive
means a newly created account still lands at the end, as it does today.

---

## Files to change

### New files

**`app/src/main/java/tw/tib/financisto/utils/AccountReorder.java`** — pure logic, no Android deps.

```java
public class AccountReorder {
    /** Mirrors the list order of MyEntityManager.getAllAccounts() under SORT_ORDER_ASC. */
    public static final Comparator<Account> BY_ACTIVE_THEN_SORT_ORDER = ...;

    /** Moves the item at from to to, shifting the rest — matches ItemTouchHelper semantics. */
    public static <T> void move(List<T> items, int from, int to) { ... }

    /** True if any account's position differs from its stored sortOrder ordering. */
    public static boolean isReordered(List<Account> accounts) { ... }
}
```

`isReordered` lets **OK** skip the write entirely when nothing moved, so opening and confirming the
dialog is a no-op rather than a silent mass renumber.

**`app/src/test/java/tw/tib/financisto/utils/AccountReorderTest.java`** — JUnit 4, plain
`org.junit.Assert` static imports, no mocking, matching the style of the repo's only existing unit test
(`GoogleWalletNotificationParserTest`, naming convention `scenario_expectation`). Written first.

Cases:
- `move` down / up / adjacent / same index / first-to-last / last-to-first, asserted on a `List<String>`
  so the test has zero model or Android dependency.
- `BY_ACTIVE_THEN_SORT_ORDER`: active before inactive; ascending sort order within each group; title
  as tiebreaker when sort orders collide (the all-zeros case); stability.
- `isReordered`: false for a freshly sorted list, false for the all-zeros list in title order, true
  after a `move`.

> If `Account` turns out not to instantiate under plain JUnit (it pulls in `AccountType`, which
> references `R` int constants — expected to be fine), fall back to testing the comparator's
> key-extraction on a minimal local stub. `move` is generic and unaffected either way.

**`app/src/main/java/tw/tib/financisto/dialog/AccountReorderDialog.java`** — modelled directly on
`tw.tib.financisto.dialog.AccountInfoDialog`: a plain class (not a `DialogFragment`, matching the
codebase), constructor `(Context, DatabaseAdapter, Runnable onSaved)`, driven by `show()`.

- Loads `db.getAllAccountsListWithClosed()` (all accounts, ignores `hide_closed_accounts`) and sorts
  with `AccountReorder.BY_ACTIVE_THEN_SORT_ORDER`.
- Builds `new AlertDialog.Builder(context).setTitle(R.string.reorder_accounts).setView(v)
  .setPositiveButton(R.string.ok, ...).setNegativeButton(R.string.cancel, null)`.
- On OK: if `AccountReorder.isReordered(...)`, call `db.updateAccountsSortOrder(ids)` then `onSaved.run()`.
- All accounts are shown, including closed ones (renumbering only a visible subset would leave hidden
  accounts holding colliding stale numbers). Closed accounts are dimmed as in the main list. They can
  be dragged, but note the list still applies `is_active DESC` above sort order, so a closed account
  will not actually rise above an active one — that is existing behaviour and is left alone.
- DB work runs on the main thread, matching `AccountActivity.save()` and
  `AccountRecyclerFragment.deleteAccount()`; it is one small transaction.

**`app/src/main/java/tw/tib/financisto/adapter/AccountReorderAdapter.java`** — `List<Account>`-backed
(not cursor-backed; a cursor cannot be reordered in place), implementing `ItemTouchHelperAdapter`, with
a `ViewHolder implements ItemTouchHelperViewHolder`. `onItemMove` calls `AccountReorder.move` +
`notifyItemMoved`; `onItemDismiss` is a no-op. Exposes `getAccountIds()` for the save step.

**`app/src/main/res/layout/account_reorder_dialog.xml`** — modelled on `info_dialog.xml`: vertical
`LinearLayout` wrapping a `RecyclerView`, sized so long account lists scroll inside the dialog.

**`app/src/main/res/layout/account_reorder_list_item.xml`** — slim row: `@drawable/drag_handle`
ImageView + account icon/icon-text + title styled `@style/ListPrimary`. Deliberately not a reuse of
`account_list_item.xml`, whose hardcoded `android:background="#000000"` and balance/progress columns
look wrong inside a dialog.

### Modified files

**`app/src/main/java/tw/tib/financisto/adapter/AccountRecyclerAdapter.java`** — extract the ~12 lines
of icon resolution (`AccountType` / `CardIssuer` / `ElectronicPaymentType` / `a.icon` text fallback,
`AccountRecyclerAdapter.java:101-121`) into a `public static void setAccountIcon(ImageView, TextView,
Account)` and call it from `onBindViewHolder`. `AccountReorderAdapter` reuses it. This is the one small
refactor in the change; it avoids duplicating icon logic and is behaviour-preserving.

**`app/src/main/java/tw/tib/financisto/db/MyEntityManager.java`** — add next to `saveAccount`
(`MyEntityManager.java:292-294`):

```java
public void updateAccountsSortOrder(List<Long> accountIds) {
    SQLiteDatabase db = db();
    db.beginTransaction();
    try {
        ContentValues cv = new ContentValues(1);
        for (int i = 0; i < accountIds.size(); i++) {
            cv.put(DEF_SORT_COL, i + 1);
            db.update(ACCOUNT_TABLE, cv, DEF_ID_COL + "=?",
                    new String[]{String.valueOf(accountIds.get(i))});
        }
        db.setTransactionSuccessful();
    } finally {
        db.endTransaction();
    }
}
```

Direct column update rather than `saveOrUpdate`, so reordering does not rewrite every column or bump
`updated_on` — reordering is not an account edit. Follows the transaction idiom of
`EntityManager.moveItemByChangingOrder` (`EntityManager.java:317-345`).

**`app/src/main/java/tw/tib/financisto/view/NodeInflater.java`** — add an `EditButtonBuilder`
mirroring the existing `EditColorBuilder` (`NodeInflater.java:135-151`), inflating a new
`select_entry_edit_button.xml`, with `withButtonId(int, OnClickListener)`.

**`app/src/main/res/layout/select_entry_edit_button.xml`** *(new)* — a copy of `select_entry_palette.xml`
minus the `color_preview` View: horizontal `LinearLayout`, `minHeight="@dimen/select_entry_height"`,
`background="?android:attr/listChoiceBackgroundIndicator"`, weighted `RelativeLayout@+id/layout` for
label + field, then a trailing `ImageButton`.

**`app/src/main/java/tw/tib/financisto/activity/ActivityLayout.java`** — add alongside `addEditNode`
(`ActivityLayout.java:342`) and `addColorEditNode` (`:347`):

```java
public View addEditNodeWithButton(LinearLayout layout, int labelId, int buttonId,
                                  View.OnClickListener onClickListener, EditText editText) {
    EditButtonBuilder b = inflater.new EditButtonBuilder(layout, editText);
    return b.withButtonId(buttonId, onClickListener).withLabel(labelId).create();
}
```

**`app/src/main/res/values/styles.xml`** — add a `ReorderButton` style mirroring `PaletteButton`
(`styles.xml:111-115`): `background @drawable/btn_circle2`, `src @drawable/height`, `tint #ff5c5c5c`.
`@drawable/height` is the up/down arrow the codebase already earmarked for reorder on `bReorder`.

**`app/src/main/res/values/ids.xml`** — add `<item name="reorder_accounts" type="id" />` for the button,
matching how `palette` / `sort_order` are declared.

**`app/src/main/java/tw/tib/financisto/activity/AccountActivity.java`** — change line 249 from
`x.addEditNode(...)` to `x.addEditNodeWithButton(...)`, opening `AccountReorderDialog`:

```java
x.addEditNodeWithButton(layout, R.string.sort_order, R.id.reorder_accounts, clicked -> {
    new AccountReorderDialog(this, db, () -> {
        if (account.id > 0) {
            sortOrderText.setText(String.valueOf(db.getAccount(account.id).sortOrder));
        }
    }).show();
}, sortOrderText);
```

The EditText itself is unchanged — same numeric input, same 6-char filter, same read/write at
`:301-302` and `:465`. Manual editing keeps working exactly as before.

Two behaviours to be explicit about:
- For an account being **edited** (`id > 0`), the dialog renumbers it along with the rest, so the field
  is refreshed from the DB on OK. Any number the user had typed but not yet saved is overwritten by the
  new position — which is the correct outcome, since they just chose that position by dragging.
- For a **new** account (`id == -1`) the account is not in the DB yet, so it does not appear in the
  dialog and the field is left alone; on save it still gets `max + 1` as it does today. The button stays
  enabled so the user can still tidy the existing accounts.

**`app/src/main/res/xml/pref_accounts.xml`** — add a bare preference (the file currently has no
click-backed entries; this follows the `fiscal_year_start` / `database_backup_folder` pattern from
`PreferenceFragment.java:88-107`):

```xml
<Preference
    android:key="reorder_accounts"
    android:summary="@string/reorder_accounts_summary"
    android:title="@string/reorder_accounts" />
```

> There is a pre-existing stray `/>` on line 27 of this file, after the `blur_balances` block. It is an
> editing artefact from an earlier commit. Leave it — removing it is unrelated cleanup and does not
> belong in this PR.

**`app/src/main/java/tw/tib/financisto/preference/AccountPreferencesFragment.java`** — override
`onViewCreated`, call `super.onViewCreated` first (the base class applies window insets there), then:

```java
findPreference("reorder_accounts").setOnPreferenceClickListener(p -> {
    DatabaseAdapter db = new DatabaseAdapter(getContext());
    db.open();
    new AccountReorderDialog(getContext(), db, null).show();
    return true;
});
```

with the adapter closed when the dialog is dismissed.

**`app/src/main/res/values/strings.xml`** — two new strings, placed in the account-preferences cluster
around line 942 next to `accounts_list_screen` / `hide_closed_accounts`, following the established
`foo` + `foo_summary` pairing:

```xml
<string name="reorder_accounts">Reorder accounts</string>
<string name="reorder_accounts_summary">Drag accounts to set their order in the accounts list</string>
```

English only. Translations for the ~22 other locales are left to upstream's usual process, as with other
recently added strings.

---

## Out of scope

- The hidden `@+id/bReorder` button and the commented-out `ItemTouchHelper` in `AccountRecyclerFragment`.
- The unused `account_separator` table and `v_account_with_separator` view.
- The stale "(requires restart)" text in `sort_accounts_summary`.
- The vestigial `AccountListFragment` (dead code; `AccountRecyclerFragment` is the live list).
- Any change to `sort_accounts`, `hide_closed_accounts`, or the `Account` schema.

---

## Verification

Per the workspace rule, builds and test runs go through Android Studio — do not invoke `gradlew`.

**Unit tests** (Android Studio test runner, `app/src/test/java/tw/tib/financisto/utils/AccountReorderTest`):
write these first and watch them fail before implementing `AccountReorder`.

**Manual, from the account editor:**
1. Accounts → edit an account → the Sort Order row now has a trailing reorder button; the numeric field
   still accepts typed input and saves it.
2. Tap the button → dialog lists every account, closed ones dimmed, in current sort order.
3. Drag several rows, tap **Cancel** → reopen; original order intact, no DB writes.
4. Drag again, tap **OK** → the Sort Order field updates to the account's new position. Back out; the
   accounts list reflects the new order.
5. Enable the `bShowSortOrder` toggle in the accounts-list bottom bar → numbers read 1..N with no gaps
   or duplicates.

**Manual, from preferences:**
6. Preferences → Accounts List → Reorder accounts → same dialog, reorder, OK. Return to the accounts
   list and confirm the new order.

**Regression:**
7. Set Preferences → Accounts List → Sort accounts to **By Name**. Confirm the list is alphabetical,
   open the reorder dialog, reorder, OK — the list stays alphabetical and the preference is unchanged.
   Switch back to **By Sort Order (Asc)** and confirm the new order took effect.
8. Set Sort accounts to **By Sort Order (Desc)** and confirm the order is the reverse of the dialog's.
9. Enable **Hide closed accounts**. Confirm closed accounts still appear in the reorder dialog and that
   reordering does not disturb the visible accounts' relative order.
10. Fresh-ish case: an install where every account still has `sort_order = 0`. Confirm the dialog lists
    them in title order and that a single OK assigns clean 1..N values.
11. Create a new account (Sort Order left blank) → confirm it still lands at the end of the list.
