package cn.courselens;

import android.app.*;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

public final class ChoiceChecks {
 private interface Work {void run()throws Exception;}
 private static void main(Instrumentation r,Work work){java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();r.runOnMainSync(()->{try{work.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new RuntimeException(error.get());}
 private static Object get(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 private static void set(MainActivity a,String name,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(a,value);}
 private static void call(MainActivity a,String name,Class<?>[] types,Object... args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(a,args);}
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 private static void settle(Instrumentation r,MainActivity a){
  long end=SystemClock.uptimeMillis()+6000;while(SystemClock.uptimeMillis()<end){boolean[] ready={false};main(r,()->{Object choice=get(a,"choiceSheet");boolean waiting=choice!=null&&((View)choice).getVisibility()==View.VISIBLE&&(!(boolean)get(choice,"open")||((MotionOverlay)choice).motion.value!=1||((View)get(choice,"panel")).isLayoutRequested());MainActivity.NavigationOverlay overlay=(MainActivity.NavigationOverlay)get(a,"navigationOverlay");ready[0]=!waiting&&(overlay==null||overlay.motion.value==1)&&get(a,"exitingOverlay")==null&&!((PageMotion)get(a,"pageMotion")).isRunning()&&!a.getWindow().getDecorView().isLayoutRequested();});if(ready[0])return;SystemClock.sleep(16);}throw new AssertionError("Choice/page layout did not settle");
 }
 private static void picker(Instrumentation r,MainActivity a,String tag)throws Exception{
  Spinner[] field=new Spinner[1];Object[] sheet=new Object[1];int[] initial=new int[1];
  main(r,()->{field[0]=((View)get(a,"root")).findViewWithTag(tag);initial[0]=field[0].getSelectedItemPosition();check(field[0].performClick(),tag+": not clickable");sheet[0]=get(a,"choiceSheet");});settle(r,a);
  main(r,()->{
   View panel=(View)get(sheet[0],"panel");check(Math.abs(panel.getHeight()-((View)panel.getParent()).getHeight()/2)<=1,tag+": choice sheet is not half height");check(panel.getBottom()==((View)panel.getParent()).getHeight(),"Choice sheet leaves an empty band below it");View cancel=panel.findViewWithTag("choice-cancel");ScrollView scroll=(ScrollView)get(sheet[0],"scroll");check(cancel.getTop()>=scroll.getBottom()+12*a.getResources().getDisplayMetrics().density,tag+": footer touches options");check(((RadioButton)panel.findViewWithTag("choice-option-"+initial[0])).isChecked(),tag+": current selection missing");
   List<Rect> cells=new ArrayList<>();for(int i=0;i<field[0].getCount();i++){View option=panel.findViewWithTag("choice-option-"+i);check(option!=null&&option.getHeight()>=48*a.getResources().getDisplayMetrics().density,tag+": missing or small option "+i);Rect bounds=new Rect();if(option.getGlobalVisibleRect(bounds)){for(Rect other:cells)check(!Rect.intersects(bounds,other),tag+": options overlap");cells.add(bounds);}}
   cancel.performClick();check(field[0].getSelectedItemPosition()==initial[0],tag+": cancel changed value");
  });settle(r,a);
  for(int index:new int[]{field[0].getCount()-1,0}){
   main(r,()->{field[0].setSelection(index);field[0].performClick();});settle(r,a);
   main(r,()->{View panel=(View)get(sheet[0],"panel");RadioButton selected=panel.findViewWithTag("choice-option-"+index);Rect bounds=new Rect();check(selected.isChecked()&&selected.getGlobalVisibleRect(bounds)&&bounds.height()==selected.getHeight(),tag+": selected option not scrolled into view index="+index+" actual="+field[0].getSelectedItemPosition()+" translation="+panel.getTranslationY()+" visible="+bounds.height()+" full="+selected.getHeight()+" font="+a.getResources().getConfiguration().fontScale);panel.findViewWithTag("choice-option-"+(index==0?field[0].getCount()-1:0)).performClick();check(field[0].getSelectedItemPosition()==(index==0?field[0].getCount()-1:0),tag+": selection not applied, expected="+(index==0?field[0].getCount()-1:0)+" actual="+field[0].getSelectedItemPosition());});settle(r,a);
  }
  main(r,()->{field[0].setSelection(initial[0]);field[0].performClick();});settle(r,a);main(r,()->{((View)sheet[0]).findViewWithTag("choice-shade").performClick();check(field[0].getSelectedItemPosition()==initial[0],tag+": outside tap changed value");});settle(r,a);
 }
 private static void sheets(Instrumentation r,MainActivity a,float scale)throws Exception{
  for(String page:new String[]{"detail","weekPicker"}){
   main(r,()->{call(a,"home",new Class<?>[]{});if(page.equals("detail"))call(a,page,new Class<?>[]{Course.class},new Course(4,8,9,"合成电路分析","测试教室","1-30","测试教师"));else call(a,page,new Class<?>[]{});});settle(r,a);
   main(r,()->{View panel=(View)get(a,"root");check(Math.abs(panel.getHeight()-((View)panel.getParent()).getHeight()/2)<=1,page+": sheet is not half height at font "+scale);check(panel.getBottom()==((View)panel.getParent()).getHeight(),page+": sheet is not bottom aligned");if(page.equals("weekPicker")){ScrollView scroll=(ScrollView)((ViewGroup)panel).getChildAt(((ViewGroup)panel).getChildCount()-1);scroll.fullScroll(View.FOCUS_DOWN);}});settle(r,a);
   if(page.equals("weekPicker")){long deadline=SystemClock.uptimeMillis()+6000;boolean[] visible={false};while(SystemClock.uptimeMillis()<deadline){main(r,()->{View cell=((View)get(a,"root")).findViewWithTag("week-choice-30");Rect rect=new Rect();visible[0]=cell!=null&&cell.getGlobalVisibleRect(rect)&&rect.height()==cell.getHeight();});if(visible[0])break;SystemClock.sleep(16);}check(visible[0],"Last week did not become fully visible after scrolling");}
   main(r,()->{if(page.equals("weekPicker")){View cell=((View)get(a,"root")).findViewWithTag("week-choice-30");Rect rect=new Rect();check(cell.getGlobalVisibleRect(rect)&&rect.height()==cell.getHeight(),"Last week is unreachable in half sheet");}});
   android.graphics.Bitmap image=r.getUiAutomation().takeScreenshot();check(image!=null,"Sheet screenshot missing");try(java.io.FileOutputStream out=a.openFileOutput("qa-half-"+page+"-"+scale+".png",0)){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{image.recycle();}
   main(r,a::onBackPressed);settle(r,a);
  }
 }
 public static void run(Instrumentation r){
  MainActivity activity=null;Configuration original=null;Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;
  try{
   check(r.getTargetContext().getPackageName().endsWith(".qa"),"QA only");activity=NativeChecks.startQaActivity(r);MainActivity a=activity;main(r,()->a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));original=new Configuration(a.getResources().getConfiguration());Map<String,?> stored=new HashMap<>(a.getPreferences(0).getAll());Course fixture=new Course(1,1,2,"选择器测试课程","合成教室","1-30","合成老师");
   for(float scale:(((NativeChecks)r).normalFontOnly?new float[]{original.fontScale}:new float[]{original.fontScale,1.4f})){
    main(r,()->{Configuration config=new Configuration(a.getResources().getConfiguration());config.fontScale=scale;a.getResources().updateConfiguration(config,a.getResources().getDisplayMetrics());set(a,"data",fixture.encode());set(a,"adjustments","");call(a,"courseForm",new Class<?>[]{Course.class,int.class,Runnable.class},fixture,0,(Runnable)()->{});((EditText)((View)get(a,"root")).findViewWithTag("course-name")).setText("未保存的选择器草稿");});settle(r,a);
    for(String tag:new String[]{"course-day","course-start","course-end"})picker(r,a,tag);
    main(r,()->{check(((EditText)((View)get(a,"root")).findViewWithTag("course-name")).getText().toString().equals("未保存的选择器草稿"),"Picker discarded draft");call(a,"adjustmentForm",new Class<?>[]{Course.class,int.class,boolean.class,Runnable.class},fixture,7,false,(Runnable)()->{});});settle(r,a);
    for(String tag:new String[]{"adjustment-week","adjustment-day","adjustment-start","adjustment-end"})picker(r,a,tag);sheets(r,a,scale);
   }
   check(a.getPreferences(0).getAll().equals(stored),"Pickers changed stored timetable");result.putString("fontProfile",((NativeChecks)r).normalFontOnly?"current font only":"normal and large fonts");result.putString("result","PASS: half-height detail/week sheets, reachable week 30, seven course/adjustment selectors at the requested font profile, week 30 and period 12, visible selected values, separated footer, cancel/outside, draft and preference preservation");code=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("error",android.util.Log.getStackTraceString(e));}
  finally{if(activity!=null){MainActivity a=activity;Configuration config=original;main(r,()->{if(config!=null)a.getResources().updateConfiguration(config,a.getResources().getDisplayMetrics());a.finish();});}r.finish(code,result);}
 }
}
