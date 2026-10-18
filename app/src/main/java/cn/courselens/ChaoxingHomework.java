package cn.courselens;

import java.net.URI;
import java.util.regex.*;

final class ChaoxingHomework {
 final String title, details, deadline, status;
 private ChaoxingHomework(String title,String details,String deadline,String status){this.title=title;this.details=details;this.deadline=deadline;this.status=status;}

 static boolean trusted(String url){
  try{URI uri=URI.create(url);String host=uri.getHost();return "https".equalsIgnoreCase(uri.getScheme())&&uri.getRawUserInfo()==null&&(uri.getPort()==-1||uri.getPort()==443)&&host!=null&&(host.equalsIgnoreCase("chaoxing.com")||host.toLowerCase(java.util.Locale.ROOT).endsWith(".chaoxing.com"));}catch(IllegalArgumentException e){return false;}
 }
 static ChaoxingHomework parse(String text){
  if(text==null||text.length()>8000)throw new IllegalArgumentException("作业信息为空或过长");
  String details=text.replace('\u00a0',' ').trim();
  if(details.isEmpty())throw new IllegalArgumentException("作业信息为空");
  String title=details.split("\\r?\\n",2)[0].trim();
  Matcher deadline=Pattern.compile("(?:截止时间|截止日期|截止)\\s*[:：]?\\s*(\\d{4}[-/.年]\\d{1,2}[-/.月]\\d{1,2}日?(?:\\s+\\d{1,2}:\\d{2}(?::\\d{2})?)?)").matcher(details);
  Matcher status=Pattern.compile("未提交|待完成|待批阅|已完成|已提交|已批阅|已截止").matcher(details);
  return new ChaoxingHomework(title,details,deadline.find()?deadline.group(1):"页面未提供明确截止时间",status.find()?status.group():"状态未识别");
 }
}
