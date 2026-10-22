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

public class ParcelActivity extends Activity {
 static final String OPEN_SETTINGS="cn.courselens.PARCEL_SETTINGS";
 private SecondaryUi ui;
 private SideNavigation sideNavigation;
 private LinearLayout root,list;
 private TextView feedback;
 private final TextView[] tabs=new TextView[3];
 private int filter,ink,muted,card,surface;
 private boolean saving;
 private AlertDialog currentSheet;
 private ParcelRecord draft;
 private EditText codeField,stationField,trackingField,carrierField;
 private Spinner stateField;
 private final InvalidationTracker.Observer observer=new InvalidationTracker.Observer("parcels"){
  @Override public void onInvalidated(Set<String> tables){runOnUiThread(()->{if(!isDestroyed())load();});}
 };
 @Override protected void attachBaseContext(Context base){super.attachBaseContext(Appearance.wrap(base));}
 @Override public void onCreate(Bundle state){
  super.onCreate(state);ui=new SecondaryUi(this);ink=getColor(R.color.parcel_ink);muted=getColor(R.color.parcel_muted);card=getColor(R.color.parcel_card);surface=getColor(R.color.parcel_surface);
  filter=state==null?0:state.getInt("filter",0);root=column();root.setBackgroundColor(surface);root.setPadding(0,ui.dp(8),0,0);root.setTag("parcel-center");sideNavigation=new SideNavigation(this,root,surface,card,ink,this::selectDrawerPage,this::openDrawerSettings);sideNavigation.select(2);setContentView(sideNavigation);
  root.setOnApplyWindowInsetsListener((view,insets)->{view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop()+ui.dp(8),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
  LinearLayout pinned=column();LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(ui.dp(16),ui.dp(4),ui.dp(16),0);pinned.addView(header);
  Button menu=ui.menuAction(header,()->sideNavigation.open(2));menu.getCompoundDrawables()[0].setTint(ink);TextView title=text("快递中心",24,ink);title.setGravity(Gravity.CENTER);title.setTypeface(null,Typeface.BOLD);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));icon(header,R.drawable.ic_more,"快递操作",this::showActions);header.getChildAt(header.getChildCount()-1).setTag("quick-menu-button");
  feedback=text("仅保存在本机",13,muted);feedback.setPadding(ui.dp(20),ui.dp(4),ui.dp(20),ui.dp(8));feedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);pinned.addView(feedback);
  LinearLayout selector=new LinearLayout(this);selector.setPadding(ui.dp(16),0,ui.dp(16),ui.dp(8));pinned.addView(selector);
  String[] names={"待取件","运输中","已完成"};for(int i=0;i<3;i++){final int selected=i;TextView tab=text(names[i],15,ink);tab.setTag("parcel-filter-"+i);tab.setGravity(Gravity.CENTER);tab.setMinHeight(ui.dp(48));tab.setPadding(ui.dp(4),ui.dp(12),ui.dp(4),ui.dp(12));tab.setFocusable(true);tab.setOnClickListener(v->{filter=selected;load();});selector.addView(tab,new LinearLayout.LayoutParams(0,-2,1));tabs[i]=tab;}
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(new ScrollHeader(this,pinned,scroll,surface,scroll),new LinearLayout.LayoutParams(-1,0,1));list=column();list.setTag("parcel-list");list.setPadding(ui.dp(20),0,ui.dp(20),ui.dp(24));scroll.addView(list);
  root.addView(new AppNavigation(this,surface,ink,muted,2,this::selectBottomPage),new LinearLayout.LayoutParams(-1,-2));
  ParcelRepository.IO.execute(()->{if(!isDestroyed())ParcelDatabase.get(this).getInvalidationTracker().addObserver(observer);});
  getWindow().setStatusBarColor(surface);getWindow().setNavigationBarColor(surface);Appearance.systemBars(this);
  if(state!=null&&state.containsKey("draftCode")){ParcelRecord restored=new ParcelRecord();restored.id=state.getLong("draftId");restored.code=state.getString("draftCode","");restored.station=state.getString("draftStation","");restored.trackingNumber=state.getString("draftTracking","");restored.carrier=state.getString("draftCarrier","");restored.state=state.getInt("draftState");restoreDraft(restored);}
  if(state==null){receive(getIntent());openSettings(getIntent());}
 }
 private void openSettings(Intent intent){if(OPEN_SETTINGS.equals(intent.getAction())){intent.setAction(Intent.ACTION_MAIN);settings();}}
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);openSettings(intent);}
 private void restoreDraft(ParcelRecord restored){ParcelRepository.IO.execute(()->{ParcelRecord existing=restored.id==0?restored:ParcelDatabase.get(this).parcels().get(restored.id);runOnUiThread(()->{if(isDestroyed()||existing==null)return;existing.code=restored.code;existing.station=restored.station;existing.trackingNumber=restored.trackingNumber;existing.carrier=restored.carrier;existing.state=restored.state;edit(existing);});});}
 private void showActions(){
  LinearLayout body=sheet("快递操作");
  button(body,"添加快递",()->edit(new ParcelRecord())).setTag("parcel-add");
  button(body,"识别文本 / 截图",this::importSheet).setTag("parcel-import");
  button(body,"官方身份码",()->{currentSheet.dismiss();identity();}).setTag("parcel-identity");
 }
 private void selectDrawerPage(int selected){
  if(selected==2)return;
  sideNavigation.afterClose(()->{if(!isFinishing()&&!isDestroyed())startActivity(new Intent(this,MainActivity.class).setAction(MainActivity.OPEN_TAB).putExtra("primaryTab",selected).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));});
 }
 private void openDrawerSettings(){sideNavigation.afterClose(()->{if(!isFinishing()&&!isDestroyed())startActivity(new Intent(this,MainActivity.class).setAction(MainActivity.OPEN_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));});}
 private void selectBottomPage(int selected){if(selected==2)return;startActivity(new Intent(this,MainActivity.class).setAction(MainActivity.OPEN_TAB).putExtra("primaryTab",selected).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NO_ANIMATION));overridePendingTransition(0,0);}
 @Override public void onBackPressed(){if(sideNavigation!=null&&sideNavigation.close(true))return;super.onBackPressed();}
 @Override protected void onResume(){super.onResume();load();}
 @Override protected void onDestroy(){if(sideNavigation!=null)sideNavigation.close(false);ParcelDatabase.get(this).getInvalidationTracker().removeObserver(observer);if(currentSheet!=null)currentSheet.dismiss();super.onDestroy();}
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putInt("filter",filter);if(draft!=null&&currentSheet!=null&&currentSheet.isShowing()){out.putLong("draftId",draft.id);out.putString("draftCode",codeField.getText().toString());out.putString("draftStation",stationField.getText().toString());out.putString("draftTracking",trackingField.getText().toString());out.putString("draftCarrier",carrierField.getText().toString());out.putInt("draftState",stateField.getSelectedItemPosition());}}
 private LinearLayout column(){LinearLayout view=new LinearLayout(this);view.setOrientation(LinearLayout.VERTICAL);return view;}
 private TextView text(String value,int size,int color){return ui.text(value,size,color);}
 private void icon(LinearLayout host,int drawable,String name,Runnable action){Button b=ui.iconAction(host,drawable,name,action);b.getCompoundDrawables()[0].setTint(ink);}
 private Button button(LinearLayout host,String name,Runnable action){Button b=ui.action(host,name,action);b.setTextColor(ink);b.setBackground(ui.bg(card,24));LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)b.getLayoutParams();lp.bottomMargin=ui.dp(4);lp.rightMargin=ui.dp(8);return b;}
 private void message(String value){feedback.setText(value);}
 private void load(){
  if(list==null||isDestroyed())return;
  ParcelRepository.IO.execute(()->{try{List<ParcelRecord> records=ParcelDatabase.get(this).parcels().all();runOnUiThread(()->{if(!isDestroyed())render(records);});}catch(RuntimeException e){runOnUiThread(()->{if(!isDestroyed())message("本机快递数据读取失败，请重试；未清空原数据");});}});
 }
 private void render(List<ParcelRecord> records){
  for(int i=0;i<3;i++){tabs[i].setSelected(i==filter);tabs[i].setTypeface(null,i==filter?Typeface.BOLD:Typeface.NORMAL);tabs[i].setBackground(ui.bg(i==filter?card:surface,24));}
  list.removeAllViews();TreeMap<String,List<ParcelRecord>> groups=new TreeMap<>((a,b)->a.startsWith("待确认 ·")!=b.startsWith("待确认 ·")?(a.startsWith("待确认 ·")?-1:1):a.compareTo(b));
  for(ParcelRecord p:records)if(!p.confirmed?filter==0:p.state==filter)groups.computeIfAbsent(!p.confirmed?"待确认 · 识别结果":p.group(),ignored->new ArrayList<>()).add(p);
  if(groups.isEmpty()){TextView empty=text(filter==0?"还没有待取快递\n手动添加，或在设置中开启通知识别":filter==1?"暂无运输中的快递\n可单独添加运单号":"还没有已完成记录",16,muted);empty.setGravity(Gravity.CENTER);empty.setPadding(0,ui.dp(64),0,ui.dp(24));list.addView(empty);return;}
  for(Map.Entry<String,List<ParcelRecord>> group:groups.entrySet()){
   TextView heading=text(group.getKey()+" · "+group.getValue().size(),14,muted);heading.setPadding(0,ui.dp(20),0,ui.dp(10));list.addView(heading);
   for(ParcelRecord p:group.getValue()){
    LinearLayout row=column();row.setPadding(ui.dp(16),ui.dp(14),ui.dp(16),ui.dp(12));row.setBackground(ui.bg(card,16));LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(-1,-2);size.bottomMargin=ui.dp(8);list.addView(row,size);
    TextView code=text(p.code.isEmpty()?"等待取件码":p.code,28,ink);code.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);code.setPadding(0,ui.dp(4),0,ui.dp(8));code.setMinHeight(ui.dp(48));code.setTag("parcel-code-"+p.id);if(!p.code.isEmpty()){code.setFocusable(true);code.setContentDescription("取件码 "+p.code+"，点击复制");code.setOnClickListener(v->copy(p.code));}row.addView(code);
    row.addView(text((p.confirmed?p.stateLabel():"请核对后保存")+(p.carrier.isEmpty()?"":" · "+p.carrier),14,muted));
    if(!p.trackingNumber.isEmpty())row.addView(text("运单号 "+p.trackingNumber,14,muted));
    if(!p.confirmed&&!p.station.isEmpty())row.addView(text(p.station,14,muted));
    row.addView(text("来源："+p.source+" · 规则评分 "+p.confidence+"%",12,muted));
    LinearLayout actions=new LinearLayout(this);row.addView(actions);button(actions,p.confirmed?"管理":"核对",()->actions(p));
    if(p.confirmed&&p.state!=ParcelRecord.DONE)button(actions,"已取件",()->complete(p.id));
   }
  }
 }
 private void copy(String value){ClipData clip=ClipData.newPlainText("取件码",value);PersistableBundle extras=new PersistableBundle();extras.putBoolean("android.content.extra.IS_SENSITIVE",true);clip.getDescription().setExtras(extras);((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(clip);message("取件码已复制");}
 private LinearLayout sheet(String title){
  if(currentSheet!=null)currentSheet.dismiss();LinearLayout shell=column();shell.setPadding(ui.dp(20),ui.dp(16),ui.dp(20),ui.dp(8));shell.setBackground(ui.bg(card,20));
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);TextView label=text(title,20,ink);label.setTypeface(null,Typeface.BOLD);header.addView(label,new LinearLayout.LayoutParams(0,-2,1));button(header,"关闭",()->currentSheet.dismiss());shell.addView(header);
  ScrollView scroll=new ScrollView(this);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout body=column();body.setPadding(0,ui.dp(12),0,ui.dp(20));scroll.addView(body);
  currentSheet=new AlertDialog.Builder(this).setView(shell).create();currentSheet.setOnDismissListener(d->{draft=null;});currentSheet.show();Window w=currentSheet.getWindow();w.setBackgroundDrawableResource(android.R.color.transparent);w.setGravity(Gravity.BOTTOM);w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);int available=root.getHeight()-root.getPaddingTop()-root.getPaddingBottom();w.setLayout(-1,(available>0?available:getResources().getDisplayMetrics().heightPixels)/2);return body;
 }
 private EditText field(LinearLayout body,String label,String value){TextView name=text(label,14,muted);name.setPadding(0,ui.dp(10),0,ui.dp(6));body.addView(name);EditText input=new EditText(this);input.setText(value);input.setTextSize(16);input.setTextColor(ink);input.setSingleLine(true);input.setMinHeight(ui.dp(52));input.setPadding(ui.dp(12),ui.dp(8),ui.dp(12),ui.dp(8));input.setBackgroundResource(R.drawable.form_field);input.setInputType(InputType.TYPE_CLASS_TEXT);input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);input.setContentDescription(label);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(200)});body.addView(input,new LinearLayout.LayoutParams(-1,-2));return input;}
 private void edit(ParcelRecord p){
  LinearLayout body=sheet(p.confirmed?(p.id==0?"添加快递":"编辑快递"):"确认识别结果");draft=p;
  codeField=field(body,"取件码（与运单号分开）",p.code);codeField.setTag("parcel-editor-code");stationField=field(body,"驿站 / 地址",p.station);trackingField=field(body,"运单号（选填）",p.trackingNumber);carrierField=field(body,"快递公司 / 查询编码（选填）",p.carrier);
  stateField=new Spinner(this);stateField.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"待取件","运输中","已完成"}));stateField.setSelection(p.state);stateField.setContentDescription("包裹状态");body.addView(stateField,new LinearLayout.LayoutParams(-1,ui.dp(52)));
  TextView error=text("",14,muted);error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);body.addView(error);AlertDialog editor=currentSheet;
  ui.action(body,"保存",()->{
   if(saving)return;ParcelRecord value=new ParcelRecord();value.id=p.id;value.code=codeField.getText().toString();value.station=stationField.getText().toString();value.trackingNumber=trackingField.getText().toString();value.carrier=carrierField.getText().toString();value.state=stateField.getSelectedItemPosition();
   value.source=p.source;value.rule=p.rule;value.confidence=p.confidence;value.confirmed=true;value.remindAt=p.remindAt;value.remindedAt=p.remindedAt;
   if(value.trackingNumber.equals(p.trackingNumber)&&value.carrier.equals(p.carrier)){value.trackingState=p.trackingState;value.trackingEvents=p.trackingEvents;value.lastQueryAt=p.lastQueryAt;value.retryAt=p.retryAt;value.trackingError=p.trackingError;}
   saving=true;ParcelRepository.IO.execute(()->{String failure=null;try{new ParcelRepository(this).save(value);}catch(IllegalArgumentException e){failure=e.getMessage();}catch(RuntimeException e){failure="保存失败，请重试；原数据仍保留";}String result=failure;runOnUiThread(()->{saving=false;if(isDestroyed())return;if(result==null){editor.dismiss();message("已保存到本机");load();}else error.setText(result);});});
  }).setTag("parcel-editor-save");
 }
 private void actions(ParcelRecord p){LinearLayout body=sheet("快递管理");button(body,p.confirmed?"编辑":"核对并确认",()->edit(p));
  if(p.confirmed){button(body,"物流记录 / 查询",()->tracking(p));if(p.state==ParcelRecord.READY)button(body,"未领取提醒",()->reminder(p));}
  button(body,"删除",()->delete(p));}
 private void complete(long id){ParcelRepository.IO.execute(()->{try{ParcelRecord p=ParcelDatabase.get(this).parcels().get(id);if(p==null)return;p.state=ParcelRecord.DONE;p.remindAt=0;new ParcelRepository(this).save(p);runOnUiThread(this::load);}catch(RuntimeException e){runOnUiThread(()->message("保存失败，请重试"));}});}
 private void delete(ParcelRecord p){new AlertDialog.Builder(this).setTitle("删除这条快递？").setMessage("删除后不可恢复，重复通知将在 14 天内被忽略。").setNegativeButton("取消",null).setPositiveButton("删除",(dialog,which)->ParcelRepository.IO.execute(()->{try{new ParcelRepository(this).delete(p.id);runOnUiThread(()->{if(isDestroyed())return;currentSheet.dismiss();load();});}catch(RuntimeException e){runOnUiThread(()->message("删除失败，请重试"));}})).show();}
 private void identity(){LinearLayout body=sheet("官方身份码");body.addView(text("仅打开官方 App，不读取或保存动态身份码。深链可能随官方版本变化。",14,muted));String[] names={"淘宝身份码","菜鸟身份码","拼多多身份码"};for(int i=0;i<3;i++){int index=i;button(body,names[i],()->ParcelIdentity.open(this,index));}}
 private void settings(){
  LinearLayout body=sheet("快递管理");SharedPreferences prefs=ParcelNotificationListener.settings(this);
  body.addView(text("只在本机处理选中来源的通知，不读取短信数据库，不上传通知全文。关闭后停止识别。",14,muted));
  Switch recognition=new Switch(this);recognition.setText("后台通知识别");recognition.setTextColor(ink);recognition.setMinHeight(ui.dp(56));recognition.setChecked(prefs.getBoolean("recognition",false));body.addView(recognition);
  recognition.setOnCheckedChangeListener((view,enabled)->{
   if(!enabled){prefs.edit().putBoolean("recognition",false).putString("epoch",UUID.randomUUID().toString()).apply();return;}
   new AlertDialog.Builder(this).setTitle("开启快递通知识别？").setMessage("系统授权允许读取通知。软件只解析你选中的来源，仅保存取件码、地址、运单号和来源，不保存通知全文。验证码与动态身份码会排除。你可以随时关闭，并在系统中撤销通知访问。").setNegativeButton("取消",(d,w)->recognition.setChecked(false)).setOnCancelListener(d->recognition.setChecked(false)).setPositiveButton("继续授权",(d,w)->{prefs.edit().putBoolean("recognition",true).putString("epoch",UUID.randomUUID().toString()).apply();try{android.service.notification.NotificationListenerService.requestRebind(new ComponentName(this,ParcelNotificationListener.class));startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}catch(ActivityNotFoundException|SecurityException e){message("无法打开通知访问设置，请在系统设置中手动授权");}}).show();
  });
  boolean granted=androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(this).contains(getPackageName());body.addView(text("系统通知访问："+(granted?"已授权":"未授权")+"。Android 15 及以上或系统隐私策略可能隐藏敏感通知，无法识别时可手动添加。",13,muted));
  if(prefs.getBoolean("recognitionError",false))body.addView(text("曾有识别结果保存失败，请检查剩余存储空间后重试。",13,muted));
  button(body,"选择通知来源",this::sources);
  Switch reminders=toggle(body,"快递提醒",prefs.getBoolean("reminders",false));reminders.setOnCheckedChangeListener((v,enabled)->{prefs.edit().putBoolean("reminders",enabled).apply();if(!enabled)ParcelNotifications.cancelAll(this);if(enabled&&Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},502);ParcelWorker.schedule(this);});
  boolean notifications=androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled();body.addView(text("系统通知："+(notifications?"允许":"未允许，提醒将无法显示")+"；通知权限与通知访问是两项独立授权。",13,muted));
  button(body,"系统通知设置",()->{try{startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName()));}catch(ActivityNotFoundException e){message("请在系统设置中调整课表镜的通知权限和快递提醒频道");}});
  button(body,"物流服务代理",this::proxySettings);
  Switch widget=toggle(body,"桌面小组件显示取件码与地址",prefs.getBoolean("widgetCodes",false));widget.setOnCheckedChangeListener((v,enabled)->{prefs.edit().putBoolean("widgetCodes",enabled).apply();ParcelRepository.IO.execute(()->ParcelWidget.updateAll(this));});body.addView(text("开启后，桌面可见的人也能看到取件码。默认只显示待取数量。",13,muted));
  button(body,"添加快递小组件",()->{android.appwidget.AppWidgetManager manager=android.appwidget.AppWidgetManager.getInstance(this);if(manager.isRequestPinAppWidgetSupported()){boolean accepted=manager.requestPinAppWidget(new ComponentName(this,ParcelWidget.class),null,null);message(accepted?"已向桌面请求添加，请在系统面板中确认":"桌面未接受请求，请长按桌面手动添加快递中心组件");}else message("请长按桌面 → 小组件 → 课表镜 → 快递中心");});
  body.addView(text("快递数据与课表和学习通账号独立。",14,muted));button(body,"删除全部快递数据",this::clear);
 }
 private void sources(){
  LinearLayout body=sheet("通知来源");Set<String> selected=new HashSet<>(ParcelNotificationListener.sources(this));List<String> packages=new ArrayList<>(Arrays.asList(ParcelNotificationListener.PACKAGES));List<String> names=new ArrayList<>(Arrays.asList(ParcelNotificationListener.NAMES));String sms=android.provider.Telephony.Sms.getDefaultSmsPackage(this);if(sms!=null&&!packages.contains(sms)){packages.add(sms);names.add("默认短信应用的通知（不读短信库）");}
  for(int i=0;i<packages.size();i++){String pkg=packages.get(i);CheckBox choice=new CheckBox(this);choice.setText(names.get(i));choice.setTextColor(ink);choice.setMinHeight(ui.dp(52));choice.setChecked(selected.contains(pkg));choice.setOnCheckedChangeListener((v,checked)->{if(checked)selected.add(pkg);else selected.remove(pkg);ParcelNotificationListener.settings(this).edit().putStringSet("sources",new HashSet<>(selected)).apply();});body.addView(choice);}
 }
 private void importSheet(){LinearLayout body=sheet("识别取件信息");EditText input=field(body,"粘贴快递通知（仅本机识别）","");input.setSingleLine(false);input.setMinLines(3);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(ParcelRules.MAX_TEXT)});button(body,"识别文字",()->recognize(input.getText().toString(),"粘贴文字"));button(body,"选择截图",()->{try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),501);}catch(ActivityNotFoundException e){message("系统没有可用的图片选择器");}});}
 private void receive(Intent intent){
  if(intent==null||!Intent.ACTION_SEND.equals(intent.getAction()))return;
  try{String type=intent.getType();if(type!=null&&type.startsWith("image/")){android.net.Uri uri=Build.VERSION.SDK_INT>=33?intent.getParcelableExtra(Intent.EXTRA_STREAM,android.net.Uri.class):intent.getParcelableExtra(Intent.EXTRA_STREAM);ocr(uri);}else if("text/plain".equals(type)){CharSequence value=intent.getCharSequenceExtra(Intent.EXTRA_TEXT);if(value!=null&&value.length()<=ParcelRules.MAX_TEXT)recognize(value.toString(),"Android 分享文字");else message("分享文字为空或过长，请手动添加");}}
  catch(RuntimeException e){message("无法读取分享内容，请重新分享");}
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==501&&result==RESULT_OK&&data!=null)ocr(data.getData());}
 private void ocr(android.net.Uri uri){String epoch=ParcelNotificationListener.settings(this).getString("epoch","");message("正在本机识别截图…");ParcelOcr.read(getApplicationContext(),uri,(value,error)->runOnUiThread(()->{if(isDestroyed()||!epoch.equals(ParcelNotificationListener.settings(this).getString("epoch","")))return;if(error!=null)message(error);else recognize(value,"截图 OCR");}));}
 private void recognize(String input,String source){
  List<ParcelRules.Candidate> found=ParcelRules.extract(input);if(found.isEmpty()){message("没有识别到明确的取件码；请检查内容或手动添加");return;}
  String epoch=ParcelNotificationListener.settings(this).getString("epoch","");ParcelRepository.IO.execute(()->{int added=0;try{if(!epoch.equals(ParcelNotificationListener.settings(this).getString("epoch","")))return;for(ParcelRules.Candidate c:found)if(new ParcelRepository(this).ingest(ParcelNotificationListener.record(c,source,true)))added++;int count=added;runOnUiThread(()->{if(isDestroyed())return;if(currentSheet!=null)currentSheet.dismiss();filter=0;message(count==0?"识别结果已存在，未重复添加":"已识别 "+count+" 条，请在待确认列表核对后保存");load();});}catch(RuntimeException e){runOnUiThread(()->{if(!isDestroyed())message("识别结果保存失败，请重试");});}});
 }
 private void tracking(ParcelRecord p){
  LinearLayout body=sheet("物流记录");body.addView(text(p.trackingNumber.isEmpty()?"请先编辑并填写运单号":p.trackingNumber,17,ink));
  if(!p.trackingState.isEmpty())body.addView(text(ParcelTracking.stateLabel(p.trackingState),15,ink));
  if(!p.trackingError.isEmpty())body.addView(text(p.trackingError,14,muted));
  try{if(!p.trackingEvents.isEmpty()){org.json.JSONArray events=new org.json.JSONArray(p.trackingEvents);for(int i=0;i<events.length();i++){org.json.JSONObject e=events.getJSONObject(i);TextView row=text(e.getString("time")+"\n"+e.getString("description"),15,ink);row.setPadding(0,ui.dp(12),0,ui.dp(12));body.addView(row);}}}catch(org.json.JSONException e){body.addView(text("本机物流记录无法显示，可重新查询",14,muted));}
  body.addView(text("查询仅向你配置的 HTTPS 代理发送运单号和快递公司。物流显示已签收时仍需你确认取件。",13,muted));
  if(!p.trackingNumber.isEmpty()&&p.state!=ParcelRecord.DONE)button(body,"查询物流",()->{message("正在查询物流…");ParcelTracking.NETWORK.execute(()->{String result=ParcelTracking.query(getApplicationContext(),p.id);runOnUiThread(()->{if(isDestroyed())return;message(result);if(currentSheet!=null)currentSheet.dismiss();load();});});});
 }
 private void reminder(ParcelRecord p){
  LinearLayout body=sheet("未领取提醒");body.addView(text("提醒可能因省电、后台限制而延迟。请先在快递管理中开启提醒。",14,muted));
  button(body,"选择日期与时间",()->{java.util.Calendar date=java.util.Calendar.getInstance();new DatePickerDialog(this,(picker,y,m,d)->new TimePickerDialog(this,(clock,h,min)->{date.set(y,m,d,h,min,0);date.set(java.util.Calendar.MILLISECOND,0);if(date.getTimeInMillis()<=System.currentTimeMillis()){message("请选择未来的提醒时间");return;}saveReminder(p.id,date.getTimeInMillis());},date.get(java.util.Calendar.HOUR_OF_DAY),date.get(java.util.Calendar.MINUTE),true).show(),date.get(java.util.Calendar.YEAR),date.get(java.util.Calendar.MONTH),date.get(java.util.Calendar.DAY_OF_MONTH)).show();});
  button(body,"取消本条提醒",()->saveReminder(p.id,0));
 }
 private void saveReminder(long id,long time){ParcelRepository.IO.execute(()->{try{ParcelDatabase db=ParcelDatabase.get(this);db.runInTransaction(()->{ParcelRecord p=db.parcels().get(id);if(p==null||p.state!=ParcelRecord.READY)return;p.remindAt=time;p.remindedAt=0;db.parcels().update(p);});ParcelWorker.schedule(this);runOnUiThread(()->{if(isDestroyed())return;currentSheet.dismiss();message(time==0?"提醒已取消":"提醒时间已保存；请确认系统允许通知");});}catch(RuntimeException e){runOnUiThread(()->message("提醒保存失败，请重试"));}});}
 private Switch toggle(LinearLayout body,String label,boolean checked){Switch view=new Switch(this);view.setText(label);view.setTextColor(ink);view.setMinHeight(ui.dp(56));view.setChecked(checked);body.addView(view,new LinearLayout.LayoutParams(-1,-2));return view;}
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);if(request==502&&(grants.length==0||grants[0]!=android.content.pm.PackageManager.PERMISSION_GRANTED)){ParcelNotificationListener.settings(this).edit().putBoolean("reminders",false).apply();ParcelWorker.schedule(this);message("未允许通知，提醒已关闭；快递管理和识别仍可使用");if(currentSheet!=null&&currentSheet.isShowing())settings();}}
 private void proxySettings(){
  LinearLayout body=sheet("物流服务代理");SharedPreferences prefs=ParcelNotificationListener.settings(this);body.addView(text("默认不联网。仅连接你信任的 HTTPS 代理，由服务端保管快递100 / 快递鸟密钥；查询只发送运单号和快递公司，不发送取件码、地址、通知或图片。没有代理时仍可离线管理快递。",14,muted));
  EditText endpoint=field(body,"HTTPS 代理地址（留空关闭）",prefs.getString("proxy",""));endpoint.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2048)});endpoint.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
  Switch background=toggle(body,"后台查询（约每 6 小时）",prefs.getBoolean("backgroundTracking",false));TextView error=text("",14,muted);body.addView(error);
  button(body,"保存",()->{String value=endpoint.getText().toString().trim();try{if(!value.isEmpty())value=ProxyTrackingProvider.validateEndpoint(value);else if(background.isChecked())throw new IllegalArgumentException("后台查询需要先配置代理地址");prefs.edit().putString("proxy",value).putBoolean("backgroundTracking",background.isChecked()).putLong("trackingRetryAt",0).putString("epoch",UUID.randomUUID().toString()).apply();ParcelWorker.schedule(this);currentSheet.dismiss();message(value.isEmpty()?"物流联网已关闭":"代理设置已保存；尚未验证真实物流服务");}catch(IllegalArgumentException e){error.setText(e.getMessage());}});
 }
 private void clear(){new AlertDialog.Builder(this).setTitle("删除全部快递数据？").setMessage("将关闭后台识别和查询，删除快递、识别候选、去重记录和快递设置，不影响课表。系统通知访问可在系统设置中撤销。").setNegativeButton("取消",null).setPositiveButton("全部删除",(dialog,which)->{ParcelNotificationListener.settings(this).edit().clear().putString("epoch",UUID.randomUUID().toString()).apply();ParcelWorker.schedule(this);ParcelNotifications.cancelAll(this);ParcelRepository.IO.execute(()->{try{new ParcelRepository(this).clear();runOnUiThread(()->{if(isDestroyed())return;currentSheet.dismiss();message("快递数据已删除，后台功能已关闭");load();});}catch(RuntimeException e){runOnUiThread(()->message("删除失败，请重试；后台功能已关闭"));}});}).show();}
}
