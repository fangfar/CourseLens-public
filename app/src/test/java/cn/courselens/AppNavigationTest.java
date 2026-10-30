package cn.courselens;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.*;
import android.widget.ImageButton;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={33,34,35,36})
public class AppNavigationTest {
 @After public void finishParcelWork()throws Exception {
  ParcelRepository.IO.submit(()->{}).get(10,java.util.concurrent.TimeUnit.SECONDS);
  shadowOf(android.os.Looper.getMainLooper()).idle();
  ParcelRepository.IO.submit(()->{}).get(10,java.util.concurrent.TimeUnit.SECONDS);
  ParcelDatabase.get(RuntimeEnvironment.getApplication()).close();
  org.robolectric.util.ReflectionHelpers.setStaticField(ParcelDatabase.class,"instance",null);
 }
 @Test public void threeAccessibleTabsKeepExistingTouchStyle(){
  int[] clicked={-1};AppNavigation bar=new AppNavigation(RuntimeEnvironment.getApplication(),0xfff3f4f5,0xff171717,0xff999999,2,index->clicked[0]=index);
  int width=Math.round(320*bar.getResources().getDisplayMetrics().density),minimum=Math.round(48*bar.getResources().getDisplayMetrics().density);bar.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));bar.layout(0,0,width,bar.getMeasuredHeight());
  String[] tags={"nav-timetable","nav-tasks","nav-parcels"},labels={"课表","任务","快递"};
  for(int i=0;i<tags.length;i++){ImageButton tab=bar.findViewWithTag(tags[i]);assertNotNull(tab);assertEquals(labels[i],tab.getContentDescription());assertEquals(i==2,tab.isSelected());assertNull(tab.getBackground());assertNull(tab.getForeground());assertTrue(tab.getWidth()>=minimum&&tab.getHeight()>=minimum);tab.performClick();assertEquals(i,clicked[0]);}
  bar.select(1);assertTrue(bar.findViewWithTag("nav-tasks").isSelected());assertFalse(bar.findViewWithTag("nav-parcels").isSelected());
 }
 @Test public void bottomTabsSwitchInstantlyWithoutPageMotionAndKeepTimetableScroll(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);layoutContent(content);
   PageMotion motion=org.robolectric.util.ReflectionHelpers.getField(activity,"pageMotion");android.widget.FrameLayout stage=org.robolectric.util.ReflectionHelpers.getField(activity,"stage");View home=stage.getChildAt(0);
   android.widget.ScrollView scroll=org.robolectric.util.ReflectionHelpers.getField(activity,"timetableScroll");scroll.scrollTo(0,120);
   content.findViewWithTag("nav-tasks").performClick();
   assertFalse("Primary tab switches must not start a page animation",motion.isRunning());assertEquals(1,stage.getChildCount());assertEquals("tasks-page",stage.getChildAt(0).getTag());assertTrue(content.findViewWithTag("nav-tasks").isSelected());assertFalse(content.findViewWithTag("nav-parcels").isSelected());assertNull(shadowOf(activity).getNextStartedActivity());
   content.findViewWithTag("nav-parcels").performClick();
   assertFalse(motion.isRunning());assertEquals(1,stage.getChildCount());assertEquals("parcel-center",stage.getChildAt(0).getTag());assertTrue(content.findViewWithTag("nav-parcels").isSelected());assertNull(shadowOf(activity).getNextStartedActivity());
   content.findViewWithTag("nav-timetable").performClick();
   assertFalse(motion.isRunning());assertSame(home,stage.getChildAt(0));assertEquals(120,scroll.getScrollY());assertTrue(content.findViewWithTag("nav-timetable").isSelected());
  }
 }
 @Test public void drawerSwitchesAllSixDirectionsWhileTheDrawerRebounds(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);layoutContent(content);
   PageMotion motion=org.robolectric.util.ReflectionHelpers.getField(activity,"pageMotion");android.widget.FrameLayout stage=org.robolectric.util.ReflectionHelpers.getField(activity,"stage");
   String[][] steps={{"drawer-tasks","nav-tasks","tasks-page"},{"drawer-timetable","nav-timetable",null},{"drawer-parcels","nav-parcels","parcel-center"},{"drawer-timetable","nav-timetable",null},{"drawer-tasks","nav-tasks","tasks-page"},{"drawer-parcels","nav-parcels","parcel-center"}};
   for(String[] step:steps){
    content.findViewWithTag("side-menu-button").performClick();SideNavigation drawer=content.findViewWithTag("side-navigation");
    shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(320));assertTrue(drawer.isOpen());
    drawer.findViewWithTag(step[0]).performClick();
    assertTrue("Drawer must stay expanded while the page routes",drawer.isOpen());
    assertFalse("Drawer routes must not add page motion",motion.isRunning());
    assertEquals(1,stage.getChildCount());assertSame(org.robolectric.util.ReflectionHelpers.getField(activity,"root"),stage.getChildAt(0));
    if(step[2]!=null)assertEquals(step[2],stage.getChildAt(0).getTag());
    assertTrue(content.findViewWithTag(step[1]).isSelected());assertTrue(drawer.findViewWithTag(step[0]).isSelected());assertNull(shadowOf(activity).getNextStartedActivity());
    shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(320));assertFalse(drawer.isOpen());
   }
  }
 }
 @Test public void drawerDestinationRetargetingDuringReboundIsIgnored(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);layoutContent(content);
   PageMotion motion=org.robolectric.util.ReflectionHelpers.getField(activity,"pageMotion");android.widget.FrameLayout stage=org.robolectric.util.ReflectionHelpers.getField(activity,"stage");
   content.findViewWithTag("side-menu-button").performClick();SideNavigation drawer=content.findViewWithTag("side-navigation");
   shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(320));
   drawer.findViewWithTag("drawer-tasks").performClick();View tasks=stage.getChildAt(0);
   drawer.findViewWithTag("drawer-timetable").performClick();
   assertTrue(drawer.isOpen());assertFalse(motion.isRunning());assertSame(tasks,stage.getChildAt(0));assertEquals("tasks-page",tasks.getTag());assertTrue(content.findViewWithTag("nav-tasks").isSelected());
   shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(320));assertFalse(drawer.isOpen());assertSame(tasks,stage.getChildAt(0));
  }
 }
 @Test public void currentDrawerDestinationOnlyClosesWithoutRebuildingThePage(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);layoutContent(content);
   android.widget.FrameLayout stage=org.robolectric.util.ReflectionHelpers.getField(activity,"stage");PageMotion motion=org.robolectric.util.ReflectionHelpers.getField(activity,"pageMotion");
   String[] nav={"nav-timetable","nav-tasks","nav-parcels"},destinations={"drawer-timetable","drawer-tasks","drawer-parcels"};
   for(int i=0;i<nav.length;i++){
    if(i>0)content.findViewWithTag(nav[i]).performClick();
    View page=org.robolectric.util.ReflectionHelpers.getField(activity,"root");
    content.findViewWithTag("side-menu-button").performClick();SideNavigation drawer=content.findViewWithTag("side-navigation");
    shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(320));assertTrue(drawer.isOpen());
    drawer.findViewWithTag(destinations[i]).performClick();
    assertTrue(drawer.isOpen());assertFalse(motion.isRunning());assertEquals(1,stage.getChildCount());assertSame(page,stage.getChildAt(0));
    shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(320));assertFalse(drawer.isOpen());assertSame(page,stage.getChildAt(0));assertTrue(content.findViewWithTag(nav[i]).isSelected());
   }
  }
 }
 @Test public void rapidBottomTabsLatestTapWinsWithoutCompetingMotions(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);layoutContent(content);
   PageMotion motion=org.robolectric.util.ReflectionHelpers.getField(activity,"pageMotion");android.widget.FrameLayout stage=org.robolectric.util.ReflectionHelpers.getField(activity,"stage");View home=stage.getChildAt(0);
   content.findViewWithTag("nav-tasks").performClick();content.findViewWithTag("nav-timetable").performClick();
   assertFalse(motion.isRunning());assertEquals(1,stage.getChildCount());assertSame(home,stage.getChildAt(0));assertTrue(content.findViewWithTag("nav-timetable").isSelected());
  }
 }
 @Test public void parcelTabKeepsItsPageAndScrollWhenSwitchingTabs()throws Exception {
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);layoutContent(content);
   android.widget.FrameLayout stage=org.robolectric.util.ReflectionHelpers.getField(activity,"stage");
   content.findViewWithTag("nav-parcels").performClick();drainParcelWork();View parcel=stage.getChildAt(0);assertEquals("parcel-center",parcel.getTag());
   android.widget.ScrollView scroll=parcel.findViewWithTag("parcel-scroll");assertNotNull(scroll);int y=scroll.getScrollY();assertNull(parcel.findViewWithTag("parcel-filter-pager"));
   content.findViewWithTag("nav-timetable").performClick();content.findViewWithTag("nav-parcels").performClick();drainParcelWork();
   assertSame(parcel,stage.getChildAt(0));assertSame(scroll,parcel.findViewWithTag("parcel-scroll"));assertEquals(y,scroll.getScrollY());assertTrue(content.findViewWithTag("nav-parcels").isSelected());
  }
 }
 @Test public void openTabIntentsSwitchInstantlyAndRepeatedIntentKeepsThePage(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);layoutContent(content);
   android.widget.FrameLayout stage=org.robolectric.util.ReflectionHelpers.getField(activity,"stage");PageMotion motion=org.robolectric.util.ReflectionHelpers.getField(activity,"pageMotion");
   View home=stage.getChildAt(0);android.widget.ScrollView scroll=org.robolectric.util.ReflectionHelpers.getField(activity,"timetableScroll");scroll.scrollTo(0,120);
   controller.newIntent(primaryTab(activity,1));
   assertFalse(motion.isRunning());assertEquals(1,stage.getChildCount());View tasks=stage.getChildAt(0);assertEquals("tasks-page",tasks.getTag());assertTrue(content.findViewWithTag("nav-tasks").isSelected());
   controller.newIntent(primaryTab(activity,1));assertFalse(motion.isRunning());assertSame(tasks,stage.getChildAt(0));
   controller.newIntent(primaryTab(activity,2));assertFalse(motion.isRunning());View parcel=stage.getChildAt(0);assertEquals("parcel-center",parcel.getTag());assertTrue(content.findViewWithTag("nav-parcels").isSelected());
   controller.newIntent(primaryTab(activity,2));assertFalse(motion.isRunning());assertSame(parcel,stage.getChildAt(0));
   controller.newIntent(primaryTab(activity,0));assertFalse(motion.isRunning());assertSame(home,stage.getChildAt(0));assertEquals(120,scroll.getScrollY());assertTrue(content.findViewWithTag("nav-timetable").isSelected());
  }
 }
 @Test public void taskEntrySurvivesActivityRecreation(){
  Intent entry=new Intent(RuntimeEnvironment.getApplication(),MainActivity.class).setAction(MainActivity.OPEN_TAB).putExtra("primaryTab",1);
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class,entry).setup()){
   assertTrue(controller.get().findViewById(android.R.id.content).findViewWithTag("nav-tasks").isSelected());controller.recreate();assertTrue(controller.get().findViewById(android.R.id.content).findViewWithTag("nav-tasks").isSelected());
  }
 }
 @Test public void parcelEntrySurvivesActivityRecreation(){
  Intent entry=new Intent(RuntimeEnvironment.getApplication(),MainActivity.class).setAction(MainActivity.OPEN_TAB).putExtra("primaryTab",2);
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class,entry).setup()){
   View content=controller.get().findViewById(android.R.id.content);assertTrue(content.findViewWithTag("nav-parcels").isSelected());assertEquals("parcel-center",((View)org.robolectric.util.ReflectionHelpers.getField(controller.get(),"root")).getTag());controller.recreate();content=controller.get().findViewById(android.R.id.content);assertTrue(content.findViewWithTag("nav-parcels").isSelected());assertEquals("parcel-center",((View)org.robolectric.util.ReflectionHelpers.getField(controller.get(),"root")).getTag());
  }
 }
 @Test public void taskAddUsesHeaderAndConfigurationLivesInManagement()throws Exception {
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);content.findViewWithTag("nav-tasks").performClick();TaskPage page=content.findViewWithTag("tasks-page");java.util.concurrent.ExecutorService worker=org.robolectric.util.ReflectionHelpers.getField(page,"worker");worker.submit(()->{}).get(10,java.util.concurrent.TimeUnit.SECONDS);shadowOf(android.os.Looper.getMainLooper()).idle();
   assertNotNull(page.findViewWithTag("task-add"));assertEquals("打开导航菜单",((android.view.ViewGroup)page.findViewWithTag("task-add").getParent()).getChildAt(0).getContentDescription());assertNull(page.findViewWithTag("quick-menu-button"));assertNull(page.findViewWithTag("chaoxing-connect"));page.findViewWithTag("task-settings").performClick();assertNotNull(content.findViewWithTag("chaoxing-connect"));content.findViewWithTag("chaoxing-connect").performClick();android.app.AlertDialog configuration=org.robolectric.util.ReflectionHelpers.getField(page,"configuration");assertNotNull(configuration);assertTrue(configuration.isShowing());
  }
 }
 @Test public void parcelAddOpensEditorAndReadingRemainsDirect()throws Exception{
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
   View content=controller.get().findViewById(android.R.id.content);ParcelRepository.IO.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idle();assertNotNull(content.findViewWithTag("parcel-read-info"));content.findViewWithTag("quick-menu-button").performClick();
   android.app.AlertDialog editor=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(editor.getWindow().getDecorView().findViewWithTag("parcel-editor-save"));assertNull(editor.getWindow().getDecorView().findViewWithTag("parcel-import"));((MotionSheetDialog)editor).dismissNow();content.findViewWithTag("parcel-read-info").performClick();android.app.AlertDialog read=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotSame(editor,read);assertNotNull(read.getWindow().getDecorView().findViewWithTag("parcel-import-text"));
  }
 }
 @Test public void commonSettingsContainMigratedTimetableOptionsAndReturnToTheirModule()throws Exception {
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);
   for(String module:new String[]{"nav-timetable","nav-tasks"}){
    content.findViewWithTag(module).performClick();content.findViewWithTag("side-menu-button").performClick();content.findViewWithTag("drawer-settings").performClick();
    View root=org.robolectric.util.ReflectionHelpers.getField(activity,"root");assertEquals("设置",root.getTag());for(String tag:new String[]{"settings-term","add-home-widget","school-account","data-management"})assertNotNull(root.findViewWithTag(tag));assertNull(root.findViewWithTag("task-settings"));assertNull(root.findViewWithTag("parcel-settings"));assertNotNull(root.findViewWithTag("timetable-management"));assertNull(root.findViewWithTag("switch-timetable"));assertNotNull(root.findViewWithTag("dark-mode-setting"));assertNotNull(root.findViewWithTag("about-app"));
    for(String tag:new String[]{"settings-term","add-home-widget"}){root.findViewWithTag(tag).performClick();shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(320));activity.onBackPressed();assertSame(root,org.robolectric.util.ReflectionHelpers.getField(activity,"root"));}
    shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(320));activity.onBackPressed();assertTrue(content.findViewWithTag(module).isSelected());settle(org.robolectric.util.ReflectionHelpers.getField(activity,"stage"));
   }
  }
 }
 @Test public void timetableSettingsReopenTheirMenuWhenReturningFromChildren(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);content.findViewWithTag("timetable-settings").performClick();View root=org.robolectric.util.ReflectionHelpers.getField(activity,"root");assertEquals("课表设置",root.getTag());assertNotNull(root.findViewWithTag("adjustment-manager"));assertNotNull(root.findViewWithTag("timetable-login"));for(String tag:new String[]{"add-timetable","settings-times","manage-courses","switch-timetable","refresh-timetable"})assertNull(root.findViewWithTag(tag));root.findViewWithTag("timetable-login").performClick();assertNotNull(content.findViewWithTag("select-school"));activity.onBackPressed();assertTrue(content.findViewWithTag("nav-timetable").isSelected());
  }
 }
 @Test public void timetableMenuAddsCourseWithMultipleSlots(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity a=controller.get();View content=a.findViewById(android.R.id.content);content.findViewWithTag("timetable-settings").performClick();content.findViewWithTag("timetable-add-course").performClick();View form=org.robolectric.util.ReflectionHelpers.getField(a,"root");assertEquals("添加课程",form.getTag());((android.widget.EditText)form.findViewWithTag("course-name")).setText("菜单新增课程");form.findViewWithTag("add-course-slot").performClick();assertNotNull(form.findViewWithTag("course-slot-2"));form.findViewWithTag("save-course").performClick();assertEquals(2,Course.parse(a.getPreferences(0).getString("courses","")).size());
  }
 }
 @Test public void parcelGearInTheEmbeddedPageReturnsToTheParcelModule(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);layoutContent(content);
   content.findViewWithTag("nav-parcels").performClick();
   content.findViewWithTag("side-menu-button").performClick();shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(320));content.findViewWithTag("drawer-settings").performClick();
   View root=org.robolectric.util.ReflectionHelpers.getField(activity,"root");assertEquals("设置",root.getTag());assertNull(root.findViewWithTag("parcel-settings"));
   shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(320));
   activity.onBackPressed();
   settle(org.robolectric.util.ReflectionHelpers.getField(activity,"stage"));layoutContent(content);
   assertEquals("parcel-center",((View)org.robolectric.util.ReflectionHelpers.getField(activity,"root")).getTag());assertTrue(content.findViewWithTag("nav-parcels").isSelected());
  }
 }
 @Test public void parcelManagementStaysInItsModuleAndReportsFeedback(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);layoutContent(content);content.findViewWithTag("nav-parcels").performClick();View root=org.robolectric.util.ReflectionHelpers.getField(activity,"root");root.findViewWithTag("parcel-settings").performClick();assertSame(root,org.robolectric.util.ReflectionHelpers.getField(activity,"root"));assertTrue(content.findViewWithTag("nav-parcels").isSelected());assertNull(shadowOf(activity).getNextStartedActivity());
   ParcelPage page=org.robolectric.util.ReflectionHelpers.getField(activity,"parcelPage");AlertDialog sheet=org.robolectric.util.ReflectionHelpers.getField(page,"currentSheet");assertNotNull(sheet);assertTrue(sheet.isShowing());org.robolectric.util.ReflectionHelpers.callInstanceMethod(page,"message",org.robolectric.util.ReflectionHelpers.ClassParameter.from(String.class,"合成反馈"));assertEquals("合成反馈",org.robolectric.shadows.ShadowToast.getTextOfLatestToast());sheet.dismiss();assertSame(root,org.robolectric.util.ReflectionHelpers.getField(activity,"root"));
  }
 }
 @Test public void commonSettingsFromParcelActivityReturnsToHostOnBack(){
  Intent entry=new Intent(RuntimeEnvironment.getApplication(),MainActivity.class).setAction(MainActivity.OPEN_SETTINGS);
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class,entry).setup()){
   assertEquals("设置",((View)org.robolectric.util.ReflectionHelpers.getField(controller.get(),"root")).getTag());controller.recreate();MainActivity activity=controller.get();assertEquals("设置",((View)org.robolectric.util.ReflectionHelpers.getField(activity,"root")).getTag());
   activity.onBackPressed();Intent back=shadowOf(activity).getNextStartedActivity();assertNotNull(back);assertEquals(ParcelActivity.class.getName(),back.getComponent().getClassName());assertTrue((back.getFlags()&Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)!=0);assertNull(org.robolectric.util.ReflectionHelpers.getField(activity,"pageBack"));
  }
 }
 @Test public void existingMainActivityAlsoAcceptsCommonSettingsFromParcel(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   controller.newIntent(new Intent(controller.get(),MainActivity.class).setAction(MainActivity.OPEN_SETTINGS));assertEquals("设置",((View)org.robolectric.util.ReflectionHelpers.getField(controller.get(),"root")).getTag());assertEquals(Intent.ACTION_MAIN,controller.get().getIntent().getAction());
  }
 }
 @Test public void parcelHostRetainsBottomNavigationAndRoutesBothExistingTabs(){
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
   ParcelActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);assertTrue(content.findViewWithTag("nav-parcels").isSelected());content.findViewWithTag("side-menu-button").performClick();SideNavigation drawer=content.findViewWithTag("side-navigation");assertTrue(drawer.isOpen());assertTrue(content.findViewWithTag("drawer-parcels").isSelected());activity.onBackPressed();shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(320));assertFalse(drawer.isOpen());assertFalse(activity.isFinishing());content.findViewWithTag("nav-parcels").performClick();assertNull(shadowOf(activity).getNextStartedActivity());
   String[] tags={"nav-timetable","nav-tasks"};for(int i=0;i<tags.length;i++){content.findViewWithTag(tags[i]).performClick();Intent open=shadowOf(activity).getNextStartedActivity();assertNotNull(open);assertEquals(MainActivity.class.getName(),open.getComponent().getClassName());assertEquals(MainActivity.OPEN_TAB,open.getAction());assertEquals(i,open.getIntExtra("primaryTab",-1));assertTrue((open.getFlags()&Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)!=0);assertEquals(0,open.getFlags()&Intent.FLAG_ACTIVITY_NO_ANIMATION);org.robolectric.util.ReflectionHelpers.setField(activity,"navigatingMain",false);}
  }
 }
 @Test public void parcelHostSamePageOnlyClosesAndOtherPagesStartImmediately(){
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
   ParcelActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);
   content.findViewWithTag("side-menu-button").performClick();SideNavigation drawer=content.findViewWithTag("side-navigation");
   shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(320));drawer.findViewWithTag("drawer-parcels").performClick();
   assertNull(shadowOf(activity).getNextStartedActivity());assertTrue(drawer.isOpen());
   shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(320));assertFalse(drawer.isOpen());
   drawer.open(2);shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(320));drawer.findViewWithTag("drawer-tasks").performClick();
   Intent open=shadowOf(activity).getNextStartedActivity();assertNotNull(open);assertEquals(MainActivity.class.getName(),open.getComponent().getClassName());assertEquals(MainActivity.OPEN_TAB,open.getAction());assertEquals(1,open.getIntExtra("primaryTab",-1));assertTrue((open.getFlags()&Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)!=0);assertEquals(0,open.getFlags()&Intent.FLAG_ACTIVITY_CLEAR_TOP);assertEquals(0,open.getFlags()&Intent.FLAG_ACTIVITY_NO_ANIMATION);assertFalse(activity.isFinishing());assertTrue(drawer.isOpen());
  }
 }
 @Test public void parcelHostGearRoutesToCommonSettingsAndRetainsItsActivity(){
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
   ParcelActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);content.findViewWithTag("side-menu-button").performClick();content.findViewWithTag("drawer-settings").performClick();
   Intent entry=shadowOf(activity).getNextStartedActivity();assertNotNull(entry);assertEquals(MainActivity.class.getName(),entry.getComponent().getClassName());assertEquals(MainActivity.OPEN_SETTINGS,entry.getAction());assertTrue((entry.getFlags()&Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)!=0);assertEquals(0,entry.getFlags()&Intent.FLAG_ACTIVITY_CLEAR_TOP);assertFalse(activity.isFinishing());
  }
 }
 @Test public void parcelSettingsDetailOpensForColdAndReusedActivity(){
  Intent entry=new Intent(RuntimeEnvironment.getApplication(),ParcelActivity.class).setAction(ParcelActivity.OPEN_SETTINGS);
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class,entry).setup()){
   ParcelPage page=org.robolectric.util.ReflectionHelpers.getField(controller.get(),"page");android.app.AlertDialog first=org.robolectric.util.ReflectionHelpers.getField(page,"currentSheet");assertNotNull(first);assertTrue(first.isShowing());first.dismiss();
   controller.newIntent(new Intent(controller.get(),ParcelActivity.class).setAction(ParcelActivity.OPEN_SETTINGS));page=org.robolectric.util.ReflectionHelpers.getField(controller.get(),"page");android.app.AlertDialog second=org.robolectric.util.ReflectionHelpers.getField(page,"currentSheet");assertNotSame(first,second);assertTrue(second.isShowing());
  }
 }
 private void drainParcelWork()throws Exception {ParcelRepository.IO.submit(()->{}).get(10,java.util.concurrent.TimeUnit.SECONDS);shadowOf(android.os.Looper.getMainLooper()).idle();}
 private void layoutContent(View content){int density=Math.round(content.getResources().getDisplayMetrics().density);int width=360*density,height=800*density;content.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));content.layout(0,0,width,height);}
 private Intent primaryTab(MainActivity activity,int tab){return new Intent(activity,MainActivity.class).setAction(MainActivity.OPEN_TAB).putExtra("primaryTab",tab).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);}
 private void settle(android.widget.FrameLayout stage){shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(320));if(stage.getViewTreeObserver().isAlive())stage.getViewTreeObserver().dispatchOnPreDraw();shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(360));}
}
