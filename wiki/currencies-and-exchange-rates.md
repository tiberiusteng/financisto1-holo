---
layout: default
title: Currencies and exchange rates
---

<nav>

**[Home](index.md)** · [Features](features.md) · [Notification templates](notification-templates.md) · [Intents](intents-and-integration.md) · [Currencies](currencies-and-exchange-rates.md) · [Reports](reports-and-budgets.md) · [Backup](backup-and-restore.md) · [Automation](automation-and-scripts.md) · [Changelog](changelog.md) · [Versions](versions-and-forks.md) · [Development](development.md) · [FAQ](faq.md) · [Resources](resources.md)

</nav>

# Currencies and exchange rates

## Basics
- Multiple currencies, with one **home currency**
- Exchange rates can be downloaded or entered manually
- Transfers between accounts of different currencies use rates
- Group and decimal separators and a symbol format are configurable per currency
- Transactions in a currency other than the account's are supported, including split transactions

## Rate providers
- FloatRates.com, which is the default since 2026-08-16. The previous provider, FreeCurrency, had an SSL certificate problem
- OpenExchangeRates.org, with an API key. Missing historical rates can then be downloaded automatically during balance calculation
- Download all current rates at once from the menu in the Exchange rates screen
- Offline rates are provided when the network isn't available

## In the transaction screen
- The "dots" button opens the calculator; the "=" button updates the field from the other two fields
- A new line shows the rate calculated from the current amounts. The rate input is filled with the latest downloaded rate; when it differs from the calculated value, it is shown in red
- Downloading a rate in the transaction window saves it to the exchange rates
- Rate hints can come from historical transactions. You can switch back to rate entities in Preferences, New transaction screen, Exchange rate source
- Amounts are rounded according to the currency setting (can be turned off)

## Totals in home currency
- Totals use the latest rates instead of converting each transaction with a historical rate
- The totals window shows the exchange rates used
- Rates may be estimated in the inverse direction, or indirectly through the home currency (this affects totals only)

## Custom currencies for stocks and funds
Currencies can be used as meta-currencies for stocks, securities and mutual funds:
- Currency names longer than 3 characters are allowed, and can be excluded from rate updates
- Decimal places can be more than 2, up to 12 (storage is a 64-bit signed integer). Changing the decimal places of a currency **does not modify existing transactions**; update them manually and back up first
- A **trading currency** can be set for an investment currency. The app uses it as an intermediate step to show the balance in home currency. Example: a VTI currency with trading currency USD and home currency TWD, with VTI to USD and USD to TWD rates defined. Tap the balance at the bottom right of the account transaction list to see the value in TWD
- Duplicate currencies are prevented, and deleting a currency deletes its exchange rates
- CSV and QIF export use the currency-specific decimal places
- Custom group digits: `#,##0.00` gives `1,234,567.89`, `#,###0.00` gives `1,2345,6789.00`, and `#,##,##0.00` gives `12,34,56,789.00` (the last needs Android 7.0+). Edit them in Menu, Entities, Currencies

---

[← Back to Home](index.md)
