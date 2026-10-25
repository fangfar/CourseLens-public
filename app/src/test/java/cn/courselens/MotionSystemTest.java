package cn.courselens;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import java.time.Duration;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class) @Config(sdk={33,34})
public class MotionSystemTest {
 @Before public void enableAnimations(){org.robolectric.shadows.ShadowChoreographer.setPaused(true);org.robolectric.shadows.ShadowChoreographer.setFrameDelay(Duration.ofMillis(16));org.robolectric.util.ReflectionHelpers.callStaticMethod(android.animation.ValueAnimator.class,"setDurationScale",org.robolectric.util.ReflectionHelpers.ClassParameter.from(float.class,1f));}
 private void finish(Motion.Progress p){android.animation.ValueAnimator animator=org.robolectric.util.ReflectionHelpers.getField(p,"animator");if(animator!=null)animator.end();shadowOf(Looper.getMainLooper()).idle();}
 private void seek(Motion.Progress p,long ms){android.animation.ValueAnimator animator=org.robolectric.util.ReflectionHelpers.getField(p,"animator");assertNotNull(animator);animator.setCurrentPlayTime(ms);}
 private void layout(View view){org.robolectric.shadows.ShadowChoreographer.setPaused(false);shadowOf(Looper.getMainLooper()).idle();org.robolectric.shadows.ShadowChoreographer.setPaused(true);view.measure(View.MeasureSpec.makeMeasureSpec(390,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(844,View.MeasureSpec.EXACTLY));view.layout(0,0,390,844);org.robolectric.util.ReflectionHelpers.callStaticMethod(android.animation.ValueAnimator.class,"setDurationScale",org.robolectric.util.ReflectionHelpers.ClassParameter.from(float.class,1f));view.getViewTreeObserver().dispatchOnPreDraw();}
 @Test public void reversingUsesCurrentProgressAndOnlyCommitsTheLatestEnd(){
  float[] drawn={0};int[] commits={0};Motion.Progress progress=new Motion.Progress(0,p->drawn[0]=p);
  progress.to(1,Motion.SHEET_IN,false,()->commits[0]++);seek(progress,120);float interrupted=progress.value;assertTrue("progress="+interrupted+" enabled="+android.animation.ValueAnimator.areAnimatorsEnabled(),interrupted>0&&interrupted<1);
  progress.to(0,Motion.SHEET_OUT,false,()->commits[0]+=10);assertEquals(interrupted,progress.value,0);finish(progress);assertEquals(0,drawn[0],0);assertEquals(10,commits[0]);
  progress.to(1,Motion.SHEET_IN,true,()->commits[0]++);finish(progress);assertEquals(1,drawn[0],0);assertEquals(11,commits[0]);
 }
 @Test public void courseDetailKeepsHalfHeightAndSlidesAnOpaquePanel(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup().visible()){
   MainActivity activity=controller.get();layout(activity.findViewById(android.R.id.content));MainActivity.NavigationOverlay sheet=activity.new NavigationOverlay(false,"课程详情");layout(sheet);sheet.motion.set(0);assertEquals(sheet.getHeight()/2,sheet.panel.getHeight());assertEquals(sheet.panel.getHeight(),sheet.panel.getTranslationY(),.01f);sheet.motion.set(.4f);assertEquals(1,sheet.panel.getAlpha(),0);float y=sheet.panel.getTranslationY();sheet.beginBack();sheet.backProgress(0);assertEquals(y,sheet.panel.getTranslationY(),.01f);sheet.cancelBack();finish(sheet.motion);assertEquals("progress before close",1f,sheet.motion.value,0);sheet.close(false);assertNull(sheet.getParent());assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO,((View)org.robolectric.util.ReflectionHelpers.getField(activity,"root")).getImportantForAccessibility());
  }
 }
 @Test public void sheetDragFollowsFingerAndCancelReboundsWithoutChangingContent(){
  try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup().visible()){
   Activity activity=controller.get();MotionOverlay overlay=new MotionOverlay(activity);View shade=new View(activity);LinearLayout panel=new LinearLayout(activity);panel.addView(new TextView(activity));overlay.addView(shade,new FrameLayout.LayoutParams(-1,-1));overlay.addView(panel,new FrameLayout.LayoutParams(-1,300,Gravity.BOTTOM));int[] dismiss={0};overlay.bindMotion(panel,shade,false,()->dismiss[0]++);activity.setContentView(overlay);layout(overlay);overlay.motion.set(1);
   long t=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,100,560,0);overlay.onInterceptTouchEvent(down);down.recycle();MotionEvent move=MotionEvent.obtain(t,t+40,MotionEvent.ACTION_MOVE,100,610,0);assertTrue(overlay.onInterceptTouchEvent(move));overlay.onTouchEvent(move);move.recycle();assertEquals(50,panel.getTranslationY(),.01f);assertEquals(overlay.motion.value,shade.getAlpha(),.001f);
   MotionEvent cancel=MotionEvent.obtain(t,t+70,MotionEvent.ACTION_CANCEL,100,610,0);overlay.onTouchEvent(cancel);cancel.recycle();finish(overlay.motion);assertEquals(0,panel.getTranslationY(),.01f);assertEquals(0,dismiss[0]);assertEquals(1,shade.getAlpha(),0);
  }
 }
 @Test public void pageReversePreservesTheVisibleFrameAndReleasesInputOnce(){
  try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup().visible()){
   Activity activity=controller.get();FrameLayout stage=new FrameLayout(activity);View old=new View(activity),next=new View(activity);stage.addView(old);stage.addView(next);activity.setContentView(stage);layout(stage);PageMotion motion=new PageMotion();motion.start(stage,old,next,false,24);ViewTreeObserver.OnPreDrawListener pending=org.robolectric.util.ReflectionHelpers.getField(motion,"pending");assertNotNull(pending);pending.onPreDraw();Motion.Progress p=org.robolectric.util.ReflectionHelpers.getField(motion,"motion");seek(p,90);float x=next.getTranslationX();int[] end={0};motion.reverse(()->end[0]++);assertEquals(x,next.getTranslationX(),0);motion.reverse(()->end[0]+=10);finish(p);assertFalse(motion.isRunning());assertEquals(1,end[0]);assertEquals(1,stage.getChildCount());assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO,stage.getImportantForAccessibility());
  }
 }
 @Test public void nestedSheetKeepsItsParentStableWhileReturningAndCancelling(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity a=controller.get();MainActivity.NavigationOverlay parent=a.new NavigationOverlay(false,"父面板");layout(parent);parent.motion.set(1);
   MainActivity.NavigationOverlay child=a.new NavigationOverlay(false,"子面板");layout(child);child.motion.set(1);
   assertEquals(View.VISIBLE,parent.getVisibility());assertEquals(1,parent.panel.getAlpha(),0);
   child.beginBack();child.backProgress(.4f);assertEquals(1,parent.panel.getAlpha(),0);assertTrue(child.panel.getTranslationY()>0);assertEquals(0,parent.panel.getTranslationY(),0);assertEquals(1,parent.shade.getAlpha(),0);assertEquals(0,child.shade.getAlpha(),0);
   float position=child.panel.getTranslationY();child.beginBack();child.backProgress(0);assertEquals(position,child.panel.getTranslationY(),0);
   child.cancelBack();finish(child.motion);assertEquals(1,parent.panel.getAlpha(),0);assertTrue(child.open);
   child.close(false);assertEquals(1,parent.panel.getAlpha(),0);assertSame(parent,org.robolectric.util.ReflectionHelpers.getField(a,"navigationOverlay"));parent.close(false);
  }
 }
 @Test public void disabledSystemAnimationsCommitOnceWithoutAnAnimator(){
  org.robolectric.util.ReflectionHelpers.callStaticMethod(android.animation.ValueAnimator.class,"setDurationScale",org.robolectric.util.ReflectionHelpers.ClassParameter.from(float.class,0f));
  int[] end={0};Motion.Progress progress=new Motion.Progress(0,p->{});progress.to(1,Motion.SHEET_IN,false,()->end[0]++);assertEquals(1,progress.value,0);assertFalse(progress.running());assertEquals(1,end[0]);
 }

 @Test public void menuNearBottomOpensAboveItsAnchorWithoutClipping(){
  try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
   Activity activity=controller.get();MotionOverlay overlay=new MotionOverlay(activity);View shade=new View(activity);TextView panel=new TextView(activity);panel.setMinHeight(180);overlay.addView(shade);FrameLayout.LayoutParams size=new FrameLayout.LayoutParams(184,-2,Gravity.TOP|Gravity.END);overlay.addView(panel,size);overlay.bindMotion(panel,shade,true,()->{});overlay.placeMenu(844,800,48);overlay.measure(View.MeasureSpec.makeMeasureSpec(390,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(844,View.MeasureSpec.EXACTLY));overlay.layout(0,0,390,844);assertTrue(panel.getBottom()<=752);assertTrue(panel.getTop()>=12);assertEquals(panel.getHeight(),panel.getPivotY(),0);
  }
 }

 @Test public void predictiveBackDuringEntryCancelsFromTheCurrentFrame(){
  try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
   Activity a=controller.get();FrameLayout stage=new FrameLayout(a);View previous=new View(a),next=new View(a);stage.addView(previous);stage.addView(next);a.setContentView(stage);layout(stage);PageMotion motion=new PageMotion();motion.start(stage,previous,next,false,24);((ViewTreeObserver.OnPreDrawListener)org.robolectric.util.ReflectionHelpers.getField(motion,"pending")).onPreDraw();Motion.Progress p=org.robolectric.util.ReflectionHelpers.getField(motion,"motion");seek(p,90);float start=p.value;motion.beginBack();motion.backProgress(0);assertEquals(start,p.value,0);motion.backProgress(.4f);assertEquals(start*.6f,p.value,.001f);assertTrue(previous.getAlpha()>0);motion.cancelBack();finish(p);assertFalse(motion.isRunning());assertEquals(0,next.getTranslationX(),0);assertEquals(1,next.getAlpha(),0);
  }
 }

 @Test public void restartingAStablePageBackGestureKeepsItsCurrentVisualOffset(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity a=controller.get();org.robolectric.util.ReflectionHelpers.callInstanceMethod(a,"menu");((PageMotion)org.robolectric.util.ReflectionHelpers.getField(a,"pageMotion")).cancel();View root=org.robolectric.util.ReflectionHelpers.getField(a,"root");root.setTranslationX(7);root.setAlpha(.95f);org.robolectric.util.ReflectionHelpers.callInstanceMethod(a,"startBackMotion");org.robolectric.util.ReflectionHelpers.callInstanceMethod(a,"progressBackMotion",org.robolectric.util.ReflectionHelpers.ClassParameter.from(float.class,0f));assertEquals(7,root.getTranslationX(),0);assertEquals(.95f,root.getAlpha(),0);
  }
 }

}
