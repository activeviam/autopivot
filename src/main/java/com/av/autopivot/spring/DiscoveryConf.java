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

import com.activeviam.fwk.ActiveViamRuntimeException;
import com.av.csv.CSVFormat;
import com.av.csv.discover.CSVDiscovery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.nio.charset.Charset;
import java.util.logging.Logger;


/**
 * Spring configuration to discover the structure and format .<br>
 *
 * @author ActiveViam
 */
@Configuration
public class DiscoveryConf {

    /**
     * Property to identify the name of the file to load
     */
    public static final String FILENAME_PROPERTY = "fileName";
    /**
     * Logger
     **/
    protected static Logger LOGGER = Logger.getLogger(DiscoveryConf.class.getName());
    /**
     * Spring environment, automatically wired
     */
    @Autowired
    protected Environment env;


    /**
     * @return charset used by the CSV parsers.
     */
    @Bean
    public Charset charset() {
        String charsetName = env.getProperty("charset");
        if (charsetName != null) {
            try {
                return Charset.forName(charsetName);
            } catch (Exception e) {
                LOGGER.warning("Unknown charset: " + charsetName);
            }
        }
        return Charset.defaultCharset();
    }


    /**
     * Discover the input data file (CSV separator, column types)
     */
    @Bean
    public CSVFormat discoverFile() {
        String fileName = env.getRequiredProperty(FILENAME_PROPERTY);
        try {
            CSVFormat discovery = new CSVDiscovery().discoverFile(fileName, charset());
            return discovery;
        } catch (Exception e) {
            throw new ActiveViamRuntimeException("Could not discover csv file: " + fileName, e);
        }
    }


}
