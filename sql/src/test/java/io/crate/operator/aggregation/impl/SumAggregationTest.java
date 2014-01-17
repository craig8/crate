/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.operator.aggregation.impl;

import com.google.common.collect.ImmutableList;
import io.crate.metadata.FunctionIdent;
import io.crate.operator.aggregation.AggregationTest;
import org.cratedb.DataType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SumAggregationTest extends AggregationTest {

    private Object[][] executeAggregation(DataType dataType) throws Exception {
        return executeAggregation("sum", dataType);
    }

    @Test
    public void testReturnType() throws Exception {
        FunctionIdent fi = new FunctionIdent("sum", ImmutableList.of(DataType.INTEGER));
        // Return type is fixed to Double
        assertEquals(DataType.DOUBLE ,functions.get(fi).info().returnType());
    }

    @Test
    public void testDouble() throws Exception {
        setUpTestData(new Object[][]{{0.7d}, {0.3d}});
        Object[][] result = executeAggregation(DataType.DOUBLE);

        assertEquals(1.0d, result[0][0]);
    }

    @Test
    public void testFloat() throws Exception {
        setUpTestData(new Object[][]{{0.7f}, {0.3f}});
        Object[][] result = executeAggregation(DataType.FLOAT);

        assertEquals(1.0d, result[0][0]);
    }

    @Test
    public void testInteger() throws Exception {
        setUpTestData(new Object[][]{{7}, {3}});
        Object[][] result = executeAggregation(DataType.INTEGER);

        assertEquals(10d, result[0][0]);
    }

    @Test
    public void testLong() throws Exception {
        setUpTestData(new Object[][]{{7L}, {3L}});
        Object[][] result = executeAggregation(DataType.LONG);

        assertEquals(10d, result[0][0]);
    }

    @Test
    public void testShort() throws Exception {
        setUpTestData(new Object[][]{{(short) 7}, {(short) 3}});
        Object[][] result = executeAggregation(DataType.SHORT);

        assertEquals(10d, result[0][0]);
    }

    @Test
    public void testUnsupportedType() throws Exception {
        setUpTestData(new Object[][]{{"Youri"}, {"Ruben"}});

        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("AggregationFunction implementation not found [FunctionIdent{name=sum, argumentTypes=[string]}]");
        Object[][] result = executeAggregation(DataType.STRING);
    }
}
