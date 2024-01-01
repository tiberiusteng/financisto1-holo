package tw.tib.financisto.recur;

import com.google.ical.iter.RecurrenceIterator;
import com.google.ical.iter.RecurrenceIteratorFactory;
import com.google.ical.util.TimeUtils;
import com.google.ical.values.RRule;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import static tw.tib.financisto.recur.RecurrencePeriod.dateToDateValue;
import static tw.tib.financisto.recur.RecurrencePeriod.dateValueToDate;

public class DateRecurrenceIterator {

	private final RecurrenceIterator ri;
    private Date firstDate;
    private boolean isStartDateInDaylight;

	private DateRecurrenceIterator(RecurrenceIterator ri) {
		this.ri = ri;
	}

	public boolean hasNext() {
		return firstDate != null || ri.hasNext();
	}

	public Date next() {
        if (firstDate != null) {
            Date date = firstDate;
            firstDate = null;
            return date;
        }
        return dateValueToDate(ri.next(), isStartDateInDaylight);
	}

	public static DateRecurrenceIterator create(RRule rrule, Date nowDate, Date startDate) throws ParseException {
        RecurrenceIterator ri = RecurrenceIteratorFactory.createRecurrenceIterator(rrule,
                dateToDateValue(startDate), Calendar.getInstance().getTimeZone());
        Date date = null;
        boolean isStartDateInDaylight = Calendar.getInstance().getTimeZone().inDaylightTime(startDate);
        while (ri.hasNext() && (date = dateValueToDate(ri.next(), isStartDateInDaylight)).before(nowDate));
        //ri.advanceTo(dateToDateValue(nowDate));
        DateRecurrenceIterator iterator = new DateRecurrenceIterator(ri);
        iterator.isStartDateInDaylight = isStartDateInDaylight;
        iterator.firstDate = date;
        return iterator;
	}

    public static DateRecurrenceIterator empty() {
        return new EmptyDateRecurrenceIterator();
    }

    private static class EmptyDateRecurrenceIterator extends DateRecurrenceIterator {
        public EmptyDateRecurrenceIterator() {
            super(null);
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Date next() {
            return null;
        }
    }
}
