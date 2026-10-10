package cn.courselens;

import java.util.*;

public final class CourseDisplay {
 public static String key(int week,Course c){return week+":"+c.day+"|"+c.start+"|"+c.end+"|"+c.name+"|"+c.room+"|"+c.teacher+(c.details.isEmpty()&&c.className.isEmpty()?"":"|"+c.details+"|"+c.className);}
 private static String identity(Course c){return c.name+"|"+c.teacher+"|"+c.details+"|"+c.className;}
 private static long rank(String entry){try{String[] p=entry.split(":",3);return p.length==3&&p[0].equals("priority")?Math.max(0,Long.parseLong(p[1])):0;}catch(NumberFormatException e){return 0;}}
 private static boolean matches(String entry,Course c,boolean exact){if(rank(entry)==0)return false;String id=entry.split(":",3)[2];return exact?id.equals(identity(c)):id.split("\\|",-1)[0].equals(c.name);}
 private static long priority(Course c,Set<String> choices,boolean exact){long rank=0;for(String entry:choices)if(matches(entry,c,exact))rank=Math.max(rank,rank(entry));return rank;}
 public static long priority(Course c,Set<String> choices){return priority(c,choices,false);}
 public static void prefer(Course c,Set<String> choices){
  long latest=0;for(String entry:choices)latest=Math.max(latest,rank(entry));
  choices.removeIf(entry->matches(entry,c,false));
  choices.add("priority:"+Math.addExact(latest,1)+":"+identity(c));
 }
 public static final class Group {
  public final List<Course> courses;
  public final Course selected;
  private Group(List<Course> courses,int week,Set<String> choices){
   this.courses=Collections.unmodifiableList(new ArrayList<>(courses));Course selected=courses.get(0);
   for(Course c:courses)if(choices.contains(key(week,c))){selected=c;break;}
   long highest=0;for(Course c:courses){long rank=priority(c,choices);if(rank>highest||(rank>0&&rank==highest&&priority(c,choices,true)==rank)){selected=c;highest=rank;}}
   this.selected=selected;
  }
 }
 public static List<Group> groups(List<Course> courses,int week,Set<String> choices){
  List<Course> active=new ArrayList<>();Set<String> seen=new HashSet<>();
  for(Course c:courses)if(c.active(week)&&seen.add(key(week,c)))active.add(c);
  active.sort(Comparator.comparingInt((Course c)->c.day).thenComparingInt(c->c.start));
  List<Group> groups=new ArrayList<>();List<Course> current=new ArrayList<>();int day=0,end=0;
  for(Course c:active){
   if(c.day!=day||c.start>end){if(!current.isEmpty())groups.add(new Group(current,week,choices));current=new ArrayList<>();day=c.day;end=0;}
   current.add(c);end=Math.max(end,c.end);
  }
  if(!current.isEmpty())groups.add(new Group(current,week,choices));return groups;
 }
}
