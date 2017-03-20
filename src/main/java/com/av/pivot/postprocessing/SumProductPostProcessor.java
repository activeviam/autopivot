/*
 * (C) ActiveViam 2017
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
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
 * The base measures are calculated at the leaf levels of the post processors,
 * multiplied together, and then the products are aggregated together
 * up to the level of the query (SUM by default).
 * <p>
 * This post processor can be used directly from an MDX frontend
 * using the ActiveMeasure MDX function.
 * 
 * @author ActiveViam
 *
 */
@QuartetExtendedPluginValue(intf = IPostProcessor.class, key = SumProductPostProcessor.PLUGIN_TYPE)
public class SumProductPostProcessor extends ADynamicAggregationPostProcessor<Double> {

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
		// Should never get called as the optimised leaf evaluation is implemented
		throw new UnsupportedOperationException();
	}

}