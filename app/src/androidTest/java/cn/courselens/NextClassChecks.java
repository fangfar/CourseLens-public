package cn.courselens;

import android.app.*;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import java.lang.reflect.*;

// QA fixtures and navigation preferences are restored in finally.
public final class NextClassChecks {
 private static Field field(String name)throws Exception{Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);return field;}
 private static void call(MainActivity activity,String name)throws Exception{Method method=MainActivity.class.getDeclaredMethod(name);method.setAccessible(true);method.invoke(activity);}
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void run(Instrumentation runner){Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity activity=null;
  android.content.SharedPreferences prefs=null;java.util.Map<String,?> savedPreferences=null;
  try{
   check(runner.getTargetContext().getPackageName().endsWith(".qa"),"Use isolated QA package");activity=NativeChecks.startQaActivity(runner);
   MainActivity a=activity;android.content.SharedPreferences activityPrefs=a.getPreferences(0);prefs=activityPrefs;savedPreferences=prefs.getAll();
   java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
   int initialWeek=field("week").getInt(a);
   for(int viewed:new int[]{1,7,15,30}){
    runner.runOnMainSync(()->{try{field("week").setInt(a,viewed);call(a,"home");}catch(Throwable e){failure.set(e);}});
    runner.waitForIdleSync();android.os.SystemClock.sleep(400);
    runner.runOnMainSync(()->{try{View host=((View)field("root").get(a)).findViewWithTag("week-days-viewport"),row=host.findViewWithTag("week-days-"+viewed);check(row!=null&&row.getWidth()==host.getWidth()&&Math.abs(row.getTranslationX())<=1,"Date header invisible at week "+viewed+" hostWidth="+host.getWidth()+" rowWidth="+(row==null?-1:row.getWidth())+" x="+(row==null?0:row.getTranslationX()));}catch(Throwable e){failure.set(e);}});
    if(failure.get()!=null)throw new RuntimeException(failure.get());
   }
   runner.runOnMainSync(()->{try{field("week").setInt(a,initialWeek);call(a,"home");}catch(Throwable e){failure.set(e);}});runner.waitForIdleSync();android.os.SystemClock.sleep(400);
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
    java.time.LocalDate today=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"));String term=(String)field("term").get(a);
    field("term").set(a,today.minusDays(today.getDayOfWeek().getValue()-1).minusWeeks(14).toString());field("week").setInt(a,15);call(a,"home");
    View home=(View)field("root").get(a),dates=home.findViewWithTag("home-date"),button=home.findViewWithTag("return-current-week"),more=home.findViewWithTag("quick-menu-button");
    check(!dates.isClickable()&&!dates.hasOnClickListeners(),"Date still opens week picker");
    android.view.ViewGroup toolbar=(android.view.ViewGroup)button.getParent();check(toolbar.indexOfChild(button)+1==toolbar.indexOfChild(more),"Return button is not left of more");
    androidx.viewpager.widget.ViewPager testPager=(androidx.viewpager.widget.ViewPager)field("pager").get(a);int[] offsets={0,-1,1,-2,2},expected={View.GONE,View.GONE,View.GONE,View.VISIBLE,View.VISIBLE};for(int i=0;i<offsets.length;i++){testPager.setCurrentItem(14+offsets[i],false);check(button.getVisibility()==expected[i],"Wrong return button visibility at offset "+offsets[i]);}
    field("term").set(a,term);call(a,"home");androidx.viewpager.widget.ViewPager pager=(androidx.viewpager.widget.ViewPager)field("pager").get(a);pager.setCurrentItem(currentWeek[0]<=15?29:0,false);
    button=((View)field("root").get(a)).findViewWithTag("return-current-week");check(button.getVisibility()==View.VISIBLE&&button.isFocusable(),"Far week return button missing");button.performClick();
   }catch(Throwable e){failure.set(e);}});
   runner.waitForIdleSync();android.os.SystemClock.sleep(500);
   runner.runOnMainSync(()->{try{
    check(field("week").getInt(a)==currentWeek[0]&&((androidx.viewpager.widget.ViewPager)field("pager").get(a)).getCurrentItem()==currentWeek[0]-1,"Return to current week failed");
    check(field("pageBack").get(a)==null,"Current week did not return home");check(((View)field("root").get(a)).findViewWithTag("return-current-week").getVisibility()==View.GONE,"Return button still visible this week");
    call(a,"menu");View settings=(View)field("root").get(a);check(settings.findViewWithTag("return-current-week")==null,"Duplicate return entry in settings");check(settings.findViewWithTag("select-teaching-week")==null,"Temporary week-selection entry is still visible");a.onBackPressed();
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
   runner.runOnMainSync(()->{try{
    field("term").set(a,"2026-11-30");field("week").setInt(a,1);call(a,"home");
   }catch(Throwable e){failure.set(e);}});runner.waitForIdleSync();android.os.SystemClock.sleep(400);
   runner.runOnMainSync(()->{try{
    androidx.viewpager.widget.ViewPager p=(androidx.viewpager.widget.ViewPager)field("pager").get(a);View header=((View)field("root").get(a)).findViewWithTag("week-days-viewport");
    android.view.ViewGroup first=header.findViewWithTag("week-days-1"),next=header.findViewWithTag("week-days-2");
    check(first.getWidth()==p.getWidth()&&next.getWidth()==p.getWidth()&&first.getChildAt(1).getWidth()>0&&first.getChildAt(1).getHeight()>0,"Week header not measured");
    check(((TextView)first.getChildAt(0)).getText().toString().equals("11\n月")&&((TextView)next.getChildAt(0)).getText().toString().equals("12\n月"),"Adjacent week months incorrect");
    check(((TextView)first.getChildAt(1)).getText().toString().equals("一\n11/30")&&((TextView)next.getChildAt(1)).getText().toString().equals("一\n12/7"),"Adjacent dates incorrect");
    check(p.beginFakeDrag(),"Week drag did not begin");p.fakeDragBy(-p.getWidth()/2f);check(p.getScrollX()>0&&p.getScrollX()<p.getWidth(),"Week did not reach partial drag");
    for(int w:new int[]{1,2}){View days=header.findViewWithTag("week-days-"+w),grid=p.findViewWithTag("week-grid-"+w);check(Math.abs(days.getTranslationX()-(grid.getLeft()-p.getScrollX()))<=1,"Date row does not follow grid during drag");}
    check(((View)field("root").get(a)).findViewWithTag("home-date").getTranslationX()==0,"Toolbar moved with week drag");
   }catch(Throwable e){failure.set(e);}});
   if(failure.get()!=null)throw new RuntimeException(failure.get());runner.waitForIdleSync();android.os.SystemClock.sleep(350);
   android.graphics.Bitmap dragging=runner.getUiAutomation().takeScreenshot();check(dragging!=null,"Drag screenshot unavailable");try(java.io.FileOutputStream out=runner.getTargetContext().openFileOutput("qa-week-header-drag.png",0)){dragging.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{dragging.recycle();}
   runner.runOnMainSync(()->{try{androidx.viewpager.widget.ViewPager p=(androidx.viewpager.widget.ViewPager)field("pager").get(a);p.endFakeDrag();p.setCurrentItem(1,false);}catch(Throwable e){failure.set(e);}});runner.waitForIdleSync();android.os.SystemClock.sleep(400);
   runner.runOnMainSync(()->{try{
    View header=((View)field("root").get(a)).findViewWithTag("week-days-viewport"),row=header.findViewWithTag("week-days-2");check(row.getTranslationX()==0,"Selected date row is not aligned");
    int[] before=new int[2],after=new int[2];header.getLocationInWindow(before);((android.widget.ScrollView)field("timetableScroll").get(a)).scrollTo(0,240);header.getLocationInWindow(after);check(before[1]==after[1],"Date row moved with vertical scroll");
    ((androidx.viewpager.widget.ViewPager)field("pager").get(a)).setCurrentItem(0,false);
   }catch(Throwable e){failure.set(e);}});runner.waitForIdleSync();android.os.SystemClock.sleep(400);
   int[] scrollY={0};
   runner.runOnMainSync(()->{try{
    android.widget.ScrollView scroll=(android.widget.ScrollView)field("timetableScroll").get(a);scroll.scrollTo(0,240);scrollY[0]=scroll.getScrollY();check(scrollY[0]>0,"Scroll fixture did not move");
    call(a,"menu");call(a,"home");Method pause=Activity.class.getDeclaredMethod("onPause");pause.setAccessible(true);pause.invoke(a);call(a,"onResume");check(((View)field("root").get(a)).findViewWithTag("next-class-card")==null,"Resuming recreated hint");
   }catch(Throwable e){failure.set(e);}});
   runner.waitForIdleSync();android.os.SystemClock.sleep(400);
   runner.runOnMainSync(()->{try{check(((android.widget.ScrollView)field("timetableScroll").get(a)).getScrollY()==scrollY[0],"Settings return reset scroll");check(field("week").getInt(a)==1,"Settings return changed viewed week");}catch(Throwable e){failure.set(e);}});
   if(failure.get()!=null)throw new RuntimeException(failure.get());
   result.putString("result","PASS: home return icon left of more, both-direction 0/1/2-week visibility boundary, return/hide, hidden direct week selection, read-only date, no home next-class hint, preserved saved timetable, secondary navigation, lifecycle, month/date row follows partial horizontal drag, vertically pinned header and scroll return");code=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("result","FAIL: "+e);}
  finally{if(activity!=null){MainActivity a=activity;runner.runOnMainSync(a::finish);}if(prefs!=null){android.content.SharedPreferences.Editor editor=prefs.edit();if(savedPreferences.containsKey("week"))editor.putInt("week",(Integer)savedPreferences.get("week"));else editor.remove("week");for(String key:new String[]{"courses","notes","term"})if(savedPreferences.containsKey(key))editor.putString(key,(String)savedPreferences.get(key));else editor.remove(key);check(editor.commit()&&prefs.getAll().equals(savedPreferences),"Original preferences not restored");}runner.finish(code,result);}
 }
}
