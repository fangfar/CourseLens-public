package cn.courselens;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.room.InvalidationTracker;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import java.util.*;

@android.annotation.SuppressLint("ViewConstructor")
final class ParcelPage extends LinearLayout {
 private final Activity activity;
 private final SecondaryUi ui;
 private final LinearLayout[] lists=new LinearLayout[3];
 private final ScrollView[] scrolls=new ScrollView[3];
 private final ViewPager pager;
 private final View capsule;
 private final LinearLayout choices;
 private final LinearLayout snack;
 private final Handler timer=new Handler(Looper.getMainLooper());
 private float pagePosition;
 private int[] restoredScroll;
 private long expandedTracking;
 private final TextView feedback;
 private final Runnable clearFeedback=this::feedbackViewHide;
 private final TextView[] tabs=new TextView[3];
 private int filter,ink,muted,card,surface;
 private boolean saving,active,closed;

 private AlertDialog currentSheet;
 private ParcelRecord draft;
 private EditText codeField,stationField,trackingField,carrierField;

 private final InvalidationTracker.Observer observer=new InvalidationTracker.Observer("parcels"){
  @Override public void onInvalidated(Set<String> tables){activity.runOnUiThread(()->{if(active&&!closed)load();});}
 };

 ParcelPage(Activity activity,Bundle state,Runnable openMenu){
  super(activity);this.activity=activity;ui=new SecondaryUi(activity);ink=activity.getColor(R.color.parcel_ink);muted=activity.getColor(R.color.parcel_muted);card=activity.getColor(R.color.parcel_card);surface=activity.getColor(R.color.parcel_surface);
  filter=state==null?0:state.getInt("filter",0);
  setTag("parcel-center");setOrientation(VERTICAL);setBackgroundColor(surface);setPadding(0,ui.dp(8),0,0);
  LinearLayout pinned=new LinearLayout(activity);pinned.setOrientation(VERTICAL);
  LinearLayout header=new LinearLayout(activity);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(ui.dp(16),ui.dp(4),ui.dp(16),0);pinned.addView(header);
  Button menu=ui.menuAction(header,openMenu);menu.getCompoundDrawables()[0].setTint(ink);TextView title=text("快递中心",24,ink);title.setGravity(Gravity.CENTER);title.setTypeface(Typeface.create("serif",Typeface.BOLD));header.addView(title,new LinearLayout.LayoutParams(0,-2,1));icon(header,R.drawable.ic_add,"添加快递",this::showActions);header.getChildAt(header.getChildCount()-1).setTag("quick-menu-button");
  feedback=text("",13,muted);feedback.setVisibility(GONE);feedback.setPadding(ui.dp(20),ui.dp(4),ui.dp(20),ui.dp(8));feedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);pinned.addView(feedback);
  pinned.setBackground(new android.graphics.drawable.ColorDrawable(surface));
  FrameLayout selector=new FrameLayout(activity);selector.setPadding(ui.dp(4),ui.dp(4),ui.dp(4),ui.dp(4));selector.setBackground(ui.bg(activity.getColor(R.color.parcel_soft),999));LinearLayout.LayoutParams selectorSize=new LinearLayout.LayoutParams(-1,-2);selectorSize.setMargins(ui.dp(16),ui.dp(12),ui.dp(16),ui.dp(4));pinned.addView(selector,selectorSize);
  capsule=new View(activity);capsule.setTag("parcel-filter-capsule");capsule.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);capsule.setBackground(ui.bg(card,999));selector.addView(capsule,new FrameLayout.LayoutParams(0,0));
  choices=new LinearLayout(activity);selector.addView(choices,new FrameLayout.LayoutParams(-1,-2));
  String[] names={"待取件","运输中","已取件"};for(int i=0;i<3;i++){final int selected=i;TextView tab=text(names[i],14,ink);tab.setTag("parcel-filter-"+i);tab.setGravity(Gravity.CENTER);tab.setMinHeight(ui.dp(48));tab.setPadding(ui.dp(4),ui.dp(12),ui.dp(4),ui.dp(12));tab.setFocusable(true);tab.setOnClickListener(v->selectFilter(selected));choices.addView(tab,new LinearLayout.LayoutParams(0,-2,1));tabs[i]=tab;}
  restoredScroll=state==null?null:state.getIntArray("filterScrollY");
  for(int i=0;i<3;i++){scrolls[i]=new ScrollView(activity);scrolls[i].setFillViewport(true);scrolls[i].setTag("parcel-filter-page-"+i);lists[i]=column();lists[i].setTag("parcel-list");lists[i].setPadding(ui.dp(18),0,ui.dp(18),ui.dp(24));scrolls[i].addView(lists[i]);}
  pager=new ViewPager(activity);pager.setTag("parcel-filter-pager");pager.setOffscreenPageLimit(2);addView(new ScrollHeader(activity,pinned,pager,surface,scrolls),new LinearLayout.LayoutParams(-1,0,1));
  pager.setAdapter(new PagerAdapter(){public int getCount(){return 3;}public boolean isViewFromObject(View v,Object o){return v==o;}public Object instantiateItem(ViewGroup host,int position){host.addView(scrolls[position]);return scrolls[position];}public void destroyItem(ViewGroup host,int position,Object o){host.removeView((View)o);}});
  pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){public void onPageScrolled(int position,float offset,int pixels){pagePosition=position+offset;moveCapsule();}public void onPageSelected(int position){markFilter(position);}});
  choices.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->moveCapsule());filter=Math.max(0,Math.min(2,filter));pagePosition=filter;pager.setCurrentItem(filter,false);markFilter(filter);
  snack=new LinearLayout(activity);snack.setGravity(Gravity.CENTER_VERTICAL);snack.setPadding(ui.dp(16),0,ui.dp(8),0);snack.setBackground(ui.bg(ink,16));snack.setVisibility(GONE);addView(snack,new LinearLayout.LayoutParams(-1,-2));
  ParcelRepository.IO.execute(()->{if(!closed)ParcelDatabase.get(activity).getInvalidationTracker().addObserver(observer);});
  if(state!=null&&state.containsKey("draftCode")){ParcelRecord restored=new ParcelRecord();restored.id=state.getLong("draftId");restored.code=state.getString("draftCode","");restored.station=state.getString("draftStation","");restored.trackingNumber=state.getString("draftTracking","");restored.carrier=state.getString("draftCarrier","");restored.state=state.getInt("draftState");restoreDraft(restored);}
 }
 void resume(){if(active)return;active=true;load();}
 void pause(){active=false;}
 void close(){
  closed=true;active=false;timer.removeCallbacksAndMessages(null);feedback.removeCallbacks(clearFeedback);
  ParcelDatabase.get(activity).getInvalidationTracker().removeObserver(observer);
  if(currentSheet!=null){currentSheet.setOnDismissListener(null);currentSheet.dismiss();currentSheet=null;}
 }
 void saveState(Bundle out){
  out.putInt("filter",filter);out.putIntArray("filterScrollY",new int[]{scrolls[0].getScrollY(),scrolls[1].getScrollY(),scrolls[2].getScrollY()});
  if(draft!=null&&currentSheet!=null&&currentSheet.isShowing()){out.putLong("draftId",draft.id);out.putString("draftCode",codeField.getText().toString());out.putString("draftStation",stationField.getText().toString());out.putString("draftTracking",trackingField.getText().toString());out.putString("draftCarrier",carrierField.getText().toString());out.putInt("draftState",draft.state);}
 }
 void handleIntent(Intent intent){
  if(intent==null)return;
  if(ParcelActivity.OPEN_SETTINGS.equals(intent.getAction())){intent.setAction(Intent.ACTION_MAIN);settings();return;}
  receive(intent);
 }
 void onActivityResult(int request,int result,Intent data){if(request==501&&result==Activity.RESULT_OK&&data!=null)ocr(data.getData());}
 void onRequestPermissionsResult(int request,String[] permissions,int[] grants){
  if(request!=502)return;
  if(grants.length==0||grants[0]!=android.content.pm.PackageManager.PERMISSION_GRANTED){ParcelNotificationListener.settings(activity).edit().putBoolean("reminders",false).apply();ParcelWorker.schedule(activity);message("未允许通知，提醒已关闭；快递管理和识别仍可使用");if(currentSheet!=null&&currentSheet.isShowing())settings();}
 }
 void settings(){
  LinearLayout settings=sheet("快递设置"),body=settingGroup(settings,"识别");SharedPreferences prefs=ParcelNotificationListener.settings(activity);
  body.addView(text("只在本机处理选中来源的通知，不读取短信数据库，不上传通知全文。关闭后停止识别。",14,muted));
  Switch recognition=new Switch(activity);recognition.setText("后台通知识别");recognition.setTextColor(ink);recognition.setMinHeight(ui.dp(56));recognition.setChecked(prefs.getBoolean("recognition",false));body.addView(recognition);
  recognition.setOnCheckedChangeListener((view,enabled)->{
   if(!enabled){prefs.edit().putBoolean("recognition",false).putString("epoch",UUID.randomUUID().toString()).apply();return;}
   new AlertDialog.Builder(activity).setTitle("开启快递通知识别？").setMessage("系统授权允许读取通知。软件只解析你选中的来源，仅保存取件码、地址、运单号和来源，不保存通知全文。验证码与动态身份码会排除。你可以随时关闭，并在系统中撤销通知访问。").setNegativeButton("取消",(d,w)->recognition.setChecked(false)).setOnCancelListener(d->recognition.setChecked(false)).setPositiveButton("继续授权",(d,w)->{prefs.edit().putBoolean("recognition",true).putString("epoch",UUID.randomUUID().toString()).apply();try{android.service.notification.NotificationListenerService.requestRebind(new ComponentName(activity,ParcelNotificationListener.class));activity.startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}catch(ActivityNotFoundException|SecurityException e){message("无法打开通知访问设置，请在系统设置中手动授权");}}).show();
  });
  boolean granted=androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(activity).contains(activity.getPackageName());button(body,"通知访问权限 · "+(granted?"已授权":"未授权"),()->{try{activity.startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}catch(ActivityNotFoundException|SecurityException e){message("请在系统设置中调整通知访问权限");}});
  if(prefs.getBoolean("recognitionError",false))body.addView(text("曾有识别结果保存失败，请检查剩余存储空间后重试。",13,muted));
  button(body,"选择通知来源",this::sources);
  body=settingGroup(settings,"提醒");
  Switch reminders=toggle(body,"快递提醒",prefs.getBoolean("reminders",false));reminders.setOnCheckedChangeListener((v,enabled)->{prefs.edit().putBoolean("reminders",enabled).apply();if(!enabled)ParcelNotifications.cancelAll(activity);if(enabled&&Build.VERSION.SDK_INT>=33&&activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)activity.requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},502);ParcelWorker.schedule(activity);});
  boolean notifications=androidx.core.app.NotificationManagerCompat.from(activity).areNotificationsEnabled();
  button(body,"系统通知 · "+(notifications?"已开启":"未开启"),()->{try{activity.startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,activity.getPackageName()));}catch(ActivityNotFoundException e){message("请在系统设置中调整课表镜的通知权限和快递提醒频道");}});
  body=settingGroup(settings,"物流查询");button(body,prefs.getString("proxy","").isEmpty()?"物流代理 · 未配置":"物流代理 · 已配置",this::proxySettings);
  body=settingGroup(settings,"小组件");
  Switch widget=toggle(body,"桌面小组件显示取件码与地址",prefs.getBoolean("widgetCodes",false));widget.setOnCheckedChangeListener((v,enabled)->{prefs.edit().putBoolean("widgetCodes",enabled).apply();ParcelRepository.IO.execute(()->ParcelWidget.updateAll(activity));});body.addView(text("开启后，桌面可见的人也能看到取件码。默认只显示待取数量。",13,muted));
  button(body,"添加快递小组件",()->{android.appwidget.AppWidgetManager manager=android.appwidget.AppWidgetManager.getInstance(activity);if(manager.isRequestPinAppWidgetSupported()){boolean accepted=manager.requestPinAppWidget(new ComponentName(activity,ParcelWidget.class),null,null);message(accepted?"已向桌面请求添加，请在系统面板中确认":"桌面未接受请求，请长按桌面手动添加快递中心组件");}else message("请长按桌面 → 小组件 → 课表镜 → 快递中心");});
  body=settingGroup(settings,"数据");body.addView(text("快递记录仅保存在本机，与课表和学习通账号独立。",14,muted));button(body,"删除全部快递数据",this::clear);
 }
 private void showActions(){
  LinearLayout body=sheet("添加快递");
  button(body,"添加快递",()->edit(new ParcelRecord())).setTag("parcel-add");
  button(body,"识别文本 / 截图",this::importSheet).setTag("parcel-import");

 }
 private void restoreDraft(ParcelRecord restored){ParcelRepository.IO.execute(()->{ParcelRecord existing=restored.id==0?restored:ParcelDatabase.get(activity).parcels().get(restored.id);activity.runOnUiThread(()->{if(closed||existing==null)return;existing.code=restored.code;existing.station=restored.station;existing.trackingNumber=restored.trackingNumber;existing.carrier=restored.carrier;existing.state=restored.state;edit(existing);});});}
 private void receive(Intent intent){
  if(intent==null||!Intent.ACTION_SEND.equals(intent.getAction()))return;
  try{String type=intent.getType();if(type!=null&&type.startsWith("image/")){android.net.Uri uri=Build.VERSION.SDK_INT>=33?intent.getParcelableExtra(Intent.EXTRA_STREAM,android.net.Uri.class):intent.getParcelableExtra(Intent.EXTRA_STREAM);ocr(uri);}else if("text/plain".equals(type)){CharSequence value=intent.getCharSequenceExtra(Intent.EXTRA_TEXT);if(value!=null&&value.length()<=ParcelRules.MAX_TEXT)recognize(value.toString(),"Android 分享文字");else message("分享文字为空或过长，请手动添加");}}
  catch(RuntimeException e){message("无法读取分享内容，请重新分享");}
 }
 private LinearLayout column(){LinearLayout view=new LinearLayout(activity);view.setOrientation(LinearLayout.VERTICAL);return view;}
 private TextView text(String value,int size,int color){return ui.text(value,size,color);}
 private void icon(LinearLayout host,int drawable,String name,Runnable action){Button b=ui.iconAction(host,drawable,name,action);b.setBackgroundTintList(null);b.setBackground(ui.bg(ink,24));b.getCompoundDrawables()[0].setTint(card);}
 private Button button(LinearLayout host,String name,Runnable action){Button b=ui.action(host,name,action);b.setBackgroundTintList(null);b.setTextColor(ink);b.setBackground(ui.bg(card,24));LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)b.getLayoutParams();lp.bottomMargin=ui.dp(4);lp.rightMargin=ui.dp(8);return b;}
 private void feedbackViewHide(){if(!closed)feedback.setVisibility(GONE);}
 private void message(String value){if(closed)return;if(!isShown()||currentSheet!=null&&currentSheet.isShowing()){Toast.makeText(activity,value,Toast.LENGTH_LONG).show();return;}feedback.removeCallbacks(clearFeedback);feedback.setText(value);feedback.setVisibility(VISIBLE);feedback.postDelayed(clearFeedback,6000);}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();pager.requestLayout();}
 private void selectFilter(int value){pager.setCurrentItem(value,android.animation.ValueAnimator.areAnimatorsEnabled());}
 private void markFilter(int value){filter=value;for(int i=0;i<3;i++){boolean selected=i==value;tabs[i].setSelected(selected);tabs[i].setTextColor(selected?ink:muted);tabs[i].setTypeface(null,selected?Typeface.BOLD:Typeface.NORMAL);scrolls[i].setImportantForAccessibility(selected?IMPORTANT_FOR_ACCESSIBILITY_AUTO:IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);}}
 private void moveCapsule(){if(choices.getWidth()==0)return;ViewGroup.LayoutParams size=capsule.getLayoutParams();int width=choices.getWidth()/3;if(size.width!=width||size.height!=choices.getHeight()){size.width=width;size.height=choices.getHeight();capsule.setLayoutParams(size);}capsule.setTranslationX(pagePosition*width);}
 private void load(){
  if(closed)return;
  ParcelRepository.IO.execute(()->{try{List<ParcelRecord> records=ParcelDatabase.get(activity).parcels().all();activity.runOnUiThread(()->{if(!closed)render(records);});}catch(RuntimeException e){activity.runOnUiThread(()->{if(!closed)message("本机快递数据读取失败，请重试；未清空原数据");});}});
 }
 private Button pill(LinearLayout host,String label,boolean primary,Runnable action){
  Button b=button(host,label,action);b.setTextSize(14);b.setTypeface(null,Typeface.BOLD);b.setPadding(ui.dp(8),ui.dp(10),ui.dp(8),ui.dp(10));b.setBackgroundTintList(null);b.setBackground(ui.bg(primary?ink:activity.getColor(R.color.parcel_soft),999));b.setTextColor(primary?card:ink);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(ui.dp(4),ui.dp(8),ui.dp(4),0);b.setLayoutParams(lp);return b;
 }
 private LinearLayout actionRow(LinearLayout host){
  LinearLayout row=new LinearLayout(activity){
   @Override protected void onMeasure(int width,int height){
    String[] labels=new String[getChildCount()];for(int i=0;i<labels.length;i++)labels[i]=((TextView)getChildAt(i)).getText().toString();
    boolean stack=labels.length>1&&!fitsRow(MeasureSpec.getSize(width)-getPaddingLeft()-getPaddingRight(),new String[0],labels,labels.length);setOrientation(stack?VERTICAL:HORIZONTAL);
    for(int i=0;i<getChildCount();i++){View child=getChildAt(i);LinearLayout.LayoutParams size=(LinearLayout.LayoutParams)child.getLayoutParams();if(size.width!=(stack?-1:0)||size.weight!=(stack?0:1)||size.bottomMargin!=0||size.leftMargin!=ui.dp(4)){size.width=stack?-1:0;size.weight=stack?0:1;size.setMargins(ui.dp(4),ui.dp(8),ui.dp(4),0);child.setLayoutParams(size);}}super.onMeasure(width,height);
   }
  };host.addView(row,new LinearLayout.LayoutParams(-1,-2));return row;
 }
 // Weighted pills shrink below their text once space runs out, so measure first and stack rows that cannot stay single-line.
 private boolean fitsRow(int available,String[] prefixes,String[] pills,int columns){
  float prefix=0;for(String value:prefixes)prefix+=measure(value,12,false);
  float widest=0;for(String value:pills)widest=Math.max(widest,measure(value,14,true));
  return widest+ui.dp(18)<=(available-prefix-ui.dp(8*columns))/(float)columns;
 }
 private float measure(String value,int size,boolean bold){android.graphics.Paint probe=new android.graphics.Paint();probe.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);probe.setTextSize(size*activity.getResources().getDisplayMetrics().scaledDensity);return probe.measureText(value);}
 private int listInnerWidth(LinearLayout list){int width=list.getWidth()>0?list.getWidth()-list.getPaddingLeft()-list.getPaddingRight():activity.getResources().getDisplayMetrics().widthPixels-ui.dp(36);return width;}
 private LinearLayout card(LinearLayout host,boolean pending){LinearLayout row=column();row.setPadding(ui.dp(16),ui.dp(14),ui.dp(16),ui.dp(14));android.graphics.drawable.GradientDrawable bg=ui.bg(card,20);if(pending)bg.setStroke(ui.dp(1),muted,ui.dp(5),ui.dp(4));row.setBackground(bg);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=ui.dp(12);host.addView(row,lp);return row;}
 private void render(List<ParcelRecord> records){
  String[] names={"待取件","运输中","已取件"};int[] counts=new int[3];for(ParcelRecord p:records)counts[p.confirmed?p.state:0]++;
  for(int page=0;page<3;page++){
   final int index=page;LinearLayout list=lists[page];int y=restoredScroll!=null&&restoredScroll.length==3?restoredScroll[page]:scrolls[page].getScrollY();tabs[page].setText(names[page]+" "+counts[page]);list.removeAllViews();
   if(page==0){LinearLayout identity=card(list,false);identity.setPadding(ui.dp(8),ui.dp(4),ui.dp(8),ui.dp(12));LinearLayout strip=new LinearLayout(activity);strip.setGravity(Gravity.CENTER_VERTICAL);identity.addView(strip);String[] platforms={"淘宝","菜鸟","拼多多"};boolean large=activity.getResources().getConfiguration().fontScale>=1.3f||!fitsRow(listInnerWidth(list)-ui.dp(16),new String[]{"身份码"},platforms,3);if(large)strip.setOrientation(VERTICAL);strip.addView(text("身份码",12,muted));for(int i=0;i<3;i++){final int platform=i;Button entry=pill(strip,platforms[i],false,()->ParcelIdentity.open(activity,platform));entry.setContentDescription("打开"+platforms[i]+"官方身份码");entry.setTag("parcel-identity-"+i);if(large){LinearLayout.LayoutParams entrySize=(LinearLayout.LayoutParams)entry.getLayoutParams();entrySize.width=-1;entrySize.weight=0;}}}
   TreeMap<String,List<ParcelRecord>> groups=new TreeMap<>((a,b)->a.startsWith("待确认")!=b.startsWith("待确认")?(a.startsWith("待确认")?-1:1):a.compareTo(b));
   for(ParcelRecord p:records)if(!p.confirmed?page==0:p.state==page)groups.computeIfAbsent(!p.confirmed?"待确认 · 识别结果":page==0?p.group():page==1?"运输中":"已取件",ignored->new ArrayList<>()).add(p);
   if(groups.isEmpty()){
    LinearLayout empty=card(list,false);empty.setPadding(ui.dp(16),ui.dp(32),ui.dp(16),ui.dp(28));TextView title=text(page==0?"没有待取件的快递":page==1?"暂无运输中的快递":"还没有已取件记录",18,ink);title.setGravity(Gravity.CENTER);empty.addView(title);
    if(page!=2){TextView hint=text(page==0?"手动添加，或开启通知识别自动整理取件信息":"添加运单号后，可查询物流进度",13,muted);hint.setGravity(Gravity.CENTER);hint.setPadding(0,ui.dp(12),0,ui.dp(8));empty.addView(hint);LinearLayout actions=new LinearLayout(activity);empty.addView(actions);String recognizeLabel=ParcelNotificationListener.settings(activity).getBoolean("recognition",false)?"识别设置":"开启通知识别";boolean stack=page==0&&!fitsRow(listInnerWidth(list)-ui.dp(32),new String[0],new String[]{"添加快递",recognizeLabel},2);if(stack)actions.setOrientation(VERTICAL);pill(actions,"添加快递",true,this::showActions);if(page==0){Button recognize=pill(actions,recognizeLabel,false,this::settings);if(stack)for(int i=0;i<actions.getChildCount();i++){LinearLayout.LayoutParams actionSize=(LinearLayout.LayoutParams)actions.getChildAt(i).getLayoutParams();actionSize.width=-1;actionSize.weight=0;}}}
   }
   for(Map.Entry<String,List<ParcelRecord>> group:groups.entrySet()){
    TextView heading=text(group.getKey()+"  ·  "+group.getValue().size()+" 件",13,muted);heading.setTypeface(null,Typeface.BOLD);heading.setPadding(ui.dp(2),ui.dp(8),0,ui.dp(12));list.addView(heading);
    if(group.getKey().startsWith("待确认")){TextView note=text("识别信息可能有误，请核对后确认",12,muted);note.setPadding(0,0,0,ui.dp(12));list.addView(note);}
    LinearLayout done=page==2?card(list,false):null;
    for(ParcelRecord p:group.getValue()){
     if(page==2){LinearLayout row=column();row.setPadding(0,ui.dp(12),0,ui.dp(12));done.addView(row);TextView code=text((p.code.isEmpty()?p.trackingNumber:p.code)+"  "+p.carrier,16,muted);code.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);row.addView(code);row.addView(text(p.group()+" · "+date(p.updatedAt),12,muted));row.setMinimumHeight(ui.dp(48));row.setFocusable(true);row.setContentDescription("管理已取件快递 "+p.code+" "+p.group());row.setOnClickListener(v->actions(p));ui.ripple(row);continue;}
     LinearLayout row=card(list,!p.confirmed);row.setTag("parcel-record-"+p.id);TextView station=text(page==0?p.group():p.carrier.isEmpty()?"快递运输中":p.carrier,14,ink);station.setTypeface(null,Typeface.BOLD);row.addView(station);
     if(page==0){
      TextView code=text(p.code.isEmpty()?"取件码待补充":p.code,28,ink);code.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);code.setPadding(0,ui.dp(8),0,ui.dp(4));code.setMinHeight(ui.dp(48));code.setTag("parcel-code-"+p.id);if(!p.code.isEmpty()){code.setFocusable(true);code.setContentDescription("取件码 "+p.code+"，点击复制");code.setOnClickListener(v->copy(p.code));ui.ripple(code);}row.addView(code);
      if(!p.code.isEmpty())row.addView(text("点按取件码复制",12,muted));
      if(!p.carrier.isEmpty())row.addView(text(p.carrier,13,muted));
      if(!p.trackingNumber.isEmpty())row.addView(text("运单号 "+p.trackingNumber,12,muted));
      if(!p.confirmed)row.addView(text("识别自"+p.source+"，请核对",13,muted));
      LinearLayout actions=actionRow(row);
      if(p.confirmed){pill(actions,"已取件",true,()->complete(p.id));pill(actions,"管理",false,()->actions(p));}
      else{if(!p.code.isEmpty()&&!p.station.isEmpty())pill(actions,"确认无误",true,()->confirm(p.id));pill(actions,"核对",false,()->edit(p));}
     }else renderTransit(row,p);
    }
   }
   scrolls[page].post(()->{if(!closed)scrolls[index].scrollTo(0,y);});
  }
  restoredScroll=null;
 }
 private String date(long time){return time==0?"":android.text.format.DateFormat.format("MM-dd HH:mm",time).toString();}
 private void renderTransit(LinearLayout row,ParcelRecord p){
  row.addView(text("运单号 "+p.trackingNumber,12,muted));
  org.json.JSONArray events=new org.json.JSONArray();try{if(!p.trackingEvents.isEmpty())events=new org.json.JSONArray(p.trackingEvents);}catch(org.json.JSONException ignored){}
  if(events.length()>0){org.json.JSONObject latest=events.optJSONObject(0);if(latest!=null){TextView summary=text(latest.optString("description"),17,ink);summary.setTypeface(null,Typeface.BOLD);summary.setPadding(0,ui.dp(12),0,ui.dp(4));row.addView(summary);row.addView(text(latest.optString("time"),12,muted));}
   LinearLayout timeline=column();row.addView(timeline);timeline.setVisibility(expandedTracking==p.id?VISIBLE:GONE);for(int i=0;i<events.length();i++){org.json.JSONObject e=events.optJSONObject(i);if(e==null)continue;TextView event=text("•  "+e.optString("description")+"\n    "+e.optString("time"),13,muted);event.setPadding(0,ui.dp(10),0,ui.dp(4));timeline.addView(event);}
   Button expand=button(row,expandedTracking==p.id?"收起物流记录":"展开物流记录",()->{});expand.setOnClickListener(v->{boolean open=timeline.getVisibility()!=VISIBLE;timeline.setVisibility(open?VISIBLE:GONE);expandedTracking=open?p.id:0;expand.setText(open?"收起物流记录":"展开物流记录");});row.setFocusable(true);row.setOnClickListener(v->expand.performClick());ui.ripple(row);
  }else{TextView empty=text("暂无物流动态",16,muted);empty.setPadding(0,ui.dp(12),0,ui.dp(4));row.addView(empty);}
  if(!p.trackingError.isEmpty())row.addView(text(p.trackingError,13,muted));
  boolean configured=!ParcelNotificationListener.settings(activity).getString("proxy","").isEmpty();if(!configured)row.addView(text("配置物流代理后可查进度，已有物流记录仍可查看",13,muted));
  LinearLayout actions=actionRow(row);pill(actions,configured?"查询物流":"去配置",true,configured?()->tracking(p):this::proxySettings);pill(actions,"管理",false,()->actions(p));
 }
 private void confirm(long id){ParcelRepository.IO.execute(()->{try{ParcelRecord p=ParcelDatabase.get(activity).parcels().get(id);if(p==null||p.confirmed)return;p.confirmed=true;p.state=ParcelRecord.READY;new ParcelRepository(activity).save(p);activity.runOnUiThread(()->{if(!closed){message("已确认，放入待取件");load();}});}catch(RuntimeException e){activity.runOnUiThread(()->message("确认失败，请核对信息后保存"));}});}
 private void copy(String value){ClipData clip=ClipData.newPlainText("取件码",value);PersistableBundle extras=new PersistableBundle();extras.putBoolean("android.content.extra.IS_SENSITIVE",true);clip.getDescription().setExtras(extras);((ClipboardManager)activity.getSystemService(Activity.CLIPBOARD_SERVICE)).setPrimaryClip(clip);message("取件码已复制");}
 private LinearLayout sheet(String title){
  if(currentSheet!=null)currentSheet.dismiss();LinearLayout shell=column();shell.setPadding(ui.dp(20),ui.dp(16),ui.dp(20),ui.dp(8));shell.setBackground(ui.bg(card,20));
  LinearLayout header=new LinearLayout(activity);header.setGravity(Gravity.CENTER_VERTICAL);TextView label=text(title,20,ink);label.setTypeface(null,Typeface.BOLD);header.addView(label,new LinearLayout.LayoutParams(0,-2,1));button(header,"关闭",()->currentSheet.dismiss());shell.addView(header);
  ScrollView scroll=new ScrollView(activity);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout body=column();body.setPadding(0,ui.dp(12),0,ui.dp(20));scroll.addView(body);
  currentSheet=new AlertDialog.Builder(activity).setView(shell).create();currentSheet.setOnDismissListener(d->{draft=null;});currentSheet.show();Window w=currentSheet.getWindow();w.setBackgroundDrawableResource(android.R.color.transparent);w.setGravity(Gravity.BOTTOM);w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);int available=getHeight()-getPaddingTop()-getPaddingBottom();w.setLayout(-1,(available>0?available:activity.getResources().getDisplayMetrics().heightPixels)/2);return body;
 }
 private EditText field(LinearLayout body,String label,String value){TextView name=text(label,14,muted);name.setPadding(0,ui.dp(10),0,ui.dp(6));body.addView(name);EditText input=new EditText(activity);input.setText(value);input.setTextSize(16);input.setTextColor(ink);input.setSingleLine(true);input.setMinHeight(ui.dp(52));input.setPadding(ui.dp(12),ui.dp(8),ui.dp(12),ui.dp(8));input.setBackgroundResource(R.drawable.form_field);input.setInputType(InputType.TYPE_CLASS_TEXT);input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);input.setContentDescription(label);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(200)});body.addView(input,new LinearLayout.LayoutParams(-1,-2));return input;}
 private void edit(ParcelRecord p){
  LinearLayout body=sheet(p.confirmed?(p.id==0?"添加快递":"编辑快递"):"确认识别结果");draft=p;
  body.addView(text(p.state==ParcelRecord.DONE?"修改信息后仍保留在已取件记录中。":"填写取件码后归入待取件；只有运单号时归入运输中。取件后请在列表点“已取件”。",13,muted));
  codeField=field(body,"取件码（与运单号分开）",p.code);codeField.setTag("parcel-editor-code");stationField=field(body,"驿站 / 地址",p.station);trackingField=field(body,"运单号（选填）",p.trackingNumber);carrierField=field(body,"快递公司 / 查询编码（选填）",p.carrier);
  TextView error=text("",14,ui.DANGER);error.setVisibility(GONE);error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);AlertDialog editor=currentSheet;
  Button save=ui.action(body,p.confirmed?"保存":"保存并确认",()->{
   if(saving)return;ParcelRecord value=new ParcelRecord();value.id=p.id;value.code=codeField.getText().toString();value.station=stationField.getText().toString();value.trackingNumber=trackingField.getText().toString();value.carrier=carrierField.getText().toString();value.state=p.state==ParcelRecord.DONE?ParcelRecord.DONE:value.code.trim().isEmpty()?ParcelRecord.TRANSIT:ParcelRecord.READY;
   value.source=p.source;value.rule=p.rule;value.confidence=p.confidence;value.confirmed=true;value.remindAt=p.remindAt;value.remindedAt=p.remindedAt;
   if(value.trackingNumber.equals(p.trackingNumber)&&value.carrier.equals(p.carrier)){value.trackingState=p.trackingState;value.trackingEvents=p.trackingEvents;value.lastQueryAt=p.lastQueryAt;value.retryAt=p.retryAt;value.trackingError=p.trackingError;}
   error.setVisibility(GONE);saving=true;Button submit=editor.getWindow().getDecorView().findViewWithTag("parcel-editor-save");submit.setEnabled(false);submit.setText("保存中…");ParcelRepository.IO.execute(()->{String failure=null;try{new ParcelRepository(activity).save(value);}catch(IllegalArgumentException e){failure=e.getMessage();}catch(RuntimeException e){failure="保存失败，请重试；原数据仍保留";}String result=failure;activity.runOnUiThread(()->{saving=false;if(closed)return;if(result==null){editor.dismiss();selectFilter(value.state);message("已保存到"+new String[]{"待取件","运输中","已取件"}[value.state]);load();}else{submit.setEnabled(true);submit.setText(p.confirmed?"保存":"保存并确认");error.setText(result);error.setVisibility(VISIBLE);}});});
  });save.setTag("parcel-editor-save");
  body.removeView(save);LinearLayout shell=(LinearLayout)body.getParent().getParent();shell.addView(error,new LinearLayout.LayoutParams(-1,-2));LinearLayout footer=actionRow(shell);footer.setPadding(0,ui.dp(4),0,ui.dp(12));footer.setTag("parcel-editor-actions");pill(footer,"取消",false,editor::dismiss);footer.addView(save);save.setTextColor(card);save.setBackgroundTintList(null);save.setBackground(ui.bg(ink,999));save.setTextSize(14);save.setTypeface(null,Typeface.BOLD);save.setPadding(ui.dp(8),ui.dp(10),ui.dp(8),ui.dp(10));
  int available=getHeight()>0?getHeight():activity.getResources().getDisplayMetrics().heightPixels;editor.getWindow().setLayout(-1,Math.max(ui.dp(240),available-ui.dp(48)));
 }
 private void actions(ParcelRecord p){LinearLayout body=sheet("快递管理");button(body,p.confirmed?"编辑":"核对并确认",()->edit(p));
  if(p.confirmed){button(body,"物流记录 / 查询",()->tracking(p));if(p.state==ParcelRecord.READY)button(body,"未领取提醒",()->reminder(p));}
  button(body,"删除",()->delete(p));}
 private LinearLayout settingGroup(LinearLayout host,String title){TextView heading=text(title,13,muted);heading.setTypeface(null,Typeface.BOLD);heading.setPadding(0,ui.dp(16),0,ui.dp(8));host.addView(heading);LinearLayout group=column();group.setPadding(ui.dp(12),ui.dp(8),ui.dp(12),ui.dp(12));group.setBackground(ui.bg(surface,18));host.addView(group,new LinearLayout.LayoutParams(-1,-2));return group;}
 private void undoBar(String label,Runnable undo){
  timer.removeCallbacksAndMessages(null);snack.removeAllViews();TextView value=text(label,14,card);value.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);snack.addView(value,new LinearLayout.LayoutParams(0,-2,1));Button button=ui.action(snack,"撤销",()->{snack.setVisibility(GONE);timer.removeCallbacksAndMessages(null);undo.run();});button.setTextColor(card);button.setBackgroundTintList(null);button.setBackground(ui.bg(ink,24));LinearLayout.LayoutParams size=(LinearLayout.LayoutParams)button.getLayoutParams();size.topMargin=0;size.bottomMargin=0;snack.setVisibility(VISIBLE);timer.postDelayed(()->snack.setVisibility(GONE),5000);
 }
 private void complete(long id){ParcelRepository.IO.execute(()->{try{ParcelRecord p=ParcelDatabase.get(activity).parcels().get(id);if(p==null||!p.confirmed||p.state!=ParcelRecord.READY)return;int previous=p.state;long remindAt=p.remindAt,remindedAt=p.remindedAt;p.state=ParcelRecord.DONE;p.remindAt=0;new ParcelRepository(activity).save(p);long changedAt=p.updatedAt;activity.runOnUiThread(()->{if(closed)return;load();undoBar("已标记取件",()->ParcelRepository.IO.execute(()->{try{new ParcelRepository(activity).undoComplete(id,changedAt,previous,remindAt,remindedAt);activity.runOnUiThread(this::load);}catch(RuntimeException e){activity.runOnUiThread(()->message("恢复失败，记录可能已变更"));}}));});}catch(RuntimeException e){activity.runOnUiThread(()->message("保存失败，请重试"));}});}
 private void delete(ParcelRecord p){ParcelRepository.IO.execute(()->{try{ParcelRepository repository=new ParcelRepository(activity);ParcelRecord removed=ParcelDatabase.get(activity).parcels().get(p.id);if(removed==null)return;repository.delete(p.id);activity.runOnUiThread(()->{if(closed)return;if(currentSheet!=null)currentSheet.dismiss();load();undoBar("已删除快递",()->ParcelRepository.IO.execute(()->{try{repository.restoreDeleted(removed);activity.runOnUiThread(this::load);}catch(RuntimeException e){activity.runOnUiThread(()->message("恢复失败，已有相同快递或记录已变更"));}}));});}catch(RuntimeException e){activity.runOnUiThread(()->message("删除失败，请重试"));}});}
 private void sources(){
  LinearLayout body=sheet("通知来源");Set<String> selected=new HashSet<>(ParcelNotificationListener.sources(activity));List<String> packages=new ArrayList<>(Arrays.asList(ParcelNotificationListener.PACKAGES));List<String> names=new ArrayList<>(Arrays.asList(ParcelNotificationListener.NAMES));String sms=android.provider.Telephony.Sms.getDefaultSmsPackage(activity);if(sms!=null&&!packages.contains(sms)){packages.add(sms);names.add("默认短信应用的通知（不读短信库）");}
  for(int i=0;i<packages.size();i++){String pkg=packages.get(i);CheckBox choice=new CheckBox(activity);choice.setText(names.get(i));choice.setTextColor(ink);choice.setMinHeight(ui.dp(52));choice.setChecked(selected.contains(pkg));choice.setOnCheckedChangeListener((v,checked)->{if(checked)selected.add(pkg);else selected.remove(pkg);ParcelNotificationListener.settings(activity).edit().putStringSet("sources",new HashSet<>(selected)).apply();});body.addView(choice);}
 }
 private void importSheet(){LinearLayout body=sheet("识别取件信息");EditText input=field(body,"粘贴快递通知（仅本机识别）","");input.setSingleLine(false);input.setMinLines(3);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(ParcelRules.MAX_TEXT)});button(body,"识别文字",()->recognize(input.getText().toString(),"粘贴文字"));button(body,"选择截图",()->{try{activity.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),501);}catch(ActivityNotFoundException e){message("系统没有可用的图片选择器");}});}
 private void ocr(android.net.Uri uri){String epoch=ParcelNotificationListener.settings(activity).getString("epoch","");message("正在本机识别截图…");ParcelOcr.read(activity.getApplicationContext(),uri,(value,error)->activity.runOnUiThread(()->{if(closed||!epoch.equals(ParcelNotificationListener.settings(activity).getString("epoch","")))return;if(error!=null)message(error);else recognize(value,"截图 OCR");}));}
 private void recognize(String input,String source){
  List<ParcelRules.Candidate> found=ParcelRules.extract(input);if(found.isEmpty()){message("没有识别到明确的取件码；请检查内容或手动添加");return;}
  String epoch=ParcelNotificationListener.settings(activity).getString("epoch","");ParcelRepository.IO.execute(()->{int added=0;try{if(!epoch.equals(ParcelNotificationListener.settings(activity).getString("epoch","")))return;for(ParcelRules.Candidate c:found)if(new ParcelRepository(activity).ingest(ParcelNotificationListener.record(c,source,true)))added++;int count=added;activity.runOnUiThread(()->{if(closed)return;if(currentSheet!=null)currentSheet.dismiss();selectFilter(0);message(count==0?"识别结果已存在，未重复添加":"已识别 "+count+" 条，请在待确认列表核对后保存");load();});}catch(RuntimeException e){activity.runOnUiThread(()->{if(!closed)message("识别结果保存失败，请重试");});}});
 }
 private void tracking(ParcelRecord p){
  LinearLayout body=sheet("物流记录");body.addView(text(p.trackingNumber.isEmpty()?"请先编辑并填写运单号":p.trackingNumber,17,ink));
  if(!p.trackingState.isEmpty())body.addView(text(ParcelTracking.stateLabel(p.trackingState),15,ink));
  if(!p.trackingError.isEmpty())body.addView(text(p.trackingError,14,muted));
  try{if(!p.trackingEvents.isEmpty()){org.json.JSONArray events=new org.json.JSONArray(p.trackingEvents);for(int i=0;i<events.length();i++){org.json.JSONObject e=events.getJSONObject(i);TextView row=text(e.getString("time")+"\n"+e.getString("description"),15,ink);row.setPadding(0,ui.dp(12),0,ui.dp(12));body.addView(row);}}}catch(org.json.JSONException e){body.addView(text("本机物流记录无法显示，可重新查询",14,muted));}
  body.addView(text("查询仅向你配置的 HTTPS 代理发送运单号和快递公司。物流显示已签收时仍需你确认取件。",13,muted));
  if(p.trackingNumber.isEmpty())button(body,"补充运单号",()->edit(p));
  else if(p.state!=ParcelRecord.DONE&&ParcelNotificationListener.settings(activity).getString("proxy","").isEmpty())button(body,"配置物流代理",this::proxySettings);
  else if(p.state!=ParcelRecord.DONE)button(body,"查询物流",()->{message("正在查询物流…");ParcelTracking.NETWORK.execute(()->{String result=ParcelTracking.query(activity.getApplicationContext(),p.id);activity.runOnUiThread(()->{if(closed)return;message(result);if(currentSheet!=null)currentSheet.dismiss();load();});});});
 }
 private void reminder(ParcelRecord p){
  LinearLayout body=sheet("未领取提醒");body.addView(text("提醒可能因省电、后台限制而延迟。请先在快递管理中开启提醒。",14,muted));
  button(body,"选择日期与时间",()->{java.util.Calendar date=java.util.Calendar.getInstance();new DatePickerDialog(activity,(picker,y,m,d)->new TimePickerDialog(activity,(clock,h,min)->{date.set(y,m,d,h,min,0);date.set(java.util.Calendar.MILLISECOND,0);if(date.getTimeInMillis()<=System.currentTimeMillis()){message("请选择未来的提醒时间");return;}saveReminder(p.id,date.getTimeInMillis());},date.get(java.util.Calendar.HOUR_OF_DAY),date.get(java.util.Calendar.MINUTE),true).show(),date.get(java.util.Calendar.YEAR),date.get(java.util.Calendar.MONTH),date.get(java.util.Calendar.DAY_OF_MONTH)).show();});
  button(body,"取消本条提醒",()->saveReminder(p.id,0));
 }
 private void saveReminder(long id,long time){ParcelRepository.IO.execute(()->{try{ParcelDatabase db=ParcelDatabase.get(activity);db.runInTransaction(()->{ParcelRecord p=db.parcels().get(id);if(p==null||p.state!=ParcelRecord.READY)return;p.remindAt=time;p.remindedAt=0;db.parcels().update(p);});ParcelWorker.schedule(activity);activity.runOnUiThread(()->{if(closed)return;currentSheet.dismiss();message(time==0?"提醒已取消":"提醒时间已保存；请确认系统允许通知");});}catch(RuntimeException e){activity.runOnUiThread(()->message("提醒保存失败，请重试"));}});}
 private Switch toggle(LinearLayout body,String label,boolean checked){Switch view=new Switch(activity);view.setText(label);view.setTextColor(ink);view.setMinHeight(ui.dp(56));view.setChecked(checked);body.addView(view,new LinearLayout.LayoutParams(-1,-2));return view;}
 private void proxySettings(){
  LinearLayout body=sheet("物流服务代理");SharedPreferences prefs=ParcelNotificationListener.settings(activity);body.addView(text("默认不联网。仅连接你信任的 HTTPS 代理，由服务端保管快递100 / 快递鸟密钥；查询只发送运单号和快递公司，不发送取件码、地址、通知或图片。没有代理时仍可离线管理快递。",14,muted));
  EditText endpoint=field(body,"HTTPS 代理地址（留空关闭）",prefs.getString("proxy",""));endpoint.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2048)});endpoint.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
  Switch background=toggle(body,"后台查询（约每 6 小时）",prefs.getBoolean("backgroundTracking",false));TextView error=text("",14,muted);body.addView(error);
  button(body,"保存",()->{String value=endpoint.getText().toString().trim();try{if(!value.isEmpty())value=ProxyTrackingProvider.validateEndpoint(value);else if(background.isChecked())throw new IllegalArgumentException("后台查询需要先配置代理地址");prefs.edit().putString("proxy",value).putBoolean("backgroundTracking",background.isChecked()).putLong("trackingRetryAt",0).putString("epoch",UUID.randomUUID().toString()).apply();ParcelWorker.schedule(activity);currentSheet.dismiss();message(value.isEmpty()?"物流联网已关闭":"代理设置已保存；尚未验证真实物流服务");}catch(IllegalArgumentException e){error.setText(e.getMessage());}});
 }
 private void clear(){new AlertDialog.Builder(activity).setTitle("删除全部快递数据？").setMessage("将关闭后台识别和查询，删除快递、识别候选、去重记录和快递设置，不影响课表。系统通知访问可在系统设置中撤销。").setNegativeButton("取消",null).setPositiveButton("全部删除",(dialog,which)->{snack.setVisibility(GONE);timer.removeCallbacksAndMessages(null);ParcelNotificationListener.settings(activity).edit().clear().putString("epoch",UUID.randomUUID().toString()).apply();ParcelWorker.schedule(activity);ParcelNotifications.cancelAll(activity);ParcelRepository.IO.execute(()->{try{new ParcelRepository(activity).clear();activity.runOnUiThread(()->{if(closed)return;currentSheet.dismiss();message("快递数据已删除，后台功能已关闭");load();});}catch(RuntimeException e){activity.runOnUiThread(()->message("删除失败，请重试；后台功能已关闭"));}});}).show();}
}
