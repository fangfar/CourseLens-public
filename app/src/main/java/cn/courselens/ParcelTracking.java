package cn.courselens;

import android.content.Context;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

final class ParcelTracking {
 static final ExecutorService NETWORK=Executors.newFixedThreadPool(2);
 private static final Set<Long> running=ConcurrentHashMap.newKeySet();
 static String query(Context context,long id){
  String endpoint=ParcelNotificationListener.settings(context).getString("proxy","");if(endpoint.isEmpty())return "请先在快递设置中配置物流代理；当前没有连接真实物流服务";
  try{return query(context,id,new ProxyTrackingProvider(endpoint));}catch(IllegalArgumentException e){return e.getMessage();}
 }
 static String query(Context context,long id,TrackingProvider provider){
  String result=query(context,id,provider,ParcelDatabase.get(context));ParcelRepository.IO.execute(()->ParcelWidget.updateAll(context));return result;
 }
 static String query(Context context,long id,TrackingProvider provider,ParcelDatabase db){
  if(!running.add(id))return "正在查询，请勿重复操作";
  try{
   long now=System.currentTimeMillis();String epoch=ParcelNotificationListener.settings(context).getString("epoch","");
   if(ParcelNotificationListener.settings(context).getLong("trackingRetryAt",0)>now)return "物流服务暂时受限，请稍后查询";
   ParcelRecord requested=db.runInTransaction(()->{ParcelRecord p=db.parcels().get(id);if(p==null||!p.confirmed||p.state==ParcelRecord.DONE||p.trackingNumber.isEmpty()||p.retryAt>now||now-p.lastQueryAt<300000)return null;p.lastQueryAt=now;db.parcels().update(p);return p;});
   if(requested==null)return "记录不可查询，或尚在查询间隔内（至少 5 分钟）";
   String endpoint=ParcelNotificationListener.settings(context).getString("proxy","");TrackingProvider.Result result=provider.query(requested.trackingNumber,requested.carrier);
   if(!epoch.equals(ParcelNotificationListener.settings(context).getString("epoch",""))||!endpoint.equals(ParcelNotificationListener.settings(context).getString("proxy","")))return "设置已更改，已忽略本次结果";
   if(result.error==TrackingProvider.Error.RATE_LIMIT||result.error==TrackingProvider.Error.UNAVAILABLE)ParcelNotificationListener.settings(context).edit().putLong("trackingRetryAt",now+Math.max(60000,Math.min(86400000,result.retryAfterMillis))).apply();
   return db.runInTransaction(()->{
    ParcelRecord p=db.parcels().get(id);if(p==null||p.state==ParcelRecord.DONE||!p.trackingNumber.equals(requested.trackingNumber)||!p.carrier.equals(requested.carrier)||p.updatedAt!=requested.updatedAt)return "记录已变更，已忽略旧查询结果";
    p.retryAt=now+Math.max(300000,Math.min(86400000,result.retryAfterMillis));p.trackingError=result.error==TrackingProvider.Error.NONE?"":label(result.error);
    boolean changed=result.error==TrackingProvider.Error.NONE&&!p.trackingState.isEmpty()&&!p.trackingState.equals(result.state);
    if(result.error==TrackingProvider.Error.NONE){p.trackingState=result.state;JSONArray events=new JSONArray();try{for(TrackingProvider.Event event:result.events)events.put(new JSONObject().put("time",event.time).put("description",event.description));}catch(JSONException e){throw new IllegalStateException(e);}p.trackingEvents=events.toString();if(result.state.equals("READY")&&!p.code.isEmpty())p.state=ParcelRecord.READY;}
    db.parcels().update(p);if(changed)ParcelNotifications.post(context,p.id,"物流状态有更新","打开快递中心查看最新物流记录");
    return result.error==TrackingProvider.Error.NONE?"物流已更新；物流签收不自动视为本人已取件":label(result.error);
   });
  }catch(RuntimeException e){return "查询未完成，已有快递和物流记录仍保留";}finally{running.remove(id);}
 }
 static String label(TrackingProvider.Error error){switch(error){case EMPTY:return "暂未查到物流，稍后再试";case RATE_LIMIT:return "物流服务频率受限，请稍后再试";case NETWORK:return "网络连接失败，已有物流记录仍保留";case UNAVAILABLE:return "物流服务不可用，请检查代理服务";case NOT_CONFIGURED:return "尚未配置物流代理";default:return "物流响应无效，已有物流记录仍保留";}}
 static String stateLabel(String state){switch(state){case "IN_TRANSIT":return "运输中";case "READY":return "到达取件点";case "DELIVERED":return "物流显示已签收";default:return "状态未知";}}
}
