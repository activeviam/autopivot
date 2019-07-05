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

import static com.av.csv.CSVSplitter.split;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TestCsvSplitter {

	@Test
	public void testBasicSplit() {
		Assertions.assertThat(split("a,b,c", ",")).containsExactly("a", "b", "c");
		Assertions.assertThat(split("123,test,444,\"don't split, this\",more test,1", ","))
				.containsExactly("123", "test", "444", "don't split, this", "more test", "1");
	}

	@Test
	public void testClassicSeparator() {
		Assertions.assertThat(split("a,b,c", ",")).containsExactly("a", "b", "c");
		Assertions.assertThat(split("a b c", " ")).containsExactly("a", "b", "c");
		Assertions.assertThat(split("a\tb\tc", "\t")).containsExactly("a", "b", "c");
		Assertions.assertThat(split("a;b;c", ";")).containsExactly("a", "b", "c");
		Assertions.assertThat(split("a|b|c", "|")).containsExactly("a", "b", "c");
	}

	@Test
	public void testUnsupportedSeparator() {
		Assertions.assertThatThrownBy(() -> split("a|||b|||c", "|||"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testSplitWithEmptyColumns() {
		Assertions.assertThat(split("a,,c", ",")).containsExactly("a", "", "c");
		Assertions.assertThat(split("a,,", ",")).containsExactly("a", "", "");
		Assertions.assertThat(split(",,", ",")).containsExactly("", "", "");
	}

	@Test
	public void testSplitDoubleQuotes() {
		Assertions.assertThat(split("a,\"b\",c", ",")).containsExactly("a", "b", "c");
		Assertions.assertThat(split("a,\"b,c,d\",e", ",")).containsExactly("a", "b,c,d", "e");
		Assertions.assertThat(split("\"a\",\"b\",\"c\"", ",")).containsExactly("a", "b", "c");
		Assertions.assertThat(split("a,\"\"b\"\",c", ",")).containsExactly("a", "\"b\"", "c");
	}
}
