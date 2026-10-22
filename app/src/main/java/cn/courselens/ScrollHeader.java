package cn.courselens;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

@android.annotation.SuppressLint("ViewConstructor")
final class ScrollHeader extends FrameLayout {
 private final View header,fade;
 private final ScrollView[] scrolls;
 private final int[] originalTop;
 private final int gap;
 ScrollHeader(Context context,View header,View content,int surface,ScrollView... scrolls){
  super(context);this.header=header;this.scrolls=scrolls;gap=Math.round(16*getResources().getDisplayMetrics().density);originalTop=new int[scrolls.length];
  setTag("scroll-header");addView(content,new LayoutParams(-1,-1));
  fade=new View(context);fade.setTag("header-fade");fade.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);fade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{surface,surface,surface,surface,surface&0x00ffffff}));addView(fade,new LayoutParams(-1,0));
  header.setClickable(true);header.setFocusable(false);header.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);addView(header,new LayoutParams(-1,-2,Gravity.TOP));
  for(int i=0;i<scrolls.length;i++){originalTop[i]=scrolls[i].getPaddingTop();scrolls[i].setClipToPadding(false);}
 }
 @Override protected void onMeasure(int width,int height){
  measureChildWithMargins(header,width,0,height,0);int inset=header.getMeasuredHeight()+gap;fade.getLayoutParams().height=inset;
  for(int i=0;i<scrolls.length;i++){ScrollView scroll=scrolls[i];if(scroll.getPaddingTop()!=originalTop[i]+inset)scroll.setPadding(scroll.getPaddingLeft(),originalTop[i]+inset,scroll.getPaddingRight(),scroll.getPaddingBottom());}
  super.onMeasure(width,height);
 }
 @Override public void requestChildFocus(View child,View focused){
  super.requestChildFocus(child,focused);
  // Native focus scrolling does not know that the header covers the top of the viewport.
  post(()->{if(!focused.hasFocus()||!focused.isShown())return;for(ScrollView scroll:scrolls){ViewParent parent=focused.getParent();while(parent instanceof View&&parent!=scroll)parent=parent.getParent();if(parent!=scroll)continue;int[] field=new int[2],bar=new int[2];focused.getLocationInWindow(field);header.getLocationInWindow(bar);int delta=field[1]-bar[1]-header.getHeight()-gap;if(delta<0)scroll.scrollBy(0,delta);}});
 }
}
