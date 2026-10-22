package cn.courselens;

import android.app.Activity;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import java.time.Duration;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={33,34})
public class SideNavigationTest {
 private SideNavigation drawer(Activity activity,View page,int[] destination,int[] settings){
  SideNavigation drawer=new SideNavigation(activity,page,activity.getColor(R.color.app_surface),activity.getColor(R.color.app_card),activity.getColor(R.color.app_ink),index->destination[0]=index,()->settings[0]++);
  activity.setContentView(drawer);drawer.measure(View.MeasureSpec.makeMeasureSpec(390,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(844,View.MeasureSpec.EXACTLY));drawer.layout(0,0,390,844);return drawer;
 }
 @Test public void shadeClosesPushPanelAndRestoresPageAccessibility(){
  try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
   Activity activity=controller.get();FrameLayout page=new FrameLayout(activity);page.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);int[] selected={-1},settings={0};SideNavigation drawer=drawer(activity,page,selected,settings);
   assertFalse(drawer.isOpen());drawer.open(1);shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(320));
   assertTrue(drawer.isOpen());assertTrue(drawer.findViewWithTag("drawer-tasks").isSelected());assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,page.getImportantForAccessibility());
   assertTrue(((View)page.getParent()).getTranslationX()>0);drawer.findViewWithTag("drawer-shade").performClick();shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(320));
   assertFalse(drawer.isOpen());assertEquals(0f,((View)page.getParent()).getTranslationX(),0f);assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_YES,page.getImportantForAccessibility());assertEquals(-1,selected[0]);assertEquals(0,settings[0]);
  }
 }
 @Test public void destinationsAndSettingsRouteImmediatelyWhileDrawerRetracts(){
  try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
   Activity activity=controller.get();FrameLayout page=new FrameLayout(activity);int[] selected={-1},settings={0};SideNavigation drawer=drawer(activity,page,selected,settings);
   String[] tags={"drawer-timetable","drawer-tasks","drawer-parcels"};
   for(int i=0;i<tags.length;i++){
    selected[0]=-1;drawer.open(0);shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(320));
    drawer.findViewWithTag(tags[i]).performClick();
    assertEquals(i,selected[0]);
    assertTrue(drawer.isOpen());
    assertTrue(((View)page.getParent()).getTranslationX()>0f);
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(320));
    assertFalse(drawer.isOpen());assertEquals(0f,((View)page.getParent()).getTranslationX(),0f);
   }
   drawer.open(2);shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(320));drawer.findViewWithTag("drawer-settings").performClick();
   assertEquals(1,settings[0]);assertTrue(drawer.isOpen());shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(320));
   assertFalse(drawer.isOpen());
  }
 }
 @Test public void rapidTapsRouteOnceAndShadeDoesNotCancelTheChosenPage(){
  try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
   Activity activity=controller.get();FrameLayout page=new FrameLayout(activity);int[] selected={-1},routes={0},settings={0};
   SideNavigation drawer=new SideNavigation(activity,page,activity.getColor(R.color.app_surface),activity.getColor(R.color.app_card),activity.getColor(R.color.app_ink),index->{selected[0]=index;routes[0]++;},()->settings[0]++);
   activity.setContentView(drawer);drawer.measure(View.MeasureSpec.makeMeasureSpec(390,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(844,View.MeasureSpec.EXACTLY));drawer.layout(0,0,390,844);
   drawer.open(0);shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(320));
   drawer.findViewWithTag("drawer-timetable").performClick();drawer.findViewWithTag("drawer-parcels").performClick();drawer.findViewWithTag("drawer-shade").performClick();
   assertEquals(0,selected[0]);assertEquals(1,routes[0]);assertTrue(drawer.isOpen());
   shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(400));
   assertEquals(0,selected[0]);assertEquals(1,routes[0]);assertFalse(drawer.isOpen());
  }
 }
 @Test public void shadeAndBackCloseWithoutRoutingAndReopenReversesAPlainClose(){
  try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
   Activity activity=controller.get();int[] selected={-1},settings={0};SideNavigation drawer=drawer(activity,new FrameLayout(activity),selected,settings);
   drawer.open(0);shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(320));
   drawer.findViewWithTag("drawer-shade").performClick();
   shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(400));
   assertEquals(-1,selected[0]);assertFalse(drawer.isOpen());
   drawer.open(0);shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(320));
   assertTrue(drawer.close(true));
   drawer.open(0);shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(400));
   assertEquals(-1,selected[0]);assertTrue(drawer.isOpen());
  }
 }
 @Test public void reversingCloseAndDetachingDoNotLeavePageHidden(){
  try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
   Activity activity=controller.get();FrameLayout page=new FrameLayout(activity);page.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);SideNavigation drawer=drawer(activity,page,new int[]{-1},new int[]{0});
   drawer.open(0);drawer.close(true);drawer.open(0);drawer.close(false);assertFalse(drawer.isOpen());assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_YES,page.getImportantForAccessibility());
   drawer.open(0);activity.setContentView(new FrameLayout(activity));assertFalse(drawer.isOpen());assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_YES,page.getImportantForAccessibility());
  }
 }
 @Test public void settingsIsAnAccessibleCircleAtTheBottomLeft(){
  try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
   SideNavigation drawer=drawer(controller.get(),new FrameLayout(controller.get()),new int[]{-1},new int[]{0});drawer.open(0);drawer.measure(View.MeasureSpec.makeMeasureSpec(390,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(844,View.MeasureSpec.EXACTLY));drawer.layout(0,0,390,844);
   View gear=drawer.findViewWithTag("drawer-settings");int target=Math.round(48*drawer.getResources().getDisplayMetrics().density);assertEquals(target,gear.getWidth());assertEquals(target,gear.getHeight());assertEquals("设置",gear.getContentDescription());assertTrue(gear.isFocusable());
   android.graphics.Rect bounds=new android.graphics.Rect();gear.getDrawingRect(bounds);drawer.offsetDescendantRectToMyCoords(gear,bounds);assertTrue(bounds.left<drawer.getWidth()/4);assertTrue(bounds.bottom>drawer.getHeight()*3/4);
  }
 }

}
