package tw.tib.financisto.utils;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import tw.tib.financisto.R;
import static tw.tib.financisto.model.Category.isSplit;
import static tw.tib.financisto.utils.Utils.isNotEmpty;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 7/19/11 7:53 PM
 */
public class TransactionTitleUtils {

    private boolean colorizeItem;
    private StringBuilder sb;

    private ForegroundColorSpan categorySpan;
    private ForegroundColorSpan payeeSpan;
    private ForegroundColorSpan locationSpan;
    private ForegroundColorSpan noteSpan;
    private ForegroundColorSpan transferSpan;

    public TransactionTitleUtils(Context context, boolean colorizeItem) {
        this.colorizeItem = colorizeItem;
        this.sb = new StringBuilder();

        Resources r = context.getResources();

        this.categorySpan = new ForegroundColorSpan(r.getColor(R.color.transaction_category));
        this.payeeSpan = new ForegroundColorSpan(r.getColor(R.color.transaction_payee));
        this.locationSpan = new ForegroundColorSpan(r.getColor(R.color.transaction_location));
        this.noteSpan = new ForegroundColorSpan(r.getColor(R.color.transaction_note));
        this.transferSpan = new ForegroundColorSpan(r.getColor(R.color.transfer_color));
    }

    public CharSequence generateTransactionTitle(boolean isTransfer, String payee, String transfer, String note, String location, long categoryId, String category) {
        if (isSplit(categoryId)) {
            return generateTransactionTitleForSplit(payee, transfer, note, location, category);
        } else {
            return generateTransactionTitleForRegular(isTransfer, payee, transfer, note, location, category);
        }
    }

    private CharSequence generateTransactionTitleForRegular(boolean isTransfer, String payee, String transfer, String note, String location, String category) {
        if (this.colorizeItem) {
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            boolean hasText = false;

            if (isNotEmpty(category)) {
                ssb.append(category, categorySpan, 0);
                hasText = true;
            }
            if (isNotEmpty(payee)) {
                if (hasText) ssb.append(" ");
                ssb.append(payee, payeeSpan, 0);
                hasText = true;
            }
            if (isNotEmpty(location)) {
                if (hasText) ssb.append(" ");
                ssb.append(location, locationSpan, 0);
                hasText = true;
            }
            if (isNotEmpty(transfer)) {
                if (hasText) ssb.append(" ");
                ssb.append(transfer, transferSpan, 0);
                hasText = true;
            }
            if (isNotEmpty(note)) {
                if (hasText) ssb.append(" ");
                ssb.append(note, noteSpan, 0);
            }

            return ssb;
        }
        else {
            String secondPart = joinAdditionalFields(payee, transfer, note, location);
            if (isNotEmpty(category)) {
                if (isNotEmpty(secondPart)) {
                    sb.append(category).append(" (").append(secondPart).append(")");
                    return sb.toString();
                } else {
                    return category;
                }
            } else {
                return secondPart;
            }
        }
    }

    private String joinAdditionalFields(String payee, String transfer, String note, String location) {
        sb.setLength(0);
        append(sb, payee);
        append(sb, location);
        append(sb, transfer);
        append(sb, note);
        String secondPart = sb.toString();
        sb.setLength(0);
        return secondPart;
    }

    private CharSequence generateTransactionTitleForSplit(String payee, String transfer, String note, String location, String category) {
        if (this.colorizeItem) {
            SpannableStringBuilder ssb = new SpannableStringBuilder();

            if (isNotEmpty(payee)) {
                ssb.append("[", categorySpan, 0);
                ssb.append(payee, payeeSpan, 0);
                ssb.append("...]", categorySpan, 0);
            }
            else {
                ssb.append("[...]", categorySpan, 0);
            }
            if (isNotEmpty(location)) {
                ssb.append(" ");
                ssb.append(location, locationSpan, 0);
            }
            if (isNotEmpty(transfer)) {
                ssb.append(" ");
                ssb.append(transfer, transferSpan, 0);
            }
            if (isNotEmpty(note)) {
                ssb.append(" ");
                ssb.append(note, noteSpan, 0);
            }

            return ssb;
        }
        else {
            String secondPart = joinAdditionalFields(transfer, note, location);
            if (isNotEmpty(payee)) {
                if (isNotEmpty(secondPart)) {
                    return sb.append("[").append(payee).append("...] ").append(secondPart).toString();
                }
                return sb.append("[").append(payee).append("...]").toString();
            } else {
                if (isNotEmpty(secondPart)) {
                    return sb.append("[...] ").append(secondPart).toString();
                }
                return category;
            }
        }
    }

    private String joinAdditionalFields(String transfer, String note, String location) {
        sb.setLength(0);
        append(sb, location);
        append(sb, transfer);
        append(sb, note);
        String secondPart = sb.toString();
        sb.setLength(0);
        return secondPart;
    }


    private static void append(StringBuilder sb, String s) {
        if (isNotEmpty(s)) {
            if (sb.length() > 0) {
                sb.append(": ");
            }
            sb.append(s);
        }
    }

}
