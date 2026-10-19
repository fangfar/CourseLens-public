package cn.courselens;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
public final class WidgetSchedule {
 public final Course next,following,tomorrow;
 public final int remainingCount,todayCount;
 public final List<Course> remaining;
 private WidgetSchedule(List<Course> remaining,int todayCount,Course tomorrow){this.remaining=Collections.unmodifiableList(new ArrayList<>(remaining));remainingCount=remaining.size();this.todayCount=todayCount;this.tomorrow=tomorrow;next=remaining.isEmpty()?null:remaining.get(0);following=remaining.size()<2?null:remaining.get(1);}
 public static WidgetSchedule from(String data,String term,String adjustments,Set<String> choices,LocalDateTime now){
  return from(data,term,adjustments,choices,now,ClassTimes.DEFAULT);
 }
 public static WidgetSchedule from(String data,String term,String adjustments,Set<String> choices,LocalDateTime now,ClassTimes times){
  LocalDate first=LocalDate.parse(term);List<Course> courses=CourseAdjustment.apply(Course.parse(data),term,CourseAdjustment.parse(adjustments));
  times.validate(courses);long days=ChronoUnit.DAYS.between(first,now.toLocalDate());List<Course> remaining=new ArrayList<>();int todayCount=0;Course tomorrow=null;
  if(days>=0&&days<210){int week=(int)(days/7)+1,day=(int)(days%7)+1;
   for(CourseDisplay.Group group:CourseDisplay.groups(courses,week,choices)){Course c=group.selected;if(c.day==day){todayCount++;if(now.toLocalTime().isBefore(LocalTime.parse(times.ends[c.end-1])))remaining.add(c);}}
   remaining.sort(Comparator.comparingInt(c->c.start));
  }
  long tomorrowDays=days+1;if(tomorrowDays>=0&&tomorrowDays<210){int week=(int)(tomorrowDays/7)+1,day=(int)(tomorrowDays%7)+1;for(CourseDisplay.Group group:CourseDisplay.groups(courses,week,choices)){Course c=group.selected;if(c.day==day&&(tomorrow==null||c.start<tomorrow.start))tomorrow=c;}}
  return new WidgetSchedule(remaining,todayCount,tomorrow);
 }
}
