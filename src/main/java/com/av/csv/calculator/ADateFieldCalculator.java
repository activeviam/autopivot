/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
