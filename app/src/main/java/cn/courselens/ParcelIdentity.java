package cn.courselens;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.widget.Toast;

final class ParcelIdentity {
 private static final String[] PACKAGES={"com.taobao.taobao","com.cainiao.wireless","com.xunmeng.pinduoduo"};
 private static final String[] LINKS={
  "taobao://m.taobao.com/tbopen/index.html?h5Url="+Uri.encode("https://pages-fast.m.taobao.com/wow/z/uniapp/1011717/last-mile-fe/end-collect-platform/identity-code"),
  "cainiao://router/station_code","pinduoduo://com.xunmeng.pinduoduo/mdkd/package"};
 static void open(Activity activity,int index){
  try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(LINKS[index])).setPackage(PACKAGES[index]));
   Toast.makeText(activity,"已请求打开官方入口；若未直达，请在官方 App 内查找身份码",Toast.LENGTH_LONG).show();return;
  }catch(ActivityNotFoundException|SecurityException ignored){}
  try{Intent launch=activity.getPackageManager().getLaunchIntentForPackage(PACKAGES[index]);if(launch!=null){activity.startActivity(launch);Toast.makeText(activity,"已打开官方 App，请在应用内查找身份码",Toast.LENGTH_LONG).show();return;}}
  catch(ActivityNotFoundException|SecurityException ignored){}
  Toast.makeText(activity,"未安装对应 App，或系统阻止了跳转，请手动打开官方 App",Toast.LENGTH_LONG).show();
 }
}
