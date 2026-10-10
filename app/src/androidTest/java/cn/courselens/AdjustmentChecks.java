package cn.courselens;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

public final class AdjustmentChecks {
 private interface Work {void run()throws Exception;}
 private static void main(Instrumentation runner,Work work){java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();runner.runOnMainSync(()->{try{work.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new RuntimeException(error.get());}
 private static Field field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f;}
 private static Object get(MainActivity a,String name)throws Exception{return field(name).get(a);}
 private static void set(MainActivity a,String name,Object value)throws Exception{field(name).set(a,value);}
 private static Object call(MainActivity a,String name)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);return m.invoke(a);}
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 private static View root(MainActivity a)throws Exception{return (View)get(a,"root");}
 private static void detail(MainActivity a,Course c)throws Exception{Method m=MainActivity.class.getDeclaredMethod("detail",Course.class);m.setAccessible(true);m.invoke(a,c);}
 private static List<Course> effective(MainActivity a)throws Exception{return (List<Course>)call(a,"effectiveCourses");}
 private static void settle(Instrumentation r){r.waitForIdleSync();SystemClock.sleep(400);}
 private static ViewGroup grid(MainActivity a)throws Exception{androidx.viewpager.widget.ViewPager p=(androidx.viewpager.widget.ViewPager)get(a,"pager");for(int i=0;i<p.getChildCount();i++)if(p.getChildAt(i).getLeft()==p.getScrollX())return (ViewGroup)p.getChildAt(i);throw new AssertionError("Grid missing");}
 private static View card(MainActivity a)throws Exception{ViewGroup g=grid(a);for(int i=0;i<g.getChildCount();i++)if("course-card".equals(g.getChildAt(i).getTag()))return g.getChildAt(i);throw new AssertionError("Card missing");}
 private static void screenshot(Instrumentation r,String name)throws Exception{android.graphics.Bitmap image=r.getUiAutomation().takeScreenshot();check(image!=null,"Screenshot unavailable");try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(r.getTargetContext().getFilesDir(),name))){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{image.recycle();}}
 public static void run(Instrumentation runner){
  Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity activity=null;SharedPreferences prefs=null;Map<String,?> backup=null;
  try{
   check(runner.getTargetContext().getPackageName().endsWith(".qa"),"Only the isolated QA package may run adjustment checks");
   activity=(MainActivity)runner.startActivitySync(new Intent(runner.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));MainActivity a=activity;prefs=a.getPreferences(0);backup=prefs.getAll();SharedPreferences actualPrefs=prefs;
   Course original=new Course(1,1,2,"单次调课测试","A101","1-3","合成教师","合成实验","合成班级");String fixture=original.encode();
   main(runner,()->{set(a,"data",fixture);set(a,"term","2026-08-31");set(a,"week",1);set(a,"adjustments","");set(a,"displayChoices",new HashSet<String>());call(a,"save");call(a,"home");});settle(runner);
   main(runner,()->{card(a).performClick();root(a).findViewWithTag("adjust-course").performClick();root(a).findViewWithTag("save-adjustment").performClick();check(((TextView)root(a).findViewWithTag("adjustment-error")).getText().length()>0,"Unchanged arrangement accepted");a.onBackPressed();check(get(a,"adjustments").equals(""),"Cancel saved adjustment");root(a).findViewWithTag("adjust-course").performClick();((Spinner)root(a).findViewWithTag("adjustment-start")).setSelection(5);root(a).findViewWithTag("save-adjustment").performClick();check(((TextView)root(a).findViewWithTag("adjustment-error")).getText().length()>0,"Reversed periods accepted");});
   main(runner,()->{Spinner day=root(a).findViewWithTag("adjustment-day");check(day.performClick(),"Shared picker failed");a.getWindow().getDecorView().findViewWithTag("choice-option-2").performClick();((Spinner)root(a).findViewWithTag("adjustment-week")).setSelection(1);((Spinner)root(a).findViewWithTag("adjustment-end")).setSelection(6);((EditText)root(a).findViewWithTag("adjustment-room")).setText("B202");});settle(runner);screenshot(runner,"qa-adjustment-form.png");
   main(runner,()->root(a).findViewWithTag("save-adjustment").performClick());settle(runner);
   String persisted=(String)get(a,"adjustments");
   main(runner,()->{check(get(a,"data").equals(fixture),"Adjustment rewrote original data");check(actualPrefs.getString("courseAdjustments","").equals(persisted),"Adjustment not persisted");check(CourseDisplay.groups(effective(a),1,Collections.emptySet()).isEmpty(),"Source occurrence remains");check(CourseDisplay.groups(effective(a),2,Collections.emptySet()).size()==2,"Cross-week replacement missing");check(CourseDisplay.groups(effective(a),3,Collections.emptySet()).size()==1,"Unrelated week changed");set(a,"week",2);call(a,"save");call(a,"home");});settle(runner);
   main(runner,()->{ViewGroup g=grid(a);View moved=null;for(int i=0;i<g.getChildCount();i++)if(g.getChildAt(i).findViewWithTag("adjustment-badge")!=null)moved=g.getChildAt(i);check(moved!=null,"Moved card not marked");moved.performClick();root(a).findViewWithTag("adjust-course").performClick();check(((Spinner)root(a).findViewWithTag("adjustment-week")).getSelectedItemPosition()==1,"Moved course prefill failed");check(((EditText)root(a).findViewWithTag("adjustment-room")).getText().toString().equals("B202"),"Room prefill failed");a.onBackPressed();root(a).findViewWithTag("cancel-course").performClick();a.onBackPressed();check(get(a,"adjustments").equals(persisted),"Cancel of stopped-course form changed data");});settle(runner);
   main(runner,a::finish);activity=(MainActivity)runner.startActivitySync(new Intent(runner.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));MainActivity restarted=activity;settle(runner);
   main(runner,()->{check(get(restarted,"adjustments").equals(persisted),"Restart lost adjustment");check(effective(restarted).get(1).room.equals("B202"),"Restart lost replacement");call(restarted,"menu");root(restarted).findViewWithTag("adjustment-manager").performClick();root(restarted).findViewWithTag("restore-adjustment-0").performClick();check(get(restarted,"adjustments").equals(""),"Restore did not remove adjustment");set(restarted,"week",1);call(restarted,"home");});settle(runner);
   main(runner,()->{card(restarted).performClick();root(restarted).findViewWithTag("cancel-course").performClick();root(restarted).findViewWithTag("save-adjustment").performClick();check(CourseDisplay.groups(effective(restarted),1,Collections.emptySet()).isEmpty(),"Stopped class still active");check(CourseDisplay.groups(effective(restarted),2,Collections.emptySet()).size()==1,"Stop leaked to other weeks");check(get(restarted,"data").equals(fixture),"Stop rewrote source");call(restarted,"adjustmentManager");});settle(runner);screenshot(runner,"qa-adjustment-records.png");
   main(runner,()->{root(restarted).findViewWithTag("edit-adjustment-0").performClick();set(restarted,"data",fixture+"\n2|3|4|后续课程|C|1");root(restarted).findViewWithTag("save-adjustment").performClick();check(((TextView)root(restarted).findViewWithTag("adjustment-error")).getText().toString().contains("已更新"),"Stale form saved");set(restarted,"data",fixture);call(restarted,"adjustmentManager");root(restarted).findViewWithTag("restore-adjustment-0").performClick();check(effective(restarted).get(0).encode().equals(fixture),"Restore changed source");detail(restarted,original);root(restarted).findViewWithTag("cancel-course").performClick();root(restarted).findViewWithTag("save-adjustment").performClick();set(restarted,"term","2027-02-22");check(effective(restarted).get(0).encode().equals(fixture),"Adjustment leaked to different term");call(restarted,"adjustmentManager");check(root(restarted).findViewWithTag("edit-adjustment-0")==null,"Stale adjustment can be edited");root(restarted).findViewWithTag("restore-adjustment-0").performClick();check(get(restarted,"adjustments").equals(""),"Stale removal failed");});
   result.putString("result","PASS: UI move/stop/cancel/restore, cross-week scope, picker, validation, unchanged source, marked card, edit moved course, persistence/restart, stale form and term isolation");code=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("result","FAIL: "+e);}
  finally{
   if(activity!=null){MainActivity a=activity;runner.runOnMainSync(a::finish);}
   if(prefs!=null){SharedPreferences.Editor editor=prefs.edit().clear();for(Map.Entry<String,?> entry:backup.entrySet()){Object v=entry.getValue();if(v instanceof String)editor.putString(entry.getKey(),(String)v);else if(v instanceof Integer)editor.putInt(entry.getKey(),(Integer)v);else if(v instanceof Boolean)editor.putBoolean(entry.getKey(),(Boolean)v);else if(v instanceof Long)editor.putLong(entry.getKey(),(Long)v);else if(v instanceof Float)editor.putFloat(entry.getKey(),(Float)v);else if(v instanceof Set)editor.putStringSet(entry.getKey(),new HashSet<>((Set<String>)v));}check(editor.commit()&&prefs.getAll().equals(backup),"Original preferences not restored");}
   runner.finish(code,result);
  }
 }
}
