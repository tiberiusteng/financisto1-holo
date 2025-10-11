package tw.tib.financisto.export.qif;

import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.AccountType;
import tw.tib.financisto.model.Currency;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/7/11 8:03 PM
 */
public class QifAccount {

    public String type = "";
    public String memo = "";

    public Account dbAccount;
    public final List<QifTransaction> transactions = new ArrayList<QifTransaction>();

    public static QifAccount fromAccount(Account account) {
        QifAccount qifAccount = new QifAccount();
        qifAccount.type = decodeAccountType(account.type);
        qifAccount.memo = account.title;
        return qifAccount;
    }

    public Account toAccount(Currency currency) {
        Account a = new Account();
        a.id = -1;
        a.currency = currency;
        a.title = memo;
        a.type = encodeAccountType(type);
        a.icon = "";
        a.accentColor = "";
        return a;
    }

    /*
    !Type:Bank	Bank account transactions
    !Type:Cash	Cash account transactions
    !Type:CCard	Credit card account transactions
    !Type:Invst	Investment account transactions
    !Type:Oth A	Asset account transactions
    !Type:Oth L	Liability account transactions
    */
    private static String decodeAccountType(String type) {
        AccountType t = AccountType.valueOf(type);
        switch (t) {
            case BANK:
                return "Bank";
            case CASH:
                return "Cash";
            case CREDIT_CARD:
                return "CCard";
            case ASSET:
                return "Oth A";
            default:
                return "Oth L";
        }
    }

    private String encodeAccountType(String type) {
        if ("Bank".endsWith(type))
            return AccountType.BANK.name();
        else if ("Cash".equals(type))
            return AccountType.CASH.name();
        else if ("CCard".equals(type))
            return AccountType.CREDIT_CARD.name();
        else if ("Oth A".equals(type))
            return AccountType.ASSET.name();
        else if ("Oth L".equals(type))
            return AccountType.LIABILITY.name();
        else
            return AccountType.OTHER.name();
    }

    public void writeTo(QifBufferedWriter w) throws IOException {
        w.writeAccountsHeader();
        w.write("N").write(memo).newLine();
        w.write("T").write(type).newLine();
        w.end();
    }

    public void readFrom(QifBufferedReader r) throws IOException {
        String line;
        while ((line = r.readLine()) != null) {
            if (line.startsWith("^")) {
                break;
            }
            if (line.startsWith("N")) {
                this.memo = QifUtils.trimFirstChar(line);
            } else if (line.startsWith("T")) {
                this.type = QifUtils.trimFirstChar(line);
            }
        }
    }
}
