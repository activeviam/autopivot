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
package com.av.agg.impl;

import com.qfs.agg.IAggregation;
import com.qfs.agg.IAggregationBinding;
import com.qfs.agg.IAggregationFunction;
import com.qfs.agg.impl.AAggregation;
import com.qfs.agg.impl.AAggregationBinding;
import com.qfs.agg.impl.AAggregationFunction;
import com.qfs.chunk.IAllocationSettings;
import com.qfs.chunk.IArrayReader;
import com.qfs.chunk.IArrayWriter;
import com.qfs.chunk.IChunk;
import com.qfs.chunk.IChunkAllocator;
import com.qfs.chunk.IWritableArray;
import com.qfs.chunk.impl.Chunks;
import com.qfs.store.Types;
import com.quartetfs.fwk.QuartetPluginValue;

/**
 *
 * Append only MIN aggregation function.
 * <p>
 * This aggregation function does not support disaggregation so
 * it will not work with aggregate providers that maintain aggregates
 * such as the bitmap aggregate provider, when those aggregates
 * are removed or updated.
 * <p>
 * It will always work with JustInTime aggregate providers
 * that compute aggregate from scratch at each query.
 *
 * @author ActiveViam
 *
 */
@QuartetPluginValue(intf = IAggregationFunction.class)
public class MinAppendOnlyFunction extends AAggregationFunction {

	/** serialVersionUID */
	private static final long serialVersionUID = 2557675987778312967L;

	/** Plugin value key */
	public static final String KEY = "min";

	public MinAppendOnlyFunction() { super(KEY); }

	@Override
	public Object key() { return KEY; }

	@Override
	public IChunk<?> createAggregateChunk(int inputDataType, int chunkSize, boolean isTransient, IAllocationSettings allocationSettings) {
		final IChunkAllocator allocator = Chunks.allocator(isTransient);
		if (Types.isDictionary(inputDataType)) {
			throw unsupportedInputDataType(inputDataType);
		} else if (Types.isArray(inputDataType)) {
			throw unsupportedInputDataType(inputDataType);
		} else if (Types.isPrimitive(inputDataType)) {
			boolean nullable = Types.isNullable(inputDataType);
			switch (Types.getContentType(inputDataType)) {
			case Types.CONTENT_DOUBLE: return allocator.allocateChunkDouble(chunkSize, nullable);
			case Types.CONTENT_FLOAT: return allocator.allocateChunkFloat(chunkSize, nullable);
			case Types.CONTENT_LONG: return allocator.allocateChunkLong(chunkSize, nullable);
			case Types.CONTENT_INT: return allocator.allocateChunkInteger(chunkSize, nullable);
			default: throw unsupportedInputDataType(inputDataType);
			}
		} else {
			// When not specified we expect scalar double values
			return allocator.allocateChunkDouble(chunkSize, true);
		}
	}

	@Override
	public IAggregation createAggregation(final String id, final int inputDataType) {
		if (Types.isDictionary(inputDataType)) {
			throw unsupportedInputDataType(inputDataType);
		} else if (Types.isArray(inputDataType)) {
			throw unsupportedInputDataType(inputDataType);
		} else if (Types.isPrimitive(inputDataType)) {
			if (Types.isNullable(inputDataType)) {
				switch (Types.getContentType(inputDataType)) {
				case Types.CONTENT_DOUBLE: return new MinAggregationDoubleNullable(id, this, inputDataType);
				case Types.CONTENT_FLOAT: return new MinAggregationFloatNullable(id, this, inputDataType);
				case Types.CONTENT_LONG: return new MinAggregationLongNullable(id, this, inputDataType);
				case Types.CONTENT_INT: return new MinAggregationIntNullable(id, this, inputDataType);
				default: throw unsupportedInputDataType(inputDataType);
				}
			} else {
				switch (Types.getContentType(inputDataType)) {
				case Types.CONTENT_DOUBLE: return new MinAggregationDouble(id, this, inputDataType);
				case Types.CONTENT_FLOAT: return new MinAggregationFloat(id, this, inputDataType);
				case Types.CONTENT_LONG: return new MinAggregationLong(id, this, inputDataType);
				case Types.CONTENT_INT: return new MinAggregationInt(id, this, inputDataType);
				default: throw unsupportedInputDataType(inputDataType);
				}
			}
		} else {
			// When not specified we expect scalar double values
			return new MinAggregationDoubleNullable(id, this, inputDataType);
		}
	}

