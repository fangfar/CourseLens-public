package cn.courselens;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.ScrollView;
import androidx.viewpager.widget.ViewPager;
import java.lang.reflect.*;
import java.util.*;

// Dispatch through the real decor -> ScrollView -> ViewPager -> card hierarchy.
final class TimetableGestureChecks {
 private final Instrumentation runner;
 private final MainActivity activity;
 private final View decor;
 private final ScrollView scroll;
 private final ViewPager pager;
 private final int slop;
 private long downTime;
 private float x,y;
 private TimetableGestureChecks(Instrumentation runner,MainActivity activity)throws Exception{
  this.runner=runner;this.activity=activity;decor=activity.getWindow().getDecorView();scroll=(ScrollView)get("timetableScroll");pager=(ViewPager)get("pager");slop=ViewConfiguration.get(activity).getScaledTouchSlop();
 }
 private static Field field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f;}
 private Object get(String name)throws Exception{return field(name).get(activity);}
 private void main(Runnable work){main(runner,work);}
 private static void main(Instrumentation runner,Runnable work){java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();runner.runOnMainSync(()->{try{work.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new RuntimeException(error.get());}
 private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 private void report(String name){Bundle progress=new Bundle();progress.putString("gesture",name+": PASS");runner.sendStatus(0,progress);}
 private void reset(int week)throws Exception{
  main(()->{MotionEvent stop=MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),MotionEvent.ACTION_DOWN,0,0,0);try{scroll.onTouchEvent(stop);stop.setAction(MotionEvent.ACTION_CANCEL);scroll.onTouchEvent(stop);}finally{stop.recycle();}pager.setCurrentItem(week-1,false);scroll.scrollTo(0,0);});runner.waitForIdleSync();SystemClock.sleep(300);
  int[] location=new int[2];main(()->scroll.getLocationInWindow(location));x=pager.getWidth()*.8f;y=location[1]+Math.min(scroll.getHeight()/3f,200);downTime=SystemClock.uptimeMillis();
 }
 private void event(int action,float dx,float dy){
  main(()->{MotionEvent e=MotionEvent.obtain(downTime,SystemClock.uptimeMillis(),action,x+dx,y+dy,0);e.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);try{decor.dispatchTouchEvent(e);}finally{e.recycle();}});
 }
 private void move(float dx,float dy){SystemClock.sleep(16);event(MotionEvent.ACTION_MOVE,dx,dy);}
 private void settle(){runner.waitForIdleSync();SystemClock.sleep(650);runner.waitForIdleSync();}
 private void card(){
  main(()->{ViewGroup grid=null;for(int i=0;i<pager.getChildCount();i++)if("week-grid-6".equals(pager.getChildAt(i).getTag()))grid=(ViewGroup)pager.getChildAt(i);check(grid!=null,"course grid missing");View card=grid.findViewWithTag("course-card");check(card!=null,"synthetic course missing: width="+grid.getWidth()+", children="+grid.getChildCount()+", attached="+pager.isAttachedToWindow());int[] location=new int[2];card.getLocationInWindow(location);x=location[0]+card.getWidth()/2f;y=location[1]+card.getHeight()/2f;downTime=SystemClock.uptimeMillis();});
 }
 private void horizontal(float dy,int duration,String name)throws Exception{
  reset(6);int startX=pager.getScrollX();event(MotionEvent.ACTION_DOWN,0,0);
  int steps=Math.max(2,duration/16);
  for(int i=1;i<=steps;i++){SystemClock.sleep(duration/steps);event(MotionEvent.ACTION_MOVE,-pager.getWidth()*.6f*i/steps,dy*i/steps);}
  check(scroll.getScrollY()==0,name+": parent stole horizontal gesture");check(pager.getScrollX()>startX,name+": pager did not follow finger");
  event(MotionEvent.ACTION_UP,-pager.getWidth()*.6f,dy);settle();check(pager.getCurrentItem()==6,name+": week did not advance: item="+pager.getCurrentItem()+", x="+pager.getScrollX()+", start="+startX+", width="+pager.getWidth());check((int)get("week")==7,name+": week header did not advance");report(name);
 }
 private void vertical(float dx,String name)throws Exception{
  reset(6);int startX=pager.getScrollX();event(MotionEvent.ACTION_DOWN,0,0);move(dx/4,-slop*2);move(dx,-150);event(MotionEvent.ACTION_UP,dx,-150);settle();check(scroll.getScrollY()>0,name+": vertical scroll did not move");check(pager.getScrollX()==startX&&pager.getCurrentItem()==5,name+": pager stole vertical gesture");report(name);
 }
 private void run()throws Exception{
  reset(6);event(MotionEvent.ACTION_DOWN,0,0);move(-slop*1.5f,-slop*.5f);move(-slop*2,-slop*5);move(-slop*3,-150);
  check(scroll.getScrollY()==0,"horizontal start then vertical drift: ScrollView stole locked gesture");event(MotionEvent.ACTION_CANCEL,-slop*3,-150);settle();report("horizontal direction stays locked; CANCEL");
  horizontal(0,400,"horizontal drag and settle");horizontal(-pager.getWidth()*.4f,400,"horizontal diagonal");horizontal(0,32,"fast horizontal swipe");
  reset(6);card();event(MotionEvent.ACTION_DOWN,0,0);for(int i=1;i<=10;i++)move(-pager.getWidth()*.7f*i/10,-pager.getWidth()*.45f*i/10);check(scroll.getScrollY()==0&&pager.getScrollX()>0,"card diagonal did not follow horizontally");event(MotionEvent.ACTION_UP,-pager.getWidth()*.7f,-pager.getWidth()*.45f);settle();check(pager.getCurrentItem()==6&&get("navigationOverlay")==null,"card diagonal opened detail or failed to page");report("horizontal diagonal starting on course card");
  reset(6);event(MotionEvent.ACTION_DOWN,0,0);for(int i=1;i<=4;i++)move(-pager.getWidth()*.2f*i/4,0);check(pager.getCurrentItem()==5,"short fling changed week before UP");event(MotionEvent.ACTION_UP,-pager.getWidth()*.2f,0);settle();check(pager.getCurrentItem()==6&&scroll.getScrollY()==0,"short fast swipe lost native fling");report("short swipe advances by velocity");
  reset(6);event(MotionEvent.ACTION_DOWN,0,0);move(-slop*.4f,0);check(scroll.getScrollY()==0&&pager.getScrollX()==0,"sub-slop movement scrolled");move(-slop*.5f,-slop*3);move(-slop*.5f,-150);event(MotionEvent.ACTION_UP,-slop*.5f,-150);settle();check(scroll.getScrollY()>0&&pager.getCurrentItem()==5,"tiny horizontal movement prematurely locked direction");report("sub-slop jitter remains undecided");
  vertical(0,"vertical drag and fling");vertical(-70,"vertical diagonal");
  reset(6);main(()->scroll.scrollTo(0,200));int initialY=scroll.getScrollY();event(MotionEvent.ACTION_DOWN,0,0);move(0,slop*3);move(0,120);event(MotionEvent.ACTION_UP,0,120);settle();check(scroll.getScrollY()<initialY&&pager.getCurrentItem()==5,"downward scroll failed");report("downward vertical scroll");
  reset(6);event(MotionEvent.ACTION_DOWN,0,0);move(-slop*.5f,-slop*2);move(-pager.getWidth()*.6f,-slop*3);event(MotionEvent.ACTION_UP,-pager.getWidth()*.6f,-slop*3);settle();check(pager.getCurrentItem()==5,"vertical start switched to horizontal");report("vertical direction stays locked; UP");
  reset(6);main(()->pager.setCurrentItem(6,true));event(MotionEvent.ACTION_DOWN,0,0);move(0,-slop*3);move(0,-150);event(MotionEvent.ACTION_UP,0,-150);settle();check(scroll.getScrollY()>0&&pager.getCurrentItem()==6,"settling pager blocked vertical scrolling");report("vertical gesture during pager settling");
  reset(6);event(MotionEvent.ACTION_DOWN,0,0);move(0,-slop*3);move(0,-150);event(MotionEvent.ACTION_UP,0,-150);SystemClock.sleep(16);downTime=SystemClock.uptimeMillis();event(MotionEvent.ACTION_DOWN,0,0);int stoppedY=scroll.getScrollY();move(-slop*3,0);for(int i=1;i<=8;i++)move(-pager.getWidth()*.7f*i/8,-slop*2);check(scroll.getScrollY()==stoppedY,"vertical fling continued during horizontal gesture");event(MotionEvent.ACTION_UP,-pager.getWidth()*.7f,-slop*2);settle();check(pager.getCurrentItem()==6,"vertical fling prevented horizontal paging");report("horizontal gesture stops vertical fling");
  for(int week:new int[]{1,30}){
   reset(week);float outward=week==1?pager.getWidth()*.15f:-pager.getWidth()*.6f;event(MotionEvent.ACTION_DOWN,0,0);move(outward/3,-slop*2);move(outward,-100);event(MotionEvent.ACTION_UP,outward,-100);settle();check(pager.getCurrentItem()==week-1&&scroll.getScrollY()==0,"boundary switched to vertical or escaped week range: week="+week+", item="+pager.getCurrentItem()+", y="+scroll.getScrollY());
   reset(week);float inward=week==1?-pager.getWidth()*.75f:pager.getWidth()*.7f;x=week==1?pager.getWidth()*.8f:pager.getWidth()*.2f;event(MotionEvent.ACTION_DOWN,0,0);for(int i=1;i<=6;i++)move(inward*i/6,0);event(MotionEvent.ACTION_UP,inward,0);settle();check(pager.getCurrentItem()==(week==1?1:28),"boundary inward swipe failed: week="+week+", item="+pager.getCurrentItem());
   reset(week);event(MotionEvent.ACTION_DOWN,0,0);move(0,-slop*3);move(0,-150);event(MotionEvent.ACTION_UP,0,-150);settle();check(scroll.getScrollY()>0&&pager.getCurrentItem()==week-1,"boundary vertical scroll failed");report("week "+week+" outward/inward/vertical");
  }
  // UP/CANCEL must leave the next gesture free to choose either axis.
  reset(6);event(MotionEvent.ACTION_DOWN,0,0);move(-slop*3,0);event(MotionEvent.ACTION_CANCEL,-slop*3,0);settle();vertical(0,"vertical after horizontal cancel");horizontal(0,300,"horizontal after vertical up");
  for(boolean jitter:new boolean[]{false,true}){
   reset(6);card();event(MotionEvent.ACTION_DOWN,0,0);if(jitter)move(slop*.4f,slop*.4f);event(MotionEvent.ACTION_UP,jitter?slop*.4f:0,jitter?slop*.4f:0);settle();check(get("navigationOverlay")!=null,"tap did not open course detail, jitter="+jitter);check(pager.getCurrentItem()==5&&scroll.getScrollY()==0,"tap changed week or scroll");main(activity::onBackPressed);settle();report(jitter?"course tap with sub-slop movement":"course tap");
  }
 }
 static void run(Instrumentation runner){
  Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity activity=null;SharedPreferences prefs=null;Map<String,?> backup=null;
  try{
   check(runner.getTargetContext().getPackageName().endsWith(".qa"),"Use isolated QA package");activity=NativeChecks.startQaActivity(runner);MainActivity a=activity;prefs=a.getPreferences(0);backup=prefs.getAll();
   main(runner,()->{try{a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);field("data").set(a,"5|2|3|Gesture fixture|Room|1-30");field("adjustments").set(a,"");field("notes").set(a,"");field("displayChoices").set(a,new HashSet<String>());field("week").setInt(a,6);field("timetableScrollY").setInt(a,0);Method home=MainActivity.class.getDeclaredMethod("home");home.setAccessible(true);home.invoke(a);}catch(Exception e){throw new RuntimeException(e);}});runner.waitForIdleSync();SystemClock.sleep(400);
   new TimetableGestureChecks(runner,a).run();result.putString("result","PASS: direction lock, touch slop, diagonal/fast swipes, follow/settle, taps, UP/CANCEL reset, week 1/30 boundaries");code=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("error",android.util.Log.getStackTraceString(e));}
  finally{
   if(prefs!=null&&backup!=null){SharedPreferences.Editor edit=prefs.edit().clear();for(Map.Entry<String,?> entry:backup.entrySet()){Object v=entry.getValue();if(v instanceof String)edit.putString(entry.getKey(),(String)v);else if(v instanceof Integer)edit.putInt(entry.getKey(),(Integer)v);else if(v instanceof Boolean)edit.putBoolean(entry.getKey(),(Boolean)v);else if(v instanceof Long)edit.putLong(entry.getKey(),(Long)v);else if(v instanceof Float)edit.putFloat(entry.getKey(),(Float)v);else if(v instanceof Set)edit.putStringSet(entry.getKey(),new HashSet<>((Set<String>)v));}if(!edit.commit()||!prefs.getAll().equals(backup)){code=Activity.RESULT_CANCELED;result.putString("restoreError","Preferences restore failed");}}
   if(activity!=null){MainActivity a=activity;main(runner,a::finish);}runner.finish(code,result);
  }
 }
}
