package tw.tib.financisto.model;

import java.util.Calendar;

/**
 * Data that represents a result in a month.
 * @author Rodrigo Sousa
 */
public class PeriodValue { 
	/**
	 * The reference month.
	 */
	private Calendar month;
	
	/**
	 * The result value of the corresponding month.
	 */
	private double value;

	/**
	 * If false, this data point has not been set a value
	 * used to fill chart when the value continues with last value
	 * if this duration doesn't have any transactions (i.e. used for balance chart)
	 */
	private boolean hasValue;
	
	/**
	 * Default constructor.
	 * @param month The month of reference.
	 * @param value The result value in the given month.
	 */
	public PeriodValue(Calendar month, double value) {
		this.month = month;
		this.value = value;
		this.hasValue = true;
	}

	public PeriodValue(Calendar month) {
		this.month = month;
		this.value = 0;
		this.hasValue = false;
	}
	

	/**
	 * @return The reference month. 
	 */
	public Calendar getMonth() {
		return month;
	}


	/**
	 * @return The reference month in time milliseconds.
	 */
	public long getMonthTimeInMillis() {
		return month.getTimeInMillis();
	}


	/**
	 * @return The monthly result value.
	 */
	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
		this.hasValue = true;
	}

	public boolean hasValue() {
		return hasValue;
	}

}
