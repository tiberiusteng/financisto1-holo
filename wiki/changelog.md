---
layout: default
title: Changelog
---

<nav>

**[Home](index.md)** · [Features](features.md) · [Notification templates](notification-templates.md) · [Intents](intents-and-integration.md) · [Currencies](currencies-and-exchange-rates.md) · [Reports](reports-and-budgets.md) · [Backup](backup-and-restore.md) · [Automation](automation-and-scripts.md) · [Changelog](changelog.md) · [Versions](versions-and-forks.md) · [Development](development.md) · [FAQ](faq.md) · [Resources](resources.md)

</nav>

# Changelog

Condensed from the in-app What's New page. `[+]` is new, `[*]` is changed, `[-]` is fixed. Holo-era releases are listed by date; legacy versions (1.0 to 1.6.8) are summarized at the end.

## 2026

**2026-10-06**
- [+] Notification template option to match group summary
- [+] Notification template fills location (vov4uk, #164)
- [+] Restore backup with intent (maximtop, #163)

**2026-09-24**
- [+] Blotter: full transaction note in a separate section (Preferences, Blotter)
- [*] Full split-child attributes and notes in info dialog and edit screen; touch a field to copy it
- [*] Blotter date color changed for contrast
- [-] Category selector shows the full category name
- [-] Total balance by period report respects "include in reports"

**2026-09-11**
- [+] Transaction tags (#152, ahmaddxb); tags shown in the blotter
- [*] Template `{{e}}` is non-greedy; the last placeholder extends to the end of the text (#154)
- [-] Intent transaction validation (#153, #155)
- [-] Skip group summary notification to deduplicate
- [-] Remove template sender and pattern length limit

**2026-09-04**
- [+] Payee and location aliases for search and notification matching
- [+] Notification template matching by package name
- [+] Intent: `TIMESTAMP_MILLIS` and `TIMESTAMP_ISO8601` extras
- [*] Template `{{e}}` matches payee names with spaces (#146); Google Wallet tweaks
- [*] Split allowed in balance-adjust mode (#151)
- [-] Notification listener stopped working after update (#135); running balance after split (#145); database restore failure; split category attributes

**2026-08-16**
- [+] Trigger backup creation with an intent
- [*] Default exchange rate provider is now FloatRates
- [-] Reports menu taps, Shortcut Maker "Select Template", currency crash fixes

**2026-08-09**
- [+] Exchange rate hint from historical transactions; FloatRates.com provider
- [-] Account filter after restart; project and location cleared when selector hidden; 2D line chart reference period

**2026-08-07**
- [+] Reorder accounts with drag-and-drop (#132)
- [*] `{{x}}` transfer-to account accepts all non-whitespace; notification list ordered by time
- [-] Report filter currency includes foreign transactions; importing backups with splits gave wrong running balance

**2026-08-02**
- [+] Take a photo to attach directly

**2026-07-27**
- [+] Template placeholder `{{g}}` (Unix time in ms); highlight copied but unedited transactions
- [*] Prevent duplicate currencies; delete rates with currencies

**2026-07-23**
- [+] Transactions from Google Wallet notifications (#125)
- [+] Account "include into reports" setting (#124); fast scroll only after scrolling starts

**2026-07-19**
- [+] "Go To Today" button (blotter preferences)

**2026-07-14**
- [+] Total balance in home currency by date report; "Week" aggregate period
- [+] Edit payee/location of split children; blotter shows first split child's payee or note
- [+] Bar chart split summary or children filter
- [-] Split transfer running balance; budget subtotal

**2026-07-10**
- [+] Split interface tweaks, transfer-in children, independent child status, split blotter and mass-op filters

**2026-07-06**
- [+] Option to prevent editing cleared or reconciled transactions; select status for duplicated foreign transactions; split transfer (experimental)
- [*] Backup handles multi-line notes (enable in options); new CSV library

**2026-06-29**
- [+] Duplicate-with-time option for end-of-day journaling
- [-] Android 16/17 light dialog theme

**2026-06-28**
- [+] Trading currency for stocks, securities and funds; currencies with more than 2 decimals (up to 12); category selector background loading; CSV/QIF export with currency decimals

**2026-06-22**
- [+] Back from a generated transaction returns to the blotter
- [*] Template account, payee and project names accept all non-whitespace characters

**2026-06-17**
- [+] CSV transfers in a single line (`to account`, `to amount`, `to balance`, `to currency`); CSV transfer import; line charts cover all data; merge multiple payees

**2026-06-13**
- [+] Template launcher shortcut; fiscal year; yearly line chart aggregation; view transactions per data point; `{{f}}` foreign currency in templates

**2026-06-10 and earlier in 2026**
- [-] View transaction shows the full note; 3-button navigation bar color

## 2025
- **12-21**: toggle to show sort order in account list
- **10-12**: QIF import supports ISO dates; dark button bar
- **10-06**: RuPay card type; multiple-choice status filter
- **09-30**: remember last category in transfer
- **08-09**: account balance by period report
- **08-02**: custom currency group digits; option to hide time of day in blotter
- **07-31**: when blurring is off, balance click opens the account blotter; long press opens quick actions
- **07-20**: locale-aware sorting of payees, projects and locations; convert transaction to transfer and back
- **07-18**: blur balances
- **07-14**: balance on account selector; totals window shows rates; totals use the latest rates; Czech translation
- **07-09**: character or emoji account icon and accent color
- **07-07**: Android SDK update, edge-to-edge on Android 15+; intent supports credit card payment; blotter search includes category title
- **07-05**: create transactions with intents (such as from MacroDroid)
- **05-03**: budget limit on "no project" versus any project
- **04-10**: QIF import fix for anMoney
- **03-02**: CSV running balance field; copy with the recent project; configurable first weekday; 15 recurrence dates shown
- **02-19**: scrollable blotter buttons; Dropbox SDK upgrade
- **02-07**: App Shortcuts
- **02-01**: 2D chart mean line
- **01-31**: mass op can mark restored, pending, unreconciled
- **01-25**: "Transfer current balance" quick action
- **01-18**: CSV export with custom attributes
- **01-13**: CSV import from Google Drive, Dropbox or Files; update existing transactions by `txid`
- **01-02**: improved category suggestion

## 2024
- **12-28 / 12-15**: template preselects payee and project; `{{r}}` project placeholder
- **10-08**: category selector suggests recent categories for the account
- **08-27**: CSV export option for split parents and children
- **08-05**: attach pictures to transactions (stored in the backup folder's `pictures` subfolder; pictures aren't deleted when you remove them from a transaction)
- **05-01**: yearly budgets; budget sorting; search-first or list-first selector option
- **02-27**: template matches origin or transfer account from the title, creates payee; period recalculation
- **02-23**: 2D and pie charts moved to MPAndroidChart; entities auto-created from search text
- **02-19**: reports exclude transfers; mark payee, project, location inactive; "Show in account blotter"
- **02-10**: Payee search interface tweak
- **02-07**: calculator with 4 memories; planner filter
- **01-27 / 01-25 / 01-21**: exchange rate revamp; account list search; show full category path; custom currency names; adaptive launcher icon
- **01-13**: "last year" period; twin date picker; custom note in notification templates
- **01-10**: auto-refresh blotter for background-created transactions; copy transaction keeping time of day
- **01-09 / 01-08**: partial match on notification title with `%`; title prefixed to body

## 2023
- **12-26**: **Notification template** introduced (based on the SMS template); notification fixes for Android 8+
- **12-25**: CSV status field; searching a number matches amount and note
- **12-13**: amount search syntax; reworked scheduled transactions
- **12-09**: options for date in the account list and for credit-card bill payment
- **12-04**: project name in the blotter
- **11-24**: removed picture attach (re-added in 2024-08); auto-backup scheduling fix
- **10-30**: able to import backups from Financier and Financisto 1.8+; filter by "No Category"
- **10-17**: hide content in recent tasks; prevent showing data before unlock
- **10-10**: Android 11+ scoped storage; system folder selector
- **2022-04-26**: Dropbox SDK update

## Legacy versions (original Financisto)

| Version | Highlights |
|---|---|
| 1.6.8 | Backup pictures on Google Drive; Flowzr improvements |
| 1.6.5 to 1.6.7 | Better Dropbox and Google Drive backup; Flowzr sync (paid third-party service); account in budgets; payee in transfers; offline rate fallback |
| 1.6.4 | More exchange rate providers; download all rates at once; original amount and currency in CSV |
| 1.6.3 | Scheduled transactions planner |
| 1.6.2 | Transactions in a currency different from the account; startup screen option; Reconcile action |
| 1.6.1 | Integrity fix |
| 1.6.0 | Archiving of old transactions; hide closed accounts; split screen improvements |
| 1.5.9 to 1.5.6 | CSV import creates categories and projects; detailed totals by currency; pie charts; hierarchical category selector; widgets 3x1 and 4x1; home currency and exchange rates |
| 1.5.5 / 1.5.4 | Dropbox integration; daily auto-backup; gzipped backup; configurable backup folder |
| 1.5.3 | QIF and CSV import |
| 1.5.0 | Split transactions |
| 1.4.x | Lock after a delay; QIF export options; weekly budgets; credit card closing day; payee field; mass operations; 2D graph reports; manual locale; Google Docs backup; monthly preview and credit card statements |
| 1.3.x | Open-sourced (1.3.7); scheduled and recurring transactions and templates (1.3.5); attach pictures (1.3.8); widgets; budgets; CSV export |
| 1.2.x | Recurring budgets; totals; backup and restore database (1.2.1); PIN protection (1.2.0); close and duplicate accounts and transactions |
| 1.1.0 | Account types, saved blotter filter, GPS location |
| 1.0.0 | Initial public release |

---

[← Back to Home](index.md)
