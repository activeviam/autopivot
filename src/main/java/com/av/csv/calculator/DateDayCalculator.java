/*
 * (C) ActiveViam 2017
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.av.csv.calculator;

import java.util.Calendar;

/**
 * 
 * Extract year from a date.
 * 
 * @author ActiveViam
 *
 */
public class DateDayCalculator extends ADateFieldCalculator {

	public DateDayCalculator(String baseColumnName, String columnName) {
		super(baseColumnName, columnName);
	}

	@Override
	protected Object compute(Calendar calendar) {
		return calendar.get(Calendar.DAY_OF_MONTH);
	}

}
