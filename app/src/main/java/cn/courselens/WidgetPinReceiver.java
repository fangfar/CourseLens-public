package cn.courselens;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.*;

/** Receives the launcher's explicit, one-shot success callback. */
public final class WidgetPinReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){
  android.content.SharedPreferences prefs=context.getSharedPreferences("widget_add",Context.MODE_PRIVATE);
  String token=intent.getStringExtra("requestToken");
  if(token==null||!token.equals(prefs.getString("requestToken","")))return;
  int id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
  if(id==AppWidgetManager.INVALID_APPWIDGET_ID)return;
  AppWidgetProviderInfo info=AppWidgetManager.getInstance(context).getAppWidgetInfo(id);
  if(info==null||!new ComponentName(context,CourseWidget.class).equals(info.provider))return;
  prefs.edit().putString("confirmedToken",token).apply();
 }
}
