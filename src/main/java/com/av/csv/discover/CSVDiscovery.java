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
package com.av.csv.discover;

import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.av.csv.CSVFormat;
import com.av.csv.CSVSplitter;
import com.quartetfs.fwk.QuartetRuntimeException;
import com.quartetfs.fwk.Registry;
import com.quartetfs.fwk.format.IParser;
import com.quartetfs.fwk.impl.Pair;
import com.quartetfs.fwk.types.IPlugin;

/**
 * 
 * Utilities to discover the structure and format 
 * of a CSV file.
 * 
 * @author ActiveViam
 *
 */
public class CSVDiscovery {

	/** Logger component */
	public static final Logger LOG = Logger.getLogger(CSVDiscovery.class.getName());
	
	/** Default list of candidate separators */
	public static final List<String> DEFAULT_SEPARATORS = Stream.of(";", "	", "|", ",").collect(toList());
	
	/** Number of rows sampled when detecting the csv separator */
	public static final int SEPARATOR_DETECTION_SAMPLE = 100;
	
	/** Number of rows sampled when detecting column types */
	public static final int DATATYPE_DETECTION_SAMPLE = 100;
	
	/** Candidate separators */
	protected final List<String> separators;

	/** Candidate parsers */
	protected final List<IParser<?>> parsers;
	
	/**
	 * Constructor with default options
	 */
	public CSVDiscovery() {
		this(DEFAULT_SEPARATORS, createDefaultParsers());
	}
	

	/**
	 * Constructor
	 *
	 * @param separators candidate separators
	 * @param parsers cabdidate parsers
	 */
	public CSVDiscovery(List<String> separators, List<IParser<?>> parsers) {
		this.separators = separators;
		this.parsers = parsers;
	}

	/** @return the default list of parsers */
	public static List<IParser<?>> createDefaultParsers() {

		@SuppressWarnings("rawtypes")
		IPlugin<IParser> plugin = Registry.getPlugin(IParser.class);
		
		return Stream.<IParser<?>>of(
				plugin.valueOf("int"),
				plugin.valueOf("long"),
				plugin.valueOf("double"),
				plugin.valueOf("LOCALDATE[yyyy-MM-dd]"),
				plugin.valueOf("LOCALDATE[yyyy/MM/dd]"),
				plugin.valueOf("LOCALDATE[MM-dd-yyyy]"),
				plugin.valueOf("LOCALDATE[MM/dd/yyyy]"),
				plugin.valueOf("LOCALDATE[dd-MM-yyyy]"),
				plugin.valueOf("LOCALDATE[dd/MM/yyyy]"),
				plugin.valueOf("LOCALDATE[d-MMM-yyyy]"),
				plugin.valueOf("ZONEDDATETIME[EEE MMM dd HH:mm:ss zzz yyyy]"))
				.collect(toList());
	}

	/**
	 * 
	 * Determine if a field type is of date type.
	 * 
	 * @param fieldType
	 * @return boolean
	 */
	public static boolean isDate(String fieldType) {
		return fieldType.startsWith("DATE") ||
			fieldType.startsWith("LocalDate") ||
			fieldType.startsWith("ZonedDate");
	}
	
	/**
	 * 
	 * Discover the CSV format of a CSV file.
	 * 
	 * @param fileName
	 * @return CSV Format
	 * @throws IOException
	 */
	public CSVFormat discoverFile(String fileName) throws IOException {
		return discoverFile(fileName, Charset.defaultCharset());
	}
	
