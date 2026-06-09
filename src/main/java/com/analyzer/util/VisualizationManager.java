package com.analyzer.util;

import com.analyzer.graph.DependencyGraph;
import com.analyzer.model.ClassNode;
import com.analyzer.model.DependencyEdge;
import com.analyzer.visualization.engine.SuperClearLayoutEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;

public class VisualizationManager {
    private static final Logger logger = LoggerFactory.getLogger(VisualizationManager.class);
    private DependencyGraph graph;
    private Map<String, ClassNode> classNodes;

    public VisualizationManager(DependencyGraph graph, Map<String, ClassNode> classNodes) {
        this.graph = graph;
        this.classNodes = classNodes;
    }

    static class Nd { String id,label; double x,y; int w; Nd(String i,String l){id=i;label=l;w=Math.max(90,l.length()*9+16);} }
    static class Ed { String s,t; Ed(String a,String b){s=a;t=b;} }

    private Map<String,Nd> nodeMap() {
        Map<String,Nd> m=new LinkedHashMap<>();
        for(ClassNode cn:classNodes.values()) m.put(cn.fullName,new Nd(cn.fullName,cn.simpleName));
        return m;
    }

    private Map<String,SuperClearLayoutEngine.NodePosition> toLN(Map<String,Nd> nodes){
        Map<String,SuperClearLayoutEngine.NodePosition> m=new LinkedHashMap<>();
        for(Nd n:nodes.values()){
            SuperClearLayoutEngine.NodePosition p=new SuperClearLayoutEngine.NodePosition(n.id,n.label);
            p.width=n.w; m.put(n.id,p);
        }
        return m;
    }
    private List<SuperClearLayoutEngine.Edge> toLE(Map<String,Nd> nodes){
        List<SuperClearLayoutEngine.Edge> l=new ArrayList<>();
        if(graph.getEdges()!=null) for(DependencyEdge de:graph.getEdges())
            if(nodes.containsKey(de.source)&&nodes.containsKey(de.target))
                l.add(new SuperClearLayoutEngine.Edge(de.source,de.target,de.type));
        return l;
    }
    private void apply(Map<String,Nd> nodes,Map<String,SuperClearLayoutEngine.NodePosition> ln){
        for(Nd n:nodes.values()){
            SuperClearLayoutEngine.NodePosition p=ln.get(n.id);
            if(p!=null){n.x=p.x;n.y=p.y;}
        }
    }

