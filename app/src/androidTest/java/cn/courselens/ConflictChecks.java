package cn.courselens;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

public final class ConflictChecks {
 private static Field field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f;}
 private static Object get(MainActivity a,String name)throws Exception{return field(name).get(a);}
 private static void set(MainActivity a,String name,Object value)throws Exception{field(name).set(a,value);}
 private static void call(MainActivity a,String name)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(a);}
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 private interface Work {void run()throws Exception;}
 private static void main(Instrumentation runner,Work work){java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();runner.runOnMainSync(()->{try{work.run();}catch(Throwable e){failure.set(e);}});if(failure.get()!=null)throw new RuntimeException(failure.get());}
 private static View root(MainActivity a)throws Exception{return (View)get(a,"root");}
 private static void iconStyle(View view){float density=view.getResources().getDisplayMetrics().density;int size=Math.round(48*density),glyph=Math.round(22*density);check(view.getWidth()==size&&view.getHeight()==size,"Icon touch target differs from refresh");TextView icon=(TextView)view;check(icon.getText().length()==0&&icon.getCompoundDrawables()[0]!=null&&icon.getCompoundDrawables()[0].getBounds().width()==glyph,"Icon is not a shared-size vector");check(view.getBackground() instanceof android.graphics.drawable.GradientDrawable&&((android.graphics.drawable.GradientDrawable)view.getBackground()).getColor().getDefaultColor()==view.getContext().getColor(R.color.app_card),"Icon background differs from refresh");check(view.getForeground() instanceof android.graphics.drawable.RippleDrawable&&view.getClipToOutline()&&view.getTooltipText()!=null,"Icon feedback or accessible label missing");}
 private static int labelOccurrences(View view,String prefix){int count=view.getContentDescription()!=null&&view.getContentDescription().toString().startsWith(prefix)?1:0;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)count+=labelOccurrences(group.getChildAt(i),prefix);}return count;}
 private static int occurrences(View view,String value){int count=view instanceof TextView&&((TextView)view).getText().toString().equals(value)?1:0;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)count+=occurrences(((ViewGroup)view).getChildAt(i),value);return count;}
 private static ViewGroup grid(MainActivity a)throws Exception{androidx.viewpager.widget.ViewPager pager=(androidx.viewpager.widget.ViewPager)get(a,"pager");ViewGroup grid=pager.findViewWithTag("week-grid-"+(pager.getCurrentItem()+1));check(grid!=null,"Selected week grid missing");return grid;}
 private static View conflict(MainActivity a)throws Exception{ViewGroup grid=grid(a);for(int i=0;i<grid.getChildCount();i++){View v=grid.getChildAt(i);if(v.findViewWithTag("conflict-badge")!=null)return v;}throw new AssertionError("Conflict card missing");}
 private static void settle(Instrumentation runner){runner.waitForIdleSync();SystemClock.sleep(400);}
 private static void screenshot(Instrumentation runner,String name)throws Exception{android.graphics.Bitmap image=runner.getUiAutomation().takeScreenshot();check(image!=null,"Screenshot unavailable");try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(runner.getTargetContext().getFilesDir(),name))){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{image.recycle();}}
 public static void run(Instrumentation runner){Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity activity=null;SharedPreferences prefs=null;Map<String,?> backup=null;
  try{
   check(runner.getTargetContext().getPackageName().endsWith(".qa"),"Use isolated QA package");
   String importedClasses="25L测试；25测试（合成学院）";for(int i=1;i<=90;i++)importedClasses+="；25测试"+i;
   Course imported=Course.parse(SchoolParser.jsonCourses("{courseName:'合成课程',weekday:1,startUnit:1,endUnit:2,weekIndexes:[1],lessonName:'"+importedClasses+"'}","{lessons:[]}")).get(0);
   check(imported.details.isEmpty()&&imported.className.equals(importedClasses),"JSON import left class list in experiment field");
   for(String name:new String[]{"英语示例课","体育示例课"})for(String source:new String[]{"lessonName","lessonRemark"}){
    String identifier=name+"-本部-合成分班A";Course ordinary=Course.parse(SchoolParser.jsonCourses("{courseName:'"+name+"',weekday:1,startUnit:1,endUnit:2,weekIndexes:[1],"+source+":'"+identifier+"'}","{lessons:[]}")).get(0);
    check(ordinary.details.isEmpty()&&ordinary.className.equals(identifier),"Teaching identifier treated as experiment: "+source);
   }
   activity=NativeChecks.startQaActivity(runner);MainActivity a=activity;prefs=a.getPreferences(0);backup=prefs.getAll();SharedPreferences actualPrefs=prefs;
   int existingChecked=0;String storedData=(String)get(a,"data");
   for(Course item:Course.parse(storedData))if(Course.isTeachingClassName(item.name,item.details)){
    main(runner,()->{Method detail=MainActivity.class.getDeclaredMethod("detail",Course.class);detail.setAccessible(true);detail.invoke(a,item);check(labelOccurrences(root(a),"实验内容，")==0,"Existing teaching identifier still displayed");check(get(a,"data").equals(storedData),"Existing data rewritten");a.onBackPressed();});settle(runner);existingChecked++;
   }
   result.putInt("existingTeachingIdentifiersChecked",existingChecked);
   Course english=new Course(1,1,2,"英语示例课","A101","1","合成教师","英语示例课-本部-合成分班A","");
   Course sports=new Course(2,3,4,"体育示例课","A102","1","合成教师","体育示例课-本部-合成分班B","");
   Course lab=new Course(3,5,6,"合成实验","L101","1","合成教师","重力加速度测定","");String ordinaryFixture=english.encode()+"\n"+sports.encode()+"\n"+lab.encode();
   main(runner,()->{set(a,"data",ordinaryFixture);set(a,"week",1);call(a,"home");});settle(runner);
   for(Course item:new Course[]{english,sports,lab}){
    main(runner,()->{Method detail=MainActivity.class.getDeclaredMethod("detail",Course.class);detail.setAccessible(true);detail.invoke(a,item);check(labelOccurrences(root(a),"实验内容，")== (item==lab?1:0),"Wrong content row for ordinary/lab course");check(occurrences(root(a),item.details)== (item==lab?1:0),"Teaching identifier displayed or real lab hidden");check(((TextView)root(a).findViewWithTag("navigation-title")).getText().toString().equals(item.name),"Course not in top-left heading");check(occurrences(root(a),item.room)==1&&occurrences(root(a),item.teacher)==1,"Missing or repeated classroom/teacher");check(get(a,"data").equals(ordinaryFixture),"Display rewrote stored data");NativeChecks.openSheetMenu(a);check(root(a).findViewWithTag("conflict-course-details")==null,"Ordinary course has conflict details");a.onBackPressed();});settle(runner);screenshot(runner,item==english?"qa-ordinary-english.png":item==sports?"qa-ordinary-sports.png":"qa-real-lab.png");main(runner,a::onBackPressed);settle(runner);
   }
   String fixture="1|3|4|C语言程序设计|A201|1-3|教师甲|指针实验|合成一班\n1|3|4|C语言程序设计|A201|1|教师甲|指针实验|合成一班\n1|3|4|大学物理|B202|1-3|教师乙|光学实验|合成二班\n1|3|4|C语言程序设计|A203|1-3|教师丙|链表实验|合成三班\n2|6|7|独立课程|C303|1-3";
   main(runner,()->{set(a,"displayChoices",new HashSet<String>());set(a,"data",fixture);set(a,"week",1);call(a,"home");});settle(runner);
   main(runner,()->{ViewGroup grid=grid(a);int cards=0;for(int i=0;i<grid.getChildCount();i++)if("course-card".equals(grid.getChildAt(i).getTag()))cards++;check(cards==2,"Conflict still renders multiple cards");View card=conflict(a);check(card.getContentDescription().toString().startsWith("C语言程序设计，"),"Default course changed");float density=a.getResources().getDisplayMetrics().density;check(card.getWidth()>=(grid.getWidth()-Math.round(34*density))/7-Math.round(4*density),"Conflict card split into lanes");});
   screenshot(runner,"qa-conflict-grid.png");
   main(runner,()->{conflict(a).performClick();check(occurrences(root(a),"C语言程序设计")==1&&occurrences(root(a),"A201")==1,"Course name or room repeated");check(occurrences(root(a),"大学物理")==0,"Unselected course visible");check(occurrences(root(a),"上课班级")==0&&occurrences(root(a),"合成一班")==0,"Class displayed in detail");});settle(runner);screenshot(runner,"qa-conflict-selected.png");
   main(runner,()->{View sheet=root(a);View switcher=sheet.findViewWithTag("conflict-switch");iconStyle(switcher);iconStyle(sheet.findViewWithTag("sheet-action-button"));switcher.performClick();check(root(a)==sheet,"Cycle replaced sheet");check(occurrences(sheet,"大学物理")==1&&occurrences(sheet,"C语言程序设计")==0,"Cycle did not change displayed course");check(conflict(a).getContentDescription().toString().startsWith("大学物理，"),"Grid not refreshed");switcher.performClick();check(occurrences(sheet,"A203")==1,"Same-name course lost its metadata");switcher.performClick();check(occurrences(sheet,"A201")==1,"Cycle did not wrap");Set<String> before=new HashSet<>(actualPrefs.getStringSet("displayChoices",Collections.emptySet()));switcher.performLongClick();check(root(a)==sheet&&actualPrefs.getStringSet("displayChoices",Collections.emptySet()).equals(before),"Long press changed selection or opened comparison");check(!switcher.getContentDescription().toString().contains("长按"),"Switch still advertises long press");NativeChecks.openSheetMenu(a);View menu=root(a);check(occurrences(menu,"冲突详情")==1&&menu.findViewWithTag("conflict-course-details")!=null,"Conflict details missing from more menu");check(menu.findViewWithTag("adjust-course")!=null&&menu.findViewWithTag("cancel-course")!=null&&menu.findViewWithTag("edit-course")!=null,"Existing course actions missing");a.onBackPressed();check(root(a)==sheet,"Menu back lost sheet");check(actualPrefs.getStringSet("displayChoices",Collections.emptySet()).equals(before),"Menu dismissal changed selection");switcher.performClick();NativeChecks.openSheetMenu(a);});settle(runner);screenshot(runner,"qa-conflict-menu.png");
   main(runner,()->{root(a).findViewWithTag("conflict-course-details").performClick();check(root(a).findViewWithTag("conflict-comparison-2")!=null,"Comparison missing candidates");check(root(a).findViewWithTag("conflict-comparison-3")==null,"Duplicate candidate not removed");check(occurrences(root(a).findViewWithTag("conflict-comparison-1"),"当前显示")==1,"Comparison lost cycled selection");check(occurrences(root(a),"上课班级")==0&&occurrences(root(a),"合成一班")==0,"Class displayed in comparison");check(occurrences(root(a),"指针实验")==1&&occurrences(root(a),"光学实验")==1&&occurrences(root(a),"链表实验")==1,"Comparison discarded experiment metadata");});settle(runner);screenshot(runner,"qa-conflict-comparison.png");
   main(runner,()->{a.onBackPressed();check(occurrences(root(a),"大学物理")==1,"Comparison back lost selected course");NativeChecks.openSheetMenu(a);check(root(a).findViewWithTag("conflict-course-details")!=null,"Comparison back lost menu entry");a.onBackPressed();a.onBackPressed();});settle(runner);
   Set<String> saved=new HashSet<>(actualPrefs.getStringSet("displayChoices",Collections.emptySet()));
   main(runner,()->{check(conflict(a).getContentDescription().toString().startsWith("大学物理，"),"Selected course not displayed");check(get(a,"data").equals(fixture),"Changing display rewrote courses");check(CourseDisplay.priority(Course.parse(fixture).get(2),saved)>0,"Choice not persisted");set(a,"week",2);call(a,"home");});settle(runner);
   main(runner,()->{check(conflict(a).getContentDescription().toString().startsWith("大学物理，"),"Global priority missing in another week");set(a,"week",1);call(a,"home");});settle(runner);
   main(runner,()->{conflict(a).performClick();set(a,"data",fixture+"\n3|1|2|后来课程|D|1");root(a).findViewWithTag("conflict-switch").performClick();check(actualPrefs.getStringSet("displayChoices",Collections.emptySet()).equals(saved),"Stale picker saved choice");});settle(runner);
   main(runner,a::finish);
   activity=NativeChecks.startQaActivity(runner);MainActivity restarted=activity;
   main(runner,()->{Set<String> restored=(Set<String>)get(restarted,"displayChoices");check(restored.equals(saved),"Reopening lost preference");check(CourseDisplay.groups(Course.parse(fixture),1,restored).get(0).selected.name.equals("大学物理"),"Reopening lost selected course");});
   result.putString("result","PASS: shared refresh-style switch/more icons, selected course only, no repeated name/room, in-place cycle/wrap, same-name metadata, no long-press action, more-menu conflict details, comparison selection and return, cycle persistence, menu dismissal, grid/global priority, unchanged courses, stale rejection and restart");code=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("result","FAIL: "+e);}
  finally{if(activity!=null){MainActivity a=activity;runner.runOnMainSync(a::finish);}if(prefs!=null){NativeChecks.restoreDisplayChoices(prefs,backup);check(prefs.getAll().equals(backup),"Original preferences not restored");}runner.finish(code,result);}
 }
}
