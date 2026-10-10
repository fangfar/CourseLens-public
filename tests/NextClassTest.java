import cn.courselens.*;
import java.time.*;
import java.util.*;

public class NextClassTest {
 public static void main(String[] args){
  LocalDate term=LocalDate.of(2026,8,31);
  List<Course> courses=Course.parse("1|1|2|周一|A|1,3\n7|12|12|周日|B|1\n1|3|4|下周|C|2\n1|1|2|周一|A|1,3\n1|1|2|周一|A|1\n1|1|2|重叠课|D|1");
  NextClass next=NextClass.find(courses,term,term.atTime(7,59));
  assert next.courses.size()==1&&next.courses.get(0).name.equals("周一");
  assert next.minutesUntil(term.atTime(7,59))==1;
  assert next.minutesUntil(term.atTime(7,58,59))==2;
  assert next.minutesUntil(term.atTime(7,58,59,999000000))==2;
  assert NextClass.find(courses,term,term.atTime(8,0)).courses.size()==1;
  assert NextClass.find(courses,term,term.atTime(8,0,1)).courses.get(0).name.equals("周日");
  assert NextClass.find(courses,term,term.plusDays(6).atTime(20,21)).start.equals(term.plusDays(7).atTime(9,50));
  assert NextClass.find(courses,term,term.plusDays(7).atTime(9,51)).start.equals(term.plusDays(14).atTime(8,0));
  assert NextClass.find(courses,term,term.minusDays(1).atStartOfDay()).start.equals(term.atTime(8,0));
  assert NextClass.find(courses,term,term.plusDays(210).atStartOfDay())==null;
  assert NextClass.find(Collections.emptyList(),term,term.atStartOfDay())==null;
  List<Course> last=Course.parse("7|12|12|最后一课||30");
  assert NextClass.find(last,term,term.atStartOfDay()).start.equals(term.plusDays(209).atTime(20,20));
  assert NextClass.find(last,term,term.plusDays(209).atTime(20,20,1))==null;
  assert NextClass.find(courses,term,term.atStartOfDay()).courses.get(0).encode().equals(courses.get(0).encode());
  System.out.println("PASS: next class boundaries, cross-week filtering, duplicates, overlaps, empty and semester end");
 }
}
