package cn.courselens;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.widget.RemoteViews;
import java.util.*;

public class ParcelWidget extends AppWidgetProvider {
 @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){PendingResult pending=goAsync();ParcelRepository.IO.execute(()->{try{updateAll(context);}finally{pending.finish();}});}
 static void updateAll(Context context){
  AppWidgetManager manager=AppWidgetManager.getInstance(context);int[] ids=manager.getAppWidgetIds(new ComponentName(context,ParcelWidget.class));if(ids.length==0)return;
  RemoteViews views=new RemoteViews(context.getPackageName(),R.layout.parcel_widget);
  boolean reveal=ParcelNotificationListener.settings(context).getBoolean("widgetCodes",false);
  try{List<ParcelRecord> ready=new ArrayList<>();int pending=0;for(ParcelRecord p:ParcelDatabase.get(context).parcels().all()){if(!p.confirmed)pending++;else if(p.state==ParcelRecord.READY)ready.add(p);}
   views.setTextViewText(R.id.parcel_widget_count,ready.isEmpty()?"暂无待取快递":ready.size()+" 件待领取");
   StringBuilder lines=new StringBuilder();if(reveal)for(int i=0;i<Math.min(2,ready.size());i++){if(i>0)lines.append("\n");lines.append(ready.get(i).code).append("  ").append(ready.get(i).group());}
   if(!reveal&&!ready.isEmpty())lines.append("点击打开快递中心");if(lines.length()==0)lines.append(pending>0?pending+" 条识别结果待确认":"新的取件信息会显示在这里");views.setTextViewText(R.id.parcel_widget_codes,lines.toString());
  }catch(RuntimeException e){views.setTextViewText(R.id.parcel_widget_count,"暂时无法读取");views.setTextViewText(R.id.parcel_widget_codes,"点击打开快递中心重试");}
  PendingIntent open=PendingIntent.getActivity(context,0,new Intent(context,ParcelActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);views.setOnClickPendingIntent(R.id.parcel_widget_root,open);manager.updateAppWidget(ids,views);
 }
}
