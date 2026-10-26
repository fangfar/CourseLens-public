package cn.courselens;

import java.io.IOException;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RemoteConfigTest {
 private String config(String repository)throws Exception {
  return new JSONObject().put("schemaVersion",1).put("announcements",new JSONArray()).put("repositoryUrl",repository)
   .put("android",new JSONObject().put("versionCode",16).put("versionName","2.7.4").put("changelog","").put("apkUrl","https://github.com/fangfar/Morrow/releases/download/v2.7.4/CourseLens-2.7.4-vc16.apk").put("diskUrl","")).toString();
 }
 @Test public void currentRepositoryAccepted()throws Exception {
  RemoteConfig parsed=RemoteConfig.parse(config("https://github.com/fangfar/Morrow"));
  assertEquals("https://github.com/fangfar/Morrow",parsed.repositoryUrl);
  assertTrue(parsed.downloadUrl().startsWith("https://github.com/fangfar/Morrow/releases/download/"));
 }
 @Test public void otherRepositoriesRejected()throws Exception {
  for(String url:new String[]{"https://github.com/fangfar/Other","https://github.com/fangfar/Morrow-fake","https://github.com.example.org/fangfar/Morrow"}){
   try{RemoteConfig.parse(config(url));fail("Accepted another repository: "+url);}catch(IOException expected){}
  }
 }
 @Test public void editionUpdatesKeepTheSelectedDistribution()throws Exception {
  JSONObject value=new JSONObject(config(RemoteConfig.REPOSITORY));String lite="https://example.com/lite.apk",full="https://example.com/full.apk";value.getJSONObject("android").put("liteApkUrl",lite).put("fullApkUrl",full);
  RemoteConfig parsed=RemoteConfig.parse(value.toString());assertEquals(lite,parsed.downloadUrl(false));assertEquals(full,parsed.downloadUrl(true));
  assertEquals(RemoteConfig.REPOSITORY+"/releases/latest",RemoteConfig.parse(config(RemoteConfig.REPOSITORY)).downloadUrl(true));
 }
}