	/** Min aggregation for the 'long' data type */
	protected static class MinAggregationLong extends AAggregation<MinAppendOnlyFunction> {

		/**
		 * Constructor
		 *
		 * @param id A String that identifies this operation
		 * @param aggFun The {@link MinAppendOnlyFunction} that created this aggregation
		 * @param dataType The data {@link Types type} of the data elements that will
		 *        be aggregated
		 */
		public MinAggregationLong(final String id, final MinAppendOnlyFunction aggFun, final int dataType) {
			super(id, aggFun, dataType);
		}

		@Override
		public IAggregationBinding bindSource(IArrayReader source, IArrayWriter destination) {
			return new MinBindingLong(source, (IWritableArray) destination);
		}

		@Override
		public IAggregationBinding bindAggregates(IArrayReader source, IArrayWriter destination) {
			return new MinBindingLong(source, (IWritableArray) destination);
		}

	}

	/** Min aggregation for the 'int' data type */
	protected static class MinAggregationInt extends AAggregation<MinAppendOnlyFunction> {

		/**
		 * Constructor
		 *
		 * @param id A String that identifies this operation
		 * @param aggFun The {@link MinAppendOnlyFunction} that created this aggregation
		 * @param dataType The data {@link Types type} of the data elements that will
		 *        be aggregated
		 */
		public MinAggregationInt(final String id, final MinAppendOnlyFunction aggFun, final int dataType) {
			super(id, aggFun, dataType);
		}

		@Override
		public IAggregationBinding bindSource(IArrayReader source, IArrayWriter destination) {
			return new MinBindingInt(source, (IWritableArray) destination);
		}

		@Override
		public IAggregationBinding bindAggregates(IArrayReader source, IArrayWriter destination) {
			return new MinBindingInt(source, (IWritableArray) destination);
		}

	}

	/** Min aggregation for the nullable 'long' data type */
	protected static class MinAggregationLongNullable extends AAggregation<MinAppendOnlyFunction> {

		/**
		 * Constructor
		 *
		 * @param id A String that identifies this operation
		 * @param aggFun The {@link MinAppendOnlyFunction} that created this aggregation
		 * @param dataType The data {@link Types type} of the data elements that will
		 *        be aggregated
		 */
		public MinAggregationLongNullable(final String id, final MinAppendOnlyFunction aggFun, final int dataType) {
			super(id, aggFun, dataType);
		}

		@Override
		public IAggregationBinding bindSource(IArrayReader source, IArrayWriter destination) {
			return new MinBindingLongNullable(source, (IWritableArray) destination);
		}

		@Override
		public IAggregationBinding bindAggregates(IArrayReader source, IArrayWriter destination) {
			return new MinBindingLongNullable(source, (IWritableArray) destination);
		}

	}

	/** Min aggregation for the nullable 'int' data type */
	protected static class MinAggregationIntNullable extends AAggregation<MinAppendOnlyFunction> {

		/**
		 * Constructor
		 *
		 * @param id A String that identifies this operation
		 * @param aggFun The {@link MinAppendOnlyFunction} that created this aggregation
		 * @param dataType The data {@link Types type} of the data elements that will
		 *        be aggregated
		 */
		public MinAggregationIntNullable(final String id, final MinAppendOnlyFunction aggFun, final int dataType) {
			super(id, aggFun, dataType);
		}

		@Override
		public IAggregationBinding bindSource(IArrayReader source, IArrayWriter destination) {
			return new MinBindingIntNullable(source, (IWritableArray) destination);
		}

		@Override
		public IAggregationBinding bindAggregates(IArrayReader source, IArrayWriter destination) {
			return new MinBindingIntNullable(source, (IWritableArray) destination);
		}

	}

	/** Min aggregation for the 'double' data type */
	protected static class MinAggregationDouble extends AAggregation<MinAppendOnlyFunction> {

		/**
		 * Constructor
		 *
		 * @param id A String that identifies this operation
		 * @param aggFun The {@link MinAppendOnlyFunction} that created this aggregation
		 * @param dataType The data {@link Types type} of the data elements that will
		 *        be aggregated
		 */
		public MinAggregationDouble(final String id, final MinAppendOnlyFunction aggFun, final int dataType) {
			super(id, aggFun, dataType);
		}

