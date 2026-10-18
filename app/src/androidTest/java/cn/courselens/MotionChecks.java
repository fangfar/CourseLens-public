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
 private static void settingsScrollReturn(Instrumentation runner,MainActivity a)throws Exception{
  main(runner,()->{try{call(a,"menu");}catch(Exception e){throw new RuntimeException(e);}});
  runner.waitForIdleSync();SystemClock.sleep(350);
  LinearLayout settings=(LinearLayout)field("root").get(a);ScrollView scroll=(ScrollView)settings.getChildAt(1);
  String[] pages={"termPicker","edit","adjustmentManager","widgetAddPage","about","weekPicker"};
  for(int position=0;position<3;position++)for(int page=0;page<pages.length;page++){
   final int offset=position,index=page;int[] saved={0},firstFrame={-1};
   main(runner,()->{try{
    int max=scroll.getChildAt(0).getHeight()-scroll.getHeight()+scroll.getPaddingTop()+scroll.getPaddingBottom();
    check(max>0,"Settings fixture must scroll");scroll.scrollTo(0,max*offset/2);saved[0]=scroll.getScrollY();
    if(pages[index].equals("edit"))call(a,"edit",new Class<?>[]{Runnable.class},(Runnable)()->{try{call(a,"menu");}catch(Exception e){throw new RuntimeException(e);}});
    else if(pages[index].equals("weekPicker"))settings.findViewWithTag("select-teaching-week").performClick();
    else call(a,pages[index]);
   }catch(Exception e){throw new RuntimeException(e);}});
   runner.waitForIdleSync();SystemClock.sleep(350);
   main(runner,()->{try{
    if(index%2==0){LinearLayout child=(LinearLayout)field("root").get(a);((ViewGroup)child.getChildAt(0)).getChildAt(0).performClick();}
    else a.onBackPressed();
    check(field("root").get(a)==settings,"Settings rebuilt after "+pages[index]);
    check(settings.getChildAt(1)==scroll&&scroll.getScrollY()==saved[0],"Immediate settings scroll lost: "+pages[index]);
    settings.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){settings.getViewTreeObserver().removeOnPreDrawListener(this);firstFrame[0]=scroll.getScrollY();return true;}});
   }catch(Exception e){throw new RuntimeException(e);}});
   runner.waitForIdleSync();SystemClock.sleep(350);
   main(runner,()->{try{
    check(firstFrame[0]==saved[0],"First frame jumped or was not drawn: "+pages[index]);
    check(scroll.getScrollY()==saved[0],"Settled settings scroll lost: "+pages[index]);
    check(settings.getAlpha()==1f&&settings.getTranslationX()==0f,"Settings transition did not settle");
    check(settings.getImportantForAccessibility()!=View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,"Settings accessibility not restored");
    check(((ViewGroup)field("stage").get(a)).getChildCount()==1,"Settings return retained outgoing page");
    check(field("remoteStatus").get(a)==settings.findViewWithTag("update-status"),"Settings update status still points to child");
   }catch(Exception e){throw new RuntimeException(e);}});
  }
  main(runner,()->{try{a.onBackPressed();call(a,"menu");check(field("root").get(a)!=settings,"Fresh settings entry reused old session");}catch(Exception e){throw new RuntimeException(e);}});
  runner.waitForIdleSync();SystemClock.sleep(350);
 }
 private static void capture(Instrumentation runner,String name)throws Exception{
  android.graphics.Bitmap bitmap=runner.getUiAutomation().takeScreenshot();check(bitmap!=null,"Screenshot unavailable");
  try(java.io.FileOutputStream out=runner.getTargetContext().openFileOutput("qa-navigation-"+name+".png",0)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{bitmap.recycle();}
 }
 public static void run(Instrumentation runner,boolean baseline){
  Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity activity=null;
  HandlerThread collector=new HandlerThread("motion-frames");collector.start();
  List<Long> frames=Collections.synchronizedList(new ArrayList<>());List<Long> builds=new ArrayList<>();
  Window.OnFrameMetricsAvailableListener listener=(window,metrics,dropped)->frames.add(metrics.getMetric(FrameMetrics.TOTAL_DURATION));
  SharedPreferences prefs=null;Map<String,?> original=null;
  try{
   check(runner.getTargetContext().getPackageName().endsWith(".qa"),"Use isolated QA package");
   activity=NativeChecks.startQaActivity(runner);
   MainActivity a=activity;prefs=a.getPreferences(0);original=prefs.getAll();
   String fixture="1|1|2|动画测试课程|A101|1-30|测试教师|实验内容，检查全部信息|合成一班\n1|1|2|冲突测试课程|A102|1-30|测试教师|另一实验项目|合成二班";
   Course course=Course.parse(fixture).get(0);
   CourseDisplay.Group group=CourseDisplay.groups(Course.parse(fixture),6,Collections.emptySet()).get(0);
   main(runner,()->{try{a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);field("data").set(a,fixture);field("week").setInt(a,6);field("adjustments").set(a,"");call(a,"home");}catch(Exception e){throw new RuntimeException(e);}});
   runner.waitForIdleSync();SystemClock.sleep(350);
   settingsScrollReturn(runner,a);
   a.getWindow().addOnFrameMetricsAvailableListener(listener,new Handler(collector.getLooper()));
   int[] scrollPosition={0};
   String[] pages={"menu","weekPicker","edit","termPicker","detail","courseForm","adjustmentForm","cancelForm","adjustmentManager","conflictPicker","widgetAddPage","login"};
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
     if(!baseline&&field("navigationOverlay").get(a)==null&&android.animation.ValueAnimator.areAnimatorsEnabled())check(((View)field("root").get(a)).getTranslationX()>0f,"Forward page did not enter from right: "+page);
    }catch(Exception e){throw new RuntimeException(e);}});
    runner.waitForIdleSync();SystemClock.sleep(350);
    if(round==0)capture(runner,page);
    if(round==0&&page.equals("widgetAddPage")){
     main(runner,()->{try{call(a,"widgetManualGuide");check(((AlertDialog)field("widgetDialog").get(a)).isShowing(),"Manual guide did not open");}catch(Exception e){throw new RuntimeException(e);}});
     runner.waitForIdleSync();SystemClock.sleep(350);capture(runner,"widgetManualGuide");
     main(runner,()->{try{((AlertDialog)field("widgetDialog").get(a)).dismiss();}catch(Exception e){throw new RuntimeException(e);}});
    }
    if(round==0&&(page.equals("weekPicker")||page.equals("detail")||page.equals("conflictPicker"))){
     View sheet=(View)field("root").get(a);
     main(runner,()->{try{NativeChecks.openSheetMenu(a);}catch(Exception e){throw new RuntimeException(e);}});
     runner.waitForIdleSync();SystemClock.sleep(350);capture(runner,page+"-actions");
     main(runner,()->{a.onBackPressed();NativeChecks.discardUnsaved(a);try{check(field("root").get(a)==sheet,"Menu dismissal replaced sheet");}catch(Exception e){throw new RuntimeException(e);}});
     runner.waitForIdleSync();SystemClock.sleep(350);
    }
    main(runner,()->{try{
     View root=(View)field("root").get(a);
     check(root.isAttachedToWindow()&&root.getAlpha()==1f&&root.getScaleX()==1f&&root.getTranslationY()==0f,"Unsettled "+page+" attached="+root.isAttachedToWindow()+" alpha="+root.getAlpha()+" scale="+root.getScaleX()+" y="+root.getTranslationY()+" width="+root.getWidth());
     check(root.getLayerType()==View.LAYER_TYPE_NONE,"Leaked hardware layer "+page);
     if(page.equals("detail")||page.equals("weekPicker")||page.equals("conflictPicker")){ViewGroup panel=(ViewGroup)root;int available=((View)panel.getParent()).getHeight();check(panel.getHeight()>=available/2&&panel.getHeight()<=available*4/5,"Bottom sheet exceeds readable size bounds: "+page);TextView title=panel.findViewWithTag("navigation-title");check(title.getTextSize()/a.getResources().getDisplayMetrics().scaledDensity>=20,"Sheet title was shrunk: "+page);if(page.equals("weekPicker")){check(panel.findViewWithTag("week-choice-30")!=null,"Direct week grid incomplete");View cell=panel.findViewWithTag("week-choice-6");check(cell.isSelected()&&cell.getHeight()>=48*a.getResources().getDisplayMetrics().density,"Week selection or touch target missing");}}
     if(page.equals("edit")||page.equals("widgetAddPage")){View primary=root.findViewWithTag(page.equals("edit")?"manage-add-course":"pin-home-widget");check(((ViewGroup.MarginLayoutParams)primary.getLayoutParams()).bottomMargin>=16*a.getResources().getDisplayMetrics().density,"Primary action touches following controls: "+page);}
     if(page.equals("courseForm")){check(root.findViewWithTag("course-name")!=null,"Course form replaced before draft check: tag="+root.getTag());((EditText)root.findViewWithTag("course-name")).setText("不应保存的草稿");}
     if(page.equals("adjustmentForm"))((EditText)root.findViewWithTag("adjustment-room")).setText("不应保存的教室");
     boolean sheet=field("navigationOverlay").get(a)!=null;
     a.onBackPressed();NativeChecks.discardUnsaved(a);
     check(field("data").get(a).equals(fixture)&&field("adjustments").get(a).equals(""),"Back saved an unfinished form: "+page);
     if(!baseline&&!sheet&&field("navigationOverlay").get(a)==null&&android.animation.ValueAnimator.areAnimatorsEnabled())check(((View)field("root").get(a)).getTranslationX()<0f,"Back page did not enter from left: "+page+" x="+((View)field("root").get(a)).getTranslationX()+" oldWidth="+root.getWidth()+" stage="+((ViewGroup)field("stage").get(a)).getChildCount()+" running="+((PageMotion)field("pageMotion").get(a)).isRunning());
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
    call(a,"menu");call(a,"weekPicker",new Class<?>[]{Runnable.class},(Runnable)()->{try{call(a,"menu");}catch(Exception e){throw new RuntimeException(e);}});a.onBackPressed();NativeChecks.discardUnsaved(a);a.onBackPressed();NativeChecks.discardUnsaved(a);
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
     Spinner spinner=root.findViewWithTag(tag);spinner.performClick();call(sheet,"close",new Class<?>[]{boolean.class},true);spinner.performClick();a.onBackPressed();NativeChecks.discardUnsaved(a);
    }
   }catch(Exception e){throw new RuntimeException(e);}});
   runner.waitForIdleSync();SystemClock.sleep(350);
   main(runner,()->{try{
    check(((View)field("choiceSheet").get(a)).getVisibility()==View.INVISIBLE,"Reopened sheet did not close");a.onBackPressed();NativeChecks.discardUnsaved(a);
   }catch(Exception e){throw new RuntimeException(e);}});
   runner.waitForIdleSync();SystemClock.sleep(350);
   main(runner,()->{try{
    call(a,"home");
   }catch(Exception e){throw new RuntimeException(e);}});
   runner.waitForIdleSync();SystemClock.sleep(350);
   main(runner,()->{try{
    View home=(View)field("root").get(a);ScrollView scroll=(ScrollView)field("timetableScroll").get(a);scroll.scrollTo(0,240);int y=scroll.getScrollY();
    call(a,"weekPicker");check(field("timetableScroll").get(a)==scroll,"Week sheet replaced timetable");NativeChecks.openSheetMenu(a);check(((View)field("root").get(a)).findViewWithTag("week-30")!=null,"Week 30 missing");a.onBackPressed();NativeChecks.discardUnsaved(a);a.onBackPressed();NativeChecks.discardUnsaved(a);check(field("root").get(a)==home&&scroll.getScrollY()==y,"Week dismissal lost home state");
    call(a,"conflictPicker",new Class<?>[]{CourseDisplay.Group.class,int.class},group,6);View conflict=(View)field("root").get(a);NativeChecks.openSheetMenu(a);View actions=(View)field("root").get(a);actions.findViewWithTag("conflict-course-details").performClick();check("冲突课程对比".equals(((View)field("root").get(a)).getTag()),"Comparison sheet missing");check(((View)field("root").get(a)).findViewWithTag("sheet-action-button").getVisibility()==View.GONE,"Comparison retains empty menu");a.onBackPressed();NativeChecks.discardUnsaved(a);check(field("root").get(a)==conflict,"Detail back rebuilt conflict candidates");a.onBackPressed();NativeChecks.discardUnsaved(a);check(field("root").get(a)==home&&scroll.getScrollY()==y,"Conflict dismissal lost home state");
   }catch(Exception e){throw new RuntimeException(e);}});
   runner.waitForIdleSync();SystemClock.sleep(350);
   main(runner,()->{try{call(a,"conflictPicker",new Class<?>[]{CourseDisplay.Group.class,int.class},group,6);NativeChecks.openSheetMenu(a);((View)field("root").get(a)).findViewWithTag("adjust-course").performClick();a.onBackPressed();NativeChecks.discardUnsaved(a);check(field("showingDetail").getBoolean(a),"Form cancel lost detail sheet");View conflict=(View)field("root").get(a);check(conflict.findViewWithTag("conflict-switch")!=null,"Form cancel lost conflict switch");NativeChecks.openSheetMenu(a);((View)field("root").get(a)).findViewWithTag("conflict-course-details").performClick();ViewGroup selected=((View)field("root").get(a)).findViewWithTag("conflict-comparison-0");check(((TextView)selected.getChildAt(1)).getText().toString().equals("当前显示"),"Form cancel lost conflict selection");a.onBackPressed();NativeChecks.discardUnsaved(a);a.onBackPressed();NativeChecks.discardUnsaved(a);}catch(Exception e){throw new RuntimeException(e);}});
   runner.waitForIdleSync();SystemClock.sleep(350);
   String[] destinations={"课程管理","课表设置","同步课表"};
   for(int i=0;i<destinations.length;i++){final int action=i;
    main(runner,()->{try{View home=(View)field("root").get(a);home.findViewWithTag("quick-menu-button").performClick();View menu=(View)field("root").get(a);for(int item=0;item<destinations.length;item++)check(menu.findViewWithTag("quick-action-"+item)!=null,"Missing quick action");check(((View)field("navigationOverlay").get(a)).findViewWithTag("navigation-shade")!=null,"Menu outside dismissal missing");}catch(Exception e){throw new RuntimeException(e);}});
    runner.waitForIdleSync();SystemClock.sleep(350);if(action==0)capture(runner,"menu");
    main(runner,()->{try{View menu=(View)field("root").get(a);FrameLayout stage=(FrameLayout)field("stage").get(a);check(menu.getWidth()<stage.getWidth()&&menu.getHeight()<stage.getHeight(),"Menu is not a small window: tag="+menu.getTag()+" size="+menu.getWidth()+"x"+menu.getHeight()+" stage="+stage.getWidth()+"x"+stage.getHeight()+" action="+action);check(menu.getPivotX()==menu.getWidth()&&menu.getPivotY()==0f,"Menu pivot is not top right");menu.findViewWithTag("quick-action-"+action).performClick();check(field("navigationOverlay").get(a)==null,"Menu retained after action");check(destinations[action].equals(((View)field("root").get(a)).getTag()),"Wrong quick action destination");}catch(Exception e){throw new RuntimeException(e);}});
    runner.waitForIdleSync();SystemClock.sleep(350);
    main(runner,()->{a.onBackPressed();NativeChecks.discardUnsaved(a);});runner.waitForIdleSync();SystemClock.sleep(350);
   }
   main(runner,()->{try{View home=(View)field("root").get(a);call(a,"quickMenu");a.onBackPressed();NativeChecks.discardUnsaved(a);call(a,"quickMenu");((View)field("navigationOverlay").get(a)).findViewWithTag("navigation-shade").performClick();check(field("root").get(a)==home,"Reopening menu lost home");}catch(Exception e){throw new RuntimeException(e);}});
   runner.waitForIdleSync();SystemClock.sleep(350);
   main(runner,()->{try{check(((ViewGroup)field("stage").get(a)).getChildCount()==1,"Dismissed overlay leaked views");}catch(Exception e){throw new RuntimeException(e);}});
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
   main(runner,()->{try{a.onBackPressed();NativeChecks.discardUnsaved(a);}catch(Exception e){throw new RuntimeException(e);}});
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
    a.onBackPressed();NativeChecks.discardUnsaved(a);check(field("pageBack").get(a)==null,"Nested back did not reach home");
    if(Build.VERSION.SDK_INT>=33)check(!field("backRegistered").getBoolean(a),"Home retained secondary system back callback");
   }catch(Exception e){throw new RuntimeException(e);}});
   runner.waitForIdleSync();SystemClock.sleep(350);
   main(runner,()->{try{
    call(a,"menu");call(a,"about");View about=(View)field("root").get(a);check(about.findViewWithTag("about-app-icon") instanceof ImageView,"About does not use the App icon");check(about.findViewWithTag("check-app-update")==null,"About duplicates update action");
   }catch(Exception e){throw new RuntimeException(e);}});runner.waitForIdleSync();SystemClock.sleep(350);capture(runner,"about");
   main(runner,()->{try{
    call(a,"home");call(a,"weekPicker");android.content.SharedPreferences stored=a.getPreferences(0);boolean had=stored.contains("week");int previous=stored.getInt("week",6);
    try{((View)field("root").get(a)).findViewWithTag("week-choice-30").performClick();check(field("week").getInt(a)==30,"Direct grid did not select week 30");call(a,"weekPicker");((View)field("root").get(a)).findViewWithTag("week-choice-6").performClick();check(field("week").getInt(a)==6,"Direct grid did not return to week 6");}
    finally{android.content.SharedPreferences.Editor restore=stored.edit();if(had)restore.putInt("week",previous);else restore.remove("week");check(restore.commit(),"Week preference restore failed");}
    String before=(String)field("data").get(a);AlertDialog clear=(AlertDialog)call(a,"confirmClear",new Class<?>[]{});check(clear.isShowing(),"Clear confirmation missing");check(clear.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString().equals("清空"),"Clear confirmation action missing");clear.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();check(before.equals(field("data").get(a)),"Cancelling clear changed courses");
   }catch(Exception e){throw new RuntimeException(e);}});runner.waitForIdleSync();SystemClock.sleep(350);
   check(prefs.getAll().equals(original),"Motion checks changed preferences");
   synchronized(frames){result.putString("frames",String.format(Locale.ROOT,"n=%d total-duration median=%.2fms p95=%.2fms",frames.size(),percentile(frames,.5),percentile(frames,.95)));}
   result.putString("builds",String.format(Locale.ROOT,"n=%d median=%.2fms p95=%.2fms",builds.size(),percentile(builds,.5),percentile(builds,.95)));
   result.putString("result","PASS: three bottom sheets, nested sheet return, three corner menu actions, rapid menu reopen, 12 page entries, rapid navigation, mid-animation back, touch blocking, fade through, scroll return, three sheets, system back, unsaved drafts and unchanged preferences");code=Activity.RESULT_OK;
  }catch(Throwable e){java.io.StringWriter trace=new java.io.StringWriter();e.printStackTrace(new java.io.PrintWriter(trace));result.putString("result","FAIL: "+trace);}
  finally{if(activity!=null){MainActivity a=activity;a.getWindow().removeOnFrameMetricsAvailableListener(listener);main(runner,a::finish);}collector.quitSafely();runner.finish(code,result);}
 }
}