    // ==================== LAYOUTS ====================
    private Map<String,Nd> layoutLayer(){
        Map<String,Nd> nodes=nodeMap();
        Map<String,SuperClearLayoutEngine.NodePosition> ln=toLN(nodes);
        new SuperClearLayoutEngine.LayerLayout().layout(ln,toLE(nodes),3600,2600);
        apply(nodes,ln); return nodes;
    }
    private Map<String,Nd> layoutForce(){
        Map<String,Nd> nodes=nodeMap();
        List<Nd> list=new ArrayList<>(nodes.values());
        if(list.isEmpty()) return nodes;
        Map<String,Integer> idx=new HashMap<>();
        for(int i=0;i<list.size();i++) idx.put(list.get(i).id,i);
        Set<String> conn=new HashSet<>();
        if(graph.getEdges()!=null) for(DependencyEdge de:graph.getEdges())
            if(idx.containsKey(de.source)&&idx.containsKey(de.target)){
                conn.add(de.source+">"+de.target); conn.add(de.target+">"+de.source);
            }
        Random rnd=new Random(42);
        double area=Math.max(800,Math.sqrt(list.size())*350);
        for(Nd n:list){n.x=rnd.nextDouble()*area; n.y=rnd.nextDouble()*area;}
        for(int iter=0;iter<200;iter++){
            double[] fx=new double[list.size()],fy=new double[list.size()];
            double temp=25.0*(1.0-iter/200.0);
            for(int i=0;i<list.size();i++) for(int j=i+1;j<list.size();j++){
                double dx=list.get(j).x-list.get(i).x,dy=list.get(j).y-list.get(i).y;
                double d=Math.sqrt(dx*dx+dy*dy)+0.01;
                double f=20000.0/(d*d);
                fx[i]-=f*dx/d; fy[i]-=f*dy/d; fx[j]+=f*dx/d; fy[j]+=f*dy/d;
            }
            for(String c:conn){
                String[] p=c.split(">");
                int a=idx.getOrDefault(p[0],-1),b=idx.getOrDefault(p[1],-1);
                if(a<0||b<0) continue;
                double dx=list.get(b).x-list.get(a).x,dy=list.get(b).y-list.get(a).y;
                double d=Math.sqrt(dx*dx+dy*dy)+0.01;
                double f=d*0.02;
                fx[a]+=f*dx/d; fy[a]+=f*dy/d; fx[b]-=f*dx/d; fy[b]-=f*dy/d;
            }
            double cx=area/2,cy=area/2;
            for(int i=0;i<list.size();i++){fx[i]+=(cx-list.get(i).x)*0.02; fy[i]+=(cy-list.get(i).y)*0.02;}
            for(int i=0;i<list.size();i++){
                double len=Math.sqrt(fx[i]*fx[i]+fy[i]*fy[i])+0.01;
                double step=Math.min(temp,len);
                list.get(i).x+=fx[i]/len*step; list.get(i).y+=fy[i]/len*step;
            }
        }
        return nodes;
    }
    private Map<String,Nd> layoutHierarchy(){
        Map<String,Nd> nodes=nodeMap();
        Map<String,List<Nd>> pkgs=new TreeMap<>();
        for(Nd n:nodes.values()){
            String pkg="(default)";
            int dot=n.id.lastIndexOf('.');
            if(dot>0) pkg=n.id.substring(0,dot);
            pkgs.computeIfAbsent(pkg,k->new ArrayList<>()).add(n);
        }
        double x=60,y=60,colW=340;
        int col=0,maxPerCol=12,rowInCol=0;
        for(Map.Entry<String,List<Nd>> e:pkgs.entrySet()){
            for(Nd n:e.getValue()){
                n.x=x+col*colW; n.y=y+rowInCol*50; rowInCol++;
                if(rowInCol>=maxPerCol){rowInCol=0;col++;}
            }
            if(rowInCol>0){rowInCol=0;col++;}
            if(col>=6){col=0;y+=maxPerCol*50+40;}
        }
        return nodes;
    }
    private Map<String,Nd> layoutSunburst(){
        Map<String,Nd> nodes=nodeMap();
        Map<String,SuperClearLayoutEngine.NodePosition> ln=toLN(nodes);
        List<SuperClearLayoutEngine.Edge> le=toLE(nodes);
        String center=le.stream().collect(Collectors.groupingBy(e->e.target,Collectors.counting()))
            .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
            .orElse(ln.keySet().iterator().next());
        new SuperClearLayoutEngine.WideSpreadLayout().layout(ln,le,1600,1200,center);
        apply(nodes,ln);
        return nodes;
    }

