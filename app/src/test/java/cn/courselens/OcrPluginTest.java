package cn.courselens;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.*;
import android.content.pm.*;
import android.os.Bundle;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import java.io.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
public class OcrPluginTest {
 @Test public void distributionLabelMatchesBundledEngine()throws Exception{
  assertEquals(LocalOcr.AVAILABLE?"":"轻量版",RuntimeEnvironment.getApplication().getString(R.string.distribution_name));
  try{Class.forName("cn.courselens.ocr.OcrEngine");assertTrue("Lite must not include the OCR engine",LocalOcr.AVAILABLE);}catch(ClassNotFoundException absent){assertFalse("Full must bundle the OCR engine",LocalOcr.AVAILABLE);}
 }
 @Test public void imagePickerUsesGalleryInsteadOfDocuments(){
  Intent photo=ImagePicker.intent(34);assertEquals(MediaStore.ACTION_PICK_IMAGES,photo.getAction());assertEquals("image/*",photo.getType());
  Intent gallery=ImagePicker.intent(28);assertEquals(Intent.ACTION_PICK,gallery.getAction());assertEquals(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,gallery.getData());
 }
 private JSONObject row()throws Exception{return new JSONObject().put("text","合成快递取件码 6-2-2088").put("left",0).put("top",0).put("right",800).put("bottom",80);}
 private String result(JSONObject row)throws Exception{return new JSONObject().put("protocol",1).put("lines",new JSONArray().put(row)).toString();}
 @Test public void preservesLayoutAndConfirmation()throws Exception{
  var lines=ParcelOcr.lines(result(row()));assertTrue(ParcelOcr.text(lines).contains("6-2-2088"));var candidates=ParcelRules.extractImage(lines);assertEquals("6-2-2088",candidates.get(0).code);assertFalse(candidates.get(0).autoSave());
 }
 @Test public void rejectsMalformedAndOversizedResults()throws Exception{
  for(String value:new String[]{"{}",result(row())+"{}",result(row().put("left",-1)),result(row().put("bottom",2500)),result(row().put("left",900)),result(row().put("text","x".repeat(12001))),result(row().put("text",42)),result(row().put("top",0.5)),"x".repeat(128*1024+1)}){
   try{ParcelOcr.lines(value);fail("Accepted invalid result");}catch(Exception expected){}
  }
 }
 private JSONObject release()throws Exception{return new JSONObject().put("schemaVersion",1).put("protocol",1).put("enabled",true).put("packageName","cn.courselens.ocr").put("url","https://example.com/plugin.apk").put("sha256","a".repeat(64)).put("bytes",1000).put("versionCode",1);}
 @Test public void acceptsBoundedHttpsDownload()throws Exception{var value=OcrPluginRelease.parse(release().toString());assertEquals(1000,value.bytes);assertEquals(1,value.versionCode);}
 @Test public void qaDownloadCannotCrossIntoProductionOrLan()throws Exception{
  JSONObject qa=release().put("packageName","cn.courselens.ocr.qa").put("url","http://127.0.0.1:8765/plugin.apk");assertEquals(1000,OcrPluginRelease.parse(qa.toString(),true).bytes);
  try{OcrPluginRelease.parse(qa.toString());fail();}catch(Exception expected){}
  try{OcrPluginRelease.parse(release().toString(),true);fail();}catch(Exception expected){}
  for(String url:new String[]{"http://192.168.1.1:8765/a","http://127.0.0.1.evil:8765/a","http://user@127.0.0.1:8765/a","http://127.0.0.1/a"})assertFalse(OcrPluginRelease.validLink(url,true));
 }
 @Test public void rejectsUntrustedDownloadDescriptors()throws Exception{
  for(JSONObject value:new JSONObject[]{release().put("enabled",false),release().put("url","http://example.com/plugin.apk"),release().put("url","https://user@example.com/a"),release().put("sha256","no"),release().put("bytes",OcrPluginRelease.MAX_BYTES+1),release().put("bytes",1.5),release().put("versionCode",0),release().put("packageName","other"),release().put("protocol",2)}){
   try{OcrPluginRelease.parse(value.toString());fail("Accepted untrusted descriptor");}catch(Exception expected){}
  }
 }
 @Test public void requiresMatchingPackageSignerAndProtocol()throws Exception{
  Context context=RuntimeEnvironment.getApplication();PackageManager pm=context.getPackageManager();PackageInfo host=pm.getPackageInfo(context.getPackageName(),PackageManager.GET_SIGNATURES);host.signatures=new Signature[]{new Signature("1234")};Shadows.shadowOf(pm).installPackage(host);
  PackageInfo plugin=new PackageInfo();plugin.packageName=OcrPluginActivity.pluginPackage(context);plugin.setLongVersionCode(1);plugin.signatures=host.signatures;plugin.applicationInfo=new ApplicationInfo();plugin.applicationInfo.enabled=true;plugin.applicationInfo.metaData=new Bundle();plugin.applicationInfo.metaData.putInt("cn.courselens.ocr.PROTOCOL",1);
  assertTrue(OcrPluginActivity.compatible(context,plugin));plugin.signatures=new Signature[]{new Signature("abcd")};assertFalse(OcrPluginActivity.compatible(context,plugin));plugin.signatures=host.signatures;plugin.packageName="other";assertFalse(OcrPluginActivity.compatible(context,plugin));plugin.packageName=OcrPluginActivity.pluginPackage(context);plugin.applicationInfo.metaData.putInt("cn.courselens.ocr.PROTOCOL",2);assertFalse(OcrPluginActivity.compatible(context,plugin));
 }
 @Test public void missingPluginShowsDownloadEntry(){
  try(var controller=Robolectric.buildActivity(OcrPluginActivity.class).setup()){
   Button action=controller.get().findViewById(android.R.id.content).findViewWithTag("ocr-plugin-action");assertEquals("下载并安装",action.getText().toString());assertTrue(action.isEnabled());assertNull(Shadows.shadowOf(controller.get()).getNextStartedActivity());
  }
 }
 private PackageInfo installTrustedPlugin()throws Exception{
  Context context=RuntimeEnvironment.getApplication();PackageManager pm=context.getPackageManager();PackageInfo host=pm.getPackageInfo(context.getPackageName(),PackageManager.GET_SIGNATURES);host.signatures=new Signature[]{new Signature("1234")};Shadows.shadowOf(pm).installPackage(host);
  PackageInfo plugin=new PackageInfo();plugin.packageName=OcrPluginActivity.pluginPackage(context);plugin.setLongVersionCode(1);plugin.signatures=host.signatures;plugin.applicationInfo=new ApplicationInfo();plugin.applicationInfo.packageName=plugin.packageName;plugin.applicationInfo.enabled=true;plugin.applicationInfo.metaData=new Bundle();plugin.applicationInfo.metaData.putInt("cn.courselens.ocr.PROTOCOL",1);
  ActivityInfo entry=new ActivityInfo();entry.name="cn.courselens.ocr.OcrActivity";entry.packageName=plugin.packageName;entry.applicationInfo=plugin.applicationInfo;entry.enabled=true;entry.exported=true;plugin.activities=new ActivityInfo[]{entry};Shadows.shadowOf(pm).installPackage(plugin);
  return plugin;
 }
 @Test public void verifiedPluginGetsTemporaryImageGrantAndReturnsEpoch()throws Exception{
  Context context=RuntimeEnvironment.getApplication();PackageInfo plugin=installTrustedPlugin();
  assertTrue(OcrPluginActivity.installed(context));
  Uri image=Uri.parse("content://synthetic/image");Intent request=new Intent(context,OcrPluginActivity.class).setData(image).putExtra("epoch","synthetic-epoch");
  try(var controller=Robolectric.buildActivity(OcrPluginActivity.class,request).setup()){
   assertNull("Installed OCR must not show download controls",controller.get().findViewById(android.R.id.content).findViewWithTag("ocr-plugin-action"));
   var launched=Shadows.shadowOf(controller.get()).getNextStartedActivityForResult();assertEquals(601,launched.requestCode);assertEquals(plugin.packageName,launched.intent.getComponent().getPackageName());assertEquals(image,launched.intent.getClipData().getItemAt(0).getUri());assertTrue((launched.intent.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0);
   Intent answer=new Intent().putExtra("ocrResult",result(row()));controller.get().onActivityResult(601,Activity.RESULT_OK,answer);assertTrue(controller.get().isFinishing());assertEquals("synthetic-epoch",Shadows.shadowOf(controller.get()).getResultIntent().getStringExtra("epoch"));
  }
 }
 @Test public void installedPluginOpensImagePickerWithoutDownloadPage()throws Exception{
  installTrustedPlugin();
  try(var controller=Robolectric.buildActivity(OcrPluginActivity.class).setup()){
   var picker=Shadows.shadowOf(controller.get()).getNextStartedActivityForResult();assertNotNull(picker);assertEquals(MediaStore.ACTION_PICK_IMAGES,picker.intent.getAction());assertEquals("image/*",picker.intent.getType());
   assertNull("Installed OCR must bypass the download screen",controller.get().findViewById(android.R.id.content).findViewWithTag("ocr-plugin-action"));
  }
 }
 @Test public void parcelImageButtonOpensPickerDirectlyOnEveryClick()throws Exception{
  installTrustedPlugin();
  try(var controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
   ParcelActivity activity=controller.get();ParcelPage page=org.robolectric.util.ReflectionHelpers.getField(activity,"page");page.pause();
   for(int i=0;i<3;i++){ParcelRepository.IO.submit(()->{}).get();Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(400));}
   page.findViewWithTag("parcel-read-info").performClick();android.app.AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();Button choose=dialog.getWindow().getDecorView().findViewWithTag("parcel-ocr-plugin");assertEquals("选择图片",choose.getText().toString());
   for(int i=0;i<3;i++){choose.performClick();var picker=Shadows.shadowOf(activity).getNextStartedActivityForResult();assertEquals(501,picker.requestCode);assertEquals(MediaStore.ACTION_PICK_IMAGES,picker.intent.getAction());assertEquals(MediaStore.ACTION_PICK_IMAGES,Shadows.shadowOf(activity).getNextStartedActivity().getAction());page.onActivityResult(501,Activity.RESULT_CANCELED,null);assertNull(Shadows.shadowOf(activity).getNextStartedActivity());}
  }
 }
 @Test public void deniedInstallPermissionKeepsRetryAvailable(){
  try(var controller=Robolectric.buildActivity(OcrPluginActivity.class).setup()){
   controller.get().onActivityResult(602,Activity.RESULT_CANCELED,null);Button action=controller.get().findViewById(android.R.id.content).findViewWithTag("ocr-plugin-action");assertEquals("允许安装插件",action.getText().toString());assertTrue(action.isEnabled());
  }
 }
 @Test public void downloadProgressSurvivesRecreationAndCanBeCancelled(){
  Context context=RuntimeEnvironment.getApplication();DownloadManager manager=context.getSystemService(DownloadManager.class);DownloadManager.Request request=new DownloadManager.Request(Uri.parse("https://example.com/plugin.apk"));long id=manager.enqueue(request);
  org.robolectric.shadows.ShadowDownloadManager.ShadowRequest download=org.robolectric.shadow.api.Shadow.extract(request);download.setStatus(DownloadManager.STATUS_RUNNING);download.setBytesSoFar(500);download.setTotalSize(1000);
  context.getSharedPreferences("ocr-plugin",0).edit().putLong("downloadId",id).putLong("bytes",1000).commit();
  try(var controller=Robolectric.buildActivity(OcrPluginActivity.class).setup()){
   TextView status=controller.get().findViewById(android.R.id.content).findViewWithTag("ocr-plugin-status");assertTrue(status.getText().toString().contains("50%"));controller.recreate();status=controller.get().findViewById(android.R.id.content).findViewWithTag("ocr-plugin-status");assertTrue(status.getText().toString().contains("50%"));
   controller.get().findViewById(android.R.id.content).findViewWithTag("ocr-plugin-cancel").performClick();assertEquals(0,Shadows.shadowOf(manager).getRequestCount());assertFalse(context.getSharedPreferences("ocr-plugin",0).contains("downloadId"));assertTrue(status.getText().toString().contains("取消"));
  }
 }
 @Test public void oversizedDownloadIsRemoved(){
  Context context=RuntimeEnvironment.getApplication();DownloadManager manager=context.getSystemService(DownloadManager.class);DownloadManager.Request request=new DownloadManager.Request(Uri.parse("https://example.com/plugin.apk"));long id=manager.enqueue(request);
  org.robolectric.shadows.ShadowDownloadManager.ShadowRequest download=org.robolectric.shadow.api.Shadow.extract(request);download.setStatus(DownloadManager.STATUS_RUNNING);download.setBytesSoFar(1001);download.setTotalSize(1001);context.getSharedPreferences("ocr-plugin",0).edit().putLong("downloadId",id).putLong("bytes",1000).commit();
  try(var controller=Robolectric.buildActivity(OcrPluginActivity.class).setup()){
   TextView status=controller.get().findViewById(android.R.id.content).findViewWithTag("ocr-plugin-status");assertTrue(status.getText().toString().contains("校验失败"));assertEquals(0,Shadows.shadowOf(manager).getRequestCount());assertNull(Shadows.shadowOf(controller.get()).getNextStartedActivity());
  }
 }
 @Test @GraphicsMode(GraphicsMode.Mode.NATIVE) public void downloadScreenFitsNormalAndLargeDarkText()throws Exception{
  for(boolean large:new boolean[]{false,true}){
   RuntimeEnvironment.setQualifiers(large?"w320dp-h640dp-night":"w380dp-h800dp-notnight");RuntimeEnvironment.setFontScale(large?2f:1f);
   try(var controller=Robolectric.buildActivity(OcrPluginActivity.class).setup()){
    Activity activity=controller.get();View root=activity.findViewById(android.R.id.content);int width=activity.getResources().getDisplayMetrics().widthPixels,height=activity.getResources().getDisplayMetrics().heightPixels;root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));root.layout(0,0,width,height);
    Button action=root.findViewWithTag("ocr-plugin-action");assertTrue(action.getHeight()>=48*activity.getResources().getDisplayMetrics().density);assertTrue(action.getLayout().getHeight()+action.getCompoundPaddingTop()+action.getCompoundPaddingBottom()<=action.getHeight());
    android.widget.ScrollView scroll=(android.widget.ScrollView)((android.view.ViewGroup)root).getChildAt(0);View body=scroll.getChildAt(0);Bitmap image=Bitmap.createBitmap(width,body.getHeight(),Bitmap.Config.ARGB_8888);body.draw(new Canvas(image));File folder=new File(System.getProperty("java.io.tmpdir"),"morrow-ocr-qa");if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException();try(OutputStream out=new FileOutputStream(new File(folder,large?"download-dark-large.png":"download-normal.png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}finally{image.recycle();}
   }
  }
 }
}
