/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package tw.tib.financisto.export.csv;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import tw.tib.financisto.datetime.DateUtils;
import tw.tib.financisto.db.DatabaseHelper;
import tw.tib.financisto.export.Export;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.*;
import tw.tib.financisto.model.Category;
import tw.tib.financisto.model.MyLocation;
import tw.tib.financisto.model.Payee;
import tw.tib.financisto.model.Project;
import tw.tib.financisto.utils.CurrencyCache;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.Transaction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class CsvExport extends Export {
    private static final String TAG = "CsvExport";

    static final String[] HEADER = "txid,date,time,status,account,amount,balance,currency,to account,to amount,to balance,to currency,original amount,original currency,category,parent,payee,location,project,note".split(",");
    static final String TXID_HEADER = "txid";
    static final String STATUS_HEADER = "status";
    static final String BALANCE_HEADER = "balance";
    static final String TO_ACCOUNT_HEADER = "to account";
    static final String TO_AMOUNT_HEADER = "to amount";
    static final String TO_BALANCE_HEADER = "to balance";
    static final String TO_CURRENCY_HEADER = "to currency";

    private static final MyLocation TRANSFER_IN = new MyLocation();
    private static final MyLocation TRANSFER_OUT = new MyLocation();

    static {
        TRANSFER_IN.title = "Transfer In";
        TRANSFER_OUT.title = "Transfer Out";
    }

    private final DatabaseAdapter db;
    private final CsvExportOptions options;

    private CSVFormat csvFormat;

    private Map<Long, Category> categoriesMap;
    private Map<Long, Account> accountsMap;
    private Map<Long, Payee> payeeMap;
    private Map<Long, Project> projectMap;
    private Map<Long, MyLocation> locationMap;
    private Map<Long, Attribute> attributeMap;
    private Set<Long> usedAttributes;

    public CsvExport(Context context, DatabaseAdapter db, CsvExportOptions options) {
        super(context, false);
        this.db = db;
        this.options = options;

        Log.d(TAG, "useCurrencySpecificDecimals=" + options.useCurrencySpecificDecimals);

        csvFormat = CSVFormat.EXCEL.builder().setDelimiter(options.fieldSeparator).get();
    }

    @Override
    protected String getExtension() {
        return ".csv";
    }

    @Override
    protected void writeHeader(BufferedWriter bw) throws IOException {
        if (options.writeUtfBom) {
            byte[] bom = new byte[3];
            bom[0] = (byte) 0xEF;
            bom[1] = (byte) 0xBB;
            bom[2] = (byte) 0xBF;
            bw.write(new String(bom, "UTF-8"));
        }
        if (options.includeHeader) {
            CSVPrinter p = csvFormat.print(bw);
            for (String h : HEADER) {
                if (h.equals(TXID_HEADER) && !options.exportTxIDs) continue;
                if (h.equals(STATUS_HEADER) && !options.includeTxStatus) continue;
                if (h.equals(BALANCE_HEADER) && !options.exportRunningBalance) continue;
                if (h.equals(TO_ACCOUNT_HEADER) && !options.exportTransferInSingleLine) continue;
                if (h.equals(TO_AMOUNT_HEADER) && !options.exportTransferInSingleLine) continue;
                if (h.equals(TO_BALANCE_HEADER) && !(options.exportTransferInSingleLine && options.exportRunningBalance)) continue;
                if (h.equals(TO_CURRENCY_HEADER) && !options.exportTransferInSingleLine) continue;
                p.print(h);
            }
            if (options.exportAttributes) {
                attributeMap = db.getAllAttributesByIdMap();
                // only export attributes actually used by exporting transactions
                usedAttributes = new LinkedHashSet<>();
                try (Cursor c = db.getBlotterWithSplits(options.filter)) {
                    while (c.moveToNext()) {
                        long id = c.getLong(DatabaseHelper.BlotterColumns._id.ordinal());
                        Map<Long, String> attrs = db.getAllAttributesForTransaction(id);
                        usedAttributes.addAll(attrs.keySet());
                    }
                }
                for (long i : usedAttributes) {
                    p.print("attr:" + attributeMap.get(i).title);
                }
            }
            p.println();
        }
    }

    @Override
    protected void writeBody(BufferedWriter bw) throws IOException {
        try (CSVPrinter p = csvFormat.print(bw)) {
            accountsMap = db.getAllAccountsMap();
            categoriesMap = db.getAllCategoriesMap();
            payeeMap = db.getAllPayeeByIdMap();
            projectMap = db.getAllProjectsByIdMap(false);
            locationMap = db.getAllLocationsByIdMap(false);
            try (Cursor c = db.getBlotterWithSplits(options.filter)) {
                while (c.moveToNext()) {
                    Transaction t = Transaction.fromBlotterCursor(c);
                    writeLine(p, t);
                }
            }
        }
    }

    private void writeLine(CSVPrinter p, Transaction t) throws IOException {
        Date dt = t.dateTime > 0 ? new Date(t.dateTime) : null;
        Category category = getCategoryById(t.categoryId);
        Project project = getProjectById(t.projectId);
        Account fromAccount = getAccount(t.fromAccountId);
        if (t.isTransfer()) {
            Account toAccount = getAccount(t.toAccountId);
            if (options.exportTransferInSingleLine) {
                writeLine(p, t.id, dt, t.status, fromAccount.title, t.fromAmount, t.fromAccountBalance, fromAccount.currency.id,
                        toAccount.title, t.toAmount, t.toAccountBalance, toAccount.currency.id,
                        0, 0,
                        category, null, null, project, t.note);
            }
            else {
                writeLine(p, t.id, dt, t.status, fromAccount.title, t.fromAmount, t.fromAccountBalance, fromAccount.currency.id,
                        null, 0, 0, 0,
                        0, 0,
                        category, null, TRANSFER_OUT, project, t.note);
                writeLine(p, t.id, dt, t.status, toAccount.title, t.toAmount, t.toAccountBalance, toAccount.currency.id,
                        null, 0, 0, 0,
                        0, 0,
                        category, null, TRANSFER_IN, project, t.note);
            }
        } else {
            boolean isSplit = (category != null && category.isSplit());
            MyLocation location = getLocationById(t.locationId);
            Payee payee = getPayee(t.payeeId);
            if ((t.parentId == 0 && (!isSplit || options.exportSplitParents)) ||
                (t.parentId != 0 && options.exportSplits))
            {
                writeLine(p, t.id, dt, t.status, fromAccount.title, t.fromAmount, t.fromAccountBalance, fromAccount.currency.id,
                        null, 0, 0, 0,
                        t.originalFromAmount, t.originalCurrencyId,
                        category, payee, location, project, t.note);
            }
        }
    }

    private void writeLine(CSVPrinter p, long transactionId, Date dt, TransactionStatus status,
                           String account, long amount, long balance, long currencyId,
                           String toAccount, long toAmount, long toBalance, long toCurrencyId,
                           long originalAmount, long originalCurrencyId,
                           Category category, Payee payee, MyLocation location, Project project, String note) throws IOException {
        if (options.exportTxIDs) {
            p.print(String.valueOf(transactionId));
        }
        if (dt != null) {
            p.print(DateUtils.FORMAT_DATE_ISO_8601.format(dt));
            p.print(DateUtils.FORMAT_TIME_ISO_8601.format(dt));
        } else {
            p.print("~");
            p.print("");
        }
        if (options.includeTxStatus) {
            p.print(status.toString());
        }
        // from account
        p.print(account);
        Currency c = CurrencyCache.getCurrency(currencyId);
        String amountFormatted;
        if (options.useCurrencySpecificDecimals) {
            amountFormatted = options.getCurrencyAmountFormat(currencyId).format(new BigDecimal(amount).movePointLeft(c.getScale()));
        }
        else {
            amountFormatted = options.amountFormat.format(new BigDecimal(amount).movePointLeft(c.getScale()));
        }
        p.print(amountFormatted);
        if (options.exportRunningBalance) {
            String balanceFormatted;
            if (options.useCurrencySpecificDecimals) {
                balanceFormatted = options.getCurrencyAmountFormat(currencyId).format(new BigDecimal(balance).movePointLeft(c.getScale()));
            }
            else {
                balanceFormatted = options.amountFormat.format(new BigDecimal(balance).movePointLeft(c.getScale()));
            }
            p.print(balanceFormatted);
        }
        p.print(c.name);
        // to account
        if (options.exportTransferInSingleLine) {
            p.print(toAccount);
            if (toAccount == null) {
                p.print(null); // to amount
                if (options.exportRunningBalance) {
                    p.print(null); // to balance
                }
                p.print(null); // to currency
            }
            else {
                c = CurrencyCache.getCurrency(toCurrencyId);
                if (options.useCurrencySpecificDecimals) {
                    amountFormatted = options.getCurrencyAmountFormat(toCurrencyId).format(
                            new BigDecimal(toAmount).movePointLeft(c.getScale()));
                }
                else {
                    amountFormatted = options.amountFormat.format(
                            new BigDecimal(toAmount).movePointLeft(c.getScale()));
                }
                p.print(amountFormatted);
                if (options.exportRunningBalance) {
                    String balanceFormatted;
                    if (options.useCurrencySpecificDecimals) {
                        balanceFormatted = options.getCurrencyAmountFormat(toCurrencyId).format(
                                new BigDecimal(toBalance).movePointLeft(c.getScale()));
                    }
                    else {
                        balanceFormatted = options.amountFormat.format(
                                new BigDecimal(toBalance).movePointLeft(c.getScale()));
                    }
                    p.print(balanceFormatted);
                }
                p.print(c.name);
            }
        }
        if (originalCurrencyId > 0) {
            Currency originalCurrency = CurrencyCache.getCurrency(originalCurrencyId);
            if (options.useCurrencySpecificDecimals) {
                p.print(options.getCurrencyAmountFormat(originalCurrencyId).format(
                        new BigDecimal(originalAmount).movePointLeft(originalCurrency.getScale())));
            }
            else {
                p.print(options.amountFormat.format(
                        new BigDecimal(originalAmount).movePointLeft(originalCurrency.getScale())));
            }
            p.print(originalCurrency.name);
        } else {
            p.print("");
            p.print("");
        }
        p.print(category != null ? category.title : "");
        String sParent = buildPath(category);
        p.print(sParent);
        p.print(payee != null ? payee.title : "");
        p.print(location != null ? location.title : "");
        p.print(project != null ? project.title : "");
        p.print(note);
        if (options.exportAttributes) {
            Map<Long, String> attrs = db.getAllAttributesForTransaction(transactionId);
            for (long i : usedAttributes) {
                String value = attrs.get(i);
                if (value != null) {
                    p.print(value);
                }
                else {
                    p.print("");
                }
            }
        }
        p.println();
    }

    private String buildPath(Category category) {
        if (category == null || category.parent == null) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder(category.parent.title);
            for (Category cat = category.parent.parent; cat != null; cat = cat.parent) {
                sb.insert(0, ":").insert(0, cat.title);
            }
            return sb.toString();
        }
    }

    @Override
    protected void writeFooter(BufferedWriter bw) throws IOException {
    }

    private Account getAccount(long accountId) {
        return accountsMap.get(accountId);
    }

    public Category getCategoryById(long id) {
        Category category = categoriesMap.get(id);
        if (category.id == 0) return null;
        if (category.isSplit()) {
            category.title = "SPLIT";
        }
        return category;
    }

    private Payee getPayee(long payeeId) {
        return payeeMap.get(payeeId);
    }

    private Project getProjectById(long projectId) {
        return projectMap.get(projectId);
    }

    private MyLocation getLocationById(long locationId) {
        return locationMap.get(locationId);
    }

}
