/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

package io.crate.planner.node.dql;

import io.crate.analyze.WhereClause;
import io.crate.analyze.symbol.Symbol;
import io.crate.metadata.Routing;
import io.crate.planner.distribution.DistributionInfo;
import io.crate.planner.distribution.UpstreamPhase;
import io.crate.planner.node.ExecutionPhaseVisitor;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.Set;
import java.util.function.Function;

public class CountPhase implements UpstreamPhase {

    private final int executionPhaseId;
    private final Routing routing;
    private WhereClause whereClause;
    private DistributionInfo distributionInfo;

    public CountPhase(int executionPhaseId,
                      Routing routing,
                      WhereClause whereClause,
                      DistributionInfo distributionInfo) {
        this.executionPhaseId = executionPhaseId;
        this.routing = routing;
        this.whereClause = whereClause;
        this.distributionInfo = distributionInfo;
    }

    @Override
    public Type type() {
        return Type.COUNT;
    }

    @Override
    public String name() {
        return "count";
    }

    public Routing routing() {
        return routing;
    }

    public WhereClause whereClause() {
        return whereClause;
    }

    @Override
    public int phaseId() {
        return executionPhaseId;
    }

    @Override
    public Set<String> nodeIds() {
        return routing.nodes();
    }

    @Override
    public DistributionInfo distributionInfo() {
        return distributionInfo;
    }

    @Override
    public void distributionInfo(DistributionInfo distributionInfo) {
        this.distributionInfo = distributionInfo;
    }

    @Override
    public <C, R> R accept(ExecutionPhaseVisitor<C, R> visitor, C context) {
        return visitor.visitCountPhase(this, context);
    }

    public CountPhase(StreamInput in) throws IOException {
        executionPhaseId = in.readVInt();
        routing = new Routing(in);
        whereClause = new WhereClause(in);
        distributionInfo = DistributionInfo.fromStream(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeVInt(executionPhaseId);
        routing.writeTo(out);
        whereClause.writeTo(out);
        distributionInfo.writeTo(out);
    }

    public void replaceSymbols(Function<Symbol, Symbol> replaceFunction) {
        whereClause.replace(replaceFunction);
    }
}
