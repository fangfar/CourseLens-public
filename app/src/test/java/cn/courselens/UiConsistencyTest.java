package cn.courselens;

import android.app.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.util.ReflectionHelpers.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=34) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class UiConsistencyTest {
 @Test public void settingsKeepAllDestinationsAndReturnToOrigin()throws Exception{
  for(String variant:new String[]{"normal","dark","large"}){
   RuntimeEnvironment.setQualifiers(variant.equals("large")?"w320dp-h640dp-notnight":"w380dp-h800dp-"+(variant.equals("dark")?"night":"notnight"));RuntimeEnvironment.setFontScale(variant.equals("large")?2f:1f);
   try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
    MainActivity a=controller.get();callInstanceMethod(a,"tasks");idleTasks(a);callInstanceMethod(a,"openSideMenu");settle();snapshot(a,variant+"-drawer.png");
    root(a).findViewWithTag("drawer-settings").performClick();settle();snapshot(a,variant+"-settings.png");
    View settings=getField(a,"root");View data=settings.findViewWithTag("data-management"),permissions=settings.findViewWithTag("permission-settings"),about=settings.findViewWithTag("about-app");assertSame(data.getParent(),permissions.getParent());assertSame(data.getParent(),about.getParent());assertNotNull(settings.findViewWithTag("settings-term"));assertNotNull(settings.findViewWithTag("school-account"));assertNotNull(settings.findViewWithTag("add-home-widget"));
    for(String tag:new String[]{"timetable-management","dark-mode-setting","data-management","permission-settings","about-app"}){
     ((View)getField(a,"root")).findViewWithTag(tag).performClick();settle();snapshot(a,variant+"-"+tag+".png");
     if(tag.equals("timetable-management")){View page=getField(a,"root");for(String action:new String[]{"manage-courses","timetable-","add-timetable"})assertNotNull(page.findViewWithTag(action));page.findViewWithTag("add-timetable").performClick();settle();a.onBackPressed();settle();assertEquals("课表管理",((View)getField(a,"root")).getTag());}
     if(tag.equals("dark-mode-setting")){View appearance=getField(a,"root");assertTrue(appearance.findViewWithTag("appearance-0").isSelected());assertNotNull(appearance.findViewWithTag("appearance-2"));}
     if(tag.equals("data-management")){View page=getField(a,"root");for(String action:new String[]{"clear-timetable","clear-tasks","clear-parcels","task-deletion-history"})assertNotNull(page.findViewWithTag(action));}
     a.onBackPressed();settle();assertSame(settings,getField(a,"root"));
    }
    a.onBackPressed();settle();assertNotNull(root(a).findViewWithTag("tasks-page"));assertTrue(root(a).findViewWithTag("nav-tasks").isSelected());
   }
  }
 }
 @Test public void timetableManagementRestoresWithoutRefresh(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity a=controller.get();a.getPreferences(0).edit().putBoolean("schoolImported",true).commit();callInstanceMethod(a,"menu");((View)getField(a,"root")).findViewWithTag("timetable-management").performClick();assertNull(((View)getField(a,"root")).findViewWithTag("settings-refresh-timetable"));controller.recreate();a=controller.get();assertEquals("课表管理",((View)getField(a,"root")).getTag());assertNull(((View)getField(a,"root")).findViewWithTag("settings-refresh-timetable"));a.onBackPressed();assertEquals("设置",((View)getField(a,"root")).getTag());
  }
 }
 @Test public void timetableManagementSwitchesInlineAndRenames()throws Exception{
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity a=controller.get();Timetables.add(a,"合成课表","squ",ClassTimes.DEFAULT,"2026-08-31");String second=Timetables.active(a);Timetables.select(a,"");callInstanceMethod(a,"reloadTimetable");callInstanceMethod(a,"menu");((View)getField(a,"root")).findViewWithTag("timetable-management").performClick();View page=getField(a,"root");assertTrue(page.findViewWithTag("timetable-").isSelected());page.findViewWithTag("timetable-"+second).performClick();page=getField(a,"root");assertEquals("课表管理",page.getTag());assertEquals(second,Timetables.active(a));assertTrue(page.findViewWithTag("timetable-"+second).isSelected());assertNull(getField(a,"navigationOverlay"));assertTrue(page.findViewWithTag("timetable-"+second).performLongClick());((EditText)((View)getField(a,"root")).findViewWithTag("rename-timetable-name")).setText("修改名称");((View)getField(a,"root")).findViewWithTag("save-timetable-name").performClick();assertEquals("修改名称",Timetables.names(a).get(second));assertEquals("课表管理",((View)getField(a,"root")).getTag());
  }
 }
 @Test public void emptyTimetableCreatesDirectlyAndCancelKeepsData()throws Exception{
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity a=controller.get();int current=callInstanceMethod(a,"currentWeek");setField(a,"week",current);assertEquals("第 "+current+" 周",callInstanceMethod(a,"weekCaption"));setField(a,"week",current==1?2:1);assertTrue(((String)callInstanceMethod(a,"weekCaption")).contains("本周第 "+current+" 周"));View add=root(a).findViewWithTag("timetable-import");assertNotNull(add);add.performClick();settle();assertNotNull(root(a).findViewWithTag("select-school"));a.onBackPressed();settle();assertEquals("",getField(a,"data"));
   callInstanceMethod(a,"edit");settle();View emptyAdd=root(a).findViewWithTag("manage-empty-add");assertNotNull(emptyAdd);emptyAdd.performClick();settle();assertEquals("添加课程",((View)getField(a,"root")).getTag());
  }
 }
 @Test public void courseFieldsGrowWithLargeText()throws Exception{
  RuntimeEnvironment.setQualifiers("w320dp-h640dp-notnight");RuntimeEnvironment.setFontScale(2f);
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity a=controller.get();callInstanceMethod(a,"courseForm",ClassParameter.from(Course.class,(Course)null),ClassParameter.from(int.class,-1),ClassParameter.from(Runnable.class,(Runnable)()->{}));View root=root(a);snapshot(a,"large-course-form.png");EditText name=root.findViewWithTag("course-name");assertTrue(name.getHeight()>=a.getResources().getDisplayMetrics().density*SecondaryUi.CONTROL_HEIGHT);assertTrue(name.getHeight()>=name.getLineHeight()+name.getCompoundPaddingTop()+name.getCompoundPaddingBottom());
  }
 }
 @Test public void widgetHighlightsCourseWithoutExtraLabels()throws Exception{
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity a=controller.get();String term="2026-10-26";a.getPreferences(0).edit().putString("term",term).putString("courses","1|1|2|第一门|A101|1-30\n1|3|4|第二门|A202|1-30").commit();
   View widget=CourseWidget.views(a,0,LocalDate.parse(term).atTime(7,0)).apply(a,new FrameLayout(a));
   assertEquals(View.GONE,widget.findViewById(R.id.widget_header).getVisibility());assertEquals(View.GONE,widget.findViewById(R.id.widget_status).getVisibility());
   assertNotEquals(((TextView)widget.findViewById(R.id.widget_title)).getCurrentTextColor(),((TextView)widget.findViewById(R.id.widget_title_second)).getCurrentTextColor());
   assertNull(widget.findViewById(R.id.widget_content).getBackground());assertTrue(widget.findViewById(R.id.widget_content).getContentDescription().toString().contains("下一节"));
  }
 }
 @Test public void widgetHighlightsOnlyCurrentCourseWhenAnotherFollows(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity a=controller.get();String term="2026-10-26";a.getPreferences(0).edit().putString("term",term).putString("classTimes","08:00-09:00\n09:00-10:00").putString("courses","1|1|1|第一门|A101|1\n1|2|2|第二门|A102|1").commit();View widget=CourseWidget.views(a,0,LocalDate.parse(term).atTime(8,30)).apply(a,new FrameLayout(a));assertNotEquals(((TextView)widget.findViewById(R.id.widget_title)).getCurrentTextColor(),((TextView)widget.findViewById(R.id.widget_title_second)).getCurrentTextColor());
  }
 }
 private boolean containsVisibleText(View view,String text){if(view.getVisibility()!=View.VISIBLE)return false;if(view instanceof TextView&&((TextView)view).getText().toString().contains(text))return true;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)if(containsVisibleText(group.getChildAt(i),text))return true;}return false;}
 @Test public void widgetEmptyStatesDoNotShowTomorrowHint()throws Exception{
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity a=controller.get();String term="2026-10-26";a.getPreferences(0).edit().putString("term",term).putString("courses","1|1|2|合成课程|测试教室|1-30\n2|1|2|明日合成课|测试教室|1-30").commit();
   for(boolean finished:new boolean[]{true,false}){
    if(!finished)a.getPreferences(0).edit().putString("courses","2|1|2|明日合成课|测试教室|1-30").commit();
    View widget=CourseWidget.views(a,0,LocalDate.parse(term).atTime(21,0)).apply(a,new FrameLayout(a));assertEquals(finished?"今日课程已结束":"今日无课",((TextView)widget.findViewById(R.id.widget_empty_title)).getText().toString());assertEquals(1,((ViewGroup)widget.findViewById(R.id.widget_empty_panel)).getChildCount());assertEquals(Gravity.CENTER,((LinearLayout)widget.findViewById(R.id.widget_empty_panel)).getGravity());assertEquals(Gravity.CENTER,((TextView)widget.findViewById(R.id.widget_empty_title)).getGravity());assertFalse(containsVisibleText(widget,"明日合成课"));
   }
  }
 }
 @Test public void productScreensUseSyntheticDataAndKeepActions()throws Exception{
  RuntimeEnvironment.setQualifiers("w360dp-h616dp-notnight-xhdpi");RuntimeEnvironment.setFontScale(1f);
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity a=controller.get();setField(a,"data","1|1|2|示例A|A101|1-30\n2|3|4|示例B|A202|1-30\n3|5|6|示例C|A303|1-30\n4|1|2|示例D|A404|1-30\n5|3|4|示例E|A505|1-30");setField(a,"week",9);callInstanceMethod(a,"home");settle();snapshot(a,"app-timetable.webp");
   callInstanceMethod(a,"tasks");idleTasks(a);TaskPage task=getField(a,"taskPage");ChaoxingClient.Result tasks=new ChaoxingClient.Result();ChaoxingClient.Work first=new ChaoxingClient.Work("合成课程 A","课后练习","未完成","");first.deadline="10-29 18:00";first.workKey="local:example-a";tasks.items.add(first);ChaoxingClient.Work second=new ChaoxingClient.Work("合成课程 B","实验报告","未完成","");second.deadline="10-30 20:00";second.workKey="local:example-b";tasks.items.add(second);setField(task,"localTasks",tasks);callInstanceMethod(task,"render");settle();snapshot(a,"app-tasks.webp");assertNull(root(a).findViewWithTag("task-empty-action"));assertNotNull(root(a).findViewWithTag("task-work-title"));
   callInstanceMethod(a,"parcels");ParcelPage parcels=getField(a,"parcelPage");parcels.pause();ParcelRepository.IO.submit(()->{}).get();settle();ParcelRecord parcel=new ParcelRecord();parcel.id=100;parcel.code="6-2-2088";parcel.station="合成东门驿站";callInstanceMethod(parcels,"render",ClassParameter.from(List.class,Arrays.asList(parcel)));snapshot(a,"app-parcels.webp");root(a).findViewWithTag("quick-menu-button").performClick();assertNotNull(((AlertDialog)getField(parcels,"currentSheet")).getWindow().getDecorView().findViewWithTag("parcel-editor-save"));
  }
 }
 @Test public void courseTextStaysWhiteAndNavigationKeepsContrastInBothThemes()throws Exception{
  for(boolean dark:new boolean[]{false,true}){
   RuntimeEnvironment.setQualifiers("w380dp-h800dp-"+(dark?"night":"notnight"));
   try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
    MainActivity a=controller.get();setField(a,"data","1|1|2|合成课|A1|1-30");setField(a,"week",1);callInstanceMethod(a,"home");settle();snapshot(a,(dark?"dark":"normal")+"-course-contrast.png");TextView card=root(a).findViewWithTag("course-card");assertNotNull(card);
    assertEquals("Course text keeps its original white color",Color.WHITE,card.getCurrentTextColor());
    assertTrue(androidx.core.graphics.ColorUtils.calculateContrast(a.getColor(R.color.app_nav_muted),a.getColor(R.color.app_surface))>=3);
   }
  }
 }
 private void idleTasks(MainActivity a)throws Exception{ExecutorService worker=getField(getField(a,"taskPage"),"worker");worker.submit(()->{}).get();settle();}
 private void settle(){shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(600));}
 private View root(Activity a){return a.findViewById(android.R.id.content);}
 private void snapshot(Activity a,String name)throws Exception{
  View root=root(a);android.util.DisplayMetrics dm=a.getResources().getDisplayMetrics();root.measure(View.MeasureSpec.makeMeasureSpec(dm.widthPixels,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(dm.heightPixels,View.MeasureSpec.EXACTLY));root.layout(0,0,dm.widthPixels,dm.heightPixels);
  String dir=System.getProperty("courselens.parcelPreviewDir");if(dir==null)return;File folder=new File(dir);folder.mkdirs();Bitmap bitmap=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));try(FileOutputStream out=new FileOutputStream(new File(folder,name))){bitmap.compress(name.endsWith("webp")?Bitmap.CompressFormat.WEBP_LOSSLESS:Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
 }
}
