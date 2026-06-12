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
	private Calendar timeframe;
	
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
	 * @param timeframe The month of reference.
	 * @param value The result value in the given month.
	 */
	public PeriodValue(Calendar timeframe, double value) {
		this.timeframe = timeframe;
		this.value = value;
		this.hasValue = true;
	}

	public PeriodValue(Calendar timeframe) {
		this.timeframe = timeframe;
		this.value = 0;
		this.hasValue = false;
	}
	

	/**
	 * @return The reference month. 
	 */
	public Calendar getTimeframe() {
		return timeframe;
	}


	/**
	 * @return The reference month in time milliseconds.
	 */
	public long getTimeframeTimeInMillis() {
		return timeframe.getTimeInMillis();
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
