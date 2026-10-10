package cn.courselens;

import android.animation.*;
import android.view.*;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

// One owner for pending layout, animation, input blocking and cancellation cleanup.
final class PageMotion {
 private static final PathInterpolator EASING=new PathInterpolator(.4f,0f,.2f,1f);
 private FrameLayout stage;
 private View outgoing,incoming;
 private ViewTreeObserver.OnPreDrawListener pending;
 private ValueAnimator animator;
 boolean isRunning(){return incoming!=null;}
 void start(FrameLayout container,View previous,View next,boolean backwards,boolean fadeThrough,float distance){
  cancel();stage=container;outgoing=previous;incoming=next;
  stage.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
  previous.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
  float direction=backwards?-1f:1f;
  next.setAlpha(0f);next.setTranslationX(fadeThrough?0f:direction*distance);
  next.setScaleX(fadeThrough?.98f:1f);next.setScaleY(fadeThrough?.98f:1f);
  pending=()->{
   next.getViewTreeObserver().removeOnPreDrawListener(pending);pending=null;
   if(!ValueAnimator.areAnimatorsEnabled()){finish();return true;}
   long duration=fadeThrough?200:("课程详情".equals(next.getTag())||"课程详情".equals(previous.getTag())?300:260);
   animator=ValueAnimator.ofFloat(0f,1f);animator.setDuration(duration);animator.setInterpolator(EASING);
   previous.setLayerType(View.LAYER_TYPE_HARDWARE,null);next.setLayerType(View.LAYER_TYPE_HARDWARE,null);
   animator.addUpdateListener(value->{
    float progress=(float)value.getAnimatedValue();
    // Material FadeThroughProvider completes exit at 35%, then reveals the new page.
    previous.setAlpha(Math.max(0f,1f-progress/.35f));next.setAlpha(Math.max(0f,(progress-.35f)/.65f));
    if(fadeThrough){float scale=.98f+.02f*progress;next.setScaleX(scale);next.setScaleY(scale);}
    else{previous.setTranslationX(-direction*distance*progress);next.setTranslationX(direction*distance*(1f-progress));}
   });
   animator.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator animation){finish();}});
   animator.start();return true;
  };
  next.getViewTreeObserver().addOnPreDrawListener(pending);
 }
 void cancel(){
  if(pending!=null&&incoming!=null){if(incoming.getViewTreeObserver().isAlive())incoming.getViewTreeObserver().removeOnPreDrawListener(pending);pending=null;}
  if(animator!=null)animator.cancel();
  finish();
 }
 private void finish(){
  if(incoming==null)return;
  for(View view:new View[]{outgoing,incoming}){view.setAlpha(1f);view.setTranslationX(0f);view.setScaleX(1f);view.setScaleY(1f);view.setLayerType(View.LAYER_TYPE_NONE,null);}
  stage.removeView(outgoing);stage.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
  outgoing=null;incoming=null;animator=null;
 }
}
