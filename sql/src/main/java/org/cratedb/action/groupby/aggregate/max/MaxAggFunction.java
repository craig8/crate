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

package org.cratedb.action.groupby.aggregate.max;


import com.google.common.collect.ImmutableSet;
import org.cratedb.DataType;
import org.cratedb.action.groupby.aggregate.AggFunction;

import java.util.Set;

public class MaxAggFunction<T extends Comparable<T>> extends AggFunction<MaxAggState<T>> {

    public static final String NAME = "MAX";
    public static final Set<DataType> supportedColumnTypes = new ImmutableSet.Builder<DataType>()
            .addAll(DataType.NUMERIC_TYPES)
            .add(DataType.STRING)
            .add(DataType.TIMESTAMP)
            .build();

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean iterate(MaxAggState<T> state, Object columnValue) {
        if (state.compareValue((T)columnValue) < 0) {
            state.setValue((T)columnValue);
        }
        return true;
    }


    @Override
    public Set<DataType> supportedColumnTypes() {
        return supportedColumnTypes;
    }
}
