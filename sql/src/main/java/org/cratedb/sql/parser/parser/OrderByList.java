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

/* The original from which this derives bore the following: */

package org.cratedb.sql.parser.parser;

import java.util.Properties;

/**
 * An OrderByList is an ordered list of columns in the ORDER BY clause.
 * That is, the order of columns in this list is significant - the
 * first column in the list is the most significant in the ordering,
 * and the last column in the list is the least significant.
 *
 */
public class OrderByList extends OrderedColumnList<OrderByColumn>
{
    private boolean allAscending = true;

    /**
       Add a column to the list

       @param column The column to add to the list
    */
    public void addOrderByColumn(OrderByColumn column) {
        add(column);

        if (!column.isAscending())
            allAscending = false;
    }

    /**
     * Are all columns in the list ascending.
     *
     * @return Whether or not all columns in the list ascending.
     */
    boolean allAscending() {
        return allAscending;
    }

    /**
       Get a column from the list

       @param position The column to get from the list
    */
    public OrderByColumn getOrderByColumn(int position) {
        return get(position);
    }

    public String toString() {
        return
            "allAscending: " + allAscending + "\n" +
            super.toString();
    }

}
