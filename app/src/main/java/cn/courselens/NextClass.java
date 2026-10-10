package cn.courselens;

import java.time.*;
import java.util.*;

public final class NextClass {
 public static final String[] START_TIMES={"08:00","08:50","09:50","10:40","11:30","14:00","14:50","15:50","16:40","18:40","19:30","20:20"};
 public final LocalDateTime start;
 public final List<Course> courses;
 private NextClass(LocalDateTime start,List<Course> courses){this.start=start;this.courses=Collections.unmodifiableList(courses);}
 public static NextClass find(List<Course> courses,LocalDate term,LocalDateTime now){
  return find(courses,term,now,Collections.emptySet());
 }
 public static NextClass find(List<Course> courses,LocalDate term,LocalDateTime now,Set<String> choices){
  LocalDateTime nearest=null;List<Course> matches=new ArrayList<>();
  for(int week=1;week<=30;week++)for(CourseDisplay.Group group:CourseDisplay.groups(courses,week,choices)){
   Course course=group.selected;
   LocalDateTime start=term.plusDays((week-1)*7L+course.day-1).atTime(LocalTime.parse(START_TIMES[course.start-1]));
   if(start.isBefore(now)||(nearest!=null&&start.isAfter(nearest)))continue;
   if(!start.equals(nearest)){nearest=start;matches.clear();}
   boolean duplicate=false;for(Course match:matches)if(match.day==course.day&&match.start==course.start&&match.end==course.end&&match.name.equals(course.name)&&match.room.equals(course.room)&&match.teacher.equals(course.teacher)){duplicate=true;break;}
   if(!duplicate)matches.add(course);
  }
  return nearest==null?null:new NextClass(nearest,matches);
 }
 public long minutesUntil(LocalDateTime now){return Math.max(0,(Duration.between(now,start).toMillis()+59999)/60000);}
}
