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
 private static JSONArray encodeWorks(List<ChaoxingClient.Work> works)throws JSONException {
  if(works.size()>20000)throw new JSONException("Cache entries exceed limit");JSONArray items=new JSONArray();for(ChaoxingClient.Work work:works)items.put(new JSONObject().put("course",work.course).put("title",work.title).put("status",work.status).put("deadline",work.deadline).put("courseKey",work.courseKey).put("workKey",work.workKey).put("deletedAt",work.deletedAt).put("edited",work.edited).put("originalTitle",work.originalTitle));return items;
 }
 static String encode(ChaoxingClient.Result result)throws JSONException {
  if(result.warnings.size()>20000||result.courses.size()>20000||result.disabledCourses.size()>20000)throw new JSONException("Cache entries exceed limit");JSONArray warnings=new JSONArray(),courses=new JSONArray(),disabled=new JSONArray();for(String warning:result.warnings)warnings.put(warning);for(String key:result.disabledCourses)disabled.put(key);
  for(ChaoxingClient.Subject course:result.courses)courses.put(new JSONObject().put("name",course.name).put("id",course.id).put("clazz",course.clazz));
  return new JSONObject().put("schema",1).put("total",result.total).put("checked",result.checked).put("items",encodeWorks(result.items)).put("warnings",warnings).put("courses",courses).put("disabledCourses",disabled).put("deleted",encodeWorks(result.deleted)).put("intervalMinutes",result.intervalMinutes).put("lastAttempt",result.lastAttempt).toString();
 }
 private static String text(JSONObject value,String key)throws JSONException {String text=value.getString(key);if(text.length()>8192)throw new JSONException("Cached field too large");return text;}
 private static JSONArray array(JSONObject value,String key)throws JSONException {JSONArray items=value.has(key)?value.getJSONArray(key):new JSONArray();if(items.length()>20000)throw new JSONException("Cache entries exceed limit");return items;}
 private static void decodeWorks(JSONArray items,List<ChaoxingClient.Work> result)throws JSONException {
  for(int i=0;i<items.length();i++){JSONObject item=items.getJSONObject(i);ChaoxingClient.Work work=new ChaoxingClient.Work(text(item,"course"),text(item,"title"),text(item,"status"),"");work.deadline=text(item,"deadline");work.courseKey=item.has("courseKey")?text(item,"courseKey"):"";work.workKey=item.has("workKey")?text(item,"workKey"):"";work.edited=item.optBoolean("edited",false);work.originalTitle=item.has("originalTitle")?text(item,"originalTitle"):"";work.deletedAt=item.has("deletedAt")?item.getLong("deletedAt"):0;if(work.deletedAt<0)throw new JSONException("Invalid deletion time");result.add(work);}
 }
 static ChaoxingClient.Result decode(String json)throws JSONException {
  JSONObject value=new JSONObject(json);if(value.getInt("schema")!=1)throw new JSONException("Unsupported cache schema");ChaoxingClient.Result result=new ChaoxingClient.Result();result.total=value.getInt("total");result.checked=value.getInt("checked");if(result.total<0||result.checked<0||result.checked>result.total)throw new JSONException("Invalid cached counts");
  if(!value.has("items")||!value.has("warnings"))throw new JSONException("Missing cache data");decodeWorks(array(value,"items"),result.items);decodeWorks(array(value,"deleted"),result.deleted);
  JSONArray warnings=array(value,"warnings"),courses=array(value,"courses"),disabled=array(value,"disabledCourses");
  for(int i=0;i<warnings.length();i++){String warning=warnings.getString(i);if(warning.length()>16384)throw new JSONException("Cached warning too large");result.warnings.add(warning);}
  for(int i=0;i<courses.length();i++){JSONObject course=courses.getJSONObject(i);String id=text(course,"id"),clazz=text(course,"clazz");if(!id.matches("[0-9]{1,256}")||!clazz.matches("[0-9]{1,256}"))throw new JSONException("Invalid course identity");result.courses.add(new ChaoxingClient.Subject(text(course,"name"),id,clazz,""));}
  for(int i=0;i<disabled.length();i++){String key=disabled.getString(i);if(!key.matches("[0-9]{1,256}:[0-9]{1,256}"))throw new JSONException("Invalid disabled course");result.disabledCourses.add(key);}
  result.intervalMinutes=value.has("intervalMinutes")?value.getInt("intervalMinutes"):60;result.lastAttempt=value.has("lastAttempt")?value.getLong("lastAttempt"):0;if(result.intervalMinutes<0||result.intervalMinutes>TaskRefresh.MAX_MINUTES||result.lastAttempt<0)throw new JSONException("Invalid refresh settings");return result;
 }
 ChaoxingClient.Result load()throws Exception {
  if(file.getBaseFile().length()>MAX_BYTES)throw new IOException("Offline data too large");
  try{return decode(new String(file.readFully(),StandardCharsets.UTF_8));}catch(FileNotFoundException e){return null;}
 }
 void save(ChaoxingClient.Result result)throws Exception {
  byte[] data=encode(result).getBytes(StandardCharsets.UTF_8);if(data.length>MAX_BYTES)throw new IOException("Offline data too large");FileOutputStream out=null;
  try{out=file.startWrite();out.write(data);file.finishWrite(out);}catch(Exception e){if(out!=null)file.failWrite(out);throw e;}
 }
 private static ChaoxingClient.Work copy(ChaoxingClient.Work value){ChaoxingClient.Work copy=new ChaoxingClient.Work(value.course,value.title,value.status,value.url);copy.deadline=value.deadline;copy.courseKey=value.courseKey;copy.workKey=value.workKey;copy.deletedAt=value.deletedAt;copy.edited=value.edited;copy.originalTitle=value.originalTitle;return copy;}
 private static ChaoxingClient.Work previous(ChaoxingClient.Work item,List<ChaoxingClient.Work> old,List<ChaoxingClient.Work> fresh){
  ChaoxingClient.Work match=null;
  if(!item.workKey.isEmpty()){for(ChaoxingClient.Work candidate:old)if(item.workKey.equals(candidate.workKey)){if(match!=null)return null;match=candidate;}if(match!=null)return match;}
  int oldCount=0,newCount=0;for(ChaoxingClient.Work candidate:old)if(item.title.equals(candidate.originalTitle.isEmpty()?candidate.title:candidate.originalTitle)){oldCount++;if(item.workKey.isEmpty()||candidate.workKey.isEmpty())match=candidate;}for(ChaoxingClient.Work candidate:fresh)if(item.title.equals(candidate.title))newCount++;
  return oldCount==1&&newCount==1?match:null;
 }
 static ChaoxingClient.Result copy(ChaoxingClient.Result source){
  ChaoxingClient.Result result=new ChaoxingClient.Result();if(source==null)return result;result.total=source.total;result.checked=source.checked;result.courses.addAll(source.courses);result.warnings.addAll(source.warnings);result.disabledCourses.addAll(source.disabledCourses);result.intervalMinutes=source.intervalMinutes;result.lastAttempt=source.lastAttempt;for(ChaoxingClient.Work item:source.items)result.items.add(copy(item));for(ChaoxingClient.Work item:source.deleted)result.deleted.add(copy(item));return result;
 }
 private static String courseKey(ChaoxingClient.Work work,List<ChaoxingClient.Subject> courses){
  if(work.courseKey.isEmpty()){ChaoxingClient.Subject found=null;int matches=0;for(ChaoxingClient.Subject course:courses)if(course.name.equals(work.course)){found=course;matches++;}if(matches==1)work.courseKey=found.key();}return work.courseKey.isEmpty()?"legacy:"+work.course:work.courseKey;
 }
 static boolean visible(ChaoxingClient.Result result,ChaoxingClient.Work work){
  if(result.disabledCourses.contains(work.courseKey))return false;
  if(work.courseKey.isEmpty()){int matches=0;String key="";for(ChaoxingClient.Subject course:result.courses)if(course.name.equals(work.course)){matches++;key=course.key();}if(matches==1&&result.disabledCourses.contains(key))return false;}return true;
 }
 static ChaoxingClient.Result merge(ChaoxingClient.Result old,ChaoxingClient.Result fresh){
  ChaoxingClient.Result result=copy(old);result.items.clear();result.total=fresh.total;result.checked=fresh.checked;result.warnings.clear();result.warnings.addAll(fresh.warnings);
  Map<String,ChaoxingClient.Subject> courses=new LinkedHashMap<>();for(ChaoxingClient.Subject course:result.courses)courses.put(course.key(),course);for(ChaoxingClient.Subject course:fresh.courses)courses.put(course.key(),new ChaoxingClient.Subject(course.name,course.id,course.clazz,""));result.courses.clear();result.courses.addAll(courses.values());
  Map<String,List<ChaoxingClient.Work>> before=new LinkedHashMap<>(),after=new LinkedHashMap<>(),deleted=new LinkedHashMap<>();
  if(old!=null)for(ChaoxingClient.Work value:old.items){ChaoxingClient.Work work=copy(value);String key=courseKey(work,result.courses);before.computeIfAbsent(key,k->new ArrayList<>()).add(work);}
  for(ChaoxingClient.Work work:result.deleted)deleted.computeIfAbsent(courseKey(work,result.courses),k->new ArrayList<>()).add(work);
  for(ChaoxingClient.Work work:fresh.items)after.computeIfAbsent(work.courseKey,k->new ArrayList<>()).add(work);
  Set<String> keys=new LinkedHashSet<>(before.keySet());keys.addAll(after.keySet());int retained=0;
  for(String key:keys){List<ChaoxingClient.Work> previous=before.getOrDefault(key,Collections.emptyList()),updated=after.getOrDefault(key,Collections.emptyList());
   Map<ChaoxingClient.Work,ChaoxingClient.Work> matches=new IdentityHashMap<>();Set<ChaoxingClient.Work> replaced=Collections.newSetFromMap(new IdentityHashMap<>());for(ChaoxingClient.Work work:updated){ChaoxingClient.Work earlier=previous(work,previous,updated);matches.put(work,earlier);if(earlier!=null)replaced.add(earlier);}
   for(ChaoxingClient.Work work:previous)if(!replaced.contains(work)&&(work.edited||!Boolean.TRUE.equals(fresh.readCourses.get(key))))result.items.add(work);if(!Boolean.TRUE.equals(fresh.readCourses.get(key))&&!previous.isEmpty()&&!fresh.requested.contains(key)&&!result.disabledCourses.contains(key))retained++;
   for(ChaoxingClient.Work value:updated){ChaoxingClient.Work work=copy(value),earlier=matches.get(value);if(earlier!=null&&earlier.edited){work=copy(earlier);work.courseKey=value.courseKey;work.workKey=value.workKey;}else if(work.deadline.equals("截止时间未知")&&earlier!=null)work.deadline=earlier.deadline;result.items.add(work);}
  }
  Map<String,List<ChaoxingClient.Work>> combined=new LinkedHashMap<>();for(ChaoxingClient.Work work:result.items)combined.computeIfAbsent(courseKey(work,result.courses),k->new ArrayList<>()).add(work);
  for(Map.Entry<String,List<ChaoxingClient.Work>> group:combined.entrySet())for(ChaoxingClient.Work work:group.getValue()){
   ChaoxingClient.Work removed=previous(work,deleted.getOrDefault(group.getKey(),Collections.emptyList()),group.getValue());if(removed!=null){if(removed.workKey.isEmpty())removed.workKey=work.workKey;result.items.remove(work);}
  }
  if(retained>0)result.warnings.add("另有 "+retained+" 门课程本次未刷新，继续显示本机作业。");return result;
 }
}
