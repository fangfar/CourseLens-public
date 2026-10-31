package cn.courselens;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.room.InvalidationTracker;
import java.util.*;

@android.annotation.SuppressLint("ViewConstructor")
final class ParcelPage extends LinearLayout {
 private final Activity activity;
 private final SecondaryUi ui;
 private final LinearLayout list;
 private final ScrollView scroll;
 private final LinearLayout snack;
 private final Handler timer=new Handler(Looper.getMainLooper());
 private int restoredScroll;
 private long expandedParcel;
 private final TextView feedback;
 private final Runnable clearFeedback=this::feedbackViewHide;
 private int ink,muted,card,surface;
 private boolean saving,active,closed,clearing;

 private AlertDialog currentSheet,discardDialog;private Integer sheetNavigationColor;
 private ParcelRecord draft;
 private EditText codeField,stationField,trackingField,carrierField,importInput;
 private TextView importError;

 private final InvalidationTracker.Observer observer=new InvalidationTracker.Observer("parcels"){
  @Override public void onInvalidated(Set<String> tables){activity.runOnUiThread(()->{if(active&&!closed)load();});}
 };

 ParcelPage(Activity activity,Bundle state,Runnable openMenu){
  super(activity);this.activity=activity;ui=new SecondaryUi(activity);ink=activity.getColor(R.color.parcel_ink);muted=activity.getColor(R.color.parcel_muted);card=activity.getColor(R.color.parcel_card);surface=activity.getColor(R.color.parcel_surface);
  restoredScroll=state==null?0:state.getInt("scrollY",0);expandedParcel=state==null?0:state.getLong("expandedParcel",0);
  setTag("parcel-center");setOrientation(VERTICAL);setBackgroundColor(surface);setPadding(0,ui.dp(8),0,0);
  LinearLayout pinned=new LinearLayout(activity);pinned.setOrientation(VERTICAL);
  LinearLayout header=new LinearLayout(activity);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(ui.dp(16),ui.dp(4),ui.dp(16),0);pinned.addView(header);
  Button menu=ui.menuAction(header,openMenu);menu.getCompoundDrawables()[0].setTint(ink);header.addView(new View(activity),new LinearLayout.LayoutParams(ui.dp(56),1));TextView title=text("快递",24,ink);title.setTag("primary-page-title");title.setGravity(Gravity.CENTER);title.setTypeface(Typeface.DEFAULT_BOLD);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));icon(header,R.drawable.ic_add,"添加快递",()->edit(new ParcelRecord()));header.getChildAt(header.getChildCount()-1).setTag("quick-menu-button");icon(header,R.drawable.ic_more,"快递设置",this::settings);header.getChildAt(header.getChildCount()-1).setTag("parcel-settings");ui.fitHeader(header,title);
  feedback=text("",13,muted);feedback.setVisibility(GONE);feedback.setPadding(ui.dp(20),ui.dp(4),ui.dp(20),ui.dp(8));feedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);pinned.addView(feedback);
  pinned.setBackground(new android.graphics.drawable.ColorDrawable(surface));
  scroll=new ScrollView(activity);scroll.setFillViewport(true);scroll.setTag("parcel-scroll");list=column();list.setTag("parcel-list");list.setPadding(ui.dp(16),0,ui.dp(16),ui.dp(24));scroll.addView(list);
  addView(new ScrollHeader(activity,pinned,scroll,surface,scroll),new LinearLayout.LayoutParams(-1,0,1));
  snack=new LinearLayout(activity);snack.setGravity(Gravity.CENTER_VERTICAL);snack.setPadding(ui.dp(16),0,ui.dp(8),0);snack.setBackground(ui.bg(ink,16));snack.setVisibility(GONE);addView(snack,new LinearLayout.LayoutParams(-1,-2));
  ParcelRepository.IO.execute(()->{if(!closed){SharedPreferences settings=ParcelNotificationListener.settings(activity);if(settings.contains("proxy")||settings.contains("backgroundTracking")||settings.contains("trackingRetryAt"))ParcelWorker.schedule(activity);ParcelDatabase.get(activity).getInvalidationTracker().addObserver(observer);}});
  if(state!=null&&state.containsKey("draftCode")){ParcelRecord restored=new ParcelRecord();restored.id=state.getLong("draftId");restored.code=state.getString("draftCode","");restored.station=state.getString("draftStation","");restored.trackingNumber=state.getString("draftTracking","");restored.carrier=state.getString("draftCarrier","");restored.state=state.getInt("draftState");restoreDraft(restored);}
 }
 void resume(){if(active)return;active=true;load();if(currentSheet!=null&&currentSheet.isShowing()){TextView permission=currentSheet.getWindow().getDecorView().findViewWithTag("parcel-notification-access");if(permission!=null)permission.setText("通知访问权限 · "+(notificationAccess()?"已授权":"未授权"));}}
 void pause(){active=false;}
 void close(){
  closed=true;active=false;if(discardDialog!=null)discardDialog.dismiss();timer.removeCallbacksAndMessages(null);feedback.removeCallbacks(clearFeedback);
  ParcelRepository.IO.execute(()->ParcelDatabase.get(activity).getInvalidationTracker().removeObserver(observer));
  if(currentSheet!=null){currentSheet.setOnDismissListener(null);if(currentSheet instanceof MotionSheetDialog)((MotionSheetDialog)currentSheet).dismissNow();else currentSheet.dismiss();currentSheet=null;restoreNavigationColor();}
 }
 void saveState(Bundle out){
  out.putInt("scrollY",scroll.getScrollY());out.putLong("expandedParcel",expandedParcel);
  if(draft!=null&&currentSheet!=null&&currentSheet.isShowing()){out.putLong("draftId",draft.id);out.putString("draftCode",codeField.getText().toString());out.putString("draftStation",stationField.getText().toString());out.putString("draftTracking",trackingField.getText().toString());out.putString("draftCarrier",carrierField.getText().toString());out.putInt("draftState",draft.state);}
 }
 void handleIntent(Intent intent){
  if(intent==null)return;
  if(ParcelActivity.OPEN_SETTINGS.equals(intent.getAction())){intent.setAction(Intent.ACTION_MAIN);settings();return;}
  receive(intent);
 }
 void onActivityResult(int request,int result,Intent data){
  if(request==501){if(result==Activity.RESULT_OK&&data!=null&&data.getData()!=null){if(importInput!=null&&importInput.isShown()&&currentSheet!=null)currentSheet.dismiss();ocr(data.getData());}return;}
  if(request!=503)return;
  if(data==null)return;
  if(!Objects.equals(data.getStringExtra("epoch"),ParcelNotificationListener.settings(activity).getString("epoch","")))return;
  if(result!=Activity.RESULT_OK){String error=data.getStringExtra("error");if(error!=null)readFeedback(error);return;}
  try{List<ParcelRules.OcrLine> lines=ParcelOcr.lines(data.getStringExtra("ocrResult"));ingestCandidates(ParcelRules.extractImage(lines),"截图 OCR");}
  catch(Exception e){readFeedback("图片识别结果无效，请更新插件后重试");}
 }
 void onRequestPermissionsResult(int request,String[] permissions,int[] grants){
  if(request!=502)return;
  if(grants.length==0||grants[0]!=android.content.pm.PackageManager.PERMISSION_GRANTED){ParcelNotificationListener.settings(activity).edit().putBoolean("reminders",false).apply();ParcelWorker.schedule(activity);message("未允许通知，提醒已关闭；快递管理和识别仍可使用");if(currentSheet!=null&&currentSheet.isShowing())settingsSection(1);}
 }
 void settings(){
  LinearLayout body=sheet("快递设置",true);AlertDialog menu=currentSheet;
  String[] labels={"识别设置","提醒设置","桌面小组件"};
  for(int i=0;i<labels.length;i++){final int section=i;if(i==2)ui.popupDivider(body);TextView item=ui.popupItem(body,labels[i],()->{menu.dismiss();settingsSection(section);});item.setTag("parcel-setting-"+i);}
  ui.popupDivider(body);ui.popupItem(body,"快递恢复",()->{menu.dismiss();history();}).setTag("parcel-history-entry");
 }
 void history(){
  if(closed||clearing)return;LinearLayout body=sheet("快递恢复");body.setTag("parcel-history");TextView status=text("正在读取…",14,muted);body.addView(status);AlertDialog history=currentSheet;
  ParcelRepository.IO.execute(()->{try{List<ParcelRecord> deleted=ParcelDatabase.get(activity).parcels().deleted();activity.runOnUiThread(()->{if(closed||currentSheet!=history||!history.isShowing())return;body.removeAllViews();if(deleted.isEmpty()){body.addView(text("暂无可恢复的快递",14,muted));return;}for(ParcelRecord p:deleted){String label=(p.code.isEmpty()?p.trackingNumber:p.code)+(p.station.isEmpty()?"":" · "+p.station);Button item=button(body,label,()->ParcelRepository.IO.execute(()->{try{new ParcelRepository(activity).restoreDeleted(p,false);activity.runOnUiThread(()->{if(!closed){load();message("快递已恢复");if(currentSheet==history)history();}});}catch(RuntimeException e){activity.runOnUiThread(()->message("恢复失败，已有相同快递或记录已变更"));}}));item.setTag("parcel-history-restore-"+p.id);}});}catch(RuntimeException e){activity.runOnUiThread(()->{if(currentSheet==history)status.setText("读取失败，请重试");});}});
 }
 private void settingsSection(int section){
  String[] labels={"识别设置","提醒设置","桌面小组件"};LinearLayout settings=sheet(labels[section]),body;SharedPreferences prefs=ParcelNotificationListener.settings(activity);
  if(section==0){body=settings;
  body.addView(text("本机识别短信、菜鸟、淘宝、拼多多、微信和 QQ 的通知，不读取短信数据库，不上传通知全文。",14,muted));
  Switch recognition=toggle(body,"后台通知识别",prefs.getBoolean("recognition",false));
  recognition.setOnCheckedChangeListener((view,enabled)->{
   if(!enabled){prefs.edit().putBoolean("recognition",false).putString("epoch",UUID.randomUUID().toString()).apply();return;}
   AlertDialog consent=new AlertDialog.Builder(activity,R.style.SecondaryDialog).setTitle("开启快递通知识别？").setMessage("系统授权允许读取通知。软件解析短信、菜鸟、淘宝、拼多多、微信和 QQ 的通知，仅保存取件码、地址、运单号和来源，不保存通知全文。验证码与动态身份码会排除。你可以随时关闭，并在系统中撤销通知访问。").setNegativeButton("取消",(d,w)->recognition.setChecked(false)).setOnCancelListener(d->recognition.setChecked(false)).setPositiveButton("继续",(d,w)->{prefs.edit().putBoolean("recognition",true).putString("epoch",UUID.randomUUID().toString()).apply();try{android.service.notification.NotificationListenerService.requestRebind(new ComponentName(activity,ParcelNotificationListener.class));if(!notificationAccess())activity.startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}catch(ActivityNotFoundException|SecurityException e){message("无法打开通知访问设置，请在系统设置中手动授权");}}).create();consent.show();ui.styleDialog(consent,false);
  });
  button(body,"通知访问权限 · "+(notificationAccess()?"已授权":"未授权"),()->{try{activity.startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}catch(ActivityNotFoundException|SecurityException e){message("请在系统设置中调整通知访问权限");}}).setTag("parcel-notification-access");
  if(prefs.getBoolean("recognitionError",false))body.addView(text("曾有识别结果保存失败，请检查剩余存储空间后重试。",13,muted));
  button(body,LocalOcr.AVAILABLE?"图片识别 · 已内置":OcrPluginActivity.installed(activity)?"图片识别插件 · 已安装":"下载图片识别插件",()->ocr(null)).setTag("parcel-ocr-plugin-settings");
  }
  if(section==1){body=settings;
  Switch reminders=toggle(body,"快递提醒",prefs.getBoolean("reminders",false));reminders.setOnCheckedChangeListener((v,enabled)->{prefs.edit().putBoolean("reminders",enabled).apply();if(!enabled)ParcelNotifications.cancelAll(activity);if(enabled&&Build.VERSION.SDK_INT>=33&&activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)activity.requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},502);ParcelWorker.schedule(activity);});
  boolean notifications=androidx.core.app.NotificationManagerCompat.from(activity).areNotificationsEnabled();
  button(body,"系统通知 · "+(notifications?"已开启":"未开启"),()->{try{activity.startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,activity.getPackageName()));}catch(ActivityNotFoundException e){message("请在系统设置中调整 Morrow 的通知权限和快递提醒频道");}});
  }
  if(section==2){body=settings;
  Switch widget=toggle(body,"桌面小组件显示取件码与地址",prefs.getBoolean("widgetCodes",false));widget.setOnCheckedChangeListener((v,enabled)->{prefs.edit().putBoolean("widgetCodes",enabled).apply();ParcelRepository.IO.execute(()->ParcelWidget.updateAll(activity));});body.addView(text("开启后，桌面可见的人也能看到取件码。默认只显示待取数量。",13,muted));
  button(body,"添加快递小组件",()->{android.appwidget.AppWidgetManager manager=android.appwidget.AppWidgetManager.getInstance(activity);if(manager.isRequestPinAppWidgetSupported()){boolean accepted=manager.requestPinAppWidget(new ComponentName(activity,ParcelWidget.class),null,null);message(accepted?"已向桌面请求添加，请在系统面板中确认":"桌面未接受请求，请长按桌面手动添加快递中心组件");}else message("请长按桌面 → 小组件 → Morrow → 快递中心");});
  }
 }

 private void restoreDraft(ParcelRecord restored){ParcelRepository.IO.execute(()->{ParcelRecord existing=restored.id==0?restored:ParcelDatabase.get(activity).parcels().get(restored.id);activity.runOnUiThread(()->{if(closed||existing==null)return;existing.code=restored.code;existing.station=restored.station;existing.trackingNumber=restored.trackingNumber;existing.carrier=restored.carrier;existing.state=restored.state;edit(existing);});});}
 private void receive(Intent intent){
  if(intent==null||!Intent.ACTION_SEND.equals(intent.getAction()))return;
  try{String type=intent.getType();if(type!=null&&type.startsWith("image/")){android.net.Uri uri=Build.VERSION.SDK_INT>=33?intent.getParcelableExtra(Intent.EXTRA_STREAM,android.net.Uri.class):intent.getParcelableExtra(Intent.EXTRA_STREAM);ocr(uri);}else if("text/plain".equals(type)){CharSequence value=intent.getCharSequenceExtra(Intent.EXTRA_TEXT);if(value!=null&&value.length()<=ParcelRules.MAX_TEXT)recognize(value.toString(),"Android 分享文字");else message("分享文字为空或过长，请手动添加");}}
  catch(RuntimeException e){message("无法读取分享内容，请重新分享");}
 }
 private LinearLayout column(){LinearLayout view=new LinearLayout(activity);view.setOrientation(LinearLayout.VERTICAL);return view;}
 private TextView text(String value,int size,int color){return ui.text(value,size,color);}
 private void icon(LinearLayout host,int drawable,String name,Runnable action){Button b=ui.iconAction(host,drawable,name,action);b.getCompoundDrawables()[0].setTint(ink);}
 private Button button(LinearLayout host,String name,Runnable action){Button b=ui.action(host,name,action);b.setBackgroundTintList(null);b.setTextColor(ink);b.setBackground(ui.bg(activity.getColor(R.color.parcel_soft),16));b.setGravity(Gravity.CENTER);LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)b.getLayoutParams();lp.width=host.getOrientation()==VERTICAL?-1:-2;lp.bottomMargin=ui.dp(4);return b;}
 private void feedbackViewHide(){if(!closed)feedback.setVisibility(GONE);}
 private void message(String value){if(closed)return;if(!isShown()||currentSheet!=null&&currentSheet.isShowing()){Toast.makeText(activity,value,Toast.LENGTH_LONG).show();return;}feedback.removeCallbacks(clearFeedback);feedback.setText(value);feedback.setVisibility(VISIBLE);feedback.postDelayed(clearFeedback,6000);}
 private void load(){
  if(closed)return;
  ParcelRepository.IO.execute(()->{try{List<ParcelRecord> records=ParcelDatabase.get(activity).parcels().all();activity.runOnUiThread(()->{if(!closed)render(records);});}catch(RuntimeException e){activity.runOnUiThread(()->{if(!closed)message("本机快递数据读取失败，请重试；未清空原数据");});}});
 }
 private Button pill(LinearLayout host,String label,boolean primary,Runnable action){
  Button b=button(host,label,action);b.setTextSize(14);b.setTypeface(null,Typeface.BOLD);b.setPadding(ui.dp(8),ui.dp(10),ui.dp(8),ui.dp(10));b.setBackgroundTintList(null);b.setBackground(ui.bg(primary?ui.PRIMARY:activity.getColor(R.color.parcel_soft),24));b.setTextColor(primary?ui.ON_PRIMARY:ink);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(ui.dp(4),ui.dp(8),ui.dp(4),0);b.setLayoutParams(lp);return b;
 }
 private LinearLayout actionRow(LinearLayout host){
  LinearLayout row=new LinearLayout(activity){
   @Override protected void onMeasure(int width,int height){
    String[] labels=new String[getChildCount()];for(int i=0;i<labels.length;i++)labels[i]=((TextView)getChildAt(i)).getText().toString();
    boolean stack=labels.length>1&&!fitsRow(MeasureSpec.getSize(width)-getPaddingLeft()-getPaddingRight(),labels);setOrientation(stack?VERTICAL:HORIZONTAL);
    for(int i=0;i<getChildCount();i++){View child=getChildAt(i);LinearLayout.LayoutParams size=(LinearLayout.LayoutParams)child.getLayoutParams();if(size.width!=(stack?-1:0)||size.weight!=(stack?0:1)||size.bottomMargin!=0||size.leftMargin!=ui.dp(4)||size.gravity!=-1){size.width=stack?-1:0;size.weight=stack?0:1;size.gravity=-1;size.setMargins(ui.dp(4),ui.dp(8),ui.dp(4),0);child.setLayoutParams(size);}}super.onMeasure(width,height);
   }
  };row.setBaselineAligned(false);host.addView(row,new LinearLayout.LayoutParams(-1,-2));return row;
 }
 // Weighted pills shrink below their text once space runs out, so measure first and stack rows that cannot stay single-line.
 private boolean fitsRow(int available,String[] pills){
  float widest=0;for(String value:pills)widest=Math.max(widest,measure(value,14,true));
  return widest+ui.dp(24)<=(available-ui.dp(8*pills.length))/(float)pills.length;
 }
 private float measure(String value,int size,boolean bold){android.graphics.Paint probe=new android.graphics.Paint();probe.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);probe.setTextSize(size*activity.getResources().getDisplayMetrics().scaledDensity);return probe.measureText(value);}
 private LinearLayout card(LinearLayout host){LinearLayout row=column();row.setPadding(ui.dp(16),ui.dp(16),ui.dp(16),ui.dp(16));row.setBackground(ui.bg(card,SecondaryUi.CARD_RADIUS));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=ui.dp(12);host.addView(row,lp);return row;}
 private void render(List<ParcelRecord> records){
  int y=restoredScroll>0?restoredScroll:scroll.getScrollY();restoredScroll=0;list.removeAllViews();
  LinearLayout identity=card(list);identity.setTag("parcel-identity");TextView label=text("身份码",14,ink);label.setTypeface(null,Typeface.BOLD);identity.addView(label);LinearLayout strip=actionRow(identity);for(int i=0;i<3;i++){final int platform=i;String name=new String[]{"淘宝","菜鸟","拼多多"}[i];Button entry=pill(strip,name,false,()->ParcelIdentity.open(activity,platform));entry.setContentDescription("打开"+name+"官方身份码");entry.setTag("parcel-identity-"+i);}
  LinearLayout add=actionRow(list);((LinearLayout.LayoutParams)add.getLayoutParams()).bottomMargin=ui.dp(12);pill(add,"手动添加",true,()->edit(new ParcelRecord())).setTag("parcel-manual-add");pill(add,"读取信息",false,this::importSheet).setTag("parcel-read-info");
  List<ParcelRecord> visible=new ArrayList<>(records);visible.sort(Comparator.comparing((ParcelRecord p)->p.state==ParcelRecord.DONE).thenComparing(Comparator.comparingLong((ParcelRecord p)->p.createdAt).reversed()));
  if(visible.isEmpty()){expandedParcel=0;TextView empty=text("暂无快递记录",17,muted);empty.setGravity(Gravity.CENTER);empty.setPadding(0,ui.dp(32),0,ui.dp(32));list.addView(empty);}
  else{
   LinearLayout rows=card(list);rows.setTag("parcel-rows");rows.setPadding(ui.dp(16),0,ui.dp(16),0);boolean expandedExists=false;
   for(ParcelRecord p:visible){
    boolean done=p.state==ParcelRecord.DONE;
    if(rows.getChildCount()>0){View line=new View(activity);line.setBackgroundColor(ui.DIVIDER);LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(-1,ui.dp(1));rows.addView(line,size);}
    LinearLayout row=column();row.setTag("parcel-record-"+p.id);rows.addView(row,new LinearLayout.LayoutParams(-1,-2));LinearLayout heading=new LinearLayout(activity);heading.setGravity(Gravity.TOP);heading.setPadding(0,ui.dp(12),0,ui.dp(12));row.addView(heading,new LinearLayout.LayoutParams(-1,-2));
    LinearLayout content=column();content.setPadding(0,ui.dp(4),0,ui.dp(6));content.setMinimumHeight(ui.dp(48));content.setTag("parcel-content-"+p.id);heading.addView(content,new LinearLayout.LayoutParams(0,-2,1));TextView station=text(p.station.isEmpty()?(p.carrier.isEmpty()?"快递信息":p.carrier):p.station,16,done?muted:ink);station.setTypeface(null,Typeface.BOLD);content.addView(station);
    TextView code=text(p.code.isEmpty()?"取件码待补充":p.code,22,done?muted:ink);code.setTag("parcel-code-"+p.id);if(done)code.setPaintFlags(code.getPaintFlags()|android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);code.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);code.setPadding(0,ui.dp(6),0,0);content.addView(code);if(!p.confirmed)content.addView(text("识别结果待核对",13,muted));
    LinearLayout details=column();details.setTag("parcel-inline-actions-"+p.id);details.setPadding(0,0,0,ui.dp(12));row.addView(details,new LinearLayout.LayoutParams(-1,-2));
    if(!p.carrier.isEmpty()&&!p.carrier.equals(station.getText().toString()))details.addView(text(p.carrier,13,muted));if(!p.trackingNumber.isEmpty())details.addView(text("运单号 "+p.trackingNumber,13,muted));
    if(!p.confirmed)details.addView(text("识别自"+p.source+"，请核对取件码和地址",13,muted));
    LinearLayout edit=actionRow(details);if(done)pill(edit,"恢复未取件",false,()->restore(p.id)).setTag("parcel-restore-"+p.id);else if(p.confirmed)pill(edit,"已取件",true,()->complete(p.id)).setTag("parcel-complete-"+p.id);pill(edit,p.confirmed?"编辑":"核对并确认",false,()->edit(p));pill(edit,"删除",false,()->confirmDelete(p)).setTextColor(ui.DANGER);
    if(!p.code.isEmpty()){LinearLayout pickup=actionRow(details);pill(pickup,"复制取件码",false,()->copy(p.code));pill(pickup,"分享取件码",false,()->share(p.code)).setTag("parcel-share-"+p.id);if(p.confirmed&&p.state==ParcelRecord.READY)pill(pickup,"提醒",false,()->reminder(p));}
    if(!p.trackingEvents.isEmpty())button(details,"物流记录",()->tracking(p));
    details.setVisibility(expandedParcel==p.id?VISIBLE:GONE);if(expandedParcel==p.id)expandedExists=true;
    content.setFocusable(true);content.setContentDescription((done?"已取件快递 ":"未取件快递 ")+station.getText()+"，"+code.getText()+"，点击展开或收起操作");content.setOnClickListener(v->toggleParcel(p.id,rows));ui.focusOutline(content);row.setOnLongClickListener(v->{actions(p);return true;});
   }
   if(!expandedExists)expandedParcel=0;
  }
  scroll.post(()->{if(!closed)scroll.scrollTo(0,y);});
 }
 private void toggleParcel(long id,LinearLayout rows){
  boolean open=expandedParcel!=id;if(android.animation.ValueAnimator.areAnimatorsEnabled())android.transition.TransitionManager.beginDelayedTransition(rows,new android.transition.TransitionSet().addTransition(new android.transition.ChangeBounds()).addTransition(new android.transition.Fade()).setDuration(open?Motion.EXPAND:Motion.COLLAPSE).setInterpolator(Motion.ENTER));
  View previous=rows.findViewWithTag("parcel-inline-actions-"+expandedParcel);if(previous!=null)previous.setVisibility(GONE);expandedParcel=open?id:0;View next=rows.findViewWithTag("parcel-inline-actions-"+id);next.setVisibility(open?VISIBLE:GONE);
 }
 private void confirmDelete(ParcelRecord p){AlertDialog dialog=new AlertDialog.Builder(activity,R.style.SecondaryDialog).setTitle("删除这条快递？").setMessage("删除后可在快递恢复中找回。").setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->delete(p)).create();dialog.show();ui.styleDialog(dialog,true);}
 private void trackingEvent(LinearLayout host,String time,String value){
  LinearLayout entry=column();entry.setPadding(0,ui.dp(14),0,ui.dp(14));host.addView(entry,new LinearLayout.LayoutParams(-1,-2));entry.addView(text(time,12,muted));TextView description=text(value,15,ink);description.setPadding(0,ui.dp(6),0,0);entry.addView(description);View divider=new View(activity);divider.setBackgroundColor(ui.DIVIDER);host.addView(divider,new LinearLayout.LayoutParams(-1,ui.dp(1)));
 }
 private void copy(String value){ClipData clip=ClipData.newPlainText("取件码",value);PersistableBundle extras=new PersistableBundle();extras.putBoolean("android.content.extra.IS_SENSITIVE",true);clip.getDescription().setExtras(extras);((ClipboardManager)activity.getSystemService(Activity.CLIPBOARD_SERVICE)).setPrimaryClip(clip);message("取件码已复制");}
 private void share(String value){try{Intent send=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,value);activity.startActivity(Intent.createChooser(send,"分享取件码"));}catch(ActivityNotFoundException|SecurityException e){message("无法打开分享，请复制取件码");}}
 private void restoreNavigationColor(){if(sheetNavigationColor!=null){if(Build.VERSION.SDK_INT>=29)try{activity.getWindow().setNavigationBarContrastEnforced(sheetNavigationColor==0);}catch(RuntimeException ignored){}sheetNavigationColor=null;}}
 private LinearLayout sheet(String title){return sheet(title,false);}
 private LinearLayout sheet(String title,boolean dropdown){
  if(currentSheet!=null){currentSheet.setOnDismissListener(null);currentSheet.dismiss();draft=null;}if(sheetNavigationColor==null){if(Build.VERSION.SDK_INT>=29)try{sheetNavigationColor=activity.getWindow().isNavigationBarContrastEnforced()?1:0;}catch(RuntimeException e){sheetNavigationColor=0;}}LinearLayout shell=column();shell.setPadding(ui.dp(dropdown?4:20),ui.dp(dropdown?SecondaryUi.MENU_VERTICAL_PADDING:16),ui.dp(dropdown?4:20),ui.dp(dropdown?SecondaryUi.MENU_VERTICAL_PADDING:8));shell.setBackground(dropdown?ui.popupSurface():ui.sheetSurface());shell.setClipToOutline(true);shell.setTag(dropdown?"parcel-settings-menu":"parcel-sheet");
  if(!dropdown){LinearLayout header=new LinearLayout(activity);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(0,0,0,ui.dp(12));TextView label=text(title,20,ink);label.setTypeface(null,Typeface.BOLD);header.addView(label,new LinearLayout.LayoutParams(0,-2,1));Button close=button(header,"关闭",()->currentSheet.cancel());close.setTextSize(14);close.setPadding(ui.dp(12),ui.dp(8),ui.dp(12),ui.dp(8));LinearLayout.LayoutParams closeSize=(LinearLayout.LayoutParams)close.getLayoutParams();closeSize.topMargin=0;closeSize.bottomMargin=0;shell.addView(header);View divider=new View(activity);divider.setBackgroundColor(ui.DIVIDER);shell.addView(divider,new LinearLayout.LayoutParams(-1,ui.dp(1)));}
  android.graphics.Rect visible=new android.graphics.Rect();getWindowVisibleDisplayFrame(visible);int[] at=new int[2];View anchor=findViewWithTag("parcel-settings");anchor.getLocationOnScreen(at);int popupTop=Math.max(0,at[1]+anchor.getHeight());
  ScrollView scroll=new ScrollView(activity){@Override protected void onMeasure(int width,int height){int available=activity.getResources().getDisplayMetrics().heightPixels;int cap=dropdown?Math.max(ui.dp(52),visible.height()-ui.dp(28)):Math.max(ui.dp(48),available-ui.dp(180));super.onMeasure(width,MeasureSpec.makeMeasureSpec(MeasureSpec.getMode(height)==MeasureSpec.UNSPECIFIED?cap:Math.min(cap,MeasureSpec.getSize(height)),MeasureSpec.AT_MOST));}};scroll.setClipToPadding(false);shell.addView(scroll,new LinearLayout.LayoutParams(-1,-2));LinearLayout body=column();body.setPadding(0,ui.dp(dropdown?0:12),0,ui.dp(dropdown?0:20));scroll.addView(body);
  MotionSheetDialog sheet=new MotionSheetDialog(activity,shell,dropdown,popupTop);currentSheet=sheet;sheet.setOnDismissListener(d->{if(currentSheet==sheet){draft=null;restoreNavigationColor();}});
  if(Build.VERSION.SDK_INT>=29){try{sheet.getWindow().setNavigationBarContrastEnforced(false);activity.getWindow().setNavigationBarContrastEnforced(dropdown);}catch(RuntimeException ignored){}}
  sheet.show();return body;
 }
 private EditText field(LinearLayout body,String label,String value){TextView name=text(label,14,muted);name.setPadding(0,ui.dp(16),0,ui.dp(8));body.addView(name);EditText input=new EditText(activity);input.setText(value);input.setTextSize(16);input.setTextColor(ink);input.setSingleLine(true);input.setMinHeight(ui.dp(SecondaryUi.CONTROL_HEIGHT));input.setPadding(ui.dp(16),ui.dp(8),ui.dp(16),ui.dp(8));input.setBackgroundResource(R.drawable.form_field);input.setInputType(InputType.TYPE_CLASS_TEXT);input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);input.setContentDescription(label);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(200)});body.addView(input,new LinearLayout.LayoutParams(-1,-2));return input;}
 private void edit(ParcelRecord p){
  LinearLayout body=sheet(p.confirmed?(p.id==0?"添加快递":"编辑快递"):"确认识别结果");draft=p;
  body.addView(text(p.state==ParcelRecord.DONE?"修改信息后仍保留在已取件记录中。":"填写取件码或运单号即可保存，取件后展开条目，点击“已取件”完成。",13,muted));
  codeField=field(body,"取件码（与运单号分开）",p.code);codeField.setTag("parcel-editor-code");stationField=field(body,"驿站 / 地址",p.station);trackingField=field(body,"运单号（选填）",p.trackingNumber);carrierField=field(body,"快递公司（选填）",p.carrier);
  TextView error=text("",14,ui.DANGER);error.setVisibility(GONE);error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);AlertDialog editor=currentSheet;
  ((MotionSheetDialog)editor).cancelRequest=()->{if(saving){message("正在保存，请稍候");return;}boolean changed=!codeField.getText().toString().equals(p.code)||!stationField.getText().toString().equals(p.station)||!trackingField.getText().toString().equals(p.trackingNumber)||!carrierField.getText().toString().equals(p.carrier);if(changed){if(discardDialog!=null&&discardDialog.isShowing())return;AlertDialog confirm=new AlertDialog.Builder(activity,R.style.SecondaryDialog).setTitle("放弃修改？").setMessage("尚未保存的快递信息将被丢弃。").setNegativeButton("继续编辑",null).setPositiveButton("放弃",(d,w)->editor.dismiss()).create();discardDialog=confirm;confirm.setOnDismissListener(d->{if(discardDialog==confirm)discardDialog=null;});confirm.show();ui.styleDialog(confirm,false);}else editor.dismiss();};
  Button save=ui.action(body,p.confirmed?"保存":"保存并确认",()->{
   if(saving)return;ParcelRecord value=new ParcelRecord();value.id=p.id;value.code=codeField.getText().toString();value.station=stationField.getText().toString();value.trackingNumber=trackingField.getText().toString();value.carrier=carrierField.getText().toString();value.state=p.state==ParcelRecord.DONE?ParcelRecord.DONE:value.code.trim().isEmpty()?ParcelRecord.TRANSIT:ParcelRecord.READY;
   value.source=p.source;value.rule=p.rule;value.confidence=p.confidence;value.confirmed=true;value.remindAt=p.remindAt;value.remindedAt=p.remindedAt;
   if(value.trackingNumber.equals(p.trackingNumber)&&value.carrier.equals(p.carrier)){value.trackingState=p.trackingState;value.trackingEvents=p.trackingEvents;value.lastQueryAt=p.lastQueryAt;value.retryAt=p.retryAt;value.trackingError=p.trackingError;}
   error.setVisibility(GONE);saving=true;Button submit=editor.getWindow().getDecorView().findViewWithTag("parcel-editor-save");submit.setEnabled(false);submit.setText("保存中…");ParcelRepository.IO.execute(()->{String failure=null;try{new ParcelRepository(activity).save(value);}catch(IllegalArgumentException e){failure=e.getMessage();}catch(RuntimeException e){failure="保存失败，请重试；原数据仍保留";}String result=failure;activity.runOnUiThread(()->{saving=false;if(closed)return;if(result==null){editor.dismiss();message("快递信息已保存");load();}else{submit.setEnabled(true);submit.setText(p.confirmed?"保存":"保存并确认");error.setText(result);error.setVisibility(VISIBLE);}});});
  });save.setTag("parcel-editor-save");
  fixedActions(body,save,error,"parcel-editor-actions");
 }
 private void fixedActions(LinearLayout body,Button save,TextView error,String tag){
  AlertDialog dialog=currentSheet;body.removeView(save);LinearLayout shell=(LinearLayout)body.getParent().getParent();View scroll=(View)body.getParent();scroll.setLayoutParams(new LinearLayout.LayoutParams(-1,0,1));
  if(error!=null){body.removeView(error);shell.addView(error,new LinearLayout.LayoutParams(-1,-2));}
  LinearLayout footer=actionRow(shell);footer.setPadding(0,ui.dp(4),0,ui.dp(12));footer.setTag(tag);pill(footer,"取消",false,dialog::cancel);footer.addView(save);save.setTextColor(ui.ON_PRIMARY);save.setBackgroundTintList(null);save.setBackground(ui.bg(ui.PRIMARY,24));save.setTextSize(14);save.setTypeface(null,Typeface.BOLD);save.setPadding(ui.dp(8),ui.dp(10),ui.dp(8),ui.dp(10));
 }
 private void actions(ParcelRecord p){LinearLayout body=sheet("快递管理");TextView summary=text(p.code.isEmpty()?p.trackingNumber:p.code,22,ink);summary.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);body.addView(summary);if(!p.station.isEmpty())body.addView(text(p.station,14,muted));button(body,p.confirmed?"编辑":"核对并确认",()->edit(p));
  if(p.confirmed){if(!p.trackingEvents.isEmpty())button(body,"物流记录",()->tracking(p));if(p.state==ParcelRecord.READY)button(body,"未领取提醒",()->reminder(p));}
  button(body,"删除",()->confirmDelete(p)).setTextColor(ui.DANGER);}
 private void undoBar(String label,Runnable undo){
  timer.removeCallbacksAndMessages(null);snack.removeAllViews();TextView value=text(label,14,card);value.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);snack.addView(value,new LinearLayout.LayoutParams(0,-2,1));Button button=ui.action(snack,"撤销",()->{snack.setVisibility(GONE);timer.removeCallbacksAndMessages(null);undo.run();});button.setTextColor(card);button.setBackgroundTintList(null);button.setBackground(ui.bg(ink,24));LinearLayout.LayoutParams size=(LinearLayout.LayoutParams)button.getLayoutParams();size.topMargin=0;size.bottomMargin=0;snack.setVisibility(VISIBLE);timer.postDelayed(()->snack.setVisibility(GONE),5000);
 }
 private void complete(long id){ParcelRepository.IO.execute(()->{try{ParcelRecord p=ParcelDatabase.get(activity).parcels().get(id);if(p==null||!p.confirmed||p.state==ParcelRecord.DONE)return;int previous=p.state;long remindAt=p.remindAt,remindedAt=p.remindedAt;p.state=ParcelRecord.DONE;p.remindAt=0;new ParcelRepository(activity).save(p);long changedAt=p.updatedAt;activity.runOnUiThread(()->{if(closed)return;load();undoBar("已标记取件",()->ParcelRepository.IO.execute(()->{try{new ParcelRepository(activity).undoComplete(id,changedAt,previous,remindAt,remindedAt);activity.runOnUiThread(this::load);}catch(RuntimeException e){activity.runOnUiThread(()->message("恢复失败，记录可能已变更"));}}));});}catch(RuntimeException e){activity.runOnUiThread(()->message("保存失败，请重试"));}});}
 private void restore(long id){ParcelRepository.IO.execute(()->{try{ParcelRecord p=ParcelDatabase.get(activity).parcels().get(id);if(p==null||p.state!=ParcelRecord.DONE)return;new ParcelRepository(activity).undoComplete(id,p.updatedAt,p.code.isEmpty()?ParcelRecord.TRANSIT:ParcelRecord.READY,0,0);activity.runOnUiThread(()->{if(!closed){load();message("已恢复为未取件");}});}catch(RuntimeException e){activity.runOnUiThread(()->message("恢复失败，记录可能已变更"));}});}
 private void delete(ParcelRecord p){ParcelRepository.IO.execute(()->{try{ParcelRepository repository=new ParcelRepository(activity);ParcelRecord removed=repository.delete(p.id);if(removed==null)return;activity.runOnUiThread(()->{if(closed)return;if(currentSheet!=null)currentSheet.dismiss();load();undoBar("已删除快递",()->ParcelRepository.IO.execute(()->{try{repository.restoreDeleted(removed);activity.runOnUiThread(this::load);}catch(RuntimeException e){activity.runOnUiThread(()->message("恢复失败，已有相同快递或记录已变更"));}}));});}catch(RuntimeException e){activity.runOnUiThread(()->message("删除失败，请重试"));}});}
 private boolean notificationAccess(){return androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(activity).contains(activity.getPackageName());}
 private void importSheet(){
  LinearLayout body=sheet("读取快递信息");importInput=field(body,"粘贴取件信息，或选择图片（仅本机读取）","");importInput.setTag("parcel-import-text");importInput.setSingleLine(false);importInput.setMinLines(3);importInput.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(ParcelRules.MAX_TEXT)});
  importError=text("",14,ui.DANGER);importError.setTag("parcel-import-feedback");importError.setVisibility(GONE);importError.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);body.addView(importError);
  EditText input=importInput;button(body,"读取文字",()->recognize(input.getText().toString(),"粘贴文字")).setTag("parcel-read-text");button(body,LocalOcr.AVAILABLE||OcrPluginActivity.installed(activity)?"选择图片":"下载图片识别插件",()->ocr(null)).setTag("parcel-ocr-plugin");
  button(body,"通知读取设置",()->settingsSection(0)).setTag("parcel-notification-settings");button(body,"手动添加",()->edit(new ParcelRecord()));
 }
 private void readFeedback(String value){if(importError!=null&&currentSheet!=null&&currentSheet.isShowing()&&currentSheet.getWindow().getDecorView().findViewWithTag("parcel-import-feedback")==importError){importError.setText(value);importError.setVisibility(VISIBLE);}else message(value);}
 private void ocr(android.net.Uri uri){
  if(uri==null&&(LocalOcr.AVAILABLE||OcrPluginActivity.installed(activity))){
   try{ImagePicker.open(activity,501);}catch(ActivityNotFoundException e){readFeedback("系统没有可用的图片选择器");}
   return;
  }
  if(LocalOcr.AVAILABLE){
   String epoch=ParcelNotificationListener.settings(activity).getString("epoch","");
   LocalOcr.read(activity.getApplicationContext(),uri,(value,error)->activity.runOnUiThread(()->{if(closed)return;Intent result=new Intent().putExtra("epoch",epoch);if(error!=null)result.putExtra("error",error);else result.putExtra("ocrResult",value);onActivityResult(503,error==null?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);}));
   return;
  }
  Intent intent=new Intent(activity,OcrPluginActivity.class).putExtra("epoch",ParcelNotificationListener.settings(activity).getString("epoch",""));
  if(uri!=null){intent.setDataAndType(uri,"image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);intent.setClipData(ClipData.newRawUri("待识别截图",uri));}
  try{activity.startActivityForResult(intent,503);}catch(RuntimeException e){readFeedback("无法打开图片识别插件，请重试");}
 }
 private void recognize(String input,String source){
  ingestCandidates(ParcelRules.extract(input),source);
 }
 private void ingestCandidates(List<ParcelRules.Candidate> found,String source){
  if(found.isEmpty()){readFeedback(source.equals("截图 OCR")?"未找到明确的取件码，请尝试其他图片或手动添加":"未找到明确的取件码，可检查上方文字或手动添加");return;}
  String epoch=ParcelNotificationListener.settings(activity).getString("epoch","");ParcelRepository.IO.execute(()->{int added=0;try{if(!epoch.equals(ParcelNotificationListener.settings(activity).getString("epoch","")))return;for(ParcelRules.Candidate c:found)if(new ParcelRepository(activity).ingest(ParcelNotificationListener.record(c,source,true)))added++;int count=added;activity.runOnUiThread(()->{if(closed)return;if(importInput!=null&&currentSheet!=null&&currentSheet.isShowing()&&currentSheet.getWindow().getDecorView().findViewWithTag("parcel-import-text")==importInput)currentSheet.dismiss();message(count==0?"识别结果已存在，未重复添加":"已读取 "+count+" 条，请展开记录核对后保存");load();});}catch(RuntimeException e){activity.runOnUiThread(()->{if(!closed)message("识别结果保存失败，请重试");});}});
 }
 private String trackingStateLabel(String state){switch(state){case "IN_TRANSIT":return "运输中";case "READY":return "到达取件点";case "DELIVERED":return "物流显示已签收";default:return "状态未知";}}
 private void tracking(ParcelRecord p){
  LinearLayout body=sheet("物流记录");body.addView(text(p.trackingNumber.isEmpty()?"请先编辑并填写运单号":p.trackingNumber,17,ink));
  if(!p.trackingState.isEmpty())body.addView(text(trackingStateLabel(p.trackingState),15,ink));
  try{if(!p.trackingEvents.isEmpty()){org.json.JSONArray events=new org.json.JSONArray(p.trackingEvents);for(int i=0;i<events.length();i++){org.json.JSONObject event=events.getJSONObject(i);trackingEvent(body,event.getString("time"),event.getString("description"));}}}catch(org.json.JSONException e){body.addView(text("本机物流记录无法显示",14,muted));}
  body.addView(text("仅展示本机已有物流记录，不自动更新；取件后请手动标记已取件。",13,muted));
 }
 private void reminder(ParcelRecord p){
  LinearLayout body=sheet("未领取提醒");body.addView(text("提醒可能因省电、后台限制而延迟。请先在快递管理中开启提醒。",14,muted));
  button(body,"选择日期与时间",()->pickReminder(p));
  button(body,"取消本条提醒",()->saveReminder(p.id,0));
 }
 private void pickReminder(ParcelRecord p){
  java.util.Calendar date=java.util.Calendar.getInstance();if(p.remindAt>System.currentTimeMillis())date.setTimeInMillis(p.remindAt);
  LinearLayout box=column();DatePicker picker=new DatePicker(new android.view.ContextThemeWrapper(activity,R.style.SecondaryPicker));picker.init(date.get(java.util.Calendar.YEAR),date.get(java.util.Calendar.MONTH),date.get(java.util.Calendar.DAY_OF_MONTH),null);picker.setTag("parcel-reminder-date");box.addView(picker);TimePicker time=new TimePicker(new android.view.ContextThemeWrapper(activity,R.style.TaskTimePickerTheme));time.setIs24HourView(true);time.setHour(date.get(java.util.Calendar.HOUR_OF_DAY));time.setMinute(date.get(java.util.Calendar.MINUTE));time.setTag("parcel-reminder-time");box.addView(time);TextView error=text("",14,ui.DANGER);error.setTag("parcel-reminder-error");error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);box.addView(error);ScrollView scroll=new ScrollView(activity);scroll.setFillViewport(true);scroll.addView(box);
  AlertDialog dialog=new AlertDialog.Builder(activity,R.style.SecondaryDialog).setTitle("提醒时间").setView(scroll).setNegativeButton("取消",null).setPositiveButton("确定",null).create();dialog.show();ui.styleDialog(dialog,false);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{picker.clearFocus();time.clearFocus();date.set(picker.getYear(),picker.getMonth(),picker.getDayOfMonth(),time.getHour(),time.getMinute(),0);date.set(java.util.Calendar.MILLISECOND,0);if(date.getTimeInMillis()<=System.currentTimeMillis()){error.setText("请选择未来的提醒时间");scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));return;}dialog.dismiss();saveReminder(p.id,date.getTimeInMillis());});
 }
 private void saveReminder(long id,long time){ParcelRepository.IO.execute(()->{try{ParcelDatabase db=ParcelDatabase.get(activity);db.runInTransaction(()->{ParcelRecord p=db.parcels().get(id);if(p==null||p.state!=ParcelRecord.READY)return;p.remindAt=time;p.remindedAt=0;db.parcels().update(p);});ParcelWorker.schedule(activity);activity.runOnUiThread(()->{if(closed)return;currentSheet.dismiss();message(time==0?"提醒已取消":"提醒时间已保存；请确认系统允许通知");});}catch(RuntimeException e){activity.runOnUiThread(()->message("提醒保存失败，请重试"));}});}
 private Switch toggle(LinearLayout body,String label,boolean checked){Switch view=new Switch(activity);view.setText(label);view.setTextSize(15);view.setTextColor(ink);view.setPadding(0,ui.dp(12),0,ui.dp(12));view.setSwitchPadding(ui.dp(16));view.setMinHeight(ui.dp(56));view.setChecked(checked);body.addView(view,new LinearLayout.LayoutParams(-1,-2));return view;}
 void clearData(){if(saving||closed||clearing){Toast.makeText(activity,"快递正在保存或删除，请稍后重试。",Toast.LENGTH_SHORT).show();return;}AlertDialog confirmation=new AlertDialog.Builder(activity,R.style.SecondaryDialog).setTitle("删除全部快递数据？").setMessage("将关闭后台识别和提醒，删除快递、识别候选、去重记录和快递设置，不影响课表和任务，此操作无法撤销。系统通知访问可在系统设置中撤销。").setNegativeButton("取消",null).setPositiveButton("全部删除",(dialog,which)->{if(saving||closed||clearing)return;clearing=true;snack.setVisibility(GONE);timer.removeCallbacksAndMessages(null);ParcelNotificationListener.settings(activity).edit().clear().putString("epoch",UUID.randomUUID().toString()).apply();ParcelWorker.schedule(activity);ParcelNotifications.cancelAll(activity);ParcelRepository.IO.execute(()->{try{new ParcelRepository(activity).clear();activity.runOnUiThread(()->{clearing=false;if(closed)return;Toast.makeText(activity,"快递数据已删除，后台功能已关闭",Toast.LENGTH_LONG).show();load();});}catch(RuntimeException e){activity.runOnUiThread(()->{clearing=false;Toast.makeText(activity,"删除失败，请重试；后台功能已关闭",Toast.LENGTH_LONG).show();});}});}).create();confirmation.show();ui.styleDialog(confirmation,true);}
}
