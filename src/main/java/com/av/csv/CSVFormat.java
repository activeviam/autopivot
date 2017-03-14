/*
 * (C) ActiveViam 2017
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
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
	
	
	public CSVFormat(String separator, List<String> columnNames, List<String> columnTypes) {
		this.separator = separator;
		this.columnNames = columnNames;
		this.columnTypes = columnTypes;
	}

	public String getSeparator() { return separator; }
	
	public List<String> getColumnNames() { return columnNames; }
	
	public int getColumnCount() { return columnNames.size(); }
	
	public String getColumnName(int columnIndex) {
		return columnNames.get(columnIndex);
	}
	
	public String getColumnType(int columnIndex) {
		return columnTypes.get(columnIndex);
	}
	
}
