import cn.courselens.*;
import java.time.*;
import java.util.*;

public class WidgetScheduleTest {
 public static void main(String[] args){
  String term="2026-08-31",data="1|1|2|上午课程|A|1-3\n1|6|7|下午课程|B|1-3\n2|3|4|明日课程|C|1-3";List<Course> courses=Course.parse(data);LocalDate first=LocalDate.parse(term);
  WidgetSchedule s=WidgetSchedule.from(data,term,"",Set.of(),first.atTime(7,59));
  assert s.todayCount==2&&s.next.courses.get(0).name.equals("上午课程")&&!s.empty;
  assert WidgetSchedule.from(data,term,"",Set.of(),first.atTime(8,0,1)).next.courses.get(0).name.equals("下午课程");
  assert WidgetSchedule.from(data,term,"",Set.of(),first.atTime(23,59)).next.courses.get(0).name.equals("明日课程");
  assert WidgetSchedule.from(data,term,"",Set.of(),first.plusDays(1).atStartOfDay()).todayCount==1;
  assert WidgetSchedule.from(data,term,"",Set.of(),first.minusDays(1).atStartOfDay()).todayCount==0;
  assert WidgetSchedule.from(data,term,"",Set.of(),first.plusDays(210).atStartOfDay()).next==null;
  assert WidgetSchedule.from("",term,"",Set.of(),first.atStartOfDay()).empty;
  CourseAdjustment cancel=new CourseAdjustment(term,1,courses.get(0),null);
  s=WidgetSchedule.from(data,term,cancel.encode(),Set.of(),first.atTime(7,59));assert s.todayCount==1&&s.next.courses.get(0).name.equals("下午课程");
  Course original=courses.get(0),moved=new Course(2,1,2,original.name,"新教室","2");CourseAdjustment change=new CourseAdjustment(term,1,original,moved);
  s=WidgetSchedule.from(data,term,change.encode(),Set.of(),first.plusDays(8).atTime(7,59));assert s.todayCount==2&&s.next.courses.get(0).room.equals("新教室");
  String conflicts=data+"\n1|1|2|冲突课程|D|1-3";Course chosen=Course.parse(conflicts).get(3);
  s=WidgetSchedule.from(conflicts,term,"",Set.of(CourseDisplay.key(1,chosen)),first.atTime(7,59));assert s.todayCount==2&&s.next.courses.get(0).name.equals("冲突课程");
  Set<String> priorities=new HashSet<>();CourseDisplay.prefer(chosen,priorities);
  s=WidgetSchedule.from(conflicts,term,"",priorities,first.plusWeeks(1).atTime(7,59));assert s.todayCount==2&&s.next.courses.get(0).name.equals("冲突课程");
  String only=original.encode();s=WidgetSchedule.from(only,term,cancel.encode()+"\n"+new CourseAdjustment(term,2,original,null).encode()+"\n"+new CourseAdjustment(term,3,original,null).encode(),Set.of(),first.atStartOfDay());assert s.next==null&&!s.empty&&s.todayCount==0;
  try{WidgetSchedule.from(data,"invalid","",Set.of(),first.atStartOfDay());throw new AssertionError("Invalid term accepted");}catch(java.time.DateTimeException expected){}
  System.out.println("PASS: widget today count, next class boundaries, date rollover, empty/end states, adjustments and selected conflict");
 }
}
