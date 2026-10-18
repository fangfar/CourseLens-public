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
 private static void sameSurface(MainActivity activity,View page){View bar=activity.findViewById(android.R.id.content).findViewWithTag("bottom-navigation");for(View surface:new View[]{page,bar})check(surface.getBackground() instanceof android.graphics.drawable.ColorDrawable&&((android.graphics.drawable.ColorDrawable)surface.getBackground()).getColor()==SecondaryUi.SURFACE,"Page and bottom navigation use different backgrounds");check(activity.getWindow().getStatusBarColor()==SecondaryUi.SURFACE&&activity.getWindow().getNavigationBarColor()==SecondaryUi.SURFACE,"System bars do not match the page background");}
 private static void textFits(View view){
  if(view.getVisibility()!=View.VISIBLE)return;
  if(view instanceof TextView){TextView text=(TextView)view;android.text.Layout layout=text.getLayout();if(layout!=null&&text.length()>0)check(layout.getHeight()<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom()+1,"Clipped text: "+text.getText());}
  if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)textFits(group.getChildAt(i));}
 }
 private static void capture(Instrumentation runner,MainActivity activity,String name){android.graphics.Bitmap bitmap=runner.getUiAutomation().takeScreenshot();check(bitmap!=null,"Screenshot unavailable");try(java.io.FileOutputStream out=activity.openFileOutput(name,0)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}catch(java.io.IOException e){throw new RuntimeException(e);}finally{bitmap.recycle();}}
 private static void compactLayout(Instrumentation runner,MainActivity activity){
  android.content.res.Configuration original=new android.content.res.Configuration(activity.getResources().getConfiguration());
  Map<String,?> preferences=new HashMap<>(activity.getPreferences(0).getAll());
  try{for(float scale:new float[]{1f,1.45f}){
   main(runner,()->{android.content.res.Configuration config=new android.content.res.Configuration(original);config.fontScale=scale;activity.getResources().updateConfiguration(config,activity.getResources().getDisplayMetrics());call(activity,"menu");});settled(runner);
   main(runner,()->{View root=(View)get(activity,"root");textFits(root);View clear=root.findViewWithTag("clear-timetable");check(clear instanceof TextView&&!(clear instanceof Button)&&clear.hasOnClickListeners(),"Clear is still a large button or has no action");Method method=MainActivity.class.getDeclaredMethod("confirmClear");method.setAccessible(true);AlertDialog dialog=(AlertDialog)method.invoke(activity);dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();});settled(runner);
   capture(runner,activity,"qa-compact-settings-"+scale+".png");
   main(runner,()->{activity.onBackPressed();TaskPage previous=(TaskPage)get(activity,"taskPage");if(previous!=null)previous.close();Field page=MainActivity.class.getDeclaredField("taskPage");page.setAccessible(true);page.set(activity,null);call(activity,"tasks");});settled(runner);
   main(runner,()->{View root=(View)get(activity,"root");textFits(root);float density=activity.getResources().getDisplayMetrics().density;
    for(String tag:new String[]{"chaoxing-connect","chaoxing-refresh"}){TextView action=root.findViewWithTag(tag);check(action.length()==0&&action.getContentDescription()!=null,"Header action lost its accessible label");check(Math.abs(action.getWidth()/density-48)<1&&action.getHeight()/density>=48,"Header action is not compact or has a small touch target");}
    View navigation=activity.findViewById(android.R.id.content).findViewWithTag("bottom-navigation");
    for(String tag:new String[]{"nav-timetable","nav-tasks"}){ImageButton tab=navigation.findViewWithTag(tag);check(tab.getDrawable()!=null&&tab.getContentDescription()!=null,"Bottom tab is not an accessible icon");check(tab.getBackground()==null,"Bottom tab retains a large selection block");}
   });capture(runner,activity,"qa-compact-tasks-"+scale+".png");
   main(runner,()->{View root=(View)get(activity,"root");root.findViewWithTag("chaoxing-connect").performClick();Field configuration=TaskPage.class.getDeclaredField("configuration");configuration.setAccessible(true);AlertDialog dialog=(AlertDialog)configuration.get(root);check(dialog!=null&&dialog.isShowing()&&(dialog.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0,"Compact configuration action did not open a secure dialog");dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();activity.onBackPressed();call(activity,"edit");});settled(runner);
   main(runner,()->{View root=(View)get(activity,"root");textFits(root);View add=root.findViewWithTag("manage-add-course");check(add.getWidth()<((View)add.getParent()).getWidth(),"Primary action still stretches across the page");activity.onBackPressed();});settled(runner);
   main(runner,()->{Method detail=MainActivity.class.getDeclaredMethod("detail",Course.class);detail.setAccessible(true);detail.invoke(activity,new Course(4,8,9,"合成电路分析","测试教室","1,2,3,4,5,6,7,8,9,10","测试教师",""));});settled(runner);
   main(runner,()->{View root=(View)get(activity,"root");textFits(root);if(scale==1f)check(root.getHeight()<((View)get(activity,"stage")).getHeight()/2,"Short detail still occupies half the screen");View icon=root.findViewWithTag("course-info-icon");check(icon.getWidth()<=Math.round(22*activity.getResources().getDisplayMetrics().density),"Detail icon is oversized");});capture(runner,activity,"qa-compact-detail-"+scale+".png");
   main(runner,()->activity.onBackPressed());settled(runner);
  }main(runner,()->check(activity.getPreferences(0).getAll().equals(preferences),"Compact layout checks changed preferences"));}
  finally{main(runner,()->activity.getResources().updateConfiguration(original,activity.getResources().getDisplayMetrics()));}
 }

 static void verify(Instrumentation runner,MainActivity activity){
  Map<String,?> preferences=new HashMap<>(activity.getPreferences(0).getAll());View[] home=new View[1];int[] position=new int[2];
  main(runner,()->{call(activity,"home");});settled(runner);
  capture(runner,activity,"qa-compact-home.png");
  main(runner,()->{home[0]=(View)get(activity,"root");sameSurface(activity,home[0]);ScrollView scroll=(ScrollView)get(activity,"timetableScroll");scroll.scrollTo(0,100);position[0]=scroll.getScrollY();position[1]=(int)get(activity,"week");View navigation=activity.findViewById(android.R.id.content).findViewWithTag("bottom-navigation");check(navigation!=null&&navigation.getVisibility()==View.VISIBLE,"Primary navigation missing");check(navigation.getParent()==((View)get(activity,"stage")).getParent(),"Navigation scrolls with page content");check(navigation.findViewWithTag("nav-timetable").isSelected(),"Timetable not selected");navigation.findViewWithTag("nav-tasks").performClick();});settled(runner);
  main(runner,()->{View root=(View)get(activity,"root");check("tasks-page".equals(root.getTag()),"Task tab did not open independent page");sameSurface(activity,root);check(get(activity,"pageBack")==null,"Tasks treated as secondary page");check(activity.findViewById(android.R.id.content).findViewWithTag("nav-tasks").isSelected(),"Task tab not selected");check(root.getImportantForAccessibility()!=View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,"Task page hidden from accessibility");root.findViewWithTag("task-filter-1").performClick();check(((TextView)root.findViewWithTag("tasks-empty-title")).getText().toString().equals("尚未读取作业"),"Task filter did not change empty state");check(root.findViewWithTag("task-filter-1").isSelected(),"Task filter not marked selected");Bundle state=new Bundle();((TaskPage)root).saveState(state);TaskPage restored=new TaskPage(activity,state);check(restored.findViewWithTag("task-filter-1").isSelected(),"Saved task filter lost");restored.close();activity.findViewById(android.R.id.content).findViewWithTag("nav-timetable").performClick();});settled(runner);
  main(runner,()->{check(get(activity,"root")==home[0],"Tab switch rebuilt retained timetable");check(((ScrollView)get(activity,"timetableScroll")).getScrollY()==position[0]&&(int)get(activity,"week")==position[1],"Tab switch lost timetable position");activity.findViewById(android.R.id.content).findViewWithTag("nav-tasks").performClick();});settled(runner);
  main(runner,()->{View root=(View)get(activity,"root");check(root.findViewWithTag("task-filter-1").isSelected(),"Switching tabs lost task filter");check(root.getImportantForAccessibility()!=View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,"Reopened task page hidden from accessibility");activity.onBackPressed();});settled(runner);
  main(runner,()->{check(!(boolean)get(activity,"tasksVisible")&&get(activity,"root")==home[0],"Task back did not return to timetable");call(activity,"menu");check(activity.findViewById(android.R.id.content).findViewWithTag("bottom-navigation").getVisibility()==View.GONE,"Navigation allows leaving secondary forms");activity.onBackPressed();});settled(runner);
  main(runner,()->{check(activity.findViewById(android.R.id.content).findViewWithTag("bottom-navigation").getVisibility()==View.VISIBLE,"Returning from settings did not restore navigation");check(activity.getPreferences(0).getAll().equals(preferences),"Task navigation changed stored timetable");});
 }

 public static void run(Instrumentation runner){
  Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity activity=null;
  try{check(runner.getTargetContext().getPackageName().endsWith(".qa"),"QA only");activity=NativeChecks.startQaActivity(runner);verify(runner,activity);compactLayout(runner,activity);result.putString("result","PASS: independent task page, filters, saved filter, tab selection, timetable retention, back, secondary-page navigation, compact actions and unclipped normal/large-font layouts, clear cancellation and unchanged preferences");code=Activity.RESULT_OK;}
  catch(Throwable e){result.putString("error",android.util.Log.getStackTraceString(e));}
  finally{if(activity!=null){MainActivity current=activity;main(runner,current::finish);}runner.finish(code,result);}
 }
}
