package cn.courselens;

public final class ChaoxingHomeworkTest {
 public static void main(String[] args){
  assert ChaoxingHomework.trusted("https://passport2.chaoxing.com/login");
  for(String url:new String[]{"http://i.chaoxing.com","https://chaoxing.com.evil.test","https://evilchaoxing.com","https://user@chaoxing.com","javascript:alert(1)","file:///x","https://chaoxing.com:444/","bad"})assert !ChaoxingHomework.trusted(url):url;
  ChaoxingHomework item=ChaoxingHomework.parse("测试作业一\n未提交\n截止时间：2026-10-20 23:59");
  assert item.title.equals("测试作业一");assert item.status.equals("未提交");assert item.deadline.equals("2026-10-20 23:59");
  item=ChaoxingHomework.parse("合成测试\n已提交\n开始时间：2026-10-18 12:00\n剩余2天");
  assert item.status.equals("已提交");assert item.deadline.equals("页面未提供明确截止时间");
  assert ChaoxingHomework.parse("合成条目").status.equals("状态未识别");
  for(String text:new String[]{"", "  ",null,"x".repeat(8001)}){boolean rejected=false;try{ChaoxingHomework.parse(text);}catch(IllegalArgumentException e){rejected=true;}assert rejected;}
  System.out.println("PASS: Chaoxing origin boundary and conservative homework text parsing");
 }
}
