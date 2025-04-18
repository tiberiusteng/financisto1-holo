package tw.tib.financisto.service;

import android.util.Log;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.Payee;
import tw.tib.financisto.model.Project;
import tw.tib.financisto.model.SmsTemplate;
import tw.tib.financisto.model.Transaction;
import tw.tib.financisto.model.TransactionStatus;
import tw.tib.financisto.utils.StringUtil;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.math.BigDecimal.ZERO;
import static java.util.regex.Pattern.DOTALL;
import static tw.tib.financisto.service.SmsTransactionProcessor.Placeholder.*;

public class SmsTransactionProcessor {
    private static final String TAG = SmsTransactionProcessor.class.getSimpleName();
    static BigDecimal HUNDRED = new BigDecimal(100);

    private final DatabaseAdapter db;

    public SmsTransactionProcessor(DatabaseAdapter db) {
        this.db = db;
    }

    /**
     * Parses sms and adds new transaction if it matches any sms template
     * @return new transaction or null if not matched/parsed
     */
    public Transaction createTransactionBySms(String addr, String fullSmsBody, TransactionStatus status, boolean updateNote) {
        List<SmsTemplate> addrTemplates = db.getSmsTemplatesByNumber(addr);
        for (final SmsTemplate template : addrTemplates) {
            String[] match = findTemplateMatches(template.template, fullSmsBody);
            if (match != null) {
                Log.d(TAG, format("Found template \"%s\" with matches \"%s\"", template, Arrays.toString(match)));

                String account = match[ACCOUNT.ordinal()];
                String account_name = match[ACCOUNT_NAME.ordinal()];
                String transfer_to_account_name = match[TRANSFER_TO_ACCOUNT_NAME.ordinal()];
                String parsedPrice = match[PRICE.ordinal()];
                String text = match[TEXT.ordinal()];
                String greedy_text = match[GREEDY_TEXT.ordinal()];
                String payeeText =  match[PAYEE.ordinal()];
                String projectText = match[PROJECT.ordinal()];
                if (text == null && greedy_text != null) {
                    text = greedy_text;
                }
                String note = "";
                if (template.note != null && !template.note.isEmpty()) {
                    if (text == null) {
                        text = "";
                    }
                    note = template.note.replace("{{t}}", text);
                }
                else if (text != null) {
                    note = text;
                }
                else if (updateNote) {
                    note = fullSmsBody;
                }
                try {
                    BigDecimal price = toBigDecimal(parsedPrice);
                    return createNewTransaction(template, price, account, account_name,
                            transfer_to_account_name, payeeText, projectText, note, status);
                } catch (Exception e) {
                    Log.e(TAG, format("Failed to parse price value: \"%s\"", parsedPrice), e);
                }
            }
        }
        return null;
    }

    /**
     * from <a href="https://stackoverflow.com/a/41697399/365675>SO</a>
     */
    static public BigDecimal toBigDecimal(final String value) {
        if (value != null) {
            final String EMPTY = "";
            final char COMMA = ',';
            final String POINT_AS_STRING = ".";
            final char POINT = '.';
            final String COMMA_AS_STRING = ",";

            String trimmed = value.trim();
            boolean negativeNumber =
                ((trimmed.contains("(") && trimmed.contains(")"))
                    || trimmed.endsWith("-")
                    || trimmed.startsWith("-"));

            String parsedValue = value.replaceAll("[^0-9,.]", EMPTY);

            if (negativeNumber) parsedValue = "-" + parsedValue;

            int lastPointPosition = parsedValue.lastIndexOf(POINT);
            int lastCommaPosition = parsedValue.lastIndexOf(COMMA);

            //handle '1423' case, just a simple number
            if (lastPointPosition == -1 && lastCommaPosition == -1) {
                return new BigDecimal(parsedValue);
            }
            //handle '45.3' and '4.550.000' case, only points are in the given String
            if (lastPointPosition > -1 && lastCommaPosition == -1) {
                int firstPointPosition = parsedValue.indexOf(POINT);
                if (firstPointPosition != lastPointPosition)
                    return new BigDecimal(parsedValue.replace(POINT_AS_STRING, EMPTY));
                else
                    return new BigDecimal(parsedValue);
            }
            //handle '45,3' and '4,550,000' case, only commas are in the given String
            //assume decimal part only have at most 2 digits
            if (lastPointPosition == -1 && lastCommaPosition > -1) {
                int firstCommaPosition = parsedValue.indexOf(COMMA);
                if (firstCommaPosition != lastCommaPosition ||
                    lastCommaPosition < (parsedValue.length() - 3))
                    return new BigDecimal(parsedValue.replace(COMMA_AS_STRING, EMPTY));
                else
                    return new BigDecimal(parsedValue.replace(COMMA, POINT));
            }
            //handle '2.345,04' case, points are in front of commas
            if (lastPointPosition < lastCommaPosition) {
                parsedValue = parsedValue.replace(POINT_AS_STRING, EMPTY);
                return new BigDecimal(parsedValue.replace(COMMA, POINT));
            }
            //handle '2,345.04' case, commas are in front of points
            if (lastCommaPosition < lastPointPosition) {
                parsedValue = parsedValue.replace(COMMA_AS_STRING, EMPTY);
                return new BigDecimal(parsedValue);
            }
        }
        throw new NumberFormatException("Unexpected number format. Cannot convert '" + value + "' to BigDecimal.");
    }

