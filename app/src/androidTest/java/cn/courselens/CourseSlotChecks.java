package cn.courselens;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

public final class CourseSlotChecks {
 private interface Work {void run()throws Exception;}
 private static void main(Instrumentation r,Work work){java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();r.runOnMainSync(()->{try{work.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new RuntimeException(error.get());}
 private static Object get(MainActivity a,String key)throws Exception{Field f=MainActivity.class.getDeclaredField(key);f.setAccessible(true);return f.get(a);}
 private static void set(MainActivity a,String key,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(key);f.setAccessible(true);f.set(a,value);}
 private static void call(MainActivity a,String key)throws Exception{Method m=MainActivity.class.getDeclaredMethod(key);m.setAccessible(true);m.invoke(a);}
 private static View root(MainActivity a)throws Exception{return (View)get(a,"root");}
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 private static void form(MainActivity a,Course c,int index)throws Exception{Method m=MainActivity.class.getDeclaredMethod("courseForm",Course.class,int.class,Runnable.class);m.setAccessible(true);m.invoke(a,c,index,(Runnable)()->{try{call(a,"home");}catch(Exception e){throw new RuntimeException(e);}});}
 private static void click(MainActivity a,String tag)throws Exception{View v=root(a).findViewWithTag(tag);check(v!=null,"Missing "+tag);v.performClick();}
 private static void text(MainActivity a,String tag,String value)throws Exception{((EditText)root(a).findViewWithTag(tag)).setText(value);}
 public static void run(Instrumentation r){
  Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity activity=null;SharedPreferences prefs=null;Map<String,?> backup=null;
  try{
   check(r.getTargetContext().getPackageName().endsWith(".qa"),"Only QA may run course-slot checks");activity=NativeChecks.startQaActivity(r);MainActivity a=activity;prefs=a.getPreferences(0);backup=prefs.getAll();
   Course first=new Course(1,1,2,"合成课程","A101","1-4","教师甲","合成课程-本部-分班A","隐藏班级甲");
   Course other=new Course(2,3,4,"其他课程","C303","1-16","其他教师");
   Course second=new Course(4,5,6,"合成课程","B202","6,8,10","教师乙","实验一","隐藏班级乙");String fixture=first.encode()+"\n"+other.encode()+"\n"+second.encode();
   main(r,()->{set(a,"data",fixture);set(a,"week",1);call(a,"save");call(a,"home");call(a,"quickMenu");check(((TextView)root(a).findViewWithTag("quick-action-0")).getText().toString().equals("课程管理")&&root(a).findViewWithTag("quick-action-1")==null,"Course actions not merged");click(a,"quick-action-0");check(root(a).findViewWithTag("manage-course-0")!=null&&root(a).findViewWithTag("manage-course-1")!=null&&root(a).findViewWithTag("manage-course-2")==null,"Same course scattered");click(a,"manage-course-0");check(root(a).findViewWithTag("course-slot-2")!=null,"Second schedule missing");check(((EditText)root(a).findViewWithTag("course-details")).getText().length()==0,"Teaching identifier exposed");check(((EditText)root(a).findViewWithTag("course-details-slot-2")).getText().toString().equals("实验一"),"Experiment lost");});
   r.waitForIdleSync();SystemClock.sleep(450);
   main(r,()->{text(a,"course-room","A102");text(a,"course-room-slot-2","B203");click(a,"add-course-slot");text(a,"course-room-slot-3","D404");text(a,"course-teacher-slot-3","教师丙");((Spinner)root(a).findViewWithTag("course-day-slot-3")).setSelection(5);((Spinner)root(a).findViewWithTag("course-start-slot-3")).setSelection(8);((Spinner)root(a).findViewWithTag("course-end-slot-3")).setSelection(7);click(a,"save-course");check(get(a,"data").equals(fixture),"Invalid slot partially saved");((Spinner)root(a).findViewWithTag("course-end-slot-3")).setSelection(9);click(a,"course-weeks-3-slot-3");click(a,"save-course");check(get(a,"data").equals(fixture),"Empty weeks partially saved");((ToggleButton)root(a).findViewWithTag("course-week-20-slot-3")).setChecked(true);click(a,"save-course");List<Course> saved=Course.parse((String)get(a,"data"));check(saved.size()==4,"Multi-slot count incorrect");check(saved.get(0).room.equals("A102")&&saved.get(0).details.equals(first.details)&&saved.get(0).className.equals(first.className),"First metadata lost");check(saved.get(1).encode().equals(other.encode()),"Unrelated record changed");check(saved.get(2).room.equals("B203")&&saved.get(2).teacher.equals(second.teacher)&&saved.get(2).details.equals(second.details)&&saved.get(2).className.equals(second.className),"Second metadata lost");for(int w=1;w<=30;w++)check(saved.get(2).active(w)==second.active(w),"Existing weeks changed");Course added=saved.get(3);check(added.day==6&&added.start==9&&added.end==10&&added.weeks.equals("20")&&added.room.equals("D404")&&added.teacher.equals("教师丙"),"New slot fields incorrect");check(a.getPreferences(0).getString("courses","").equals(get(a,"data")),"Save not persisted");});
   r.waitForIdleSync();SystemClock.sleep(450);
   main(r,()->{List<Course> saved=Course.parse((String)get(a,"data"));String before=(String)get(a,"data");form(a,saved.get(0),0);click(a,"remove-course-slot-2");text(a,"course-name","未保存的改名");a.onBackPressed();NativeChecks.discardUnsaved(a);check(get(a,"data").equals(before),"Cancel wrote changes");form(a,saved.get(2),2);text(a,"course-name","课程新名");click(a,"remove-course-slot-2");click(a,"save-course");List<Course> changed=Course.parse((String)get(a,"data"));check(changed.size()==3&&changed.get(0).name.equals("课程新名")&&changed.get(2).name.equals("课程新名")&&changed.get(1).encode().equals(other.encode()),"Rename/remove changed unrelated course");check(changed.get(0).displayDetails().isEmpty()&&changed.get(0).className.contains(first.details),"Rename exposed or erased hidden identifier");form(a,changed.get(0),0);String updated=(String)get(a,"data")+"\n7|1|2|后续课程|Z|1";set(a,"data",updated);click(a,"save-course");check(get(a,"data").equals(updated)&&((TextView)root(a).findViewWithTag("course-error")).getText().toString().contains("已更新"),"Stale form overwrote timetable");});
   main(r,()->{set(a,"data","");call(a,"edit");click(a,"manage-add-course");text(a,"course-name","多时段新课");click(a,"remove-course-slot-1");check(root(a).findViewWithTag("course-slot-1")!=null,"Last slot removed");click(a,"add-course-slot");text(a,"course-room-slot-2","E505");click(a,"save-course");List<Course> added=Course.parse((String)get(a,"data"));check(added.size()==2&&added.get(0).name.equals(added.get(1).name)&&added.get(1).room.equals("E505"),"New multi-slot course failed");});
   result.putString("result","PASS: grouped management, multi-slot add/edit/remove/rename, independent metadata/weeks, atomic validation, cancel, stale data and persistence");code=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("error",android.util.Log.getStackTraceString(e));}
  finally{
   if(activity!=null){MainActivity a=activity;main(r,a::finish);}
   if(prefs!=null&&backup!=null){SharedPreferences.Editor editor=prefs.edit().clear();for(Map.Entry<String,?> entry:backup.entrySet()){Object v=entry.getValue();if(v instanceof String)editor.putString(entry.getKey(),(String)v);else if(v instanceof Integer)editor.putInt(entry.getKey(),(Integer)v);else if(v instanceof Boolean)editor.putBoolean(entry.getKey(),(Boolean)v);else if(v instanceof Long)editor.putLong(entry.getKey(),(Long)v);else if(v instanceof Float)editor.putFloat(entry.getKey(),(Float)v);else if(v instanceof Set)editor.putStringSet(entry.getKey(),new HashSet<>((Set<String>)v));}check(editor.commit()&&prefs.getAll().equals(backup),"Original preferences not restored");}
   r.finish(code,result);
  }
 }
}
