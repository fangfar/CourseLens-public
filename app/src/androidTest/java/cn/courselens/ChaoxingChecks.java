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
 private static ChaoxingClient fake(List<String> calls,boolean failSecond){return fake(calls,failSecond,false,false);}
 private static ChaoxingClient fake(List<String> calls,boolean failSecond,boolean partialPage,boolean failDeadline){return new ChaoxingClient((url,body)->{
  calls.add(url);
  if(url.endsWith("fanyalogin")){check(body!=null&&!body.contains("fixture-password")&&body.contains("uname="),"Credential transport not encoded");return "{\"status\":true}";}
  check(body==null,"Query unexpectedly submitted a form");
  if(url.endsWith("/base"))return "<script>var fid = '100';</script>";
  if(url.contains("courselistdata"))return COURSES;
  if(url.contains("stucoursemiddle")){if(failSecond&&url.contains("courseid=4"))throw new java.io.IOException("fixture failure");return TOKENS;}
  if(url.contains("/work/list")){check(url.contains("enc=work-token")&&url.contains("stuenc=page-token")&&url.contains("openc=open-token"),"Wrong platform access parameters");return url.contains("courseId=4")?"<p>暂无作业</p>":LIST+(partialPage?"<p>共2页</p>":"");}
  if(url.contains("/work/task")){if(failDeadline)throw new java.io.IOException("fixture detail failure");return "<p>作答时间：10-18 08:00 至 10-21 23:59</p>";}
  throw new AssertionError("Unexpected request");
 });}
 private static void clientChecks()throws Exception {
  check(ChaoxingClient.institution("<script>let fid = '100';</script>").equals("100"),"Modern institution declaration ignored");
  check(ChaoxingClient.institution("<input name='fid' value='101'>").equals("101"),"Institution input ignored");
  String clickCourse="<li><div onclick=\"window.open('/visit/stucoursemiddle?courseid=1&amp;clazzid=2&amp;cpi=3')\"></div><span class='course-name'>合成点击课程</span></li>";
  check(ChaoxingClient.subjects(clickCourse).size()==1,"Click-handler course ignored");
  check(ChaoxingClient.redirected("https://i.chaoxing.com/base","http://i.chaoxing.com/base?x=1").equals("https://i.chaoxing.com/base?x=1"),"Official redirect not upgraded before request");
  check(ChaoxingClient.redirected("https://i.chaoxing.com/base","/base?t=1").equals("https://i.chaoxing.com/base?t=1"),"Relative redirect failed");
  for(String target:new String[]{"http://evil.example/path","http://user@i.chaoxing.com/base","http://i.chaoxing.com:8080/base"}){boolean blocked=false;try{ChaoxingClient.redirected("https://i.chaoxing.com/base",target);}catch(java.io.IOException e){blocked=true;}check(blocked,"Unsafe redirect accepted");}
  String nested="<div><div><a href='/visit/stucoursemiddle?courseid=1&amp;clazzid=2&amp;cpi=3'><img></a></div><div><span class='course-name' title=''>高等数学</span></div></div>";
  check(ChaoxingClient.subjects(nested).get(0).name.equals("高等数学"),"Nested course name not found");
  String neighbors="<section><div><a href='/visit/stucoursemiddle?courseid=1&amp;clazzid=2&amp;cpi=3'>课程</a></div><div><a href='/visit/stucoursemiddle?courseid=4&amp;clazzid=5&amp;cpi=6'>课程</a><span class='course-name'>线性代数</span></div></section>";
  List<ChaoxingClient.Subject> named=ChaoxingClient.subjects(neighbors);check(named.get(0).name.equals("课程名称未识别")&&named.get(1).name.equals("线性代数"),"Neighbor course name assigned incorrectly");
  List<String> calls=new ArrayList<>();ChaoxingClient client=fake(calls,false);client.login("fixture-user","fixture-password");ChaoxingClient.Result result=client.sync(s->{});
  check(result.total==2&&result.checked==2&&result.warnings.isEmpty(),"Automatic traversal incomplete");check(result.items.size()==1,"Homework aggregation failed");check(result.items.get(0).course.equals("合成课程 A")&&result.items.get(0).title.equals("合成作业 & 报告")&&result.items.get(0).deadline.equals("10-21 23:59"),"Metadata differs");
  String cached=ChaoxingCache.encode(result);check(!cached.contains("workId")&&!cached.contains("enc="),"Signed URL persisted");ChaoxingClient.Result restored=ChaoxingCache.decode(cached);check(restored.items.size()==result.items.size()&&restored.items.get(0).title.equals(result.items.get(0).title)&&restored.items.get(0).deadline.equals("10-21 23:59"),"Offline snapshot changed");
  check(calls.size()==8,"Wrong login/query sequence");client.close();boolean canceled=false;try{client.sync(s->{});}catch(java.io.InterruptedIOException e){canceled=true;}check(canceled,"Closed session still queries");
  result=fake(new ArrayList<>(),true).sync(s->{});check(result.checked==1&&result.warnings.size()==1&&result.items.size()==1,"Partial failures not represented");
  for(String html:new String[]{"<html>登录</html>","<p>没有此页面访问权限</p>"}){boolean rejected=false;try{ChaoxingClient.works(html,"test");}catch(java.io.IOException e){rejected=true;}check(rejected,"Unknown page reported as empty homework");}
  check(ChaoxingClient.paged("<p>共10页</p>")&&!ChaoxingClient.paged("<p>共1页</p>"),"Pagination completeness check failed");
  check(!ChaoxingClient.safeDetail("https://evil.example/mooc2/work/task"),"Foreign detail endpoint allowed");check(!ChaoxingClient.safeDetail("https://mooc1.chaoxing.com/work/addStudentWorkNew"),"Write endpoint allowed");
  check(ChaoxingClient.deadline("<p>截止时间：2026-10-21 23:59</p>").equals("2026-10-21 23:59"),"Explicit deadline failed");check(ChaoxingClient.deadline("<p>没有日期</p>").equals("截止时间未知"),"Deadline invented");
  check(!new ChaoxingClient.Work("c","w","未完成","u").completed(),"Pending status treated as complete");check(new ChaoxingClient.Work("c","w","待批阅","u").completed(),"Submitted status not complete");
  boolean rejected=false;try{new ChaoxingClient((url,body)->"{\"status\":false}").login("fixture","fixture-password");}catch(java.io.IOException e){rejected=true;}check(rejected,"Rejected login allowed");
 }
 private static ChaoxingClient.Work work(String course,String title,String key,String workKey,String deadline){ChaoxingClient.Work item=new ChaoxingClient.Work(course,title,"未提交","");item.courseKey=key;item.workKey=workKey;item.deadline=deadline;return item;}
 private static ChaoxingClient.Result oldWork(){ChaoxingClient.Result result=new ChaoxingClient.Result();result.items.add(work("合成课程 A","合成作业 & 报告","","","10-20 12:00"));result.items.add(work("合成课程 A","旧已删除作业","","","10-19 12:00"));result.items.add(work("合成课程 B","保留作业","","","10-30 12:00"));return result;}
 private static void mergeChecks()throws Exception {
  ChaoxingClient.Result old=oldWork(),fresh=fake(new ArrayList<>(),true).sync(s->{}),merged=ChaoxingCache.merge(old,fresh);
  check(merged.items.size()==2&&merged.items.stream().noneMatch(w->w.title.equals("旧已删除作业")),"Successful course did not replace obsolete work");check(merged.items.stream().anyMatch(w->w.course.equals("合成课程 B")&&w.title.equals("保留作业")),"Failed course lost cached work");check(merged.items.stream().anyMatch(w->w.courseKey.equals("1:2")&&w.workKey.equals("7")&&w.deadline.equals("10-21 23:59")),"Legacy cache was not matched and updated");check(old.items.size()==3&&old.items.get(0).courseKey.isEmpty()&&old.items.get(0).deadline.equals("10-20 12:00"),"Merge mutated the previous snapshot");
  fresh=fake(new ArrayList<>(),false).sync(s->{});merged=ChaoxingCache.merge(old,fresh);check(merged.items.size()==1&&merged.items.get(0).course.equals("合成课程 A"),"Empty successful course did not clear its own old work");
  fresh=fake(new ArrayList<>(),true,true,true).sync(s->{});merged=ChaoxingCache.merge(old,fresh);check(merged.items.size()==3&&merged.items.stream().anyMatch(w->w.title.equals("旧已删除作业")),"Incomplete page discarded unseen cached work");check(merged.items.stream().anyMatch(w->w.workKey.equals("7")&&w.deadline.equals("10-20 12:00")),"Detail failure erased a known deadline or duplicated the updated task");
  ChaoxingClient.Result restored=ChaoxingCache.decode(ChaoxingCache.encode(merged));check(restored.items.stream().anyMatch(w->w.courseKey.equals("1:2")&&w.workKey.equals("7")),"Stable identities lost after reopening cache");
  org.json.JSONObject legacy=new org.json.JSONObject(ChaoxingCache.encode(old));org.json.JSONArray legacyItems=legacy.getJSONArray("items");for(int i=0;i<legacyItems.length();i++){legacyItems.getJSONObject(i).remove("courseKey");legacyItems.getJSONObject(i).remove("workKey");}check(ChaoxingCache.decode(legacy.toString()).items.size()==3,"Original schema-1 cache no longer loads");
  ChaoxingClient.Result sameNames=new ChaoxingClient.Result();sameNames.courses.add(new ChaoxingClient.Subject("同名课程","10","11","12"));sameNames.courses.add(new ChaoxingClient.Subject("同名课程","20","21","22"));sameNames.requested.add("10:11");sameNames.readCourses.put("10:11",true);sameNames.items.add(work("同名课程","同名作业","10:11","100","截止时间未知"));ChaoxingClient.Result existing=new ChaoxingClient.Result();existing.items.add(work("同名课程","同名作业","20:21","100","10-20 12:00"));merged=ChaoxingCache.merge(existing,sameNames);check(merged.items.size()==2&&merged.items.stream().anyMatch(w->w.courseKey.equals("10:11")&&w.deadline.equals("截止时间未知")),"Same-name courses cross-replaced work or borrowed a deadline");
 }
 private static Object get(Object owner,String name)throws Exception{Field field=owner.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(owner);}
 private static void set(Object owner,String name,Object value)throws Exception{Field field=owner.getClass().getDeclaredField(name);field.setAccessible(true);field.set(owner,value);}
 private static Object call(Object owner,String name,Class<?>[] types,Object... args)throws Exception{Method method=owner.getClass().getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(owner,args);}
 private static void waitPage(Instrumentation runner,TaskPage page)throws Exception {for(int i=0;i<100;i++){boolean[] busy=new boolean[1];main(runner,()->busy[0]=(boolean)get(page,"busy"));if(!busy[0]){runner.waitForIdleSync();SystemClock.sleep(350);return;}SystemClock.sleep(50);}throw new AssertionError("Course operation did not finish");}
 private static void capture(Instrumentation runner,MainActivity activity,String name)throws Exception{android.graphics.Bitmap image=runner.getUiAutomation().takeScreenshot();try(java.io.FileOutputStream out=activity.openFileOutput(name,0)){check(image!=null,"Screenshot missing");image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{if(image!=null)image.recycle();}}
 private static void partialUiChecks(Instrumentation runner)throws Exception {
  Context context=runner.getTargetContext();SharedPreferences prefs=context.getSharedPreferences("chaoxing_account",Context.MODE_PRIVATE);Map<String,?> backup=new HashMap<>(prefs.getAll());String username="qa-partial-update-"+System.nanoTime();ChaoxingCache cache=new ChaoxingCache(context,username);MainActivity[] activity=new MainActivity[1];TaskPage[] page=new TaskPage[1];
  try{
   new ChaoxingAccount(context).save(username,"synthetic-password");cache.save(oldWork());activity[0]=NativeChecks.startQaActivity(runner);main(runner,()->{call(activity[0],"tasks",new Class<?>[]{});page[0]=(TaskPage)get(activity[0],"taskPage");});waitPage(runner,page[0]);
   main(runner,()->{check(page[0].findViewWithTag("chaoxing-course-selection")==null,"Canceled course selector is still visible");set(page[0],"snapshot",oldWork());call(page[0],"busy",new Class<?>[]{boolean.class},true);});call(page[0],"sync",new Class<?>[]{ChaoxingClient.class,int.class,String.class},fake(new ArrayList<>(),true),(int)get(page[0],"generation"),username);waitPage(runner,page[0]);check(cache.load().items.size()==2&&cache.load().items.stream().anyMatch(w->w.title.equals("保留作业")),"Task-page partial update did not persist successes and retain failed work");
   main(runner,()->{String status=((TextView)page[0].findViewWithTag("chaoxing-status")).getText().toString();check(status.contains("已更新 1 / 2")&&!status.contains("本次读取不完整"),"Task page still rejects the whole partial update");LinearLayout list=page[0].findViewWithTag("chaoxing-results");View notes=list.findViewWithTag("task-read-notes");check(notes!=null&&list.indexOfChild(notes)>0&&((TextView)list.getChildAt(list.getChildCount()-1)).getText().toString().contains("合成课程 B"),"Read failure is not at the end of the list");});runner.waitForIdleSync();capture(runner,activity[0],"qa-partial-course-update.png");
  }finally{
   if(activity[0]!=null)main(runner,()->activity[0].finish());SharedPreferences.Editor restore=prefs.edit().clear();for(Map.Entry<String,?> entry:backup.entrySet())restore.putString(entry.getKey(),(String)entry.getValue());check(restore.commit(),"Account preferences not restored");((android.util.AtomicFile)get(cache,"file")).delete();
  }
 }
 public static void run(Instrumentation r){Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;try{check(r.getTargetContext().getPackageName().endsWith(".qa"),"QA only");clientChecks();mergeChecks();partialUiChecks(r);result.putString("result","PASS: partial merge, failed/unvisited retention, empty-course replacement, paging and deadline fallback, legacy cache, stable identities, persisted task-page partial update, login/query chain and read-only boundary");code=Activity.RESULT_OK;}catch(Throwable e){result.putString("error",android.util.Log.getStackTraceString(e));}r.finish(code,result);}
}
