package cn.courselens;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import androidx.core.app.NotificationManagerCompat;

final class ParcelNotifications {
 static final String CHANNEL="parcel_updates";
 static boolean post(Context context,long id,String title,String message){
  if(!ParcelNotificationListener.settings(context).getBoolean("reminders",false))return false;
  if(android.os.Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false;
  if(!NotificationManagerCompat.from(context).areNotificationsEnabled())return false;
  NotificationManager manager=context.getSystemService(NotificationManager.class);
  manager.createNotificationChannel(new NotificationChannel(CHANNEL,"快递提醒",NotificationManager.IMPORTANCE_DEFAULT));
  if(manager.getNotificationChannel(CHANNEL).getImportance()==NotificationManager.IMPORTANCE_NONE)return false;
  PendingIntent open=PendingIntent.getActivity(context,0,new Intent(context,ParcelActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification n=new Notification.Builder(context,CHANNEL).setSmallIcon(R.drawable.ic_parcel).setContentTitle(title).setContentText(message).setVisibility(Notification.VISIBILITY_PRIVATE).setContentIntent(open).setAutoCancel(true).build();
  try{manager.notify("parcel:"+id,1,n);return true;}catch(SecurityException e){return false;}
 }
 static void newParcel(Context context,ParcelRecord p){post(context,p.id,p.confirmed?"新的待取快递":"快递信息待确认","打开快递中心查看，信息仅保存在本机");}
 static void cancel(Context context,long id){context.getSystemService(NotificationManager.class).cancel("parcel:"+id,1);}
 static void cancelAll(Context context){NotificationManager manager=context.getSystemService(NotificationManager.class);for(android.service.notification.StatusBarNotification n:manager.getActiveNotifications())if(n.getTag()!=null&&n.getTag().startsWith("parcel:"))manager.cancel(n.getTag(),n.getId());}
}
