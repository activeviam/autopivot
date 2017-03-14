/*
 * (C) ActiveViam 2017
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.av.csv;

import java.util.Arrays;

/**
 * 
 * Logic to split the fields of a CSV text row.
 * 
 * @author ActiveViam
 *
 */
public class CSVSplitter {
	
	public static String[] split(String text, String separator) {
		
		if(separator == null || separator.length() != 1) {
			throw new IllegalArgumentException("Cannot split text, unsupported separator: " + separator);
		}
		char sep = separator.charAt(0);
		char dq = '"';
		
		int fieldCount = 1;
		
		boolean withinQuotes = false;

		// First pass, count fields
		for(int c = 0; c < text.length(); c++) {
			char current = text.charAt(c);
			if(dq == current) {
				// double quote detected, is this the beginning of a field?
				if(c == 0 || sep == text.charAt(c-1)) {
					// Beginning of a new field, delimited by quotes
					withinQuotes = true;
				}
				// or else is it the end of a field?
				else if((c == text.length()-1) || (sep == text.charAt(c+1))) {
					if(!withinQuotes) {
						throw new IllegalStateException("Unexpected double quote character at position " + c + " in " + text);
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
		String[] fields = new String[fieldCount];
		fieldCount = 0;
		int fieldStart = 0;
		for(int c = 0; c < text.length(); c++) {
			char current = text.charAt(c);
			if(dq == current) {
				// double quote detected, is this the beginning of a field?
				if(c == 0 || sep == text.charAt(c-1)) {
					// Beginning of a new field, delimited by quotes
					withinQuotes = true;
				}
				// or else is it the end of a field?
				else if((c == text.length()-1) || (sep == text.charAt(c+1))) {
					if(!withinQuotes) {
						throw new IllegalStateException("Unexpected double quote character at position " + c + " in " + text);
					}
					withinQuotes = false;
				}
				
				if(c == text.length()-1) {
					// End of row
					fields[fieldCount] = text.substring(fieldStart+1, c-1);
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
			} else if(c == text.length()-1) {
				// End of row, field without double quotes
				fields[fieldCount] = text.substring(fieldStart, text.length());
			}
		}
		
		return fields;
	}
	
	
	public static void main(String[] params) {
		
		String text = "\"2\",\"Allison, Miss Helen \"Loraine\"\",\"1st\",2,\"female\",0,1";
		System.out.println(text);
		
		System.out.println(Arrays.asList(split(text, ",")));
		
	}
	
}
