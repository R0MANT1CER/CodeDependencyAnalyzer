package com.analyzer.visualization.engine;

import java.util.*;

/**
 * Ultra-clear layout engine - maximizes readability and minimizes edge crossings.
 * Core principles: maximize canvas usage, minimize edge crossings, reinforce hierarchy.
 */
public class SuperClearLayoutEngine {

    public static class NodePosition {
        public String id;
        public double x, y;
        public int depth;
        public String label;
        public String className;
        public double width, height;

        public NodePosition(String id, String label) {
            this.id = id;
            this.label = label;
            this.width = Math.max(120, label.length() * 8);
            this.height = 30;
        }
    }

    public static class Edge {
        public String source, target, type;

        public Edge(String source, String target, String type) {
            this.source = source;
            this.target = target;
            this.type = type;
        }
    }

    /**
     * Optimized Tree Layout - uses subtree size calculation for proper spacing.
     */
    public static class OptimizedTreeLayout {

        public Map<String, NodePosition> layout(
                Map<String, NodePosition> nodes,
                List<Edge> edges,
                double canvasWidth, double canvasHeight,
                String rootId) {

            // Build tree structure
            Map<String, List<String>> childrenMap = new HashMap<>();
            for (Edge edge : edges) {
                childrenMap.computeIfAbsent(edge.source, k -> new ArrayList<>()).add(edge.target);
            }

            // Calculate subtree sizes
            Map<String, Integer> subtreeSize = new HashMap<>();
            calculateSubtreeSize(rootId, childrenMap, subtreeSize);

            // Calculate max depth
            int maxDepth = getMaxDepth(rootId, childrenMap, 0);

            // Layout
            Map<String, Double> xPositions = new HashMap<>();
            Map<String, Integer> yPositions = new HashMap<>();
            double[] xAccumulator = {40}; // left margin

            layoutTree(rootId, childrenMap, subtreeSize, xAccumulator, canvasWidth, maxDepth, xPositions, yPositions, 0);

            // Apply positions
            double vertSpacing = (canvasHeight - 80) / Math.max(1, maxDepth);
            for (NodePosition node : nodes.values()) {
                Double x = xPositions.get(node.id);
                Integer y = yPositions.get(node.id);
                if (x != null) node.x = x;
                if (y != null) node.y = 60 + y * vertSpacing;
                node.depth = y != null ? y : 0;
            }

            return nodes;
        }

        private void calculateSubtreeSize(String nodeId,
                                          Map<String, List<String>> childrenMap,
                                          Map<String, Integer> subtreeSize) {
            List<String> children = childrenMap.getOrDefault(nodeId, Collections.emptyList());
            int size = 1;
            for (String child : children) {
                calculateSubtreeSize(child, childrenMap, subtreeSize);
                size += subtreeSize.getOrDefault(child, 1);
            }
            subtreeSize.put(nodeId, size);
        }

        private void layoutTree(String nodeId,
                                Map<String, List<String>> childrenMap,
                                Map<String, Integer> subtreeSize,
                                double[] xAccumulator,
                                double canvasWidth, int maxDepth,
                                Map<String, Double> xPositions,
                                Map<String, Integer> yPositions,
                                int depth) {

            List<String> children = childrenMap.getOrDefault(nodeId, Collections.emptyList());

            // Layout children first (in-order traversal)
            for (String child : children) {
                layoutTree(child, childrenMap, subtreeSize, xAccumulator,
                        canvasWidth, maxDepth, xPositions, yPositions, depth + 1);
            }

            double nodeWidth = 140;
            double horizGap = 50;
            double nodeX = xAccumulator[0] + nodeWidth / 2;
            xPositions.put(nodeId, nodeX);
            xAccumulator[0] += nodeWidth + horizGap;
            yPositions.put(nodeId, depth);
        }

        private int getMaxDepth(String nodeId, Map<String, List<String>> childrenMap, int current) {
            List<String> children = childrenMap.getOrDefault(nodeId, Collections.emptyList());
            int maxChild = 0;
            for (String child : children) {
                maxChild = Math.max(maxChild, getMaxDepth(child, childrenMap, current + 1));
            }
            return Math.max(current, maxChild);
        }
    }

