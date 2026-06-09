package com.analyzer.util;

import com.analyzer.graph.MethodGraph;
import com.analyzer.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MethodVisualizationManager {
    private static final Logger logger = LoggerFactory.getLogger(MethodVisualizationManager.class);
    private MethodGraph methodGraph;
    private Gson gson;

    public MethodVisualizationManager(MethodGraph methodGraph) {
        this.methodGraph = methodGraph;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    // ==================== CALL TREE ====================
    public String generateMethodCallTree(String methodId, String theme, int depth) {
        Map<String, Object> treeData = methodGraph.getCallTree(methodId, depth);
        String json = gson.toJson(treeData);
        String bg = theme.equals("dark") ? "#1a1a2e" : "#f8f9fa";
        String fg = theme.equals("dark") ? "#e0e0e0" : "#212121";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Call Tree</title><style>" +
            "body{margin:0;overflow:hidden;background:" + bg + ";font-family:'Segoe UI',Arial,sans-serif}" +
            "#info{position:fixed;top:10px;left:10px;color:" + fg + ";background:" + (theme.equals("dark") ? "#16213e" : "#fff") + 
            ";padding:8px 16px;border-radius:6px;font-size:13px;z-index:10}" +
            "canvas{display:block}" +
            "</style></head><body>" +
            "<div id='info'></div>" +
            "<canvas id='c'></canvas>" +
            "<script>" +
            "const data=" + json + ";\n" +
            getCallTreeScript(theme) +
            "</script></body></html>";
    }

    private String getCallTreeScript(String theme) {
        String bg = theme.equals("dark") ? "#1a1a2e" : "#f8f9fa";
        String fg = theme.equals("dark") ? "#e0e0e0" : "#212121";
        return """
            var c=document.getElementById('c'),ctx=c.getContext('2d');
            var info=document.getElementById('info');
            var nodes=[],edges=[],dragNode=null,offsetX=0,offsetY=0;
            var scale=1,panX=0,panY=0,lastX=0,lastY=0,isPanning=false;
            function resize(){c.width=window.innerWidth;c.height=window.innerHeight;draw();}
            window.addEventListener('resize',resize);
            function flatten(n,x,y,lvl,depth){
                nodes.push({id:n.id,label:n.label||n.id,x:x,y:y,level:lvl,children:n.children||[]});
                var kids=n.children||[];
                var startX=x-(kids.length-1)*90;
                kids.forEach(function(ch,i){
                    var cx=startX+i*180;
                    var cy=y+80;
                    edges.push({from:x,fromY:y,to:cx,toY:cy});
                    flatten(ch,cx,cy,lvl+1,depth);
                });
            }
            flatten(data,c.width/2,60,0,5);
            function draw(){
                ctx.clearRect(0,0,c.width,c.height);
                ctx.fillStyle='" + bg + "';
                ctx.fillRect(0,0,c.width,c.height);
                ctx.save();
                ctx.translate(panX,panY);
                ctx.scale(scale,scale);
                edges.forEach(function(e){
                    ctx.strokeStyle='" + (theme.equals("dark") ? "#555" : "#ccc") + "';
                    ctx.lineWidth=1.5;
                    ctx.beginPath();
                    ctx.moveTo(e.from,e.fromY+12);
                    ctx.lineTo(e.to,e.toY-12);
                    ctx.stroke();
                });
                nodes.forEach(function(n){
                    var r=Math.max(14,30-n.level*3);
                    var grad=ctx.createRadialGradient(n.x,n.y,r*0.3,n.x,n.y,r);
                    grad.addColorStop(0,'#4CAF50');
                    grad.addColorStop(1,'#2E7D32');
                    ctx.beginPath();
                    ctx.arc(n.x,n.y,r,0,Math.PI*2);
                    ctx.fillStyle=grad;
                    ctx.fill();
                    ctx.strokeStyle='#fff';
                    ctx.lineWidth=2;
                    ctx.stroke();
                    ctx.fillStyle='#fff';
                    ctx.font='bold 11px "Segoe UI",Arial';
                    ctx.textAlign='center';
                    var lbl=trunc(n.label,16);
                    ctx.fillText(lbl,n.x,n.y+r+16);
                });
                ctx.restore();
            }
            function trunc(s,n){return s&&s.length>n?s.substring(0,n)+'..':s||'?'}
            c.addEventListener('mousemove',function(e){
                if(dragNode){
                    dragNode.x=e.clientX/scale-panX/scale;
                    dragNode.y=e.clientY/scale-panY/scale;
                    draw();
                    return;
                }
                if(isPanning){
                    panX=e.clientX-lastX;
                    panY=e.clientY-lastY;
                    draw();
                    return;
                }
                var mx=e.clientX/scale-panX/scale,my=e.clientY/scale-panY/scale;
                var found=null;
                nodes.forEach(function(n){
                    var r=Math.max(14,30-n.level*3);
                    var dx=mx-n.x,dy=my-n.y;
                    if(dx*dx+dy*dy<r*r)found=n;
                });
                c.style.cursor=found?'pointer':'default';
                info.textContent=found?found.label||found.id:'';
            });
            c.addEventListener('mousedown',function(e){
                var mx=e.clientX/scale-panX/scale,my=e.clientY/scale-panY/scale;
                nodes.forEach(function(n){
                    var r=Math.max(14,30-n.level*3);
                    var dx=mx-n.x,dy=my-n.y;
                    if(dx*dx+dy*dy<r*r){dragNode=n;offsetX=mx-n.x;offsetY=my-n.y;}
                });
                if(!dragNode){isPanning=true;lastX=e.clientX-panX;lastY=e.clientY-panY;}
            });
            c.addEventListener('mouseup',function(){dragNode=null;isPanning=false;});
            c.addEventListener('wheel',function(e){
                e.preventDefault();
                var ds=e.deltaY<0?1.1:0.9;
                scale=Math.max(0.2,Math.min(3,scale*ds));
                draw();
            });
            resize();
            """;
    }

    // ==================== IMPACT ANALYSIS ====================
    public String generateImpactAnalysis(String methodId, String theme) {
        Set<String> impacted = methodGraph.getImpactAnalysis(methodId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("targetMethod", methodId);
        data.put("targetLabel", extractMethodName(methodId));
        data.put("impactedCount", impacted.size());
        
        List<Map<String, String>> nodes = new ArrayList<>();
        Map<String, String> center = new LinkedHashMap<>();
        center.put("id", methodId);
        center.put("label", extractMethodName(methodId));
        center.put("type", "modified");
        nodes.add(center);
        
        List<Map<String, String>> edges = new ArrayList<>();
        int idx = 0;
        for (String m : impacted) {
            Map<String, String> node = new LinkedHashMap<>();
            node.put("id", m);
            node.put("label", extractMethodName(m));
            node.put("type", "impacted");
            node.put("level", String.valueOf(idx % 3));
            nodes.add(node);
            Map<String, String> edge = new LinkedHashMap<>();
            edge.put("source", methodId);
            edge.put("target", m);
            edges.add(edge);
            idx++;
        }
        
        data.put("nodes", nodes);
        data.put("edges", edges);
        String json = gson.toJson(data);
        String bg = theme.equals("dark") ? "#1a1a2e" : "#f8f9fa";
        String fg = theme.equals("dark") ? "#e0e0e0" : "#212121";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Impact Analysis</title><style>" +
            "body{margin:0;overflow:hidden;background:" + bg + ";font-family:'Segoe UI',Arial}" +
            "#stats{position:fixed;top:10px;right:10px;color:" + fg + ";background:" + (theme.equals("dark") ? "#16213e" : "#fff") +
            ";padding:8px 16px;border-radius:6px;font-size:14px;z-index:10}" +
            "canvas{display:block}" +
            "</style></head><body>" +
            "<div id='stats'></div>" +
            "<canvas id='c'></canvas>" +
            "<script>var data=" + json + ";\n" +
            getImpactScript(theme) +
            "</script></body></html>";
    }

    private String getImpactScript(String theme) {
        return """
            var c=document.getElementById('c'),ctx=c.getContext('2d');
            var nodes=data.nodes,edges=data.edges;
            var cx,cy,r,anim=0;
            var hovered=null;
            var stats=document.getElementById('stats');
            stats.textContent='Impacted: '+data.impactedCount;
            function resize(){c.width=window.innerWidth;c.height=window.innerHeight;cx=c.width/2;cy=c.height/2;r=Math.min(cx,cy)*0.4;draw();}
            window.addEventListener('resize',resize);
            function draw(){
                ctx.clearRect(0,0,c.width,c.height);
                ctx.fillStyle='" + (theme.equals("dark") ? "#1a1a2e" : "#f8f9fa") + "';
                ctx.fillRect(0,0,c.width,c.height);
                // Center node
                var grad=ctx.createRadialGradient(cx,cy,r*0.05,cx,cy,r*0.12);
                grad.addColorStop(0,'#FF5252');grad.addColorStop(1,'#B71C1C');
                ctx.beginPath();ctx.arc(cx,cy,r*0.1,0,Math.PI*2);
                ctx.fillStyle=grad;ctx.fill();
                ctx.fillStyle='#fff';ctx.font='bold 12px "Segoe UI",Arial';ctx.textAlign='center';
                ctx.fillText(trunc(data.targetLabel,14),cx,cy+r*0.15);
                // Impacted nodes on ring
                var count=Math.max(1,edges.length);
                edges.forEach(function(e,i){
                    var angle=(i/count)*Math.PI*2-Math.PI/2;
                    var dist=r*0.35+Math.sin(anim+i)*5;
                    var nx=cx+Math.cos(angle)*dist;
                    var ny=cy+Math.sin(angle)*dist;
                    var nlabel=trunc(findLabel(e.target),12);
                    // Edge line
                    ctx.strokeStyle='" + (theme.equals("dark") ? "#555" : "#ddd") + "';
                    ctx.lineWidth=1;
                    ctx.beginPath();
                    ctx.moveTo(cx+Math.cos(angle)*r*0.1,cy+Math.sin(angle)*r*0.1);
                    ctx.lineTo(nx,ny);
                    ctx.stroke();
                    // Node
                    var nr=10+(i%3)*3;
                    ctx.beginPath();ctx.arc(nx,ny,nr,0,Math.PI*2);
                    ctx.fillStyle=hovered===e?'#FF9800':'#FFA726';
                    ctx.fill();
                    ctx.fillStyle='" + (theme.equals("dark") ? "#e0e0e0" : "#333") + "';
                    ctx.font='10px "Segoe UI",Arial';
                    ctx.fillText(nlabel,nx,ny+nr+12);
                });
                anim+=0.02;
                requestAnimationFrame(draw);
            }
            function findLabel(id){
                for(var i=0;i<nodes.length;i++){if(nodes[i].id===id)return nodes[i].label;}
                return id;
            }
            function trunc(s,n){return s&&s.length>n?s.substring(0,n)+'..':s||'?'}
            c.addEventListener('mousemove',function(e){
                var mx=e.clientX,my=e.clientY;
                var found=null;
                for(var i=0;i<edges.length;i++){
                    var angle=(i/edges.length)*Math.PI*2-Math.PI/2;
                    var dist=r*0.35+Math.sin(anim+i)*5;
                    var nx=cx+Math.cos(angle)*dist,ny=cy+Math.sin(angle)*dist;
                    var nr=10+(i%3)*3;
                    var dx=mx-nx,dy=my-ny;
                    if(dx*dx+dy*dy<nr*nr+25){found=edges[i];break;}
                }
                var dx=mx-cx,dy=my-cy;
                if(!found&&dx*dx+dy*dy<r*r*0.02)found={target:data.targetMethod};
                hovered=found;
                c.style.cursor=found?'pointer':'default';
                if(found)stats.textContent=found.target||data.targetMethod;
                else stats.textContent='Impacted: '+data.impactedCount;
            });
            resize();
            """;
    }

    // ==================== COMPLEXITY MATRIX ====================
    public String generateComplexityMatrix(String theme) {
        List<MethodNode> methods = new ArrayList<>(methodGraph.getMethods().values());
        methods.sort((a, b) -> Integer.compare(b.complexity, a.complexity));
        
        String bg = theme.equals("dark") ? "#1a1a2e" : "#f8f9fa";
        String fg = theme.equals("dark") ? "#e0e0e0" : "#212121";
        String cardBg = theme.equals("dark") ? "#16213e" : "#ffffff";
        String border = theme.equals("dark") ? "#0f3460" : "#e0e0e0";

        StringBuilder rows = new StringBuilder();
        int limit = Math.min(50, methods.size());
        for (int i = 0; i < limit; i++) {
            MethodNode m = methods.get(i);
            String cls = m.complexity > 15 ? "high" : m.complexity > 8 ? "medium" : "low";
            String color = m.complexity > 15 ? "#D32F2F" : m.complexity > 8 ? "#F57F17" : "#388E3C";
            rows.append("<tr class='").append(cls).append("'>");
            rows.append("<td>").append(i + 1).append("</td>");
            rows.append("<td title='").append(esc(m.methodId)).append("'>").append(esc(trunc(extractMethodName(m.methodId), 35))).append("</td>");
            rows.append("<td>").append(esc(trunc(m.className, 30))).append("</td>");
            rows.append("<td class='cx-cell' style='color:").append(color).append("'>").append(m.complexity).append("</td>");
            rows.append("<td>").append(m.codeLines).append("</td>");
            rows.append("<td>").append(m.invokeCount).append("</td>");
            rows.append("<td>").append(m.callMethods.size()).append("</td>");
            rows.append("<td>").append(m.calledByMethods.size()).append("</td>");
            rows.append("</tr>");
        }

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Complexity</title><style>" +
            "body{margin:0;padding:20px;background:" + bg + ";color:" + fg + ";font-family:'Segoe UI',Arial}" +
            "h2{margin:0 0 16px}" +
            "table{width:100%;border-collapse:collapse;font-size:13px}" +
            "th{background:#4CAF50;color:#fff;padding:10px 8px;text-align:left;position:sticky;top:0;z-index:1}" +
            "td{padding:8px;border-bottom:1px solid " + border + "}" +
            "tr.high{background:" + (theme.equals("dark") ? "#3e1a1a" : "#FFEBEE") + "}" +
            "tr.medium{background:" + (theme.equals("dark") ? "#3e2e1a" : "#FFF3E0") + "}" +
            "tr.low{background:" + (theme.equals("dark") ? "#1a2e1a" : "#E8F5E9") + "}" +
            "tr:hover{filter:brightness(1.2)}" +
            ".cx-cell{font-weight:bold;font-size:16px}" +
            "</style></head><body>" +
            "<h2>Complexity Analysis (" + limit + " methods)</h2>" +
            "<table><thead><tr>" +
            "<th>#</th><th>Method</th><th>Class</th><th>Cx</th><th>Lines</th><th>Calls</th><th>Out</th><th>In</th>" +
            "</tr></thead><tbody>" + rows + "</tbody></table>" +
            "</body></html>";
    }

    // ==================== CYCLE DETECTION ====================
    public String generateCycleDetection(String theme) {
        List<List<String>> cycles = methodGraph.findCyclicCalls();
        String bg = theme.equals("dark") ? "#1a1a2e" : "#f8f9fa";
        String fg = theme.equals("dark") ? "#e0e0e0" : "#212121";
        String cardBg = theme.equals("dark") ? "#16213e" : "#ffffff";

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Cycles</title><style>");
        html.append("body{margin:0;padding:20px;background:").append(bg).append(";color:").append(fg).append(";font-family:'Segoe UI',Arial}");
        html.append(".success{color:#388E3C;font-weight:bold;font-size:18px}");
        html.append(".warning{color:#F57F17;font-weight:bold;font-size:18px}");
        html.append(".cycle{background:").append(cardBg).append(";padding:15px;margin:10px 0;border-left:4px solid #F57F17;border-radius:4px}");
        html.append(".badge{display:inline-block;background:#4CAF50;color:#fff;padding:4px 10px;border-radius:3px;font-size:12px;margin:2px}");
        html.append(".arrow{margin:0 4px;color:#999}");
        html.append("</style></head><body>");

        if (cycles.isEmpty()) {
            html.append("<p class='success'>No cyclic calls detected!</p>");
            html.append("<p>Total methods: ").append(methodGraph.getMethods().size()).append("</p>");
        } else {
            html.append("<p class='warning'>Found ").append(cycles.size()).append(" cycle(s)!</p>");
            for (int i = 0; i < cycles.size(); i++) {
                List<String> cycle = cycles.get(i);
                html.append("<div class='cycle'><strong>Cycle #").append(i + 1).append("</strong> (length: ")
                    .append(cycle.size()).append(")<br><br>");
                for (int j = 0; j < cycle.size(); j++) {
                    html.append("<span class='badge'>").append(esc(extractMethodName(cycle.get(j)))).append("</span>");
                    if (j < cycle.size() - 1) html.append("<span class='arrow'>-></span>");
                }
                html.append("</div>");
            }
        }
        html.append("</body></html>");
        return html.toString();
    }

    // ==================== FIELD ACCESS ====================
    public String generateFieldAccessTracking(String theme) {
        String bg = theme.equals("dark") ? "#1a1a2e" : "#f8f9fa";
        String fg = theme.equals("dark") ? "#e0e0e0" : "#212121";
        String cardBg = theme.equals("dark") ? "#16213e" : "#ffffff";

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Fields</title><style>");
        html.append("body{margin:0;padding:20px;background:").append(bg).append(";color:").append(fg).append(";font-family:'Segoe UI',Arial}");
        html.append(".field{background:").append(cardBg).append(";padding:15px;margin:10px 0;border-radius:4px;border:1px solid ").append(theme.equals("dark") ? "#0f3460" : "#e0e0e0").append("}");
        html.append(".badge{display:inline-block;padding:3px 8px;border-radius:3px;font-size:11px;margin:2px;color:#fff}");
        html.append(".read{background:#2196F3}");
        html.append(".write{background:#FF9800}");
        html.append(".field-name{font-weight:bold;font-size:14px}");
        html.append(".field-meta{font-size:12px;color:").append(theme.equals("dark") ? "#aaa" : "#666").append(";margin:4px 0}");
        html.append("</style></head><body>");

        html.append("<h2>Field Access Tracking (").append(methodGraph.getFields().size()).append(" fields)</h2>");

        for (FieldNode field : methodGraph.getFields().values()) {
            html.append("<div class='field'>");
            html.append("<div class='field-name'>").append(esc(field.fieldName)).append("</div>");
            html.append("<div class='field-meta'>").append(esc(field.className)).append(" | ").append(field.fieldType)
                .append(" | read:").append(field.readCount).append(" write:").append(field.writeCount).append("</div>");
            
            if (!field.readByMethods.isEmpty()) {
                html.append("<div><strong>Read by:</strong> ");
                for (String m : field.readByMethods) {
                    html.append("<span class='badge read'>").append(esc(extractMethodName(m))).append("</span>");
                }
                html.append("</div>");
            }
            if (!field.writtenByMethods.isEmpty()) {
                html.append("<div><strong>Written by:</strong> ");
                for (String m : field.writtenByMethods) {
                    html.append("<span class='badge write'>").append(esc(extractMethodName(m))).append("</span>");
                }
                html.append("</div>");
            }
            html.append("</div>");
        }
        html.append("</body></html>");
        return html.toString();
    }

    // ==================== HELPERS ====================
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String trunc(String s, int n) {
        if (s == null) return "";
        return s.length() > n ? s.substring(0, n) + ".." : s;
    }

    private String extractMethodName(String methodId) {
        if (methodId == null) return "?";
        String[] parts = methodId.split("#");
        if (parts.length > 1) {
            String classAndMethod = parts[1];
            int parenIdx = classAndMethod.indexOf('(');
            return parenIdx > 0 ? classAndMethod.substring(0, parenIdx) : classAndMethod;
        }
        return methodId;
    }
}