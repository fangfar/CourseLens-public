package cn.courselens;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import org.json.*;
import java.util.*;
import java.util.regex.*;

public final class SchoolParser {
 public static String scriptJson(String html,String variable)throws Exception {
  Matcher m=Pattern.compile("(?:var|let|const)\\s+"+Pattern.quote(variable)+"\\s*=\\s*(\\{[^\\n]+?\\})\\s*;").matcher(html);
  if(!m.find())throw new IllegalArgumentException("学校登录接口已变化，未找到认证配置。");return m.group(1);
 }
 public static String htmlCourses(String html) {
  Document doc=Jsoup.parse(html);Set<String> out=new LinkedHashSet<>();
  for(Element table:doc.select("table")) {
   Elements rows=table.select("tr");int h=-1;
   for(int i=0;i<rows.size();i++)if(rows.get(i).text().contains("星期一")&&rows.get(i).text().contains("星期日")){h=i;break;}
   if(h<0)continue;boolean[][] used=new boolean[rows.size()+100][64];
   for(int r=h;r<rows.size();r++){int col=0;for(Element cell:rows.get(r).children()){
    if(!cell.tagName().equals("td")&&!cell.tagName().equals("th"))continue;
    while(col<64&&used[r][col])col++;int day=col,cs=span(cell,"colspan"),rs=span(cell,"rowspan");
    for(int y=r;y<Math.min(used.length,r+rs);y++)for(int x=col;x<Math.min(64,col+cs);x++)used[y][x]=true;col+=cs;
    if(r==h||day<1||day>7)continue;
    Element clone=cell.clone();for(Element block:clone.select("br,div,p"))block.appendText("\n");
    String text=clone.wholeText().replace('\u00a0',' ');
    Matcher m=Pattern.compile("[（(]([\\d\\s,，、~～\\-单双]+)周[）)]\\s*[（(](\\d+)\\s*[-~～]\\s*(\\d+)节[）)]\\s*([^\\n☭]*)").matcher(text);
    while(m.find()) {
     String prefix=text.substring(0,m.start());Matcher code=Pattern.compile("([^\\n]*?)(\\d{6,}\\.\\d+)").matcher(prefix);String name="";while(code.find()){name=code.group(1).trim();if(name.isEmpty()){String[] lines=prefix.substring(0,code.start()).trim().split("\\n");name=lines[lines.length-1].trim();}}
     if(name.isEmpty())continue;String w=weeks(m.group(1));if(w.isEmpty())continue;
     String line=day+"|"+m.group(2)+"|"+m.group(3)+"|"+clean(name+(text.contains("免听")?"〔免听〕":""))+"|"+clean(m.group(4))+"|"+w;
     Course.parse(line);out.add(line);
    }
   }}
  }
  return String.join("\n",out);
 }
 private static int span(Element e,String key){try{return Math.min(50,Math.max(1,Integer.parseInt(e.attr(key))));}catch(Exception x){return 1;}}
 public static String clean(String s){return Jsoup.parse(s.replaceAll("&nbsp(?!;)","&nbsp;")).text().replace('\u00a0',' ').replace('|','／').replaceAll("\\s+"," ").trim();}
 public static String weeks(String s){boolean odd=s.contains("单"),even=s.contains("双");s=s.replaceAll("\\s|单|双","").replace('~','-').replace('～','-').replace('，',',').replace('、',',');if(!odd&&!even)return s;List<String> out=new ArrayList<>();for(String p:s.split(",")){String[] r=p.split("-");int a=Integer.parseInt(r[0]),b=r.length==2?Integer.parseInt(r[1]):a;if(a<1||b>30||b<a)throw new IllegalArgumentException("学校周次超出支持范围");for(int w=a;w<=b;w++)if(w%2==(odd?1:0))out.add(""+w);}return String.join(",",out);}
 public static String jsonCourses(String json,String lessonsJson)throws Exception {
  Object tree=new JSONTokener(json).nextValue();JSONObject summary=new JSONObject(lessonsJson);Map<Integer,String> names=new HashMap<>();JSONArray lessons=summary.optJSONArray("lessons");if(lessons!=null)for(int i=0;i<lessons.length();i++){JSONObject l=lessons.getJSONObject(i),c=l.optJSONObject("course");if(c!=null)names.put(l.optInt("id"),c.optString("nameZh"));}
  Set<String> rows=new LinkedHashSet<>();walk(tree,"",names,rows);String result=String.join("\n",rows);Course.parse(result);return result;
 }
 public static String scheduleNotes(String json)throws Exception {Set<String> notes=new LinkedHashSet<>();collectNotes(new JSONTokener(json).nextValue(),notes);return String.join("\n",notes);}
 private static void collectNotes(Object node,Set<String> notes)throws Exception {if(node instanceof JSONArray){JSONArray a=(JSONArray)node;for(int i=0;i<a.length();i++)collectNotes(a.get(i),notes);}else if(node instanceof JSONObject){JSONObject o=(JSONObject)node;JSONArray p=o.optJSONArray("practiceWeekScheduleTexts");if(p!=null)for(int i=0;i<p.length();i++)notes.add(Jsoup.parse(p.getString(i)).text());Iterator<String> keys=o.keys();while(keys.hasNext()){Object value=o.get(keys.next());if(value instanceof JSONObject||value instanceof JSONArray)collectNotes(value,notes);}}}
 private static void walk(Object node,String inherited,Map<Integer,String> names,Set<String> rows)throws Exception {
  if(node instanceof JSONArray){JSONArray a=(JSONArray)node;for(int i=0;i<a.length();i++)walk(a.get(i),inherited,names,rows);return;}
  if(!(node instanceof JSONObject))return;JSONObject o=(JSONObject)node;String name=o.optString("courseName",o.optString("lessonName",inherited));JSONObject c=o.optJSONObject("course");if(c!=null)name=c.optString("nameZh",name);if(name.isEmpty())name=names.getOrDefault(o.optInt("lessonId",o.optInt("id")),"");
  if((o.has("dayOfWeek")||o.has("weekday"))&&o.has("startUnit")&&o.has("endUnit")&&!name.isEmpty()){
   String w=o.optString("weekIndex","");JSONArray wa=o.optJSONArray("weekIndexes");if(wa==null)wa=o.optJSONArray("weekIndices");if(wa!=null){List<String> ws=new ArrayList<>();for(int i=0;i<wa.length();i++)ws.add(""+wa.getInt(i));w=String.join(",",ws);}if(w.isEmpty())w=weeks(o.optString("weeksStr",""));String room=o.optString("roomName","");if(o.opt("room") instanceof String)room=o.getString("room");String teacher="";JSONArray teachers=o.optJSONArray("teachers");if(teachers!=null){List<String> namesList=new ArrayList<>();for(int i=0;i<teachers.length();i++)namesList.add(teachers.getString(i));teacher=String.join("/",namesList);}JSONObject ro=o.optJSONObject("room");if(ro!=null)room=ro.optString("nameZh",room);if(!w.isEmpty()){String line=o.optInt("weekday",o.optInt("dayOfWeek"))+"|"+o.getInt("startUnit")+"|"+o.getInt("endUnit")+"|"+clean(name)+"|"+clean(room)+"|"+w+(teacher.isEmpty()?"":"|"+clean(teacher));Course.parse(line);rows.add(line);}
  }
  Iterator<String> keys=o.keys();while(keys.hasNext()){Object value=o.get(keys.next());if(value instanceof JSONObject||value instanceof JSONArray)walk(value,name,names,rows);}
 }
}
