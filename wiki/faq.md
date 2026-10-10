---
layout: default
title: FAQ
---

<nav>

**[Home](index.md)** · [Features](features.md) · [Notification templates](notification-templates.md) · [Intents](intents-and-integration.md) · [Currencies](currencies-and-exchange-rates.md) · [Reports](reports-and-budgets.md) · [Backup](backup-and-restore.md) · [Automation](automation-and-scripts.md) · [Changelog](changelog.md) · [Versions](versions-and-forks.md) · [Development](development.md) · [FAQ](faq.md) · [Resources](resources.md)

</nav>

# FAQ

**Does Financisto need an account or internet connection?**
No. It works fully offline. Online backup (Google Drive, Dropbox) and exchange-rate download are optional.

**Where is my data stored?**
On your device. Back it up regularly. See [Backup and restore](backup-and-restore.md).

**How do I move my old Financisto data into Holo?**
Choose or create a new backup folder, copy your old backup files into it, then restore from backup. Backups from Financier and Financisto 1.8+ can also be imported.

**Why doesn't SMS auto-import work?**
Google no longer allows SMS permissions for newly submitted apps. Use [notification templates](notification-templates.md) (SMS notifications can be parsed that way), or build from source.

**My notification template stopped matching after an update.**
Payee, account and project placeholders became non-greedy. Anchor the payee with fixed text after it, and see the [placeholder notes](notification-templates.md). The maintainer asks for an example by email if you can't make it work.

**Dropbox authorization is stuck.**
Close the browser and return to the app, or install the Dropbox app and retry.

**Running balances look wrong.**
Run Integrity Fix (Menu, More).

**My notification template doesn't create transactions.**
Check the permission, use the app's package name as the sender, fill the account's Card Number, and make sure no earlier template matches first. See [Notification templates](notification-templates.md).

**How do I make a template for incoming money?**
Set the plus sign in the template editor (left of the account). The category doesn't decide the sign.

**How do I find a category quickly?**
Press the funnel icon to the left of Category to open a search box with autocomplete.

**How do I search by amount?**
Use the blotter search with `123.45`, `<123.45`, `>123.45` or `123~456`. See [Features](features.md).

**Can I use it for stocks or funds?**
Yes, with custom currencies, more decimal places and a trading currency. See [Currencies and exchange rates](currencies-and-exchange-rates.md).

**Can I export my data?**
Yes: QIF and CSV, the gzipped backup, and the [hledger export script](automation-and-scripts.md).

**Is there a map or location feature?**
The Google Maps-based location features were removed. Locations still exist as entities and can be set on transactions and templates.

**Are my attached pictures deleted with the transaction?**
No. Pictures stay in the `pictures` subfolder of the backup folder. Delete them with a file manager.

**Which Android version is required?**
Not confirmed for Holo. The original app required Android 4.4+. Holo supports edge-to-edge on Android 15+ and Android 16/17 dialog themes.

**Where do I report bugs?**
[Holo issue tracker](https://github.com/tiberiusteng/financisto1-holo/issues).

---

[← Back to Home](index.md)
