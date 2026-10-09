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
  for(String bad:new String[]{"8|1|2|课程|地点|6","1|3|2|课程|地点|6","1|1|2|课程|地点|16-1","1|1|2||地点|6","1|1|2|课程|地点|6|老师|多余字段","1|1|2|课程|地点||老师"}) {
   boolean rejected=false;try{Course.parse(bad);}catch(IllegalArgumentException e){rejected=true;}assert rejected;
  }
  System.out.println("PASS: 23 sample courses, week filtering, input rejection");
 }
}
