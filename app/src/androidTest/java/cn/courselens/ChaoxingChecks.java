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
 private static Field field(String name)throws Exception{Field f=ChaoxingActivity.class.getDeclaredField(name);f.setAccessible(true);return f;}
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
 private static void capture(Activity activity,String name)throws Exception {View view=activity.findViewById(android.R.id.content);android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(view.getWidth(),view.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);view.draw(new android.graphics.Canvas(image));try(java.io.FileOutputStream out=activity.openFileOutput(name,0)){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}image.recycle();}
 public static void run(Instrumentation r){
  Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity home=null;ChaoxingActivity activity=null;
  try{
   check(r.getTargetContext().getPackageName().endsWith(".qa"),"QA only");clientChecks();home=NativeChecks.startQaActivity(r);MainActivity a=home;
   Map<String,?> prefs=new HashMap<>(home.getPreferences(0).getAll());
   Instrumentation.ActivityMonitor monitor=r.addMonitor(ChaoxingActivity.class.getName(),null,false);
   main(r,()->{Method tasks=MainActivity.class.getDeclaredMethod("tasks");tasks.setAccessible(true);tasks.invoke(a);a.findViewById(android.R.id.content).findViewWithTag("chaoxing-connect").performClick();});
   activity=(ChaoxingActivity)r.waitForMonitorWithTimeout(monitor,10000);r.removeMonitor(monitor);check(activity!=null,"Login screen did not open");ChaoxingActivity screen=activity;r.waitForIdleSync();
   main(r,()->{Button button=screen.findViewById(android.R.id.content).findViewWithTag("chaoxing-login");check(button.getBackgroundTintList().getDefaultColor()==SecondaryUi.AQUA,"Button not using shared design");button.performClick();check(((TextView)field("message").get(screen)).getText().toString().equals("请输入学习通账号和密码。"),"Blank login not rejected");capture(screen,"qa-chaoxing-login.png");((EditText)field("account").get(screen)).setText("fixture-user");((EditText)field("password").get(screen)).setText("fixture-password");field("client").set(screen,fake(new ArrayList<>(),false));Method start=ChaoxingActivity.class.getDeclaredMethod("start",boolean.class);start.setAccessible(true);start.invoke(screen,true);check(((EditText)field("password").get(screen)).length()==0,"Password not cleared");});
   for(int i=0;i<40;i++){boolean[] busy={true};main(r,()->busy[0]=field("busy").getBoolean(screen));if(!busy[0])break;SystemClock.sleep(100);}
   r.waitForIdleSync();main(r,()->{check(!field("busy").getBoolean(screen),"Automatic sync did not finish");check(((TextView)field("message").get(screen)).getText().toString().contains("2 / 2"),"Login did not automatically sync");check(((LinearLayout)field("results").get(screen)).getChildCount()==1,"Result card missing");capture(screen,"qa-chaoxing-results.png");check(a.getPreferences(0).getAll().equals(prefs),"Login/query changed timetable preferences");});
   result.putString("result","PASS: automatic login/query chain, course tokens, deadline, read-only boundary, partial failures, native form validation, shared buttons, unchanged timetable");code=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("error",android.util.Log.getStackTraceString(e));}
  finally{ChaoxingActivity screen=activity;MainActivity a=home;main(r,()->{if(screen!=null)screen.finish();if(a!=null)a.finish();});r.finish(code,result);}
 }
}
