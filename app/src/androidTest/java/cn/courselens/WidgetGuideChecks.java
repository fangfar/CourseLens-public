package cn.courselens;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import java.lang.reflect.*;

/** Checks guidance without changing vendor permissions or timetable preferences. */
public final class WidgetGuideChecks {
 private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 private static Object field(MainActivity a,String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(a);}
 private static void call(MainActivity a,String name)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(a);}
 private static void main(Instrumentation r,Runnable work){java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();r.runOnMainSync(()->{try{work.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new RuntimeException(error.get());}
 public static void run(Instrumentation r){
  Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity a=null;android.appwidget.AppWidgetHost[] fixture=new android.appwidget.AppWidgetHost[1];int[] fixtureId={-1};
  SharedPreferences prefs=r.getTargetContext().getSharedPreferences("widget_add",Context.MODE_PRIVATE);
  java.util.Map<String,?> backup=prefs.getAll();
  try{
   check(r.getTargetContext().getPackageName().endsWith(".qa"),"QA only");
   Instrumentation.ActivityMonitor monitor=r.addMonitor(MainActivity.class.getName(),null,false);
   try{try(java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(r.getUiAutomation().executeShellCommand("am start -n "+r.getTargetContext().getPackageName()+"/cn.courselens.MainActivity"))){while(in.read()!=-1){}}a=(MainActivity)r.waitForMonitorWithTimeout(monitor,10000);}finally{r.removeMonitor(monitor);}
   check(a!=null,"QA did not start");MainActivity active=a;boolean xiaomi=Build.MANUFACTURER.equalsIgnoreCase("Xiaomi");
   prefs.edit().clear().commit();
   main(r,()->{
    fixture[0]=new android.appwidget.AppWidgetHost(active,6179);fixture[0].deleteHost();fixtureId[0]=fixture[0].allocateAppWidgetId();
    check(android.appwidget.AppWidgetManager.getInstance(active).bindAppWidgetIdIfAllowed(fixtureId[0],new ComponentName(active,CourseWidget.class)),"Temporary QA bind grant required");
   });
   main(r,()->{try{
    active.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    call(active,"widgetAddPage");check(active.getWindow().getDecorView().findViewWithTag("pin-home-widget")!=null,"Retry entry absent");
    if(xiaomi){
     call(active,"pinWidget");AlertDialog dialog=(AlertDialog)field(active,"widgetDialog");
     check(dialog.isShowing(),"First user-triggered guide absent");
     check(dialog.getButton(-1).getText().toString().equals("前往设置"),"Settings option absent");
     check(dialog.getButton(-2).getText().toString().equals("手动添加"),"Manual option absent");
     check(prefs.getBoolean("guideSeen",false)&&!prefs.contains("requestToken"),"Guide fakes request or is not remembered");
     dialog.getButton(-2).performClick();
    }
   }catch(Exception e){throw new RuntimeException(e);}});
   r.waitForIdleSync();SystemClock.sleep(300);
   main(r,()->{try{if(xiaomi){AlertDialog manual=(AlertDialog)field(active,"widgetDialog");check(manual.isShowing()&&manual.getButton(-1).getText().toString().equals("知道了"),"Manual guide absent");manual.dismiss();}}catch(Exception e){throw new RuntimeException(e);}});
   r.waitForIdleSync();SystemClock.sleep(300);
   main(r,()->{try{
    prefs.edit().putString("requestToken","current").putInt("pendingCount",CourseWidget.count(active)).putLong("requestedAt",SystemClock.elapsedRealtime()-2000).apply();
    new WidgetPinReceiver().onReceive(active,new Intent().putExtra("requestToken","stale"));
    new WidgetPinReceiver().onReceive(active,new Intent().putExtra("requestToken","current"));
    check(!prefs.contains("confirmedToken"),"Invalid callback accepted");
   }catch(Exception e){throw new RuntimeException(e);}});
   SystemClock.sleep(300);r.waitForIdleSync();
   main(r,()->{try{
    call(active,"checkWidgetPin");check(!prefs.contains("pendingCount")&&!prefs.contains("consumedToken"),"Unconfirmed request state: focused="+active.hasWindowFocus()+", pending="+prefs.contains("pendingCount")+", consumed="+prefs.contains("consumedToken"));
    AlertDialog dialog=(AlertDialog)field(active,"widgetDialog");check(dialog.isShowing(),"Unconfirmed request has no fallback");
    if(xiaomi){check(dialog.getButton(-3).getText().toString().equals("权限教程"),"Permission tutorial cannot be reopened");}
    ((AlertDialog)field(active,"widgetDialog")).dismiss();
   }catch(Exception e){throw new RuntimeException(e);}});

   prefs.edit().putString("requestToken","callback-test").apply();
   PendingIntent callback=PendingIntent.getBroadcast(active,97,new Intent(active,WidgetPinReceiver.class).putExtra("requestToken","callback-test"),PendingIntent.FLAG_ONE_SHOT|(Build.VERSION.SDK_INT>=31?PendingIntent.FLAG_MUTABLE:0));
   callback.send(active,0,new Intent().putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,fixtureId[0]));
   long deadline=SystemClock.elapsedRealtime()+3000;while(!"callback-test".equals(prefs.getString("confirmedToken",""))&&SystemClock.elapsedRealtime()<deadline)SystemClock.sleep(100);
   check("callback-test".equals(prefs.getString("confirmedToken","")),"Explicit callback did not validate bound provider");
   main(r,()->{try{call(active,"checkWidgetPin");check("callback-test".equals(prefs.getString("consumedToken","")),"Success not consumed");}catch(Exception e){throw new RuntimeException(e);}});
   if(xiaomi){
    main(r,()->{try{call(active,"widgetAddPage");call(active,"widgetPermissionGuide");((AlertDialog)field(active,"widgetDialog")).getButton(-1).performClick();}catch(Exception e){throw new RuntimeException(e);}});
    SystemClock.sleep(600);android.view.accessibility.AccessibilityNodeInfo settings=r.getUiAutomation().getRootInActiveWindow();
    check(settings!=null&&!active.getPackageName().contentEquals(settings.getPackageName()),"Settings did not open");
    try(java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(r.getUiAutomation().executeShellCommand("input -d "+active.getDisplay().getDisplayId()+" keyevent 4"))){while(in.read()!=-1){}}
    SystemClock.sleep(600);r.waitForIdleSync();
    main(r,()->check(active.getWindow().getDecorView().findViewWithTag("pin-home-widget")!=null,"Returning from settings lost retry entry"));
   }
   result.putString("result","PASS: user-triggered guide, manual alternative, remembered explanation, invalid callback rejection, explicit callback validation against bound provider and unconfirmed-request fallback, settings launch/return retry entry; no vendor permission changed");code=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("result","FAIL: "+e);}
  finally{
   if(fixture[0]!=null&&fixtureId[0]>=0)fixture[0].deleteAppWidgetId(fixtureId[0]);
   if(a!=null){MainActivity active=a;r.runOnMainSync(active::finish);}
   SharedPreferences.Editor restore=prefs.edit().clear();for(java.util.Map.Entry<String,?> e:backup.entrySet()){Object v=e.getValue();if(v instanceof String)restore.putString(e.getKey(),(String)v);else if(v instanceof Boolean)restore.putBoolean(e.getKey(),(Boolean)v);else if(v instanceof Integer)restore.putInt(e.getKey(),(Integer)v);else if(v instanceof Long)restore.putLong(e.getKey(),(Long)v);}restore.commit();r.finish(code,result);
  }
 }
}
