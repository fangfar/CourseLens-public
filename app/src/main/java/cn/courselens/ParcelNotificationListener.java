package cn.courselens;

import android.app.Notification;
import android.content.*;
import android.os.Bundle;
import android.service.notification.*;
import java.util.*;

public class ParcelNotificationListener extends NotificationListenerService {
 static final String PREFS="parcelSettings";
 static final String[] PACKAGES={"com.cainiao.wireless","com.taobao.taobao","com.xunmeng.pinduoduo","com.tencent.mm","com.tencent.mobileqq"};
 static SharedPreferences settings(Context context){return context.getSharedPreferences(PREFS,0);}
 static String smsPackage(Context context){String sms=android.provider.Telephony.Sms.getDefaultSmsPackage(context);if(sms!=null)return sms;try{context.getPackageManager().getPackageInfo("com.android.mms",0);return "com.android.mms";}catch(android.content.pm.PackageManager.NameNotFoundException e){return null;}}
 static boolean accepts(Context context,String pkg){return pkg.equals(smsPackage(context))||Arrays.asList(PACKAGES).contains(pkg);}
 @Override public void onNotificationPosted(StatusBarNotification sbn){
  if(sbn==null||!settings(this).getBoolean("recognition",false)||!accepts(this,sbn.getPackageName()))return;
  Notification n=sbn.getNotification();if(n==null||(n.flags&(Notification.FLAG_GROUP_SUMMARY|Notification.FLAG_ONGOING_EVENT))!=0)return;
  Bundle extras=n.extras;if(extras==null)return;
  CharSequence title,body;try{title=extras.getCharSequence(Notification.EXTRA_TITLE);body=extras.getCharSequence(Notification.EXTRA_BIG_TEXT);if(body==null)body=extras.getCharSequence(Notification.EXTRA_TEXT);}catch(RuntimeException e){return;}
  if((title==null?0:title.length())+(body==null?0:body.length())>ParcelRules.MAX_TEXT)return;
  String text=(title==null?"":title.toString())+"\n"+(body==null?"":body.toString());
  String source="通知 · "+sbn.getPackageName();String epoch=settings(this).getString("epoch","");Context context=getApplicationContext();
  ParcelRepository.IO.execute(()->{
   if(!settings(context).getBoolean("recognition",false)||!epoch.equals(settings(context).getString("epoch",""))||!accepts(context,sbn.getPackageName()))return;
   try{for(ParcelRules.Candidate candidate:ParcelRules.extract(text)){
    if(!settings(context).getBoolean("recognition",false)||!epoch.equals(settings(context).getString("epoch","")))break;
    boolean official=sbn.getPackageName().equals(PACKAGES[0])||sbn.getPackageName().equals(PACKAGES[1])||sbn.getPackageName().equals(PACKAGES[2]);ParcelRecord p=record(candidate,source,!official);if(new ParcelRepository(context).ingest(p))ParcelNotifications.newParcel(context,p);
   }}catch(RuntimeException ignored){settings(context).edit().putBoolean("recognitionError",true).apply();}
  });
 }
 static ParcelRecord record(ParcelRules.Candidate c,String source,boolean confirmAlways){ParcelRecord p=new ParcelRecord();p.code=c.code;p.station=c.station;p.trackingNumber=c.tracking;p.confidence=confirmAlways?Math.min(85,c.confidence):c.confidence;p.confirmed=!confirmAlways&&c.autoSave();p.rule=c.rule;p.source=source;return p;}
}
