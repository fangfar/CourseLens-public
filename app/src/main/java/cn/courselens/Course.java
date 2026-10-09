package cn.courselens;

import java.util.*;

public final class Course {
 public final int day, start, end;
 public final String name, room, weeks, teacher;
 public Course(int d,int s,int e,String n,String r,String w) { this(d,s,e,n,r,w,""); }
 public Course(int d,int s,int e,String n,String r,String w,String t) { day=d;start=s;end=e;name=n;room=r;weeks=w;teacher=t; }
 public static List<Course> parse(String text) {
  List<Course> out=new ArrayList<>(); int line=0;
  for(String raw:text.split("\\n")) { line++; if(raw.trim().isEmpty())continue;
   String[] p=raw.split("\\|",-1);
   try {
    if(p.length!=6&&p.length!=7)throw new IllegalArgumentException();
    int d=Integer.parseInt(p[0].trim()),s=Integer.parseInt(p[1].trim()),e=Integer.parseInt(p[2].trim());
    if(d<1||d>7||s<1||e<s||e>12||p[3].trim().isEmpty())throw new IllegalArgumentException();
    String w=p[5].trim(); if(w.isEmpty())throw new IllegalArgumentException();
    for(String part:w.split(",",-1)) {
     String[] range=part.split("-",-1); if(range.length>2)throw new IllegalArgumentException();
     int a=Integer.parseInt(range[0]),b=range.length==2?Integer.parseInt(range[1]):a;
     if(a<1||b<a||b>30)throw new IllegalArgumentException();
    }
    out.add(new Course(d,s,e,p[3].trim(),p[4].trim(),w,p.length==7?p[6].trim():""));
   } catch(Exception ex) { throw new IllegalArgumentException("第 "+line+" 行格式不正确：星期1–7、节次1–12、周次1–30。",ex); }
  }
  return out;
 }
 public boolean active(int week) {
  for(String p:weeks.split(",")){String[] r=p.split("-");int a=Integer.parseInt(r[0]),b=r.length==2?Integer.parseInt(r[1]):a;if(week>=a&&week<=b)return true;}return false;
 }
 public String encode(){return day+"|"+start+"|"+end+"|"+name+"|"+room+"|"+weeks+(teacher.isEmpty()?"":"|"+teacher);}
}
