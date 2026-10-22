package cn.courselens;

import android.app.Activity;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
public class ScrollHeaderTest {
 private void layout(View view){view.measure(View.MeasureSpec.makeMeasureSpec(390,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));view.layout(0,0,390,600);}
 @Test public void contentScrollsBehindPinnedHeaderAndEndRemainsReachable(){
  try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
   Activity activity=controller.get();TextView header=new TextView(activity);header.setHeight(96);ScrollView scroll=new ScrollView(activity);scroll.setSmoothScrollingEnabled(false);View rows=new View(activity);rows.setMinimumHeight(2000);scroll.addView(rows,new ScrollView.LayoutParams(-1,-2));
   ScrollHeader page=new ScrollHeader(activity,header,scroll,activity.getColor(R.color.app_surface),scroll);activity.setContentView(page);layout(page);
   assertEquals(0,scroll.getTop());assertEquals(page.getHeight(),scroll.getHeight());assertTrue(scroll.getPaddingTop()>=header.getBottom());assertEquals(scroll.getPaddingTop(),rows.getTop());
   scroll.scrollTo(0,240);assertEquals(0,header.getTop());assertTrue("header="+header.getBottom()+" rowsTop="+rows.getTop()+" scrollY="+scroll.getScrollY()+" rowsHeight="+rows.getHeight()+" scrollHeight="+scroll.getHeight(),rows.getTop()-scroll.getScrollY()<header.getBottom());
   header.setHeight(144);layout(page);assertTrue(scroll.getPaddingTop()>=header.getBottom());assertEquals(240,scroll.getScrollY());
   scroll.fullScroll(View.FOCUS_DOWN);assertEquals(scroll.getHeight(),rows.getBottom()-scroll.getScrollY());
  }
 }
 @Test public void focusedRowIsRevealedBelowHeader(){
  try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
   Activity activity=controller.get();TextView header=new TextView(activity);header.setHeight(96);ScrollView scroll=new ScrollView(activity);scroll.setSmoothScrollingEnabled(false);LinearLayout rows=new LinearLayout(activity);rows.setOrientation(LinearLayout.VERTICAL);Button focus=new Button(activity);focus.setText("合成任务");rows.addView(focus,new LinearLayout.LayoutParams(-1,48));View space=new View(activity);rows.addView(space,new LinearLayout.LayoutParams(-1,2000));scroll.addView(rows);
   ScrollHeader page=new ScrollHeader(activity,header,scroll,activity.getColor(R.color.app_surface),scroll);activity.setContentView(page);layout(page);scroll.scrollTo(0,240);focus.setFocusableInTouchMode(true);focus.requestFocus();shadowOf(Looper.getMainLooper()).idle();
   int[] field=new int[2],bar=new int[2];focus.getLocationInWindow(field);header.getLocationInWindow(bar);assertTrue(field[1]>=bar[1]+header.getHeight());
  }
 }
}
