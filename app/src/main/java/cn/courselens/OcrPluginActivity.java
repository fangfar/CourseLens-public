package cn.courselens;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.*;
import java.net.HttpURLConnection;

/** Optional APK download/install and a bounded Activity-result bridge to local OCR. */
public final class OcrPluginActivity extends Activity {
 static final String MANIFEST="https://morrow.xvyan.cn/remote/ocr-plugin.json";
 private static final ExecutorService IO=Executors.newSingleThreadExecutor();
 private final Handler timer=new Handler(Looper.getMainLooper());
 private SharedPreferences prefs;private DownloadManager downloads;private TextView status;private Button action,cancel;private ProgressBar progress;
 private Uri image;private boolean foreground,busy;private String stage="idle";
 private final Runnable poll=this::pollDownload;
 private boolean qaDownloads(){return getPackageName().endsWith(".qa")&&(getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0;}
 private String manifestUrl()throws PackageManager.NameNotFoundException {
  if(qaDownloads()){Bundle metadata=getPackageManager().getApplicationInfo(getPackageName(),PackageManager.GET_META_DATA).metaData;if(metadata!=null){String value=metadata.getString("cn.courselens.ocr.QA_MANIFEST","");if(!value.isEmpty())return value;}}
  return MANIFEST;
 }
 @Override protected void attachBaseContext(Context base){super.attachBaseContext(Appearance.wrap(base));}
 static String pluginPackage(Context context){return context.getPackageName().endsWith(".qa")?"cn.courselens.ocr.qa":"cn.courselens.ocr";}
 static boolean compatible(Context context,PackageInfo info){
  try{
   PackageManager pm=context.getPackageManager();PackageInfo host=pm.getPackageInfo(context.getPackageName(),PackageManager.GET_SIGNATURES);
   return info!=null&&pluginPackage(context).equals(info.packageName)&&androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(info)>=1&&info.signatures!=null&&info.signatures.length==1&&host.signatures.length==1&&host.signatures[0].equals(info.signatures[0])&&info.applicationInfo.enabled&&info.applicationInfo.metaData!=null&&info.applicationInfo.metaData.getInt("cn.courselens.ocr.PROTOCOL")==ParcelOcr.PROTOCOL;
  }catch(PackageManager.NameNotFoundException|RuntimeException e){return false;}
 }
 static boolean installed(Context context){
  try{return compatible(context,context.getPackageManager().getPackageInfo(pluginPackage(context),PackageManager.GET_SIGNATURES|PackageManager.GET_META_DATA))&&context.getPackageManager().getActivityInfo(new ComponentName(pluginPackage(context),"cn.courselens.ocr.OcrActivity"),0).enabled;}
  catch(PackageManager.NameNotFoundException|RuntimeException e){return false;}
 }
 @Override public void onCreate(Bundle saved){
  if(!installed(this))setTheme(R.style.AppTheme);super.onCreate(saved);prefs=getSharedPreferences("ocr-plugin",MODE_PRIVATE);downloads=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);image=getIntent().getData();
  if(saved!=null){stage=saved.getString("stage","idle");String value=saved.getString("image");if(value!=null)image=Uri.parse(value);}
  if(installed(this)){
   if(!"recognizing".equals(stage)&&!"picker".equals(stage)&&!"permission".equals(stage)&&!"installer".equals(stage)){stage="idle";usePlugin();}
   return;
  }
  downloadView();
  if("recognizing".equals(stage)||"picker".equals(stage)||"permission".equals(stage)||"installer".equals(stage)){show("等待系统操作完成…","继续");return;}
  stage="idle";if(installed(this))usePlugin();else if(prefs.getLong("downloadId",-1)<0)show("未安装图片识别插件","下载并安装");
 }
 private void downloadView(){
  SecondaryUi ui=new SecondaryUi(this);ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(ui.dp(24),ui.dp(32),ui.dp(24),ui.dp(24));body.setBackgroundColor(ui.SURFACE);scroll.addView(body);setContentView(scroll);
  body.addView(ui.text("图片识别插件",24,ui.INK));TextView explanation=ui.text("图片在本机离线识别，不上传。下载后需在系统界面确认安装；插件可单独卸载，不影响已有数据。",15,ui.MUTED);explanation.setPadding(0,ui.dp(20),0,ui.dp(20));body.addView(explanation);
  status=ui.text("",15,ui.INK);status.setTag("ocr-plugin-status");status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);body.addView(status);
  progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);body.addView(progress,new LinearLayout.LayoutParams(-1,ui.dp(20)));
  action=ui.action(body,"下载并安装",this::perform);action.setTag("ocr-plugin-action");cancel=ui.action(body,"取消下载",()->{clearDownload();busy=false;stage="idle";show("下载已取消，可稍后重试","下载并安装");});cancel.setTag("ocr-plugin-cancel");cancel.setVisibility(View.GONE);
  ui.action(body,"返回",this::finish);
 }
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("stage",stage);if(image!=null)out.putString("image",image.toString());}
 @Override protected void onResume(){super.onResume();foreground=true;if("idle".equals(stage)&&prefs.getLong("downloadId",-1)>=0)pollDownload();}
 @Override protected void onPause(){super.onPause();foreground=false;timer.removeCallbacks(poll);}
 @Override protected void onDestroy(){timer.removeCallbacks(poll);super.onDestroy();}
 private void show(String message,String label){if(isDestroyed())return;if(status==null)downloadView();status.setText(message);action.setText(label);action.setEnabled(!busy);progress.setVisibility(busy?View.VISIBLE:View.GONE);cancel.setVisibility(!busy&&prefs.getLong("downloadId",-1)>=0?View.VISIBLE:View.GONE);}
 private void perform(){
  if(busy)return;
  if(installed(this)){usePlugin();return;}
  if(privateApk().isFile()&&prefs.contains("sha256")){verifyAndInstall();return;}
  if(prefs.getLong("downloadId",-1)>=0){pollDownload();return;}
  busy=true;show("正在获取插件下载信息…","请稍候");
  IO.execute(()->{
   try{
    OcrPluginRelease release=OcrPluginRelease.parse(fetchManifest(manifestUrl(),qaDownloads()),qaDownloads());
    runOnUiThread(()->{if(isDestroyed())return;try{
     clearDownload();File folder=getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);if(folder==null)throw new IOException();File target=new File(folder,"ocr-plugin.apk");if(target.exists()&&!target.delete())throw new IOException();
     long id=downloads.enqueue(new DownloadManager.Request(Uri.parse(release.url)).setTitle("Morrow 图片识别插件").setMimeType("application/vnd.android.package-archive").setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED).setDestinationInExternalFilesDir(this,Environment.DIRECTORY_DOWNLOADS,"ocr-plugin.apk"));
     if(!prefs.edit().putLong("downloadId",id).putString("sha256",release.sha256).putLong("bytes",release.bytes).putLong("versionCode",release.versionCode).commit()){downloads.remove(id);throw new IOException();}
     busy=false;if(foreground)pollDownload();
    }catch(Exception e){failed("无法开始下载，请检查存储空间和系统下载管理器后重试");}});
   }catch(Exception e){runOnUiThread(()->failed(e instanceof IOException&&e.getMessage()!=null&&e.getMessage().startsWith("图片识别插件尚未")?e.getMessage():getPackageName().endsWith(".qa")?"获取 QA 插件失败，请确认测试下载源连接后重试":"获取插件失败，请检查网络后重试"));}
  });
 }
 static String fetchManifest(String endpoint,boolean qa)throws Exception {
  String url=endpoint;
  for(int redirects=0;redirects<4;redirects++){
   if(!OcrPluginRelease.validLink(url,qa))throw new IOException();
   HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();connection.setConnectTimeout(15000);connection.setReadTimeout(15000);connection.setInstanceFollowRedirects(false);
   try{
    int code=connection.getResponseCode();if(code>=300&&code<400){String next=new URL(new URL(url),connection.getHeaderField("Location")).toString();if(!OcrPluginRelease.validLink(next,qa))throw new IOException();url=next;continue;}
    if(code!=200)throw new IOException();try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[2048];int count;while((count=in.read(buffer))!=-1){if(out.size()+count>16384)throw new IOException();out.write(buffer,0,count);}return out.toString(StandardCharsets.UTF_8.name());}
   }finally{connection.disconnect();}
  }
  throw new IOException();
 }
 private void pollDownload(){
  if(!foreground||busy||isDestroyed())return;long id=prefs.getLong("downloadId",-1);if(id<0)return;
  try(Cursor cursor=downloads.query(new DownloadManager.Query().setFilterById(id))){
   if(cursor==null||!cursor.moveToFirst()){clearDownload();failed("下载记录已失效，请重新下载");return;}
   int state=cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));long done=cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));long total=cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
   if(done>prefs.getLong("bytes",0)||total>prefs.getLong("bytes",0)){clearDownload();failed("插件大小校验失败，请重新下载");return;}
   if(state==DownloadManager.STATUS_SUCCESSFUL){verifyAndInstall();return;}
   if(state==DownloadManager.STATUS_FAILED){clearDownload();failed("插件下载失败，请检查网络和存储空间后重试");return;}
   int percent=total>0?(int)(done*100/total):0;status.setText(state==DownloadManager.STATUS_PAUSED?"下载已暂停，等待网络恢复…":"正在下载图片识别插件 · "+percent+"%");progress.setVisibility(View.VISIBLE);progress.setIndeterminate(total<=0);progress.setProgress(percent);action.setEnabled(false);cancel.setVisibility(View.VISIBLE);timer.postDelayed(poll,700);
  }catch(RuntimeException e){failed("无法读取下载进度，请重试");}
 }
 private File privateApk(){return new File(getFilesDir(),"ocr/plugin.apk");}
 private void verifyAndInstall(){
  if(busy)return;busy=true;show("正在校验插件…","请稍候");
  IO.execute(()->{
   try{
    File apk=privateApk();if(!apk.isFile()){
     File parent=apk.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException();
     long id=prefs.getLong("downloadId",-1);try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(downloads.openDownloadedFile(id));OutputStream out=new FileOutputStream(apk)){byte[] buffer=new byte[16384];int count;long total=0;while((count=in.read(buffer))!=-1){total+=count;if(total>OcrPluginRelease.MAX_BYTES)throw new IOException();out.write(buffer,0,count);}}
    }
    MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(apk)){byte[] buffer=new byte[16384];int count;while((count=in.read(buffer))!=-1)digest.update(buffer,0,count);}
    StringBuilder hash=new StringBuilder();for(byte b:digest.digest())hash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
    PackageInfo info=getPackageManager().getPackageArchiveInfo(apk.getPath(),PackageManager.GET_SIGNATURES|PackageManager.GET_META_DATA);
    if(apk.length()!=prefs.getLong("bytes",0)||!hash.toString().equals(prefs.getString("sha256",""))||!compatible(this,info)||androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(info)!=prefs.getLong("versionCode",0))throw new IOException();
    runOnUiThread(()->{busy=false;if(isDestroyed())return;stage="ready";show("下载完成，已通过安全校验","安装插件");if(foreground)install();});
   }catch(Exception e){runOnUiThread(()->{clearDownload();failed("插件校验失败，已丢弃文件，请重新下载");});}
  });
 }
 private void install(){
  try{
   if(!getPackageManager().canRequestPackageInstalls()){stage="permission";show("请允许 Morrow 安装应用，返回后继续安装插件","允许安装插件");startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())),602);return;}
   stage="installer";Uri uri=FileProvider.getUriForFile(this,getPackageName()+".ocr.files",privateApk());
   Intent install=new Intent(Intent.ACTION_INSTALL_PACKAGE).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).putExtra(Intent.EXTRA_RETURN_RESULT,true);install.setClipData(ClipData.newRawUri("插件安装包",uri));startActivityForResult(install,603);
  }catch(RuntimeException e){stage="idle";show("无法打开安装界面，请检查系统安装权限后重试","重试安装");}
 }
 private void usePlugin(){
  if(image==null){stage="picker";try{ImagePicker.open(this,604);}catch(RuntimeException e){stage="idle";show("系统没有可用的图片选择器","重试");}return;}
  try{
   if(!installed(this))throw new SecurityException();stage="recognizing";
   Intent recognize=new Intent().setComponent(new ComponentName(pluginPackage(this),"cn.courselens.ocr.OcrActivity")).setDataAndType(image,"image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).putExtra("protocol",ParcelOcr.PROTOCOL);recognize.setClipData(ClipData.newRawUri("待识别图片",image));startActivityForResult(recognize,601);
  }catch(RuntimeException e){stage="idle";show("插件无法使用，请重新安装后重试","重试");}
 }
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);stage="idle";
  if(request==601){Intent answer=data==null?new Intent():data;answer.putExtra("epoch",getIntent().getStringExtra("epoch"));setResult(result,answer);finish();}
  else if(request==602){if(getPackageManager().canRequestPackageInstalls())verifyAndInstall();else show("未允许安装，授权后可继续","允许安装插件");}
  else if(request==603){if(installed(this)){clearDownload();usePlugin();}else show("安装未完成，可再次尝试","重试安装");}
  else if(request==604){if(result==RESULT_OK&&data!=null){image=data.getData();usePlugin();}else finish();}
 }
 private void failed(String message){if(isDestroyed())return;busy=false;stage="idle";show(message,"重试");}
 private void clearDownload(){long id=prefs.getLong("downloadId",-1);if(id>=0)try{downloads.remove(id);}catch(RuntimeException ignored){}privateApk().delete();prefs.edit().clear().commit();timer.removeCallbacks(poll);}
}
