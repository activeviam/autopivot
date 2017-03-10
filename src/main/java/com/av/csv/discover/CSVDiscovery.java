/*
 * (C) ActiveViam 2017
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.av.csv.discover;

import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

import com.quartetfs.fwk.QuartetRuntimeException;
import com.quartetfs.fwk.Registry;
import com.quartetfs.fwk.format.IParser;
import com.quartetfs.fwk.impl.Pair;
import com.quartetfs.fwk.types.IPlugin;

public class CSVDiscovery {

	/** Logger component */
	public static final Logger LOG = Logger.getLogger(CSVDiscovery.class.getName());
	
	/** Default list of candidate separators */
	public static final List<String> DEFAULT_SEPARATORS = Stream.of(";", "	", "\\|", ",").collect(toList());
	
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

		return Stream.of(
				plugin.valueOf("DATE[yyyy-MM-dd]"),
				plugin.valueOf("DATE[yyyy/MM/dd]"),
				plugin.valueOf("DATE[MM-dd-yyyy]"),
				plugin.valueOf("DATE[MM/dd/yyyy]"),
				plugin.valueOf("DATE[dd-MM-yyyy]"),
				plugin.valueOf("DATE[dd/MM/yyyy]"),
				plugin.valueOf("int"),
				plugin.valueOf("double"))
				.collect(toList());
	}


	public CSVDiscoveryResult discoverFile(String fileName) throws IOException {
		
		LOG.info("Detecting CSV parser configuration for file " + fileName);
		
		InputStream is = openFile(fileName);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			List<String> lines = reader.lines().limit(1000L).collect(Collectors.toList());
			if(lines.size() <= 0) {
				throw new QuartetRuntimeException("Cannot process empty file: " + fileName);
			}
			
			String separator = detectSeparator(lines);

			LOG.info("Detected separator: " + separator);
			
			// Extract column names from header
			List<String> headers = Arrays.asList(lines.get(0).split(separator));
			
			LOG.info("Column names: " + headers);
			
			// Remove header and detect column types
			List<String> content = lines.subList(1, lines.size());
			List<List<String>> columns = toColumns(content, separator);
			List<String> types = new ArrayList<>(columns.size());
			for(List<String> column : columns) {
				String type = detectType(column);
				types.add(type);
			}

			LOG.info("Detected types: " + types);
			
			return new CSVDiscoveryResult(separator, headers, types);
		}
	}

	/** Convert a list of text rows into columns of text fields */
	public static List<List<String>> toColumns(List<String> rows, String separator) {
		
		List<List<String>> columns = new ArrayList<>();
		rows.forEach(row -> {
			String[] fields = row.split(separator);
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
		.map(sep -> new Pair<>(sep, lines.stream().collect(Collectors.summarizingInt(s -> s.split(sep).length))))
		.filter(p -> {
			IntSummaryStatistics stats = p.getRight();
			return stats.getCount() > 0
					&& stats.getAverage() >= 2.0
					&& (stats.getMax() < 2*stats.getAverage());
		})
		.findFirst().orElse(new Pair<String, IntSummaryStatistics>()).getLeft();
	}
	

	/**
	 * 
	 * @param fileName
	 * @return input stream on the file
	 * @throws IOException
	 */
	public InputStream openFile(String fileName) throws IOException {
		Path path = Paths.get(fileName);
		if(Files.exists(path)) {
			// Standard file
			return Files.newInputStream(path);
		} else {
			// Lookup file in classpath
			return getClass().getClassLoader().getResourceAsStream(fileName);
		}
	}


}