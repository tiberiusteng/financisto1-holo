---
layout: default
title: Notification templates
---

<nav>

**[Home](index.md)** · [Features](features.md) · [Notification templates](notification-templates.md) · [Intents](intents-and-integration.md) · [Currencies](currencies-and-exchange-rates.md) · [Reports](reports-and-budgets.md) · [Backup](backup-and-restore.md) · [Automation](automation-and-scripts.md) · [Changelog](changelog.md) · [Versions](versions-and-forks.md) · [Development](development.md) · [FAQ](faq.md) · [Resources](resources.md)

</nav>

# Notification templates

After you grant Financisto Holo permission to read notifications, it matches incoming notifications against rules you define and creates transactions from them. Any app that posts a notification can trigger a transaction: SMS messages, bank apps, or instant messengers. You could even message yourself from another phone to log a transaction.

This replaces the old SMS templates. Google no longer allows SMS permissions for new apps, so reading SMS directly isn't available in the Play build; with a notification template you can still parse SMS notifications. You can also build the app from source to get the old SMS behavior. See [Development](development.md).

## Create a template
1. Open the SMS/Notification template list. A button at the top right shows the notifications the app can currently see.
2. Make a purchase, or wait for the next one, then open that screen. The colored line is the notification title (the sender as the app sees it).
3. Tap a notification to copy its sender and content to the clipboard, then paste it into the template screen and edit it.
4. The body doesn't need to contain the whole message, only the relevant parts.
5. Tap **(?)** to see the placeholders.
6. Paste the original notification body into the test area to see how it extracts data.
7. Save. The next matching notification creates a transaction.

<img alt="Notification list" src="assets/notification_list.png" width="40%" />
<img alt="Template editor" src="assets/template.png" width="40%" />
<img alt="Template test" src="assets/template_test.png" width="40%" />
<img alt="Received notification" src="assets/received_notification.png" width="40%" />

## Placeholders

| Placeholder | Meaning |
|---|---|
| `{{a}}` | Code used to look up the account |
| `{{e}}` | Payee name to look up in the database (see below) |
| `{{p}}` | The amount or price |
| `{{t}}` | Text extracted as the transaction note, non-greedy (as short as possible) |
| `{{u}}` | Text extracted as the transaction note, greedy (as long as possible) |
| `{{*}}` | Anything irrelevant; skips a dynamic part you don't need |
| `{{x}}` | Transfer-to account; supports all non-whitespace characters |
| `{{r}}` | Project title to match or create |
| `{{f}}` | Foreign currency as an ISO 4217 code |
| `{{g}}` | Transaction time as a Unix timestamp in milliseconds |

Placeholder notes drawn from the changelog:
- Account, payee and project names accept any non-whitespace characters, non-greedy. If you have templates from before 2026-06-22, you may need to adjust them.
- `{{e}}` is non-greedy, and the last placeholder automatically extends to the end of the text. It can match payee names containing spaces. If a template stops working after an update, anchor the payee with a fixed string after it.
- Titles can be partially matched with `%` as a wildcard. For `Money Out $123` use `Money Out %`; for `$567 received` use `% received`.
- The notification body is prefixed with its title, so values can be extracted from the title.
- Amounts with non-breaking spaces and `1,234` style prices are handled.
- Matching works by package name as well.
- Sender and pattern length limits were removed, and an invalid pattern no longer crashes the app.