    private Transaction createNewTransaction(SmsTemplate smsTemplate, BigDecimal price,
        String accountDigits,
        String accountName,
        String transferToAccountName,
        String payeeText,
        String projectText,
        String note,
        TransactionStatus status) {
        Transaction res = null;
        long accountId = 0;
        long transferToAccountId = 0;
        if (accountName != null) {
            accountId = db.getEntityIdByTitle(Account.class, accountName);
        }
        if (accountId == 0) {
            accountId = findAccount(accountDigits, smsTemplate.accountId);
        }
        if (transferToAccountName != null) {
            transferToAccountId = db.getEntityIdByTitle(Account.class, transferToAccountName);
        }
        if (transferToAccountId == 0 && smsTemplate.toAccountId != -1) {
            transferToAccountId = smsTemplate.toAccountId;
        }
        Payee payee = null;
        Project project = null;
        if (payeeText != null) {
            payee = db.findOrInsertEntityByTitle(Payee.class, payeeText);
        }
        if (projectText != null) {
            project = db.findOrInsertEntityByTitle(Project.class, projectText);
        }
        Log.d(TAG, format("payee=%s project=%s template.payeeId=%s template.projectId=%s",
                payee, project, smsTemplate.payeeId, smsTemplate.projectId));
        if (price.compareTo(ZERO) > 0 && accountId > 0) {
            res = new Transaction();
            res.isTemplate = 0;
            res.fromAccountId = accountId;

            if (payee != null) {
                res.payeeId = payee.id;
                res.categoryId = payee.lastCategoryId;
            }
            else if (smsTemplate.payeeId != Payee.EMPTY.id) {
                Payee templatePayee = db.get(Payee.class, smsTemplate.payeeId);
                if (templatePayee != null) {
                    res.payeeId = smsTemplate.payeeId;
                    res.categoryId = templatePayee.lastCategoryId;
                }
            }

            if (project != null) {
                res.projectId = project.id;
            } else if (smsTemplate.projectId != Project.NO_PROJECT_ID) {
                Project templateProject = db.get(Project.class, smsTemplate.projectId);
                if (templateProject != null) {
                    res.projectId = smsTemplate.projectId;
                }
            }

            res.fromAmount = (smsTemplate.isIncome ? 1 : -1) * Math.abs(price.multiply(HUNDRED).longValue());
            if (transferToAccountId != 0) {
                res.toAccountId = transferToAccountId;
                res.toAmount = (smsTemplate.isIncome ? -1 : 1) * Math.abs(price.multiply(HUNDRED).longValue());
            }
            res.note = note;
            if (smsTemplate.categoryId != 0) {
                res.categoryId = smsTemplate.categoryId;
            }
            res.status = status;
            long id = db.insertOrUpdate(res);
            res.id = id;

            Log.i(TAG, format("Transaction `%s` was added with id=%s", res, id));
        } else {
            Log.e(TAG, format("Account not found or price wrong for `%s` sms template", smsTemplate));
        }
        return res;
    }

    private long findAccount(String accountLastDigits, long defaultId) {
        long res = defaultId;
        long matchedAccId = findAccountByCardNumber(accountLastDigits);
        if (matchedAccId > 0) {
            res = matchedAccId;
            Log.d(TAG, format("Found account %s by sms match: `%s`", matchedAccId, accountLastDigits));
        }
        return res;
    }

