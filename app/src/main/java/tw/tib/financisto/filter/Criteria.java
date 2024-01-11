/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.filter;

import android.content.Intent;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.Arrays;
import java.util.LinkedList;

import tw.tib.financisto.utils.StringUtil;
import tw.tib.orb.Expression;
import tw.tib.orb.Expressions;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 12/17/12 9:06 PM
 */
public class Criteria {
    private static final String TAG = "Criteria";

    public static Criteria eq(String column, String value) {
        return new Criteria(column, WhereFilter.Operation.EQ, value);
    }

    public static Criteria neq(String column, String value) {
        return new Criteria(column, WhereFilter.Operation.NEQ, value);
    }

    public static Criteria btw(String column, String... values) {
        if (values.length < 2) throw new IllegalArgumentException("No values for BTW filter!");
        return new Criteria(column, WhereFilter.Operation.BTW, values);
    }

    public static Criteria in(String column, String... values) {
        if (values.length == 0) throw new IllegalArgumentException("No values for IN filter!");
        return new Criteria(column, WhereFilter.Operation.IN, values);
    }

    public static Criteria gt(String column, String value) {
        return new Criteria(column, WhereFilter.Operation.GT, value);
    }

    public static Criteria gte(String column, String value) {
        return new Criteria(column, WhereFilter.Operation.GTE, value);
    }

    public static Criteria lt(String column, String value) {
        return new Criteria(column, WhereFilter.Operation.LT, value);
    }

    public static Criteria lte(String column, String value) {
        return new Criteria(column, WhereFilter.Operation.LTE, value);
    }

    public static Criteria isNull(String column) {
        return new Criteria(column, WhereFilter.Operation.ISNULL);
    }

    public static Criteria like(String column, String text) {
        return new Criteria(column, WhereFilter.Operation.LIKE, text);
    }

    public static Criteria raw(String text) {
        return new Criteria("(" + text + ")", WhereFilter.Operation.NOPE);
    }

    public static Criteria or(Criteria... children) {
        Log.d(TAG, "Criteria or() children.length=" + children.length);
        return new Criteria(children[0].columnName, WhereFilter.Operation.OR, combineValues(children), children);
    }

    public static Criteria and(Criteria... children) {
        Log.d(TAG, "Criteria and() children.length=" + children.length);
        return new Criteria(children[0].columnName, WhereFilter.Operation.AND, combineValues(children), children);
    }

    private static String[] combineValues(Criteria... children) {
        LinkedList<String> values = new LinkedList<>();
        for (Criteria c : children) {
            values.addAll(Arrays.asList(c.getValues()));
        }
        String[] ret = new String[values.size()];
        return values.toArray(ret);
    }
    
    public final String columnName;
    public final WhereFilter.Operation operation;
    private final String[] values;
    private final Criteria[] children;

    public Criteria(String columnName, WhereFilter.Operation operation, String... values) {
        this.columnName = columnName;
        this.operation = operation;
        this.values = values;
        this.children = new Criteria[0];
    }

    public Criteria(String columnName, WhereFilter.Operation operation, String[] values, Criteria... children) {
        this.columnName = columnName;
        this.operation = operation;
        this.values = values;
        this.children = children;
    }

    public boolean isNull() {
        return operation == WhereFilter.Operation.ISNULL;
    }
    
    @Deprecated // todo.mb: not used, can be removed
    public Expression toWhereExpression() {
        switch (operation) {
            case EQ:
                return Expressions.eq(columnName, getLongValue1());
            case GT:
                return Expressions.gt(columnName, getLongValue1());
            case GTE:
                return Expressions.gte(columnName, getLongValue1());
            case LT:
                return Expressions.lt(columnName, getLongValue1());
            case LTE:
                return Expressions.lte(columnName, getLongValue1());
            case BTW:
                return Expressions.btw(columnName, getLongValue1(), getLongValue2());
            case LIKE:
                return Expressions.like(columnName, getStringValue());
        }
        throw new IllegalArgumentException();
    }

    public String toStringExtra() {
        JsonArray ret = new JsonArray();
        if (this instanceof DateTimeCriteria) {
            ret.add(new JsonPrimitive(DateTimeCriteria.TAG));
        }
        else {
            ret.add(new JsonPrimitive(Criteria.TAG));
        }
        ret.add(new Gson().toJsonTree(this));
        return ret.toString();
    }

    public static Criteria fromStringExtra(String extra) {
        Log.d(TAG, "fromStringExtra: " + extra);

        JsonArray array = new JsonParser().parse(extra).getAsJsonArray();
        String typeTag = array.get(0).getAsString();
        if (typeTag.equals(DateTimeCriteria.TAG)) {
            return new Gson().fromJson(array.get(1), DateTimeCriteria.class);
        }

        return new Gson().fromJson(array.get(1), Criteria.class);
    }

    public String[] getValues() {
        return values;
    }

    public String getStringValue() {
        return values.length > 0 ? values[0] : "";
    }

    public int getIntValue() {
        return values.length > 0 ? Integer.parseInt(values[0]) : -1;
    }

    public long getLongValue1() {
        return values.length > 0 ? Long.parseLong(values[0]) : -1;
    }

    public long getLongValue2() {
        return values.length > 1 ? Long.parseLong(values[1]) : -1;
    }

    public String getSelection() {
        if (operation == WhereFilter.Operation.AND || operation == WhereFilter.Operation.OR)
        {
            String[] childSelection = new String[children.length];
            for (int i = 0; i< children.length; ++i) {
                childSelection[i] = children[i].getSelection();
            }
            return "(" + String.join(" " + operation.getOp(0) + " ", childSelection) + ")";
        }

        String exp = columnName + " " + operation.getOp(getSelectionArgs().length);
        if (operation.getGroupOp() != null && getValues().length > operation.getValsPerGroup()) {
            int groupNum = getValues().length / operation.getValsPerGroup();
            String groupDelim = " " + operation.getGroupOp() + " ";
            return  "(" + StringUtil.generateSeparated(exp, groupDelim, groupNum) + ")";
        }
        return exp;
    }

    public int size() {
        return values != null ? values.length : 0;
    }

    public String[] getSelectionArgs() {
        if (children.length > 0) {
            LinkedList<String> args = new LinkedList<>();
            for (Criteria c : children) {
                args.addAll(Arrays.asList(c.getSelectionArgs()));
            }
            String[] ret = new String[args.size()];
            ret = args.toArray(ret);
            return ret;
        }
        return values;
    }

    public void toIntent(String title, Intent intent) {
        intent.putExtra(WhereFilter.TITLE_EXTRA, title);
        intent.putExtra(WhereFilter.FILTER_EXTRA, new String[]{toStringExtra()});
    }

}
