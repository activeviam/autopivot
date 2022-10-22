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
package com.av.autopivot.spring;

import com.av.autopivot.AutoPivotGenerator;
import com.av.csv.CSVFormat;
import com.av.csv.calculator.DateDayCalculator;
import com.av.csv.calculator.DateMonthCalculator;
import com.av.csv.calculator.DateYearCalculator;
import com.qfs.msg.IColumnCalculator;
import com.qfs.msg.csv.ICSVSource;
import com.qfs.msg.csv.IFileInfo;
import com.qfs.msg.csv.ILineReader;
import com.qfs.msg.csv.filesystem.impl.SingleFileCSVTopic;
import com.qfs.msg.csv.impl.CSVParserConfiguration;
import com.qfs.msg.csv.impl.CSVSource;
import com.qfs.msg.csv.impl.CSVSourceConfiguration;
import com.qfs.platform.IPlatform;
import com.qfs.server.cfg.IDatastoreConfig;
import com.qfs.source.impl.CSVMessageChannelFactory;
import com.qfs.source.impl.Fetch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static com.av.csv.discover.CSVDiscovery.isDate;


/**
 *
 * Spring configuration of the Sandbox ActivePivot server.<br>
 * The parameters of the Sandbox ActivePivot server can be quickly changed by modifying the
 * pojo.properties file.
 *
 * @author ActiveViam
 *
 */
@Configuration
public class SourceConfig {

	/** Logger **/
	protected static Logger LOGGER = Logger.getLogger(SourceConfig.class.getName());

	/** Property to identify the name of the file to load */
	public static final String FILENAME_PROPERTY = "fileName";


	/** Spring environment, automatically wired */
	@Autowired
	protected Environment env;

	/** Application datastore, automatically wired. */
	@Autowired
	protected IDatastoreConfig datastoreConfig;

	@Autowired
	protected DiscoveryConf discoveryConf;

	/** Create and configure the CSV engine */
	@Bean
	public ICSVSource<Path> CSVSource() throws IOException {
		
		// Allocate half the the machine cores to CSV parsing
		Integer parserThreads = Math.min(8, Math.max(1, IPlatform.CURRENT_PLATFORM.getProcessorCount() / 2));
		LOGGER.info(()->"Allocating " + parserThreads + " parser threads.");
		
		CSVSource<Path> source = new CSVSource<Path>();
		CSVSourceConfiguration.CSVSourceConfigurationBuilder<Path> sourceConfigurationBuilder = new CSVSourceConfiguration.CSVSourceConfigurationBuilder<>();
		sourceConfigurationBuilder.parserThreads(parserThreads);
		final String bufferSize = env.getProperty("csvSource.bufferSize","256");
		sourceConfigurationBuilder.bufferSize(Integer.parseInt(bufferSize));
		source.configure(sourceConfigurationBuilder.build());
		
		return source;
	}
	




	/**
	 * Load the CSV file
	 */
	@Bean
	@DependsOn(value = "startManager")
	public Void loadData(ICSVSource<Path> source) throws Exception {
		
		CSVFormat discovery = discoveryConf.discoverFile();
		
		// Create parser configuration
		CSVParserConfiguration configuration = new CSVParserConfiguration(
				discoveryConf.charset(),
				discovery.getSeparator().charAt(0),
				discovery.getColumnCount(),
				true,
				true,
				1,
				CSVParserConfiguration.toMap(discovery.getColumnNames()));
		configuration.setProcessQuotes(discovery.getQuoteProcessing());
		
		String fileName = env.getRequiredProperty("fileName");
		SingleFileCSVTopic topic = new SingleFileCSVTopic(AutoPivotGenerator.BASE_STORE, configuration, fileName, 1000);
		source.addTopic(topic);
		
		CSVMessageChannelFactory<Path> channelFactory = new CSVMessageChannelFactory<>(source, datastoreConfig.database());
		
		// Derive calculated columns
		List<IColumnCalculator<ILineReader>> calculatedColumns = new ArrayList<IColumnCalculator<ILineReader>>();
		for(int c = 0; c < discovery.getColumnCount(); c++) {
			String columnName = discovery.getColumnName(c);
			String columnType = discovery.getColumnType(c);
			
			// When a date field is detected, we automatically
			// calculate the YEAR, MONTH and DAY fields.
			if(isDate(columnType)) {
				calculatedColumns.add(new DateYearCalculator(columnName, columnName + ".YEAR"));
				calculatedColumns.add(new DateMonthCalculator(columnName, columnName + ".MONTH"));
				calculatedColumns.add(new DateDayCalculator(columnName, columnName + ".DAY"));
			}
			
		};
		channelFactory.setCalculatedColumns(AutoPivotGenerator.BASE_STORE, calculatedColumns);
		
		
		Fetch<IFileInfo<Path>, ILineReader> fetch = new Fetch<IFileInfo<Path>, ILineReader>(channelFactory);
		fetch.fetch(source);
		
		LOGGER.info("AutoPivot initial loading complete.");
		
		return null; // Void
	}
	
}
