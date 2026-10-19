package cn.courselens;

import android.app.*;
import android.os.*;
import android.view.*;
import androidx.viewpager.widget.ViewPager;
import java.lang.reflect.*;
import java.util.*;

// Cold task-page construction, then the first physical drag through the real hierarchy.
final class CapsuleChecks {
 private interface Work {void run()throws Exception;}
 private static void main(Instrumentation runner,Work work){java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();runner.runOnMainSync(()->{try{work.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new RuntimeException(error.get());}
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 private static Field field(String name)throws Exception{Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);return field;}
 private static void call(MainActivity activity,String name)throws Exception{Method method=MainActivity.class.getDeclaredMethod(name);method.setAccessible(true);method.invoke(activity);}
 private static float position(ViewPager pager){View first=pager.findViewWithTag("task-filter-page-0");return (float)(pager.getScrollX()-first.getLeft())/pager.getWidth();}
 private static void settle(Instrumentation runner,ViewPager pager){for(int i=0;i<100;i++){boolean[] done={false};main(runner,()->done[0]=Math.abs(position(pager)-pager.getCurrentItem())*pager.getWidth()<=2);if(done[0]){runner.waitForIdleSync();return;}SystemClock.sleep(20);}throw new AssertionError("Pager did not settle: current="+pager.getCurrentItem()+", position="+position(pager));}
 private static void event(Instrumentation runner,MainActivity activity,long down,int action,float x,float y){main(runner,()->{MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);try{activity.getWindow().getDecorView().dispatchTouchEvent(event);}finally{event.recycle();}});}
 static void run(Instrumentation runner){
  Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity activity=null;List<String> trace=new ArrayList<>();
  try{
   check(runner.getTargetContext().getPackageName().endsWith(".qa"),"QA only");activity=NativeChecks.startQaActivity(runner);MainActivity a=activity;TaskPage[] page=new TaskPage[1];ViewPager[] pager=new ViewPager[1];View[] pill=new View[1],tabs=new View[1];float[] worst={0};int[] frames={0};
   main(runner,()->{a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);field("remoteResumed").setBoolean(a,false);Bundle state=new Bundle();state.putInt("filter",1);field("taskState").set(a,state);call(a,"tasks");page[0]=(TaskPage)field("root").get(a);pager[0]=page[0].findViewWithTag("task-filter-pager");pill[0]=page[0].findViewWithTag("task-filter-capsule");tabs[0]=(View)page[0].findViewWithTag("task-filter-0").getParent();page[0].getViewTreeObserver().addOnPreDrawListener(()->{if(pager[0].getWidth()>0&&tabs[0].getWidth()>0){float position=position(pager[0]),expected=position*tabs[0].getWidth()/3,error=Math.abs(pill[0].getTranslationX()-expected);worst[0]=Math.max(worst[0],error);if(frames[0]<8||error>1.5f)trace.add("frame="+frames[0]+", page="+position+", capsule="+pill[0].getTranslationX()+", expected="+expected+", widths="+tabs[0].getWidth()+"/"+pill[0].getWidth());frames[0]++;}return true;});});
   runner.waitForIdleSync();SystemClock.sleep(400);
   main(runner,()->{float expected=tabs[0].getWidth()/3f;check(pager[0].getCurrentItem()==1&&Math.abs(position(pager[0])-1f)<.001f,"Restored page did not initialize to completed tasks");check(pill[0].getWidth()>0&&Math.abs(pill[0].getTranslationX()-expected)<=1.5f,"Capsule is misaligned before the first gesture: actual="+pill[0].getTranslationX()+", expected="+expected);});
   // Local-cache loading changes the progress bar visibility after the pager has been measured.
   main(runner,()->{Method busy=TaskPage.class.getDeclaredMethod("busy",boolean.class);busy.setAccessible(true);busy.invoke(page[0],true);});runner.waitForIdleSync();SystemClock.sleep(100);
   main(runner,()->check(Math.abs(pill[0].getTranslationX()-tabs[0].getWidth()/3f)<=1.5f,"Layout overwrote the capsule before first gesture: page="+position(pager[0])+", rawScroll="+pager[0].getScrollX()+", firstPageLeft="+pager[0].findViewWithTag("task-filter-page-0").getLeft()+", capsule="+pill[0].getTranslationX()));
   int[] at=new int[2];main(runner,()->pager[0].getLocationInWindow(at));float x=at[0]+pager[0].getWidth()*.2f,y=at[1]+pager[0].getHeight()*.4f;long down=SystemClock.uptimeMillis();event(runner,a,down,MotionEvent.ACTION_DOWN,x,y);
   for(int i=1;i<=16;i++){SystemClock.sleep(16);event(runner,a,down,MotionEvent.ACTION_MOVE,x+pager[0].getWidth()*.65f*i/16,y);}
   event(runner,a,down,MotionEvent.ACTION_UP,x+pager[0].getWidth()*.65f,y);settle(runner,pager[0]);
   main(runner,()->{check(pager[0].getCurrentItem()==0&&Math.abs(position(pager[0]))*pager[0].getWidth()<=2,"First right swipe did not settle on pending tasks: current="+pager[0].getCurrentItem()+", position="+position(pager[0]));check(worst[0]<=1.5f,"Capsule jumps away from page progress: max error="+worst[0]);check(Math.abs(pill[0].getTranslationX())<=1.5f,"Capsule did not center on pending tasks");});
   x=at[0]+pager[0].getWidth()*.8f;down=SystemClock.uptimeMillis();event(runner,a,down,MotionEvent.ACTION_DOWN,x,y);
   for(int i=1;i<=8;i++){SystemClock.sleep(16);event(runner,a,down,MotionEvent.ACTION_MOVE,x-pager[0].getWidth()*.45f*i/8,y);}
   for(int i=1;i<=8;i++){SystemClock.sleep(16);event(runner,a,down,MotionEvent.ACTION_MOVE,x-pager[0].getWidth()*(.45f-.3f*i/8),y);}
   event(runner,a,down,MotionEvent.ACTION_CANCEL,x-pager[0].getWidth()*.15f,y);settle(runner,pager[0]);
   main(runner,()->{check(pager[0].getCurrentItem()==0&&Math.abs(position(pager[0]))*pager[0].getWidth()<=2,"Reversed and cancelled drag did not return to pending tasks");page[0].findViewWithTag("task-filter-2").performClick();});SystemClock.sleep(80);
   main(runner,()->{if(android.animation.ValueAnimator.areAnimatorsEnabled())check(position(pager[0])>0&&position(pager[0])<2,"Click lost its smooth page animation");});settle(runner,pager[0]);
   main(runner,()->{check(pager[0].getCurrentItem()==2&&Math.abs(position(pager[0])-2)*pager[0].getWidth()<=2&&Math.abs(pill[0].getTranslationX()-tabs[0].getWidth()*2/3f)<=1.5f,"Clicked capsule did not center on all tasks");check(worst[0]<=1.5f,"Layout, reversed drag or click desynchronized the capsule: max error="+worst[0]);for(String tag:new String[]{"nav-timetable","nav-tasks"}){View button=a.findViewById(android.R.id.content).findViewWithTag(tag);check(button.getBackground()==null&&button.getForeground()==null,"Bottom navigation still has a click ripple");}});
   result.putString("result","PASS: cold task page restored to completed, loading-layout change before first touch, first right swipe, reversed/cancelled drag, smooth click and precise centers, bottom navigation without ripple; frames="+frames[0]+", maxError="+worst[0]);code=Activity.RESULT_OK;
  }catch(Throwable error){result.putString("error",android.util.Log.getStackTraceString(error));}
  finally{result.putString("capsuleTrace",String.join("\n",trace));if(activity!=null){MainActivity a=activity;main(runner,a::finish);}runner.finish(code,result);}
 }
}
