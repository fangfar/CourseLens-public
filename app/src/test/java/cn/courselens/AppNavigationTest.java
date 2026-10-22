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
  int[] clicked={-1};AppNavigation bar=new AppNavigation(RuntimeEnvironment.getApplication(),0xfff3f4f5,0xff171717,0xff999999,2,index->clicked[0]=index);
  int width=Math.round(320*bar.getResources().getDisplayMetrics().density),minimum=Math.round(48*bar.getResources().getDisplayMetrics().density);bar.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));bar.layout(0,0,width,bar.getMeasuredHeight());
  String[] tags={"nav-timetable","nav-tasks","nav-parcels"},labels={"课表","任务","快递"};
  for(int i=0;i<tags.length;i++){ImageButton tab=bar.findViewWithTag(tags[i]);assertNotNull(tab);assertEquals(labels[i],tab.getContentDescription());assertEquals(i==2,tab.isSelected());assertNull(tab.getBackground());assertNull(tab.getForeground());assertTrue(tab.getWidth()>=minimum&&tab.getHeight()>=minimum);tab.performClick();assertEquals(i,clicked[0]);}
  bar.select(1);assertTrue(bar.findViewWithTag("nav-tasks").isSelected());assertFalse(bar.findViewWithTag("nav-parcels").isSelected());
 }
 @Test public void mainParcelEntryAndReturningTaskSelection(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);assertTrue(content.findViewWithTag("nav-timetable").isSelected());content.findViewWithTag("nav-parcels").performClick();
   Intent open=shadowOf(activity).getNextStartedActivity();assertEquals(ParcelActivity.class.getName(),open.getComponent().getClassName());assertTrue((open.getFlags()&Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)!=0);
   controller.newIntent(new Intent(activity,MainActivity.class).setAction(MainActivity.OPEN_TAB).putExtra("primaryTab",1));assertTrue(content.findViewWithTag("nav-tasks").isSelected());assertNotNull(content.findViewWithTag("tasks-page"));
   controller.newIntent(new Intent(activity,MainActivity.class).setAction(MainActivity.OPEN_TAB).putExtra("primaryTab",0));assertTrue(content.findViewWithTag("nav-timetable").isSelected());
  }
 }
 @Test public void parcelRetainsBottomNavigationAndRoutesBothExistingTabs(){
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
   ParcelActivity activity=controller.get();View content=activity.findViewById(android.R.id.content);assertTrue(content.findViewWithTag("nav-parcels").isSelected());content.findViewWithTag("nav-parcels").performClick();assertNull(shadowOf(activity).getNextStartedActivity());
   String[] tags={"nav-timetable","nav-tasks"};for(int i=0;i<tags.length;i++){content.findViewWithTag(tags[i]).performClick();Intent open=shadowOf(activity).getNextStartedActivity();assertEquals(MainActivity.class.getName(),open.getComponent().getClassName());assertEquals(MainActivity.OPEN_TAB,open.getAction());assertEquals(i,open.getIntExtra("primaryTab",-1));assertTrue((open.getFlags()&Intent.FLAG_ACTIVITY_CLEAR_TOP)!=0);assertTrue((open.getFlags()&Intent.FLAG_ACTIVITY_SINGLE_TOP)!=0);}
  }
 }
 @Test public void taskEntrySurvivesActivityRecreation(){
  Intent entry=new Intent(RuntimeEnvironment.getApplication(),MainActivity.class).setAction(MainActivity.OPEN_TAB).putExtra("primaryTab",1);
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class,entry).setup()){
   assertTrue(controller.get().findViewById(android.R.id.content).findViewWithTag("nav-tasks").isSelected());controller.recreate();assertTrue(controller.get().findViewById(android.R.id.content).findViewWithTag("nav-tasks").isSelected());
  }
 }
}