    // ==================== RENDER ====================
    private String render(String theme,String title,Map<String,Nd> nodes,boolean sunburst){
        boolean dark="dark".equals(theme);
        String bg=dark?"#1a1a2e":"#fafafa",ed=dark?"#556":"#bbc";
        // Nodes JSON
        StringBuilder nj=new StringBuilder("[");
        boolean f=true;
        for(Nd n:nodes.values()){
            if(!f) nj.append(",");
            nj.append("{i:").append(js(n.label)).append(",x:").append(fmt(n.x)).append(",y:").append(fmt(n.y)).append(",w:").append(n.w).append("}");
            f=false;
        }
        nj.append("]");
        // Edges JSON
        Map<String,Nd> nm=new HashMap<>();
        for(Nd n:nodes.values()) nm.put(n.id,n);
        StringBuilder ej=new StringBuilder("[");
        f=true; int ec=0;
        if(graph.getEdges()!=null) for(DependencyEdge de:graph.getEdges()){
            Nd s=nm.get(de.source),t=nm.get(de.target);
            if(s==null||t==null) continue;
            if(!f) ej.append(",");
            ej.append("{sx:").append(fmt(s.x)).append(",sy:").append(fmt(s.y))
              .append(",tx:").append(fmt(t.x)).append(",ty:").append(fmt(t.y)).append("}");
            f=false; ec++;
        }
        ej.append("]");
        String sbFlag = sunburst ? "1" : "0";
        // Heatmap needs to calculate center for sunburst
        String centerX="0",centerY="0";
        if(sunburst){
            Map<String,SuperClearLayoutEngine.NodePosition> ln=toLN(nodes);
            List<SuperClearLayoutEngine.Edge> le=toLE(nodes);
            String ctr=le.stream().collect(Collectors.groupingBy(e->e.target,Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
                .orElse(ln.keySet().iterator().next());
            SuperClearLayoutEngine.NodePosition cp=ln.get(ctr);
            if(cp!=null){centerX=fmt(cp.x);centerY=fmt(cp.y);}
        }

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>"+esc(title)+"</title><style>"+
            "body{margin:0;overflow:hidden;background:"+bg+";font-family:Arial}"+
            "canvas{display:block}"+
            "#bar{position:fixed;top:0;left:0;right:0;background:"+(dark?"#1a1a3e":"#333")+";color:#fff;padding:5px 14px;font-size:12px;z-index:10;display:flex;align-items:center;gap:8px}"+
            "#bar button{background:#4682b4;border:none;color:#fff;padding:3px 10px;border-radius:3px;cursor:pointer}"+
            "#bar button.g{background:#2e8b57}#bar button.r{background:#777}"+
            "#info{position:fixed;bottom:6px;right:12px;color:"+(dark?"#ddd":"#777")+";font-size:11px;z-index:10}"+
            "</style></head><body>"+
            "<div id='bar'><b>"+esc(title)+"</b><span style='flex:1'></span>"+
            "<button onclick='Z(1.15)'>+</button><button onclick='Z(0.85)'>-</button>"+
            "<button class='r' onclick='R()'>R</button><button class='g' onclick='D()'>PNG</button></div>"+
            "<div id='info'></div><canvas id='c'></canvas><script>"+
            "var N="+nj+";var E="+ej+";var NC="+nodes.size()+";var EC="+ec+";var SB="+sbFlag+";var CX="+centerX+";var CY="+centerY+";"+
            "var c=document.getElementById('c'),g=c.getContext('2d');"+
            "var info=document.getElementById('info');"+
            "var s=1,px=0,py=0,pn=0,px0=0,py0=0,hv=null,m=60;"+
            "function rs(){c.width=innerWidth;c.height=innerHeight;}"+
            "window.addEventListener('resize',rs);"+
            "function dr(){"+
            "g.clearRect(0,0,c.width,c.height);"+
            "g.fillStyle='"+bg+"';g.fillRect(0,0,c.width,c.height);"+
            "g.save();g.translate(px,py);g.scale(s,s);"+
            // Sunburst: draw concentric guide rings
            "if(SB==1){"+
            "var ox=CX+m,oy=CY+m;"+
            "for(var r=80;r<900;r+=80){"+
            "g.beginPath();g.arc(ox,oy,r,0,Math.PI*2);"+
            "g.strokeStyle='"+(dark?"rgba(255,255,255,0.06)":"rgba(0,0,0,0.05)")+"';g.lineWidth=1;g.stroke();"+
            "}"+
            // Radial spokes
            "for(var a=0;a<Math.PI*2;a+=Math.PI/8){"+
            "g.beginPath();g.moveTo(ox,oy);g.lineTo(ox+Math.cos(a)*850,oy+Math.sin(a)*850);"+
            "g.strokeStyle='"+(dark?"rgba(255,255,255,0.04)":"rgba(0,0,0,0.03)")+"';g.stroke();"+
            "}"+
            "}"+
            // Edges
            "g.strokeStyle='"+ed+"';g.lineWidth=1;"+
            "for(var i=0;i<E.length;i++){"+
            "var e=E[i];"+
            "var x1=e.sx+m,y1=e.sy+15+m;"+
            "var x2=e.tx+m,y2=e.ty-15+m;"+
            "var mx=(x1+x2)/2;"+
            "g.beginPath();g.moveTo(x1,y1);"+
            "g.quadraticCurveTo(mx,y1,mx,y2);"+
            "g.quadraticCurveTo(mx,y2,x2,y2);"+
            "g.stroke();"+
            "var a=Math.atan2(y2-y1,x2-x1);"+
            "g.beginPath();g.moveTo(x2,y2);"+
            "g.lineTo(x2-6*Math.cos(a-0.4),y2-6*Math.sin(a-0.4));"+
            "g.lineTo(x2-6*Math.cos(a+0.4),y2-6*Math.sin(a+0.4));"+
            "g.closePath();g.fillStyle='"+ed+"';g.fill();"+
            "}"+
            // Nodes
            "for(var i=0;i<N.length;i++){"+
            "var n=N[i];"+
            "var x=n.x+m,y=n.y+m;"+
            "var rx=x-n.w/2,ry=y-15,w=n.w,h=30;"+
            "var hov=hv&&hv.i===n.i;"+
            "var r=5;"+
            "g.beginPath();"+
            "g.moveTo(rx+r,ry);g.lineTo(rx+w-r,ry);"+
            "g.quadraticCurveTo(rx+w,ry,rx+w,ry+r);"+
            "g.lineTo(rx+w,ry+h-r);g.quadraticCurveTo(rx+w,ry+h,rx+w-r,ry+h);"+
            "g.lineTo(rx+r,ry+h);g.quadraticCurveTo(rx,ry+h,rx,ry+h-r);"+
            "g.lineTo(rx,ry+r);g.quadraticCurveTo(rx,ry,rx+r,ry);"+
            "g.closePath();"+
            "g.fillStyle=hov?'#ff9800':'#3498db';g.fill();"+
            "g.strokeStyle=hov?'#fff':'rgba(255,255,255,0.2)';g.lineWidth=1.5;g.stroke();"+
            "g.fillStyle='#fff';g.font='bold 11px Arial';g.textAlign='center';g.textBaseline='middle';"+
            "var lb=n.i;if(lb.length>16)lb=lb.substring(0,14)+'..';"+
            "g.fillText(lb,x,ry+h/2);"+
            "}"+
            "g.restore();"+
            "info.textContent='Nodes:'+NC+' Edges:'+EC+' Zoom:'+Math.round(s*100)+'%';"+
            "}"+
            "function Z(v){s=Math.max(0.05,Math.min(5,s*v));dr();}"+
            "function R(){s=1;px=0;py=0;dr();}"+
            "function D(){var a=document.createElement('a');a.download='graph.png';a.href=c.toDataURL();a.click();}"+
            "c.onmousemove=function(e){"+
            "var mx=(e.clientX-px)/s,my=(e.clientY-py)/s;"+
            "if(pn){px=e.clientX-px0;py=e.clientY-py0;dr();return;}"+
            "hv=null;"+
            "for(var i=0;i<N.length;i++){"+
            "var n=N[i];var x=n.x+m,y=n.y+m;"+
            "if(mx>x-n.w/2&&mx<x+n.w/2&&my>y-15&&my<y+15)hv=n;"+
            "}"+
            "c.style.cursor=hv?'pointer':'grab';dr();"+
            "};"+
            "c.onmousedown=function(e){if(hv)return;pn=1;px0=e.clientX-px;py0=e.clientY-py;};"+
            "c.onmouseup=function(){pn=0;};"+
            "c.onwheel=function(e){e.preventDefault();Z(e.deltaY<0?1.1:0.9);};"+
            "rs();dr();"+
            "</script></body></html>";
    }

    // ==================== HEATMAP ====================
    private String heatmap(String theme){
        boolean dark="dark".equals(theme);
        List<ClassNode> cls=new ArrayList<>(classNodes.values());
        cls.sort(Comparator.comparing(c->c.simpleName));
        int n=Math.min(40,cls.size());
        Map<String,Integer> idx=new HashMap<>();
        for(int i=0;i<n;i++) idx.put(cls.get(i).fullName,i);
        boolean[][] mat=new boolean[n][n];
        if(graph.getEdges()!=null) for(DependencyEdge de:graph.getEdges()){
            Integer r=idx.get(de.source),c=idx.get(de.target);
            if(r!=null&&c!=null) mat[r][c]=true;
        }
        StringBuilder h=new StringBuilder();
        h.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Heatmap</title><style>");
        h.append("body{font-family:Arial;margin:10px;background:").append(dark?"#1a1a2e":"#fff").append(";color:").append(dark?"#ddd":"#333").append("}");
        h.append("table{border-collapse:collapse;table-layout:fixed;font-size:10px}");
        h.append("td{border:1px solid ").append(dark?"#444":"#ddd").append(";overflow:hidden}");
        h.append("td.label{width:110px;padding:1px 4px;text-align:right;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;font-size:10px}");
        h.append("th.colh{width:22px;height:90px;padding:0;position:relative}");
        h.append("th.colh span{position:absolute;bottom:2px;left:50%;transform-origin:bottom left;transform:rotate(-60deg);white-space:nowrap;font-size:8px;font-weight:normal}");
        h.append("td.cell{width:22px;height:22px;cursor:pointer}");
        h.append("td.cell:hover{outline:2px solid #ff0;z-index:1;position:relative}");
        h.append("#tip{position:fixed;background:").append(dark?"#333":"#2c3e50").append(";color:#fff;padding:4px 8px;border-radius:3px;font-size:11px;z-index:100;pointer-events:none;display:none}");
        h.append("</style></head><body>");
        h.append("<h3>Dependency Heatmap (").append(n).append(" classes)</h3>");
        h.append("<div id='tip'></div><div style='overflow:auto;max-height:85vh'><table>");
        // Header
        h.append("<colgroup>");
        h.append("<col style='width:110px'>");
        for(int c=0;c<n;c++) h.append("<col style='width:22px'>");
        h.append("</colgroup>");
        h.append("<tr><th></th>");
        for(int c=0;c<n;c++) h.append("<th class='colh'><span>").append(esc(cls.get(c).simpleName)).append("</span></th>");
        h.append("</tr>");
        // Data
        for(int r=0;r<n;r++){
            h.append("<tr>");
            h.append("<td class='label' title='").append(esc(cls.get(r).fullName)).append("'>").append(esc(cls.get(r).simpleName)).append("</td>");
            for(int c=0;c<n;c++){
                String cl=mat[r][c]?(dark?"#4CAF50":"#2E7D32"):(dark?"#2a2a3e":"#f0f0f0");
                String tt=esc(cls.get(r).simpleName+" -> "+cls.get(c).simpleName);
                h.append("<td class='cell' style='background:").append(cl).append("' title='").append(tt).append("'></td>");
            }
            h.append("</tr>");
        }
        h.append("</table></div>");
        h.append("<script>");
        h.append("var t=document.getElementById('tip');");
        h.append("document.querySelectorAll('.cell').forEach(function(td){td.onmouseenter=function(){t.textContent=this.title;t.style.display='block';};td.onmousemove=function(e){t.style.left=(e.clientX+12)+'px';t.style.top=(e.clientY-8)+'px';};td.onmouseleave=function(){t.style.display='none';};});");
        h.append("</script></body></html>");
        return h.toString();
    }

    // ==================== PUBLIC ====================
    public String generateCytoscapeGraph(String theme){return render(theme,"Class Dependency Graph (Layer)",layoutLayer(),false);}
    public String generateForceGraph(String theme){return render(theme,"Force-Directed Graph",layoutForce(),false);}
    public String generateHierarchicalGraph(String theme){return render(theme,"Package Hierarchy",layoutHierarchy(),false);}
    public String generateSunburstChart(String theme){return render(theme,"Sunburst Chart",layoutSunburst(),true);}
    public String generateHeatmapMatrix(String theme){return heatmap(theme);}

    public static String getPlaceholder(String theme){
        boolean dark="dark".equals(theme);
        return "<html><head><meta charset='UTF-8'><style>"+
            "body{display:flex;align-items:center;justify-content:center;height:100vh;margin:0;"+
            "background:"+(dark?"#1a1a2e":"#fafafa")+";font-family:Arial;color:"+(dark?"#ddd":"#333")+"}"+
            ".b{text-align:center}h2{font-size:22px}</style></head><body>"+
            "<div class='b'><h2>BugScope</h2><p>Open a project first</p></div></body></html>";
    }

    private static String esc(String s){return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
    private static String js(String s){return s==null?"''":"'"+s.replace("\\","\\\\").replace("'","\\'")+"'";}
    private static String fmt(double d){return String.format(Locale.US,"%.1f",d);}
}
