package cn.courselens;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.json.*;

final class RemoteConfig {
 static final int MAX_BYTES=64*1024;
 static final String REPOSITORY="https://github.com/fangfar/Morrow";
 final List<Announcement> announcements;final long versionCode;final boolean forcePrompt;final String versionName,changelog,promptTitle,promptMessage,apkUrl,diskUrl,repositoryUrl;
 static final class Announcement {
  final String id,title,body,publishedAt;final boolean enabled;
  Announcement(JSONObject value)throws Exception{id=string(value,"id",120,false);title=string(value,"title",160,false);body=string(value,"body",8000,false);publishedAt=string(value,"publishedAt",40,true);if(!publishedAt.isEmpty())Instant.parse(publishedAt);Object flag=value.get("enabled");if(!(flag instanceof Boolean))throw new IOException("公告启用状态无效");enabled=(Boolean)flag;}
 }
 private RemoteConfig(JSONObject value)throws Exception {
  if(integer(value,"schemaVersion")!=1)throw new IOException("暂不支持此配置协议");
  JSONArray items=value.getJSONArray("announcements");if(items.length()>20)throw new IOException("公告数量过多");List<Announcement> list=new ArrayList<>();Set<String> ids=new HashSet<>();
  for(int i=0;i<items.length();i++){Announcement item=new Announcement(items.getJSONObject(i));if(!ids.add(item.id))throw new IOException("公告 ID 重复");list.add(item);}announcements=Collections.unmodifiableList(list);
  JSONObject android=value.getJSONObject("android");versionCode=integer(android,"versionCode");if(versionCode<1)throw new IOException("版本号无效");versionName=string(android,"versionName",80,false);changelog=string(android,"changelog",12000,true);
  Object force=android.has("forcePrompt")?android.get("forcePrompt"):Boolean.FALSE;if(!(force instanceof Boolean))throw new IOException("强制更新提示状态无效");forcePrompt=(Boolean)force;promptTitle=android.has("promptTitle")?string(android,"promptTitle",160,true):"";promptMessage=android.has("promptMessage")?string(android,"promptMessage",12000,true):"";
  apkUrl=link(android,"apkUrl");diskUrl=link(android,"diskUrl");repositoryUrl=link(value,"repositoryUrl");if(!repositoryUrl.isEmpty()&&!REPOSITORY.equals(repositoryUrl))throw new IOException("仓库入口必须为公开发布仓库");
 }
 static RemoteConfig parse(String text)throws Exception {
  if(text==null||text.getBytes(StandardCharsets.UTF_8).length>MAX_BYTES)throw new IOException("远程配置过大");
  JSONTokener reader=new JSONTokener(text);Object value=reader.nextValue();if(!(value instanceof JSONObject)||reader.nextClean()!=0)throw new IOException("远程配置格式无效");return new RemoteConfig((JSONObject)value);
 }
 private static long integer(JSONObject object,String key)throws Exception {Object value=object.get(key);if(!(value instanceof Integer)&&!(value instanceof Long))throw new IOException("配置整数无效："+key);return ((Number)value).longValue();}
 private static String string(JSONObject object,String key,int max,boolean empty)throws Exception {Object value=object.get(key);if(!(value instanceof String))throw new IOException("配置文本无效："+key);String text=((String)value).trim();if(text.length()>max||(!empty&&text.isEmpty()))throw new IOException("配置文本长度无效："+key);return text;}
 private static String link(JSONObject object,String key)throws Exception {String value=string(object,key,2048,true);if(!value.isEmpty()&&!validLink(value))throw new IOException("下载或仓库链接无效");return value;}
 static boolean validLink(String value){try{URI uri=new URI(value);return "https".equalsIgnoreCase(uri.getScheme())&&uri.getHost()!=null&&uri.getRawUserInfo()==null&&(uri.getPort()==-1||uri.getPort()==443);}catch(Exception e){return false;}}
 String downloadUrl(){return apkUrl.isEmpty()?diskUrl:apkUrl;}
 boolean newerThan(long installed){return versionCode>installed;}
}
