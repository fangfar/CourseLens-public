package cn.courselens;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.*;
import java.util.concurrent.TimeUnit;

public class ParcelWorker extends Worker {
 public ParcelWorker(@NonNull Context context,@NonNull WorkerParameters params){super(context,params);}
 static void schedule(Context context){
  WorkManager manager=WorkManager.getInstance(context);android.content.SharedPreferences settings=ParcelNotificationListener.settings(context);
  if(settings.getBoolean("reminders",false))manager.enqueueUniquePeriodicWork("parcel-reminders",ExistingPeriodicWorkPolicy.KEEP,new PeriodicWorkRequest.Builder(ParcelWorker.class,15,TimeUnit.MINUTES).build());else manager.cancelUniqueWork("parcel-reminders");
  manager.cancelUniqueWork("parcel-tracking");
  settings.edit().remove("proxy").remove("backgroundTracking").remove("trackingRetryAt").apply();
 }
 @NonNull @Override public Result doWork(){
  Context context=getApplicationContext();android.content.SharedPreferences settings=ParcelNotificationListener.settings(context);
  try{
   // Old queued tracking work must finish without querying or sending reminders.
   if(getInputData().getBoolean("tracking",false))return Result.success();
   if(settings.getBoolean("reminders",false)){
    ParcelDatabase db=ParcelDatabase.get(context);for(ParcelRecord record:db.parcels().all()){
     if(isStopped()||!settings.getBoolean("reminders",false))break;
     db.runInTransaction(()->{ParcelRecord p=db.parcels().get(record.id);if(p!=null&&p.confirmed&&p.state==ParcelRecord.READY&&p.remindAt>0&&p.remindAt<=System.currentTimeMillis()&&p.remindedAt<p.remindAt&&ParcelNotifications.post(context,p.id,"还有快递待领取","这是你设置的未领取提醒")){p.remindedAt=System.currentTimeMillis();db.parcels().update(p);}});
    }
   }
   return Result.success();
  }catch(RuntimeException e){return Result.retry();}
 }
}
