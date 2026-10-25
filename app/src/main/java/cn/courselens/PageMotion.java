package cn.courselens;

import android.view.*;
import android.widget.FrameLayout;

/** Opaque pages slide over their parent without blending text; reversal uses the current frame. */
final class PageMotion {
 private FrameLayout stage;private View outgoing,incoming;private ViewTreeObserver.OnPreDrawListener pending;
 private int oldAccessibility,newAccessibility;private float direction,distance,oldX,backStart;private boolean reversing;
 private final Motion.Progress motion=new Motion.Progress(0,this::render);
 boolean isRunning(){return incoming!=null;}
 boolean isReturning(){return incoming!=null&&direction<0;}
 void start(FrameLayout container,View previous,View next,boolean backwards,float distance){
  cancel();stage=container;outgoing=previous;incoming=next;this.distance=distance;direction=backwards?-1:1;oldX=previous.getTranslationX();oldAccessibility=previous.getImportantForAccessibility();newAccessibility=next.getImportantForAccessibility();
  if(backwards)previous.bringToFront();
  stage.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);previous.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);motion.set(0);
  pending=()->{removePending();if(incoming==null)return true;motion.to(1,backwards?Motion.PAGE_OUT:Motion.PAGE_IN,false,()->finish(null));return true;};next.getViewTreeObserver().addOnPreDrawListener(pending);
 }
 private void render(float p){if(incoming==null)return;outgoing.setAlpha(1);incoming.setAlpha(1);outgoing.setTranslationX(direction>0?oldX-distance*.08f*p:oldX+(distance-oldX)*p);incoming.setTranslationX(direction>0?distance*(1-p):-distance*.08f*(1-p));}
 void beginBack(){removePending();backStart=motion.value;motion.stop();}
 void backProgress(float progress){motion.set(backStart*(1-progress));}
 void cancelBack(){motion.to(1,Motion.PAGE_IN,false,()->finish(null));}
 void reverse(Runnable end){if(incoming==null){end.run();return;}if(reversing)return;reversing=true;removePending();motion.to(0,Motion.PAGE_OUT,false,()->finish(end));}
 void cancel(){removePending();motion.stop();finish(null);}
 private void removePending(){if(pending!=null&&incoming!=null){if(incoming.getViewTreeObserver().isAlive())incoming.getViewTreeObserver().removeOnPreDrawListener(pending);pending=null;}}
 private void finish(Runnable end){
  if(incoming==null)return;View previous=outgoing,next=incoming;for(View view:new View[]{previous,next}){view.setAlpha(1);view.setTranslationX(0);view.setScaleX(1);view.setScaleY(1);}
  previous.setImportantForAccessibility(oldAccessibility);next.setImportantForAccessibility(newAccessibility);stage.removeView(previous);stage.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);outgoing=null;incoming=null;reversing=false;if(end!=null)end.run();
 }
}