	/**
	 * 
	 * Discover the CSV format of a CSV file.
	 * 
	 * @param fileName
	 * @param charset
	 * @return CSV Format
	 * @throws IOException
	 */
	public CSVFormat discoverFile(String fileName, Charset charset) throws IOException {
		
		LOG.info("Detecting CSV parser configuration for file " + fileName);
		
		InputStream is = openFile(fileName);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			List<String> lines = reader.lines().limit(1000L).collect(Collectors.toList());
			if(lines.size() <= 0) {
				throw new QuartetRuntimeException("Cannot process empty file: " + fileName);
			}
			
			String separator = detectSeparator(lines);

			LOG.info("Detected separator: " + separator);
			
			// Extract column names from header, replace spaces and the '/' reserved character if needed
			List<String> headers = Arrays.asList(CSVSplitter.split(lines.get(0), separator));
			headers = headers.stream().map(s -> s.replaceAll(" ", "_")).collect(Collectors.toList());
			headers = headers.stream().map(s -> s.replaceAll("/", "_")).collect(Collectors.toList());
			
			LOG.info("Column names: " + headers);
			
			// Remove header and detect column types
			List<String> content = lines.subList(1, lines.size());
			List<List<String>> columns = toColumns(content, separator);
			List<String> types = new ArrayList<>(columns.size());
			boolean quoteProcessing = false;
			for(List<String> column : columns) {
				String type = detectType(column);
				types.add(type);
				boolean qp = detectQuoteProcessing(column);
				quoteProcessing = quoteProcessing || qp;
			}

			LOG.info("Detected types: " + types);
			
			return new CSVFormat(separator, headers, types, quoteProcessing);
		}
	}

	/** Convert a list of text rows into columns of text fields */
	public static List<List<String>> toColumns(List<String> rows, String separator) {
		
		List<List<String>> columns = new ArrayList<>();
		rows.forEach(row -> {
			String[] fields = CSVSplitter.split(row, separator);
			for(int f = 0; f < fields.length; f++) {
				if(columns.size() <= f) {
					columns.add(new ArrayList<>());
				}
				columns.get(f).add(fields[f]);
			}
		});
	
		return columns;

	}
	
	
	/**
	 * Brute force detection of the type of a field,
	 * based on several samples of the field text representation.
	 * 
	 * @param fields
	 * @return type of the field (string by default)
	 */
	public String detectType(List<String> fields) {
		
		boolean emptyColumn = true;
		for(String field : fields) {
			if(field != null && field.length() > 0) {
				emptyColumn = false;
				break;
			}
		}

		if(!emptyColumn) {
			for(IParser<?> parser : this.parsers) {
				try {
					for(String field : fields) {
						parser.parse(field);
					}
					return parser.key().toString();
				} catch(Exception e) {
					// Wrong parser, continue with the next one
				}
			}
		}
		
		// Default
		return "String";
	}
	
	/**
	 * Use several text samples of a field to detect whether quote
	 * processing should be enabled.
	 * <br>
	 * Quote processing must be enabled if at least one sample
	 * appears to be surrounded by quotes.
	 * 
	 * @param fields
	 * @return true if quote processing is necessary
	 */
	public boolean detectQuoteProcessing(List<String> fields) {
		for(String field : fields) {
			if(field != null && field.startsWith("\"") && field.endsWith("\"")) {
				return true;
			}
		}
		return false;
	}



	/**
	 * 
	 * Detect the csv separator character of a file
	 * 
	 * @param fileName
	 * @return separator character or null if the detection has failed
	 * @throws IOException
	 */
	public String detectSeparator(String fileName) throws IOException {
		InputStream is = openFile(fileName);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			List<String> lines = reader.lines().limit(100L).collect(Collectors.toList());
			return detectSeparator(lines);
		}
	}


	/**
	 * 
	 * Detect the csv separator character from a sample of lines.
	 * The algorithm tries a series of candidate separators
	 * and computes statistics when the separator is used.
	 * 
	 * @param lines
	 * @return separator String, or null if the detection algorithm did not succeed
	 */
	public String detectSeparator(List<String> lines) {
		return separators.stream()
		.map(sep -> new Pair<>(sep, lines.stream().collect(Collectors.summarizingInt(s -> CSVSplitter.split(s, sep).length))))
		.filter(p -> {
			IntSummaryStatistics stats = p.getRight();
			return stats.getCount() > 0
					&& stats.getAverage() >= 2.0
					&& (stats.getMax() < 2*stats.getAverage());
		}).sorted((p1, p2) -> p2.getRight().getMax() - p1.getRight().getMax())
		.findFirst().orElse(new Pair<String, IntSummaryStatistics>()).getLeft();
	}
	

	/**
	 * 
	 * Open a file input stream, works if the file is in the
	 * classpath or in the file system.
	 * 
	 * @param fileName
	 * @return input stream on the file
	 * @throws IOException
	 */
	public InputStream openFile(String fileName) throws IOException {
		InputStream is;
		
		Path path = Paths.get(fileName);
		if(Files.exists(path)) {
			// Standard file
			is = Files.newInputStream(path);
		} else {
			// Lookup file in classpath
			is = getClass().getClassLoader().getResourceAsStream(fileName);
		}
		
		if(null == is) {
			throw new IOException("File not found: " + fileName);
		}
		
		return is;
	}


}