package cn.courselens;

import android.content.*;
import java.util.*;

final class Timetables {
 private static SharedPreferences registry(Context context){return context.getSharedPreferences("timetables",Context.MODE_PRIVATE);}
 static String active(Context context){return registry(context).getString("active","");}
 static SharedPreferences preferences(Context context){return preferences(context,active(context));}
 static SharedPreferences preferences(Context context,String id){
  String name=MainActivity.class.getName(),prefix=context.getPackageName()+".";if(name.startsWith(prefix))name=name.substring(prefix.length());
  return context.getSharedPreferences(name+(id.isEmpty()?"":".timetable."+id),Context.MODE_PRIVATE);
 }
 static Map<String,String> names(Context context){
  SharedPreferences registry=registry(context);Map<String,String> names=new LinkedHashMap<>();names.put("",registry.getString("name.","我的课表"));
  for(String id:registry.getString("ids","").split("\n"))if(!id.isEmpty())names.put(id,registry.getString("name."+id,"课表"));return names;
 }
 static void select(Context context,String id){
  if(!names(context).containsKey(id))throw new IllegalArgumentException("课表不存在。");SharedPreferences prefs=preferences(context,id);ClassTimes times=ClassTimes.parse(prefs.getString("classTimes",""));times.validate(Course.parse(prefs.getString("courses","")));
  if(!registry(context).edit().putString("active",id).commit())throw new IllegalStateException("课表切换未保存，请重试。");
 }
 private static String checkedName(String name){name=name.trim();if(name.isEmpty()||name.length()>40)throw new IllegalArgumentException("课表名称请填写 1–40 个字。");return name;}
 static void rename(Context context,String id,String name){
  if(!names(context).containsKey(id))throw new IllegalArgumentException("课表不存在。");
  if(!registry(context).edit().putString("name."+id,checkedName(name)).commit())throw new IllegalStateException("课表名称未保存，请重试。");
 }
 static void add(Context context,String name,String school,ClassTimes times,String term){
  name=checkedName(name);
  String id=UUID.randomUUID().toString();
  if(!preferences(context,id).edit().putString("school",school).putString("classTimes",times.encode()).putString("term",term).putInt("week",1).commit())throw new IllegalStateException("新增课表未保存，请重试。");
  SharedPreferences registry=registry(context);String ids=registry.getString("ids","");
  if(!registry.edit().putString("ids",ids.isEmpty()?id:ids+"\n"+id).putString("name."+id,name).putString("active",id).commit())throw new IllegalStateException("新增课表未保存，请重试。");
 }
}
