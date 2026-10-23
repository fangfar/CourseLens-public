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
}
