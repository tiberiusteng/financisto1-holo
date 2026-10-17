---
layout: default
title: Backup and restore
---

<nav>

**[Home](index.md)** · [Features](features.md) · [Notification templates](notification-templates.md) · [Intents](intents-and-integration.md) · [Currencies](currencies-and-exchange-rates.md) · [Reports](reports-and-budgets.md) · [Backup](backup-and-restore.md) · [Automation](automation-and-scripts.md) · [Changelog](changelog.md) · [Versions](versions-and-forks.md) · [Development](development.md) · [FAQ](faq.md) · [Resources](resources.md)

</nav>

# Backup and restore

**Be sure to back up your data.** Financisto stores everything locally on your device.

## Options
- Local backups to a folder you choose
- Automatic daily backups
- Google Drive backup and restore (optional)
- Dropbox backup and restore (optional)

Cloud backup is off unless you explicitly enable it.

## The backup file
The backup is a **gzipped text file containing key-value pairs of database rows**. This makes it easy to inspect and to process with scripts. See [Automation and scripts](automation-and-scripts.md).

Backups from Financisto Holo are compatible with the Play Store Financisto 1.7.1.

## Importing old Financisto backups
1. Choose or create a new backup folder in the app.
2. Copy your old backup files into that folder.
3. Restore from backup.

## Intents
Backups can be created (since 2026-08-16) and restored (since 2026-10-06, by maximtop in [#163](https://github.com/tiberiusteng/financisto1-holo/pull/163)) with an intent, so other apps or adb can trigger them. See [Intents and shortcuts](intents-and-integration.md).

## Details worth knowing
- Choose the backup folder first. You are prompted to select one before an export or backup when needed.
- Auto-backup runs in a window starting 60 minutes before the configured time.
- Multi-line notes are supported in the backup using `\n` and `\\` escape sequences. Enable this in the options.
- The database backup is gzipped by default. Backups from Financier and Financisto 1.8+ can be imported.
- **Pictures:** attached pictures are copied into the `pictures` subfolder of the backup folder. Let Google Photos, another photo app, or the Google Drive / Dropbox auto-upload back that folder up. If a picture is missing locally, or you restore on another phone, the app can try to download pictures that were uploaded to Google Drive or Dropbox. Pictures are never deleted when you remove them from a transaction or delete the transaction; use a file manager to delete them.
- Exports can be sent to Dropbox, Google Drive, email or other apps through the share intent.
- If running balances are wrong after a restore or import, run Integrity Fix (Menu, More).
- CSV import can read files from Google Drive, Dropbox or the Files app (choose "Open with ..."), and can update existing transactions by `txid`.
- QIF import accepts ISO dates and files from anMoney.
- Changing the decimal places of a currency doesn't modify existing transactions.

## Dropbox authorization problems
If authorization stays on the same page after you click "Authorize":
- Close the browser, then return to Financisto, or
- Install the Dropbox app and try again.

## Other import and export
QIF and CSV import/export are also available.

<!-- SCREENSHOT: backup/restore menu -->

---

[← Back to Home](index.md)
