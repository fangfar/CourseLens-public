package cn.courselens;

import android.app.AlertDialog;
import android.os.Looper;
import android.view.View;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.util.ReflectionHelpers.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=34,shadows=DataManagementTest.AtomicRename.class)
public class DataManagementTest {
 // Android rename replaces existing files; Windows File.renameTo does not.
 @org.robolectric.annotation.Implements(android.util.AtomicFile.class)
 public static class AtomicRename {
  @org.robolectric.annotation.Implementation protected static void rename(java.io.File source,java.io.File target)throws java.io.IOException {
   Files.move(source.toPath(),target.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
  }
 }
 private void tasksIdle(TaskPage page)throws Exception {
  ExecutorService worker=getField(page,"worker");
  for(int i=0;i<3;i++){worker.submit(()->{}).get(10,TimeUnit.SECONDS);shadowOf(Looper.getMainLooper()).idle();}
 }
 private void parcelsIdle()throws Exception {
  for(int i=0;i<3;i++){ParcelRepository.IO.submit(()->{}).get(10,TimeUnit.SECONDS);shadowOf(Looper.getMainLooper()).idle();}
 }
 @After public void closeDatabase()throws Exception {
  parcelsIdle();ParcelDatabase.get(RuntimeEnvironment.getApplication()).close();setStaticField(ParcelDatabase.class,"instance",null);
 }
 @Test public void deletedManualTaskCanBeRestoredFromHistoryAndDeletedAgain()throws Exception {
  ChaoxingClient.Result fixture=new ChaoxingClient.Result();ChaoxingClient.Work work=new ChaoxingClient.Work("合成课程","合成可恢复任务","未完成","");work.courseKey="local";work.workKey="local:restore-regression";fixture.items.add(work);
  ChaoxingCache cache=new ChaoxingCache(RuntimeEnvironment.getApplication(),"");cache.save(fixture);
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();callInstanceMethod(activity,"prepareTasks");TaskPage page=getField(activity,"taskPage");tasksIdle(page);
   callInstanceMethod(page,"delete",ClassParameter.from(ChaoxingClient.Work.class,work));tasksIdle(page);
   assertEquals(1,cache.load().deleted.size());page.history();View decor=activity.findViewById(android.R.id.content);View restore=decor.findViewWithTag("task-restore");assertNotNull(restore);restore.performClick();tasksIdle(page);
   assertTrue(cache.load().deleted.isEmpty());assertEquals(1,cache.load().items.size());assertFalse(cache.load().items.get(0).completed());
   callInstanceMethod(page,"delete",ClassParameter.from(ChaoxingClient.Work.class,work));tasksIdle(page);page.findViewWithTag("task-undo").performClick();tasksIdle(page);assertTrue(cache.load().deleted.isEmpty());assertEquals(1,cache.load().items.size());
  }
 }
 @Test public void taskMenuOffersDeletionHistoryWithoutAnAccount()throws Exception {
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();callInstanceMethod(activity,"prepareTasks");TaskPage page=getField(activity,"taskPage");tasksIdle(page);page.settings();View action=activity.findViewById(android.R.id.content).findViewWithTag("task-deletion-history");assertNotNull("Task dropdown must expose deletion history",action);action.performClick();shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(600));assertNotNull(activity.findViewById(android.R.id.content).findViewWithTag("task-history"));
  }
 }
 @Test public void locallyCompletedTaskCanBeReopenedWithoutExposingRemoteSubmissions()throws Exception {
  ChaoxingClient.Result fixture=new ChaoxingClient.Result();ChaoxingClient.Work work=new ChaoxingClient.Work("合成课程","此前点击圆圈的手动任务","已完成","");work.courseKey="local";work.workKey="local:completed-restore";fixture.items.add(work);ChaoxingCache cache=new ChaoxingCache(RuntimeEnvironment.getApplication(),"");cache.save(fixture);
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();callInstanceMethod(activity,"prepareTasks");TaskPage page=getField(activity,"taskPage");tasksIdle(page);ChaoxingClient.Result remote=new ChaoxingClient.Result();remote.items.add(new ChaoxingClient.Work("合成平台课程","平台已提交记录","已提交",""));setField(page,"snapshot",remote);
   page.history();View decor=activity.findViewById(android.R.id.content);View reopen=decor.findViewWithTag("task-reopen");assertNotNull("Previously hidden local completion must be recoverable",reopen);reopen.performClick();tasksIdle(page);assertFalse(cache.load().items.get(0).completed());assertTrue(cache.load().deleted.isEmpty());page.history();assertNull(decor.findViewWithTag("task-reopen"));
  }
 }
 @Test public void deletionActionsAreCentralizedAndCancellationKeepsData()throws Exception {
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();activity.getPreferences(0).edit().putString("courses","synthetic-timetable").commit();
   callInstanceMethod(activity,"menu");View general=getField(activity,"root");assertNull(general.findViewWithTag("clear-timetable"));general.findViewWithTag("data-management").performClick();
   View data=getField(activity,"root");assertEquals("数据管理",data.getTag());
   for(String tag:new String[]{"clear-timetable","task-deletion-history","clear-tasks","clear-parcels"})assertNotNull(data.findViewWithTag(tag));
   for(String tag:new String[]{"clear-timetable","clear-tasks","clear-parcels"}){
    if(tag.equals("clear-tasks")){callInstanceMethod(activity,"prepareTasks");tasksIdle(getField(activity,"taskPage"));}
    data.findViewWithTag(tag).performClick();AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(dialog.isShowing());dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertSame(data,getField(activity,"root"));
   }
   assertEquals("synthetic-timetable",activity.getPreferences(0).getString("courses",""));
   TaskPage tasks=getField(activity,"taskPage");tasks.settings();assertNotNull(((View)getField(activity,"root")).findViewWithTag("task-deletion-history"));activity.onBackPressed();
   ParcelPage parcels=getField(activity,"parcelPage");parcels.settings();AlertDialog menu=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNull(menu.getWindow().getDecorView().findViewWithTag("parcel-setting-4"));menu.dismiss();
   activity.onBackPressed();assertSame(general,getField(activity,"root"));
   general.findViewWithTag("data-management").performClick();controller.recreate();assertEquals("数据管理",((View)getField(controller.get(),"root")).getTag());controller.get().onBackPressed();assertEquals("设置",((View)getField(controller.get(),"root")).getTag());
  }
 }
 @Test public void clearingTasksRemovesAllAccountCachesAndCredentialsOnlyAfterConfirmation()throws Exception {
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();ChaoxingClient.Result fixture=new ChaoxingClient.Result();fixture.items.add(new ChaoxingClient.Work("合成课程","合成任务","未交",""));fixture.deleted.add(new ChaoxingClient.Work("合成课程","合成历史","未交",""));
   for(String account:new String[]{"","synthetic-one","synthetic-two"})new ChaoxingCache(activity,account).save(fixture);
   java.io.File cache=new java.io.File(activity.getFilesDir(),"chaoxing-work-"+"a".repeat(64)+".json");Files.write(new java.io.File(cache+".bak").toPath(),"backup".getBytes(java.nio.charset.StandardCharsets.UTF_8));Files.write(new java.io.File(cache+".new").toPath(),"pending".getBytes(java.nio.charset.StandardCharsets.UTF_8));
   java.io.File unrelated=new java.io.File(activity.getFilesDir(),"unrelated.json");Files.write(unrelated.toPath(),"keep".getBytes(java.nio.charset.StandardCharsets.UTF_8));activity.getPreferences(0).edit().putString("data-test","keep").commit();ParcelNotificationListener.settings(activity).edit().putString("data-test","keep").commit();
   activity.getSharedPreferences("chaoxing_account",0).edit().putString("credentials","synthetic-encrypted-value").putString("iv","synthetic-iv").commit();
   callInstanceMethod(activity,"prepareTasks");TaskPage page=getField(activity,"taskPage");tasksIdle(page);page.clearData();AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();dialog.getButton(-2).performClick();assertNotNull(new ChaoxingCache(activity,"").load());assertTrue(new ChaoxingAccount(activity).configured());
   page.clearData();org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().getButton(-1).performClick();tasksIdle(page);
   assertFalse(new ChaoxingAccount(activity).configured());for(java.io.File file:activity.getFilesDir().listFiles())assertFalse(file.getName().startsWith("chaoxing-work-"));assertTrue(((ChaoxingClient.Result)getField(page,"localTasks")).items.isEmpty());assertTrue(((ChaoxingClient.Result)getField(page,"snapshot")).items.isEmpty());
   assertEquals("keep",new String(Files.readAllBytes(unrelated.toPath()),java.nio.charset.StandardCharsets.UTF_8));assertEquals("keep",activity.getPreferences(0).getString("data-test",""));assertEquals("keep",ParcelNotificationListener.settings(activity).getString("data-test",""));
  }
 }
 @Test public void clearingParcelsWithoutAnOpenParcelSheetPreservesOtherModules()throws Exception {
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();androidx.work.WorkManager.initialize(activity,new androidx.work.Configuration.Builder().build());activity.getPreferences(0).edit().putString("data-test","keep").commit();new ChaoxingCache(activity,"").save(new ChaoxingClient.Result());
   ParcelNotificationListener.settings(activity).edit().putBoolean("recognition",true).putBoolean("reminders",true).putBoolean("backgroundTracking",true).putString("proxy","https://example.com").commit();
   ParcelRepository.IO.submit(()->{ParcelRecord parcel=new ParcelRecord();parcel.code="123456";parcel.station="合成驿站";new ParcelRepository(activity).save(parcel);}).get(10,TimeUnit.SECONDS);
   callInstanceMethod(activity,"prepareParcels");ParcelPage page=getField(activity,"parcelPage");assertNull(getField(page,"currentSheet"));page.clearData();org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().getButton(-1).performClick();parcelsIdle();
   assertTrue(ParcelRepository.IO.submit(()->ParcelDatabase.get(activity).parcels().all().isEmpty()).get(10,TimeUnit.SECONDS));assertFalse(ParcelNotificationListener.settings(activity).getBoolean("recognition",false));assertFalse(ParcelNotificationListener.settings(activity).getBoolean("backgroundTracking",false));assertFalse(ParcelNotificationListener.settings(activity).getBoolean("reminders",false));assertFalse(ParcelNotificationListener.settings(activity).getString("epoch","").isEmpty());
   assertEquals("keep",activity.getPreferences(0).getString("data-test",""));assertNotNull(new ChaoxingCache(activity,"").load());
  }
 }
 @Test public void restoringTaskHistoryKeepsTheDataManagementPage()throws Exception {
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();ChaoxingClient.Result fixture=new ChaoxingClient.Result();ChaoxingClient.Work removed=new ChaoxingClient.Work("合成分类","合成已删任务","未交","");removed.workKey="local:synthetic-history";removed.deletedAt=1234;fixture.deleted.add(removed);ChaoxingCache cache=new ChaoxingCache(activity,"");cache.save(fixture);
   callInstanceMethod(activity,"prepareTasks");TaskPage page=getField(activity,"taskPage");tasksIdle(page);callInstanceMethod(activity,"menu");((View)getField(activity,"root")).findViewWithTag("data-management").performClick();View data=getField(activity,"root");data.findViewWithTag("task-deletion-history").performClick();((View)getField(activity,"root")).findViewWithTag("task-restore").performClick();tasksIdle(page);
   assertSame(data,getField(activity,"root"));assertTrue(cache.load().deleted.isEmpty());assertEquals(1,cache.load().items.size());assertEquals(0,cache.load().items.get(0).deletedAt);
  }
 }
}
