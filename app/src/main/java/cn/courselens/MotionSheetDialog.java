package cn.courselens;

import android.app.*;
import android.view.*;
import android.widget.*;

/** Parcel sheets use the same surface controller as the activity overlays. */
final class MotionSheetDialog extends AlertDialog {
 final MotionOverlay overlay;final View panel;private final boolean menu;private boolean closing,cancellable=true;private Object back;
 private int panelHeight=-2;Runnable cancelRequest;
 MotionSheetDialog(Activity activity,View panel,boolean menu,int top){
  super(activity,R.style.SecondaryDialog);this.panel=panel;this.menu=menu;
  overlay=new MotionOverlay(activity){@Override protected void onMeasure(int width,int height){FrameLayout.LayoutParams size=(FrameLayout.LayoutParams)panel.getLayoutParams();int available=MeasureSpec.getSize(height)-getPaddingBottom();if(!menu)size.height=panelHeight<0?-2:Math.min(panelHeight,Math.max(1,available-Math.max(dp(24),safeTop())));super.onMeasure(width,height);}};
  View shade=new View(activity);shade.setBackgroundColor(menu?android.graphics.Color.TRANSPARENT:0x26000000);shade.setOnClickListener(v->cancel());overlay.addView(shade,new FrameLayout.LayoutParams(-1,-1));
  FrameLayout.LayoutParams size=new FrameLayout.LayoutParams(menu?Math.min(dp(184),activity.getResources().getDisplayMetrics().widthPixels-dp(24)):-1,-2,menu?Gravity.TOP|Gravity.END:Gravity.BOTTOM);if(menu){size.rightMargin=dp(16);size.topMargin=Math.max(dp(12),Math.min(top,activity.getResources().getDisplayMetrics().heightPixels-dp(80)));}overlay.addView(panel,size);panel.setClickable(true);overlay.bindMotion(panel,shade,menu,this::cancel);setView(overlay,0,0,0,0);getWindow().setWindowAnimations(0);
 }
 private int dp(int value){return Math.round(value*getContext().getResources().getDisplayMetrics().density);}
 void panelHeight(int height){panelHeight=height;overlay.requestLayout();}
 @Override public void show(){
  super.show();if(overlay.getParent()!=null)((ViewGroup)overlay.getParent()).removeView(overlay);setContentView(overlay);Window window=getWindow();androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window,false);window.setBackgroundDrawableResource(android.R.color.transparent);window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.setDimAmount(0);window.setWindowAnimations(0);window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);window.setGravity(menu?Gravity.TOP|Gravity.END:Gravity.BOTTOM);window.setLayout(-1,-1);window.getDecorView().setPadding(0,0,0,0);setCanceledOnTouchOutside(false);
  overlay.showMotion();back=MotionBack.register(window,0,overlay::beginBack,p->{if(cancellable)overlay.backProgress(p);},overlay::cancelBack,this::cancel);
 }
 @Override public void setCancelable(boolean value){cancellable=value;super.setCancelable(value);}
 @Override public void cancel(){if(closing)return;if(!cancellable){overlay.cancelBack();return;}if(cancelRequest!=null){cancelRequest.run();if(!closing)overlay.cancelBack();}else super.cancel();}
 @Override public void dismiss(){if(closing||!isShowing())return;closing=true;overlay.hideMotion(true,this::dismissNow);}
 void dismissNow(){closing=true;overlay.cancelMotion();MotionBack.unregister(getWindow(),back);back=null;super.dismiss();}
}
