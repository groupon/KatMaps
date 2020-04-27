/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package com.groupon.katmaps.katmaps_library.util

import android.content.res.Resources
import android.graphics.Rect
import com.google.android.gms.maps.GoogleMap
import com.groupon.katmaps.katmaps_library.MapMarkerContainer
import java.util.ArrayDeque
import java.util.Queue

internal object MapLabelOverlapHider {
    private data class Node(val marker: MapMarkerContainer, val labelBound: Rect) {
        override fun hashCode(): Int = marker.hashCode()
    }

    private data class Edge(val marker1: Node, val marker2: Node) {
        override fun equals(other: Any?): Boolean {
            if (other !is Edge) return false

            return marker1 == other.marker1 && marker2 == other.marker2 || marker1 == other.marker2 && marker2 == other.marker1
        }

        override fun hashCode(): Int = marker1.hashCode() * marker2.hashCode()
    }

    private enum class BipartiteColors {
        RED,
        BLUE
    }

    fun findLabelsToShow(googleMap: GoogleMap, resources: Resources, markers: Set<MapMarkerContainer>, selectedMarker: MapMarkerContainer?): Set<MapMarkerContainer> {
        // Start figuring out the bounds of a label
        val nodes = buildNodes(googleMap, resources, markers)

        // Figure out which node is a selected marker for prioritization
        val selectedMarkerNode = nodes.find { it.marker == selectedMarker }

        // Find edges/build adjacency list
        val adjacencyList = buildGraph(nodes)

        // Graph coloring of a bipartite graph: false set and true set
        val nodesToShow = findNodesToShow(nodes, adjacencyList, selectedMarkerNode)

        return nodesToShow.map { it.marker }.toSet()
    }

    /**
     * Gets the rect bounds for each marker and creates a node for each. Each node
     * is a marker.
     * Assumptions:
     * - Marker anchor is in the default position (bottom center)
     * - Label is directly below the marker and is centered horizontally
     */
    private fun buildNodes(googleMap: GoogleMap, resources: Resources, markers: Set<MapMarkerContainer>): List<Node> {
        return markers.mapNotNull { marker ->
            if (marker.labelBitmap == null) {
                null
            } else {
                marker.getLabelBounds(googleMap, resources)?.run {
                    Node(marker, this) // Needs filtering for nodes that aren't visible
                }
            }
        }
    }

    /**
     * Builds an undirected graph where edges are labels that are overlapping another label.
     */
    private fun buildGraph(nodes: List<Node>): HashMap<Node, HashSet<Node>> {
        val edges = HashSet<Edge>()
        val adjacencyList = HashMap<Node, HashSet<Node>>(nodes.map { it to HashSet<Node>() }.toMap())

        nodes.forEach { firstNode ->
            nodes.forEach { secondNode ->
                if (firstNode != secondNode && Rect.intersects(firstNode.labelBound, secondNode.labelBound)) {
                    val edge = Edge(firstNode, secondNode)
                    if (!edges.contains(edge)) {
                        edges.add(edge)

                        adjacencyList[firstNode]?.add(secondNode)
                        adjacencyList[secondNode]?.add(firstNode)
                    }
                }
            }
        }

        return adjacencyList
    }

    /**
     * Find which labels to hide by maximizing the number of labels shown while
     * prioritizing the current selected marker aka Minimum Vertex Cover.
     * Solves the problem by converting the undirected graph into two bipartite sets.
     * Either sets will contain markers that don't have their labels overlapping.
     * Algorithm then adds the selected marker to both sets and removes any marker that
     * overlaps the selected marker in each respective set. The largest set after that
     * is the set that is returned to show on the map.
     */
    private fun findNodesToShow(nodes: List<Node>, adjacencyList: HashMap<Node, HashSet<Node>>, selectedMarkerNode: Node?): MutableList<Node> {
        val visitedSet = HashSet<Node>()
        val nodesToShow = mutableListOf<Node>()

        nodes.forEach { nextNode ->
            if (visitedSet.contains(nextNode)) return@forEach

            // Create a queue
            val queue: Queue<Node> = ArrayDeque()
            var color = BipartiteColors.BLUE
            queue.add(nextNode) // Provide initial color?
            visitedSet.add(nextNode)

            // Bipartite sets
            val redSet = HashSet<Node>()
            val blueSet = HashSet<Node>()

            blueSet.add(nextNode)

            // Start a BFS
            while (queue.isNotEmpty()) {
                // Pop node
                val currentNode = queue.remove()
                color = when (color) {
                    BipartiteColors.RED -> BipartiteColors.BLUE
                    BipartiteColors.BLUE -> BipartiteColors.RED
                }

                // Find neighbors
                adjacencyList[currentNode]?.forEach { neighborNode ->
                    if (!visitedSet.contains(neighborNode)) {
                        visitedSet.add(neighborNode)
                        queue.add(neighborNode)
                        when (color) {
                            BipartiteColors.RED -> redSet.add(neighborNode)
                            BipartiteColors.BLUE -> blueSet.add(neighborNode)
                        }
                    } else {
                        // This node breaks bipartite condition. Get rid of this node
                        redSet.remove(currentNode)
                        blueSet.remove(currentNode)
                    }
                }
            }

            // Prioritize selected marker and remove any markers' labels that overlap that
            selectedMarkerNode?.let { selectedMarkerNode ->
                redSet.add(selectedMarkerNode)
                blueSet.add(selectedMarkerNode)
                adjacencyList[selectedMarkerNode]?.forEach { adjacentNode ->
                    redSet.remove(adjacentNode)
                    blueSet.remove(adjacentNode)
                }
            }

            if (redSet.size > blueSet.size) {
                nodesToShow.addAll(redSet)
            } else {
                nodesToShow.addAll(blueSet)
            }
        }

        return nodesToShow
    }
}
