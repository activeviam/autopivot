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
package com.av.csv;

import java.util.List;

/**
 * 
 * Result of the discovery of a CSV file.
 * 
 * @author ActiveViam
 *
 */
public class CSVFormat {

	/** CSV separator */
	protected final String separator;
	
	/** Column names */
	protected final List<String> columnNames;
	
	/** Column types */
	protected final List<String> columnTypes;
	
	/** Quote processing */
	protected final boolean quoteProcessing;
	
	
	public CSVFormat(String separator, List<String> columnNames, List<String> columnTypes, boolean quoteProcessing) {
		this.separator = separator;
		this.columnNames = columnNames;
		this.columnTypes = columnTypes;
		this.quoteProcessing = quoteProcessing;
	}

	public String getSeparator() { return separator; }
	
	public List<String> getColumnNames() { return columnNames; }
	
	public int getColumnCount() { return columnNames.size(); }
	
	public boolean getQuoteProcessing() { return quoteProcessing; }
	
	public String getColumnName(int columnIndex) {
		return columnNames.get(columnIndex);
	}
	
	public String getColumnType(int columnIndex) {
		return columnTypes.get(columnIndex);
	}
	
}
