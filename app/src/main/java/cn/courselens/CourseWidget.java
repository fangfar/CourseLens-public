package cn.courselens;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class CourseWidget extends AppWidgetProvider {
 private static final int[] CONTENT={R.id.widget_content,R.id.widget_content_second,R.id.widget_content_third,R.id.widget_content_fourth},TITLE={R.id.widget_title,R.id.widget_title_second,R.id.widget_title_third,R.id.widget_title_fourth},TIME={R.id.widget_time,R.id.widget_time_second,R.id.widget_time_third,R.id.widget_time_fourth},ROOM={R.id.widget_room,R.id.widget_room_second,R.id.widget_room_third,R.id.widget_room_fourth},STATUS={R.id.widget_status,R.id.widget_status_second,R.id.widget_status_third,R.id.widget_status_fourth};
 public static final String OPEN="cn.courselens.OPEN_WIDGET",REFRESH="cn.courselens.REFRESH_WIDGET";
 @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){for(int id:ids)manager.updateAppWidget(id,views(context,id,LocalDateTime.now(ZoneId.of("Asia/Shanghai"))));}
 @Override public void onAppWidgetOptionsChanged(Context context,AppWidgetManager manager,int id,Bundle options){onUpdate(context,manager,new int[]{id});}
 @Override public void onReceive(Context context,Intent intent){super.onReceive(context,intent);String action=intent.getAction();if(REFRESH.equals(action)||Intent.ACTION_DATE_CHANGED.equals(action)||Intent.ACTION_TIME_CHANGED.equals(action)||Intent.ACTION_TIMEZONE_CHANGED.equals(action))updateAll(context);}
 public static void updateAll(Context context){AppWidgetManager manager=AppWidgetManager.getInstance(context);for(Class<?> provider:new Class<?>[]{CourseWidget.class,CourseWidgetWide.class,CourseWidgetLarge.class}){int[] ids=manager.getAppWidgetIds(new ComponentName(context,provider));if(ids.length>0)new CourseWidget().onUpdate(context,manager,ids);}}
 static int count(Context context){AppWidgetManager manager=AppWidgetManager.getInstance(context);int count=0;for(Class<?> provider:new Class<?>[]{CourseWidget.class,CourseWidgetWide.class,CourseWidgetLarge.class})count+=manager.getAppWidgetIds(new ComponentName(context,provider)).length;return count;}
 static RemoteViews views(Context context,int id,LocalDateTime now){
  String activity=MainActivity.class.getName(),prefix=context.getPackageName()+".";
  SharedPreferences prefs=Timetables.preferences(context);
  AppWidgetManager manager=AppWidgetManager.getInstance(context);AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);Bundle options=manager.getAppWidgetOptions(id);android.graphics.Rect padding=AppWidgetHostView.getDefaultPaddingForWidget(context,new ComponentName(context,CourseWidget.class),null);float density=context.getResources().getDisplayMetrics().density;
  int width=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,info!=null&&!info.provider.getClassName().equals(CourseWidget.class.getName())?340:164)-Math.round((padding.left+padding.right)/density),height=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,info!=null&&info.provider.getClassName().equals(CourseWidgetLarge.class.getName())?340:190)-Math.round((padding.top+padding.bottom)/density);if(width<=0)width=148;if(height<=0)height=148;
  boolean compact=width<240&&height<260,large=height>=260;int limit=large?4:2;
  RemoteViews views=new RemoteViews(context.getPackageName(),compact&&android.os.Build.VERSION.SDK_INT<31?R.layout.course_widget_square:R.layout.course_widget);
  Palette palette=new Palette(context);
  views.setInt(R.id.widget_surface,"setBackgroundResource",palette.surfaceBackground);
  views.setTextColor(R.id.widget_date,palette.ink);views.setTextColor(R.id.widget_header,palette.header);views.setInt(R.id.widget_sync_icon,"setColorFilter",palette.header);
  views.setTextColor(R.id.widget_empty_title,palette.ink);views.setTextColor(R.id.widget_tomorrow,palette.tomorrow);

  views.setViewVisibility(R.id.widget_empty_panel,android.view.View.GONE);views.setViewVisibility(R.id.widget_tomorrow,android.view.View.GONE);views.setViewVisibility(R.id.widget_space_one,android.view.View.GONE);views.setViewVisibility(R.id.widget_space_two,android.view.View.GONE);
  if(android.os.Build.VERSION.SDK_INT>=31)views.setInt(R.id.widget_date,"setGravity",android.view.Gravity.START);
  if(android.os.Build.VERSION.SDK_INT>=31){views.setViewLayoutWidth(R.id.widget_surface,compact?Math.min(width,height):width,android.util.TypedValue.COMPLEX_UNIT_DIP);views.setViewLayoutHeight(R.id.widget_surface,compact?Math.min(width,height):height,android.util.TypedValue.COMPLEX_UNIT_DIP);views.setViewLayoutHeight(R.id.widget_heading,compact?32:44,android.util.TypedValue.COMPLEX_UNIT_DIP);}
  views.setViewVisibility(R.id.widget_date,android.view.View.VISIBLE);views.setTextViewTextSize(R.id.widget_date,android.util.TypedValue.COMPLEX_UNIT_SP,compact?14:18);views.setTextViewText(R.id.widget_date,now.format(DateTimeFormatter.ofPattern(compact?"E · MM.dd":"M月d日 · EEEE",Locale.CHINA)));

  String term=prefs.getString("term","2026-08-31");
  try{
   ClassTimes times=ClassTimes.parse(prefs.getString("classTimes",""));
   WidgetSchedule schedule=WidgetSchedule.from(prefs.getString("courses",""),term,prefs.getString("courseAdjustments",""),prefs.getStringSet("displayChoices",Collections.emptySet()),now,times);
   views.setTextViewText(R.id.widget_header,"还剩 "+schedule.remainingCount+" 门");
   int upcoming=0;while(upcoming<schedule.remaining.size()&&!now.toLocalTime().isBefore(LocalTime.parse(times.starts[schedule.remaining.get(upcoming).start-1])))upcoming++;
   for(int slot=0;slot<4;slot++){
    Course c=slot<limit&&slot<schedule.remaining.size()?schedule.remaining.get(slot):null;
    views.setViewVisibility(CONTENT[slot],c!=null?android.view.View.VISIBLE:android.view.View.GONE);
    course(context,views,id,slot,c,term,now,"暂无课程",compact,slot==upcoming,times,palette);
   }
   int missing=schedule.remainingCount==0?0:limit-Math.min(limit,schedule.remainingCount);views.setViewVisibility(R.id.widget_space_one,(missing&1)!=0?android.view.View.VISIBLE:android.view.View.GONE);views.setViewVisibility(R.id.widget_space_two,(missing&2)!=0?android.view.View.VISIBLE:android.view.View.GONE);
   if(schedule.remainingCount==0){
    boolean finished=schedule.todayCount>0;views.setViewVisibility(R.id.widget_empty_panel,android.view.View.VISIBLE);
    views.setInt(R.id.widget_empty_panel,"setBackgroundResource",compact?0:palette.freeBackground);
    views.setTextViewText(R.id.widget_header,"课表");views.setTextViewText(R.id.widget_empty_title,finished?"今日课程已结束":"今日无课");
    views.setTextViewTextSize(R.id.widget_empty_title,android.util.TypedValue.COMPLEX_UNIT_SP,compact?14:large?22:20);
    if(android.os.Build.VERSION.SDK_INT>=31)views.setInt(R.id.widget_date,"setGravity",compact?android.view.Gravity.CENTER:android.view.Gravity.START);
    int inset=Math.round((compact?2:6)*density);views.setViewPadding(R.id.widget_empty_panel,inset,inset,inset,inset);
    if(android.os.Build.VERSION.SDK_INT>=31){views.setInt(R.id.widget_empty_title,"setGravity",compact||large?android.view.Gravity.CENTER:android.view.Gravity.START);views.setInt(R.id.widget_empty_panel,"setGravity",(compact||large?android.view.Gravity.CENTER:android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL));}
    views.setOnClickPendingIntent(R.id.widget_empty_panel,open(context,id,4,null,term,now.toLocalDate()));
    if(schedule.tomorrow!=null){Course tomorrow=schedule.tomorrow;String hint="明天第一节 "+times.starts[tomorrow.start-1]+" · "+SchoolParser.clean(tomorrow.name)+(large&&!tomorrow.room.isEmpty()?" · "+SchoolParser.clean(tomorrow.room):"");views.setViewVisibility(R.id.widget_tomorrow,android.view.View.VISIBLE);views.setTextViewText(R.id.widget_tomorrow,hint);views.setTextViewTextSize(R.id.widget_tomorrow,android.util.TypedValue.COMPLEX_UNIT_SP,compact?9:12);views.setInt(R.id.widget_tomorrow,"setMaxLines",large?2:1);views.setOnClickPendingIntent(R.id.widget_tomorrow,open(context,id,5,tomorrow,term,now.toLocalDate().plusDays(1)));}
   }
  }catch(RuntimeException e){
   views.setTextViewText(R.id.widget_header,"Morrow");views.setViewVisibility(CONTENT[0],android.view.View.VISIBLE);for(int slot=1;slot<4;slot++)views.setViewVisibility(CONTENT[slot],android.view.View.GONE);
   course(context,views,id,0,null,term,now,"请检查课表设置",compact,false,ClassTimes.DEFAULT,palette);
  }

  views.setContentDescription(R.id.widget_refresh,"刷新课表，上次更新 "+now.format(DateTimeFormatter.ofPattern("HH:mm")));
  Intent refresh=new Intent(context,CourseWidget.class).setAction(REFRESH).setData(Uri.parse("courselens-widget://refresh/"+id));views.setOnClickPendingIntent(R.id.widget_refresh,PendingIntent.getBroadcast(context,id,refresh,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  return views;
 }
 private static void course(Context context,RemoteViews views,int id,int slot,Course next,String term,LocalDateTime now,String empty,boolean compact,boolean upcoming,ClassTimes times,Palette palette){
  int content=CONTENT[slot],title=TITLE[slot],time=TIME[slot],room=ROOM[slot];
  if(next==null){views.setTextViewText(title,empty);views.setTextViewText(time,"");views.setTextViewText(room,"");views.setContentDescription(content,empty+"，点击打开课表");}
  else{
   Course c=next;
   String when=times.starts[c.start-1]+(compact?"":"–"+times.ends[c.end-1]),where=c.room.isEmpty()?"地点未填写":SchoolParser.clean(c.room);
   String status=now.toLocalTime().isBefore(LocalTime.parse(times.starts[c.start-1]))?(upcoming?"下一节":"待上课"):"上课中";views.setTextViewText(STATUS[slot],status);
   boolean active=upcoming||status.equals("上课中");views.setInt(content,"setBackgroundResource",active?palette.nextBackground:palette.courseBackground);views.setInt(STATUS[slot],"setBackgroundResource",active?palette.statusBackground:palette.waitingBackground);views.setTextColor(STATUS[slot],active?palette.nextStatus:palette.waitStatus);
   views.setTextViewText(title,SchoolParser.clean(c.name));views.setTextViewText(time,when);views.setTextViewText(room,where);
   views.setContentDescription(content,status+"，"+SchoolParser.clean(c.name)+"，"+when+"，第"+c.start+"到"+c.end+"节，"+where);
  }
  views.setTextColor(title,palette.ink);views.setTextColor(time,palette.muted);views.setTextColor(room,palette.muted);
  views.setTextViewTextSize(title,android.util.TypedValue.COMPLEX_UNIT_SP,compact?13:18);views.setInt(title,"setMaxLines",compact?2:1);
  views.setViewVisibility(STATUS[slot],next!=null&&(!compact||upcoming)?android.view.View.VISIBLE:android.view.View.GONE);views.setTextViewTextSize(STATUS[slot],android.util.TypedValue.COMPLEX_UNIT_SP,compact?9:11);
  views.setViewVisibility(time,next==null?android.view.View.GONE:android.view.View.VISIBLE);
  views.setViewVisibility(room,next==null?android.view.View.GONE:android.view.View.VISIBLE);
  views.setOnClickPendingIntent(content,open(context,id,slot,next,term,now.toLocalDate()));
 }
 private static final class Palette {
  final int ink,header,muted,tomorrow,nextStatus,waitStatus,surfaceBackground,courseBackground,nextBackground,statusBackground,waitingBackground,freeBackground;
  Palette(Context context){
   boolean dark=Appearance.dark(context);Context themed=Appearance.wrap(context);
   ink=themed.getColor(R.color.widget_ink);header=themed.getColor(R.color.widget_header);muted=themed.getColor(R.color.widget_muted);tomorrow=themed.getColor(R.color.widget_tomorrow);
   nextStatus=themed.getColor(R.color.widget_status_next);waitStatus=themed.getColor(R.color.widget_status_waiting);
   surfaceBackground=dark?R.drawable.widget_background_dark:R.drawable.widget_background;courseBackground=dark?R.drawable.widget_course_background_dark:R.drawable.widget_course_background;
   nextBackground=dark?R.drawable.widget_next_background_dark:R.drawable.widget_next_background;statusBackground=dark?R.drawable.widget_status_background_dark:R.drawable.widget_status_background;
   waitingBackground=dark?R.drawable.widget_waiting_background_dark:R.drawable.widget_waiting_background;freeBackground=dark?R.drawable.widget_free_background_dark:R.drawable.widget_free_background;
  }
 }
 private static PendingIntent open(Context context,int id,int slot,Course course,String term,LocalDate date){
  Intent intent=new Intent(context,MainActivity.class).setAction(OPEN).setData(Uri.parse("courselens-widget://open/"+id+"/"+slot)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  intent.putExtra("widgetTimetable",Timetables.active(context));
  if(course!=null){int week=(int)(ChronoUnit.DAYS.between(LocalDate.parse(term),date)/7)+1;intent.putExtra("widgetWeek",week).putExtra("widgetCourse",CourseDisplay.key(week,course)).putExtra("widgetTerm",term);}
  return PendingIntent.getActivity(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }

}
