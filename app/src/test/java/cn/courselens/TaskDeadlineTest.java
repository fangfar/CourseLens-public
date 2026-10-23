package cn.courselens;

import org.junit.Test;
import java.time.LocalDateTime;
import static org.junit.Assert.*;

public class TaskDeadlineTest {
 @Test public void acceptsPlatformFormatsWithoutGuessingYear(){
  assertEquals(LocalDateTime.of(2026,10,23,9,5),TaskDeadline.parse("2026年10月23日 9:05"));
  assertNotNull(TaskDeadline.parse("2026/10/23 09:05:30"));
  assertNull(TaskDeadline.parse("10-23 09:05"));assertNull(TaskDeadline.parse("2026-02-30 09:05"));
 }
 @Test public void distinguishesExpiredAndSubHourDeadlines(){
  LocalDateTime now=LocalDateTime.of(2026,10,23,9,0);
  assertEquals("已过期 1 分钟",TaskDeadline.relative(now.minusSeconds(1),now));
  assertEquals("还剩 30 分钟",TaskDeadline.relative(now.plusMinutes(30),now));
  assertEquals("现在截止",TaskDeadline.relative(now,now));
  assertEquals("截止时间未知",TaskDeadline.relative(null,now));
 }
 @Test public void sortingKeepsUnknownDatesLastWithoutChangingSource(){
  ChaoxingClient.Work unknown=new ChaoxingClient.Work("A","unknown","未完成","");unknown.deadline="10-23 12:00";
  ChaoxingClient.Work later=new ChaoxingClient.Work("A","later","未完成","");later.deadline="2026-10-25 12:00";
  ChaoxingClient.Work earlier=new ChaoxingClient.Work("B","earlier","未完成","");earlier.deadline="2026-10-23 12:00";
  java.util.List<ChaoxingClient.Work> source=java.util.Arrays.asList(unknown,later,earlier),sorted=TaskDeadline.sorted(source);
  assertSame(earlier,sorted.get(0));assertSame(unknown,sorted.get(2));assertSame(unknown,source.get(0));
  assertEquals("还剩 1 天 2 小时",TaskDeadline.countdown(LocalDateTime.of(2026,10,24,11,0),LocalDateTime.of(2026,10,23,9,0)));
 }
}
