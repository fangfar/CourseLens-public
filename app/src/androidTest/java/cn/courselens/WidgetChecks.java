package cn.courselens;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;

public final class WidgetChecks {
 private interface Work {void run()throws Exception;}
 private static void main(Instrumentation r,Work work){java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();r.runOnMainSync(()->{try{work.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new RuntimeException(error.get());}
 private static Field field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f;}
 private static Object get(MainActivity a,String name)throws Exception{return field(name).get(a);}
 private static void set(MainActivity a,String name,Object value)throws Exception{field(name).set(a,value);}
 private static void call(MainActivity a,String name)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(a);}
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 private static void settle(Instrumentation r){r.waitForIdleSync();SystemClock.sleep(600);}
 private static void screenshot(Instrumentation r,String name)throws Exception{android.graphics.Bitmap b=r.getUiAutomation().takeScreenshot();check(b!=null,"Screenshot unavailable");try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(r.getTargetContext().getFilesDir(),name))){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{b.recycle();}}
 private static Intent link(Context context,int week,Course c,String term){return new Intent(context,MainActivity.class).setAction(CourseWidget.OPEN).putExtra("widgetWeek",week).putExtra("widgetCourse",CourseDisplay.key(week,c)).putExtra("widgetTerm",term).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);}
 public static void run(Instrumentation runner){
  Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity activity=null;SharedPreferences prefs=null;Map<String,?> backup=null;AppWidgetHost host=null;int widgetId=0;
  try{
   Context context=runner.getTargetContext();check(context.getPackageName().endsWith(".qa"),"Only isolated QA may run widget checks");activity=(MainActivity)runner.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));MainActivity a=activity;prefs=a.getPreferences(0);backup=prefs.getAll();SharedPreferences stored=prefs;
   String term=LocalDate.now(ZoneId.of("Asia/Shanghai")).toString();Course course=new Course(2,3,4,"桌面组件合成课程","测试楼 A201","1","合成教师","合成项目","合成班级");String data=course.encode()+"\n1|1|2|今日合成课|测试楼 B|1";
   main(runner,()->{stored.edit().putString("courseAdjustments","").putStringSet("displayChoices",new HashSet<String>()).commit();set(a,"data",data);set(a,"term",term);set(a,"week",1);set(a,"adjustments","");set(a,"displayChoices",new HashSet<String>());call(a,"save");call(a,"home");});settle(runner);
   AppWidgetManager manager=AppWidgetManager.getInstance(context);host=new AppWidgetHost(context,10610);AppWidgetHost activeHost=host;widgetId=host.allocateAppWidgetId();int id=widgetId;
   check(manager.bindAppWidgetIdIfAllowed(id,new ComponentName(context,CourseWidget.class)),"QA widget binding permission missing");host.startListening();AppWidgetHostView[] view=new AppWidgetHostView[1];
   main(runner,()->{view[0]=activeHost.createView(a,id,manager.getAppWidgetInfo(id));LinearLayout frame=new LinearLayout(a);frame.setPadding(24,80,24,24);frame.setBackgroundColor(0xffd8e3f5);frame.addView(view[0],new LinearLayout.LayoutParams(Math.round(164*a.getResources().getDisplayMetrics().density),Math.round(190*a.getResources().getDisplayMetrics().density)));a.setContentView(frame);CourseWidget.updateAll(a);});settle(runner);
   main(runner,()->{TextView title=view[0].findViewById(R.id.widget_title);check(title!=null&&!title.getText().toString().isEmpty(),"Bound widget did not render");check(((TextView)view[0].findViewById(R.id.widget_header)).getText().toString().contains("今日 1 门"),"Today count wrong");check(((TextView)view[0].findViewById(R.id.widget_updated)).getText().toString().contains("更新 "),"Update timestamp missing");check(view[0].findViewById(R.id.widget_updated).getBottom()<=view[0].getHeight(),"Footer clipped");view[0].findViewById(R.id.widget_refresh).performClick();});settle(runner);screenshot(runner,"qa-widget-bound.png");
   main(runner,()->{set(a,"data","");call(a,"save");});settle(runner);main(runner,()->check(((TextView)view[0].findViewById(R.id.widget_title)).getText().toString().equals("还没有课表"),"Save did not refresh empty widget"));
   main(runner,()->{set(a,"data",data);set(a,"term","bad-date");call(a,"save");});settle(runner);main(runner,()->check(((TextView)view[0].findViewById(R.id.widget_title)).getText().toString().equals("请检查课表设置"),"Invalid term crashed or left old widget"));
   main(runner,()->{set(a,"term",term);call(a,"save");Bundle options=new Bundle();options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,110);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,110);manager.updateAppWidgetOptions(id,options);});settle(runner);
   main(runner,()->view[0].findViewById(R.id.widget_content).performClick());settle(runner);
   main(runner,()->{check((Boolean)get(a,"showingDetail"),"Widget click did not open details");check((Integer)get(a,"week")==1,"Wrong detail week");a.onBackPressed();check(!(Boolean)get(a,"showingDetail"),"Widget detail back did not return home");});
   // A stale widget click must never reopen a course removed from the current schedule.
   main(runner,()->{set(a,"data","");call(a,"save");Method open=MainActivity.class.getDeclaredMethod("openWidget",Intent.class);open.setAccessible(true);open.invoke(a,link(a,1,course,term));check(!(Boolean)get(a,"showingDetail"),"Stale course opened");set(a,"data",data);call(a,"save");});
   main(runner,()->{CourseAdjustment stop=new CourseAdjustment(term,1,course,null);stored.edit().putString("courseAdjustments",stop.encode()).commit();set(a,"adjustments",stop.encode());Method open=MainActivity.class.getDeclaredMethod("openWidget",Intent.class);open.setAccessible(true);open.invoke(a,link(a,1,course,term));check(!(Boolean)get(a,"showingDetail"),"Stopped course reopened");stored.edit().putString("courseAdjustments","").commit();set(a,"adjustments","");});
   main(runner,a::finish);Instrumentation.ActivityMonitor monitor=runner.addMonitor(MainActivity.class.getName(),null,false);context.startActivity(link(context,1,course,term));activity=(MainActivity)runner.waitForMonitorWithTimeout(monitor,4000);runner.removeMonitor(monitor);check(activity!=null,"Cold widget launch failed");MainActivity cold=activity;settle(runner);main(runner,()->{check((Boolean)get(cold,"showingDetail"),"Cold launch did not open course detail");cold.onBackPressed();check(!(Boolean)get(cold,"showingDetail"),"Cold detail back failed");});
   result.putString("result","PASS: bound AppWidgetHost rendering, today count/timestamp, refresh/save updates, empty/error states, resize callback, detail click/back, cold launch, stale/stopped course rejection and preference restoration");code=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("result","FAIL: "+e);}
  finally{
   if(host!=null){host.stopListening();if(widgetId!=0)host.deleteAppWidgetId(widgetId);}
   if(activity!=null){MainActivity a=activity;runner.runOnMainSync(a::finish);}
   if(prefs!=null){SharedPreferences.Editor e=prefs.edit().clear();for(Map.Entry<String,?> item:backup.entrySet()){Object v=item.getValue();if(v instanceof String)e.putString(item.getKey(),(String)v);else if(v instanceof Integer)e.putInt(item.getKey(),(Integer)v);else if(v instanceof Boolean)e.putBoolean(item.getKey(),(Boolean)v);else if(v instanceof Long)e.putLong(item.getKey(),(Long)v);else if(v instanceof Float)e.putFloat(item.getKey(),(Float)v);else if(v instanceof Set)e.putStringSet(item.getKey(),new HashSet<>((Set<String>)v));}check(e.commit()&&prefs.getAll().equals(backup),"Original preferences not restored");CourseWidget.updateAll(runner.getTargetContext());}
   runner.finish(code,result);
  }
 }
}