## Examples
These come from [issue #156](https://github.com/tiberiusteng/financisto1-holo/issues/156), where the maintainer and a user in Colombia worked through real bank messages.

### Example 1: card purchase with currency, payee and amount
The bank message (loosely translated from a Traditional Chinese bank):

```
Card #1234 had made a transaction online at SOME GAMING STORE LTD. with USD 14.99
```

Template:

```
Card #{{a}} had made a transaction online at {{e}} with {{f}} {{p}}
```

The transaction is filed to the account whose **Card Number** field contains `1234`. The payee is filled in or created, and the currency and amount are filled from `{{f}}` and `{{p}}`.

### Example 2: Spanish SMS (Bancolombia)
The message:

```
Bancolombia: Compraste COP63.261,00 en EL RANCHERITO VIVA E con tu T.Cred *0450, el 10/09/2026 a las 12:30. Si tienes dudas, encuentranos aqui: 6045109095 o 018000931987. Estamos cerca.
```

A full-message template works but is longer than it needs to be. You only need the text immediately around each placeholder, to anchor it:

```
Compraste {{f}}{{p}} en {{e}} con tu T.Cred *{{a}},
```

Notes:
- `{{f}}` and `{{p}}` can sit side by side (`COP` then `63.261,00`), with no space between them.
- The heading and trailing fixed parts (the bank name, the date, the phone numbers) can be left out.
- Keep the exact spacing and punctuation from the original message; a space before or after a fixed part changes whether the template matches.

### Example 3: money received (income)
The sign of the amount comes from the template, not from the notification and not from the category. In the template editor there's a plus/minus sign to the left of the account; set it to plus for incoming money. Choosing an Income category in the template is not enough on its own.

### Example 4: paying a credit card from a savings account (transfer)
Create a template that transfers funds from the bank account to the credit card account. In the issue, the template was set up correctly but the app only created an expense on the savings account, because an earlier template in the list matched the same SMS first. Moving the transfer template above it fixed the problem. (The `{{x}}` placeholder is available for the transfer-to account.) See [Template order](#template-order).

## Template order
Templates are processed **from top to bottom**. The first template that matches consumes the notification and creates one transaction. Long press a template in the list and drag it to reorder.

If a more specific template (such as a credit card payment) is listed below a general one (such as any debit), the general one wins. Put the specific templates first.

## Sender field
The sender is matched against the notification title or the sender shown in the notification list. You can use:
- The sender name or number as shown, such as `Bancolombia`
- A **partial match**, such as `85`, when messages arrive from several numbers (for example 85784, 85540 and 87400). A single common digit also works
- The **package name** of the app that posts the notification, for example `com.google.android.apps.messaging` for Google Messages. This is the best choice when the sender differs from message to message. In the notification list, long press the entry to copy its package name

To pick the field's value, open the Notification Template list, press the top-right button to show the notification list, and long press the message you care about.

Long package names used to hit a length limit in the sender field. The limit was removed in the 2026-09-11 release.

## Category precedence
When a transaction is created, the category comes from the first of these that applies:
1. The category selected in the template
2. The last category used with the payee matched by `{{e}}`
3. The last category used with the payee selected in the template

## If the template doesn't create a transaction
Work through this list:
1. **Permission:** grant Financisto Holo notification access. If notifications show "sensitive information hidden", see [issue #142](https://github.com/tiberiusteng/financisto1-holo/issues/142).
2. **Visible notifications:** open the notification list from the template screen. If your message appears there, the app can read it.
3. **Test area:** tap the message in the notification list to copy it, paste it into the template's test area, and check which fields it captures.
4. **Sender:** use the package name or a partial sender, as above.
5. **Account:** edit the account and put the text in the **Card Number** field. The account match won't work without it.
6. **Order:** an earlier template may have consumed the notification.
7. **Wait for a real message:** the test area only proves the pattern; the first real notification confirms the whole chain.

On a Xiaomi/HyperOS phone (Redmi Note 12 Pro+ on Android 14) the setup worked once the package name was used as the sender and the sender-length limit was lifted.

## Choosing the account
Fill in the account's **Card Number** field with the text that appears in the notification, usually the last 4 digits. If several cards belong to one account, separate them with commas or spaces.

<img alt="Account card number" src="assets/account.png" width="40%" />

A template can also match an origin or transfer-to account from the notification title, and create a payee if it doesn't exist.

## Payee extraction
`{{e}}` marks the payee name. If a payee with a matching name is found, the transaction's payee is set to it and the category becomes that payee's last used category. A category chosen in the template always wins over the payee's last category.

**Aliases:** edit a payee and add an alias to make other names match it. For example, if your payee is "McD" but Google Wallet reports "McDonald's Restaurant", put the long name in the alias. An alias also helps search: a payee with alias "shinga" can be found by typing "sh".

You can also preselect the payee and project in the template.

## Transaction note
The note is filled in this order:
1. The custom note defined in the template, with text extracted via `{{t}}` or `{{u}}`.
2. If the template note is empty, text extracted from the notification, when available.
3. If there is still no note and no extraction is defined, the full notification body, when enabled in the config.

## Google Wallet
A Google Wallet payment notification can create a transaction automatically. It requires notification access.
- Name a Financisto account the same as the card shown after "with" in the notification.
- A card number, an account whose card number field contains the last 4 digits, or an account name contained in the card label also match.
- Transactions that have only a currency symbol are treated as local currency.
- The payee name is no longer stored as the note.

## Other details
- A new option matches the notification **group summary**, and the group summary is kept and highlighted. The app also tries to skip group summary notifications to avoid duplicates.
- Templates can fill in the **location** of the transaction.
- Tapping the notification of a generated transaction opens it; going back then returns to the main blotter before dismissing the app.
- The notification list is ordered by notification time.
- Notifications for created or restored scheduled transactions, and for transactions created from templates, are shown again on Android 8+. Sound and vibration are set in Android's system settings.
- The blotter refreshes automatically when new transactions are created in the background.
- Notification processing keeps working after an app update (a bug fix in the 2026-09 releases).

---

[← Back to Home](index.md)
