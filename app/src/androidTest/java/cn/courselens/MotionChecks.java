package cn.courselens;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

// Runs only on the isolated QA package; fixtures never enter stored timetable data.
public final class MotionChecks {
 private static Field field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f;}
 private static Object call(Object target,String name,Class<?>[] types,Object... args)throws Exception{Method m=target.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(target,args);}
 private static void call(MainActivity a,String name)throws Exception{call(a,name,new Class<?>[]{});}
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 private static void main(Instrumentation runner,Runnable work){java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();runner.runOnMainSync(()->{try{work.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new RuntimeException(error.get());}
 private static double percentile(List<Long> values,double percentile){List<Long> sorted=new ArrayList<>(values);Collections.sort(sorted);return sorted.isEmpty()?0:sorted.get((int)Math.ceil(percentile*sorted.size())-1)/1000000d;}
 public static void run(Instrumentation runner,boolean baseline){
  Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity activity=null;
  HandlerThread collector=new HandlerThread("motion-frames");collector.start();
  List<Long> frames=Collections.synchronizedList(new ArrayList<>());List<Long> builds=new ArrayList<>();
  Window.OnFrameMetricsAvailableListener listener=(window,metrics,dropped)->frames.add(metrics.getMetric(FrameMetrics.TOTAL_DURATION));
  SharedPreferences prefs=null;Map<String,?> original=null;
  try{
   check(runner.getTargetContext().getPackageName().endsWith(".qa"),"Use isolated QA package");
   Instrumentation.ActivityMonitor monitor=runner.addMonitor(MainActivity.class.getName(),null,false);
   try{
    try(java.io.InputStream command=new ParcelFileDescriptor.AutoCloseInputStream(runner.getUiAutomation().executeShellCommand("am start -n "+runner.getTargetContext().getPackageName()+"/cn.courselens.MainActivity"))){byte[] buffer=new byte[1024];while(command.read(buffer)!=-1){}}
    activity=(MainActivity)runner.waitForMonitorWithTimeout(monitor,10000);
    check(activity!=null,"QA activity did not enter foreground within 10 seconds");
   }finally{runner.removeMonitor(monitor);}
   MainActivity a=activity;prefs=a.getPreferences(0);original=prefs.getAll();
   String fixture="1|1|2|动画测试课程|A101|1-30\n1|1|2|冲突测试课程|A102|1-30";
   Course course=Course.parse(fixture).get(0);
   CourseDisplay.Group group=CourseDisplay.groups(Course.parse(fixture),6,Collections.emptySet()).get(0);
   main(runner,()->{try{a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);field("data").set(a,fixture);field("week").setInt(a,6);field("adjustments").set(a,"");call(a,"home");}catch(Exception e){throw new RuntimeException(e);}});
   runner.waitForIdleSync();SystemClock.sleep(350);
   a.getWindow().addOnFrameMetricsAvailableListener(listener,new Handler(collector.getLooper()));
   int[] scrollPosition={0};
   String[] pages={"menu","weekPicker","edit","termPicker","detail","courseForm","adjustmentForm","cancelForm","adjustmentManager","conflictPicker","login"};
   for(int round=0;round<3;round++)for(String page:pages){
    Bundle progress=new Bundle();progress.putString("page",page+" "+round);runner.sendStatus(0,progress);
    main(runner,()->{try{
     long started=System.nanoTime();
     switch(page){
      case "detail":call(a,"detail",new Class<?>[]{Course.class},course);break;
      case "courseForm":call(a,"courseForm",new Class<?>[]{Course.class,int.class,Runnable.class},course,0,(Runnable)()->{try{call(a,"home");}catch(Exception e){throw new RuntimeException(e);}});break;
      case "adjustmentForm":case "cancelForm":call(a,"adjustmentForm",new Class<?>[]{Course.class,int.class,boolean.class,Runnable.class},course,6,page.equals("cancelForm"),(Runnable)()->{try{call(a,"home");}catch(Exception e){throw new RuntimeException(e);}});break;
      case "conflictPicker":call(a,"conflictPicker",new Class<?>[]{CourseDisplay.Group.class,int.class},group,6);break;
      default:call(a,page);
     }
     builds.add(System.nanoTime()-started);
     if(!baseline&&android.animation.ValueAnimator.areAnimatorsEnabled())check(((View)field("root").get(a)).getTranslationX()>0f,"Forward page did not enter from right: "+page);
    }catch(Exception e){throw new RuntimeException(e);}});
    runner.waitForIdleSync();SystemClock.sleep(350);
    main(runner,()->{try{
     View root=(View)field("root").get(a);
     check(root.isAttachedToWindow()&&root.getAlpha()==1f&&root.getScaleX()==1f&&root.getTranslationY()==0f,"Unsettled "+page+" attached="+root.isAttachedToWindow()+" alpha="+root.getAlpha()+" scale="+root.getScaleX()+" y="+root.getTranslationY()+" width="+root.getWidth());
     check(root.getLayerType()==View.LAYER_TYPE_NONE,"Leaked hardware layer "+page);
     if(page.equals("courseForm"))((EditText)root.findViewWithTag("course-name")).setText("不应保存的草稿");
     if(page.equals("adjustmentForm"))((EditText)root.findViewWithTag("adjustment-room")).setText("不应保存的教室");
     a.onBackPressed();
     check(field("data").get(a).equals(fixture)&&field("adjustments").get(a).equals(""),"Back saved an unfinished form: "+page);
     if(!baseline&&android.animation.ValueAnimator.areAnimatorsEnabled())check(((View)field("root").get(a)).getTranslationX()<0f,"Back page did not enter from left: "+page);
    }catch(Exception e){throw new RuntimeException(e);}});
    runner.waitForIdleSync();SystemClock.sleep(350);
   }
   main(runner,()->{try{
    for(int i=0;i<8;i++){call(a,"menu");call(a,"weekPicker");call(a,"home");}
   }catch(Exception e){throw new RuntimeException(e);}});
   runner.waitForIdleSync();SystemClock.sleep(350);
   main(runner,()->{try{
    View root=(View)field("root").get(a);check(root.getAlpha()==1f&&root.getTranslationY()==0f,"Rapid navigation left invisible page");
    if(!baseline){check(root.getParent()==field("stage").get(a),"Stage replaced");check(((ViewGroup)root.getParent()).getChildCount()==1,"Old page retained");}
    ScrollView scroll=(ScrollView)field("timetableScroll").get(a);scroll.scrollTo(0,240);scrollPosition[0]=scroll.getScrollY();check(scrollPosition[0]>0,"Scroll fixture failed");
    call(a,"menu");call(a,"weekPicker",new Class<?>[]{Runnable.class},(Runnable)()->{try{call(a,"menu");}catch(Exception e){throw new RuntimeException(e);}});a.onBackPressed();a.onBackPressed();
    check(!a.isFinishing(),"Nested navigation exited the test activity");
   }catch(Exception e){throw new RuntimeException(e);}});
   runner.waitForIdleSync();SystemClock.sleep(350);
   main(runner,()->{try{check(((ScrollView)field("timetableScroll").get(a)).getScrollY()==scrollPosition[0],"Lost timetable scroll");
    call(a,"courseForm",new Class<?>[]{Course.class,int.class,Runnable.class},course,0,(Runnable)()->{try{call(a,"home");}catch(Exception e){throw new RuntimeException(e);}});
   }catch(Exception e){throw new RuntimeException(e);}});
   runner.waitForIdleSync();SystemClock.sleep(350);
   main(runner,()->{try{
    View root=(View)field("root").get(a);Object sheet=field("choiceSheet").get(a);
    for(String tag:new String[]{"course-day","course-start","course-end"}){
     Spinner spinner=root.findViewWithTag(tag);spinner.performClick();call(sheet,"close",new Class<?>[]{boolean.class},true);spinner.performClick();a.onBackPressed();
    }
   }catch(Exception e){throw new RuntimeException(e);}});
   runner.waitForIdleSync();SystemClock.sleep(350);
   main(runner,()->{try{
    check(((View)field("choiceSheet").get(a)).getVisibility()==View.INVISIBLE,"Reopened sheet did not close");a.onBackPressed();
   }catch(Exception e){throw new RuntimeException(e);}});
   runner.waitForIdleSync();SystemClock.sleep(350);
   check(prefs.getAll().equals(original),"Page navigation changed saved data");
   main(runner,()->{try{
    View before=(View)field("root").get(a);check(before.getWidth()>0,"Refresh source page was not rendered");call(a,"home");View root=(View)field("root").get(a);
    check(root.getTranslationX()==0f,"Same-level refresh slid sideways");
    if(android.animation.ValueAnimator.areAnimatorsEnabled())check(root.getScaleX()<1f,"Fade through missing subtle scale");
   }catch(Exception e){throw new RuntimeException(e);}});
   runner.waitForIdleSync();SystemClock.sleep(350);
   main(runner,()->{try{
    call(a,"menu");FrameLayout stage=(FrameLayout)field("stage").get(a);View root=(View)field("root").get(a);
    boolean[] touched={false};root.setOnTouchListener((v,event)->{touched[0]=true;return true;});
    MotionEvent touch=MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),MotionEvent.ACTION_DOWN,1,1,0);
    stage.dispatchTouchEvent(touch);touch.recycle();
    if(android.animation.ValueAnimator.areAnimatorsEnabled())check(!touched[0],"Transition leaked touch to incoming page");
   }catch(Exception e){throw new RuntimeException(e);}});
   runner.waitForIdleSync();SystemClock.sleep(100);
   main(runner,()->{try{a.onBackPressed();}catch(Exception e){throw new RuntimeException(e);}});
   runner.waitForIdleSync();SystemClock.sleep(350);
   main(runner,()->{try{
    PageMotion motion=(PageMotion)field("pageMotion").get(a);check(!motion.isRunning(),"Cancelled motion still blocks input");
    FrameLayout stage=(FrameLayout)field("stage").get(a);check(stage.getChildCount()==1,"Cancelled motion retained outgoing page");
    call(a,"menu");call(a,"termPicker");
    ((android.view.inputmethod.InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(((View)field("root").get(a)).getWindowToken(),0);
   }catch(Exception e){throw new RuntimeException(e);}});
   runner.waitForIdleSync();SystemClock.sleep(350);
   try(java.io.InputStream command=new ParcelFileDescriptor.AutoCloseInputStream(runner.getUiAutomation().executeShellCommand("input keyevent KEYCODE_BACK"))){while(command.read()!=-1){}}
   runner.waitForIdleSync();SystemClock.sleep(350);
   main(runner,()->{try{
    check("课表设置".equals(((View)field("root").get(a)).getTag()),"System back did not return to parent settings: "+((View)field("root").get(a)).getTag());
    a.onBackPressed();check(field("pageBack").get(a)==null,"Nested back did not reach home");
    if(Build.VERSION.SDK_INT>=33)check(!field("backRegistered").getBoolean(a),"Home retained secondary system back callback");
   }catch(Exception e){throw new RuntimeException(e);}});
   runner.waitForIdleSync();SystemClock.sleep(350);
   check(prefs.getAll().equals(original),"Motion checks changed preferences");
   synchronized(frames){result.putString("frames",String.format(Locale.ROOT,"n=%d total-duration median=%.2fms p95=%.2fms",frames.size(),percentile(frames,.5),percentile(frames,.95)));}
   result.putString("builds",String.format(Locale.ROOT,"n=%d median=%.2fms p95=%.2fms",builds.size(),percentile(builds,.5),percentile(builds,.95)));
   result.putString("result","PASS: 11 secondary pages, rapid navigation, mid-animation back, touch blocking, fade through, scroll return, three sheets, system back, unsaved drafts and unchanged preferences");code=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("result","FAIL: "+e);}
  finally{if(activity!=null){MainActivity a=activity;a.getWindow().removeOnFrameMetricsAvailableListener(listener);main(runner,a::finish);}collector.quitSafely();runner.finish(code,result);}
 }
}
