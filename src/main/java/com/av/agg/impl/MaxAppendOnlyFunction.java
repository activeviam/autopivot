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
 * Append only MAX aggregation function.
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
public class MaxAppendOnlyFunction extends AAggregationFunction {

	/** serialVersionUID */
	private static final long serialVersionUID = 2557675987778312967L;

	/** Plugin value key */
	public static final String KEY = "max";

	public MaxAppendOnlyFunction() { super(KEY); }

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
				case Types.CONTENT_DOUBLE: return new MaxAggregationDoubleNullable(id, this, inputDataType);
				case Types.CONTENT_FLOAT: return new MaxAggregationFloatNullable(id, this, inputDataType);
				case Types.CONTENT_LONG: return new MaxAggregationLongNullable(id, this, inputDataType);
				case Types.CONTENT_INT: return new MaxAggregationIntNullable(id, this, inputDataType);
				default: throw unsupportedInputDataType(inputDataType);
				}
			} else {
				switch (Types.getContentType(inputDataType)) {
				case Types.CONTENT_DOUBLE: return new MaxAggregationDouble(id, this, inputDataType);
				case Types.CONTENT_FLOAT: return new MaxAggregationFloat(id, this, inputDataType);
				case Types.CONTENT_LONG: return new MaxAggregationLong(id, this, inputDataType);
				case Types.CONTENT_INT: return new MaxAggregationInt(id, this, inputDataType);
				default: throw unsupportedInputDataType(inputDataType);
				}
			}
		} else {
			// When not specified we expect scalar double values
			return new MaxAggregationDoubleNullable(id, this, inputDataType);
		}
	}

	/** Max aggregation for the 'long' data type */
	protected static class MaxAggregationLong extends AAggregation<MaxAppendOnlyFunction> {

		/**
		 * Constructor
		 *
		 * @param id A String that identifies this operation
		 * @param aggFun The {@link MaxAppendOnlyFunction} that created this aggregation
		 * @param dataType The data {@link Types type} of the data elements that will
		 *        be aggregated
		 */
		public MaxAggregationLong(final String id, final MaxAppendOnlyFunction aggFun, final int dataType) {
			super(id, aggFun, dataType);
		}

		@Override
		public IAggregationBinding bindSource(IArrayReader source, IArrayWriter destination) {
			return new MaxBindingLong(source, (IWritableArray) destination);
		}

		@Override
		public IAggregationBinding bindAggregates(IArrayReader source, IArrayWriter destination) {
			return new MaxBindingLong(source, (IWritableArray) destination);
		}

	}

	/** Max aggregation for the 'int' data type */
	protected static class MaxAggregationInt extends AAggregation<MaxAppendOnlyFunction> {

		/**
		 * Constructor
		 *
		 * @param id A String that identifies this operation
		 * @param aggFun The {@link MaxAppendOnlyFunction} that created this aggregation
		 * @param dataType The data {@link Types type} of the data elements that will
		 *        be aggregated
		 */
		public MaxAggregationInt(final String id, final MaxAppendOnlyFunction aggFun, final int dataType) {
			super(id, aggFun, dataType);
		}

		@Override
		public IAggregationBinding bindSource(IArrayReader source, IArrayWriter destination) {
			return new MaxBindingInt(source, (IWritableArray) destination);
		}

		@Override
		public IAggregationBinding bindAggregates(IArrayReader source, IArrayWriter destination) {
			return new MaxBindingInt(source, (IWritableArray) destination);
		}

	}

	/** Max aggregation for the nullable 'long' data type */
	protected static class MaxAggregationLongNullable extends AAggregation<MaxAppendOnlyFunction> {

		/**
		 * Constructor
		 *
		 * @param id A String that identifies this operation
		 * @param aggFun The {@link MaxAppendOnlyFunction} that created this aggregation
		 * @param dataType The data {@link Types type} of the data elements that will
		 *        be aggregated
		 */
		public MaxAggregationLongNullable(final String id, final MaxAppendOnlyFunction aggFun, final int dataType) {
			super(id, aggFun, dataType);
		}

		@Override
		public IAggregationBinding bindSource(IArrayReader source, IArrayWriter destination) {
			return new MaxBindingLongNullable(source, (IWritableArray) destination);
		}

		@Override
		public IAggregationBinding bindAggregates(IArrayReader source, IArrayWriter destination) {
			return new MaxBindingLongNullable(source, (IWritableArray) destination);
		}

	}

	/** Max aggregation for the nullable 'int' data type */
	protected static class MaxAggregationIntNullable extends AAggregation<MaxAppendOnlyFunction> {

		/**
		 * Constructor
		 *
		 * @param id A String that identifies this operation
		 * @param aggFun The {@link MaxAppendOnlyFunction} that created this aggregation
		 * @param dataType The data {@link Types type} of the data elements that will
		 *        be aggregated
		 */
		public MaxAggregationIntNullable(final String id, final MaxAppendOnlyFunction aggFun, final int dataType) {
			super(id, aggFun, dataType);
		}

		@Override
		public IAggregationBinding bindSource(IArrayReader source, IArrayWriter destination) {
			return new MaxBindingIntNullable(source, (IWritableArray) destination);
		}

		@Override
		public IAggregationBinding bindAggregates(IArrayReader source, IArrayWriter destination) {
			return new MaxBindingIntNullable(source, (IWritableArray) destination);
		}

	}

	/** Max aggregation for the 'double' data type */
	protected static class MaxAggregationDouble extends AAggregation<MaxAppendOnlyFunction> {

		/**
		 * Constructor
		 *
		 * @param id A String that identifies this operation
		 * @param aggFun The {@link MaxAppendOnlyFunction} that created this aggregation
		 * @param dataType The data {@link Types type} of the data elements that will
		 *        be aggregated
		 */
		public MaxAggregationDouble(final String id, final MaxAppendOnlyFunction aggFun, final int dataType) {
			super(id, aggFun, dataType);
		}

		@Override
		public IAggregationBinding bindSource(IArrayReader source, IArrayWriter destination) {
			return new MaxBindingDouble(source, (IWritableArray) destination);
		}

		@Override
		public IAggregationBinding bindAggregates(IArrayReader source, IArrayWriter destination) {
			return new MaxBindingDouble(source, (IWritableArray) destination);
		}

	}

	/** Max aggregation for the 'float' data type */
	protected static class MaxAggregationFloat extends AAggregation<MaxAppendOnlyFunction> {

		/**
		 * Constructor
		 *
		 * @param id A String that identifies this operation
		 * @param aggFun The {@link MaxAppendOnlyFunction} that created this aggregation
		 * @param dataType The data {@link Types type} of the data elements that will
		 *        be aggregated
		 */
		public MaxAggregationFloat(final String id, final MaxAppendOnlyFunction aggFun, final int dataType) {
			super(id, aggFun, dataType);
		}

		@Override
		public IAggregationBinding bindSource(IArrayReader source, IArrayWriter destination) {
			return new MaxBindingFloat(source, (IWritableArray) destination);
		}

		@Override
		public IAggregationBinding bindAggregates(IArrayReader source, IArrayWriter destination) {
			return new MaxBindingFloat(source, (IWritableArray) destination);
		}

	}

	/** Max aggregation for the nullable 'double' data type */
	protected static class MaxAggregationDoubleNullable extends AAggregation<MaxAppendOnlyFunction> {

		/**
		 * Constructor
		 *
		 * @param id A String that identifies this operation
		 * @param aggFun The {@link MaxAppendOnlyFunction} that created this aggregation
		 * @param dataType The data {@link Types type} of the data elements that will
		 *        be aggregated
		 */
		public MaxAggregationDoubleNullable(final String id, final MaxAppendOnlyFunction aggFun, final int dataType) {
			super(id, aggFun, dataType);
		}

		@Override
		public IAggregationBinding bindSource(IArrayReader source, IArrayWriter destination) {
			return new MaxBindingDoubleNullable(source, (IWritableArray) destination);
		}

		@Override
		public IAggregationBinding bindAggregates(IArrayReader source, IArrayWriter destination) {
			return new MaxBindingDoubleNullable(source, (IWritableArray) destination);
		}

	}

	/** Max aggregation for the nullable 'float' data type */
	protected static class MaxAggregationFloatNullable extends AAggregation<MaxAppendOnlyFunction> {

		/**
		 * Constructor
		 *
		 * @param id A String that identifies this operation
		 * @param aggFun The {@link MaxAppendOnlyFunction} that created this aggregation
		 * @param dataType The data {@link Types type} of the data elements that will
		 *        be aggregated
		 */
		public MaxAggregationFloatNullable(final String id, final MaxAppendOnlyFunction aggFun, final int dataType) {
			super(id, aggFun, dataType);
		}

		@Override
		public IAggregationBinding bindSource(IArrayReader source, IArrayWriter destination) {
			return new MaxBindingFloatNullable(source, (IWritableArray) destination);
		}

		@Override
		public IAggregationBinding bindAggregates(IArrayReader source, IArrayWriter destination) {
			return new MaxBindingFloatNullable(source, (IWritableArray) destination);
		}

	}

	// BINDINGS

	
	/** Abstract Max binding */
	protected abstract static class AMaxBinding extends AAggregationBinding {
	
		/** Input data */
		protected final IArrayReader input;

		/** Output aggregates */
		protected final IWritableArray output;

		protected AMaxBinding(IArrayReader input, IWritableArray output) {
			this.input = input;
			this.output = output;
		}
		
		@Override
		public void disaggregate(int from, int to) { 
			throw new UnsupportedOperationException(KEY + " aggregation function is append-only, it does not support removals and disaggregation.");
		}
		
	}
	
	/** Max binding for the 'long' data type */
	public static class MaxBindingLong extends AMaxBinding {

		protected MaxBindingLong(IArrayReader input, IWritableArray output) {
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
			if(in > out) {
				output.writeLong(to, in);
			}
		}

	}

	/** Max binding for the 'int' data type */
	public static class MaxBindingInt extends AMaxBinding {

		protected MaxBindingInt(IArrayReader input, IWritableArray output) {
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
			if(in > out) {
				output.writeInt(to, in);
			}
		}

	}

	/** Max binding for the nullable 'long' data type */
	public static class MaxBindingLongNullable extends AMaxBinding {

		protected MaxBindingLongNullable(IArrayReader input, IWritableArray output) {
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
					if(in > out) {
						output.writeLong(to, in);
					}
				}
			}
		}

	}

	/** Max binding for the nullable 'int' data type */
	public static class MaxBindingIntNullable extends AMaxBinding {

		protected MaxBindingIntNullable(IArrayReader input, IWritableArray output) {
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
					if(in > out) {
						output.writeInt(to, in);
					}
				}
			}
		}

	}

	/** Max binding for the 'double' data type */
	public static class MaxBindingDouble extends AMaxBinding {

		protected MaxBindingDouble(IArrayReader input, IWritableArray output) {
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
			if(in > out) {
				output.writeDouble(to, in);
			}
		}

	}

	/** Max binding for the 'float' data type */
	public static class MaxBindingFloat extends AMaxBinding {

		protected MaxBindingFloat(IArrayReader input, IWritableArray output) {
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
			if(in > out) {
				output.writeFloat(to, in);
			}
		}

	}

	/** Max binding for the nullable 'double' data type */
	public static class MaxBindingDoubleNullable extends AMaxBinding {

		protected MaxBindingDoubleNullable(IArrayReader input, IWritableArray output) {
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
					if(in > out) {
						output.writeDouble(to, in);
					}
				}
			}
		}

	}

	/** Max binding for the nullable 'float' data type */
	public static class MaxBindingFloatNullable extends AMaxBinding {

		protected MaxBindingFloatNullable(IArrayReader input, IWritableArray output) {
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
					if(in > out) {
						output.writeFloat(to, in);
					}
				}
			}
		}

	}
}