		@Override
		public IAggregationBinding bindSource(IArrayReader source, IArrayWriter destination) {
			return new MinBindingDouble(source, (IWritableArray) destination);
		}

		@Override
		public IAggregationBinding bindAggregates(IArrayReader source, IArrayWriter destination) {
			return new MinBindingDouble(source, (IWritableArray) destination);
		}

	}

	/** Min aggregation for the 'float' data type */
	protected static class MinAggregationFloat extends AAggregation<MinAppendOnlyFunction> {

		/**
		 * Constructor
		 *
		 * @param id A String that identifies this operation
		 * @param aggFun The {@link MinAppendOnlyFunction} that created this aggregation
		 * @param dataType The data {@link Types type} of the data elements that will
		 *        be aggregated
		 */
		public MinAggregationFloat(final String id, final MinAppendOnlyFunction aggFun, final int dataType) {
			super(id, aggFun, dataType);
		}

		@Override
		public IAggregationBinding bindSource(IArrayReader source, IArrayWriter destination) {
			return new MinBindingFloat(source, (IWritableArray) destination);
		}

		@Override
		public IAggregationBinding bindAggregates(IArrayReader source, IArrayWriter destination) {
			return new MinBindingFloat(source, (IWritableArray) destination);
		}

	}

	/** Min aggregation for the nullable 'double' data type */
	protected static class MinAggregationDoubleNullable extends AAggregation<MinAppendOnlyFunction> {

		/**
		 * Constructor
		 *
		 * @param id A String that identifies this operation
		 * @param aggFun The {@link MinAppendOnlyFunction} that created this aggregation
		 * @param dataType The data {@link Types type} of the data elements that will
		 *        be aggregated
		 */
		public MinAggregationDoubleNullable(final String id, final MinAppendOnlyFunction aggFun, final int dataType) {
			super(id, aggFun, dataType);
		}

		@Override
		public IAggregationBinding bindSource(IArrayReader source, IArrayWriter destination) {
			return new MinBindingDoubleNullable(source, (IWritableArray) destination);
		}

		@Override
		public IAggregationBinding bindAggregates(IArrayReader source, IArrayWriter destination) {
			return new MinBindingDoubleNullable(source, (IWritableArray) destination);
		}

	}

	/** Min aggregation for the nullable 'float' data type */
	protected static class MinAggregationFloatNullable extends AAggregation<MinAppendOnlyFunction> {

		/**
		 * Constructor
		 *
		 * @param id A String that identifies this operation
		 * @param aggFun The {@link MinAppendOnlyFunction} that created this aggregation
		 * @param dataType The data {@link Types type} of the data elements that will
		 *        be aggregated
		 */
		public MinAggregationFloatNullable(final String id, final MinAppendOnlyFunction aggFun, final int dataType) {
			super(id, aggFun, dataType);
		}

		@Override
		public IAggregationBinding bindSource(IArrayReader source, IArrayWriter destination) {
			return new MinBindingFloatNullable(source, (IWritableArray) destination);
		}

		@Override
		public IAggregationBinding bindAggregates(IArrayReader source, IArrayWriter destination) {
			return new MinBindingFloatNullable(source, (IWritableArray) destination);
		}

	}

	// BINDINGS

	
	/** Abstract Min binding */
	protected abstract static class AMinBinding extends AAggregationBinding {
	
		/** Input data */
		protected final IArrayReader input;

		/** Output aggregates */
		protected final IWritableArray output;

		protected AMinBinding(IArrayReader input, IWritableArray output) {
			this.input = input;
			this.output = output;
		}
		
		@Override
		public void disaggregate(int from, int to) { 
			throw new UnsupportedOperationException(KEY + " aggregation function is append-only, it does not support removals and disaggregation.");
		}
		
	}
	
	/** Min binding for the 'long' data type */
	public static class MinBindingLong extends AMinBinding {

		protected MinBindingLong(IArrayReader input, IWritableArray output) {
			super(input, output);
		}

		@Override
		public void copy(int from, int to) {
			output.writeLong(to, input.readLong(from));
		}

		@Override
		public void aggregate(int from, int to) {
			long out = output.readLong(to);
			long in = input.readLong(from);
			if(in < out) {
				output.writeLong(to, in);
			}
		}

	}

	/** Min binding for the 'int' data type */
	public static class MinBindingInt extends AMinBinding {

