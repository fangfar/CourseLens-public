import cn.courselens.*;
import java.time.*;
import java.util.*;

public class CourseAdjustmentTest {
 private static final String TERM="2026-08-31";
 private static Course move(Course c,int week,int day,int start,int end,String room){return new Course(day,start,end,c.name,room,""+week,c.teacher,c.details,c.className);}
 private static List<CourseDisplay.Group> groups(List<Course> base,List<CourseAdjustment> changes,int week){return CourseDisplay.groups(CourseAdjustment.apply(base,TERM,changes),week,Collections.emptySet());}
 private static void rejected(Runnable action){try{action.run();throw new AssertionError("Invalid adjustment accepted");}catch(IllegalArgumentException expected){}}
 public static void main(String[] args){
  Course original=new Course(1,1,2,"物理实验","A","1-3","老师","项目","班级"),other=new Course(2,3,4,"其他课程","C","1-3");
  List<Course> base=Arrays.asList(original,other);String encoded=original.encode();
  CourseAdjustment cancel=new CourseAdjustment(TERM,1,original,null);
  assert groups(base,List.of(cancel),1).size()==1;
  assert groups(base,List.of(cancel),2).size()==2;
  assert original.encode().equals(encoded);
  CourseAdjustment shifted=new CourseAdjustment(TERM,1,original,move(original,2,3,6,7,"新教室"));
  List<Course> effective=CourseAdjustment.apply(base,TERM,List.of(shifted));
  assert groups(base,List.of(shifted),1).size()==1;
  assert groups(base,List.of(shifted),2).size()==3;
  Course moved=effective.get(effective.size()-1);
  assert moved.active(2)&&!moved.active(1)&&moved.teacher.equals("老师")&&moved.details.equals("项目")&&moved.className.equals("班级");
  NextClass next=NextClass.find(effective,LocalDate.parse(TERM),LocalDate.parse(TERM).atTime(7,0));
  assert next.courses.get(0).name.equals("其他课程");
  next=NextClass.find(effective,LocalDate.parse(TERM),LocalDate.parse(TERM).plusDays(8).atTime(10,31));
  assert next.courses.get(0).room.equals("新教室")&&next.start.equals(LocalDate.parse(TERM).plusDays(9).atTime(14,0));
  CourseAdjustment collision=new CourseAdjustment(TERM,1,original,move(original,1,2,3,4,"新教室"));
  assert groups(base,List.of(collision),1).size()==1&&groups(base,List.of(collision),1).get(0).courses.size()==2;
  Set<String> choices=Set.of(CourseDisplay.key(1,collision.replacement));
  assert NextClass.find(CourseAdjustment.apply(base,TERM,List.of(collision)),LocalDate.parse(TERM),LocalDate.parse(TERM).atStartOfDay(),choices).courses.get(0).room.equals("新教室");
  assert groups(Arrays.asList(original,original,new Course(1,1,2,original.name,original.room,"1",original.teacher,original.details,original.className)),List.of(cancel),1).isEmpty();
  Course sameNameDifferentProject=new Course(1,1,2,original.name,original.room,original.weeks,original.teacher,"另一项目",original.className);
  assert groups(Arrays.asList(original,sameNameDifferentProject),List.of(cancel),1).get(0).selected.details.equals("另一项目");
  assert groups(base,List.of(cancel,new CourseAdjustment(TERM,2,original,null)),3).size()==2;
  assert CourseAdjustment.apply(base,"2027-02-22",List.of(cancel)).get(0)==original;
  assert !cancel.applies(List.of(other),TERM);
  assert !cancel.applies(List.of(move(original,1,1,1,2,"changed")),TERM);
  assert cancel.applies(Course.parse(encoded+"\n"+other.encode()),TERM);
  String persisted=shifted.encode()+"\n"+new CourseAdjustment(TERM,3,other,null).encode();
  assert CourseAdjustment.parse(persisted).size()==2&&CourseAdjustment.parse(persisted).get(0).encode().equals(shifted.encode());
  assert CourseAdjustment.parse("").isEmpty();
  assert CourseAdjustment.apply(base,TERM,Collections.emptyList()).get(0)==original;
  CourseAdjustment last=new CourseAdjustment(TERM,1,original,move(original,30,7,12,12,"末周"));
  assert groups(base,List.of(last),30).get(0).selected.room.equals("末周");
  rejected(()->new CourseAdjustment(TERM,0,original,null));rejected(()->new CourseAdjustment(TERM,4,original,null));
  rejected(()->new CourseAdjustment(TERM,1,original,move(original,31,1,1,2,"A")));
  rejected(()->new CourseAdjustment(TERM,1,original,move(original,1,1,4,2,"A")));
  rejected(()->new CourseAdjustment(TERM,1,original,move(original,1,8,1,2,"A")));
  rejected(()->new CourseAdjustment(TERM,1,original,move(original,1,1,1,2,"A")));
  rejected(()->new CourseAdjustment(TERM,1,original,new Course(1,1,2,"changed","A","2")));
  rejected(()->new CourseAdjustment(TERM,1,original,new Course(1,1,2,original.name,"A","1-2",original.teacher,original.details,original.className)));
  rejected(()->CourseAdjustment.parse("broken"));rejected(()->CourseAdjustment.parse(cancel.encode()+"\n"+cancel.encode()));
  rejected(()->CourseAdjustment.parse(TERM+"|1|invalid-base64|"));
  System.out.println("PASS: single occurrence cancellation/move, cross-week and last-week bounds, metadata, duplicates, conflict/next-class integration, persistence, stale source/term and validation");
 }
}
