package cn.courselens;

import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
  JSONArray items=new JSONArray(),warnings=new JSONArray();for(ChaoxingClient.Work work:result.items)items.put(new JSONObject().put("course",work.course).put("title",work.title).put("status",work.status).put("deadline",work.deadline));for(String warning:result.warnings)warnings.put(warning);
  return new JSONObject().put("schema",1).put("total",result.total).put("checked",result.checked).put("items",items).put("warnings",warnings).toString();
 }
 private static String text(JSONObject value,String key)throws JSONException {String text=value.getString(key);if(text.length()>8192)throw new JSONException("Cached field too large");return text;}
 static ChaoxingClient.Result decode(String json)throws JSONException {
  JSONObject value=new JSONObject(json);if(value.getInt("schema")!=1)throw new JSONException("Unsupported cache schema");ChaoxingClient.Result result=new ChaoxingClient.Result();result.total=value.getInt("total");result.checked=value.getInt("checked");if(result.total<0||result.checked<0||result.checked>result.total)throw new JSONException("Invalid cached counts");
  JSONArray items=value.getJSONArray("items"),warnings=value.getJSONArray("warnings");if(items.length()>20000||warnings.length()>20000)throw new JSONException("Cache entries exceed limit");
  for(int i=0;i<items.length();i++){JSONObject item=items.getJSONObject(i);ChaoxingClient.Work work=new ChaoxingClient.Work(text(item,"course"),text(item,"title"),text(item,"status"),"");work.deadline=text(item,"deadline");result.items.add(work);}for(int i=0;i<warnings.length();i++){String warning=warnings.getString(i);if(warning.length()>16384)throw new JSONException("Cached warning too large");result.warnings.add(warning);}return result;
 }
 ChaoxingClient.Result load()throws Exception {
  if(file.getBaseFile().length()>MAX_BYTES)throw new IOException("Offline data too large");
  try{return decode(new String(file.readFully(),StandardCharsets.UTF_8));}catch(FileNotFoundException e){return null;}
 }
 void save(ChaoxingClient.Result result)throws Exception {
  byte[] data=encode(result).getBytes(StandardCharsets.UTF_8);if(data.length>MAX_BYTES)throw new IOException("Offline data too large");FileOutputStream out=null;
  try{out=file.startWrite();out.write(data);file.finishWrite(out);}catch(Exception e){if(out!=null)file.failWrite(out);throw e;}
 }
}
