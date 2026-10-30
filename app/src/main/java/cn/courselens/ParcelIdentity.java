package cn.courselens;

import android.app.Activity;
import android.content.*;
import android.net.Uri;

final class ParcelIdentity {
 private static final String[] PACKAGES={"com.taobao.taobao","com.cainiao.wireless","com.xunmeng.pinduoduo"};
 private static final String[] LINKS={
  "taobao://m.taobao.com/tbopen/index.html?h5Url="+Uri.encode("https://pages-fast.m.taobao.com/wow/z/uniapp/1011717/last-mile-fe/end-collect-platform/identity-code"),
  "cainiao://router/station_code","pinduoduo://com.xunmeng.pinduoduo/mdkd/package?tab=ID_CODE&entry_source=11"};
 static void open(Activity activity,int index){
  try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(LINKS[index])).setPackage(PACKAGES[index]));return;
  }catch(ActivityNotFoundException|SecurityException ignored){}
  try{Intent launch=activity.getPackageManager().getLaunchIntentForPackage(PACKAGES[index]);if(launch!=null)activity.startActivity(launch);}
  catch(ActivityNotFoundException|SecurityException ignored){}
 }
}
