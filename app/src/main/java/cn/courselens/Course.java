package cn.courselens;

import java.util.*;
import java.util.regex.*;

public final class Course {
 public final int day, start, end;
 public final String name, room, weeks, teacher, details, className;
 public Course(int d,int s,int e,String n,String r,String w) { this(d,s,e,n,r,w,""); }
 public Course(int d,int s,int e,String n,String r,String w,String t) { this(d,s,e,n,r,w,t,""); }
 public Course(int d,int s,int e,String n,String r,String w,String t,String extra) { this(d,s,e,n,r,w,t,extra,""); }
 public Course(int d,int s,int e,String n,String r,String w,String t,String extra,String classes) { day=d;start=s;end=e;name=n;room=r;weeks=w;teacher=t;details=optionalText(extra);className=optionalText(classes); }
 public static String optionalText(String value){List<String> parts=new ArrayList<>();if(value!=null)for(String part:value.split("[;；]")){part=part.trim();if(!part.isEmpty()&&!part.equalsIgnoreCase("null"))parts.add(part);}return String.join("；",parts);}
 // shortcut: only recognizable class prefixes are split; add a school adapter if its naming format changes.
 public static Course withLessonText(int d,int s,int e,String n,String r,String w,String t,String extra){
  String value=optionalText(extra),classes="",classToken="(?:\\d{2,4}[\\p{IsHan}A-Za-z]+\\d+(?:班)?|[^\\s;；]+班)";
  Matcher m=Pattern.compile("^("+classToken+"(?:\\s*[;；]\\s*"+classToken+")*)(?:\\s+|[;；]\\s*|$)(.*)$").matcher(value);
  if(m.matches()){classes=m.group(1);value=optionalText(m.group(2));}
  return new Course(d,s,e,n,r,w,t,value,classes);
 }
 public static List<Course> parse(String text) {
  List<Course> out=new ArrayList<>(); int line=0;
  for(String raw:text.split("\\n")) { line++; if(raw.trim().isEmpty())continue;
   String[] p=raw.split("\\|",-1);
   try {
    if(p.length<6||p.length>9)throw new IllegalArgumentException();
    int d=Integer.parseInt(p[0].trim()),s=Integer.parseInt(p[1].trim()),e=Integer.parseInt(p[2].trim());
    if(d<1||d>7||s<1||e<s||e>12||p[3].trim().isEmpty())throw new IllegalArgumentException();
    String w=p[5].trim(); if(w.isEmpty())throw new IllegalArgumentException();
    for(String part:w.split(",",-1)) {
     String[] range=part.split("-",-1); if(range.length>2)throw new IllegalArgumentException();
     int a=Integer.parseInt(range[0]),b=range.length==2?Integer.parseInt(range[1]):a;
     if(a<1||b<a||b>30)throw new IllegalArgumentException();
    }
    String teacher=p.length>=7?p[6].trim():"",extra=p.length>=8?optionalText(p[7]):"";
    out.add(p.length==8?withLessonText(d,s,e,p[3].trim(),p[4].trim(),w,teacher,extra):new Course(d,s,e,p[3].trim(),p[4].trim(),w,teacher,extra,p.length==9?optionalText(p[8]):""));
   } catch(Exception ex) { throw new IllegalArgumentException("第 "+line+" 行格式不正确：星期1–7、节次1–12、周次1–30。",ex); }
  }
  return out;
 }
 public boolean active(int week) {
  for(String p:weeks.split(",")){String[] r=p.split("-");int a=Integer.parseInt(r[0]),b=r.length==2?Integer.parseInt(r[1]):a;if(week>=a&&week<=b)return true;}return false;
 }
 public String encode(){return day+"|"+start+"|"+end+"|"+name+"|"+room+"|"+weeks+(teacher.isEmpty()&&details.isEmpty()&&className.isEmpty()?"":"|"+teacher)+(details.isEmpty()&&className.isEmpty()?"":"|"+details)+(details.isEmpty()&&className.isEmpty()?"":"|"+className);}
}
