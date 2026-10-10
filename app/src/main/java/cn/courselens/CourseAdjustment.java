package cn.courselens;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

public final class CourseAdjustment {
 public final String term;
 public final int week;
 public final Course original, replacement;
 public CourseAdjustment(String term,int week,Course original,Course replacement){
  LocalDate.parse(term);Course.parse(original.encode());
  if(week<1||week>30||!original.active(week))throw new IllegalArgumentException("原课程在该周没有安排");
  if(replacement!=null){
   Course.parse(replacement.encode());
   if(!replacement.weeks.matches("(?:[1-9]|[12][0-9]|30)")||!original.name.equals(replacement.name)||!original.teacher.equals(replacement.teacher)||!original.details.equals(replacement.details)||!original.className.equals(replacement.className))throw new IllegalArgumentException("调课只能修改一次的周次、星期、节次和教室");
   if(replacement.active(week)&&CourseDisplay.key(week,original).equals(CourseDisplay.key(week,replacement)))throw new IllegalArgumentException("请选择不同的上课安排");
  }
  this.term=term;this.week=week;this.original=original;this.replacement=replacement;
 }
 public String key(){return term+":"+CourseDisplay.key(week,original);}
 public boolean applies(List<Course> courses,String term){
  if(!this.term.equals(term))return false;
  for(Course c:courses)if(c.active(week)&&c.encode().equals(original.encode()))return true;
  return false;
 }
 public String encode(){return term+"|"+week+"|"+pack(original.encode())+"|"+(replacement==null?"":pack(replacement.encode()));}
 private static String pack(String s){return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
 private static Course unpack(String s){List<Course> rows=Course.parse(new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8));if(rows.size()!=1)throw new IllegalArgumentException("调整记录格式异常");return rows.get(0);}
 public static List<CourseAdjustment> parse(String text){
  List<CourseAdjustment> out=new ArrayList<>();Set<String> keys=new HashSet<>();
  for(String row:text.split("\n")){if(row.isEmpty())continue;String[] p=row.split("\\|",-1);if(p.length!=4)throw new IllegalArgumentException("调整记录格式异常");CourseAdjustment a=new CourseAdjustment(p[0],Integer.parseInt(p[1]),unpack(p[2]),p[3].isEmpty()?null:unpack(p[3]));if(!keys.add(a.key()))throw new IllegalArgumentException("重复的调整记录");out.add(a);}
  return out;
 }
 public static List<Course> apply(List<Course> courses,String term,List<CourseAdjustment> adjustments){
  List<CourseAdjustment> valid=new ArrayList<>();for(CourseAdjustment a:adjustments)if(a.applies(courses,term))valid.add(a);
  List<Course> out=new ArrayList<>();
  for(Course c:courses){
   Set<Integer> excluded=new HashSet<>();for(CourseAdjustment a:valid)if(c.active(a.week)&&CourseDisplay.key(a.week,c).equals(CourseDisplay.key(a.week,a.original)))excluded.add(a.week);
   if(excluded.isEmpty()){out.add(c);continue;}
   List<String> weeks=new ArrayList<>();for(int w=1;w<=30;w++)if(c.active(w)&&!excluded.contains(w))weeks.add(""+w);
   if(!weeks.isEmpty())out.add(new Course(c.day,c.start,c.end,c.name,c.room,String.join(",",weeks),c.teacher,c.details,c.className));
  }
  for(CourseAdjustment a:valid)if(a.replacement!=null)out.add(a.replacement);
  return out;
 }
}
