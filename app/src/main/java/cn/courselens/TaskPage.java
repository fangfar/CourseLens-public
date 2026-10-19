package cn.courselens;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

@android.annotation.SuppressLint("ViewConstructor")
final class TaskPage extends LinearLayout {
 private final Activity activity;
 private final SecondaryUi ui;
 private final ChaoxingAccount account;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final TextView[] filters=new TextView[3];
 private final TextView platform,status,emptyTitle;
 private final LinearLayout list;
 private final ScrollView scroll;
 private final Button refresh,configure;
 private final ProgressBar progress;
 private ChaoxingClient client;
 private ChaoxingClient.Result snapshot;
 private AlertDialog configuration;
 private EditText passwordField;
 private int filter;private volatile int generation;
 private boolean busy;private volatile boolean closed;

 TaskPage(Context context,Bundle state){
  super(context);activity=(Activity)context;ui=new SecondaryUi(activity);account=new ChaoxingAccount(context);setTag("tasks-page");setOrientation(VERTICAL);
  LinearLayout header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(ui.dp(20),ui.dp(12),ui.dp(20),0);addView(header,new LayoutParams(-1,-2));
  LinearLayout heading=new LinearLayout(context);heading.setOrientation(VERTICAL);header.addView(heading,new LayoutParams(0,-2,1));TextView title=ui.text("任务",24,SecondaryUi.INK);title.setTypeface(null,Typeface.BOLD);heading.addView(title);
  platform=ui.text("未连接平台",12,SecondaryUi.MUTED);platform.setTag("task-connected-platforms");
  configure=ui.iconAction(header,R.drawable.ic_person,"账号密码配置",this::configure);configure.setTag("chaoxing-connect");
  refresh=ui.iconAction(header,R.drawable.ic_sync,"刷新作业",this::refresh);refresh.setTag("chaoxing-refresh");refresh.setEnabled(account.configured());
  LinearLayout summary=new LinearLayout(context);summary.setPadding(ui.dp(20),ui.dp(4),ui.dp(20),ui.dp(8));addView(summary,new LayoutParams(-1,-2));summary.addView(platform,new LayoutParams(-2,-2));TextView separator=ui.text("|",12,SecondaryUi.MUTED);separator.setPadding(ui.dp(8),0,ui.dp(8),0);separator.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);summary.addView(separator);
  status=ui.text("请先配置学习通账号密码，登录验证成功后才会保存。",12,SecondaryUi.MUTED);status.setTag("chaoxing-status");status.setLineSpacing(ui.dp(3),1);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);summary.addView(status,new LayoutParams(0,-2,1));
  progress=new ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(SecondaryUi.NAVY));progress.setVisibility(GONE);addView(progress,new LayoutParams(-1,ui.dp(3)));
  LinearLayout choices=new LinearLayout(context);choices.setPadding(ui.dp(4),ui.dp(4),ui.dp(4),ui.dp(4));choices.setBackground(ui.bg(SecondaryUi.SOFT,14));LayoutParams choicesLayout=new LayoutParams(-1,-2);choicesLayout.setMargins(ui.dp(16),ui.dp(8),ui.dp(16),ui.dp(4));addView(choices,choicesLayout);
  String[] labels={"待处理","已完成","全部"};for(int i=0;i<labels.length;i++){final int selected=i;TextView tab=ui.text(labels[i],14,SecondaryUi.MUTED);tab.setTag("task-filter-"+i);tab.setGravity(Gravity.CENTER);tab.setMinHeight(ui.dp(48));tab.setPadding(ui.dp(4),ui.dp(10),ui.dp(4),ui.dp(10));tab.setFocusable(true);tab.setOnClickListener(v->selectFilter(selected));filters[i]=tab;choices.addView(tab,new LayoutParams(0,-2,1));}
  scroll=new ScrollView(context);scroll.setFillViewport(true);scroll.setClipToPadding(false);addView(scroll,new LayoutParams(-1,0,1));list=new LinearLayout(context);list.setOrientation(VERTICAL);list.setPadding(ui.dp(16),0,ui.dp(16),ui.dp(16));list.setTag("chaoxing-results");scroll.addView(list,new ScrollView.LayoutParams(-1,-2));
  emptyTitle=ui.text("",18,SecondaryUi.INK);emptyTitle.setTag("tasks-empty-title");emptyTitle.setGravity(Gravity.CENTER);emptyTitle.setTypeface(null,Typeface.BOLD);emptyTitle.setPadding(ui.dp(16),ui.dp(64),ui.dp(16),ui.dp(16));
  selectFilter(state==null?0:state.getInt("filter",0));int restoredY=state==null?0:state.getInt("scrollY",0);scroll.post(()->scroll.scrollTo(0,restoredY));
  connected(false);if(account.configured()){platform.setText("学习通 · 已配置");platform.setContentDescription("已配置平台：学习通");status.setText("正在加载本机保存的作业…");loadLocal();}
 }
 private void selectFilter(int value){filter=value>=0&&value<filters.length?value:0;for(int i=0;i<filters.length;i++){boolean selected=i==filter;filters[i].setSelected(selected);filters[i].setTextColor(selected?SecondaryUi.NAVY:SecondaryUi.MUTED);filters[i].setTypeface(null,selected?Typeface.BOLD:Typeface.NORMAL);filters[i].setBackground(selected?ui.bg(android.graphics.Color.WHITE,10):null);}render();}
 private void render(){
  list.removeAllViews();Map<String,List<ChaoxingClient.Work>> groups=new LinkedHashMap<>();
  if(snapshot!=null)for(ChaoxingClient.Work work:snapshot.items){if(filter==0&&work.completed()||filter==1&&!work.completed())continue;groups.computeIfAbsent(work.course,key->new ArrayList<>()).add(work);}
  for(Map.Entry<String,List<ChaoxingClient.Work>> group:groups.entrySet()){
   TextView course=ui.text(group.getKey().isEmpty()?"课程名称未识别":group.getKey(),13,SecondaryUi.MUTED);course.setTag("task-course-label");course.setPadding(ui.dp(4),ui.dp(16),ui.dp(4),ui.dp(8));course.setLineSpacing(ui.dp(2),1);if(android.os.Build.VERSION.SDK_INT>=28)course.setAccessibilityHeading(true);list.addView(course,new LayoutParams(-1,-2));
   LinearLayout card=new LinearLayout(getContext());card.setTag("task-course-group");card.setOrientation(VERTICAL);card.setBackground(ui.bg(android.graphics.Color.WHITE,14));list.addView(card,new LayoutParams(-1,-2));
   for(ChaoxingClient.Work work:group.getValue()){
    if(card.getChildCount()>0){View divider=new View(getContext());divider.setBackgroundColor(SecondaryUi.SOFT);LayoutParams line=new LayoutParams(-1,ui.dp(1));line.setMargins(ui.dp(16),0,ui.dp(16),0);card.addView(divider,line);}
    LinearLayout row=new LinearLayout(getContext());row.setTag("task-work");row.setOrientation(VERTICAL);row.setPadding(ui.dp(16),ui.dp(14),ui.dp(16),ui.dp(14));card.addView(row,new LayoutParams(-1,-2));
    LinearLayout heading=new LinearLayout(getContext());heading.setGravity(Gravity.TOP);row.addView(heading,new LayoutParams(-1,-2));
    TextView name=ui.text(work.title,16,SecondaryUi.INK);name.setTag("task-work-title");name.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));name.setLineSpacing(ui.dp(2),1);name.setTextIsSelectable(true);heading.addView(name,new LayoutParams(0,-2,1));
    TextView state=ui.text(work.status.isEmpty()?"状态未知":work.status,13,SecondaryUi.MUTED);state.setTag("task-work-status");state.setMaxWidth(ui.dp(96));state.setLineSpacing(ui.dp(2),1);state.setPadding(ui.dp(12),ui.dp(2),0,0);heading.addView(state,new LayoutParams(-2,-2));
    TextView deadline=ui.text(work.deadline,13,SecondaryUi.MUTED);deadline.setTag("task-work-deadline");deadline.setTextIsSelectable(true);deadline.setLineSpacing(ui.dp(2),1);deadline.setContentDescription("截止："+work.deadline);android.graphics.drawable.Drawable clock=activity.getDrawable(R.drawable.ic_clock).mutate();clock.setTint(SecondaryUi.MUTED);clock.setBounds(0,0,ui.dp(16),ui.dp(16));deadline.setCompoundDrawables(clock,null,null,null);deadline.setCompoundDrawablePadding(ui.dp(6));LayoutParams time=new LayoutParams(-1,-2);time.topMargin=ui.dp(8);row.addView(deadline,time);
   }
  }
  if(groups.isEmpty()){String[] titles={"还没有待处理任务","还没有已完成任务","还没有任务"};emptyTitle.setText(snapshot==null?"尚未读取作业":!snapshot.warnings.isEmpty()?"部分课程尚未读取完成":titles[filter]);list.addView(emptyTitle,new LayoutParams(-1,-2));}
  if(snapshot!=null)for(String warning:snapshot.warnings){TextView note=ui.text(warning,13,0xff885564);note.setPadding(ui.dp(4),ui.dp(8),ui.dp(4),ui.dp(8));list.addView(note);}
 }
 private void connected(boolean value){platform.setText(value?"学习通 · 已连接":account.configured()?"学习通 · 待验证":"未连接平台");platform.setTextColor(value?SecondaryUi.NAVY:SecondaryUi.MUTED);platform.setContentDescription(platform.getText());}
 private void busy(boolean value){busy=value;configure.setEnabled(!value);platform.setEnabled(!value);refresh.setEnabled(!value&&account.configured());progress.setVisibility(value?VISIBLE:GONE);}
 private boolean current(int token){return !closed&&token==generation&&!activity.isFinishing()&&!activity.isDestroyed();}
 private void configure(){
  if(busy||closed)return;LinearLayout form=new LinearLayout(getContext());form.setOrientation(VERTICAL);form.setPadding(ui.dp(24),ui.dp(8),ui.dp(24),ui.dp(8));
  TextView intro=ui.text("输入学习通账号密码。验证登录成功后，在本机加密保存；失败时保留原配置。",14,SecondaryUi.MUTED);intro.setLineSpacing(ui.dp(4),1);form.addView(intro);
  EditText username=field(form,"学习通账号","手机号 / 超星账号",false);EditText password=field(form,"登录密码","请输入密码",true);passwordField=password;
  TextView feedback=ui.text("",13,SecondaryUi.MUTED);feedback.setPadding(0,ui.dp(12),0,ui.dp(8));feedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);form.addView(feedback);
  ScrollView scroll=new ScrollView(getContext());scroll.addView(form);
  AlertDialog dialog=new AlertDialog.Builder(activity,R.style.SecondaryDialog).setTitle("学习通密码配置").setView(scroll).setNegativeButton("取消",null).setPositiveButton("验证并保存",null).create();configuration=dialog;dialog.setOnDismissListener(d->{password.setText("");passwordField=null;configuration=null;});dialog.show();dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);ui.styleDialog(dialog,false);
  if(account.configured())try{username.setText(account.load().account);}catch(Exception e){feedback.setText("原配置无法解密，请重新输入账号密码。");}
  dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String name=username.getText().toString().trim(),secret=password.getText().toString();if(name.isEmpty()||secret.isEmpty()){feedback.setText("请输入账号和密码。");return;}password.setText("");username.setEnabled(false);password.setEnabled(false);dialog.setCancelable(false);dialog.getButton(-1).setEnabled(false);dialog.getButton(-2).setEnabled(false);feedback.setText("正在验证登录，请稍候…");busy(true);int token=++generation;
   if(client!=null)client.close();client=new ChaoxingClient();ChaoxingClient connection=client;
   worker.execute(()->{try{connection.login(name,secret);if(!current(token))return;account.save(name,secret);activity.runOnUiThread(()->{if(current(token)){connected(true);dialog.dismiss();busy(false);status.setText("登录验证成功，密码配置已加密保存。点击“刷新作业”更新数据。");snapshot=null;render();loadLocal();}});connection.close();}catch(Exception e){activity.runOnUiThread(()->{if(current(token)){busy(false);dialog.setCancelable(true);username.setEnabled(true);password.setEnabled(true);dialog.getButton(-1).setEnabled(true);dialog.getButton(-2).setEnabled(true);feedback.setText("登录验证或保存失败，未更新配置。请核对账号密码和网络；验证码或机构认证需先在学习通完成。");status.setText("配置未更新，原配置及作业保留。");}});}});
  });
 }
 private EditText field(LinearLayout form,String label,String hint,boolean secret){TextView caption=ui.text(label,13,SecondaryUi.MUTED);caption.setPadding(0,ui.dp(16),0,ui.dp(8));form.addView(caption);EditText input=new EditText(getContext());input.setTextSize(15);input.setSingleLine(true);input.setHint(hint);input.setContentDescription(label);input.setTextColor(SecondaryUi.INK);input.setHintTextColor(SecondaryUi.MUTED);input.setPadding(ui.dp(16),0,ui.dp(16),0);input.setBackgroundResource(R.drawable.form_field);input.setInputType(InputType.TYPE_CLASS_TEXT|(secret?InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_TEXT_VARIATION_NORMAL));input.setSaveEnabled(false);input.setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_NO);form.addView(input,new LayoutParams(-1,ui.dp(54)));return input;}
 private void refresh(){
  if(busy||closed)return;if(!account.configured()){configure();return;}busy(true);int token=++generation;status.setText("正在使用保存的配置登录学习通…");if(client!=null)client.close();client=new ChaoxingClient();ChaoxingClient connection=client;
  worker.execute(()->{try{ChaoxingAccount.Saved saved=account.load();connection.login(saved.account,saved.password);activity.runOnUiThread(()->{if(current(token))connected(true);});sync(connection,token,saved.account);}catch(Exception e){activity.runOnUiThread(()->{if(current(token)){busy(false);connected(false);status.setText("登录未成功，请检查网络或重新配置密码。已有作业保留。");}});}});
 }
 private void loadLocal(){
  busy(true);int token=++generation;worker.execute(()->{try{ChaoxingAccount.Saved saved=account.load();ChaoxingClient.Result local=new ChaoxingCache(getContext(),saved.account).load();activity.runOnUiThread(()->{if(current(token)){snapshot=local;busy(false);status.setText(local==null?"账号密码已保存在本机，点击“刷新作业”读取作业。":"本机已保存 "+local.items.size()+" 份作业");render();}});}catch(Exception e){activity.runOnUiThread(()->{if(current(token)){busy(false);status.setText("本机作业暂时无法读取，请点击刷新；原文件未删除。");}});}});
 }
 private void sync(ChaoxingClient connection,int token,String username){
  try{
   ChaoxingClient.Result result=connection.sync(message->activity.runOnUiThread(()->{if(current(token))status.setText(message);}));if(!current(token))return;
   if(snapshot!=null&&!result.warnings.isEmpty()){activity.runOnUiThread(()->{if(current(token)){busy(false);status.setText("本次读取不完整，已保留原有作业。"+result.warnings.get(0));}});return;}
   boolean persisted;try{new ChaoxingCache(getContext(),username).save(result);persisted=true;}catch(Exception e){persisted=false;}boolean saved=persisted;
   activity.runOnUiThread(()->{if(current(token)){snapshot=result;busy(false);status.setText("已读取 "+result.checked+" / "+result.total+" 门课程 · "+result.items.size()+" 份作业"+(result.warnings.isEmpty()?"":" · 部分信息未完成")+(saved?" · 已保存到本机":" · 本机保存失败，退出后可能无法保留"));render();}});
  }catch(Exception e){activity.runOnUiThread(()->{if(current(token)){busy(false);status.setText("作业读取失败（"+ChaoxingClient.describe(e)+"）。原有作业和密码配置已保留。");}});}
 }
 void saveState(Bundle state){state.putInt("filter",filter);state.putInt("scrollY",scroll.getScrollY());}
 void close(){closed=true;generation++;if(configuration!=null)configuration.dismiss();if(passwordField!=null)passwordField.setText("");if(client!=null)client.close();worker.shutdownNow();}
}
