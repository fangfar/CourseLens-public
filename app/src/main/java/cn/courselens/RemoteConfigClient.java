package cn.courselens;

import android.content.*;
import android.annotation.SuppressLint;
import android.os.*;
import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.HttpsURLConnection;

// Process-owned work survives Activity recreation; only the current Activity receives results.
@SuppressLint("ApplySharedPref") // Every commit runs on the worker; cache replacement checks disk success.
final class RemoteConfigClient {
 static final String UPDATE="https://update.xvyan.cn/remote/config.json",PAGES="https://fangfar.github.io/CourseLens-public/remote/config.json",RAW="https://raw.githubusercontent.com/fangfar/CourseLens-public/main/remote/config.json";
 static final long REFRESH_MS=6*60*60*1000L,RETRY_MS=30*60*1000L,STALE_MS=7*24*60*60*1000L;
 interface Listener {void changed(String error,boolean manual);}
 interface Fetcher {String fetch()throws Exception;}
 private static RemoteConfigClient instance;
 static RemoteConfigClient get(Context context){if(instance==null)instance=new RemoteConfigClient(context.getApplicationContext(),"remoteConfig",RemoteConfigClient::fetch);return instance;}
 private final Context context;private final String preferenceName;private final Fetcher fetcher;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Handler main=new Handler(Looper.getMainLooper());
 private SharedPreferences prefs;private Listener listener;private boolean manualRequested;
 RemoteConfig config;boolean checking,loaded;long validatedAt,lastAttempt;final Set<String> readIds=new HashSet<>(),snoozedVersions=new HashSet<>(),sessionShown=new HashSet<>();
 RemoteConfigClient(Context context,String preferenceName,Fetcher fetcher){this.context=context.getApplicationContext();this.preferenceName=preferenceName;this.fetcher=fetcher;}
 void listen(Listener next){listener=next;}
 void unlisten(Listener previous){if(listener==previous)listener=null;}
 boolean fresh(long now){return config!=null&&validatedAt>0&&now>=validatedAt&&now-validatedAt<=STALE_MS;}
 static boolean due(long now,long success,long attempt,boolean manual){long interval=manual?3000:RETRY_MS;if(attempt>0&&now>=attempt&&now-attempt<interval)return false;return manual||success<=0||now<success||now-success>=REFRESH_MS;}
 void check(boolean manual){
  if(checking){manualRequested|=manual;return;}
  long now=System.currentTimeMillis();if(loaded&&!due(now,validatedAt,lastAttempt,manual)){if(manual&&listener!=null)listener.changed("请稍后再检查更新",true);return;}
  checking=true;manualRequested=manual;
  worker.execute(()->{
   if(prefs==null){prefs=context.getSharedPreferences(preferenceName,0);RemoteConfig cached=null;try{String json=prefs.getString("json","");if(!json.isEmpty())cached=RemoteConfig.parse(json);}catch(Exception ignored){}if(cached==null)prefs.edit().remove("validatedAt").commit();
    RemoteConfig previous=cached;long time=prefs.getLong("validatedAt",0),attempt=prefs.getLong("lastAttempt",0);Set<String> read=new HashSet<>(prefs.getStringSet("readIds",Collections.emptySet())),snoozed=new HashSet<>(prefs.getStringSet("snoozedVersions",Collections.emptySet()));
    main.post(()->{config=previous;validatedAt=time;lastAttempt=attempt;snoozedVersions.addAll(snoozed);readIds.addAll(read);loaded=true;if(listener!=null)listener.changed(null,false);});
   }
   long success=prefs.getLong("validatedAt",0),attempt=prefs.getLong("lastAttempt",0);boolean shouldFetch=due(now,success,attempt,manual);
   RemoteConfig result=null;String error=null;long completed=0;
   if(shouldFetch){prefs.edit().putLong("lastAttempt",now).commit();try{String json=fetcher.fetch();result=RemoteConfig.parse(json);completed=System.currentTimeMillis();if(!prefs.edit().putString("json",json).putLong("validatedAt",completed).commit())throw new IOException("无法保存更新信息");}catch(Exception e){result=null;completed=0;error=e instanceof SocketTimeoutException?"检查更新超时，请稍后重试":"检查更新失败，已保留上次信息";}}
   RemoteConfig next=result;String failure=error;long time=completed;
   main.post(()->{if(shouldFetch)lastAttempt=now;if(next!=null){config=next;validatedAt=time;}checking=false;boolean requested=manualRequested;manualRequested=false;if(requested&&!shouldFetch&&!manual){check(true);return;}if(listener!=null)listener.changed(!shouldFetch&&requested?"请稍后再检查更新":failure,requested);});
  });
 }
 void read(String id){readIds.add(id);Set<String> snapshot=new HashSet<>(readIds);worker.execute(()->{if(prefs!=null)prefs.edit().putStringSet("readIds",snapshot).commit();});}
 void snooze(long version){snoozedVersions.add(Long.toString(version));Set<String> snapshot=new HashSet<>(snoozedVersions);worker.execute(()->{if(prefs!=null)prefs.edit().putStringSet("snoozedVersions",snapshot).commit();});}
 private static final class InvalidResponse extends IOException {InvalidResponse(String message){super(message);}}
 private static String fetch()throws Exception {String json;try{json=request(UPDATE);}catch(InvalidResponse e){throw e;}catch(IOException e){try{json=request(PAGES);}catch(InvalidResponse invalid){throw invalid;}catch(IOException unavailable){json=request(RAW);}}return json;}
 static String request(String address)throws Exception {
  HttpsURLConnection connection=(HttpsURLConnection)new URL(address).openConnection();connection.setConnectTimeout(5000);connection.setReadTimeout(5000);connection.setInstanceFollowRedirects(false);connection.setRequestProperty("Accept","application/json");connection.setRequestProperty("User-Agent","CourseLens");
  try{if(connection.getResponseCode()!=200)throw new IOException("配置服务暂不可用");if(connection.getContentLengthLong()>RemoteConfig.MAX_BYTES)throw new InvalidResponse("远程配置过大");try(InputStream in=connection.getInputStream()){return readBody(in);}}finally{connection.disconnect();}
 }
 static String readBody(InputStream in)throws IOException {
  ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;long deadline=SystemClock.elapsedRealtime()+10000;while((count=in.read(buffer))!=-1){if(SystemClock.elapsedRealtime()>deadline)throw new SocketTimeoutException("配置读取超时");if(out.size()+count>RemoteConfig.MAX_BYTES)throw new InvalidResponse("远程配置过大");out.write(buffer,0,count);}
  try{return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(out.toByteArray())).toString();}catch(CharacterCodingException e){throw new InvalidResponse("远程配置编码无效");}
 }
}
