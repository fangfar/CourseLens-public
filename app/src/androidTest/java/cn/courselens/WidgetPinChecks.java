package cn.courselens;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.view.accessibility.*;
import java.lang.reflect.*;

/** Exercises the real launcher pin flow. Leaves the newly added QA widget for visual review. */
public final class WidgetPinChecks {
 public static void run(Instrumentation r){
  Bundle result=new Bundle();MainActivity a=null;int code=Activity.RESULT_CANCELED;
  try{
   Context c=r.getTargetContext();if(!c.getPackageName().endsWith(".qa"))throw new AssertionError("QA only");
   AppWidgetManager manager=AppWidgetManager.getInstance(c);ComponentName provider=new ComponentName(c,CourseWidget.class);int before=CourseWidget.count(c);android.content.pm.ResolveInfo home=c.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);if(home==null)throw new AssertionError("Default launcher not found");String launcher=home.activityInfo.packageName;
   Instrumentation.ActivityMonitor monitor=r.addMonitor(MainActivity.class.getName(),null,false);
   try{try(java.io.InputStream command=new ParcelFileDescriptor.AutoCloseInputStream(r.getUiAutomation().executeShellCommand("am start -n "+c.getPackageName()+"/cn.courselens.MainActivity"))){while(command.read()!=-1){}}a=(MainActivity)r.waitForMonitorWithTimeout(monitor,10000);}finally{r.removeMonitor(monitor);}
   if(a==null)throw new AssertionError("QA did not enter foreground");MainActivity active=a;
   c.getSharedPreferences("widget_add",Context.MODE_PRIVATE).edit().putBoolean("guideSeen",true).commit();
   r.runOnMainSync(()->active.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
   r.runOnMainSync(()->{try{Method method=MainActivity.class.getDeclaredMethod("pinWidget");method.setAccessible(true);method.invoke(active);}catch(Exception e){throw new RuntimeException(e);}});
   boolean confirmed=false,guided=false;StringBuilder labels=new StringBuilder();
   for(int retry=0;retry<20;retry++){
    SystemClock.sleep(300);AccessibilityNodeInfo root=r.getUiAutomation().getRootInActiveWindow();
    if(root!=null&&root.findAccessibilityNodeInfosByText("还未确认添加到桌面").size()>0){guided=true;break;}
    if(root!=null&&launcher.contentEquals(root.getPackageName())){
     for(String label:new String[]{"自动添加","添加到桌面","添加到主屏幕","添加","Add","Add to home screen"})for(AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(label)){
      if(node.getText()!=null)labels.append(node.getText()).append(';');
      if(!confirmed&&node.getText()!=null&&label.contentEquals(node.getText())&&node.isClickable()&&node.performAction(AccessibilityNodeInfo.ACTION_CLICK))confirmed=true;
     }
    }
    if(CourseWidget.count(c)>before)break;
   }
   android.graphics.Bitmap bitmap=r.getUiAutomation().takeScreenshot();if(bitmap!=null){try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(c.getFilesDir(),"qa-widget-pin.png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}
   int after=CourseWidget.count(c);
   if(guided)throw new AssertionError("Launcher refused the pin request; fallback guidance is not a successful add");
   else if(after<=before)throw new AssertionError("Launcher did not add widget; supported="+manager.isRequestPinAppWidgetSupported()+", confirmed="+confirmed+", launcherLabels="+labels);
   long deadline=SystemClock.elapsedRealtime()+3000;
   android.content.SharedPreferences pin=c.getSharedPreferences("widget_add",Context.MODE_PRIVATE);
   while(!pin.getString("requestToken","request").equals(pin.getString("confirmedToken","confirmed"))&&SystemClock.elapsedRealtime()<deadline)SystemClock.sleep(100);
   if(!pin.getString("requestToken","request").equals(pin.getString("confirmedToken","confirmed")))throw new AssertionError("Valid launcher success callback not received");
   if(!guided)result.putString("result","PASS: real launcher success callback and new bound widget ("+before+" -> "+after+")");code=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("result","FAIL: "+e);}
  finally{if(a!=null){MainActivity active=a;r.runOnMainSync(active::finish);}r.finish(code,result);}
 }
}
