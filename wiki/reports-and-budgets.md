---
layout: default
title: Reports and budgets
---

<nav>

**[Home](index.md)** · [Features](features.md) · [Notification templates](notification-templates.md) · [Intents](intents-and-integration.md) · [Currencies](currencies-and-exchange-rates.md) · [Reports](reports-and-budgets.md) · [Backup](backup-and-restore.md) · [Automation](automation-and-scripts.md) · [Changelog](changelog.md) · [Versions](versions-and-forks.md) · [Development](development.md) · [FAQ](faq.md) · [Resources](resources.md)

</nav>

# Reports and budgets

## Reports
- Reports by period, by category (with a top-down variant and sub-categories), by payee, location and project, and a **by period** report with a "Week" aggregate period
- **Total balance in home currency by date** and **account balance by period** (new in 2025-08). Both respect each account's "include in reports" setting
- Bar charts, pie charts and 2D line charts, which use MPAndroidChart and support pinch zoom
- 2D (line) chart reports:
  - A line at the mean value
  - The period covers all data
  - Aggregate by year or fiscal year
  - View the transactions that belong to a data point
  - Filter selection by list
- Filters are remembered per report, and reports can include or exclude transfers
- Bar chart reports (other than the category report) can show only split summaries or only children, to avoid counting an amount twice
- Report filters now include foreign-currency transactions
- Periods such as Today and This Week recalculate with the current time. Predefined periods include last week, last month, this year and last year
- Fiscal year periods, with the start day set in preferences
- First day of the week is configurable (Sunday or Monday), and otherwise taken from the locale

## Budgets
- Weekly, monthly and yearly recurring budgets
- Based on multiple categories and projects, with an account filter
- "Include credit" option; budgets only count expenses
- Limit a budget to transactions with strictly "no project" versus any project
- Sort by name or by period end date, with a totals row
- A budget can be used to create a transaction, with the account auto-selected

## Other tools
- Monthly preview and credit card statements
- Integrity fix (Menu, More): rebuilds running balances if they get out of sync. Run it when you see a warning or incorrect running balances
- Re-index categories (Menu, Entities, Categories, Menu): fixes a broken category report
- Archiving: delete old transactions without affecting account balances
- Filters can use "No Category", no payee, no project or current location

---

[← Back to Home](index.md)
