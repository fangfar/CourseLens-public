import cn.courselens.*;
import java.time.*;
import java.util.*;

public class WidgetScheduleTest {
 public static void main(String[] args){
  String term="2026-08-31",data="1|1|2|上午课程|A|1-3\n1|6|7|下午课程|B|1-3\n2|3|4|明日课程|C|1-3";List<Course> courses=Course.parse(data);LocalDate first=LocalDate.parse(term);
  WidgetSchedule s=WidgetSchedule.from(data,term,"",Set.of(),first.atTime(7,59));
  assert s.remainingCount==2&&s.next.name.equals("上午课程");
  assert s.following.name.equals("下午课程");
  assert s.todayCount==2&&s.tomorrow.name.equals("明日课程");
  s=WidgetSchedule.from(data,term,"",Set.of(),first.atTime(23,59));assert s.remainingCount==0&&s.todayCount==2&&s.tomorrow!=null;
  s=WidgetSchedule.from(data,term,"",Set.of(),first.plusDays(2).atStartOfDay());assert s.todayCount==0&&s.tomorrow==null;
  s=WidgetSchedule.from("7|1|2|周日|A|1\n1|3|4|跨周明日|B|2",term,"",Set.of(),first.plusDays(6).atTime(23,0));assert s.todayCount==1&&s.tomorrow.name.equals("跨周明日");
  Course tomorrowCourse=courses.get(2);s=WidgetSchedule.from(data,term,new CourseAdjustment(term,1,tomorrowCourse,null).encode(),Set.of(),first.atStartOfDay());assert s.tomorrow==null;
  assert WidgetSchedule.from(data,term,"",Set.of(),first.atTime(8,0,1)).following.name.equals("下午课程");
  assert WidgetSchedule.from("1|1|2|仅一次|A|1",term,"",Set.of(),first.atStartOfDay()).following==null;
  assert WidgetSchedule.from(data,term,"",Set.of(),first.atTime(9,30)).next.name.equals("下午课程");
  assert WidgetSchedule.from(data,term,"",Set.of(),first.atTime(23,59)).next==null;
  assert WidgetSchedule.from(data,term,"",Set.of(),first.plusDays(1).atStartOfDay()).remainingCount==1;
  assert WidgetSchedule.from(data,term,"",Set.of(),first.minusDays(1).atStartOfDay()).remainingCount==0;
  assert WidgetSchedule.from(data,term,"",Set.of(),first.plusDays(210).atStartOfDay()).next==null;
  assert WidgetSchedule.from("",term,"",Set.of(),first.atStartOfDay()).remainingCount==0;
  CourseAdjustment cancel=new CourseAdjustment(term,1,courses.get(0),null);
  s=WidgetSchedule.from(data,term,cancel.encode(),Set.of(),first.atTime(7,59));assert s.remainingCount==1&&s.next.name.equals("下午课程")&&s.following==null;
  Course original=courses.get(0),moved=new Course(2,1,2,original.name,"新教室","2");CourseAdjustment change=new CourseAdjustment(term,1,original,moved);
  s=WidgetSchedule.from(data,term,change.encode(),Set.of(),first.plusDays(8).atTime(7,59));assert s.remainingCount==2&&s.next.room.equals("新教室");
  String conflicts=data+"\n1|1|2|冲突课程|D|1-3";Course chosen=Course.parse(conflicts).get(3);
  s=WidgetSchedule.from(conflicts,term,"",Set.of(CourseDisplay.key(1,chosen)),first.atTime(7,59));assert s.remainingCount==2&&s.next.name.equals("冲突课程");
  Set<String> priorities=new HashSet<>();CourseDisplay.prefer(chosen,priorities);
  s=WidgetSchedule.from(conflicts,term,"",priorities,first.plusWeeks(1).atTime(7,59));assert s.remainingCount==2&&s.next.name.equals("冲突课程");
  String only=original.encode();s=WidgetSchedule.from(only,term,cancel.encode()+"\n"+new CourseAdjustment(term,2,original,null).encode()+"\n"+new CourseAdjustment(term,3,original,null).encode(),Set.of(),first.atStartOfDay());assert s.next==null&&s.remainingCount==0&&s.todayCount==0;
  s=WidgetSchedule.from(data,term,"",Set.of(),first.atTime(9,29));assert s.remainingCount==2&&s.next.name.equals("上午课程");
  s=WidgetSchedule.from(data,term,"",Set.of(),first.atTime(9,30));assert s.remainingCount==1&&s.following==null;
  s=WidgetSchedule.from(data,term,"",Set.of(),first.atTime(15,30));assert s.remainingCount==0&&s.next==null&&s.following==null;
  s=WidgetSchedule.from(data+"\n1|8|9|晚课|E|1",term,"",Set.of(),first.atStartOfDay());assert s.remaining.size()==3&&s.remaining.get(2).name.equals("晚课")&&s.remainingCount==3&&s.next.name.equals("上午课程")&&s.following.name.equals("下午课程");
  try{WidgetSchedule.from(data,"invalid","",Set.of(),first.atStartOfDay());throw new AssertionError("Invalid term accepted");}catch(java.time.DateTimeException expected){}
  System.out.println("PASS: widget remaining today count, end time boundaries, date rollover, empty/end states, adjustments and selected conflict");
 }
}
