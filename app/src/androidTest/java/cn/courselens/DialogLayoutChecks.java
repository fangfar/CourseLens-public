package cn.courselens;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

public final class DialogLayoutChecks {
 private interface Work {void run()throws Exception;}
 private static void main(Instrumentation r,Work work){java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();r.runOnMainSync(()->{try{work.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new RuntimeException(error.get());}
 private static Object get(MainActivity a,String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(a);}
 private static Object call(MainActivity a,String name,Class<?>[] types,Object... args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(a,args);}
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 private static void layout(AlertDialog dialog,String label){
  Button primary=dialog.getButton(-1);if(primary!=null&&primary.getVisibility()==View.VISIBLE)check(primary.getCurrentTextColor()==dialog.getContext().getColor(R.color.app_on_primary),label+": primary action does not use the shared style");
  List<Rect> buttons=new ArrayList<>();int top=Integer.MAX_VALUE;
  for(int which:new int[]{-1,-2,-3}){Button button=dialog.getButton(which);if(button==null||button.getVisibility()!=View.VISIBLE)continue;Rect bounds=new Rect();check(button.getGlobalVisibleRect(bounds),label+": button hidden");check(bounds.height()>=button.getHeight()-1,label+": button clipped height="+button.getHeight()+" visible="+bounds+" parentHeight="+((View)button.getParent()).getHeight()+" minimum="+((View)button.getParent()).getMinimumHeight());for(Rect other:buttons)check(!Rect.intersects(bounds,other),label+": buttons overlap");buttons.add(bounds);top=Math.min(top,bounds.top);}
  View body=dialog.findViewById(android.R.id.message);if(body==null||!body.isShown())body=dialog.getWindow().getDecorView().findViewWithTag("remote-message");Rect text=new Rect();check(body!=null&&body.getGlobalVisibleRect(text),label+": message hidden");check(text.bottom<=top-8*body.getResources().getDisplayMetrics().density,label+": text touches button area");
 }
 public static void run(Instrumentation r){
  Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity activity=null;Configuration original=null;SharedPreferences prefs=null;boolean hadGuide=false,guide=false;
  try{
   check(r.getTargetContext().getPackageName().endsWith(".qa"),"QA only");activity=NativeChecks.startQaActivity(r);MainActivity a=activity;original=new Configuration(a.getResources().getConfiguration());prefs=a.getSharedPreferences("widget_add",0);hadGuide=prefs.contains("guideSeen");guide=prefs.getBoolean("guideSeen",false);String courses=(String)get(a,"data");
   for(float scale:(((NativeChecks)r).normalFontOnly?new float[]{original.fontScale}:new float[]{original.fontScale,1.45f})){
    main(r,()->{Configuration config=new Configuration(a.getResources().getConfiguration());config.fontScale=scale;a.getResources().updateConfiguration(config,a.getResources().getDisplayMetrics());});
    for(String name:new String[]{"widgetManualGuide","widgetPermissionGuide","widgetGuide","confirmClear","unsaved","remote-short","remote-long"}){
     AlertDialog[] shown=new AlertDialog[1];
     main(r,()->{
      if(name.equals("unsaved")){shown[0]=new AlertDialog.Builder(a,R.style.SecondaryDialog).setTitle("修改尚未保存").setMessage("退出前是否保存？").setPositiveButton("保存并退出",null).setNegativeButton("不保存退出",null).show();call(a,"styleDialog",new Class<?>[]{AlertDialog.class,boolean.class},shown[0],false);}
      else if(name.startsWith("remote")){shown[0]=(AlertDialog)call(a,"remoteDialog",new Class<?>[]{String.class,String.class,String.class,int.class},"布局测试",name.equals("remote-long")?String.join("\n",Collections.nCopies(40,"合成说明文字，较长时在正文区域滚动。")):"合成公告与更新说明。","",R.drawable.ic_notes);shown[0].setButton(-1,"立即更新",(d,w)->{});shown[0].setButton(-2,"稍后更新",(d,w)->{});shown[0].show();call(a,"styleDialog",new Class<?>[]{AlertDialog.class,boolean.class},shown[0],false);}
      else{Object value=call(a,name,new Class<?>[]{});shown[0]=name.equals("confirmClear")?(AlertDialog)value:(AlertDialog)get(a,"widgetDialog");}
     });
     r.waitForIdleSync();SystemClock.sleep(250);if(name.equals("unsaved")){android.graphics.Bitmap frame=r.getUiAutomation().takeScreenshot();try(java.io.FileOutputStream out=a.openFileOutput("qa-polish-unsaved.png",0)){frame.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{frame.recycle();}}main(r,()->{layout(shown[0],name+" font="+scale);shown[0].dismiss();});
    }
   }
   main(r,()->check(courses.equals(get(a,"data")),"Dialog layout checks changed courses"));result.putString("fontProfile",((NativeChecks)r).normalFontOnly?"current font only":"normal and large fonts");result.putString("result","PASS: seven dialog layouts at the requested font profile; visible buttons, separated text, no overlapping actions, unchanged courses");code=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("error",android.util.Log.getStackTraceString(e));try{android.graphics.Bitmap frame=r.getUiAutomation().takeScreenshot();try(java.io.FileOutputStream out=r.getTargetContext().openFileOutput("qa-dialog-layout-failure.png",0)){frame.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}frame.recycle();}catch(Exception ignored){}}
  finally{if(activity!=null){MainActivity a=activity;Configuration config=original;main(r,()->{if(config!=null)a.getResources().updateConfiguration(config,a.getResources().getDisplayMetrics());a.finish();});}if(prefs!=null){SharedPreferences.Editor edit=prefs.edit();if(hadGuide)edit.putBoolean("guideSeen",guide);else edit.remove("guideSeen");edit.commit();}r.finish(code,result);}
 }
}
