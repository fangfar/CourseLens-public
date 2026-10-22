package cn.courselens;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Sends only waybill and carrier to a user-configured HTTPS proxy; no vendor keys in APK. */
final class ProxyTrackingProvider implements TrackingProvider {
 private final String endpoint;
 ProxyTrackingProvider(String endpoint){this.endpoint=validateEndpoint(endpoint);}
 static String validateEndpoint(String value){
  try{URI uri=new URI(value.trim());if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getFragment()!=null||uri.getQuery()!=null||(uri.getPort()!=-1&&uri.getPort()!=443)||value.length()>2048)throw new IllegalArgumentException();return uri.toASCIIString();}
  catch(URISyntaxException|IllegalArgumentException e){throw new IllegalArgumentException("请输入 HTTPS 代理地址（443 端口，无账号、查询参数或片段）");}
 }
 @Override public Result query(String trackingNumber,String carrier){
  if(!trackingNumber.matches("[A-Z0-9]{6,40}")||carrier.length()>40)return Result.failure(Error.INVALID_RESPONSE,0);
  HttpURLConnection connection=null;
  try{
   connection=(HttpURLConnection)new URL(endpoint).openConnection();connection.setConnectTimeout(10000);connection.setReadTimeout(15000);connection.setInstanceFollowRedirects(false);connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setUseCaches(false);connection.setRequestProperty("Content-Type","application/json; charset=utf-8");connection.setRequestProperty("Accept","application/json");
   byte[] body=new JSONObject().put("schema",1).put("trackingNumber",trackingNumber).put("carrier",carrier).toString().getBytes(StandardCharsets.UTF_8);connection.setFixedLengthStreamingMode(body.length);try(OutputStream out=connection.getOutputStream()){out.write(body);}
   int status=connection.getResponseCode();long retry=retryAfter(connection.getHeaderField("Retry-After"));
   if(status==429)return Result.failure(Error.RATE_LIMIT,retry);
   if(status==404||status==204)return Result.failure(Error.EMPTY,0);
   if(status!=200)return Result.failure(Error.UNAVAILABLE,retry);
   try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
    byte[] bytes=new byte[4096];int count;while((count=in.read(bytes))!=-1){if(out.size()+count>65536)return Result.failure(Error.INVALID_RESPONSE,0);out.write(bytes,0,count);}return parse(new String(out.toByteArray(),StandardCharsets.UTF_8));
   }
  }catch(IOException e){return Result.failure(Error.NETWORK,60000);}catch(JSONException|RuntimeException e){return Result.failure(Error.INVALID_RESPONSE,0);}finally{if(connection!=null)connection.disconnect();}
 }
 static long retryAfter(String value){try{return Math.max(60000,Math.min(86400000,Math.multiplyExact(Long.parseLong(value),1000)));}catch(RuntimeException e){return 900000;}}
 static Result parse(String body){
  try{
   if(body.length()>65536)throw new JSONException("size");JSONObject object=new JSONObject(body);
   if(!(object.get("schema") instanceof Integer)||object.getInt("schema")!=1||!(object.get("state") instanceof String))throw new JSONException("schema");
   String state=object.getString("state");if(!Arrays.asList("UNKNOWN","IN_TRANSIT","READY","DELIVERED").contains(state))throw new JSONException("state");
   JSONArray array=object.getJSONArray("events");if(array.length()>100)throw new JSONException("events");List<Event> events=new ArrayList<>();
   for(int i=0;i<array.length();i++){JSONObject row=array.getJSONObject(i);if(!(row.get("time") instanceof String)||!(row.get("description") instanceof String))throw new JSONException("type");String time=row.getString("time"),description=row.getString("description");if(time.length()>80||description.length()>500)throw new JSONException("length");events.add(new Event(time,description));}
   return events.isEmpty()&&state.equals("UNKNOWN")?Result.failure(Error.EMPTY,0):new Result(Error.NONE,state,events,0);
  }catch(JSONException|RuntimeException e){return Result.failure(Error.INVALID_RESPONSE,0);}
 }
}
