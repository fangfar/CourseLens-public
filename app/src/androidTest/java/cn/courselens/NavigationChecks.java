package cn.courselens;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

public final class NavigationChecks {
 private interface Work {void run()throws Exception;}
 private static void main(Instrumentation runner,Work work){java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();runner.runOnMainSync(()->{try{work.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new RuntimeException(error.get());}
 private static Object get(MainActivity activity,String name)throws Exception{Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);return field.get(activity);}
 private static void call(MainActivity activity,String name)throws Exception{Method method=MainActivity.class.getDeclaredMethod(name);method.setAccessible(true);method.invoke(activity);}
 private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 private static void settled(Instrumentation runner){runner.waitForIdleSync();SystemClock.sleep(350);}

 static void verify(Instrumentation runner,MainActivity activity){
  Map<String,?> preferences=new HashMap<>(activity.getPreferences(0).getAll());View[] home=new View[1];int[] position=new int[2];
  main(runner,()->{call(activity,"home");});settled(runner);
  main(runner,()->{home[0]=(View)get(activity,"root");ScrollView scroll=(ScrollView)get(activity,"timetableScroll");scroll.scrollTo(0,100);position[0]=scroll.getScrollY();position[1]=(int)get(activity,"week");View navigation=activity.findViewById(android.R.id.content).findViewWithTag("bottom-navigation");check(navigation!=null&&navigation.getVisibility()==View.VISIBLE,"Primary navigation missing");check(navigation.getParent()==((View)get(activity,"stage")).getParent(),"Navigation scrolls with page content");check(navigation.findViewWithTag("nav-timetable").isSelected(),"Timetable not selected");navigation.findViewWithTag("nav-tasks").performClick();});settled(runner);
  main(runner,()->{View root=(View)get(activity,"root");check("tasks-page".equals(root.getTag()),"Task tab did not open independent page");check(get(activity,"pageBack")==null,"Tasks treated as secondary page");check(activity.findViewById(android.R.id.content).findViewWithTag("nav-tasks").isSelected(),"Task tab not selected");check(root.getImportantForAccessibility()!=View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,"Task page hidden from accessibility");root.findViewWithTag("task-filter-1").performClick();check(((TextView)root.findViewWithTag("tasks-empty-title")).getText().toString().equals("还没有已完成任务"),"Task filter did not change empty state");check(root.findViewWithTag("task-filter-1").isSelected(),"Task filter not marked selected");Bundle state=new Bundle();((TaskPage)root).saveState(state);TaskPage restored=new TaskPage(activity,state);check(restored.findViewWithTag("task-filter-1").isSelected(),"Saved task filter lost");activity.findViewById(android.R.id.content).findViewWithTag("nav-timetable").performClick();});settled(runner);
  main(runner,()->{check(get(activity,"root")==home[0],"Tab switch rebuilt retained timetable");check(((ScrollView)get(activity,"timetableScroll")).getScrollY()==position[0]&&(int)get(activity,"week")==position[1],"Tab switch lost timetable position");activity.findViewById(android.R.id.content).findViewWithTag("nav-tasks").performClick();});settled(runner);
  main(runner,()->{View root=(View)get(activity,"root");check(root.findViewWithTag("task-filter-1").isSelected(),"Switching tabs lost task filter");check(root.getImportantForAccessibility()!=View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,"Reopened task page hidden from accessibility");activity.onBackPressed();});settled(runner);
  main(runner,()->{check(!(boolean)get(activity,"tasksVisible")&&get(activity,"root")==home[0],"Task back did not return to timetable");call(activity,"menu");check(activity.findViewById(android.R.id.content).findViewWithTag("bottom-navigation").getVisibility()==View.GONE,"Navigation allows leaving secondary forms");activity.onBackPressed();});settled(runner);
  main(runner,()->{check(activity.findViewById(android.R.id.content).findViewWithTag("bottom-navigation").getVisibility()==View.VISIBLE,"Returning from settings did not restore navigation");check(activity.getPreferences(0).getAll().equals(preferences),"Task navigation changed stored timetable");});
 }

 public static void run(Instrumentation runner){
  Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity activity=null;
  try{check(runner.getTargetContext().getPackageName().endsWith(".qa"),"QA only");activity=NativeChecks.startQaActivity(runner);verify(runner,activity);result.putString("result","PASS: independent task page, filters, saved filter, tab selection, timetable retention, back, secondary-page navigation and unchanged preferences");code=Activity.RESULT_OK;}
  catch(Throwable e){result.putString("error",android.util.Log.getStackTraceString(e));}
  finally{if(activity!=null){MainActivity current=activity;main(runner,current::finish);}runner.finish(code,result);}
 }
}
