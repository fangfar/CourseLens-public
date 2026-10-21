package cn.courselens;

import java.util.Calendar;
import java.util.TimeZone;

public final class TaskRefreshTest {
 private static long at(int day,int hour,int minute){Calendar date=Calendar.getInstance();date.clear();date.set(2026,Calendar.OCTOBER,day,hour,minute);return date.getTimeInMillis();}
 public static void main(String[] args){
  TimeZone original=TimeZone.getDefault();try{
   TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));long noon=at(21,12,0);
   assert TaskRefresh.delay(0,60,noon)==0;
   assert TaskRefresh.delay(at(20,23,59),1440,at(21,0,1))==0 : "First opening must refresh across midnight";
   assert TaskRefresh.delay(noon,60,at(21,12,30))==30*60000L;
   assert TaskRefresh.delay(noon,60,at(21,13,0))==0;
   assert TaskRefresh.delay(noon,0,at(21,13,0))==11*3600000L : "Daily-only refresh must wait for midnight";
   assert TaskRefresh.delay(at(21,23,50),60,at(21,23,55))==5*60000L;
   assert TaskRefresh.delay(noon,60,at(21,11,59))==0 : "Clock rollback must not stall refresh";
   TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));Calendar dst=Calendar.getInstance();dst.clear();dst.set(2026,Calendar.MARCH,8,0,30);long start=dst.getTimeInMillis();
   assert TaskRefresh.delay(start,0,start)==(22*60+30)*60000L : "DST day must use local midnight";
  }finally{TimeZone.setDefault(original);}System.out.println("PASS: task refresh interval, daily opening, midnight, clock rollback and DST");
 }
}
