---
layout: default
title: Intents and shortcuts
---

<nav>

**[Home](index.md)** · [Features](features.md) · [Notification templates](notification-templates.md) · [Intents](intents-and-integration.md) · [Currencies](currencies-and-exchange-rates.md) · [Reports](reports-and-budgets.md) · [Backup](backup-and-restore.md) · [Automation](automation-and-scripts.md) · [Changelog](changelog.md) · [Versions](versions-and-forks.md) · [Development](development.md) · [FAQ](faq.md) · [Resources](resources.md)

</nav>

# Intents and shortcuts

Besides [notification templates](notification-templates.md), Financisto Holo can be driven from other apps and from the launcher.

## Creating transactions with intents
Apps such as MacroDroid can send an intent to create a new transaction (added 2025-07-05).
- The credit card payment field is supported (2025-07-07).
- The transaction time can be given as a Unix timestamp with the long extra `TIMESTAMP_MILLIS`, or as an ISO 8601 string with `TIMESTAMP_ISO8601`, for example `"2026-09-10T19:57:45+08:00"`.
- Intent transaction validation was fixed in the 2026-09 releases.

> The full list of intent actions and extras isn't in the changelog. Look in the source (the manifest and the intent handler activity) for the exact names.

## Backup via intent
- Triggering a backup with an intent (2026-08-16).
- Restoring a backup with an intent, contributed by maximtop in [#163](https://github.com/tiberiusteng/financisto1-holo/pull/163) (2026-10-06).

## Launcher shortcuts and widgets
- App Shortcuts: long press the app icon in the launcher (2025-02-07).
- Launcher shortcuts for new transaction and new transfer (Android 8+ fixed).
- A launcher shortcut for creating a transaction from a template, and a Shortcut Maker "Select Template" shortcut.
- Home screen widgets: 2x1 (tap the icon to cycle accounts), 3x1 and 4x1 with embedded New transaction and New transfer buttons. After a layout change, remove and re-add the widget.

## Working with the backup file
For heavier integration, the maintainer recommends the backup file instead of patching the app. See [Automation and scripts](automation-and-scripts.md).

---

[← Back to Home](index.md)
