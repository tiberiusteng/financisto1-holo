---
layout: default
title: Versions and forks
---

<nav>

**[Home](index.md)** · [Features](features.md) · [Notification templates](notification-templates.md) · [Intents](intents-and-integration.md) · [Currencies](currencies-and-exchange-rates.md) · [Reports](reports-and-budgets.md) · [Backup](backup-and-restore.md) · [Automation](automation-and-scripts.md) · [Changelog](changelog.md) · [Versions](versions-and-forks.md) · [Development](development.md) · [FAQ](faq.md) · [Resources](resources.md)

</nav>

# Versions and forks

## History
Financisto was created by Denis Solonenko (orangesoftware) and became open source in version 1.3.7. It was first hosted on Launchpad, later on GitHub. Several forks exist; **Financisto Holo** has been the most actively updated recently.

Financisto Holo's codebase started from an imported copy of an old version of the Launchpad source (<https://code.launchpad.net/~financisto-dev/financisto/trunk>) and is meant as a working interim version until a proper version 2 appears. The maintainer, tiberiusteng, had used the app for 12+ years and tweaked quirks to fit their own needs. See the [Changelog](changelog.md) for the full release history, starting with the first Holo release in October 2023.

## Main projects

| Project | Description |
|---|---|
| **Financisto Holo** ([repo](https://github.com/tiberiusteng/financisto1-holo), [Google Play](https://play.google.com/store/apps/details?id=tw.tib.financisto)) | Based on Financisto 1.6.8. Holo/Material theme, notification templates, fingerprint unlock, Android storage permissions, tags, reports, stock-friendly currencies and many tweaks. Actively developed. Backups are compatible with the Play Store Financisto 1.7.1. GPL-2.0 |
| **Original Financisto** ([dsolonenko/financisto](https://github.com/dsolonenko/financisto)) | The original author's latest development, which the Holo README points to. About 201 commits, no GitHub Releases. Third-party download sites list 1.8.2 (Android 4.4+). The Play listing `ru.orangesoftware.financisto` returned 404 when checked |
| **FinancistoUA** ([luber/financisto](https://github.com/luber/financisto)) | Material Design redesign with SMS parsing for Ukrainian PrivatBank. Removed Dropbox support, moved some screens to Fragments |
| **financisto-ai** ([yeh1518/financisto-ai](https://github.com/yeh1518/financisto-ai)) | Holo fork with AI voice bookkeeping. Its author also contributed several fixes to Holo |
| Other Holo forks | [vov4uk](https://github.com/vov4uk/financisto1-holo), [maximtop](https://github.com/maximtop/financisto1-holo) |
| Other forks of the original | [dabicho](https://github.com/dabicho/financisto), [chwan1](https://github.com/chwan1/financisto) |

## Which should I use?
- **Actively maintained Android app:** Financisto Holo.
- **Ukrainian PrivatBank SMS parsing:** FinancistoUA.
- **Reference to the original code:** dsolonenko/financisto and the Launchpad trunk.

## Releases
Neither the Holo nor the original repo publishes GitHub Releases. Holo builds are numbered in commits and distributed through Google Play. The what's-new page inside the app is the official changelog.

Alternatives are listed on [AlternativeTo](https://alternativeto.net/software/financisto); the top ones are GnuCash, MoneyManager Ex and KMyMoney.

## Credits
From the app's About page:
- Contributors to the original app: Denis Solonenko, Abdsandryk Souza, Rodrigo Sousa, Timo Lindhorst, Igor Makarov, Kubyshev Dmitry, Lyubomyr Vyhovskyy
- Holo contributors named in the changelog include yeh1518, jayc1299, vov4uk, maximtop, ahmaddxb, kokosik8998, mpstudios56, infthi, skibbipl (Polish), Zajko (Czech), paul128 (Ukrainian), Gryundig (Russian), Mauro Scaglia (Italian) and ButTaiwan (icons)
- Translations originally managed on Crowdin
- App icon by [Oxygen](http://www.oxygen-icons.org); other icons by WPZOOM, billybarker.net and Androidicons
- The original project pages (financisto.com, Facebook, Twitter, UserVoice, Flattr, Launchpad bug tracker) are historical and some links may no longer work

The maintainer accepts optional PayPal donations (see the [Holo README](https://github.com/tiberiusteng/financisto1-holo)).

---

[← Back to Home](index.md)
