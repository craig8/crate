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

import org.cratedb.sql.parser.StandardException;

/**
 * An FullOuterJoinNode represents a full outer join result set.
 */

public class FullOuterJoinNode extends JoinNode
{
    /**
     * Initializer for a FullOuterJoinNode.
     *
     * @param leftResult The ResultSetNode on the left side of this join
     * @param rightResult The ResultSetNode on the right side of this join
     * @param onClause The ON clause
     * @param usingClause The USING clause
     * @param tableProperties Properties list associated with the table
     *
     * @exception StandardException Thrown on error
     */

    public void init(Object leftResult,
                     Object rightResult,
                     Object onClause,
                     Object usingClause,
                     Object tableProperties)
            throws StandardException {
        super.init(leftResult,
                   rightResult,
                   onClause,
                   usingClause,
                   null,
                   tableProperties,
                   null);
    }
}
