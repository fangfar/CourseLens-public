import cn.courselens.Course;
import java.nio.file.*;
public class CourseTest {
 public static void main(String[] args)throws Exception {
  var courses=Course.parse(Files.readString(Path.of("app/src/main/assets/sample.txt")).replace("\uFEFF",""));
  assert courses.size()==23;
  assert courses.get(0).active(6)&&!courses.get(0).active(7);
  var c=Course.parse("7|10|12|课程|地点|1-3,6").get(0);
  assert c.active(2)&&c.active(6)&&!c.active(4);
  var legacy=Course.parse("1|1|2|旧课程|旧地点老师|1-16").get(0);
  assert legacy.teacher.isEmpty()&&legacy.encode().equals("1|1|2|旧课程|旧地点老师|1-16");
  var added=Course.parse("2|3|4|新课程|A101|1,7,16|王老师").get(0);
  assert added.teacher.equals("王老师")&&added.room.equals("A101")&&added.active(7)&&!added.active(6);
  assert Course.parse(added.encode()).get(0).teacher.equals("王老师");
  var experiment=Course.parse("6|6|7|大学物理实验|L403|6,8||测量重力加速度").get(0);
  assert experiment.teacher.isEmpty()&&experiment.details.equals("测量重力加速度");
  assert Course.parse(experiment.encode()).get(0).details.equals(experiment.details);
  var separated=Course.parse("6|6|7|测试实验|A101|6||26测试1;26测试2 测试实验项目； null").get(0);
  assert separated.className.equals("26测试1；26测试2")&&separated.details.equals("测试实验项目");
  var classesOnly=Course.parse("2|3|5|测试课程|A101|6||26测试1;26测试2； null").get(0);
  assert classesOnly.details.isEmpty()&&classesOnly.className.equals("26测试1；26测试2");
  assert Course.parse(separated.encode()).get(0).className.equals(separated.className);
  assert Course.parse("1|1|2|测试课|A101|1|||测试一班").get(0).className.equals("测试一班");
  assert Course.optionalText("null； <null>；NULL；nullification").equals("<null>；nullification");
  assert Course.parse("1|1|2|测试课|A101|1||null").get(0).details.isEmpty();
  assert Course.parse(new Course(1,1,2,"测试课","A101","1","","测试一班","").encode()).get(0).details.equals("测试一班");
  assert added.details.isEmpty()&&legacy.details.isEmpty();
  for(String bad:new String[]{"8|1|2|课程|地点|6","1|3|2|课程|地点|6","1|1|2|课程|地点|16-1","1|1|2||地点|6","1|1|2|课程|地点|6|老师|说明|班级|多余字段","1|1|2|课程|地点||老师"}) {
   boolean rejected=false;try{Course.parse(bad);}catch(IllegalArgumentException e){rejected=true;}assert rejected;
  }
  System.out.println("PASS: 23 sample courses, week filtering, input rejection");
 }
}