		protected MinBindingInt(IArrayReader input, IWritableArray output) {
			super(input, output);
		}

		@Override
		public void copy(int from, int to) {
			output.writeInt(to, input.readInt(from));
		}

		@Override
		public void aggregate(int from, int to) {
			int out = output.readInt(to);
			int in = input.readInt(from);
			if(in < out) {
				output.writeInt(to, in);
			}
		}

	}

	/** Min binding for the nullable 'long' data type */
	public static class MinBindingLongNullable extends AMinBinding {

		protected MinBindingLongNullable(IArrayReader input, IWritableArray output) {
			super(input, output);
		}

		@Override
		public void copy(int from, int to) {
			if (input.isNull(from)) {
				output.write(to, null);
			} else {
				output.writeLong(to, input.readLong(from));
			}
		}

		@Override
		public void aggregate(int from, int to) {
			if (!input.isNull(from)) {
				long in = input.readLong(from);
				if(output.isNull(to)) {
					output.writeLong(to, in);
				} else {
					long out = output.readLong(to);
					if(in < out) {
						output.writeLong(to, in);
					}
				}
			}
		}

	}

	/** Min binding for the nullable 'int' data type */
	public static class MinBindingIntNullable extends AMinBinding {

		protected MinBindingIntNullable(IArrayReader input, IWritableArray output) {
			super(input, output);
		}

		@Override
		public void copy(int from, int to) {
			if (input.isNull(from)) {
				output.write(to, null);
			} else {
				output.writeInt(to, input.readInt(from));
			}
		}

		@Override
		public void aggregate(int from, int to) {
			if (!input.isNull(from)) {
				int in = input.readInt(from);
				if(output.isNull(to)) {
					output.writeInt(to, in);
				} else {
					long out = output.readInt(to);
					if(in < out) {
						output.writeInt(to, in);
					}
				}
			}
		}

	}

	/** Min binding for the 'double' data type */
	public static class MinBindingDouble extends AMinBinding {

		protected MinBindingDouble(IArrayReader input, IWritableArray output) {
			super(input, output);
		}

		@Override
		public void copy(int from, int to) {
			output.writeDouble(to, input.readDouble(from));
		}

		@Override
		public void aggregate(int from, int to) {
			double out = output.readDouble(to);
			double in = input.readDouble(from);
			if(in < out) {
				output.writeDouble(to, in);
			}
		}

	}

	/** Min binding for the 'float' data type */
	public static class MinBindingFloat extends AMinBinding {

		protected MinBindingFloat(IArrayReader input, IWritableArray output) {
			super(input, output);
		}

		@Override
		public void copy(int from, int to) {
			output.writeFloat(to, input.readFloat(from));
		}

		@Override
		public void aggregate(int from, int to) {
			float out = output.readFloat(to);
			float in = input.readFloat(from);
			if(in < out) {
				output.writeFloat(to, in);
			}
		}

	}

	/** Min binding for the nullable 'double' data type */
	public static class MinBindingDoubleNullable extends AMinBinding {

		protected MinBindingDoubleNullable(IArrayReader input, IWritableArray output) {
			super(input, output);
		}

		@Override
		public void copy(int from, int to) {
			if (input.isNull(from)) {
				output.write(to, null);
			} else {
				output.writeDouble(to, input.readDouble(from));
			}
		}

		@Override
		public void aggregate(int from, int to) {
			if (!input.isNull(from)) {
				double in = input.readDouble(from);
				if(output.isNull(to)) {
					output.writeDouble(to, in);
				} else {
					double out = output.readDouble(to);
					if(in < out) {
						output.writeDouble(to, in);
					}
				}
			}
		}

	}

	/** Min binding for the nullable 'float' data type */
	public static class MinBindingFloatNullable extends AMinBinding {

		protected MinBindingFloatNullable(IArrayReader input, IWritableArray output) {
			super(input, output);
		}

		@Override
		public void copy(int from, int to) {
			if (input.isNull(from)) {
				output.write(to, null);
			} else {
				output.writeFloat(to, input.readFloat(from));
			}
		}

		@Override
		public void aggregate(int from, int to) {
			if (!input.isNull(from)) {
				float in = input.readFloat(from);
				if(output.isNull(to)) {
					output.writeFloat(to, in);
				} else {
					float out = output.readFloat(to);
					if(in < out) {
						output.writeFloat(to, in);
					}
				}
			}
		}

	}
}