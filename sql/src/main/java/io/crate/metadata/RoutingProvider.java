/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.metadata;

import io.crate.exceptions.UnavailableShardsException;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.HashFunction;
import org.elasticsearch.cluster.routing.IndexRoutingTable;
import org.elasticsearch.cluster.routing.IndexShardRoutingTable;
import org.elasticsearch.cluster.routing.ShardIterator;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.SuppressForbidden;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.shard.ShardNotFoundException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import static io.crate.metadata.Routing.forTableOnSingleNode;

/**
 * A variant of {@link org.elasticsearch.cluster.routing.OperationRouting} that allows to set a seed to
 * provide deterministic results.
 */
public final class RoutingProvider {

    private final int seed;
    private final String[] awarenessAttributes;
    private final Random random;

    public RoutingProvider(int seed, String[] awarenessAttributes) {
        this.seed = seed;
        this.random = new Random(seed);
        this.awarenessAttributes = awarenessAttributes;
    }

    public Routing forRandomMasterOrDataNode(TableIdent tableIdent, DiscoveryNodes nodes) {
        DiscoveryNode localNode = nodes.getLocalNode();
        if (localNode.isMasterNode() || localNode.isDataNode()) {
            return forTableOnSingleNode(tableIdent, localNode.getId());
        }
        ImmutableOpenMap<String, DiscoveryNode> masterAndDataNodes = nodes.getMasterAndDataNodes();
        int randomIdx = random.nextInt(masterAndDataNodes.size());
        Iterator<DiscoveryNode> it = masterAndDataNodes.valuesIt();
        int currIdx = 0;
        while (it.hasNext()) {
            if (currIdx == randomIdx) {
                return forTableOnSingleNode(tableIdent, it.next().getId());
            }
            currIdx++;
        }
        throw new AssertionError("Cannot find a master or data node with given random index " + randomIdx);
    }

    public Routing forIndices(ClusterState state,
                              String[] concreteIndices,
                              Map<String, Set<String>> routingValuesByIndex,
                              boolean ignoreMissingShards) {

        Set<IndexShardRoutingTable> shards;
        try {
            shards = computeTargetedShards(state, concreteIndices, routingValuesByIndex);
        } catch (IndexNotFoundException e) {
            return new Routing(Collections.emptyMap());
        }
        Map<String, Map<String, List<Integer>>> locations = new TreeMap<>();

        for (IndexShardRoutingTable shard : shards) {
            if (awarenessAttributes.length == 0) {
                ShardIterator shardIterator = shard.activeInitializingShardsIt(seed);
                ShardRouting shardRouting = shardIterator.nextOrNull();
                if (shardRouting == null) {
                    if (ignoreMissingShards) {
                        continue;
                    }
                    throw new UnavailableShardsException(shardIterator.shardId());
                }
                processShardRouting(locations, shardRouting);
            } else {
                throw new UnsupportedOperationException("TODO: implement and add tests for this case");
            }
        }
        return new Routing(locations);
    }

    private static void processShardRouting(Map<String, Map<String, List<Integer>>> locations, ShardRouting shardRouting) {
        String node = shardRouting.currentNodeId();
        Map<String, List<Integer>> nodeMap = locations.get(node);
        if (nodeMap == null) {
            nodeMap = new TreeMap<>();
            locations.put(shardRouting.currentNodeId(), nodeMap);
        }

        String indexName = shardRouting.getIndexName();
        List<Integer> shards = nodeMap.get(indexName);
        if (shards == null) {
            shards = new ArrayList<>();
            nodeMap.put(indexName, shards);
        }
        shards.add(shardRouting.id());
    }

    private static Set<IndexShardRoutingTable> computeTargetedShards(ClusterState clusterState,
                                                                     String[] concreteIndices,
                                                                     Map<String, Set<String>> routing) {
        LinkedHashSet<IndexShardRoutingTable> set = new LinkedHashSet<>();
        for (String index : concreteIndices) {
            final IndexRoutingTable indexRouting = indexRoutingTable(clusterState, index);
            final IndexMetaData indexMetaData = indexMetaData(clusterState, index);
            final Set<String> effectiveRouting = routing.get(index);
            if (effectiveRouting != null) {
                for (String r : effectiveRouting) {
                    final int routingPartitionSize = indexMetaData.getRoutingPartitionSize();
                    for (int partitionOffset = 0; partitionOffset < routingPartitionSize; partitionOffset++) {
                        set.add(shardRoutingTable(indexRouting, calculateScaledShardId(indexMetaData, r, partitionOffset)));
                    }
                }
            } else {
                for (IndexShardRoutingTable indexShard : indexRouting) {
                    set.add(indexShard);
                }
            }
        }
        return set;
    }

    private static IndexRoutingTable indexRoutingTable(ClusterState clusterState, String index) {
        IndexRoutingTable indexRouting = clusterState.routingTable().index(index);
        if (indexRouting == null) {
            throw new IndexNotFoundException(index);
        }
        return indexRouting;
    }

    private static IndexMetaData indexMetaData(ClusterState clusterState, String index) {
        IndexMetaData indexMetaData = clusterState.metaData().index(index);
        if (indexMetaData == null) {
            throw new IndexNotFoundException(index);
        }
        return indexMetaData;
    }

    private static IndexShardRoutingTable shardRoutingTable(IndexRoutingTable indexRouting, int shardId) {
        IndexShardRoutingTable indexShard = indexRouting.shard(shardId);
        if (indexShard == null) {
            throw new ShardNotFoundException(new ShardId(indexRouting.getIndex(), shardId));
        }
        return indexShard;
    }

    @SuppressForbidden(reason = "Math#abs is trappy")
    private static int calculateScaledShardId(IndexMetaData indexMetaData, String effectiveRouting, int partitionOffset) {
        final HashFunction hashFunction = indexMetaData.routingHashFunction();
        final int hash = hashFunction.hashRouting(effectiveRouting) + partitionOffset;

        // we don't use IMD#getNumberOfShards since the index might have been shrunk such that we need to use the size
        // of original index to hash documents
        if (indexMetaData.getCreationVersion().onOrAfter(Version.V_2_0_0_beta1)) {
            return Math.floorMod(hash, indexMetaData.getRoutingNumShards()) / indexMetaData.getRoutingFactor();
        } else {
            return Math.abs(hash % indexMetaData.getRoutingNumShards()) / indexMetaData.getRoutingFactor();
        }
    }
}
