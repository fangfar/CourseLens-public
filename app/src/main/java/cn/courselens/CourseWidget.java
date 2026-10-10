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

public final class CourseWidget extends AppWidgetProvider {
 public static final String OPEN="cn.courselens.OPEN_WIDGET",REFRESH="cn.courselens.REFRESH_WIDGET";
 @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){for(int id:ids)manager.updateAppWidget(id,views(context,id,LocalDateTime.now(ZoneId.of("Asia/Shanghai"))));}
 @Override public void onAppWidgetOptionsChanged(Context context,AppWidgetManager manager,int id,Bundle options){onUpdate(context,manager,new int[]{id});}
 @Override public void onReceive(Context context,Intent intent){super.onReceive(context,intent);String action=intent.getAction();if(REFRESH.equals(action)||Intent.ACTION_DATE_CHANGED.equals(action)||Intent.ACTION_TIME_CHANGED.equals(action)||Intent.ACTION_TIMEZONE_CHANGED.equals(action))updateAll(context);}
 public static void updateAll(Context context){AppWidgetManager manager=AppWidgetManager.getInstance(context);int[] ids=manager.getAppWidgetIds(new ComponentName(context,CourseWidget.class));if(ids.length>0)new CourseWidget().onUpdate(context,manager,ids);}
 static RemoteViews views(Context context,int id,LocalDateTime now){
  String activity=MainActivity.class.getName(),prefix=context.getPackageName()+".";
  SharedPreferences prefs=context.getSharedPreferences(activity.startsWith(prefix)?activity.substring(prefix.length()):activity,Context.MODE_PRIVATE);
  RemoteViews views=new RemoteViews(context.getPackageName(),R.layout.course_widget);
  Intent open=new Intent(context,MainActivity.class).setAction(OPEN).setData(Uri.parse("courselens-widget://open/"+id)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  String date=now.format(DateTimeFormatter.ofPattern("M月d日",Locale.CHINA));
  try{
   String term=prefs.getString("term","2026-08-31");WidgetSchedule schedule=WidgetSchedule.from(prefs.getString("courses",""),term,prefs.getString("courseAdjustments",""),prefs.getStringSet("displayChoices",Collections.emptySet()),now);
   views.setTextViewText(R.id.widget_header,"今日 "+schedule.todayCount+" 门");
   if(schedule.next==null){views.setTextViewText(R.id.widget_title,schedule.empty?"还没有课表":"本学期暂无待上课程");views.setTextViewText(R.id.widget_time,schedule.empty?"打开 App 同步或添加课程":"今日安排已计入上方课程数量");views.setTextViewText(R.id.widget_room,"点击打开课表");}
   else{
    Course c=schedule.next.courses.get(0);int week=(int)(ChronoUnit.DAYS.between(LocalDate.parse(term),schedule.next.start.toLocalDate())/7)+1;
    views.setTextViewText(R.id.widget_title,SchoolParser.clean(c.name));views.setTextViewText(R.id.widget_time,schedule.next.start.format(DateTimeFormatter.ofPattern("M月d日 HH:mm",Locale.CHINA))+"\n第 "+c.start+"–"+c.end+" 节");views.setTextViewText(R.id.widget_room,c.room.isEmpty()?"地点未填写":SchoolParser.clean(c.room));
    open.putExtra("widgetWeek",week).putExtra("widgetCourse",CourseDisplay.key(week,c)).putExtra("widgetTerm",term);
   }
  }catch(RuntimeException e){views.setTextViewText(R.id.widget_header,date+" · 下一节课");views.setTextViewText(R.id.widget_title,"请检查课表设置");views.setTextViewText(R.id.widget_time,"课表、调整记录或开学日期异常");views.setTextViewText(R.id.widget_room,"点击打开 App");}
  views.setTextViewText(R.id.widget_updated,"更新 "+now.format(DateTimeFormatter.ofPattern("HH:mm")));
  views.setOnClickPendingIntent(R.id.widget_content,PendingIntent.getActivity(context,id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  Intent refresh=new Intent(context,CourseWidget.class).setAction(REFRESH).setData(Uri.parse("courselens-widget://refresh/"+id));views.setOnClickPendingIntent(R.id.widget_refresh,PendingIntent.getBroadcast(context,id,refresh,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  return views;
 }
}
