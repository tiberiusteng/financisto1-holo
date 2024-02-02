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
package tw.tib.financisto.widget;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import androidx.core.content.ContextCompat;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.ViewsById;

import java.math.BigDecimal;
import java.util.List;
import java.util.Stack;

import tw.tib.financisto.R;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.Utils;

@EFragment(R.layout.calculator)
public class CalculatorInput extends DialogFragment {

    @ViewById(R.id.result)
    protected TextView tvResult;

    @ViewById(R.id.op)
    protected TextView tvOp;

    @ViewsById({R.id.b0, R.id.b1, R.id.b2, R.id.b3,
            R.id.b4, R.id.b5, R.id.b6, R.id.b7, R.id.b8, R.id.b9, R.id.bAdd,
            R.id.bSubtract, R.id.bDivide, R.id.bMultiply, R.id.bPercent,
            R.id.bPlusMinus, R.id.bDot, R.id.bResult, R.id.bClear, R.id.bDelete,
            R.id.bSTO, R.id.bX, R.id.bY, R.id.bZ, R.id.bT})
    protected List<Button> buttons;

    @SystemService
    protected Vibrator vibrator;

    @FragmentArg
    protected String amount;

    private final Stack<String> stack = new Stack<>();
    private String result = "0";
    private boolean isRestart = true;
    private boolean isInEquals = false;
    private boolean isInStore = false;
    private char lastOp = '\0';
    private AmountListener listener;
    private SharedPreferences prefs;

    public void setListener(AmountListener listener) {
        this.listener = listener;
    }

    @AfterInject
    public void init() {
        prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
    }

    @AfterViews
    public void initUi() {
        //int bgColorResource = R.color.calculator_background;
        //int bgColor = ContextCompat.getColor(getActivity(), bgColorResource);
        //getView().setBackgroundColor(bgColor);
        setDisplay(amount);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Click({R.id.b0, R.id.b1, R.id.b2, R.id.b3,
            R.id.b4, R.id.b5, R.id.b6, R.id.b7, R.id.b8, R.id.b9, R.id.bAdd,
            R.id.bSubtract, R.id.bDivide, R.id.bMultiply, R.id.bPercent,
            R.id.bPlusMinus, R.id.bDot, R.id.bResult, R.id.bClear, R.id.bDelete,
            R.id.bSTO, R.id.bX, R.id.bY, R.id.bZ, R.id.bT})
    public void onButtonClick(View v) {
        Button b = (Button) v;
        char c = b.getText().charAt(0);
        onButtonClick(c, v);
    }

    @Click(R.id.bOK)
    public void onOk() {
        if (!isInEquals) {
            doEqualsChar();
        }
        listener.onAmountChanged(result);
        dismiss();
    }

    @Click(R.id.bCancel)
    public void onCancel() {
        dismiss();
    }

    private void setDisplay(String s) {
        if (Utils.isNotEmpty(s)) {
            s = s.replaceAll(",", ".");
            result = s;
            tvResult.setText(s);
        }
    }

    private void onButtonClick(char c, View v) {
        if (MyPreferences.isPinHapticFeedbackEnabled(getActivity())) {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        switch (c) {
            case 'C':
                resetAll();
                break;
            case '<':
                doBackspace();
                break;
            case 'S':
            case 'X':
            case 'Y':
            case 'Z':
            case 'T':
                doMemories(c);
                break;
            default:
                doButton(c);
                break;
        }
    }

    private void resetAll() {
        setDisplay("0");
        tvOp.setText("");
        lastOp = '\0';
        isRestart = true;
        stack.clear();
    }

    private void doBackspace() {
        String s = tvResult.getText().toString();
        if ("0".equals(s) || isRestart) {
            return;
        }
        String newDisplay = s.length() > 1 ? s.substring(0, s.length() - 1) : "0";
        if ("-".equals(newDisplay)) {
            newDisplay = "0";
        }
        setDisplay(newDisplay);
    }

    private void doMemories(char c) {
        if (c == 'S') {
                isInStore = !isInStore;
        } else {
            if (isInStore) {
                prefs.edit().putString("M" + c, result).apply();
                isInStore = false;
            } else {
                setDisplay(prefs.getString("M" + c, "0"));
            }
        }

        if (isInStore) {
            tvOp.setText("STO");
        } else {
            tvOp.setText("");
        }
    }

    private void doButton(char c) {
        isInStore = false;

        if (Character.isDigit(c) || c == '.') {
            addChar(c);
        } else {
            switch (c) {
                case '+':
                case '-':
                case '/':
                case '*':
                    doOpChar(c);
                    break;
                case '%':
                    doPercentChar();
                    break;
                case '=':
                case '\r':
                    doEqualsChar();
                    break;
                case '\u00B1':
                    setDisplay(new BigDecimal(result).negate().toPlainString());
                    break;
            }
        }
    }

    private void addChar(char c) {
        String s = tvResult.getText().toString();
        if (c == '.' && s.indexOf('.') != -1 && !isRestart) {
            return;
        }
        if ("0".equals(s)) {
            s = String.valueOf(c);
        } else {
            s += c;
        }
        setDisplay(s);
        if (isRestart) {
            setDisplay(String.valueOf(c));
            isRestart = false;
        }
    }

    private void doOpChar(char op) {
        if (isInEquals) {
            stack.clear();
            isInEquals = false;
        }
        stack.push(result);
        doLastOp();
        lastOp = op;
        tvOp.setText(String.valueOf(lastOp));
    }

    private void doLastOp() {
        isRestart = true;
        if (lastOp == '\0' || stack.size() == 1) {
            return;
        }

        String valTwo = stack.pop();
        String valOne = stack.pop();
        switch (lastOp) {
            case '+':
                stack.push(new BigDecimal(valOne).add(new BigDecimal(valTwo)).stripTrailingZeros().toPlainString());
                break;
            case '-':
                stack.push(new BigDecimal(valOne).subtract(new BigDecimal(valTwo)).stripTrailingZeros().toPlainString());
                break;
            case '*':
                stack.push(new BigDecimal(valOne).multiply(new BigDecimal(valTwo)).stripTrailingZeros().toPlainString());
                break;
            case '/':
                BigDecimal d2 = new BigDecimal(valTwo);
                if (d2.intValue() == 0) {
                    stack.push("0.0");
                } else {
                    stack.push(new BigDecimal(valOne).divide(d2, 2, BigDecimal.ROUND_HALF_UP).stripTrailingZeros().toPlainString());
                }
                break;
            default:
                break;
        }
        setDisplay(stack.peek());
        if (isInEquals) {
            stack.push(valTwo);
        }
    }

    private void doPercentChar() {
        if (stack.size() == 0)
            return;
        setDisplay(new BigDecimal(result).divide(Utils.HUNDRED).multiply(new BigDecimal(stack.peek())).toPlainString());
        tvOp.setText("");
    }

    private void doEqualsChar() {
        if (lastOp == '\0') {
            return;
        }
        if (!isInEquals) {
            isInEquals = true;
            stack.push(result);
        }
        doLastOp();
        tvOp.setText("");
    }

}
