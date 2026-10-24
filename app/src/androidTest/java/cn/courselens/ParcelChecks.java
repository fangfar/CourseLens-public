package cn.courselens;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.Bundle;
import androidx.room.Room;
import java.util.*;

final class ParcelChecks {
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 private static ParcelRecord parcel(String code,String station,String tracking){ParcelRecord p=new ParcelRecord();p.code=code;p.station=station;p.trackingNumber=tracking;return p;}
 static void run(Instrumentation instrumentation){
  Context context=instrumentation.getTargetContext();Bundle result=new Bundle();String name="parcel-check-"+UUID.randomUUID()+".db";ParcelDatabase db=null;int status=Activity.RESULT_CANCELED;
  try{
   check(context.getPackageName().endsWith(".qa"),"Parcel tests require isolated QA package");
   db=Room.databaseBuilder(context,ParcelDatabase.class,name).build();ParcelRepository repository=new ParcelRepository(db);
   ParcelRecord p=parcel("Ａ１—２—３４５６","合成东门驿站","SF00000000001");long id=repository.save(p);check(db.parcels().get(id).code.equals("A1-2-3456"),"normalize/save failed");
   try{repository.save(parcel("A1-2-3456","合成东门驿站",""));throw new AssertionError("duplicate allowed");}catch(IllegalArgumentException expected){}
   repository.save(parcel("A1-2-3456","合成西门驿站",""));check(db.parcels().all().size()==2,"same code different station merged");
   ParcelRecord shipping=parcel("","","YT00000000002");shipping.state=ParcelRecord.TRANSIT;repository.save(shipping);check(db.parcels().get(shipping.id).code.isEmpty(),"tracking mistaken for pickup code");
   ParcelRecord candidate=ParcelNotificationListener.record(ParcelRules.extract("合成快递取件码:445566").get(0),"合成通知",false);check(!candidate.confirmed,"low score auto confirmed");check(repository.ingest(candidate)&&!repository.ingest(candidate),"notification not deduped");
   repository.delete(candidate.id);ParcelRecord replay=ParcelNotificationListener.record(ParcelRules.extract("合成快递取件码:445566").get(0),"合成通知",false);check(!repository.ingest(replay),"deleted notification resurrected");
   db.close();db=Room.databaseBuilder(context,ParcelDatabase.class,name).build();repository=new ParcelRepository(db);check(db.parcels().get(id)!=null,"offline persistence failed");check(!repository.ingest(replay),"replay filter lost after reopening");
   PackageInfo info=context.getPackageManager().getPackageInfo(context.getPackageName(),PackageManager.GET_PERMISSIONS);List<String> permissions=Arrays.asList(info.requestedPermissions);check(!permissions.contains("android.permission.READ_SMS")&&!permissions.contains("android.permission.RECEIVE_SMS"),"SMS permission added");check(!permissions.contains("android.permission.SCHEDULE_EXACT_ALARM")&&!permissions.contains("android.permission.USE_EXACT_ALARM"),"unnecessary exact alarm permission added");
   ServiceInfo service=context.getPackageManager().getServiceInfo(new ComponentName(context,ParcelNotificationListener.class),0);check("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE".equals(service.permission),"notification service unprotected");
   repository.clear();check(db.parcels().all().isEmpty()&&repository.ingest(replay),"clear did not remove replay state");
   result.putString("stream","PASS: Parcel Room persistence, normalization, dedup/replay, confirmation and manifest boundaries; API "+android.os.Build.VERSION.SDK_INT+". No real OCR/notification-access/launcher validation.");status=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("stream","FAIL: "+e.getClass().getSimpleName()+": "+e.getMessage());}
  finally{if(db!=null)db.close();context.deleteDatabase(name);instrumentation.finish(status,result);}
 }
}
