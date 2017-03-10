/*
 * (C) ActiveViam 2017
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.av.csv.calculator;

import java.util.Calendar;
import java.util.Date;

import com.qfs.msg.csv.ILineReader;
import com.qfs.msg.csv.translator.impl.AColumnCalculator;

/**
 * 
 * Extract a field from a date.
 * 
 * @author ActiveViam
 *
 */
public abstract class ADateFieldCalculator extends AColumnCalculator<ILineReader> {

	/** Name of the reference date column */
	protected final String baseColumnName;
	
	public ADateFieldCalculator(String baseColumnName, String columnName) {
		super(columnName);
		this.baseColumnName = baseColumnName;
	}

	@Override
	public Object compute(IColumnCalculationContext<ILineReader> context) {
		Date date = (Date) context.getValue(this.baseColumnName);
		if(date == null) return null;
		Calendar cal =  Calendar.getInstance();
		cal.setTime(date);
		return compute(cal);
	}
	
	protected abstract Object compute(Calendar calendar);

}
