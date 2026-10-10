package cn.courselens;

import android.app.*;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import java.lang.reflect.*;

// Synthetic fixtures stay in memory; week navigation uses the original timetable and restores its preference.
public final class NextClassChecks {
 private static Field field(String name)throws Exception{Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);return field;}
 private static void call(MainActivity activity,String name)throws Exception{Method method=MainActivity.class.getDeclaredMethod(name);method.setAccessible(true);method.invoke(activity);}
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void run(Instrumentation runner){Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity activity=null;
  android.content.SharedPreferences prefs=null;java.util.Map<String,?> savedPreferences=null;
  try{
   activity=(MainActivity)runner.startActivitySync(new Intent(runner.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
   MainActivity a=activity;android.content.SharedPreferences activityPrefs=a.getPreferences(0);prefs=activityPrefs;savedPreferences=prefs.getAll();
   java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
   runner.runOnMainSync(()->{try{
    String original=(String)field("data").get(a),term=(String)field("term").get(a);int week=field("week").getInt(a);
    String savedCourses=a.getPreferences(0).getString("courses","");
    check(((View)field("root").get(a)).findViewWithTag("next-class-card")==null,"Home still shows next class hint");
    call(a,"menu");call(a,"home");
    check(((View)field("root").get(a)).findViewWithTag("next-class-card")==null,"Returning home recreated hint");
    field("data").set(a,original);field("term").set(a,term);field("week").setInt(a,week);call(a,"home");
    check(a.getPreferences(0).getString("courses","").equals(savedCourses),"Hint changed stored timetable");
   }catch(Throwable e){failure.set(e);}});
   if(failure.get()!=null)throw new RuntimeException(failure.get());
   runner.waitForIdleSync();
   int[] currentWeek={0};
   runner.runOnMainSync(()->{try{
    Method method=MainActivity.class.getDeclaredMethod("currentWeek");method.setAccessible(true);currentWeek[0]=(Integer)method.invoke(a);
    androidx.viewpager.widget.ViewPager pager=(androidx.viewpager.widget.ViewPager)field("pager").get(a);pager.setCurrentItem(currentWeek[0]==30?0:29,false);
    check(((View)field("root").get(a)).findViewWithTag("return-current-week")==null,"Return entry occupies home header");call(a,"menu");
    View button=((View)field("root").get(a)).findViewWithTag("return-current-week");check(button!=null&&button.isFocusable(),"Settings return to current week entry missing");button.performClick();
   }catch(Throwable e){failure.set(e);}});
   runner.waitForIdleSync();android.os.SystemClock.sleep(500);
   runner.runOnMainSync(()->{try{
    check(field("week").getInt(a)==currentWeek[0]&&((androidx.viewpager.widget.ViewPager)field("pager").get(a)).getCurrentItem()==currentWeek[0]-1,"Return to current week failed");
    check(field("pageBack").get(a)==null,"Current week did not return home");call(a,"menu");((View)field("root").get(a)).findViewWithTag("return-current-week").performClick();check(field("week").getInt(a)==currentWeek[0],"Repeated return changed week");
    check(activityPrefs.getInt("week",-1)==currentWeek[0],"Current week not saved");
   }catch(Throwable e){failure.set(e);}});
   if(failure.get()!=null)throw new RuntimeException(failure.get());
   runner.runOnMainSync(()->{try{
    java.time.LocalDate today=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"));
    field("term").set(a,today.minusDays(today.getDayOfWeek().getValue()-1).toString());field("week").setInt(a,1);
    field("data").set(a,"1|1|2|示例课程|教学楼 A101|1-30\n7|6|7|示例实验|实验楼 B202|1-30");call(a,"home");
   }catch(Throwable e){failure.set(e);}});
   if(failure.get()!=null)throw new RuntimeException(failure.get());
   runner.waitForIdleSync();android.os.SystemClock.sleep(400);
   android.graphics.Bitmap screenshot=runner.getUiAutomation().takeScreenshot();check(screenshot!=null,"Screenshot unavailable");
   try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(runner.getTargetContext().getFilesDir(),"qa-next-class.png"))){screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{screenshot.recycle();}
   int[] scrollY={0};
   runner.runOnMainSync(()->{try{
    android.widget.ScrollView scroll=(android.widget.ScrollView)field("timetableScroll").get(a);scroll.scrollTo(0,240);scrollY[0]=scroll.getScrollY();check(scrollY[0]>0,"Scroll fixture did not move");
    call(a,"menu");call(a,"home");call(a,"onPause");call(a,"onResume");check(((View)field("root").get(a)).findViewWithTag("next-class-card")==null,"Resuming recreated hint");
   }catch(Throwable e){failure.set(e);}});
   runner.waitForIdleSync();android.os.SystemClock.sleep(400);
   runner.runOnMainSync(()->{try{check(((android.widget.ScrollView)field("timetableScroll").get(a)).getScrollY()==scrollY[0],"Settings return reset scroll");check(field("week").getInt(a)==1,"Settings return changed viewed week");}catch(Throwable e){failure.set(e);}});
   if(failure.get()!=null)throw new RuntimeException(failure.get());
   result.putString("result","PASS: settings current week entry returns home, repeated return, no home next-class hint, preserved saved timetable, secondary navigation, lifecycle and scroll return");code=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("result","FAIL: "+e);}
  finally{if(activity!=null){MainActivity a=activity;runner.runOnMainSync(a::finish);}if(prefs!=null){android.content.SharedPreferences.Editor editor=prefs.edit();if(savedPreferences.containsKey("week"))editor.putInt("week",(Integer)savedPreferences.get("week"));else editor.remove("week");for(String key:new String[]{"courses","notes","term"})if(savedPreferences.containsKey(key))editor.putString(key,(String)savedPreferences.get(key));else editor.remove(key);check(editor.commit()&&prefs.getAll().equals(savedPreferences),"Original preferences not restored");}runner.finish(code,result);}
 }
}
