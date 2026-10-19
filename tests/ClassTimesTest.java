import cn.courselens.*;
import java.time.*;
import java.util.*;

public class ClassTimesTest {
 public static void main(String[] args){
  assert ClassTimes.DEFAULT.starts.length==12;
  ClassTimes times=ClassTimes.parse(ClassTimes.XZIT.encode());assert times.starts.length==14&&times.starts[11].equals("18:45")&&times.ends[13].equals("21:20");
  String late="1|12|13|合成晚课|测试楼|1\n1|14|14|合成末节|测试楼|1";List<Course> courses=Course.parse(late);times.validate(courses);
  LocalDate first=LocalDate.of(2026,8,31);NextClass next=NextClass.find(courses,first,first.atTime(18,44),Set.of(),times);assert next.start.toLocalTime().equals(LocalTime.of(18,45));
  WidgetSchedule widget=WidgetSchedule.from(late,first.toString(),"",Set.of(),first.atTime(20,24),times);assert widget.remainingCount==2;
  widget=WidgetSchedule.from(late,first.toString(),"",Set.of(),first.atTime(20,25),times);assert widget.remainingCount==1&&widget.next.start==14;
  widget=WidgetSchedule.from(late,first.toString(),"",Set.of(),first.atTime(21,20),times);assert widget.remainingCount==0;
  for(String bad:new String[]{"08:00-08:00","09:00-08:00","08:00-09:00\n08:55-09:40","8:00-08:45","08:00","24:00-24:30"}){
   boolean rejected=false;try{ClassTimes.parse(bad);}catch(RuntimeException e){rejected=true;}assert rejected:bad;
  }
  boolean rejected=false;try{ClassTimes.DEFAULT.validate(courses);}catch(IllegalArgumentException e){rejected=true;}assert rejected;
  rejected=false;try{Course.parse("1|30|31|合成课程||1");}catch(IllegalArgumentException e){rejected=true;}assert rejected;
  assert Course.parse("1|30|30|合成课程||1").size()==1;
  System.out.println("PASS: dynamic periods, 14-period evening classes, widget end boundaries and invalid times");
 }
}
