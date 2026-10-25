package cn.courselens;

import android.animation.*;
import android.view.View;
import android.view.animation.PathInterpolator;
import java.util.function.Consumer;

/** Shared timings and a cancellable scalar: cancelling never commits an obsolete destination. */
final class Motion {
 static final int PRESS=120,MENU_IN=220,MENU_OUT=180,DIALOG_IN=260,DIALOG_OUT=200,SHEET_IN=340,SHEET_OUT=280,PAGE_IN=320,PAGE_OUT=280,EXPAND=260,COLLAPSE=220;
 static final PathInterpolator ENTER=new PathInterpolator(.2f,0f,0f,1f),EXIT=new PathInterpolator(.4f,0f,1f,1f);
 private Motion(){}
 static void press(View view){
  StateListAnimator states=new StateListAnimator();ObjectAnimator down=ObjectAnimator.ofFloat(view,View.ALPHA,.72f),up=ObjectAnimator.ofFloat(view,View.ALPHA,1f);down.setDuration(PRESS);up.setDuration(PRESS);down.setInterpolator(ENTER);up.setInterpolator(ENTER);
  states.addState(new int[]{android.R.attr.state_pressed,android.R.attr.state_enabled},down);states.addState(new int[]{},up);view.setStateListAnimator(states);
 }
 static final class Progress {
  float value;private ValueAnimator animator;private final Consumer<Float> render;
  Progress(float initial,Consumer<Float> render){value=initial;this.render=render;}
  boolean running(){return animator!=null;}
  void stop(){if(animator!=null){ValueAnimator old=animator;animator=null;old.removeAllListeners();old.removeAllUpdateListeners();old.cancel();}}
  void set(float next){stop();draw(next);}
  private void draw(float next){value=Math.max(0f,Math.min(1f,next));render.accept(value);}
  void to(float target,int duration,boolean spring,Runnable end){
   stop();if(!ValueAnimator.areAnimatorsEnabled()||Math.abs(target-value)<.0001f){draw(target);end.run();return;}
   animator=ValueAnimator.ofFloat(value,target);ValueAnimator current=animator;
   current.setDuration(Math.max(1,Math.round(duration*Math.max(.18f,Math.abs(target-value)))));
   // Critically damped spring response; normalized so it lands exactly, without overshoot.
   current.setInterpolator(spring?t->{double x=8*t;return (float)((1-(1+x)*Math.exp(-x))/(1-9*Math.exp(-8)));}:target>value?ENTER:EXIT);
   current.addUpdateListener(a->draw((float)a.getAnimatedValue()));
   current.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){if(animator!=current)return;animator=null;draw(target);end.run();}});current.start();
  }
 }
}