    private long findAccountByCardNumber(String accountEnding) {
        long res = -1;

        if (!StringUtil.isEmpty(accountEnding)) {
            List<Long> accountIds = db.findAccountsByNumber(accountEnding);
            if (!accountIds.isEmpty()) {
                res = accountIds.get(0);
                if (accountIds.size() > 1) {
                    Log.e(TAG, format("Accounts ending with `%s` - more than one!", accountEnding));
                }
            }
        }
        return res;
    }

    /**
     * Finds template matches or null if none
     * ex. ECMC<:A:> <:D:> покупка <:P:> TEREMOK <::>Баланс: <:B:>р
     */
    public static String[] findTemplateMatches(String template, final String sms) {
        Log.d(TAG, "findTemplateMatches template=\"" + template + "\", sms=\"" + sms + "\"");

        String[] results = null;
        template = preprocessPatterns(template);
        final int[] phIndexes = findPlaceholderIndexes(template);

        if (phIndexes != null) {
            // escape regex characters (i.e. can't use regex in template)
            template = template.replaceAll("([.\\[\\]{}()*+\\-?^$|])", "\\\\$1");
            for (int i = 0; i < phIndexes.length; i++) {
                if (phIndexes[i] != -1) {
                    Placeholder placeholder = Placeholder.values()[i];
                    template = template.replace(placeholder.code, placeholder.regexp);
                }
            }
            template = template.replace(ANY.code, ANY.regexp);
            Log.d(TAG, "template=" + template);

            Matcher matcher = Pattern.compile(template, DOTALL).matcher(sms);
            if (matcher.find()) {
                results = new String[Placeholder.values().length];
                for (int i = 0; i < phIndexes.length; i++) {
                    final int groupNum = phIndexes[i] + 1;
                    if (groupNum > 0) {
                        results[i] = matcher.group(groupNum);
                    }
                }
            }
        }
        return results;
    }

    private static String preprocessPatterns(String template) {
        String res = template;
        for (Placeholder ph : Placeholder.values()) {
            if (ph.synonyms.length > 0) {
                for (String synonym : ph.synonyms) {
                    res = StringUtil.replaceAllIgnoreCase(res, synonym, ph.code);
                }
            }
        }
        return res;
    }

    /**
     * @return null if not found Price placeholder
     */
    static int[] findPlaceholderIndexes(String template) {
        Map<Integer, Placeholder> sorted = new TreeMap<>();
        boolean foundPrice = false;
        for (Placeholder p : Placeholder.values()) {
            int i = template.indexOf(p.code);
            if (i >= 0) {
                if (p == PRICE) {
                    foundPrice = true;
                }
                if (p != ANY) {
                    sorted.put(i, p);
                }
            }
        }
        int[] result = null;
        if (foundPrice) {
            result = new int[Placeholder.values().length];
            Arrays.fill(result, -1);
            int i = 0;
            for (Placeholder p : sorted.values()) {
                result[p.ordinal()] = i++;
            }
        }
        return result;
    }


    public enum Placeholder {
        /**
         * Please note that order of constants is very important,
         * and keep it in alphabetical way
         */
        ANY("<::>", ".*?", "{{*}}"),
        ACCOUNT("<:A:>", "\\s{0,3}(\\d{4})\\s{0,3}", "{{a}}"),
        BALANCE("<:B:>", "\\s{0,3}([\\d\\.,\\-\\+\\']+(?:[\\d \\xA0\\.,]+?)*)\\s{0,3}", "{{b}}"),
        ACCOUNT_NAME("<:C:>", "(\\w+?)", "{{c}}"),
        DATE("<:D:>", "\\s{0,3}(\\d[\\d\\. /:-]{12,14}\\d)\\s*?", "{{d}}"),
        PAYEE("<:E:>", "(\\w+?)", "{{e}}"),
        PRICE("<:P:>", BALANCE.regexp, "{{p}}"),
        PROJECT("<:R:>", "(\\w+?)", "{{r}}"),
        TEXT("<:T:>", "(.*?)", "{{t}}"),
        GREEDY_TEXT("<:U:>", "(.*)", "{{u}}"),
        TRANSFER_TO_ACCOUNT_NAME("<:X:>", "(\\w+?)", "{{x}}");

        public String code;
        public String regexp;
        public String[] synonyms;

        Placeholder(String code, String regexp, String ... synonyms) {
            this.code = code;
            this.regexp = regexp;
            this.synonyms = synonyms;
        }
    }
}
