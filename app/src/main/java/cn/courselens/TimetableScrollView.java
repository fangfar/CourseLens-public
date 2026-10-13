package cn.courselens;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ScrollView;
import androidx.viewpager.widget.ViewPager;

/** One direction decision, made before either native scroller can intercept. */
final class TimetableScrollView extends ScrollView {
 private static final int UNDECIDED=0,HORIZONTAL=1,VERTICAL=2;
 private final int touchSlop;
 private int direction=UNDECIDED,pointerId=MotionEvent.INVALID_POINTER_ID;
 private float downX,downY;

 TimetableScrollView(Context context){super(context);touchSlop=ViewConfiguration.get(context).getScaledTouchSlop();}

 @Override public boolean dispatchTouchEvent(MotionEvent event){
  int action=event.getActionMasked();
  if(action==MotionEvent.ACTION_DOWN){
   direction=UNDECIDED;pointerId=event.getPointerId(0);downX=event.getX();downY=event.getY();
   super.requestDisallowInterceptTouchEvent(false);
  }else if(action==MotionEvent.ACTION_MOVE&&direction==UNDECIDED){
   int index=event.findPointerIndex(pointerId);
   if(index>=0){
    float dx=Math.abs(event.getX(index)-downX),dy=Math.abs(event.getY(index)-downY);
    if(Math.max(dx,dy)>touchSlop){direction=dx>dy?HORIZONTAL:VERTICAL;requestDisallowInterceptTouchEvent(direction==HORIZONTAL);}
   }
  }else if(action==MotionEvent.ACTION_POINTER_UP&&event.getPointerId(event.getActionIndex())==pointerId){
   int index=event.getActionIndex()==0?1:0;pointerId=event.getPointerId(index);downX=event.getX(index);downY=event.getY(index);
  }
  try{return super.dispatchTouchEvent(event);}
  finally{if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){direction=UNDECIDED;pointerId=MotionEvent.INVALID_POINTER_ID;super.requestDisallowInterceptTouchEvent(false);}}
 }

 @Override public boolean onInterceptTouchEvent(MotionEvent event){
  if(event.getActionMasked()==MotionEvent.ACTION_DOWN){
   // Stop an existing vertical fling, but let the new gesture choose its own axis.
   if(super.onInterceptTouchEvent(event))super.onTouchEvent(event);
   return false;
  }
  if(event.getActionMasked()==MotionEvent.ACTION_MOVE&&direction!=VERTICAL)return false;
  return super.onInterceptTouchEvent(event);
 }

 @Override public void requestDisallowInterceptTouchEvent(boolean disallow){
  // Native ViewPager may request this during settling, before a direction exists.
  super.requestDisallowInterceptTouchEvent(direction==HORIZONTAL);
 }

 final class WeekPager extends ViewPager {
  WeekPager(Context context){super(context);}

  @Override public boolean onInterceptTouchEvent(MotionEvent event){
   if(event.getActionMasked()==MotionEvent.ACTION_MOVE&&direction!=HORIZONTAL)return false;
   MotionEvent horizontal=MotionEvent.obtain(event);
   // Only native pager arbitration sees flattened Y; cards receive the original event.
   horizontal.offsetLocation(0,-horizontal.getY());
   try{return super.onInterceptTouchEvent(horizontal);}finally{horizontal.recycle();}
  }

  // ViewPager handles drags; the course-card children retain their own click events.
  @SuppressLint("ClickableViewAccessibility")
  @Override public boolean onTouchEvent(MotionEvent event){
   if(event.getActionMasked()==MotionEvent.ACTION_MOVE&&direction!=HORIZONTAL)return true;
   MotionEvent horizontal=MotionEvent.obtain(event);horizontal.offsetLocation(0,-horizontal.getY());
   if(event.getActionMasked()==MotionEvent.ACTION_UP&&direction==VERTICAL)horizontal.setAction(MotionEvent.ACTION_CANCEL);
   try{return super.onTouchEvent(horizontal);}finally{horizontal.recycle();}
  }
 }
}
