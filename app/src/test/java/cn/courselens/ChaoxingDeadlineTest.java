package cn.courselens;
import org.junit.Test;
import static org.junit.Assert.*;
public class ChaoxingDeadlineTest {
 private ChaoxingClient.Work work(String title,String state,String deadline){ChaoxingClient.Work w=new ChaoxingClient.Work("合成课程",title,state,"");w.courseKey="1:2";w.workKey="7";w.deadline=deadline;return w;}
 @Test public void localStatusEditDoesNotPreventMissingDeadlineFromBeingFilled(){
  ChaoxingClient.Result old=new ChaoxingClient.Result(),fresh=new ChaoxingClient.Result();ChaoxingClient.Work local=work("本地标题","未完成","截止时间未知");local.edited=true;old.items.add(local);fresh.items.add(work("平台标题","已提交","10-29 10:26"));fresh.readCourses.put("1:2",true);
  ChaoxingClient.Work merged=ChaoxingCache.merge(old,fresh).items.get(0);assertEquals("10-29 10:26",merged.deadline);assertEquals("本地标题",merged.title);assertEquals("未完成",merged.status);
 }

 @Test public void explicitLocalDeadlineClearSurvivesRefreshAndCacheRoundTrip()throws Exception{
  ChaoxingClient.Result old=new ChaoxingClient.Result(),fresh=new ChaoxingClient.Result();ChaoxingClient.Work local=work("本地任务","未完成","截止时间未知");local.edited=true;local.deadlineEdited=true;old.items.add(local);fresh.items.add(work("平台任务","未交","10-29 10:26"));fresh.readCourses.put("1:2",true);
  old=ChaoxingCache.decode(ChaoxingCache.encode(old));assertTrue(old.items.get(0).deadlineEdited);assertEquals("截止时间未知",ChaoxingCache.merge(old,fresh).items.get(0).deadline);
 }
 @Test public void onlyStartTimeDoesNotBecomeADeadline(){
  assertEquals("未提供截止时间",ChaoxingClient.deadline("<p>开始时间：10-22 10:25</p>"));
  assertEquals("截止时间未知",ChaoxingClient.deadline("<p>作答时间：页面加载失败</p>"));
  assertEquals("10-29 10:26",ChaoxingClient.deadline("<p>作答时间：10-22 10:26 至 10-29 10:26</p>"));
  assertEquals("未提供截止时间",TaskDeadline.display("未提供截止时间"));
 }
 @Test public void locallyReopenedSubmittedTaskReadsDetailOnlyWhenRequested()throws Exception{
  int[] details={0};try(ChaoxingClient client=new ChaoxingClient((url,body)->{
   if(url.endsWith("/base"))return "<input id='fid' value='1'>";
   if(url.contains("courselistdata"))return "<li><a title='合成课程' href='/visit/stucoursemiddle?courseid=1&amp;clazzid=2&amp;cpi=3'>课程</a></li>";
   if(url.contains("stucoursemiddle"))return "<input id='enc' value='page'><input id='workEnc' value='work'>";
   if(url.contains("/work/list"))return "<li data='/mooc-ans/mooc2/work/task?workId=7' onclick='goTask(this)'><p class='overHidden2'>合成任务</p><p class='status'>已提交</p></li>";
   if(url.contains("/work/task")){details[0]++;return "<p>截止时间：10-22 10:35</p>";}throw new AssertionError("Unexpected request");
  })){
   assertEquals("截止时间未知",client.sync(v->{}).items.get(0).deadline);assertEquals(0,details[0]);
   ChaoxingClient.Work read=client.sync(v->{},java.util.Collections.emptySet(),java.util.Collections.singleton("1:2:7")).items.get(0);assertEquals("10-22 10:35",read.deadline);assertEquals(1,details[0]);
   assertEquals(0,client.sync(v->{},java.util.Collections.singleton("1:2"),java.util.Collections.singleton("1:2:7")).items.size());assertEquals(1,details[0]);
  }
 }
}
