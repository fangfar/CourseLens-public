package cn.courselens;

import java.time.*;
import java.util.regex.*;

final class TaskDeadline {
 private static final Pattern DATE=Pattern.compile("^(\\d{4})[-/.年](\\d{1,2})[-/.月](\\d{1,2})日?\\s+(\\d{1,2}):(\\d{2})(?::(\\d{2}))?$");
 private TaskDeadline(){}
 static LocalDateTime parse(String value){
  Matcher m=DATE.matcher(value.trim());if(!m.matches())return null;
  try{return LocalDateTime.of(Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2)),Integer.parseInt(m.group(3)),Integer.parseInt(m.group(4)),Integer.parseInt(m.group(5)),m.group(6)==null?0:Integer.parseInt(m.group(6)));}catch(DateTimeException e){return null;}
 }
 static java.util.List<ChaoxingClient.Work> sorted(java.util.List<ChaoxingClient.Work> source){
  java.util.List<ChaoxingClient.Work> result=new java.util.ArrayList<>(source);result.sort(java.util.Comparator.comparing((ChaoxingClient.Work w)->parse(w.deadline),java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));return result;
 }
 static String countdown(LocalDateTime due,LocalDateTime now){
  long minutes=Duration.between(now,due).toMinutes();if(minutes<60)return relative(due,now);long hours=minutes/60;return hours>=24?"还剩 "+hours/24+" 天 "+hours%24+" 小时":"还剩 "+hours+" 小时";
 }
 static String relative(LocalDateTime due,LocalDateTime now){
  if(due==null)return "截止时间未知";
  long seconds=Duration.between(now,due).getSeconds();if(seconds==0)return "现在截止";
  long minutes=Math.max(1,(Math.abs(seconds)+59)/60);
  String amount=minutes>=1440?((minutes+1439)/1440)+" 天":minutes>=60?((minutes+59)/60)+" 小时":minutes+" 分钟";
  return (seconds<0?"已过期 ":"还剩 ")+amount;
 }
}
