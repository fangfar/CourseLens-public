package cn.courselens;

import java.time.LocalTime;
import java.util.*;

public final class ClassTimes {
 public static final int MAX_PERIODS=30;
 public static final ClassTimes DEFAULT=new ClassTimes(NextClass.START_TIMES,NextClass.END_TIMES);
 public static final ClassTimes XZIT=parse("08:00-08:45\n08:55-09:40\n10:05-10:50\n11:00-11:45\n12:00-12:45\n12:55-13:40\n14:00-14:45\n14:55-15:40\n16:05-16:50\n17:00-17:45\n17:55-18:35\n18:45-19:30\n19:40-20:25\n20:35-21:20");
 public final String[] starts,ends;
 public ClassTimes(String[] starts,String[] ends){
  if(starts.length<1||starts.length>MAX_PERIODS||starts.length!=ends.length)throw new IllegalArgumentException("每天应有 1–30 节课。");
  this.starts=starts.clone();this.ends=ends.clone();LocalTime previous=null;
  for(int i=0;i<starts.length;i++){
   if(!starts[i].matches("\\d{2}:\\d{2}")||!ends[i].matches("\\d{2}:\\d{2}"))throw new IllegalArgumentException("时间请填写为 HH:mm。");
   LocalTime start=LocalTime.parse(starts[i]),end=LocalTime.parse(ends[i]);
   if(!end.isAfter(start)||(previous!=null&&start.isBefore(previous)))throw new IllegalArgumentException("节次时间应依次排列，结束时间应晚于开始时间。");previous=end;
  }
 }
 public static ClassTimes parse(String text){
  if(text==null||text.trim().isEmpty())return DEFAULT;
  String[] rows=text.trim().split("\\n");String[] starts=new String[rows.length],ends=new String[rows.length];
  for(int i=0;i<rows.length;i++){String[] pair=rows[i].trim().split("-",-1);if(pair.length!=2)throw new IllegalArgumentException("每行填写一节课，例如 08:00-08:45。");starts[i]=pair[0].trim();ends[i]=pair[1].trim();}
  return new ClassTimes(starts,ends);
 }
 public void validate(List<Course> courses){for(Course course:courses)if(course.start<1||course.end>starts.length)throw new IllegalArgumentException("课程节次超过当前作息，请先完善作息时间。");}
 public String encode(){List<String> rows=new ArrayList<>();for(int i=0;i<starts.length;i++)rows.add(starts[i]+"-"+ends[i]);return String.join("\n",rows);}
}
