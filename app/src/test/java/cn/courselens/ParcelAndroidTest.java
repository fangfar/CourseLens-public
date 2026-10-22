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
import java.util.concurrent.atomic.AtomicInteger;
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
 @Test public void queriesPreserveDataAndRejectLateResponses(){
  ParcelRecord p=record("","","SF00000000001");p.state=ParcelRecord.TRANSIT;repository.save(p);AtomicInteger calls=new AtomicInteger();
  TrackingProvider fake=(number,carrier)->{calls.incrementAndGet();return new TrackingProvider.Result(TrackingProvider.Error.NONE,"IN_TRANSIT",Collections.singletonList(new TrackingProvider.Event("合成时间","合成物流")),0);};
  ParcelTracking.query(context,p.id,fake,db);assertEquals(1,calls.get());ParcelTracking.query(context,p.id,fake,db);assertEquals(1,calls.get());assertTrue(db.parcels().get(p.id).trackingEvents.contains("合成物流"));
  ParcelRecord fresh=db.parcels().get(p.id);fresh.lastQueryAt=fresh.retryAt=0;db.parcels().update(fresh);
  ParcelTracking.query(context,p.id,(number,carrier)->TrackingProvider.Result.failure(TrackingProvider.Error.NETWORK,60000),db);assertTrue(db.parcels().get(p.id).trackingEvents.contains("合成物流"));
  fresh=db.parcels().get(p.id);fresh.lastQueryAt=fresh.retryAt=0;db.parcels().update(fresh);
  ParcelTracking.query(context,p.id,(number,carrier)->{ParcelRecord edited=db.parcels().get(p.id);edited.trackingNumber="SF00000000002";repository.save(edited);return new TrackingProvider.Result(TrackingProvider.Error.NONE,"DELIVERED",Collections.emptyList(),0);},db);
  assertEquals("IN_TRANSIT",db.parcels().get(p.id).trackingState);
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
 @Test public void rateLimitAppliesAcrossRecords(){
  ParcelRecord a=record("","","SF00000000001"),b=record("","","SF00000000002");a.state=b.state=ParcelRecord.TRANSIT;repository.save(a);repository.save(b);AtomicInteger calls=new AtomicInteger();TrackingProvider limited=(number,carrier)->{calls.incrementAndGet();return TrackingProvider.Result.failure(TrackingProvider.Error.RATE_LIMIT,900000);};
  ParcelTracking.query(context,a.id,limited,db);ParcelTracking.query(context,b.id,limited,db);assertEquals(1,calls.get());assertTrue(db.parcels().get(a.id).retryAt>System.currentTimeMillis());
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
   android.view.View add=described(root,"添加快递");assertNotNull(add);assertTrue(add.getRight()<=((android.view.View)add.getParent()).getWidth());add.performClick();
   AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(dialog);assertTrue(dialog.isShowing());assertNotNull(dialog.getWindow().getDecorView().findViewWithTag("parcel-editor-code"));
   assertNotEquals(android.graphics.Color.WHITE,activity.getColor(R.color.parcel_card));
  }
  ParcelRepository.IO.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idle();
 }
 private android.view.View described(android.view.View view,String label){if(view.getContentDescription()!=null&&label.contentEquals(view.getContentDescription()))return view;if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++){android.view.View found=described(group.getChildAt(i),label);if(found!=null)return found;}}return null;}
}
