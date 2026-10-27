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
    for(String tag:new String[]{"dark-mode-setting","data-management","permission-settings","about-app"}){
     ((View)getField(a,"root")).findViewWithTag(tag).performClick();settle();snapshot(a,variant+"-"+tag+".png");
     if(tag.equals("dark-mode-setting")){View appearance=getField(a,"root");assertTrue(appearance.findViewWithTag("appearance-0").isSelected());assertNotNull(appearance.findViewWithTag("appearance-2"));}
     if(tag.equals("data-management")){View page=getField(a,"root");for(String action:new String[]{"clear-timetable","clear-tasks","clear-parcels","task-deletion-history"})assertNotNull(page.findViewWithTag(action));}
     a.onBackPressed();settle();assertSame(settings,getField(a,"root"));
    }
    a.onBackPressed();settle();assertNotNull(root(a).findViewWithTag("tasks-page"));assertTrue(root(a).findViewWithTag("nav-tasks").isSelected());
   }
  }
 }
 @Test public void emptyTimetableCreatesDirectlyAndCancelKeepsData()throws Exception{
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity a=controller.get();int current=callInstanceMethod(a,"currentWeek");setField(a,"week",current);assertEquals("第 "+current+" 周",callInstanceMethod(a,"weekCaption"));setField(a,"week",current==1?2:1);assertTrue(((String)callInstanceMethod(a,"weekCaption")).contains("本周第 "+current+" 周"));View add=root(a).findViewWithTag("timetable-import");assertNotNull(add);add.performClick();settle();assertEquals("新增课表",((View)getField(a,"root")).getTag());a.onBackPressed();settle();assertEquals("",getField(a,"data"));
   callInstanceMethod(a,"edit");settle();View emptyAdd=root(a).findViewWithTag("manage-empty-add");assertNotNull(emptyAdd);emptyAdd.performClick();settle();assertEquals("添加课程",((View)getField(a,"root")).getTag());
  }
 }
 @Test public void widgetEmptyStatesKeepTomorrowActionWithoutRepeatedCopy()throws Exception{
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity a=controller.get();String term="2026-10-26";a.getPreferences(0).edit().putString("term",term).putString("courses","1|1|2|合成课程|测试教室|1-30\n2|1|2|明日合成课|测试教室|1-30").commit();
   for(boolean finished:new boolean[]{true,false}){
    if(!finished)a.getPreferences(0).edit().putString("courses","2|1|2|明日合成课|测试教室|1-30").commit();
    View widget=CourseWidget.views(a,0,LocalDate.parse(term).atTime(21,0)).apply(a,new FrameLayout(a));assertEquals(finished?"今日课程已结束":"今日无课",((TextView)widget.findViewById(R.id.widget_empty_title)).getText().toString());assertEquals(1,((ViewGroup)widget.findViewById(R.id.widget_empty_panel)).getChildCount());assertTrue(widget.findViewById(R.id.widget_tomorrow).hasOnClickListeners());
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
