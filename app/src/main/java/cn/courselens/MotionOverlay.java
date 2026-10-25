package cn.courselens;

import android.content.Context;
import android.graphics.Rect;
import android.view.*;
import android.widget.*;
import androidx.core.view.*;

/** One progress drives a surface and its scrim, including direct manipulation and IME offsets. */
class MotionOverlay extends FrameLayout {
 private View panel,shade;private boolean menu,closing,dragging,bottomOrigin;private float downX,downY,downProgress,imeOffset,backStart=1;private int imeTop,panelPaddingBottom,safeTop;
 boolean imeAnimating;private VelocityTracker velocity;private ViewTreeObserver.OnPreDrawListener pending;
 private MotionOverlay parentSurface;
 private Runnable dismiss=()->{};final Motion.Progress motion=new Motion.Progress(0f,this::renderMotion);
 MotionOverlay(Context context){super(context);setClickable(true);}
 void bindMotion(View panel,View shade,boolean menu,Runnable dismiss){
  this.panel=panel;this.shade=shade;this.menu=menu;this.dismiss=dismiss;panelPaddingBottom=panel.getPaddingBottom();
  ViewCompat.setOnApplyWindowInsetsListener(this,(v,insets)->{androidx.core.graphics.Insets ime=insets.getInsets(WindowInsetsCompat.Type.ime()),bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());safeTop=bars.top;setPadding(bars.left,0,bars.right,ime.bottom);panel.setPadding(panel.getPaddingLeft(),panel.getPaddingTop(),panel.getPaddingRight(),panelPaddingBottom+(ime.bottom>0?0:bars.bottom));return insets;});
  ViewCompat.setWindowInsetsAnimationCallback(this,new WindowInsetsAnimationCompat.Callback(WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE){
   @Override public void onPrepare(WindowInsetsAnimationCompat a){if((a.getTypeMask()&WindowInsetsCompat.Type.ime())!=0)imeAnimating=true;int[] at=new int[2];panel.getLocationOnScreen(at);imeTop=at[1];}
   @Override public WindowInsetsAnimationCompat.BoundsCompat onStart(WindowInsetsAnimationCompat a,WindowInsetsAnimationCompat.BoundsCompat bounds){int[] at=new int[2];panel.getLocationOnScreen(at);imeOffset=imeTop-at[1];renderMotion(motion.value);return bounds;}
   @Override public WindowInsetsCompat onProgress(WindowInsetsCompat insets,java.util.List<WindowInsetsAnimationCompat> running){for(WindowInsetsAnimationCompat a:running)if((a.getTypeMask()&WindowInsetsCompat.Type.ime())!=0){float fraction=a.getInterpolatedFraction();panel.setTranslationY(baseTranslation()+imeOffset*(1-fraction));}return insets;}
   @Override public void onEnd(WindowInsetsAnimationCompat a){if((a.getTypeMask()&WindowInsetsCompat.Type.ime())!=0)imeAnimating=false;imeOffset=0;renderMotion(motion.value);}
  });renderMotion(0);
 }
 int safeTop(){return safeTop;}
 void placeMenu(int height,int preferredTop,int anchorHeight){
  if(panel==null)return;FrameLayout.LayoutParams size=(FrameLayout.LayoutParams)panel.getLayoutParams();int gap=Math.round(12*getResources().getDisplayMetrics().density),topLimit=safeTop+gap;
  panel.measure(MeasureSpec.makeMeasureSpec(size.width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));int natural=panel.getMeasuredHeight(),below=Math.max(0,height-preferredTop-gap),above=Math.max(0,preferredTop-anchorHeight-topLimit);
  bottomOrigin=natural>below&&above>below;size.topMargin=Math.min(Math.max(topLimit,bottomOrigin?preferredTop-anchorHeight-natural:preferredTop),Math.max(topLimit,height-gap*4));size.height=Math.min(natural,Math.max(1,height-size.topMargin-gap));panel.setPivotY(bottomOrigin?size.height:0);
 }
 void above(MotionOverlay parent){parentSurface=parent;renderMotion(motion.value);}
 private float distance(){return menu?getResources().getDisplayMetrics().density*6:Math.max(1,panel.getHeight());}
 private float baseTranslation(){return (menu&&!bottomOrigin?-1:1)*distance()*(1-motion.value);}
 private void renderMotion(float progress){if(panel==null)return;panel.setTranslationY(baseTranslation()+imeOffset);panel.setAlpha(menu?progress:1f);panel.setScaleX(menu?.96f+.04f*progress:1f);panel.setScaleY(menu?.96f+.04f*progress:1f);shade.setAlpha(parentSurface!=null&&!parentSurface.menu?0:progress);}
 void showMotion(){
  removePending();closing=false;
  pending=()->{removePending();if(!closing&&isAttachedToWindow()){panel.setPivotX(panel.getWidth());panel.setPivotY(bottomOrigin?panel.getHeight():0);motion.to(1,menu?Motion.MENU_IN:Motion.SHEET_IN,false,()->{});}return true;};getViewTreeObserver().addOnPreDrawListener(pending);requestApplyInsets();
 }
 void hideMotion(boolean animate,Runnable end){closing=true;removePending();if(animate&&isAttachedToWindow())motion.to(0,menu?Motion.MENU_OUT:Motion.SHEET_OUT,false,end);else{motion.set(0);end.run();}}
 void cancelMotion(){removePending();motion.stop();recycleVelocity();parentSurface=null;}
 void beginBack(){removePending();backStart=motion.value;motion.stop();}
 void backProgress(float progress){removePending();motion.set(backStart*(1-progress));}
 void cancelBack(){closing=false;motion.to(1,Motion.SHEET_IN,true,()->{});}
 private void removePending(){if(pending!=null){if(getViewTreeObserver().isAlive())getViewTreeObserver().removeOnPreDrawListener(pending);pending=null;}}
 @Override protected void onLayout(boolean changed,int l,int t,int r,int b){super.onLayout(changed,l,t,r,b);renderMotion(motion.value);}
 private boolean scrollingAt(View view,float x,float y){
  if(view instanceof EditText)return true;
  if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=group.getChildCount()-1;i>=0;i--){View child=group.getChildAt(i);if(child.getVisibility()!=VISIBLE)continue;float cx=x+view.getScrollX()-child.getX(),cy=y+view.getScrollY()-child.getY();if(cx>=0&&cy>=0&&cx<child.getWidth()&&cy<child.getHeight()&&scrollingAt(child,cx,cy))return true;}}
  return view.canScrollVertically(-1);
 }
 private boolean canDrag;
 @Override public boolean onInterceptTouchEvent(MotionEvent event){
  if(menu||closing)return super.onInterceptTouchEvent(event);
  if(event.getActionMasked()==MotionEvent.ACTION_DOWN){recycleVelocity();velocity=VelocityTracker.obtain();velocity.addMovement(event);downX=event.getX();downY=event.getY();downProgress=motion.value;dragging=false;Rect rect=new Rect();panel.getHitRect(rect);canDrag=rect.contains((int)downX,(int)downY)&&!scrollingAt(panel,downX-panel.getX(),downY-panel.getY());}
  else if(event.getActionMasked()==MotionEvent.ACTION_MOVE&&canDrag){if(velocity!=null)velocity.addMovement(event);float dy=event.getY()-downY;if(Math.abs(dy)>ViewConfiguration.get(getContext()).getScaledTouchSlop()&&Math.abs(dy)>Math.abs(event.getX()-downX)&&(dy>0||downProgress<1)){motion.stop();dragging=true;getParent().requestDisallowInterceptTouchEvent(true);return true;}}
  else if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL)recycleVelocity();
  return super.onInterceptTouchEvent(event);
 }
 @Override public boolean onTouchEvent(MotionEvent event){
  if(!dragging)return super.onTouchEvent(event);if(velocity!=null)velocity.addMovement(event);
  if(event.getActionMasked()==MotionEvent.ACTION_MOVE)motion.set(downProgress-(event.getY()-downY)/distance());
  else if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL){float speed=0;if(velocity!=null){velocity.computeCurrentVelocity(1000);speed=velocity.getYVelocity();}boolean close=event.getActionMasked()==MotionEvent.ACTION_UP&&(speed>ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity()*6||motion.value<.65f&&speed>=0);dragging=false;recycleVelocity();if(close)dismiss.run();if(!closing)cancelBack();}return true;
 }
 private void recycleVelocity(){if(velocity!=null){velocity.recycle();velocity=null;}}
 @Override protected void onDetachedFromWindow(){cancelMotion();super.onDetachedFromWindow();}
}
