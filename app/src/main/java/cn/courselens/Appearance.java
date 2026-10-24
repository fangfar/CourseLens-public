package cn.courselens;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;

/** App-wide light/dark choice; resources and programmatic surfaces share the same mode. */
final class Appearance {
 static final String PREFS="appSettings",KEY="themeMode";
 static final int SYSTEM=0,LIGHT=1,DARK=2;

 private Appearance(){}

 static int mode(Context context){
  try{int value=context.getSharedPreferences(PREFS,0).getInt(KEY,SYSTEM);return value==LIGHT||value==DARK?value:SYSTEM;}
  catch(RuntimeException e){return SYSTEM;}
 }

 static void mode(Context context,int value){
  context.getSharedPreferences(PREFS,0).edit().putInt(KEY,value==LIGHT||value==DARK?value:SYSTEM).apply();
 }

 static boolean dark(Context context){
  int mode=mode(context);
  if(mode==DARK)return true;
  if(mode==LIGHT)return false;
  return (context.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
 }

 static String label(Context context){
  int mode=mode(context);
  if(mode==DARK)return "深色";
  if(mode==LIGHT)return "浅色";
  return dark(context)?"跟随系统 · 当前深色":"跟随系统 · 当前浅色";
 }

 static Context wrap(Context base){
  int mode=mode(base);
  if(mode==SYSTEM)return base;
  Configuration configuration=new Configuration(base.getResources().getConfiguration());
  int night=mode==DARK?Configuration.UI_MODE_NIGHT_YES:Configuration.UI_MODE_NIGHT_NO;
  configuration.uiMode=(configuration.uiMode&~Configuration.UI_MODE_NIGHT_MASK)|night;
  return base.createConfigurationContext(configuration);
 }

 /** Enable edge-to-edge and let the app background paint beneath system bars. */
 static void edgeToEdge(Activity activity){
  Window window=activity.getWindow();
  androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window,false);
  systemBars(activity);
  if(Build.VERSION.SDK_INT>=29){
   try{window.setNavigationBarContrastEnforced(false);}catch(RuntimeException ignored){}
  }
 }

 /** Re-apply icon appearance after the page background changes. */
 static void systemBars(Activity activity){
  boolean light=!dark(activity);
  View decor=activity.getWindow().getDecorView();
  if(Build.VERSION.SDK_INT>=30){
   try{
    WindowInsetsController controller=activity.getWindow().getInsetsController();
    if(controller!=null){
     int mask=WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
     controller.setSystemBarsAppearance(light?mask:0,mask);
     return;
    }
   }catch(RuntimeException ignored){}
  }
  decor.setSystemUiVisibility(light?View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR:0);
 }
}
