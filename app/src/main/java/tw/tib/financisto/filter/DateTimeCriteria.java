/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.filter;

import android.content.Context;

import tw.tib.financisto.blotter.BlotterFilter;
import tw.tib.financisto.datetime.DateUtils;
import tw.tib.financisto.datetime.Period;
import tw.tib.financisto.datetime.PeriodType;

import java.util.Calendar;

/**
* Created by IntelliJ IDEA.
* User: denis.solonenko
* Date: 12/17/12 9:06 PM
*/
public class DateTimeCriteria extends Criteria {
    public static final String TAG = "DateTimeCriteria";

    private final Period period;

    public DateTimeCriteria(Period period) {
        super(BlotterFilter.DATETIME, WhereFilter.Operation.BTW, String.valueOf(period.start), String.valueOf(period.end));
        this.period = period;
    }

    public DateTimeCriteria(Context context, PeriodType period) {
        this(DateUtils.getPeriod(context, period));
    }

    public DateTimeCriteria(long start, long end) {
        this(new Period(PeriodType.CUSTOM, start, end));
    }

    public Period getPeriod() {
        return period;
    }

}
