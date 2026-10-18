package cn.courselens;

import android.app.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Shared secondary-page controls used by settings and platform connections. */
final class SecondaryUi {
 static final int INK=0xff172132,MUTED=0xff68788f,SURFACE=0xfff1f5f6,AQUA=0xff9aebf2,SOFT=0xffdff8fa,NAVY=0xff082630;
 private final Activity activity;
 SecondaryUi(Activity activity){this.activity=activity;}
 int dp(int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
 TextView text(String value,int size,int color){TextView view=new TextView(activity);view.setText(value);view.setTextSize(size);view.setTextColor(color);view.setIncludeFontPadding(false);return view;}
 GradientDrawable bg(int color,int radius){GradientDrawable shape=new GradientDrawable();shape.setColor(color);shape.setCornerRadius(dp(radius));return shape;}
 void ripple(View view){view.setForeground(new RippleDrawable(android.content.res.ColorStateList.valueOf(0x225c78c6),null,bg(Color.WHITE,8)));}
 Button action(LinearLayout body,String label,Runnable run){Button button=new Button(activity);button.setText(label);button.setTextSize(15);button.setAllCaps(false);button.setTextColor(NAVY);button.setTypeface(null,Typeface.BOLD);button.setBackground(bg(AQUA,26));button.setElevation(0);button.setBackgroundTintList(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{0xffdfe7e9,AQUA}));button.setMinHeight(dp(52));button.setOnClickListener(v->run.run());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(12);lp.bottomMargin=dp(20);body.addView(button,lp);ripple(button);return button;}
 void styleDialog(AlertDialog dialog,boolean destructive){
  Window window=dialog.getWindow();window.setLayout(Math.min(activity.getResources().getDisplayMetrics().widthPixels-dp(40),dp(440)),-2);window.setDimAmount(.32f);
  TextView message=dialog.findViewById(android.R.id.message);if(message==null||!message.isShown())message=window.getDecorView().findViewWithTag("remote-message");
  if(message!=null){message.setTextSize(15);message.setTextColor(MUTED);message.setLineSpacing(dp(4),1);message.setPadding(message.getPaddingLeft(),message.getPaddingTop(),message.getPaddingRight(),dp(16));}
  List<Button> buttons=new ArrayList<>();for(int which:new int[]{AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL,AlertDialog.BUTTON_POSITIVE}){Button button=dialog.getButton(which);if(button==null||button.getVisibility()!=View.VISIBLE)continue;boolean primary=which==AlertDialog.BUTTON_POSITIVE;button.setAllCaps(false);button.setTextSize(14);button.setTypeface(null,Typeface.BOLD);button.setMinHeight(dp(48));button.setMinimumHeight(Math.max(dp(48),(int)Math.ceil(button.getTextSize()*2.4f)+dp(20)));button.setPadding(dp(16),dp(10),dp(16),dp(10));button.setSingleLine(false);button.setMaxLines(2);button.setTextColor(primary&&destructive?0xff943d55:NAVY);button.setBackgroundTintList(null);button.setBackground(bg(primary?(destructive?0xfffbdde6:AQUA):0xffeaf0f2,24));button.setStateListAnimator(null);ripple(button);buttons.add(button);}
  if(!buttons.isEmpty()){
   ViewGroup host=(ViewGroup)buttons.get(0).getParent();boolean common=false;
   while(!common){common=true;for(Button button:buttons){ViewParent at=button.getParent();while(at!=null&&at!=host)at=at.getParent();if(at==null){common=false;break;}}if(!common)host=(ViewGroup)host.getParent();}
   // The platform button bar may override its orientation; keep our actions in an independent child.
   LinearLayout actions=new LinearLayout(activity);boolean stacked=buttons.size()>2||activity.getResources().getConfiguration().fontScale>=1.3f;actions.setOrientation(stacked?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);actions.setGravity(Gravity.END);actions.setTag("dialog-actions");actions.setPadding(dp(20),dp(16),dp(20),dp(16));int reserved=0;
   for(Button button:buttons){reserved=stacked?reserved+button.getMinimumHeight()+dp(8):Math.max(reserved,button.getMinimumHeight());((ViewGroup)button.getParent()).removeView(button);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(stacked?-1:0,-2,stacked?0:1);lp.setMargins(stacked?0:dp(4),stacked?dp(8):0,stacked?0:dp(4),0);actions.addView(button,lp);}
   actions.setMinimumHeight(reserved+dp(32));host.removeAllViews();host.setPadding(0,0,0,0);host.setMinimumHeight(reserved+dp(32));host.addView(actions,new ViewGroup.LayoutParams(-1,-2));
  }
 }

}
