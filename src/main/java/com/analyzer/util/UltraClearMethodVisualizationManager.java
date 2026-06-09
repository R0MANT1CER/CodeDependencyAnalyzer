package com.analyzer.util;

import com.analyzer.graph.MethodGraph;
import com.analyzer.model.*;
import com.analyzer.visualization.engine.SuperClearLayoutEngine;
import java.util.*;

public class UltraClearMethodVisualizationManager {
    private MethodGraph methodGraph;
    private String theme;

    public UltraClearMethodVisualizationManager(MethodGraph methodGraph, String theme) {
        this.methodGraph = methodGraph;
        this.theme = theme;
    }

    public String generateUltraClearCallTree(String methodId, int maxDepth) {
        Map<String, Object> treeData = methodGraph.getCallTree(methodId, maxDepth);
        Map<String, SuperClearLayoutEngine.NodePosition> nodes = new LinkedHashMap<>();
        List<SuperClearLayoutEngine.Edge> edges = new ArrayList<>();
        flattenTree(treeData, nodes, edges);
        if (nodes.isEmpty()) nodes.put(methodId, new SuperClearLayoutEngine.NodePosition(methodId, extractSimple(methodId)));
        new SuperClearLayoutEngine.OptimizedTreeLayout().layout(nodes, edges, 3200, 2000, methodId);
        return canvasHtml("Call Tree: " + extractSimple(methodId), nodes, edges, methodId);
    }

    public String generateUltraClearImpactAnalysis(String methodId) {
        Set<String> impacted = methodGraph.getImpactAnalysis(methodId);
        Map<String, SuperClearLayoutEngine.NodePosition> nodes = new LinkedHashMap<>();
        List<SuperClearLayoutEngine.Edge> edges = new ArrayList<>();
        nodes.put(methodId, new SuperClearLayoutEngine.NodePosition(methodId, extractSimple(methodId)));
        for (String m : impacted) {
            nodes.putIfAbsent(m, new SuperClearLayoutEngine.NodePosition(m, extractSimple(m)));
            edges.add(new SuperClearLayoutEngine.Edge(methodId, m, "impacts"));
        }
        new SuperClearLayoutEngine.WideSpreadLayout().layout(nodes, edges, 1600, 1200, methodId);
        return canvasHtml("Impact: " + extractSimple(methodId), nodes, edges, methodId);
    }

    public String generateUltraClearDependencyGraph() {
        Map<String, SuperClearLayoutEngine.NodePosition> nodes = new LinkedHashMap<>();
        List<SuperClearLayoutEngine.Edge> edges = new ArrayList<>();
        for (MethodNode mn : methodGraph.getMethods().values())
            nodes.put(mn.methodId, new SuperClearLayoutEngine.NodePosition(mn.methodId, mn.methodName + "()"));
        for (MethodCallEdge e : methodGraph.getMethodCallEdges())
            if (nodes.containsKey(e.fromMethod) && nodes.containsKey(e.toMethod))
                edges.add(new SuperClearLayoutEngine.Edge(e.fromMethod, e.toMethod, e.callType));
        new SuperClearLayoutEngine.LayerLayout().layout(nodes, edges, 3200, 2200);
        return canvasHtml("Dependency Graph", nodes, edges, null);
    }

    private void flattenTree(Map<String, Object> node, Map<String, SuperClearLayoutEngine.NodePosition> outN,
                             List<SuperClearLayoutEngine.Edge> outE) {
        if (node == null) return;
        String id = (String) node.get("id");
        String label = (String) node.get("label");
        if (label == null) label = extractSimple(id);
        if (id == null) return;
        outN.putIfAbsent(id, new SuperClearLayoutEngine.NodePosition(id, label));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
        if (children != null) for (Map<String, Object> child : children) {
            String cid = (String) child.get("id");
            if (cid != null) { outE.add(new SuperClearLayoutEngine.Edge(id, cid, "calls")); flattenTree(child, outN, outE); }
        }
    }

