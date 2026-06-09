package com.analyzer.ui;

import com.analyzer.graph.DependencyGraph;
import com.analyzer.model.DependencyEdge;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class GraphVisualizer {
    private static final Logger logger = LoggerFactory.getLogger(GraphVisualizer.class);
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * 生成HTML可视化文件
     */
    public void generateHTMLVisualization(DependencyGraph graph, String outputPath) throws IOException {
        // 生成Cytoscape数据格式
        Map<String, Object> cytoscapeData = generateCytoscapeData(graph);
        
        String html = generateHTMLTemplate(cytoscapeData);
        
        Files.write(Paths.get(outputPath), html.getBytes());
        logger.info("✓ 可视化HTML已生成: {}", outputPath);
    }

    /**
     * 生成Cytoscape.js格式的数据
     */
    private Map<String, Object> generateCytoscapeData(DependencyGraph graph) {
        Map<String, Object> data = new HashMap<>();
        
        List<Map<String, Object>> elements = new ArrayList<>();
        
        // 添加节点
        for (var classNode : graph.getNodes().values()) {
            Map<String, Object> node = new HashMap<>();
            node.put("data", Map.of(
                    "id", classNode.fullName,
                    "label", classNode.simpleName,
                    "title", classNode.fullName
            ));
            elements.add(node);
        }
        
        // 添加边
        Set<String> addedEdges = new HashSet<>();
        for (DependencyEdge edge : graph.getEdges()) {
            String edgeId = edge.source + "-" + edge.target + "-" + edge.type;
            if (!addedEdges.contains(edgeId)) {
                Map<String, Object> edgeObj = new HashMap<>();
                Map<String, Object> edgeData = new HashMap<>();
                edgeData.put("id", edgeId);
                edgeData.put("source", edge.source);
                edgeData.put("target", edge.target);
                edgeData.put("label", edge.type);
                edgeData.put("line", edge.lineNumber);
                edgeObj.put("data", edgeData);
                elements.add(edgeObj);
                addedEdges.add(edgeId);
            }
        }
        
        data.put("elements", elements);
        return data;
    }

    /**
     * 生成HTML模板
     */
    private String generateHTMLTemplate(Map<String, Object> cytoscapeData) {
        String jsonData = new Gson().toJson(cytoscapeData);
        
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>代码依赖分析 - 蜘蛛网图</title>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <script src=\"https://cdnjs.cloudflare.com/ajax/libs/cytoscape/3.28.1/cytoscape.min.js\"></script>\n" +
                "    <style>\n" +
                "        body { margin: 0; padding: 0; font-family: Arial; background: #f5f5f5; }\n" +
                "        #cy { width: 100%; height: 100vh; }\n" +
                "        .info-panel { position: absolute; top: 10px; left: 10px; background: white; padding: 15px; border-radius: 5px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); max-width: 300px; }\n" +
                "        .info-panel h3 { margin-top: 0; color: #333; }\n" +
                "        .info-panel p { margin: 5px 0; font-size: 12px; color: #666; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id=\"cy\"></div>\n" +
                "    <div class=\"info-panel\">\n" +
                "        <h3>代码依赖分析</h3>\n" +
                "        <p>点击节点查看详情</p>\n" +
                "        <p id=\"node-info\">选择一个类...</p>\n" +
                "    </div>\n" +
                "    <script>\n" +
                "        var cy = cytoscape({\n" +
                "            container: document.getElementById('cy'),\n" +
                "            elements: " + jsonData + ".elements,\n" +
                "            style: [\n" +
                "                {\n" +
                "                    selector: 'node',\n" +
                "                    style: {\n" +
                "                        'background-color': '#4CAF50',\n" +
                "                        'label': 'data(label)',\n" +
                "                        'color': 'white',\n" +
                "                        'text-halign': 'center',\n" +
                "                        'text-valign': 'center',\n" +
                "                        'padding': '10px',\n" +
                "                        'font-size': '12px'\n" +
                "                    }\n" +
                "                },\n" +
                "                {\n" +
                "                    selector: 'node:hover',\n" +
                "                    style: {\n" +
                "                        'background-color': '#2196F3',\n" +
                "                        'box-shadow': '0 0 10px rgba(0,0,0,0.3)'\n" +
                "                    }\n" +
                "                },\n" +
                "                {\n" +
                "                    selector: 'edge',\n" +
                "                    style: {\n" +
                "                        'line-color': '#999',\n" +
                "                        'target-arrow-color': '#999',\n" +
                "                        'target-arrow-shape': 'triangle',\n" +
                "                        'label': 'data(label)',\n" +
                "                        'font-size': '10px',\n" +
                "                        'edge-text-rotation': 'autorotate'\n" +
                "                    }\n" +
                "                }\n" +
                "            ],\n" +
                "            layout: {\n" +
                "                name: 'cose',\n" +
                "                directed: true,\n" +
                "                animate: true\n" +
                "            }\n" +
                "        });\n" +
                "\n" +
                "        cy.on('tap', 'node', function(evt){\n" +
                "            var node = evt.target;\n" +
                "            document.getElementById('node-info').innerHTML = \n" +
                "                '<strong>' + node.data('title') + '</strong><br/>' +\n" +
                "                '引用者数: ' + node.indegree() + '<br/>' +\n" +
                "                '被引用数: ' + node.outdegree();\n" +
                "        });\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }
}
