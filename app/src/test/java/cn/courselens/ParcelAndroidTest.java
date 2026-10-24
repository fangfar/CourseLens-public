package cn.courselens;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import androidx.room.Room;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import java.util.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** Android framework simulations; real notification access, Doze and OEM launchers need device QA. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk={33,34,35,36})
public class ParcelAndroidTest {
 private Context context;
 private ParcelDatabase db;
 private ParcelRepository repository;
 @Before public void setup(){context=RuntimeEnvironment.getApplication();ParcelNotificationListener.settings(context).edit().clear().commit();db=Room.inMemoryDatabaseBuilder(context,ParcelDatabase.class).allowMainThreadQueries().build();repository=new ParcelRepository(db);}
 @After public void close(){db.close();}
 private ParcelRecord record(String code,String station,String tracking){ParcelRecord p=new ParcelRecord();p.code=code;p.station=station;p.trackingNumber=tracking;return p;}
 @Test public void independentFieldsAndDedup(){
  ParcelRecord p=record("Ａ１—２—３４５６","合成东门驿站","");repository.save(p);assertEquals("A1-2-3456",db.parcels().get(p.id).code);
  try{repository.save(record("A1-2-3456","合成东门驿站",""));fail();}catch(IllegalArgumentException expected){}
  repository.save(record("A1-2-3456","合成西门驿站",""));
  ParcelRecord shipping=record("","","SF00000000001");shipping.state=ParcelRecord.TRANSIT;repository.save(shipping);assertEquals("",db.parcels().get(shipping.id).code);
  ParcelRecord arrival=record("123456","合成驿站","SF00000000001");arrival.confirmed=false;assertTrue(repository.ingest(arrival));assertEquals(shipping.id,arrival.id);assertFalse(db.parcels().get(shipping.id).confirmed);assertEquals(3,db.parcels().all().size());
  ParcelRecord confirmed=db.parcels().get(shipping.id);confirmed.confirmed=true;repository.save(confirmed);assertTrue(db.parcels().get(shipping.id).confirmed);
 }
 @Test public void deletedAndCompletedNotificationsDoNotResurrect(){
  ParcelRecord p=record("123456","合成驿站","");assertTrue(repository.ingest(p));repository.delete(p.id);assertFalse(repository.ingest(record("123456","合成驿站","")));
  ParcelRecord done=record("789012","另一合成驿站","YT00000000001");done.state=ParcelRecord.DONE;repository.save(done);assertFalse(repository.ingest(record("789012","另一合成驿站","YT00000000001")));
  repository.clear();assertTrue(db.parcels().all().isEmpty());assertTrue(repository.ingest(record("123456","合成驿站","")));
 }
 @Test public void undoPreservesRecordAndReplayProtection(){
  ParcelRecord p=record("123456","合成撤销驿站","YT00000000009");p.trackingEvents="[]";repository.save(p);long created=p.createdAt;
  repository.delete(p.id);assertNull(db.parcels().get(p.id));repository.restoreDeleted(p);
  assertEquals(created,db.parcels().get(p.id).createdAt);assertEquals("[]",db.parcels().get(p.id).trackingEvents);assertFalse(repository.ingest(record(p.code,p.station,p.trackingNumber)));
  try{repository.restoreDeleted(p);fail("undo must not overwrite an existing record");}catch(IllegalArgumentException expected){}
  p.state=ParcelRecord.DONE;repository.save(p);repository.undoComplete(p.id,p.updatedAt,ParcelRecord.READY,1234,567);assertEquals(ParcelRecord.READY,db.parcels().get(p.id).state);assertEquals(1234,db.parcels().get(p.id).remindAt);
  try{repository.undoComplete(p.id,p.updatedAt,ParcelRecord.READY,1234,567);fail("stale undo must not change a record");}catch(IllegalArgumentException expected){}
 }
 @Test public void notificationPermissionAndSwitchAreIndependent(){
  shadowOf((Application)context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS);
  assertFalse(ParcelNotifications.post(context,1,"合成提醒","不含取件码"));
  ParcelNotificationListener.settings(context).edit().putBoolean("reminders",true).commit();assertFalse(ParcelNotifications.post(context,1,"合成提醒","不含取件码"));
  shadowOf((Application)context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);assertTrue(ParcelNotifications.post(context,1,"合成提醒","不含取件码"));
  assertEquals(1,shadowOf(context.getSystemService(NotificationManager.class)).getAllNotifications().size());
  ParcelNotifications.post(context,1,"合成状态变化","不含取件码");assertEquals(1,shadowOf(context.getSystemService(NotificationManager.class)).getAllNotifications().size());
  ParcelNotifications.cancel(context,1);assertEquals(0,shadowOf(context.getSystemService(NotificationManager.class)).getAllNotifications().size());
  assertFalse(ParcelNotificationListener.settings(context).getBoolean("recognition",false));
 }
 @Test public void removingOnlineTrackingCancelsLegacyWorkAndKeepsReminders()throws Exception{
  android.content.SharedPreferences settings=ParcelNotificationListener.settings(context);
  settings.edit().putString("proxy","https://example.com/track").putBoolean("backgroundTracking",true).putLong("trackingRetryAt",1234).putBoolean("reminders",true).putBoolean("recognition",true).commit();
  Object previousDefault=org.robolectric.util.ReflectionHelpers.getStaticField(androidx.work.impl.WorkManagerImpl.class,"sDefaultInstance"),previousDelegate=org.robolectric.util.ReflectionHelpers.getStaticField(androidx.work.impl.WorkManagerImpl.class,"sDelegatedInstance");
  if(previousDefault==null&&previousDelegate==null)androidx.work.WorkManager.initialize(context,new androidx.work.Configuration.Builder().build());
  androidx.work.WorkManager manager=androidx.work.WorkManager.getInstance(context);
  androidx.work.PeriodicWorkRequest legacy=new androidx.work.PeriodicWorkRequest.Builder(ParcelWorker.class,6,java.util.concurrent.TimeUnit.HOURS).setInputData(new androidx.work.Data.Builder().putBoolean("tracking",true).build()).build();
  try{
  manager.enqueueUniquePeriodicWork("parcel-tracking",androidx.work.ExistingPeriodicWorkPolicy.KEEP,legacy).getResult().get(10,java.util.concurrent.TimeUnit.SECONDS);
  ParcelWorker.schedule(context);
  assertEquals(androidx.work.WorkInfo.State.CANCELLED,manager.getWorkInfoById(legacy.getId()).get(10,java.util.concurrent.TimeUnit.SECONDS).getState());
  assertFalse(settings.contains("proxy"));assertFalse(settings.contains("backgroundTracking"));assertFalse(settings.contains("trackingRetryAt"));assertTrue(settings.getBoolean("recognition",false));assertTrue(settings.getBoolean("reminders",false));
  assertFalse(manager.getWorkInfosForUniqueWork("parcel-reminders").get(10,java.util.concurrent.TimeUnit.SECONDS).isEmpty());
  }finally{manager.cancelAllWork().getResult().get(10,java.util.concurrent.TimeUnit.SECONDS);org.robolectric.util.ReflectionHelpers.setStaticField(androidx.work.impl.WorkManagerImpl.class,"sDefaultInstance",previousDefault);org.robolectric.util.ReflectionHelpers.setStaticField(androidx.work.impl.WorkManagerImpl.class,"sDelegatedInstance",previousDelegate);}
 }
 @Test public void manifestHasOnlyRequiredPermissions()throws Exception {
  String[] permissions=context.getPackageManager().getPackageInfo(context.getPackageName(),PackageManager.GET_PERMISSIONS).requestedPermissions;
  assertFalse(Arrays.asList(permissions).contains("android.permission.READ_SMS"));assertFalse(Arrays.asList(permissions).contains("android.permission.RECEIVE_SMS"));assertFalse(Arrays.asList(permissions).contains("android.permission.SCHEDULE_EXACT_ALARM"));
  assertEquals("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",context.getPackageManager().getServiceInfo(new ComponentName(context,ParcelNotificationListener.class),0).permission);
 }
 @Test public void narrowDarkLargeFontCanOpenEditor()throws Exception {
  RuntimeEnvironment.setQualifiers("w320dp-h640dp-night");RuntimeEnvironment.setFontScale(2f);
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
   ParcelActivity activity=controller.get();android.view.View root=activity.findViewById(android.R.id.content);int width=Math.round(320*activity.getResources().getDisplayMetrics().density),height=Math.round(640*activity.getResources().getDisplayMetrics().density);root.measure(android.view.View.MeasureSpec.makeMeasureSpec(width,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(height,android.view.View.MeasureSpec.EXACTLY));root.layout(0,0,width,height);
   android.view.View menu=root.findViewWithTag("quick-menu-button");assertNotNull(menu);assertTrue(menu.getRight()<=((android.view.View)menu.getParent()).getWidth());menu.performClick();
   AlertDialog actions=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(actions);android.view.View add=actions.getWindow().getDecorView().findViewWithTag("parcel-add");assertNotNull(add);add.performClick();
   AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(dialog);assertTrue(dialog.isShowing());assertNotNull(dialog.getWindow().getDecorView().findViewWithTag("parcel-editor-code"));
   assertNotEquals(android.graphics.Color.WHITE,activity.getColor(R.color.parcel_card));
  }
  ParcelRepository.IO.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idle();
 }
}
