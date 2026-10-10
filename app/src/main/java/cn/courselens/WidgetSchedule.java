package cn.courselens;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class WidgetSchedule {
 public final NextClass next;
 public final int todayCount;
 public final boolean empty;
 private WidgetSchedule(NextClass next,int todayCount,boolean empty){this.next=next;this.todayCount=todayCount;this.empty=empty;}
 public static WidgetSchedule from(String data,String term,String adjustments,Set<String> choices,LocalDateTime now){
  List<Course> original=Course.parse(data);LocalDate first=LocalDate.parse(term);
  List<Course> courses=CourseAdjustment.apply(original,term,CourseAdjustment.parse(adjustments));
  long days=ChronoUnit.DAYS.between(first,now.toLocalDate());int todayCount=0;
  if(days>=0&&days<210){int week=(int)(days/7)+1,day=(int)(days%7)+1;for(CourseDisplay.Group group:CourseDisplay.groups(courses,week,choices))if(group.selected.day==day)todayCount++;}
  return new WidgetSchedule(NextClass.find(courses,first,now,choices),todayCount,original.isEmpty());
 }
}
