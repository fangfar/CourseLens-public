package cn.courselens;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

public final class ChaoxingChecks {
 private interface Work {void run()throws Exception;}
 private static void main(Instrumentation r,Work work){java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();r.runOnMainSync(()->{try{work.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new RuntimeException(error.get());}
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}

 private static final String COURSES="<ul><li><a href='/visit/stucoursemiddle?courseid=1&amp;clazzid=2&amp;cpi=3'>课程</a><span class='course-name' title='合成课程 A'></span></li><li><a href='/visit/stucoursemiddle?courseid=4&amp;clazzid=5&amp;cpi=6'>课程</a><span class='course-name' title='合成课程 B'></span></li></ul>";
 private static final String TOKENS="<input id='enc' value='page-token'><input value='work-token' id='workEnc'><input id='openc' value='open-token'>";
 private static final String LIST="<ul><li data='/mooc-ans/mooc2/work/task?workId=7&amp;enc=x' onclick='goTask(this);'><p class='overHidden2 fl'>合成作业 &amp; 报告</p><p class='status fl'>未提交</p></li></ul>";
 private static ChaoxingClient fake(List<String> calls,boolean failSecond){return new ChaoxingClient((url,body)->{
  calls.add(url);
  if(url.endsWith("fanyalogin")){check(body!=null&&!body.contains("fixture-password")&&body.contains("uname="),"Credential transport not encoded");return "{\"status\":true}";}
  check(body==null,"Query unexpectedly submitted a form");
  if(url.endsWith("/base"))return "<script>var fid = '100';</script>";
  if(url.contains("courselistdata"))return COURSES;
  if(url.contains("stucoursemiddle")){if(failSecond&&url.contains("courseid=4"))throw new java.io.IOException("fixture failure");return TOKENS;}
  if(url.contains("/work/list")){check(url.contains("enc=work-token")&&url.contains("stuenc=page-token")&&url.contains("openc=open-token"),"Wrong platform access parameters");return url.contains("courseId=4")?"<p>暂无作业</p>":LIST;}
  if(url.contains("/work/task"))return "<p>作答时间：10-18 08:00 至 10-21 23:59</p>";
  throw new AssertionError("Unexpected request");
 });}
 private static void clientChecks()throws Exception {
  check(ChaoxingClient.institution("<script>let fid = '100';</script>").equals("100"),"Modern institution declaration ignored");
  check(ChaoxingClient.institution("<input name='fid' value='101'>").equals("101"),"Institution input ignored");
  String clickCourse="<li><div onclick=\"window.open('/visit/stucoursemiddle?courseid=1&amp;clazzid=2&amp;cpi=3')\"></div><span class='course-name'>合成点击课程</span></li>";
  check(ChaoxingClient.subjects(clickCourse).size()==1,"Click-handler course ignored");
  List<String> calls=new ArrayList<>();ChaoxingClient client=fake(calls,false);client.login("fixture-user","fixture-password");ChaoxingClient.Result result=client.sync(s->{});
  check(result.total==2&&result.checked==2&&result.warnings.isEmpty(),"Automatic traversal incomplete");check(result.items.size()==1,"Homework aggregation failed");check(result.items.get(0).course.equals("合成课程 A")&&result.items.get(0).title.equals("合成作业 & 报告")&&result.items.get(0).deadline.equals("10-21 23:59"),"Metadata differs");
  check(calls.size()==8,"Wrong login/query sequence");client.close();boolean canceled=false;try{client.sync(s->{});}catch(java.io.InterruptedIOException e){canceled=true;}check(canceled,"Closed session still queries");
  result=fake(new ArrayList<>(),true).sync(s->{});check(result.checked==1&&result.warnings.size()==1&&result.items.size()==1,"Partial failures not represented");
  for(String html:new String[]{"<html>登录</html>","<p>没有此页面访问权限</p>"}){boolean rejected=false;try{ChaoxingClient.works(html,"test");}catch(java.io.IOException e){rejected=true;}check(rejected,"Unknown page reported as empty homework");}
  check(ChaoxingClient.paged("<p>共10页</p>")&&!ChaoxingClient.paged("<p>共1页</p>"),"Pagination completeness check failed");
  check(!ChaoxingClient.safeDetail("https://evil.example/mooc2/work/task"),"Foreign detail endpoint allowed");check(!ChaoxingClient.safeDetail("https://mooc1.chaoxing.com/work/addStudentWorkNew"),"Write endpoint allowed");
  check(ChaoxingClient.deadline("<p>截止时间：2026-10-21 23:59</p>").equals("2026-10-21 23:59"),"Explicit deadline failed");check(ChaoxingClient.deadline("<p>没有日期</p>").equals("截止时间未知"),"Deadline invented");
  check(!new ChaoxingClient.Work("c","w","未完成","u").completed(),"Pending status treated as complete");check(new ChaoxingClient.Work("c","w","待批阅","u").completed(),"Submitted status not complete");
  boolean rejected=false;try{new ChaoxingClient((url,body)->"{\"status\":false}").login("fixture","fixture-password");}catch(java.io.IOException e){rejected=true;}check(rejected,"Rejected login allowed");
 }
 public static void run(Instrumentation r){Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;try{check(r.getTargetContext().getPackageName().endsWith(".qa"),"QA only");clientChecks();result.putString("result","PASS: login/query chain, course tokens, deadlines, partial failures and read-only boundary");code=Activity.RESULT_OK;}catch(Throwable e){result.putString("error",android.util.Log.getStackTraceString(e));}r.finish(code,result);}
}
