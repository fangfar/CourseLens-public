package cn.courselens;

import android.content.Context;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.*;

final class ParcelRepository {
 static final ExecutorService IO=Executors.newSingleThreadExecutor();
 static final long REPLAY_WINDOW=14L*24*60*60*1000;
 private final ParcelDatabase db;
 private final Context context;
 ParcelRepository(Context context){this.context=context.getApplicationContext();db=ParcelDatabase.get(context);}
 ParcelRepository(ParcelDatabase database){db=database;context=null;}
 private void changed(){if(context!=null)IO.execute(()->ParcelWidget.updateAll(context));}
 static String normalize(String text){return java.text.Normalizer.normalize(text,java.text.Normalizer.Form.NFKC).trim().replaceAll("[‐‑‒–—−]","-").toUpperCase(Locale.ROOT);}
 static void validate(ParcelRecord p){
  p.code=normalize(p.code);p.trackingNumber=normalize(p.trackingNumber);p.station=p.station.trim();p.carrier=p.carrier.trim();
  if(!p.code.isEmpty()&&!p.code.matches("[A-Z0-9]+(?:-[A-Z0-9]+)*")||p.code.length()>24)throw new IllegalArgumentException("取件码限 1–24 位字母、数字和横杠");
  if(!p.trackingNumber.isEmpty()&&!p.trackingNumber.matches("[A-Z0-9]{6,40}"))throw new IllegalArgumentException("运单号限 6–40 位字母和数字");
  if(p.code.isEmpty()&&p.trackingNumber.isEmpty())throw new IllegalArgumentException("请填写取件码或运单号");
  if(p.station.length()>160||p.carrier.length()>40)throw new IllegalArgumentException("地址最多 160 字，快递公司最多 40 字");
  if(p.state<0||p.state>2||p.confidence<0||p.confidence>100)throw new IllegalArgumentException("状态无效");
  if(p.state==ParcelRecord.READY&&p.code.isEmpty())throw new IllegalArgumentException("待取件记录请填写取件码，或选择运输中");
 }
 long save(ParcelRecord p){
  validate(p);
  long id=db.runInTransaction(()->{
   long now=System.currentTimeMillis();
   if(db.parcels().duplicate(p.id,p.code,p.station,p.trackingNumber,now-REPLAY_WINDOW)!=null)throw new IllegalArgumentException("已有相同运单或同地址取件码，请编辑已有记录");
   if(p.id==0){p.createdAt=now;p.updatedAt=now;p.id=db.parcels().insert(p);}
   else {ParcelRecord old=db.parcels().get(p.id);if(old==null)throw new IllegalArgumentException("记录已删除，请返回列表");
    if(old.trackingNumber.equals(p.trackingNumber)&&old.carrier.equals(p.carrier)){p.trackingState=old.trackingState;p.trackingEvents=old.trackingEvents;p.lastQueryAt=old.lastQueryAt;p.retryAt=old.retryAt;p.trackingError=old.trackingError;}
    p.remindAt=p.state==ParcelRecord.DONE?0:old.remindAt;p.remindedAt=old.remindedAt;p.createdAt=old.createdAt;p.updatedAt=now;if(db.parcels().update(p)!=1)throw new IllegalStateException("记录保存失败");}
   return p.id;
  });if(context!=null&&p.state==ParcelRecord.DONE)ParcelNotifications.cancel(context,p.id);changed();return id;
 }
 static String fingerprint(ParcelRecord p){
  try{byte[] hash=MessageDigest.getInstance("SHA-256").digest((p.code+"\n"+p.station+"\n"+p.trackingNumber).getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:hash)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
  catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
 boolean ingest(ParcelRecord p){
  validate(p);boolean added=db.runInTransaction(()->{
   long now=System.currentTimeMillis();db.parcels().prune(now);String key=fingerprint(p);
   if(db.parcels().seen(key,now)>0)return false;
   ParcelDatabase.Seen seen=new ParcelDatabase.Seen();seen.fingerprint=key;seen.expiresAt=now+REPLAY_WINDOW;db.parcels().remember(seen);
   ParcelRecord existing=db.parcels().duplicate(0,p.code,p.station,p.trackingNumber,now-REPLAY_WINDOW);
   if(existing!=null){
    if(existing.state==ParcelRecord.DONE||p.trackingNumber.isEmpty()||!p.trackingNumber.equals(existing.trackingNumber)||!existing.code.isEmpty())return false;
    existing.code=p.code;existing.station=p.station.isEmpty()?existing.station:p.station;existing.state=ParcelRecord.READY;existing.confirmed=p.confirmed;existing.source=p.source;existing.rule=p.rule;existing.confidence=p.confidence;existing.updatedAt=now;db.parcels().update(existing);p.id=existing.id;return true;
   }
   p.createdAt=p.updatedAt=now;p.id=db.parcels().insert(p);return true;
  });if(added)changed();return added;
 }
 void delete(long id){db.runInTransaction(()->{ParcelRecord p=db.parcels().get(id);if(p==null)return;ParcelDatabase.Seen seen=new ParcelDatabase.Seen();seen.fingerprint=fingerprint(p);seen.expiresAt=System.currentTimeMillis()+REPLAY_WINDOW;db.parcels().remember(seen);db.parcels().delete(id);});if(context!=null)ParcelNotifications.cancel(context,id);changed();}
 void restoreDeleted(ParcelRecord p){
  validate(p);db.runInTransaction(()->{if(db.parcels().get(p.id)!=null||db.parcels().duplicate(p.id,p.code,p.station,p.trackingNumber,System.currentTimeMillis()-REPLAY_WINDOW)!=null)throw new IllegalArgumentException("已有相同快递");p.updatedAt=Math.max(System.currentTimeMillis(),p.updatedAt+1);db.parcels().insert(p);});changed();
 }
 void undoComplete(long id,long changedAt,int previous,long remindAt,long remindedAt){
  db.runInTransaction(()->{ParcelRecord p=db.parcels().get(id);if(p==null||p.state!=ParcelRecord.DONE||p.updatedAt!=changedAt)throw new IllegalArgumentException("记录已变更");p.state=previous;p.remindAt=remindAt;p.remindedAt=remindedAt;p.updatedAt=Math.max(System.currentTimeMillis(),changedAt+1);validate(p);db.parcels().update(p);});changed();
 }
 void clear(){db.clearAllTables();changed();}
}
