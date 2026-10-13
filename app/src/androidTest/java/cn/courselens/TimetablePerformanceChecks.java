package cn.courselens;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import androidx.viewpager.widget.ViewPager;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

// Synthetic fixtures only; the same harness measures the baseline and the optimized APK.
final class TimetablePerformanceChecks {
 private static Field field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f;}
 private static Object call(MainActivity a,String name,Class<?>[] types,Object... args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(a,args);}
 private static Object call(MainActivity a,String name)throws Exception{return call(a,name,new Class<?>[]{});}
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 private static void main(Instrumentation runner,Runnable work){java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();runner.runOnMainSync(()->{try{work.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new RuntimeException(error.get());}
 private static double p(List<Long> values,double fraction){List<Long> sorted; synchronized(values){sorted=new ArrayList<>(values);}Collections.sort(sorted);return sorted.isEmpty()?0:sorted.get(Math.max(0,(int)Math.ceil(sorted.size()*fraction)-1))/1e6;}
 private static void report(Instrumentation runner,String name,List<Long> times){Bundle b=new Bundle();b.putString(name,String.format(Locale.ROOT,"n=%d p50=%.3fms p95=%.3fms max=%.3fms",times.size(),p(times,.5),p(times,.95),p(times,1)));runner.sendStatus(0,b);}
 private static ViewGroup selected(MainActivity a)throws Exception{ViewPager pager=(ViewPager)field("pager").get(a);for(int i=0;i<pager.getChildCount();i++)if(pager.getChildAt(i).getLeft()==pager.getScrollX()&&("week-grid-"+(pager.getCurrentItem()+1)).equals(pager.getChildAt(i).getTag()))return (ViewGroup)pager.getChildAt(i);return null;}
 private static void draw(Instrumentation runner,MainActivity a,Runnable action,List<Long> times)throws Exception{
  CountDownLatch drawn=new CountDownLatch(1);long[] start={0};View decor=a.getWindow().getDecorView();
  ViewTreeObserver.OnDrawListener listener=()->{try{ViewGroup grid=selected(a);if(start[0]!=0&&grid!=null&&(grid.findViewWithTag("course-card")!=null||grid.findViewWithTag("timetable-empty-message")!=null)&&drawn.getCount()>0){times.add(System.nanoTime()-start[0]);drawn.countDown();}}catch(Exception e){throw new RuntimeException(e);}};
  main(runner,()->{decor.getViewTreeObserver().addOnDrawListener(listener);start[0]=System.nanoTime();action.run();});
  check(drawn.await(20,TimeUnit.SECONDS),"Selected week content did not draw");
  main(runner,()->decor.getViewTreeObserver().removeOnDrawListener(listener));runner.waitForIdleSync();SystemClock.sleep(400);
 }
 static void run(Instrumentation runner,boolean optimized){
  Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity a=null;SharedPreferences prefs=null;Map<String,?> backup=null;
  HandlerThread collector=new HandlerThread("timetable-frames");collector.start();List<Long> frames=Collections.synchronizedList(new ArrayList<>());
  Window.OnFrameMetricsAvailableListener listener=(window,metrics,dropped)->frames.add(metrics.getMetric(FrameMetrics.TOTAL_DURATION));
  try{
   check(runner.getTargetContext().getPackageName().endsWith(".qa"),"Use isolated QA package");Bundle progress=new Bundle();progress.putString("phase","start");runner.sendStatus(0,progress);a=NativeChecks.startQaActivity(runner);progress.putString("phase","fixture");runner.sendStatus(0,progress);MainActivity activity=a;prefs=a.getPreferences(0);backup=prefs.getAll();
   StringBuilder fixture=new StringBuilder();for(int day=1;day<=7;day++)for(int slot=1;slot<=11;slot+=2)fixture.append(day+"|"+slot+"|"+(slot+1)+"|Perf "+day+"-"+slot+"|Room|1-30|Teacher|Experiment\n");for(int day=1;day<=7;day++)fixture.append(day+"|1|2|Conflict "+day+"|Other|1-30\n");String data=fixture.toString();
   List<Course> raw=Course.parse(data);List<String> changes=new ArrayList<>();for(int i=0;i<8;i++){Course c=raw.get(i);changes.add(new CourseAdjustment("2026-08-31",6,c,i%2==0?null:new Course(c.day,c.start,c.end,c.name,"Moved room","7",c.teacher,c.details,c.className)).encode());}String adjustments=String.join("\n",changes);
   main(runner,()->{try{activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);field("data").set(activity,data);field("term").set(activity,"2026-08-31");field("adjustments").set(activity,adjustments);field("displayChoices").set(activity,new HashSet<String>());field("notes").set(activity,"");field("week").setInt(activity,6);call(activity,"home");}catch(Exception e){throw new RuntimeException(e);}});runner.waitForIdleSync();SystemClock.sleep(500);
   List<Long> effective=new ArrayList<>(),markers=new ArrayList<>(),widgets=new ArrayList<>();
   main(runner,()->{try{for(int i=0;i<8;i++){long start=System.nanoTime();call(activity,"effectiveCourses");effective.add(System.nanoTime()-start);start=System.nanoTime();for(Course c:raw)call(activity,"movedAdjustment",new Class<?>[]{Course.class,int.class},c,6);markers.add(System.nanoTime()-start);start=System.nanoTime();CourseWidget.updateAll(activity);widgets.add(System.nanoTime()-start);}}catch(Exception e){throw new RuntimeException(e);}});
   report(runner,"effectiveCourses",effective);report(runner,"markers_49_cards",markers);report(runner,"updateAll_widgets_"+CourseWidget.count(activity),widgets);
   List<Long> returns=new ArrayList<>(),swipes=new ArrayList<>(),fresh=new ArrayList<>(),returnFrames=Collections.synchronizedList(new ArrayList<>()),swipeFrames=Collections.synchronizedList(new ArrayList<>()),freshFrames=Collections.synchronizedList(new ArrayList<>());
   a.getWindow().addOnFrameMetricsAvailableListener(listener,new Handler(collector.getLooper()));
   for(int i=0;i<10;i++){
    Object retained=field("pager").get(activity);main(runner,()->{try{call(activity,"menu");}catch(Exception e){throw new RuntimeException(e);}});runner.waitForIdleSync();SystemClock.sleep(400);frames.clear();
    draw(runner,activity,()->activity.onBackPressed(),returns);if(optimized){check(retained==field("pager").get(activity),"Return rebuilt unchanged ViewPager");main(runner,()->{try{check(((ViewGroup)field("stage").get(activity)).getChildCount()==1,"Return retained an outgoing page");check(((View)field("root").get(activity)).getImportantForAccessibility()!=View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,"Reused home is inaccessible");}catch(Exception e){throw new RuntimeException(e);}});}synchronized(frames){returnFrames.addAll(frames);}
    frames.clear();final int target=i%2==0?7:6;draw(runner,activity,()->{try{((ViewPager)field("pager").get(activity)).setCurrentItem(target-1,true);}catch(Exception e){throw new RuntimeException(e);}},swipes);synchronized(frames){swipeFrames.addAll(frames);}
   }
   for(int i=0;i<6;i++){frames.clear();draw(runner,activity,()->{try{call(activity,"home");}catch(Exception e){throw new RuntimeException(e);}},fresh);synchronized(frames){freshFrames.addAll(frames);}}
   report(runner,"return_content_draw",returns);report(runner,"swipe_target_draw",swipes);report(runner,"home_content_draw",fresh);report(runner,"return_frames",returnFrames);report(runner,"swipe_frames",swipeFrames);report(runner,"home_frames",freshFrames);
   // Invalidate each input independently and compare with the authoritative uncached model.
   main(runner,()->{try{for(String name:new String[]{"data","adjustments","term"}){Object original=field(name).get(activity);field(name).set(activity,name.equals("data")?data+"1|3|4|Edited|New|30":name.equals("adjustments")?"":"2027-02-22");List<Course> expected=CourseAdjustment.apply(Course.parse((String)field("data").get(activity)),(String)field("term").get(activity),CourseAdjustment.parse((String)field("adjustments").get(activity)));List<Course> actual=(List<Course>)call(activity,"effectiveCourses");check(encode(expected).equals(encode(actual)),"Stale effective courses after "+name);field(name).set(activity,original);}
    for(int week:new int[]{1,6,7,30}){field("week").setInt(activity,week);call(activity,"home");}
   }catch(Exception e){throw new RuntimeException(e);}});runner.waitForIdleSync();SystemClock.sleep(400);
   if(optimized){
    for(String name:new String[]{"data","adjustments","term","notes","displayChoices"}){
     Object original=field(name).get(activity);Object[] oldPager={null};
     main(runner,()->{try{field("week").setInt(activity,7);call(activity,"home");oldPager[0]=field("pager").get(activity);call(activity,"menu");
      Object changed=name.equals("data")?"1|3|4|Edited|New room|7":name.equals("adjustments")?"":name.equals("term")?"2027-02-22":name.equals("notes")?"Edited note":new HashSet<>(Collections.singleton("priority:1:Conflict 1||"));field(name).set(activity,changed);
     }catch(Exception e){throw new RuntimeException(e);}});runner.waitForIdleSync();SystemClock.sleep(400);
     draw(runner,activity,activity::onBackPressed,new ArrayList<>());
     main(runner,()->{try{check(oldPager[0]!=field("pager").get(activity),"Reused stale home after "+name);checkRendered(activity,7);field(name).set(activity,original);}catch(Exception e){throw new RuntimeException(e);}});
    }
    for(int shownWeek:new int[]{1,6,7,30}){
     draw(runner,activity,()->{try{field("week").setInt(activity,shownWeek);call(activity,"home");}catch(Exception e){throw new RuntimeException(e);}},new ArrayList<>());
     main(runner,()->{try{checkRendered(activity,shownWeek);}catch(Exception e){throw new RuntimeException(e);}});
    }
   }
   if(optimized)check(p(returns,.95)<100,"Return content p95 exceeds 100ms");
   result.putString("result","PASS: synthetic return/paging/content-draw measurements and course/adjustment/term invalidation");code=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("error",android.util.Log.getStackTraceString(e));}
  finally{if(a!=null){a.getWindow().removeOnFrameMetricsAvailableListener(listener);if(prefs!=null&&backup!=null){SharedPreferences.Editor edit=prefs.edit().clear();for(Map.Entry<String,?> entry:backup.entrySet()){Object v=entry.getValue();if(v instanceof String)edit.putString(entry.getKey(),(String)v);else if(v instanceof Integer)edit.putInt(entry.getKey(),(Integer)v);else if(v instanceof Boolean)edit.putBoolean(entry.getKey(),(Boolean)v);else if(v instanceof Long)edit.putLong(entry.getKey(),(Long)v);else if(v instanceof Float)edit.putFloat(entry.getKey(),(Float)v);else if(v instanceof Set)edit.putStringSet(entry.getKey(),new HashSet<>((Set<String>)v));}check(edit.commit()&&prefs.getAll().equals(backup),"Preferences restore failed");}MainActivity activity=a;main(runner,activity::finish);}collector.quitSafely();runner.finish(code,result);}
 }
 private static String encode(List<Course> courses){List<String> rows=new ArrayList<>();for(Course c:courses)rows.add(c.encode());return String.join("\n",rows);}
 private static void checkRendered(MainActivity a,int week)throws Exception{
  List<Course> effective=CourseAdjustment.apply(Course.parse((String)field("data").get(a)),(String)field("term").get(a),CourseAdjustment.parse((String)field("adjustments").get(a)));
  List<CourseDisplay.Group> expected=CourseDisplay.groups(effective,week,(Set<String>)field("displayChoices").get(a));ViewGroup grid=selected(a);check(grid!=null,"Selected grid missing");List<View> cards=new ArrayList<>();
  for(int i=0;i<grid.getChildCount();i++)if("course-card".equals(grid.getChildAt(i).getTag()))cards.add(grid.getChildAt(i));check(cards.size()==expected.size(),"Stale card count");
  for(int i=0;i<cards.size();i++){Course c=expected.get(i).selected;check(cards.get(i).getContentDescription().toString().startsWith(c.name+"，星期"+c.day+"，第"+c.start+"到"+c.end+"节，"+c.room),"Stale selected course/room");check(cards.get(i).getWidth()>0&&cards.get(i).getRight()<=grid.getWidth(),"Card layout invalid");}
 }
}
