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

	/** Double quotes character */
	static final char DQ = '"';

	/**
	 * Detect if quotes are used in this CSV row.
	 * @param text
	 * @return true is at least one field was quoted
	 */
	public static boolean containsQuotedField(String text, String separator) {
		if(separator == null || separator.length() != 1) {
			throw new IllegalArgumentException("Cannot split text, unsupported separator: " + separator);
		}
		final char sep = separator.charAt(0);

		int fieldStart = 0;
		int nbQuotes = 0;

		// First pass, count fields
		for(int c = 0; c < text.length(); c++) {
			final char current = text.charAt(c);
			if (sep == current) {
				if (nbQuotes == 0) {
					// Standard field
					fieldStart = c + 1;
				} else if (DQ == text.charAt(fieldStart)) {
					// current field is quoted
					return true;
				} else {
					// quotes used in the middle of a standard field
					// dirty but we can live with it
					fieldStart = c + 1;
					nbQuotes = 0;
				}
			} else if (DQ == current) {
				nbQuotes++;
			}
		}
		return false;
	}

	/**
	 * Split a CSV row into String fields
	 * @param text
	 * @param separator
	 * @return array of fields
	 */
	public static String[] split(String text, String separator) {

		if(separator == null || separator.length() != 1) {
			throw new IllegalArgumentException("Cannot split text, unsupported separator: " + separator);
		}
		final char sep = separator.charAt(0);

		int fieldStart = 0;
		int fieldCount = 1;
		int nbQuotes = 0;

		// First pass, count fields
		for(int c = 0; c < text.length(); c++) {
			final char current = text.charAt(c);
			if(sep == current) {
				if(nbQuotes == 0) {
					// Standard field
					fieldCount++;
					fieldStart = c+1;
				} else if (DQ == text.charAt(fieldStart)) {
					// current field is quoted
					if ((DQ == text.charAt(c - 1)) && isEven(nbQuotes)) {
						// Properly quoted field
						fieldCount++;
						fieldStart = c+1;
						nbQuotes = 0;
					} else {
						// In the middle of a quoted field
					}
				} else {
					// quotes used in the middle of a standard field
					// dirty but we can live with it
					fieldCount++;
					fieldStart = c+1;
					nbQuotes = 0;
				}
			} else if(DQ == current) {
				nbQuotes++;
			}
		}

		// Second pass, extract fields
		final String[] fields = new String[fieldCount];
		int fieldIndex = 0;
		nbQuotes = 0;
		fieldStart = 0;
		for(int c = 0; c < text.length(); c++) {
			final char current = text.charAt(c);

			if(sep == current) {
				if(nbQuotes == 0) {
					// standard field
					fields[fieldIndex++] = text.substring(fieldStart, c);
					fieldStart = c+1;
				} else if(DQ == text.charAt(fieldStart)) {
					if(DQ == text.charAt(c-1) && isEven(nbQuotes)) {
						// Properly quoted field
						fields[fieldIndex++] = text.substring(fieldStart + 1, c - 1);
						fieldStart = c + 1;
						nbQuotes = 0;
					}
					// Else in the middle of a quoted field
				} else {
					// quotes used in the middle of a standard field
					// dirty but we can live with it
					fields[fieldIndex++] = text.substring(fieldStart, c);
					fieldStart = c + 1;
					nbQuotes = 0;
				}
			} else if(DQ == current) {
				nbQuotes++;
			}
		}

		// End of the row, extract the last field of the row
		if(nbQuotes == 0) {
			// standard field, possibly empty
			fields[fieldIndex++] = text.substring(fieldStart, text.length());
		} else if(DQ == text.charAt(fieldStart)) {
			if(DQ == text.charAt(text.length()-1) && isEven(nbQuotes)) {
				// Field seems properly quoted
				fields[fieldIndex++] = text.substring(fieldStart + 1, text.length() - 1);
			} else {
				// dirty quoted field that is left open
				fields[fieldIndex++] = text.substring(fieldStart + 1, text.length());
			}
		}

		return fields;
	}

	public static boolean isEven(int i) {
		return i % 2 == 0;
	}


	public static void main(String[] params) {

		final String text = "\"2\",\"Allison, Miss Helen \"Loraine\"\",\"1st\",2,\"female\",0,1";

		System.out.println(text);

		System.out.println(Arrays.asList(split(text, ",")));
	}

}
