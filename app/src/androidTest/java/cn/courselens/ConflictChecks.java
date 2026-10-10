package cn.courselens;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

public final class ConflictChecks {
 private static Field field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f;}
 private static Object get(MainActivity a,String name)throws Exception{return field(name).get(a);}
 private static void set(MainActivity a,String name,Object value)throws Exception{field(name).set(a,value);}
 private static void call(MainActivity a,String name)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(a);}
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 private interface Work {void run()throws Exception;}
 private static void main(Instrumentation runner,Work work){java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();runner.runOnMainSync(()->{try{work.run();}catch(Throwable e){failure.set(e);}});if(failure.get()!=null)throw new RuntimeException(failure.get());}
 private static View root(MainActivity a)throws Exception{return (View)get(a,"root");}
 private static ViewGroup grid(MainActivity a)throws Exception{androidx.viewpager.widget.ViewPager pager=(androidx.viewpager.widget.ViewPager)get(a,"pager");for(int i=0;i<pager.getChildCount();i++)if(pager.getChildAt(i).getLeft()==pager.getScrollX())return (ViewGroup)pager.getChildAt(i);throw new AssertionError("Visible grid missing");}
 private static View conflict(MainActivity a)throws Exception{ViewGroup grid=grid(a);for(int i=0;i<grid.getChildCount();i++){View v=grid.getChildAt(i);if(v.findViewWithTag("conflict-badge")!=null)return v;}throw new AssertionError("Conflict card missing");}
 private static void settle(Instrumentation runner){runner.waitForIdleSync();SystemClock.sleep(400);}
 private static void screenshot(Instrumentation runner,String name)throws Exception{android.graphics.Bitmap image=runner.getUiAutomation().takeScreenshot();check(image!=null,"Screenshot unavailable");try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(runner.getTargetContext().getFilesDir(),name))){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{image.recycle();}}
 public static void run(Instrumentation runner){Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity activity=null;SharedPreferences prefs=null;Map<String,?> backup=null;
  try{
   activity=(MainActivity)runner.startActivitySync(new Intent(runner.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));MainActivity a=activity;prefs=a.getPreferences(0);backup=prefs.getAll();SharedPreferences actualPrefs=prefs;
   String fixture="1|3|4|C语言程序设计|A201|1-3\n1|3|4|C语言程序设计|A201|1\n1|3|4|大学物理|B202|1-3\n2|6|7|独立课程|C303|1-3";
   main(runner,()->{set(a,"displayChoices",new HashSet<String>());set(a,"data",fixture);set(a,"week",1);call(a,"home");});settle(runner);
   main(runner,()->{ViewGroup grid=grid(a);int cards=0;for(int i=0;i<grid.getChildCount();i++)if("course-card".equals(grid.getChildAt(i).getTag()))cards++;check(cards==2,"Conflict still renders multiple cards");View card=conflict(a);check(card.getContentDescription().toString().startsWith("C语言程序设计，"),"Default course changed");float density=a.getResources().getDisplayMetrics().density;check(card.getWidth()>=(grid.getWidth()-Math.round(34*density))/7-Math.round(4*density),"Conflict card split into lanes");});
   screenshot(runner,"qa-conflict-grid.png");
   main(runner,()->conflict(a).performClick());settle(runner);
   main(runner,()->{RadioGroup options=root(a).findViewWithTag("conflict-options");check(options.getChildCount()==2,"Duplicate candidate not removed");check(((RadioButton)options.getChildAt(0)).isChecked(),"Default option unchecked");for(int i=0;i<options.getChildCount();i++)check(((RadioButton)options.getChildAt(i)).getMaxLines()==2,"Option grows into full detail block");options.getChildAt(1).performClick();});
   screenshot(runner,"qa-conflict-picker.png");
   main(runner,a::onBackPressed);settle(runner);main(runner,()->check(conflict(a).getContentDescription().toString().startsWith("C语言程序设计，"),"Cancel saved selection"));
   main(runner,()->conflict(a).performClick());settle(runner);main(runner,()->{root(a).findViewWithTag("conflict-option-1").performClick();root(a).findViewWithTag("conflict-course-details").performClick();check((Boolean)get(a,"showingDetail"),"Course detail inaccessible");});settle(runner);main(runner,a::onBackPressed);settle(runner);
   main(runner,()->{check(root(a).findViewWithTag("conflict-options")!=null,"Detail back lost picker");root(a).findViewWithTag("conflict-option-1").performClick();root(a).findViewWithTag("save-conflict-choice").performClick();});settle(runner);
   Set<String> saved=new HashSet<>(actualPrefs.getStringSet("displayChoices",Collections.emptySet()));
   main(runner,()->{check(conflict(a).getContentDescription().toString().startsWith("大学物理，"),"Selected course not displayed");check(get(a,"data").equals(fixture),"Changing display rewrote courses");check(CourseDisplay.priority(Course.parse(fixture).get(2),saved)>0,"Choice not persisted");set(a,"week",2);call(a,"home");});settle(runner);
   main(runner,()->{check(conflict(a).getContentDescription().toString().startsWith("大学物理，"),"Global priority missing in another week");set(a,"week",1);call(a,"home");});settle(runner);
   main(runner,()->{conflict(a).performClick();set(a,"data",fixture+"\n3|1|2|后来课程|D|1");root(a).findViewWithTag("save-conflict-choice").performClick();check(actualPrefs.getStringSet("displayChoices",Collections.emptySet()).equals(saved),"Stale picker saved choice");});settle(runner);
   main(runner,a::finish);
   activity=(MainActivity)runner.startActivitySync(new Intent(runner.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));MainActivity restarted=activity;
   main(runner,()->{Set<String> restored=(Set<String>)get(restarted,"displayChoices");check(restored.equals(saved),"Reopening lost preference");check(CourseDisplay.groups(Course.parse(fixture),1,restored).get(0).selected.name.equals("大学物理"),"Reopening lost selected course");});
   result.putString("result","PASS: single full-width conflict card and badge, compact candidates, cancel/save, detail navigation, global priority across weeks, unchanged courses, stale selection rejection and preference reload");code=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("result","FAIL: "+e);}
  finally{if(activity!=null){MainActivity a=activity;runner.runOnMainSync(a::finish);}if(prefs!=null){SharedPreferences.Editor editor=prefs.edit();if(backup.containsKey("displayChoices"))editor.putStringSet("displayChoices",new HashSet<>((Set<String>)backup.get("displayChoices")));else editor.remove("displayChoices");check(editor.commit()&&prefs.getAll().equals(backup),"Original preferences not restored");}runner.finish(code,result);}
 }
}
