package cn.courselens;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

@android.annotation.SuppressLint("ViewConstructor") // Created with page state, never inflated from XML.
final class TaskPage extends LinearLayout {
 private static final int INK=SecondaryUi.INK,MUTED=SecondaryUi.MUTED,PRIMARY=SecondaryUi.NAVY;
 private final TextView[] filters=new TextView[3];
 private final TextView emptyTitle,emptyDescription;
 private final ScrollView scroll;
 private int filter;

 TaskPage(Context context,Bundle state){
  super(context);setTag("tasks-page");setOrientation(VERTICAL);
  LinearLayout header=new LinearLayout(context);header.setOrientation(VERTICAL);header.setPadding(dp(20),dp(20),dp(20),dp(20));
  TextView title=text("任务",28,INK);title.setTypeface(null,Typeface.BOLD);header.addView(title);
  TextView intro=text("课程作业与截止时间，集中查看",14,MUTED);intro.setPadding(0,dp(6),0,0);header.addView(intro);addView(header,new LayoutParams(-1,-2));

  scroll=new ScrollView(context);scroll.setFillViewport(true);scroll.setClipToPadding(false);addView(scroll,new LayoutParams(-1,0,1));
  LinearLayout body=new LinearLayout(context);body.setOrientation(VERTICAL);body.setPadding(dp(20),0,dp(20),dp(24));scroll.addView(body,new ScrollView.LayoutParams(-1,-1));

  LinearLayout platforms=new LinearLayout(context);platforms.setOrientation(VERTICAL);platforms.setPadding(dp(18),dp(18),dp(18),dp(18));platforms.setBackground(surface(SecondaryUi.SOFT,18));body.addView(platforms,new LayoutParams(-1,-2));
  LinearLayout status=new LinearLayout(context);status.setGravity(Gravity.CENTER_VERTICAL);platforms.addView(status,new LayoutParams(-1,-2));
  TextView platformTitle=text("学习平台",16,INK);platformTitle.setTypeface(null,Typeface.BOLD);status.addView(platformTitle,new LayoutParams(0,-2,1));
  TextView badge=text("暂未连接",12,MUTED);badge.setPadding(dp(10),dp(5),dp(10),dp(5));badge.setBackground(surface(0xffe5eef0,10));status.addView(badge);
  TextView names=text("学习通  ·  知到  ·  中国大学 MOOC",13,SecondaryUi.MUTED);names.setPadding(0,dp(12),0,dp(6));names.setLineSpacing(dp(3),1);platforms.addView(names);
  platforms.addView(text("登录学习通后自动获取课程作业；其他平台暂未开放",12,MUTED));
  new SecondaryUi((android.app.Activity)context).action(platforms,"连接学习通",()->context.startActivity(new android.content.Intent(context,ChaoxingActivity.class))).setTag("chaoxing-connect");

  LinearLayout choices=new LinearLayout(context);choices.setPadding(dp(4),dp(4),dp(4),dp(4));choices.setBackground(surface(0xffe5eef0,14));LayoutParams choiceLayout=new LayoutParams(-1,-2);choiceLayout.topMargin=dp(24);body.addView(choices,choiceLayout);
  String[] labels={"待处理","已完成","全部"};
  for(int i=0;i<labels.length;i++){final int index=i;TextView tab=text(labels[i],14,MUTED);tab.setTag("task-filter-"+i);tab.setGravity(Gravity.CENTER);tab.setMinHeight(dp(48));tab.setPadding(dp(4),dp(10),dp(4),dp(10));tab.setFocusable(true);tab.setContentDescription(labels[i]);tab.setOnClickListener(v->selectFilter(index));filters[i]=tab;choices.addView(tab,new LayoutParams(0,-2,1));}

  LinearLayout empty=new LinearLayout(context);empty.setOrientation(VERTICAL);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(16),dp(44),dp(16),dp(44));empty.setMinimumHeight(dp(280));body.addView(empty,new LayoutParams(-1,0,1));
  ImageView illustration=new ImageView(context);illustration.setImageResource(R.drawable.ic_tasks);illustration.setImageTintList(android.content.res.ColorStateList.valueOf(SecondaryUi.NAVY));illustration.setPadding(dp(20),dp(20),dp(20),dp(20));illustration.setBackground(surface(SecondaryUi.SOFT,28));illustration.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);empty.addView(illustration,new LayoutParams(dp(80),dp(80)));
  emptyTitle=text("",18,INK);emptyTitle.setTag("tasks-empty-title");emptyTitle.setTypeface(null,Typeface.BOLD);emptyTitle.setGravity(Gravity.CENTER);emptyTitle.setPadding(0,dp(22),0,dp(8));emptyTitle.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);empty.addView(emptyTitle,new LayoutParams(-1,-2));
  emptyDescription=text("",14,MUTED);emptyDescription.setGravity(Gravity.CENTER);emptyDescription.setLineSpacing(dp(5),1);empty.addView(emptyDescription,new LayoutParams(-1,-2));
  selectFilter(state==null?0:state.getInt("filter",0));
  int restoredY=state==null?0:state.getInt("scrollY",0);scroll.post(()->scroll.scrollTo(0,restoredY));
 }

 private void selectFilter(int value){
  filter=value>=0&&value<filters.length?value:0;
  for(int i=0;i<filters.length;i++){boolean selected=i==filter;filters[i].setSelected(selected);filters[i].setTextColor(selected?PRIMARY:MUTED);filters[i].setTypeface(null,selected?Typeface.BOLD:Typeface.NORMAL);filters[i].setBackground(selected?surface(0xffffffff,10):null);}
  String[] titles={"还没有待处理任务","还没有已完成任务","还没有任务"};emptyTitle.setText(titles[filter]);
  emptyDescription.setText("连接学习通后自动查看课程作业，\n读取结果在学习通页面中展示。");
 }
 void saveState(Bundle state){state.putInt("filter",filter);state.putInt("scrollY",scroll.getScrollY());}
 private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
 private TextView text(String value,int size,int color){TextView view=new TextView(getContext());view.setText(value);view.setTextSize(size);view.setTextColor(color);view.setIncludeFontPadding(false);return view;}
 private GradientDrawable surface(int color,int radius){GradientDrawable background=new GradientDrawable();background.setColor(color);background.setCornerRadius(dp(radius));return background;}
}
