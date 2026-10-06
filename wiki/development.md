---
layout: default
title: Development
---

<nav>

**[Home](index.md)** · [Features](features.md) · [Notification templates](notification-templates.md) · [Intents](intents-and-integration.md) · [Currencies](currencies-and-exchange-rates.md) · [Reports](reports-and-budgets.md) · [Backup](backup-and-restore.md) · [Automation](automation-and-scripts.md) · [Changelog](changelog.md) · [Versions](versions-and-forks.md) · [Development](development.md) · [FAQ](faq.md) · [Resources](resources.md)

</nav>

# Development

## Source and issues
- Source: <https://github.com/tiberiusteng/financisto1-holo>
- Issues: <https://github.com/tiberiusteng/financisto1-holo/issues>
- License: GPL-2.0

## Building
The project uses **Gradle** with Java and Android. Building it yourself is the way to get SMS-based automatic transactions, which Google Play builds cannot offer. Exact build steps are in the repository; they are not reproduced here.

## Notes from the maintainer
- The codebase started from an imported copy of an old version of the Launchpad source (<https://code.launchpad.net/~financisto-dev/financisto/trunk>).
- The Material update is only partial because the class hierarchy is hard to upgrade.
- Location features were removed because of Google Maps API changes.
- For integrations, the maintainer prefers working with the backup file rather than patching the app.

## Contributing
Pull requests are accepted. Recent examples include transaction tagging ([#152](https://github.com/tiberiusteng/financisto1-holo/pull/152)), a running-balance fix ([#145](https://github.com/tiberiusteng/financisto1-holo/pull/145)) and location in SMS templates ([#164](https://github.com/tiberiusteng/financisto1-holo/pull/164)).

---

[← Back to Home](index.md)