    // ==================== CANVAS HTML ====================
    private String canvasHtml(String title, Map<String, SuperClearLayoutEngine.NodePosition> nodes,
                              List<SuperClearLayoutEngine.Edge> edges, String highlightId) {
        boolean dark = "dark".equals(theme);
        String bg = dark ? "#1a1a2e" : "#fafafa";
        String ed = dark ? "#556" : "#aab";

        // Nodes JSON
        StringBuilder nj = new StringBuilder("[");
        boolean f = true;
        for (SuperClearLayoutEngine.NodePosition n : nodes.values()) {
            if (!f) nj.append(",");
            nj.append("{i:").append(js(n.label)).append(",x:").append(fmt(n.x)).append(",y:").append(fmt(n.y))
              .append(",w:").append((int)n.width).append(",r:").append(n.id.equals(highlightId)?1:0).append("}");
            f = false;
        }
        nj.append("]");

        // Edges JSON with pre-computed source/target positions
        Map<String, SuperClearLayoutEngine.NodePosition> nm = new HashMap<>();
        for (SuperClearLayoutEngine.NodePosition n : nodes.values()) nm.put(n.id, n);
        StringBuilder ej = new StringBuilder("[");
        f = true; int ec = 0;
        for (SuperClearLayoutEngine.Edge e : edges) {
            SuperClearLayoutEngine.NodePosition s = nm.get(e.source), t = nm.get(e.target);
            if (s == null || t == null) continue;
            if (!f) ej.append(",");
            ej.append("{sx:").append(fmt(s.x)).append(",sy:").append(fmt(s.y))
              .append(",tx:").append(fmt(t.x)).append(",ty:").append(fmt(t.y))
              .append(",sh:").append((int)s.height).append(",th:").append((int)t.height).append("}");
            f = false; ec++;
        }
        ej.append("]");

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>" + esc(title) + "</title><style>" +
            "body{margin:0;overflow:hidden;background:" + bg + ";font-family:Arial}" +
            "canvas{display:block}" +
            "#bar{position:fixed;top:0;left:0;right:0;background:" + (dark ? "#1a1a3e" : "#333") +
            ";color:#fff;padding:5px 14px;font-size:12px;z-index:10;display:flex;align-items:center;gap:8px}" +
            "#bar button{background:#4682b4;border:none;color:#fff;padding:3px 10px;border-radius:3px;cursor:pointer}" +
            "#bar button.g{background:#2e8b57}#bar button.r{background:#777}" +
            "#info{position:fixed;bottom:6px;right:12px;color:" + (dark ? "#ddd" : "#777") + ";font-size:11px;z-index:10}" +
            "</style></head><body>" +
            "<div id='bar'><b>" + esc(title) + "</b><span style='flex:1'></span>" +
            "<button onclick='Z(1.15)'>+</button><button onclick='Z(0.85)'>-</button>" +
            "<button class='r' onclick='R()'>R</button><button class='g' onclick='D()'>PNG</button></div>" +
            "<div id='info'></div><canvas id='c'></canvas><script>" +
            "var N=" + nj + ";var E=" + ej + ";var NC=" + nodes.size() + ";var EC=" + ec + ";" +
            "var c=document.getElementById('c'),g=c.getContext('2d');" +
            "var info=document.getElementById('info');" +
            "var s=1,px=0,py=0,pn=0,px0=0,py0=0,hv=null,m=80;" +
            "function rs(){c.width=innerWidth;c.height=innerHeight;}" +
            "window.addEventListener('resize',rs);" +
            "function arrow(x1,y1,x2,y2,col){" +
            "var a=Math.atan2(y2-y1,x2-x1);" +
            "g.fillStyle=col;g.beginPath();" +
            "g.moveTo(x2,y2);" +
            "g.lineTo(x2-7*Math.cos(a-0.4),y2-7*Math.sin(a-0.4));" +
            "g.lineTo(x2-7*Math.cos(a+0.4),y2-7*Math.sin(a+0.4));" +
            "g.closePath();g.fill();" +
            "}" +
            "function dr(){" +
            "g.clearRect(0,0,c.width,c.height);" +
            "g.fillStyle='" + bg + "';g.fillRect(0,0,c.width,c.height);" +
            "g.save();g.translate(px,py);g.scale(s,s);" +
            // Edges - straight line with small offset, explicit arrow
            "g.strokeStyle='" + ed + "';g.lineWidth=1.2;" +
            "for(var i=0;i<E.length;i++){" +
            "var e=E[i];" +
            "var x1=e.sx+m,y1=e.sy+e.sh/2+m;" +
            "var x2=e.tx+m,y2=e.ty-e.th/2+m;" +
            // Short straight line with slight horizontal offset
            "var dx=x2-x1,dy=y2-y1;" +
            "if(Math.abs(dx)<5){" +
            // Vertical: straight line
            "g.beginPath();g.moveTo(x1,y1);g.lineTo(x2,y2);g.stroke();" +
            "arrow(x1,y1,x2,y2,'" + ed + "');" +
            "}else{" +
            // Add slight horizontal bend
            "var midY=(y1+y2)/2;" +
            "var offX=Math.min(40,Math.abs(dx)*0.3)*Math.sign(dx);" +
            "g.beginPath();g.moveTo(x1,y1);" +
            "g.bezierCurveTo(x1+offX,y1+dy*0.2,x2-offX,y2-dy*0.2,x2,y2);" +
            "g.stroke();" +
            "arrow(x1+offX,y1+dy*0.2,x2,y2,'" + ed + "');" +
            "}" +
            "}" +
            // Nodes
            "for(var i=0;i<N.length;i++){" +
            "var n=N[i];" +
            "var x=n.x+m,y=n.y+m;" +
            "var rx=x-n.w/2,ry=y-15,w=n.w,h=30;" +
            "var hov=hv&&hv.i===n.i;" +
            "var isRoot=n.r===1;" +
            "var r=5;" +
            "g.beginPath();" +
            "g.moveTo(rx+r,ry);g.lineTo(rx+w-r,ry);" +
            "g.quadraticCurveTo(rx+w,ry,rx+w,ry+r);" +
            "g.lineTo(rx+w,ry+h-r);g.quadraticCurveTo(rx+w,ry+h,rx+w-r,ry+h);" +
            "g.lineTo(rx+r,ry+h);g.quadraticCurveTo(rx,ry+h,rx,ry+h-r);" +
            "g.lineTo(rx,ry+r);g.quadraticCurveTo(rx,ry,rx+r,ry);" +
            "g.closePath();" +
            "g.fillStyle=hov?'#ff9800':(isRoot?'#27ae60':'#3498db');g.fill();" +
            "g.strokeStyle=hov?'#fff':'rgba(255,255,255,0.2)';g.lineWidth=isRoot?2:1.5;g.stroke();" +
            "g.fillStyle='#fff';g.font='bold 11px Arial';g.textAlign='center';g.textBaseline='middle';" +
            "var lb=n.i;if(lb.length>18)lb=lb.substring(0,16)+'..';" +
            "g.fillText(lb,x,ry+h/2);" +
            "}" +
            "g.restore();" +
            "info.textContent='Nodes:'+NC+' Edges:'+EC+' Zoom:'+Math.round(s*100)+'%';" +
            "}" +
            "function Z(v){s=Math.max(0.05,Math.min(5,s*v));dr();}" +
            "function R(){s=1;px=0;py=0;dr();}" +
            "function D(){var a=document.createElement('a');a.download='graph.png';a.href=c.toDataURL();a.click();}" +
            "c.onmousemove=function(e){" +
            "var mx=(e.clientX-px)/s,my=(e.clientY-py)/s;" +
            "if(pn){px=e.clientX-px0;py=e.clientY-py0;dr();return;}" +
            "hv=null;" +
            "for(var i=0;i<N.length;i++){" +
            "var n=N[i];var x=n.x+m,y=n.y+m;" +
            "if(mx>x-n.w/2&&mx<x+n.w/2&&my>y-15&&my<y+15)hv=n;" +
            "}" +
            "c.style.cursor=hv?'pointer':'grab';dr();" +
            "};" +
            "c.onmousedown=function(e){if(hv)return;pn=1;px0=e.clientX-px;py0=e.clientY-py;};" +
            "c.onmouseup=function(){pn=0;};" +
            "c.onwheel=function(e){e.preventDefault();Z(e.deltaY<0?1.1:0.9);};" +
            "rs();dr();" +
            "</script></body></html>";
    }

    public static String extractSimple(String id) {
        if (id == null) return "?";
        int hash = id.indexOf('#');
        if (hash > 0) {
            String after = id.substring(hash + 1);
            int paren = after.indexOf('(');
            return paren > 0 ? after.substring(0, paren) + "()" : after + "()";
        }
        int dot = id.lastIndexOf('.');
        return dot > 0 ? id.substring(dot + 1) : id;
    }

    private static String esc(String s) { return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    private static String js(String s) { return s == null ? "''" : "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'"; }
    private static String fmt(double d) { return String.format(Locale.US, "%.1f", d); }
}