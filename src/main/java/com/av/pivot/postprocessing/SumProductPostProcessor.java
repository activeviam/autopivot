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
package com.av.pivot.postprocessing;

import java.util.Arrays;
import java.util.Properties;

import com.qfs.store.record.IRecordReader;
import com.quartetfs.biz.pivot.ILocation;
import com.quartetfs.biz.pivot.cube.hierarchy.measures.IPostProcessorCreationContext;
import com.quartetfs.biz.pivot.postprocessing.IPostProcessor;
import com.quartetfs.biz.pivot.postprocessing.PostProcessorInitializationException;
import com.quartetfs.biz.pivot.postprocessing.impl.ADynamicAggregationPostProcessor;
import com.quartetfs.fwk.QuartetException;
import com.quartetfs.fwk.QuartetExtendedPluginValue;

/**
 * 
 * Dynamically perform the sum product of two underlying measures.
 * The base measures are taken at the leaf levels defined in the post processor,
 * multiplied together, and then the products are aggregated together
 * up to the level of the query (SUM aggregation by default).
 * <p>
 * This post processor can be used directly from an MDX frontend
 * using the ActiveMeasure MDX function.
 * 
 * @author ActiveViam
 *
 */
@QuartetExtendedPluginValue(intf = IPostProcessor.class, key = SumProductPostProcessor.PLUGIN_TYPE)
public class SumProductPostProcessor extends ADynamicAggregationPostProcessor<Double, Double> {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** Type identifying this post processor */
	public static final String PLUGIN_TYPE = "SUMPRODUCT";
	
	/** Constructor */
	public SumProductPostProcessor(String name, IPostProcessorCreationContext creationContext) {
		super(name, creationContext);
	}
	
	@Override
	public String getType() { return PLUGIN_TYPE; }
	
	@Override
	public void init(Properties properties) throws QuartetException {
		super.init(properties);
		if(this.underlyingMeasures == null || this.underlyingMeasures.length != 2) {
			throw new PostProcessorInitializationException("Expecting exactly two underlying measures, got " + Arrays.toString(this.underlyingMeasures));
		}
	}
	

	/**
	 * @return product of the two underlying measures
	 */
	@Override
	protected Double evaluateLeaf(
			final ILocation leafLocation,
			final IRecordReader underlyingValues,
			final Object[] underlyingMeasuresBuffer)
	{
		return underlyingValues.readDouble(0) * underlyingValues.readDouble(1);
	}
	
	@Override
	protected Double evaluateLeaf(ILocation leafLocation, Object[] underlyingMeasures) {
		// Should never get called as the optimized leaf evaluation is implemented (above)
		throw new UnsupportedOperationException();
	}

}