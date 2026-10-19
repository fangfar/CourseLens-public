package cn.courselens;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;

public final class TimetableChecks {
 private interface Work {void run()throws Exception;}
 private static void main(Instrumentation runner,Work work){java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();runner.runOnMainSync(()->{try{work.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new RuntimeException(error.get());}
 private static Object get(MainActivity a,String key)throws Exception{Field field=MainActivity.class.getDeclaredField(key);field.setAccessible(true);return field.get(a);}
 private static void set(MainActivity a,String key,Object value)throws Exception{Field field=MainActivity.class.getDeclaredField(key);field.setAccessible(true);field.set(a,value);}
 private static void call(MainActivity a,String name)throws Exception{Method method=MainActivity.class.getDeclaredMethod(name);method.setAccessible(true);method.invoke(a);}
 private static View root(MainActivity a)throws Exception{return (View)get(a,"root");}
 private static void check(boolean condition,String text){if(!condition)throw new AssertionError(text);}
 private static void restore(SharedPreferences preferences,Map<String,?> backup){SharedPreferences.Editor editor=preferences.edit().clear();for(var entry:backup.entrySet()){Object value=entry.getValue();if(value instanceof String)editor.putString(entry.getKey(),(String)value);else if(value instanceof Set)editor.putStringSet(entry.getKey(),new HashSet<>((Set<String>)value));else if(value instanceof Integer)editor.putInt(entry.getKey(),(Integer)value);else if(value instanceof Boolean)editor.putBoolean(entry.getKey(),(Boolean)value);else if(value instanceof Long)editor.putLong(entry.getKey(),(Long)value);else if(value instanceof Float)editor.putFloat(entry.getKey(),(Float)value);}check(editor.commit(),"Test registry restore failed");}
 private static void snapshot(Instrumentation runner,String name)throws Exception{Bitmap image=runner.getUiAutomation().takeScreenshot();if(image!=null){try(FileOutputStream out=new FileOutputStream(new File(runner.getTargetContext().getExternalFilesDir(null),name))){image.compress(Bitmap.CompressFormat.PNG,100,out);}finally{image.recycle();}}}
 public static void run(Instrumentation runner){
  Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;Context context=runner.getTargetContext();SharedPreferences registry=context.getSharedPreferences("timetables",0);Map<String,?> backup=registry.getAll(),legacy=Timetables.preferences(context,"").getAll();Set<String> originalIds=new HashSet<>(Timetables.names(context).keySet());MainActivity activity=null;
  try{
   XzitChecks.verify();String term=LocalDate.now(ZoneId.of("Asia/Shanghai")).toString();Timetables.add(context,"合成课表甲","squ",ClassTimes.DEFAULT,term);String first=Timetables.active(context);MainActivity a=NativeChecks.startQaActivity(runner);activity=a;
   main(runner,()->{call(a,"newTimetable");check(root(a).findViewWithTag("timetable-source")!=null,"Creation source missing");((EditText)root(a).findViewWithTag("timetable-name")).setText("合成课表乙");root(a).findViewWithTag("create-timetable").performClick();});runner.waitForIdleSync();String second=Timetables.active(context);check(!second.equals(first),"New timetable replaced active data");
   main(runner,()->{call(a,"timesForm");((EditText)root(a).findViewWithTag("class-times")).setText(ClassTimes.XZIT.encode());root(a).findViewWithTag("save-class-times").performClick();check(((String[])get(a,"START_TIMES")).length==14,"Custom period count not applied");set(a,"data","1|14|14|合成末节|测试楼|1");set(a,"notes","合成备注乙");set(a,"term",term);set(a,"week",1);call(a,"save");Method form=MainActivity.class.getDeclaredMethod("courseForm",Course.class,int.class,Runnable.class);form.setAccessible(true);form.invoke(a,null,-1,(Runnable)()->{});check(((Spinner)root(a).findViewWithTag("course-end")).getCount()==14,"Editor hides late periods");call(a,"home");});runner.waitForIdleSync();
   main(runner,()->{RemoteViews views=CourseWidget.views(context,0,LocalDate.parse(term).atTime(20,34));View widget=views.apply(context,new FrameLayout(context));check(((TextView)widget.findViewById(R.id.widget_time)).getText().toString().startsWith("20:35"),"Widget uses another timetable's times");((ScrollView)get(a,"timetableScroll")).fullScroll(View.FOCUS_DOWN);});runner.waitForIdleSync();snapshot(runner,"qa-timetable-14-periods.png");
   main(runner,()->{Timetables.select(context,first);call(a,"reloadTimetable");check(((String[])get(a,"START_TIMES")).length==12&&get(a,"data").equals("")&&get(a,"notes").equals(""),"Timetable fields leaked across profiles");set(a,"data","1|1|2|合成早课|测试楼|1");set(a,"notes","合成备注甲");call(a,"save");Timetables.select(context,second);call(a,"reloadTimetable");check(get(a,"notes").equals("合成备注乙")&&Course.parse((String)get(a,"data")).get(0).end==14,"Switch lost timetable data");call(a,"timetablePicker");check(root(a).findViewWithTag("new-timetable")!=null,"Picker cannot add another timetable");});runner.waitForIdleSync();snapshot(runner,"qa-timetable-picker.png");
   main(runner,a::finish);runner.waitForIdleSync();MainActivity restarted=NativeChecks.startQaActivity(runner);activity=restarted;main(runner,()->check(get(restarted,"notes").equals("合成备注乙")&&((String[])get(restarted,"START_TIMES")).length==14,"Activity restart lost active timetable"));
   check(Timetables.preferences(context,"").getAll().equals(legacy),"Legacy data changed");check(Timetables.preferences(context,first).getString("notes","").equals("合成备注甲"),"Inactive timetable changed");
   result.putString("result","PASS: legacy preserved, UI creation, independent timetable data, 12/14-period switching, custom times, late-course editor/widget, restart, RSA, school parser and HTTP host boundaries");code=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("error",android.util.Log.getStackTraceString(e));}
  finally{if(activity!=null){MainActivity a=activity;main(runner,a::finish);}for(String id:Timetables.names(context).keySet())if(!originalIds.contains(id))check(Timetables.preferences(context,id).edit().clear().commit(),"Synthetic timetable cleanup failed");restore(registry,backup);check(registry.getAll().equals(backup)&&Timetables.preferences(context,"").getAll().equals(legacy),"Original data not restored");CourseWidget.updateAll(context);runner.finish(code,result);}
 }
}