    /**
     * Wide-Spread Star Layout - for impact analysis with center node.
     */
    public static class WideSpreadLayout {

        public Map<String, NodePosition> layout(
                Map<String, NodePosition> nodes,
                List<Edge> edges,
                double canvasWidth, double canvasHeight,
                String centerId) {

            double cx = canvasWidth / 2;
            double cy = canvasHeight / 2;

            // Position center node
            NodePosition center = nodes.get(centerId);
            if (center != null) {
                center.x = cx;
                center.y = cy;
            }

            // Position impacted nodes in concentric rings
            List<String> impacted = new ArrayList<>();
            for (Edge edge : edges) {
                if (edge.source.equals(centerId)) {
                    impacted.add(edge.target);
                }
            }

            int count = Math.max(1, impacted.size());
            double maxRadius = Math.min(canvasWidth, canvasHeight) * 0.4;

            for (int i = 0; i < impacted.size(); i++) {
                NodePosition node = nodes.get(impacted.get(i));
                if (node == null) continue;

                double angle = (2.0 * Math.PI * i / count) - Math.PI / 2;
                double radius = maxRadius * (0.4 + 0.6 * ((double) i / count));
                node.x = cx + Math.cos(angle) * radius;
                node.y = cy + Math.sin(angle) * radius;
            }

            return nodes;
        }
    }

    /**
     * Hierarchical Layer Layout - for class/method dependency graphs.
     */
    public static class LayerLayout {

        public Map<String, NodePosition> layout(
                Map<String, NodePosition> nodes,
                List<Edge> edges,
                double canvasWidth, double canvasHeight) {

            // Build dependency graph
            Map<String, Set<String>> dependents = new HashMap<>();
            Map<String, Integer> inDegree = new HashMap<>();

            for (NodePosition node : nodes.values()) {
                dependents.put(node.id, new HashSet<>());
                inDegree.put(node.id, 0);
            }
            for (Edge edge : edges) {
                dependents.computeIfAbsent(edge.source, k -> new HashSet<>()).add(edge.target);
                inDegree.merge(edge.target, 1, Integer::sum);
            }

            // Topological layering
            Map<String, Integer> layers = new HashMap<>();
            Queue<String> queue = new LinkedList<>();
            for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
                if (e.getValue() == 0) {
                    queue.offer(e.getKey());
                    layers.put(e.getKey(), 0);
                }
            }

            int maxLayer = 0;
            while (!queue.isEmpty()) {
                String current = queue.poll();
                int currentLayer = layers.get(current);
                for (String dep : dependents.getOrDefault(current, Collections.emptySet())) {
                    int newLayer = currentLayer + 1;
                    if (!layers.containsKey(dep) || layers.get(dep) < newLayer) {
                        layers.put(dep, newLayer);
                        maxLayer = Math.max(maxLayer, newLayer);
                    }
                    int degree = inDegree.merge(dep, -1, Integer::sum);
                    if (degree == 0) queue.offer(dep);
                }
            }

            // Assign any unlayered nodes
            for (String id : nodes.keySet()) {
                layers.putIfAbsent(id, maxLayer + 1);
            }

            // Group by layer and position
            Map<Integer, List<String>> layerGroups = new TreeMap<>();
            for (Map.Entry<String, Integer> e : layers.entrySet()) {
                layerGroups.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
            }

            int totalLayers = Math.max(1, layerGroups.size());
            double layerSpacing = (canvasHeight - 80) / totalLayers;

            for (Map.Entry<Integer, List<String>> entry : layerGroups.entrySet()) {
                int layer = entry.getKey();
                List<String> ids = entry.getValue();
                double nodeSpacing = (canvasWidth - 40) / Math.max(1, ids.size());
                for (int i = 0; i < ids.size(); i++) {
                    NodePosition node = nodes.get(ids.get(i));
                    if (node != null) {
                        node.x = 40 + nodeSpacing * i + nodeSpacing / 2;
                        node.y = 60 + layer * layerSpacing;
                        node.depth = layer;
                    }
                }
            }

            return nodes;
        }
    }
}