/*
 * (C) ActiveViam 2017
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.av.csv.calculator;

import java.text.DateFormatSymbols;
import java.util.Calendar;

/**
 * 
 * Extract month from a date.
 * 
 * @author ActiveViam
 *
 */
public class DateMonthCalculator extends ADateFieldCalculator {

	/** Month names */
	protected final String[] months;
	
	public DateMonthCalculator(String baseColumnName, String columnName) {
		super(baseColumnName, columnName);
	    DateFormatSymbols dfs = new DateFormatSymbols();
	    this.months = dfs.getMonths();
	}

	@Override
	protected Object compute(Calendar calendar) {
		return this.months[calendar.get(Calendar.MONTH)];
	}
	
}
