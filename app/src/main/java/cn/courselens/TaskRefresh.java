package cn.courselens;

import java.util.Calendar;

/** Foreground refresh policy, including the first opening of each local calendar day. */
final class TaskRefresh {
 static final int MAX_MINUTES=10080;
 static long delay(long lastAttempt,int minutes,long now){
  Calendar today=Calendar.getInstance();today.setTimeInMillis(now);Calendar previous=Calendar.getInstance();previous.setTimeInMillis(lastAttempt);
  if(lastAttempt<=0||lastAttempt>now||today.get(Calendar.ERA)!=previous.get(Calendar.ERA)||today.get(Calendar.YEAR)!=previous.get(Calendar.YEAR)||today.get(Calendar.DAY_OF_YEAR)!=previous.get(Calendar.DAY_OF_YEAR))return 0;
  today.add(Calendar.DAY_OF_YEAR,1);today.set(Calendar.HOUR_OF_DAY,0);today.set(Calendar.MINUTE,0);today.set(Calendar.SECOND,0);today.set(Calendar.MILLISECOND,0);
  long midnight=today.getTimeInMillis()-now;
  return minutes==0?midnight:Math.max(0,Math.min(midnight,minutes*60000L-(now-lastAttempt)));
 }
}
