---
layout: default
title: Automation and scripts
---

<nav>

**[Home](index.md)** · [Features](features.md) · [Notification templates](notification-templates.md) · [Intents](intents-and-integration.md) · [Currencies](currencies-and-exchange-rates.md) · [Reports](reports-and-budgets.md) · [Backup](backup-and-restore.md) · [Automation](automation-and-scripts.md) · [Changelog](changelog.md) · [Versions](versions-and-forks.md) · [Development](development.md) · [FAQ](faq.md) · [Resources](resources.md)

</nav>

# Automation and scripts

For automation or integration, the Holo maintainer recommends working with the **backup file** instead of modifying the app. The backup is a gzipped text file of key-value database rows.

## Example scripts
Repository: [tiberiusteng/financisto-backup-to-hledger](https://github.com/tiberiusteng/financisto-backup-to-hledger)

| Script purpose | Notes |
|---|---|
| Export Financisto backups to **hledger** text format | Easy to read and search in an editor |
| Create transactions from **Taiwan EasyCard** | Region-specific |
| Import transaction logs from the **Taiwan Government Unified Invoice** system | Region-specific |

## Other automation
- [Notification templates](notification-templates.md) create transactions from push notifications.
- Backups can be restored through an adb intent.
- A community fork, [financisto-ai](https://github.com/yeh1518/financisto-ai), adds AI voice bookkeeping: say a sentence and an LLM parses it into a transaction form.

---

[← Back to Home](index.md)
