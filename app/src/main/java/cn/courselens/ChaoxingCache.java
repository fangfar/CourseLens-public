package cn.courselens;

import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;

/** Account-scoped offline display data; never stores passwords, cookies or signed URLs. */
final class ChaoxingCache {
 private static final int MAX_BYTES=10*1024*1024;
 private final AtomicFile file;
 ChaoxingCache(Context context,String account)throws Exception {
  byte[] digest=MessageDigest.getInstance("SHA-256").digest(account.trim().getBytes(StandardCharsets.UTF_8));StringBuilder key=new StringBuilder();for(byte value:digest)key.append(String.format(java.util.Locale.ROOT,"%02x",value&255));
  file=new AtomicFile(new File(context.getFilesDir(),"chaoxing-work-"+key+".json"));
 }
 static String encode(ChaoxingClient.Result result)throws JSONException {
  JSONArray items=new JSONArray(),warnings=new JSONArray();for(ChaoxingClient.Work work:result.items)items.put(new JSONObject().put("course",work.course).put("title",work.title).put("status",work.status).put("deadline",work.deadline).put("courseKey",work.courseKey).put("workKey",work.workKey));for(String warning:result.warnings)warnings.put(warning);
  return new JSONObject().put("schema",1).put("total",result.total).put("checked",result.checked).put("items",items).put("warnings",warnings).toString();
 }
 private static String text(JSONObject value,String key)throws JSONException {String text=value.getString(key);if(text.length()>8192)throw new JSONException("Cached field too large");return text;}
 static ChaoxingClient.Result decode(String json)throws JSONException {
  JSONObject value=new JSONObject(json);if(value.getInt("schema")!=1)throw new JSONException("Unsupported cache schema");ChaoxingClient.Result result=new ChaoxingClient.Result();result.total=value.getInt("total");result.checked=value.getInt("checked");if(result.total<0||result.checked<0||result.checked>result.total)throw new JSONException("Invalid cached counts");
  JSONArray items=value.getJSONArray("items"),warnings=value.getJSONArray("warnings");if(items.length()>20000||warnings.length()>20000)throw new JSONException("Cache entries exceed limit");
  for(int i=0;i<items.length();i++){JSONObject item=items.getJSONObject(i);ChaoxingClient.Work work=new ChaoxingClient.Work(text(item,"course"),text(item,"title"),text(item,"status"),"");work.deadline=text(item,"deadline");work.courseKey=item.has("courseKey")?text(item,"courseKey"):"";work.workKey=item.has("workKey")?text(item,"workKey"):"";result.items.add(work);}for(int i=0;i<warnings.length();i++){String warning=warnings.getString(i);if(warning.length()>16384)throw new JSONException("Cached warning too large");result.warnings.add(warning);}return result;
 }
 ChaoxingClient.Result load()throws Exception {
  if(file.getBaseFile().length()>MAX_BYTES)throw new IOException("Offline data too large");
  try{return decode(new String(file.readFully(),StandardCharsets.UTF_8));}catch(FileNotFoundException e){return null;}
 }
 void save(ChaoxingClient.Result result)throws Exception {
  byte[] data=encode(result).getBytes(StandardCharsets.UTF_8);if(data.length>MAX_BYTES)throw new IOException("Offline data too large");FileOutputStream out=null;
  try{out=file.startWrite();out.write(data);file.finishWrite(out);}catch(Exception e){if(out!=null)file.failWrite(out);throw e;}
 }
 private static ChaoxingClient.Work copy(ChaoxingClient.Work value){ChaoxingClient.Work copy=new ChaoxingClient.Work(value.course,value.title,value.status,value.url);copy.deadline=value.deadline;copy.courseKey=value.courseKey;copy.workKey=value.workKey;return copy;}
 private static ChaoxingClient.Work previous(ChaoxingClient.Work item,List<ChaoxingClient.Work> old,List<ChaoxingClient.Work> fresh){
  ChaoxingClient.Work match=null;
  if(!item.workKey.isEmpty()){for(ChaoxingClient.Work candidate:old)if(item.workKey.equals(candidate.workKey)){if(match!=null)return null;match=candidate;}if(match!=null)return match;}
  int oldCount=0,newCount=0;for(ChaoxingClient.Work candidate:old)if(item.title.equals(candidate.title)){oldCount++;if(item.workKey.isEmpty()||candidate.workKey.isEmpty())match=candidate;}for(ChaoxingClient.Work candidate:fresh)if(item.title.equals(candidate.title))newCount++;
  return oldCount==1&&newCount==1?match:null;
 }
 static ChaoxingClient.Result merge(ChaoxingClient.Result old,ChaoxingClient.Result fresh){
  ChaoxingClient.Result result=new ChaoxingClient.Result();result.total=fresh.total;result.checked=fresh.checked;result.warnings.addAll(fresh.warnings);
  Map<String,List<ChaoxingClient.Work>> before=new LinkedHashMap<>(),after=new LinkedHashMap<>();
  if(old!=null)for(ChaoxingClient.Work value:old.items){ChaoxingClient.Work work=copy(value);if(work.courseKey.isEmpty()){ChaoxingClient.Subject found=null;int matches=0;for(ChaoxingClient.Subject course:fresh.courses)if(course.name.equals(work.course)){found=course;matches++;}if(matches==1)work.courseKey=found.key();}String key=work.courseKey.isEmpty()?"legacy:"+work.course:work.courseKey;before.computeIfAbsent(key,k->new ArrayList<>()).add(work);}
  for(ChaoxingClient.Work work:fresh.items)after.computeIfAbsent(work.courseKey,k->new ArrayList<>()).add(work);
  Set<String> keys=new LinkedHashSet<>(before.keySet());keys.addAll(after.keySet());int retained=0;
  for(String key:keys){List<ChaoxingClient.Work> previous=before.getOrDefault(key,Collections.emptyList()),updated=after.getOrDefault(key,Collections.emptyList());
   Map<ChaoxingClient.Work,ChaoxingClient.Work> matches=new IdentityHashMap<>();Set<ChaoxingClient.Work> replaced=Collections.newSetFromMap(new IdentityHashMap<>());for(ChaoxingClient.Work work:updated){ChaoxingClient.Work earlier=previous(work,previous,updated);matches.put(work,earlier);if(earlier!=null)replaced.add(earlier);}
   if(!Boolean.TRUE.equals(fresh.readCourses.get(key))){for(ChaoxingClient.Work work:previous)if(!replaced.contains(work))result.items.add(work);if(!previous.isEmpty()&&!fresh.requested.contains(key))retained++;}
   for(ChaoxingClient.Work value:updated){ChaoxingClient.Work work=copy(value),earlier=matches.get(value);if(work.deadline.equals("截止时间未知")&&earlier!=null)work.deadline=earlier.deadline;result.items.add(work);}
  }
  if(retained>0)result.warnings.add("另有 "+retained+" 门课程本次未刷新，继续显示本机作业。");return result;
 }
}
