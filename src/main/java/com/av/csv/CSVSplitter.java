/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.av.csv;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 *
 * Logic to split the fields of a CSV text row.
 *
 * @author ActiveViam
 *
 */
public class CSVSplitter {

	/** Logger */
	private static final Logger LOGGER = Logger.getLogger(CSVSplitter.class.getName());

	public static String[] split(String text, String separator) {

		if(separator == null || separator.length() != 1) {
			throw new IllegalArgumentException("Cannot split text, unsupported separator: " + separator);
		}
		final char sep = separator.charAt(0);
		final char dq = '"';

		int fieldCount = 1;

		boolean withinQuotes = false;

		// First pass, count fields
		for(int c = 0; c < text.length(); c++) {
			final char current = text.charAt(c);
			if(dq == current) {
				// double quote detected, is this the beginning of a field?
				if(c == 0 || sep == text.charAt(c-1)) {
					// Beginning of a new field, delimited by quotes
					withinQuotes = true;
				}
				// or else is it the end of a field?
				else if((c == text.length()-1) || (sep == text.charAt(c+1))) {
					if(!withinQuotes) {
						LOGGER.warning("Unexpected double quote character at the end of a field: " + text);
					}
					withinQuotes = false;
				}
			} else if(sep == current) {
				if(withinQuotes) {
					// In the middle of a field, not a split
				} else {
					fieldCount++;
				}
			}
		}

		// Second pass, extract fields
		final String[] fields = new String[fieldCount];
		fieldCount = 0;
		int fieldStart = 0;
		for(int c = 0; c < text.length(); c++) {
			final char current = text.charAt(c);
			if(dq == current) {
				// double quote detected, is this the beginning of a field?
				if(c == 0 || sep == text.charAt(c-1)) {
					// Beginning of a new field, delimited by quotes
					withinQuotes = true;
				}
				// or else is it the end of a field?
				else if((c == text.length()-1) || (sep == text.charAt(c+1))) {
					withinQuotes = false;
				}

				if(c == text.length()-1) {
					// End of row
					fields[fieldCount] = text.substring(fieldStart+1, c);
				}
			} else if(sep == current) {
				if(withinQuotes) {
					// In the middle of a field, not a split
				} else {
					String field;
					if(dq == text.charAt(fieldStart)) {
						field = text.substring(fieldStart+1, c-1);
					} else {
						field = text.substring(fieldStart, c);
					}
					fields[fieldCount] = field;
					fieldCount++;
					fieldStart = c+1;
				}

				if(c == text.length()-1) {
					// End of row, the last column is empty
					fields[fieldCount] = "";
				}
			} else if(c == text.length()-1) {
				// End of row, field without double quotes
				fields[fieldCount] = text.substring(fieldStart, text.length());
			}
		}

		return fields;
	}


	public static void main(String[] params) {

		final String text = "\"2\",\"Allison, Miss Helen \"Loraine\"\",\"1st\",2,\"female\",0,1";

		System.out.println(text);

		System.out.println(Arrays.asList(split(text, ",")));
	}

}
