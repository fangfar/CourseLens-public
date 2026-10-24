package cn.courselens;

import android.app.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Shared secondary-page controls used by settings and platform connections. */
final class SecondaryUi {
 final int INK,MUTED,SURFACE,CARD,PRIMARY,AQUA,SOFT,NAVY,BLUE,DIVIDER,DANGER,DISABLED,ON_PRIMARY,HANDLE,COURSE_BORDER,EMPTY_TITLE,EMPTY_HINT,NAV_MUTED,WARNING,CHIP;
 final boolean dark;
 private final Activity activity;
 SecondaryUi(Activity activity){
  this.activity=activity;dark=Appearance.dark(activity);
  INK=color(R.color.app_ink);MUTED=color(R.color.app_muted);SURFACE=color(R.color.app_surface);CARD=color(R.color.app_card);PRIMARY=color(R.color.app_primary);AQUA=color(R.color.app_selected);SOFT=color(R.color.app_soft);NAVY=INK;BLUE=color(R.color.app_blue);DIVIDER=color(R.color.app_divider);DANGER=color(R.color.app_danger);DISABLED=color(R.color.app_disabled);ON_PRIMARY=color(R.color.app_on_primary);HANDLE=color(R.color.app_handle);COURSE_BORDER=color(R.color.app_course_border);EMPTY_TITLE=color(R.color.app_empty_title);EMPTY_HINT=color(R.color.app_empty_hint);NAV_MUTED=color(R.color.app_nav_muted);WARNING=color(R.color.app_warning);CHIP=color(R.color.app_chip);
 }
 private int color(int resource){return activity.getColor(resource);}
 int dp(int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
 TextView text(String value,int size,int color){TextView view=new TextView(activity);view.setText(value);view.setTextSize(size);view.setTextColor(color);view.setIncludeFontPadding(false);return view;}
 GradientDrawable bg(int color,int radius){GradientDrawable shape=new GradientDrawable();shape.setColor(color);shape.setCornerRadius(dp(radius));return shape;}
 GradientDrawable popupSurface(){GradientDrawable shape=bg(CARD,24);shape.setStroke(dp(1),DIVIDER);return shape;}
 android.graphics.drawable.Drawable sheetSurface(){GradientDrawable shape=popupSurface();shape.setCornerRadii(new float[]{dp(24),dp(24),dp(24),dp(24),0,0,0,0});LayerDrawable edge=new LayerDrawable(new android.graphics.drawable.Drawable[]{shape});edge.setLayerInset(0,0,0,0,-dp(1));return edge;}
 TextView popupItem(LinearLayout body,String label,Runnable run){TextView item=text(label,16,INK);item.setGravity(Gravity.CENTER_VERTICAL);item.setPadding(dp(20),dp(12),dp(20),dp(12));item.setMinHeight(dp(52));item.setFocusable(true);item.setOnClickListener(v->run.run());popupChecked(item,false);focusOutline(item);body.addView(item,new LinearLayout.LayoutParams(-1,-2));return item;}
 void popupChecked(TextView item,boolean selected){android.graphics.drawable.Drawable mark=selected?activity.getDrawable(R.drawable.ic_check).mutate():new ColorDrawable(Color.TRANSPARENT);mark.setTint(INK);mark.setBounds(0,0,dp(22),dp(22));item.setCompoundDrawables(mark,null,null,null);item.setCompoundDrawablePadding(dp(12));item.setSelected(selected);}
 void popupDivider(LinearLayout body){View line=new View(activity);line.setBackgroundColor(DIVIDER);LinearLayout.LayoutParams bounds=new LinearLayout.LayoutParams(-1,dp(1));bounds.setMargins(dp(12),dp(8),dp(12),dp(8));body.addView(line,bounds);}
 void focusOutline(View view){StateListDrawable states=new StateListDrawable();GradientDrawable outline=bg(Color.TRANSPARENT,8);outline.setStroke(dp(2),PRIMARY);states.addState(new int[]{android.R.attr.state_focused},outline);states.addState(new int[]{},new ColorDrawable(Color.TRANSPARENT));view.setForeground(states);}

 Button action(LinearLayout body,String label,Runnable run){Button button=new Button(activity);button.setText(label);button.setTextSize(15);button.setAllCaps(false);button.setTextColor(ON_PRIMARY);button.setBackground(bg(PRIMARY,24));button.setStateListAnimator(null);button.setBackgroundTintList(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{DISABLED,PRIMARY}));button.setMinWidth(0);button.setMinimumWidth(0);button.setMinHeight(dp(48));button.setMinimumHeight(dp(48));button.setPadding(dp(20),dp(10),dp(20),dp(10));button.setOnClickListener(v->run.run());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.gravity=Gravity.CENTER_HORIZONTAL;lp.topMargin=dp(8);lp.bottomMargin=dp(20);body.addView(button,lp);focusOutline(button);return button;}
 void styleIcon(TextView view,int drawable){view.setTextColor(INK);view.setBackgroundTintList(null);GradientDrawable circle=bg(CARD,24);circle.setStroke(dp(1),SOFT);view.setBackground(circle);view.setClipToOutline(true);android.graphics.drawable.Drawable icon=activity.getDrawable(drawable).mutate();icon.setTintList(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{DISABLED,INK}));icon.setBounds(0,0,dp(22),dp(22));view.setCompoundDrawables(icon,null,null,null);view.setPadding(dp(13),0,dp(13),0);focusOutline(view);}
 Button iconAction(LinearLayout body,int drawable,String label,Runnable run){Button button=action(body,"",run);button.setContentDescription(label);button.setTooltipText(label);styleIcon(button,drawable);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(48),dp(48));lp.leftMargin=dp(8);body.updateViewLayout(button,lp);return button;}
 Button menuAction(LinearLayout body,Runnable run){Button button=iconAction(body,R.drawable.ic_menu,"打开导航菜单",run);button.setTag("side-menu-button");LinearLayout.LayoutParams size=(LinearLayout.LayoutParams)button.getLayoutParams();size.leftMargin=0;size.rightMargin=dp(8);return button;}
 void fitHeader(LinearLayout row,View title){
  ViewGroup parent=(ViewGroup)row.getParent();int index=parent.indexOfChild(row);ViewGroup.LayoutParams original=row.getLayoutParams();View spacer=row.getChildAt(1);parent.removeView(row);
  LinearLayout.LayoutParams inlineTitle=new LinearLayout.LayoutParams(0,-2,1),stackedTitle=new LinearLayout.LayoutParams(-1,-2),inlineSpacer=new LinearLayout.LayoutParams(dp(56),1),stackedSpacer=new LinearLayout.LayoutParams(0,1,1);stackedTitle.setMargins(dp(16),dp(8),dp(16),0);
  LinearLayout shell=new LinearLayout(activity){
   @Override protected void onMeasure(int width,int height){
    title.measure(View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));int needed=title.getMeasuredWidth()+dp(224)+row.getPaddingLeft()+row.getPaddingRight();boolean stacked=View.MeasureSpec.getSize(width)<needed;
    if(stacked&&title.getParent()==row){row.removeView(title);addView(title,0,stackedTitle);spacer.setLayoutParams(stackedSpacer);}
    else if(!stacked&&title.getParent()==this){removeView(title);row.addView(title,2,inlineTitle);spacer.setLayoutParams(inlineSpacer);}
    super.onMeasure(width,height);
   }
  };shell.setOrientation(LinearLayout.VERTICAL);shell.addView(row,new LinearLayout.LayoutParams(-1,-2));parent.addView(shell,index,original);
 }
 void styleDialog(AlertDialog dialog,boolean destructive){
  Window window=dialog.getWindow();window.setLayout(Math.min(activity.getResources().getDisplayMetrics().widthPixels-dp(40),dp(440)),-2);window.setDimAmount(0);window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.setBackgroundDrawable(popupSurface());
  TextView message=dialog.findViewById(android.R.id.message);if(message==null||!message.isShown())message=window.getDecorView().findViewWithTag("remote-message");
  if(message!=null){message.setTextSize(15);message.setTextColor(MUTED);message.setLineSpacing(dp(4),1);message.setPadding(message.getPaddingLeft(),message.getPaddingTop(),message.getPaddingRight(),dp(16));}
  int dangerButton=dark?0xffa83f56:DANGER;
  List<Button> buttons=new ArrayList<>();for(int which:new int[]{AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL,AlertDialog.BUTTON_POSITIVE}){Button button=dialog.getButton(which);if(button==null||button.getVisibility()!=View.VISIBLE)continue;boolean primary=which==AlertDialog.BUTTON_POSITIVE;button.setAllCaps(false);button.setTextSize(14);button.setTypeface(null,Typeface.NORMAL);button.setMinWidth(0);button.setMinimumWidth(0);button.setMinHeight(dp(48));button.setMinimumHeight(Math.max(dp(48),(int)Math.ceil(button.getTextSize()*1.4f)+dp(20)));button.setPadding(dp(16),dp(10),dp(16),dp(10));button.setSingleLine(false);button.setMaxLines(2);button.setTextColor(primary?ON_PRIMARY:INK);button.setBackgroundTintList(null);button.setBackground(bg(primary?(destructive?dangerButton:PRIMARY):SOFT,24));button.setStateListAnimator(null);focusOutline(button);buttons.add(button);}
  if(!buttons.isEmpty()){
   ViewGroup host=(ViewGroup)buttons.get(0).getParent();boolean common=false;
   while(!common){common=true;for(Button button:buttons){ViewParent at=button.getParent();while(at!=null&&at!=host)at=at.getParent();if(at==null){common=false;break;}}if(!common)host=(ViewGroup)host.getParent();}
   // The platform button bar may override its orientation; keep our actions in an independent child.
   LinearLayout actions=new LinearLayout(activity);boolean stacked=buttons.size()>2||activity.getResources().getConfiguration().fontScale>=1.3f;actions.setOrientation(stacked?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);actions.setGravity(Gravity.END);actions.setTag("dialog-actions");actions.setPadding(dp(20),dp(16),dp(20),dp(16));int reserved=0;
   for(Button button:buttons){reserved=stacked?reserved+button.getMinimumHeight()+dp(8):Math.max(reserved,button.getMinimumHeight());((ViewGroup)button.getParent()).removeView(button);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(stacked?-1:-2,-2);lp.setMargins(stacked?0:dp(4),stacked?dp(8):0,stacked?0:dp(4),0);actions.addView(button,lp);}
   actions.setMinimumHeight(reserved+dp(32));host.removeAllViews();host.setPadding(0,0,0,0);host.setMinimumHeight(reserved+dp(32));host.addView(actions,new ViewGroup.LayoutParams(-1,-2));
  }
 }

}
