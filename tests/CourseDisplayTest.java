import cn.courselens.*;
import java.time.*;
import java.util.*;

public class CourseDisplayTest {
 public static void main(String[] args){
  List<Course> courses=Course.parse("1|1|2|甲|A|1-3\n1|1|2|甲|A|1\n1|1|2|乙|B|1-3\n1|4|5|独立课|C|1\n2|1|2|周二|D|1\n1|1|2|别周|E|4");
  var groups=CourseDisplay.groups(courses,1,Collections.emptySet());
  assert groups.size()==3&&groups.get(0).courses.size()==2;
  assert groups.get(0).selected.name.equals("甲");
  Set<String> selected=new HashSet<>();selected.add(CourseDisplay.key(1,courses.get(2)));
  assert CourseDisplay.groups(courses,1,selected).get(0).selected.name.equals("乙");
  assert CourseDisplay.groups(courses,2,selected).get(0).selected.name.equals("甲");
  assert CourseDisplay.groups(courses,4,selected).get(0).selected.name.equals("别周");
  assert CourseDisplay.groups(courses,5,selected).isEmpty();
  List<Course> edited=Course.parse("1|1|2|甲|A|1-3\n1|1|2|乙|已改教室|1-3");
  assert CourseDisplay.groups(edited,1,selected).get(0).selected.name.equals("甲");
  List<Course> partial=Course.parse("1|1|2|甲|A|1\n1|2|3|乙|B|1\n1|3|4|丙|C|1\n1|5|6|丁|D|1");
  assert CourseDisplay.groups(partial,1,Collections.emptySet()).size()==2;
  assert CourseDisplay.groups(partial,1,Collections.emptySet()).get(0).courses.size()==3;
  assert courses.size()==6&&courses.get(2).name.equals("乙");
  NextClass next=NextClass.find(courses,LocalDate.of(2026,8,31),LocalDateTime.of(2026,8,31,7,0),selected);
  assert next.courses.size()==1&&next.courses.get(0).name.equals("乙");
  List<Course> experiments=Arrays.asList(new Course(1,1,2,"实验课","A","1","老师","项目甲","一班"),new Course(1,1,2,"实验课","A","1","老师","项目乙","一班"),new Course(1,1,2,"实验课","A","1","老师","项目甲","二班"));
  assert CourseDisplay.groups(experiments,1,Collections.emptySet()).get(0).courses.size()==3;
  Set<String> chosen=Collections.singleton(CourseDisplay.key(1,experiments.get(1)));
  assert CourseDisplay.groups(experiments,1,chosen).get(0).selected.details.equals("项目乙");
  assert NextClass.find(experiments,LocalDate.of(2026,8,31),LocalDateTime.of(2026,8,31,7,0),chosen).courses.get(0).details.equals("项目乙");
  assert CourseDisplay.key(1,courses.get(2)).equals("1:1|1|2|乙|B|");
  CourseDisplay.prefer(courses.get(2),selected);
  assert CourseDisplay.groups(courses,2,selected).get(0).selected.name.equals("乙");
  assert CourseDisplay.groups(edited,1,selected).get(0).selected.name.equals("乙");
  selected.add(CourseDisplay.key(3,courses.get(0)));
  assert CourseDisplay.groups(courses,3,selected).get(0).selected.name.equals("乙");
  CourseDisplay.prefer(courses.get(0),selected);
  assert CourseDisplay.groups(courses,1,selected).get(0).selected.name.equals("甲");
  assert CourseDisplay.groups(courses,2,selected).get(0).selected.name.equals("甲");
  CourseDisplay.prefer(courses.get(2),selected);
  assert CourseDisplay.groups(courses,3,selected).get(0).selected.name.equals("乙");
  Set<String> global=new HashSet<>();CourseDisplay.prefer(experiments.get(1),global);
  assert CourseDisplay.groups(experiments,1,global).get(0).selected.details.equals("项目乙");
  Course moved=new Course(2,3,4,"乙","新教室","2");
  Course other=new Course(2,3,4,"甲","新教室","2");
  assert CourseDisplay.groups(Arrays.asList(other,moved),2,selected).get(0).selected==moved;
  assert NextClass.find(courses,LocalDate.of(2026,8,31),LocalDateTime.of(2026,9,7,7,0),selected).courses.get(0).name.equals("乙");
  assert CourseDisplay.groups(courses,4,selected).get(0).selected.name.equals("别周");
  List<Course> labs=Course.parse("1|1|2|模电实验|A|1|教师甲|项目一|一班\n1|1|2|大学物理实验|B|1|教师乙|测重力|二班\n1|1|2|模电实验|C|2|教师丙|项目二|三班\n1|1|2|大学物理实验|D|2|教师丁|测电阻|四班");
  Set<String> labPriority=new HashSet<>();CourseDisplay.prefer(labs.get(1),labPriority);
  Course weekTwo=CourseDisplay.groups(labs,2,labPriority).get(0).selected;
  assert weekTwo.name.equals("大学物理实验")&&weekTwo.details.equals("测电阻")&&weekTwo.room.equals("D");
  assert NextClass.find(labs,LocalDate.of(2026,8,31),LocalDateTime.of(2026,9,7,7,0),labPriority).courses.get(0)==labs.get(3);
  CourseDisplay.prefer(labs.get(2),labPriority);
  assert CourseDisplay.groups(labs,1,labPriority).get(0).selected.name.equals("模电实验");
  assert CourseDisplay.groups(labs,2,labPriority).get(0).selected.details.equals("项目二");
  System.out.println("PASS: full and partial conflicts, deduplication, one default course, legacy choices, global priority, latest selection and moved courses and matching next class");
 }
}
