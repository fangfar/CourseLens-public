package cn.courselens;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

public final class SaveChecks {
 private interface Work {void run()throws Exception;}
 private static void main(Instrumentation r,Work w){java.util.concurrent.atomic.AtomicReference<Throwable> e=new java.util.concurrent.atomic.AtomicReference<>();r.runOnMainSync(()->{try{w.run();}catch(Throwable t){e.set(t);}});if(e.get()!=null)throw new RuntimeException(e.get());}
 private static Field field(String n)throws Exception{Field f=MainActivity.class.getDeclaredField(n);f.setAccessible(true);return f;}
 private static Object get(MainActivity a,String n)throws Exception{return field(n).get(a);}
 private static void set(MainActivity a,String n,Object v)throws Exception{field(n).set(a,v);}
 private static void call(MainActivity a,String n)throws Exception{Method m=MainActivity.class.getDeclaredMethod(n);m.setAccessible(true);m.invoke(a);}
 private static View root(MainActivity a)throws Exception{return (View)get(a,"root");}
 private static void check(boolean b,String n){if(!b)throw new AssertionError(n);}
 private static void form(MainActivity a,Course c)throws Exception{Method m=MainActivity.class.getDeclaredMethod("courseForm",Course.class,int.class,Runnable.class);m.setAccessible(true);m.invoke(a,c,c==null?-1:0,(Runnable)()->{try{call(a,"home");}catch(Exception e){throw new RuntimeException(e);}});}
 private static void adjust(MainActivity a,Course c,boolean cancel)throws Exception{Method m=MainActivity.class.getDeclaredMethod("adjustmentForm",Course.class,int.class,boolean.class,Runnable.class);m.setAccessible(true);m.invoke(a,c,1,cancel,(Runnable)()->{try{call(a,"home");}catch(Exception e){throw new RuntimeException(e);}});}
 private static void header(MainActivity a,String tag)throws Exception{View button=root(a).findViewWithTag(tag);ViewGroup bar=(ViewGroup)((ViewGroup)root(a)).getChildAt(0);check(button!=null&&button.getParent()==bar&&bar.getChildAt(bar.getChildCount()-1)==button&&button.getContentDescription().toString().equals("保存"),"Save is not in top-right header");}
 private static void dateLayout(MainActivity a)throws Exception{
  DatePicker picker=root(a).findViewWithTag("term-date");ViewGroup card=(ViewGroup)picker.getParent(),body=(ViewGroup)card.getParent();
  check(card.getWidth()==picker.getWidth()&&picker.getLeft()==0,"Date picker and card edges are not aligned: card="+card.getWidth()+", picker="+picker.getWidth());
  check(Math.abs((card.getLeft()-body.getPaddingLeft())-(body.getWidth()-body.getPaddingRight()-card.getRight()))<=1,"Date card is not centered");
  check(card.getHeight()-picker.getBottom()>=Math.round(12*a.getResources().getDisplayMetrics().density),"Last calendar row touches the card edge");
 }
 private static void answer(MainActivity a,int choice)throws Exception{AlertDialog d=(AlertDialog)get(a,"unsavedDialog");check(d!=null&&d.isShowing(),"Unsaved prompt missing");check(d.getButton(-1).getText().toString().equals("保存并退出")&&d.getButton(-2).getText().toString().equals("不保存退出"),"Prompt choices incorrect");d.getButton(choice).performClick();}
 private static void choose(Instrumentation r,MainActivity a,int choice){main(r,()->answer(a,choice));r.waitForIdleSync();SystemClock.sleep(180);}
 private static void snapshot(Instrumentation r,MainActivity a,String name)throws Exception{android.graphics.Bitmap[] frame=new android.graphics.Bitmap[1];main(r,()->{View decor=a.getWindow().getDecorView();frame[0]=android.graphics.Bitmap.createBitmap(decor.getWidth(),decor.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);decor.draw(new android.graphics.Canvas(frame[0]));});android.graphics.Bitmap image=frame[0];try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(a.getExternalFilesDir(null),name))){check(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out),"Screenshot failed");}finally{image.recycle();}}
 public static void run(Instrumentation r){
  Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity activity=null;SharedPreferences prefs=null;Map<String,?> backup=null;
  try{
   check(r.getTargetContext().getPackageName().endsWith(".qa"),"QA only");activity=NativeChecks.startQaActivity(r);MainActivity a=activity;prefs=a.getPreferences(0);backup=prefs.getAll();Course course=new Course(1,1,2,"保存测试","A101","1-3","合成教师");String fixture=course.encode();
   main(r,()->{set(a,"data",fixture);set(a,"term","2026-08-31");call(a,"menu");check(root(a).findViewWithTag("settings-term")!=null,"Term setting missing");});r.waitForIdleSync();SystemClock.sleep(350);snapshot(r,a,"qa-settings-design.png");
   main(r,()->{root(a).findViewWithTag("settings-term").performClick();header(a,"save-term");Button save=root(a).findViewWithTag("save-term");check(save.getBackgroundTintList().getDefaultColor()==0xff0866d9,"Save does not use the blue action color");DatePicker picker=root(a).findViewWithTag("term-date");android.util.TypedValue accent=new android.util.TypedValue();check(picker.getContext().getTheme().resolveAttribute(android.R.attr.colorAccent,accent,true)&&accent.data==0xff4266ad,"Calendar accent is not blue");});r.waitForIdleSync();SystemClock.sleep(350);snapshot(r,a,"qa-date-design.png");
   for(int[] date:new int[][]{{2021,1,28},{2026,5,30},{2026,7,31}}){
    main(r,()->((DatePicker)root(a).findViewWithTag("term-date")).updateDate(date[0],date[1],date[2]));r.waitForIdleSync();SystemClock.sleep(350);
    snapshot(r,a,"qa-date-layout-"+date[0]+"-"+(date[1]+1)+".png");main(r,()->dateLayout(a));
   }
   main(r,()->{set(a,"data",fixture);set(a,"adjustments","");set(a,"term","2026-08-31");call(a,"save");form(a,course);header(a,"save-course");a.onBackPressed();check(get(a,"unsavedDialog")==null&&get(a,"pageBack")==null,"Unchanged edit prompted");form(a,course);((EditText)root(a).findViewWithTag("course-room")).setText("B202");a.onBackPressed();});choose(r,a,-2);
   main(r,()->{check(get(a,"data").equals(fixture),"Discard saved draft");form(a,course);((EditText)root(a).findViewWithTag("course-room")).setText("B202");a.onBackPressed();});choose(r,a,-1);
   main(r,()->{check(Course.parse((String)get(a,"data")).get(0).room.equals("B202")&&get(a,"pageBack")==null,"Save-and-exit failed");set(a,"data",fixture);call(a,"save");form(a,null);((EditText)root(a).findViewWithTag("course-room")).setText("草稿");a.onBackPressed();});choose(r,a,-1);
   main(r,()->{check(root(a).findViewWithTag("course-name")!=null&&((EditText)root(a).findViewWithTag("course-name")).getError()!=null&&get(a,"data").equals(fixture),"Invalid save exited or changed data");a.onBackPressed();AlertDialog d=(AlertDialog)get(a,"unsavedDialog");d.cancel();check(root(a).findViewWithTag("course-name")!=null,"Cancelling prompt lost draft");a.onBackPressed();});choose(r,a,-2);
   main(r,()->{form(a,course);root(a).findViewWithTag("add-course-slot").performClick();a.onBackPressed();});choose(r,a,-2);
   main(r,()->{check(get(a,"data").equals(fixture),"Discarded added slot persisted");call(a,"termPicker");header(a,"save-term");((DatePicker)root(a).findViewWithTag("term-date")).updateDate(2026,8,7);((ViewGroup)((ViewGroup)root(a)).getChildAt(0)).getChildAt(0).performClick();});choose(r,a,-2);
   main(r,()->{check(get(a,"term").equals("2026-08-31"),"Discarded date saved");call(a,"termPicker");((DatePicker)root(a).findViewWithTag("term-date")).updateDate(2026,8,7);a.onBackPressed();});choose(r,a,-1);
   main(r,()->{check(get(a,"term").equals("2026-09-07"),"Date save-and-exit failed");set(a,"term","2026-08-31");call(a,"save");adjust(a,course,false);header(a,"save-adjustment");((EditText)root(a).findViewWithTag("adjustment-room")).setText("C303");call(a,"backPage");});choose(r,a,-2);
   main(r,()->{check(get(a,"adjustments").equals(""),"Discard saved adjustment");adjust(a,course,false);((EditText)root(a).findViewWithTag("adjustment-room")).setText("C303");a.onBackPressed();});choose(r,a,-1);
   String[] saved={""};main(r,()->{check(!get(a,"adjustments").equals(""),"Adjustment save-and-exit failed");saved[0]=(String)get(a,"adjustments");adjust(a,course,true);check(((TextView)root(a).findViewWithTag("save-adjustment")).getText().toString().equals("确认本次停课"),"Stop confirmation changed");a.onBackPressed();check(get(a,"unsavedDialog")==null,"Stop prompts unsaved exit");});
   main(r,()->{check(get(a,"adjustments").equals(saved[0]),"Discard saved cancellation");adjust(a,course,true);root(a).findViewWithTag("save-adjustment").performClick();});
   main(r,()->check(CourseAdjustment.parse((String)get(a,"adjustments")).get(0).replacement==null,"Stop save-and-exit failed"));
   result.putString("result","PASS: header saves, unchanged return, save/discard/cancel prompt, invalid save stays, multi-slot draft, term date, move, separate stop confirmation and toolbar/system return");code=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("error",android.util.Log.getStackTraceString(e));}
  finally{
   if(activity!=null){MainActivity a=activity;main(r,a::finish);}
   if(prefs!=null&&backup!=null){SharedPreferences.Editor editor=prefs.edit().clear();for(Map.Entry<String,?> entry:backup.entrySet()){Object v=entry.getValue();if(v instanceof String)editor.putString(entry.getKey(),(String)v);else if(v instanceof Integer)editor.putInt(entry.getKey(),(Integer)v);else if(v instanceof Boolean)editor.putBoolean(entry.getKey(),(Boolean)v);else if(v instanceof Long)editor.putLong(entry.getKey(),(Long)v);else if(v instanceof Float)editor.putFloat(entry.getKey(),(Float)v);else if(v instanceof Set)editor.putStringSet(entry.getKey(),new HashSet<>((Set<String>)v));}check(editor.commit()&&prefs.getAll().equals(backup),"Preferences not restored");}
   r.finish(code,result);
  }
 }
}
