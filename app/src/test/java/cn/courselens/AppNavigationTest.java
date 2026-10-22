package cn.courselens;

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
  int[] clicked={-1};AppNavigation bar=new AppNavigation(RuntimeEnvironment.getApplication(),0xfff3f4f5,0xff171717,2,index->clicked[0]=index);
  int width=Math.round(320*bar.getResources().getDisplayMetrics().density),minimum=Math.round(48*bar.getResources().getDisplayMetrics().density);bar.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));bar.layout(0,0,width,bar.getMeasuredHeight());
  String[] tags={"nav-timetable","nav-tasks","nav-parcels"},labels={"课表","任务","快递"};
  for(int i=0;i<tags.length;i++){ImageButton tab=bar.findViewWithTag(tags[i]);assertNotNull(tab);assertEquals(labels[i],tab.getContentDescription());assertEquals(i==2,tab.isSelected());assertNull(tab.getBackground());assertNull(tab.getForeground());assertTrue(tab.getWidth()>=minimum&&tab.getHeight()>=minimum);tab.performClick();assertEquals(i,clicked[0]);}
  bar.select(1);assertTrue(bar.findViewWithTag("nav-tasks").isSelected());assertFalse(bar.findViewWithTag("nav-parcels").isSelected());
 }
 @Test public void mainParcelEntryAndReturningTaskSelection(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);assertTrue(content.findViewWithTag("nav-timetable").isSelected());content.findViewWithTag("side-menu-button").performClick();SideNavigation drawer=content.findViewWithTag("side-navigation");assertTrue(drawer.isOpen());activity.onBackPressed();shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(320));assertFalse(drawer.isOpen());assertFalse(activity.isFinishing());content.findViewWithTag("nav-parcels").performClick();
   Intent open=shadowOf(activity).getNextStartedActivity();assertEquals(ParcelActivity.class.getName(),open.getComponent().getClassName());assertTrue((open.getFlags()&Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)!=0);assertTrue((open.getFlags()&Intent.FLAG_ACTIVITY_NO_ANIMATION)!=0);
   controller.newIntent(new Intent(activity,MainActivity.class).setAction(MainActivity.OPEN_TAB).putExtra("primaryTab",1));assertTrue(content.findViewWithTag("nav-tasks").isSelected());assertNotNull(content.findViewWithTag("tasks-page"));
   controller.newIntent(new Intent(activity,MainActivity.class).setAction(MainActivity.OPEN_TAB).putExtra("primaryTab",0));assertTrue(content.findViewWithTag("nav-timetable").isSelected());
  }
 }
 @Test public void parcelRetainsBottomNavigationAndRoutesBothExistingTabs(){
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
   ParcelActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);assertTrue(content.findViewWithTag("nav-parcels").isSelected());content.findViewWithTag("side-menu-button").performClick();SideNavigation drawer=content.findViewWithTag("side-navigation");assertTrue(drawer.isOpen());assertTrue(content.findViewWithTag("drawer-parcels").isSelected());activity.onBackPressed();shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(320));assertFalse(drawer.isOpen());assertFalse(activity.isFinishing());content.findViewWithTag("nav-parcels").performClick();assertNull(shadowOf(activity).getNextStartedActivity());
   String[] tags={"nav-timetable","nav-tasks"};for(int i=0;i<tags.length;i++){content.findViewWithTag(tags[i]).performClick();Intent open=shadowOf(activity).getNextStartedActivity();assertEquals(MainActivity.class.getName(),open.getComponent().getClassName());assertEquals(MainActivity.OPEN_TAB,open.getAction());assertEquals(i,open.getIntExtra("primaryTab",-1));assertTrue((open.getFlags()&Intent.FLAG_ACTIVITY_CLEAR_TOP)!=0);assertTrue((open.getFlags()&Intent.FLAG_ACTIVITY_SINGLE_TOP)!=0);assertTrue((open.getFlags()&Intent.FLAG_ACTIVITY_NO_ANIMATION)!=0);}
  }
 }
 @Test public void taskActionsLiveInMoreMenuAndConfigurationStillOpens()throws Exception{
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);content.findViewWithTag("nav-tasks").performClick();assertNull(content.findViewWithTag("task-add"));TaskPage page=content.findViewWithTag("tasks-page");java.util.concurrent.ExecutorService worker=org.robolectric.util.ReflectionHelpers.getField(page,"worker");worker.submit(()->{}).get(10,java.util.concurrent.TimeUnit.SECONDS);shadowOf(android.os.Looper.getMainLooper()).idle();
   content.findViewWithTag("tasks-page").findViewWithTag("quick-menu-button").performClick();for(String tag:new String[]{"task-add","chaoxing-refresh","chaoxing-connect"})assertNotNull(content.findViewWithTag(tag));
   assertNull(content.findViewWithTag("task-settings"));assertFalse(content.findViewWithTag("chaoxing-refresh").isEnabled());assertTrue(content.findViewWithTag("chaoxing-connect").isEnabled());content.findViewWithTag("chaoxing-connect").performClick();android.app.AlertDialog configuration=org.robolectric.util.ReflectionHelpers.getField(page,"configuration");assertNotNull(configuration);assertTrue(configuration.isShowing());
  }
 }
 @Test public void parcelActionsRemainAvailableFromMore(){
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
   View content=controller.get().findViewById(android.R.id.content);assertNull(content.findViewWithTag("parcel-import"));content.findViewWithTag("quick-menu-button").performClick();
   android.app.AlertDialog menu=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();for(String tag:new String[]{"parcel-add","parcel-import","parcel-identity"})assertNotNull(menu.getWindow().getDecorView().findViewWithTag(tag));assertNull(menu.getWindow().getDecorView().findViewWithTag("parcel-settings"));menu.getWindow().getDecorView().findViewWithTag("parcel-add").performClick();assertNotSame(menu,org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog());
  }
 }
 private void layoutContent(View content){int density=Math.round(content.getResources().getDisplayMetrics().density);int width=360*density,height=800*density;content.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));content.layout(0,0,width,height);}
 @Test public void bottomTabsSwitchImmediatelyWhileSecondaryPagesStillAnimate(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);layoutContent(content);assertTrue(android.animation.ValueAnimator.areAnimatorsEnabled());
   PageMotion motion=org.robolectric.util.ReflectionHelpers.getField(activity,"pageMotion");android.widget.FrameLayout stage=org.robolectric.util.ReflectionHelpers.getField(activity,"stage");View home=stage.getChildAt(0);
   android.widget.ScrollView scroll=org.robolectric.util.ReflectionHelpers.getField(activity,"timetableScroll");scroll.scrollTo(0,120);int position=scroll.getScrollY();
   for(String tag:new String[]{"nav-tasks","nav-timetable","nav-tasks","nav-timetable"}){content.findViewWithTag(tag).performClick();assertFalse(motion.isRunning());assertEquals(1,stage.getChildCount());assertEquals(1f,stage.getChildAt(0).getAlpha(),0f);assertEquals(0f,stage.getChildAt(0).getTranslationX(),0f);layoutContent(content);}
   assertSame(home,stage.getChildAt(0));assertEquals(position,scroll.getScrollY());
   controller.newIntent(new Intent(activity,MainActivity.class).setAction(MainActivity.OPEN_TAB).putExtra("primaryTab",1).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));assertFalse(motion.isRunning());assertEquals(1,stage.getChildCount());layoutContent(content);
   org.robolectric.util.ReflectionHelpers.callInstanceMethod(activity,"menu");assertTrue(motion.isRunning());
  }
 }
 @Test public void taskEntrySurvivesActivityRecreation(){
  Intent entry=new Intent(RuntimeEnvironment.getApplication(),MainActivity.class).setAction(MainActivity.OPEN_TAB).putExtra("primaryTab",1);
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class,entry).setup()){
   assertTrue(controller.get().findViewById(android.R.id.content).findViewWithTag("nav-tasks").isSelected());controller.recreate();assertTrue(controller.get().findViewById(android.R.id.content).findViewWithTag("nav-tasks").isSelected());
  }
 }
 @Test public void timetableAndTaskGearOpenTheSameSettingsAndReturnToTheirModule()throws Exception{
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);
   for(String module:new String[]{"nav-timetable","nav-tasks"}){
    content.findViewWithTag(module).performClick();content.findViewWithTag("side-menu-button").performClick();content.findViewWithTag("drawer-settings").performClick();
    View root=org.robolectric.util.ReflectionHelpers.getField(activity,"root");assertEquals("设置",root.getTag());assertNotNull(root.findViewWithTag("settings-term"));assertNotNull(root.findViewWithTag("task-settings"));assertNotNull(root.findViewWithTag("parcel-settings"));
    TaskPage page=org.robolectric.util.ReflectionHelpers.getField(activity,"taskPage");java.util.concurrent.ExecutorService worker=org.robolectric.util.ReflectionHelpers.getField(page,"worker");worker.submit(()->{}).get(10,java.util.concurrent.TimeUnit.SECONDS);shadowOf(android.os.Looper.getMainLooper()).idle();
    assertTrue(root.findViewWithTag("task-settings").isEnabled());root.findViewWithTag("task-settings").performClick();assertNotNull(content.findViewWithTag("task-deletion-history"));activity.onBackPressed();assertSame(root,org.robolectric.util.ReflectionHelpers.getField(activity,"root"));activity.onBackPressed();assertTrue(content.findViewWithTag(module).isSelected());
   }
  }
 }
 @Test public void parcelGearRoutesToCommonSettingsAndRetainsItsActivity(){
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
   ParcelActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);content.findViewWithTag("side-menu-button").performClick();content.findViewWithTag("drawer-settings").performClick();
   Intent entry=shadowOf(activity).getNextStartedActivity();assertEquals(MainActivity.class.getName(),entry.getComponent().getClassName());assertEquals(MainActivity.OPEN_SETTINGS,entry.getAction());assertTrue((entry.getFlags()&Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)!=0);assertEquals(0,entry.getFlags()&Intent.FLAG_ACTIVITY_CLEAR_TOP);assertFalse(activity.isFinishing());
  }
 }
 @Test public void commonSettingsFromParcelSurviveRecreationAndReturnToParcel(){
  Intent entry=new Intent(RuntimeEnvironment.getApplication(),MainActivity.class).setAction(MainActivity.OPEN_SETTINGS);
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class,entry).setup()){
   assertEquals("设置",((View)org.robolectric.util.ReflectionHelpers.getField(controller.get(),"root")).getTag());controller.recreate();MainActivity activity=controller.get();assertEquals("设置",((View)org.robolectric.util.ReflectionHelpers.getField(activity,"root")).getTag());
   activity.findViewById(android.R.id.content).findViewWithTag("parcel-settings").performClick();Intent detail=shadowOf(activity).getNextStartedActivity();assertEquals(ParcelActivity.OPEN_SETTINGS,detail.getAction());assertTrue((detail.getFlags()&Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)!=0);
   activity.onBackPressed();Intent back=shadowOf(activity).getNextStartedActivity();assertEquals(ParcelActivity.class.getName(),back.getComponent().getClassName());assertTrue((back.getFlags()&Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)!=0);assertNull(org.robolectric.util.ReflectionHelpers.getField(activity,"pageBack"));
  }
 }
 @Test public void existingMainActivityAlsoAcceptsCommonSettingsFromParcel(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   controller.newIntent(new Intent(controller.get(),MainActivity.class).setAction(MainActivity.OPEN_SETTINGS));assertEquals("设置",((View)org.robolectric.util.ReflectionHelpers.getField(controller.get(),"root")).getTag());assertEquals(Intent.ACTION_MAIN,controller.get().getIntent().getAction());
  }
 }
 @Test public void parcelSettingsDetailOpensForColdAndReusedActivity(){
  Intent entry=new Intent(RuntimeEnvironment.getApplication(),ParcelActivity.class).setAction(ParcelActivity.OPEN_SETTINGS);
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class,entry).setup()){
   android.app.AlertDialog first=org.robolectric.util.ReflectionHelpers.getField(controller.get(),"currentSheet");assertNotNull(first);assertTrue(first.isShowing());first.dismiss();
   controller.newIntent(new Intent(controller.get(),ParcelActivity.class).setAction(ParcelActivity.OPEN_SETTINGS));android.app.AlertDialog second=org.robolectric.util.ReflectionHelpers.getField(controller.get(),"currentSheet");assertNotSame(first,second);assertTrue(second.isShowing());
  }
 }

}
