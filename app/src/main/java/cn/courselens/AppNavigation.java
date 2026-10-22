package cn.courselens;

import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.*;
import java.util.function.IntConsumer;

@android.annotation.SuppressLint("ViewConstructor")
final class AppNavigation extends LinearLayout {
 private final ImageButton[] tabs=new ImageButton[3];
 private final int ink;
 AppNavigation(Context context,int surface,int ink,int muted,int selected,IntConsumer onSelected){
  super(context);this.ink=ink;setTag("bottom-navigation");setOrientation(VERTICAL);setBackgroundColor(surface);
  LinearLayout items=new LinearLayout(context);items.setPadding(dp(36),dp(4),dp(36),dp(4));addView(items,new LayoutParams(-1,-2));
  String[] labels={"课表","任务","快递"},tags={"nav-timetable","nav-tasks","nav-parcels"};
  for(int i=0;i<tabs.length;i++){int index=i;ImageButton tab=new ImageButton(context);tab.setTag(tags[i]);tab.setContentDescription(labels[i]);tab.setTooltipText(labels[i]);tab.setMinimumHeight(dp(48));tab.setPadding(dp(8),dp(10),dp(8),dp(10));tab.setScaleType(ImageView.ScaleType.CENTER);tab.setBackground(null);tab.setOnClickListener(v->onSelected.accept(index));LayoutParams size=new LayoutParams(0,-2,1);size.setMargins(dp(8),0,dp(8),0);items.addView(tab,size);tabs[i]=tab;}
  select(selected,muted);
 }
 private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
 private int muted=0xff999999;
 void select(int selected){select(selected,muted);}
 void select(int selected,int muted){
  this.muted=muted;int[] outlined={R.drawable.ic_calendar,R.drawable.ic_tasks,R.drawable.ic_parcel},filled={R.drawable.ic_calendar_filled,R.drawable.ic_tasks_filled,R.drawable.ic_parcel_filled};
  for(int i=0;i<tabs.length;i++){boolean active=i==selected;ImageButton tab=tabs[i];tab.setSelected(active);tab.setImageResource(active?filled[i]:outlined[i]);tab.setImageTintList(ColorStateList.valueOf(active?ink:muted));}
 }
}
