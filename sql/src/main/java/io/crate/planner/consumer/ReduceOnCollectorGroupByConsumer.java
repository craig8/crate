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

package io.crate.planner.consumer;

import io.crate.analyze.HavingClause;
import io.crate.analyze.OrderBy;
import io.crate.analyze.QuerySpec;
import io.crate.analyze.relations.AnalyzedRelation;
import io.crate.analyze.relations.DocTableRelation;
import io.crate.analyze.relations.QueriedDocTable;
import io.crate.analyze.symbol.AggregateMode;
import io.crate.analyze.symbol.Symbol;
import io.crate.collections.Lists2;
import io.crate.exceptions.VersionInvalidException;
import io.crate.metadata.RowGranularity;
import io.crate.planner.Limits;
import io.crate.planner.Plan;
import io.crate.planner.PositionalOrderBy;
import io.crate.planner.node.dql.Collect;
import io.crate.planner.node.dql.GroupByConsumer;
import io.crate.planner.node.dql.RoutedCollectPhase;
import io.crate.planner.projection.FilterProjection;
import io.crate.planner.projection.GroupProjection;
import io.crate.planner.projection.Projection;
import io.crate.planner.projection.builder.ProjectionBuilder;
import io.crate.planner.projection.builder.SplitPoints;

import java.util.ArrayList;
import java.util.List;

class ReduceOnCollectorGroupByConsumer implements Consumer {

    private final Visitor visitor;

    ReduceOnCollectorGroupByConsumer(ProjectionBuilder projectionBuilder) {
        visitor = new Visitor(projectionBuilder);
    }

    @Override
    public Plan consume(AnalyzedRelation relation, ConsumerContext context) {
        return visitor.process(relation, context);
    }

    private static class Visitor extends RelationPlanningVisitor {

        private final ProjectionBuilder projectionBuilder;

        public Visitor(ProjectionBuilder projectionBuilder) {
            this.projectionBuilder = projectionBuilder;
        }

        @Override
        public Plan visitQueriedDocTable(QueriedDocTable table, ConsumerContext context) {
            if (table.querySpec().groupBy().isEmpty()) {
                return null;
            }
            DocTableRelation tableRelation = table.tableRelation();
            if (!GroupByConsumer.groupedByClusteredColumnOrPrimaryKeys(
                tableRelation.tableInfo(), table.querySpec().where(), table.querySpec().groupBy())) {
                return null;
            }

            if (table.querySpec().where().hasVersions()) {
                context.validationException(new VersionInvalidException());
                return null;
            }
            return optimizedReduceOnCollectorGroupBy(table, context);
        }

        /**
         * grouping on doc tables by clustered column or primary keys, no distribution needed
         * only one aggregation step as the mappers (shards) have row-authority
         * <p>
         * produces:
         * <p>
         * SELECT:
         * CollectNode ( GroupProjection, [FilterProjection], [TopN] )
         * LocalMergeNode ( TopN )
         */
        private Plan optimizedReduceOnCollectorGroupBy(QueriedDocTable table, ConsumerContext context) {
            QuerySpec querySpec = table.querySpec();
            List<Symbol> groupKeys = querySpec.groupBy();
            assert !groupKeys.isEmpty() : "must have groupBy if optimizeReduceOnCollectorGroupBy is called";
            assert GroupByConsumer.groupedByClusteredColumnOrPrimaryKeys(
                table.tableRelation().tableInfo(), querySpec.where(), groupKeys) : "not grouped by clustered column or primary keys";
            GroupByConsumer.validateGroupBySymbols(groupKeys);

            SplitPoints splitPoints = SplitPoints.create(querySpec);

            // mapper / collect
            List<Symbol> collectOutputs = Lists2.concat(groupKeys, splitPoints.aggregates());
            List<Projection> projections = new ArrayList<>();
            GroupProjection groupProjection = projectionBuilder.groupProjection(
                splitPoints.toCollect(),
                groupKeys,
                splitPoints.aggregates(),
                AggregateMode.ITER_FINAL,
                RowGranularity.SHARD
            );
            projections.add(groupProjection);

            HavingClause havingClause = querySpec.having();
            if (havingClause != null) {
                FilterProjection fp = ProjectionBuilder.filterProjection(collectOutputs, havingClause);
                fp.requiredGranularity(RowGranularity.SHARD);
                projections.add(fp);
            }

            OrderBy orderBy = querySpec.orderBy();
            Limits limits = context.plannerContext().getLimits(querySpec);
            List<Symbol> qsOutputs = querySpec.outputs();
            projections.add(ProjectionBuilder.topNOrEval(
                collectOutputs,
                orderBy,
                0, // no offset
                limits.limitAndOffset(),
                qsOutputs
            ));
            RoutedCollectPhase collectPhase = RoutedCollectPhase.forQueriedTable(
                context.plannerContext(),
                table,
                splitPoints.toCollect(),
                projections
            );
            return new Collect(
                collectPhase,
                limits.finalLimit(),
                limits.offset(),
                qsOutputs.size(),
                limits.limitAndOffset(),
                PositionalOrderBy.of(orderBy, qsOutputs)
            );
        }
    }
}
