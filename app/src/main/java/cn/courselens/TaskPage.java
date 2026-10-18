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
  LinearLayout header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(ui.dp(20),ui.dp(20),ui.dp(20),ui.dp(16));addView(header,new LayoutParams(-1,-2));
  TextView title=ui.text("任务",28,SecondaryUi.INK);title.setTypeface(null,Typeface.BOLD);header.addView(title,new LayoutParams(0,-2,1));
  platform=ui.text("未连接平台",13,SecondaryUi.MUTED);platform.setTag("task-connected-platforms");platform.setGravity(Gravity.CENTER);platform.setPadding(ui.dp(12),ui.dp(10),ui.dp(12),ui.dp(10));platform.setMinHeight(ui.dp(48));platform.setBackground(ui.bg(SecondaryUi.SOFT,16));platform.setFocusable(true);platform.setOnClickListener(v->configure());ui.ripple(platform);header.addView(platform,new LayoutParams(-2,-2));
  LinearLayout toolbar=new LinearLayout(context);toolbar.setGravity(Gravity.CENTER_VERTICAL);toolbar.setPadding(ui.dp(20),0,ui.dp(20),0);addView(toolbar,new LayoutParams(-1,-2));
  configure=ui.action(toolbar,"账号密码配置",this::configure);configure.setTag("chaoxing-connect");LayoutParams configLayout=new LayoutParams(0,-2,1);configLayout.bottomMargin=ui.dp(12);toolbar.updateViewLayout(configure,configLayout);
  refresh=ui.action(toolbar,"刷新作业",this::refresh);refresh.setTag("chaoxing-refresh");LayoutParams refreshLayout=new LayoutParams(0,-2,1);refreshLayout.leftMargin=ui.dp(10);refreshLayout.bottomMargin=ui.dp(12);toolbar.updateViewLayout(refresh,refreshLayout);refresh.setEnabled(account.configured());
  status=ui.text("请先配置学习通账号密码，登录验证成功后才会保存。",13,SecondaryUi.MUTED);status.setTag("chaoxing-status");status.setPadding(ui.dp(24),0,ui.dp(24),ui.dp(12));status.setLineSpacing(ui.dp(4),1);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);addView(status,new LayoutParams(-1,-2));
  progress=new ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(SecondaryUi.NAVY));progress.setVisibility(GONE);addView(progress,new LayoutParams(-1,ui.dp(3)));
  LinearLayout choices=new LinearLayout(context);choices.setPadding(ui.dp(4),ui.dp(4),ui.dp(4),ui.dp(4));choices.setBackground(ui.bg(0xffe5eef0,14));LayoutParams choicesLayout=new LayoutParams(-1,-2);choicesLayout.setMargins(ui.dp(20),ui.dp(8),ui.dp(20),ui.dp(12));addView(choices,choicesLayout);
  String[] labels={"待处理","已完成","全部"};for(int i=0;i<labels.length;i++){final int selected=i;TextView tab=ui.text(labels[i],14,SecondaryUi.MUTED);tab.setTag("task-filter-"+i);tab.setGravity(Gravity.CENTER);tab.setMinHeight(ui.dp(48));tab.setPadding(ui.dp(4),ui.dp(10),ui.dp(4),ui.dp(10));tab.setFocusable(true);tab.setOnClickListener(v->selectFilter(selected));filters[i]=tab;choices.addView(tab,new LayoutParams(0,-2,1));}
  scroll=new ScrollView(context);scroll.setFillViewport(true);scroll.setClipToPadding(false);addView(scroll,new LayoutParams(-1,0,1));list=new LinearLayout(context);list.setOrientation(VERTICAL);list.setPadding(ui.dp(20),0,ui.dp(20),ui.dp(24));list.setTag("chaoxing-results");scroll.addView(list,new ScrollView.LayoutParams(-1,-2));
  emptyTitle=ui.text("",18,SecondaryUi.INK);emptyTitle.setTag("tasks-empty-title");emptyTitle.setGravity(Gravity.CENTER);emptyTitle.setTypeface(null,Typeface.BOLD);emptyTitle.setPadding(ui.dp(16),ui.dp(64),ui.dp(16),ui.dp(16));
  selectFilter(state==null?0:state.getInt("filter",0));int restoredY=state==null?0:state.getInt("scrollY",0);scroll.post(()->scroll.scrollTo(0,restoredY));
  connected(false);if(account.configured()){platform.setText("学习通 · 已配置");platform.setContentDescription("已配置平台：学习通，点击配置账号密码");status.setText("账号密码已加密保存在本机，点击“刷新作业”获取最新数据。");}
 }
 private void selectFilter(int value){filter=value>=0&&value<filters.length?value:0;for(int i=0;i<filters.length;i++){boolean selected=i==filter;filters[i].setSelected(selected);filters[i].setTextColor(selected?SecondaryUi.NAVY:SecondaryUi.MUTED);filters[i].setTypeface(null,selected?Typeface.BOLD:Typeface.NORMAL);filters[i].setBackground(selected?ui.bg(android.graphics.Color.WHITE,10):null);}render();}
 private void render(){
  list.removeAllViews();int shown=0;
  if(snapshot!=null)for(ChaoxingClient.Work work:snapshot.items){if(filter==0&&work.completed()||filter==1&&!work.completed())continue;shown++;LinearLayout card=new LinearLayout(getContext());card.setOrientation(VERTICAL);card.setPadding(ui.dp(20),ui.dp(18),ui.dp(20),ui.dp(18));card.setBackground(ui.bg(android.graphics.Color.WHITE,22));LayoutParams lp=new LayoutParams(-1,-2);lp.bottomMargin=ui.dp(12);list.addView(card,lp);
   TextView course=ui.text("学习通 · "+work.course,13,SecondaryUi.MUTED);card.addView(course);TextView name=ui.text(work.title,18,SecondaryUi.INK);name.setTypeface(null,Typeface.BOLD);name.setTextIsSelectable(true);name.setPadding(0,ui.dp(10),0,ui.dp(12));card.addView(name);TextView detail=ui.text("状态："+(work.status.isEmpty()?"未知":work.status)+"\n截止："+work.deadline,14,SecondaryUi.MUTED);detail.setLineSpacing(ui.dp(5),1);detail.setTextIsSelectable(true);card.addView(detail);
  }
  if(shown==0){String[] titles={"还没有待处理任务","还没有已完成任务","还没有任务"};emptyTitle.setText(snapshot==null?"尚未读取作业":!snapshot.warnings.isEmpty()?"部分课程尚未读取完成":titles[filter]);list.addView(emptyTitle,new LayoutParams(-1,-2));}
  if(snapshot!=null)for(String warning:snapshot.warnings){TextView note=ui.text(warning,13,0xff885564);note.setPadding(ui.dp(4),ui.dp(8),ui.dp(4),ui.dp(8));list.addView(note);}
 }
 private void connected(boolean value){platform.setText(value?"学习通 · 已连接":account.configured()?"学习通 · 待验证":"未连接平台");platform.setTextColor(value?SecondaryUi.NAVY:SecondaryUi.MUTED);platform.setContentDescription("已连接平台："+(value?"学习通":"无")+"，点击配置账号密码");}
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
   worker.execute(()->{try{connection.login(name,secret);if(!current(token))return;account.save(name,secret);activity.runOnUiThread(()->{if(current(token)){connected(true);dialog.dismiss();busy(false);status.setText("登录验证成功，密码配置已加密保存。点击“刷新作业”更新数据。");snapshot=null;render();}});connection.close();}catch(Exception e){activity.runOnUiThread(()->{if(current(token)){busy(false);dialog.setCancelable(true);username.setEnabled(true);password.setEnabled(true);dialog.getButton(-1).setEnabled(true);dialog.getButton(-2).setEnabled(true);feedback.setText("登录验证或保存失败，未更新配置。请核对账号密码和网络；验证码或机构认证需先在学习通完成。");status.setText("配置未更新，原配置及作业保留。");}});}});
  });
 }
 private EditText field(LinearLayout form,String label,String hint,boolean secret){TextView caption=ui.text(label,13,SecondaryUi.MUTED);caption.setPadding(0,ui.dp(16),0,ui.dp(8));form.addView(caption);EditText input=new EditText(getContext());input.setTextSize(15);input.setSingleLine(true);input.setHint(hint);input.setContentDescription(label);input.setTextColor(SecondaryUi.INK);input.setHintTextColor(SecondaryUi.MUTED);input.setPadding(ui.dp(16),0,ui.dp(16),0);input.setBackgroundResource(R.drawable.form_field);input.setInputType(InputType.TYPE_CLASS_TEXT|(secret?InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_TEXT_VARIATION_NORMAL));input.setSaveEnabled(false);input.setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_NO);form.addView(input,new LayoutParams(-1,ui.dp(54)));return input;}
 private void refresh(){
  if(busy||closed)return;if(!account.configured()){configure();return;}busy(true);int token=++generation;status.setText("正在使用保存的配置登录学习通…");if(client!=null)client.close();client=new ChaoxingClient();ChaoxingClient connection=client;
  worker.execute(()->{try{ChaoxingAccount.Saved saved=account.load();connection.login(saved.account,saved.password);activity.runOnUiThread(()->{if(current(token))connected(true);});sync(connection,token);}catch(Exception e){activity.runOnUiThread(()->{if(current(token)){busy(false);connected(false);status.setText("登录未成功，请检查网络或重新配置密码。已有作业保留。");}});}});
 }
 private void sync(ChaoxingClient connection,int token){
  try{ChaoxingClient.Result result=connection.sync(message->activity.runOnUiThread(()->{if(current(token))status.setText(message);}));activity.runOnUiThread(()->{if(current(token)){snapshot=result;busy(false);status.setText("已读取 "+result.checked+" / "+result.total+" 门课程 · "+result.items.size()+" 份作业"+(result.warnings.isEmpty()?"":" · 部分信息未完成"));render();}});}catch(Exception e){activity.runOnUiThread(()->{if(current(token)){busy(false);status.setText("作业读取失败（"+ChaoxingClient.describe(e)+"）。密码配置已保留。");}});}
 }
 void saveState(Bundle state){state.putInt("filter",filter);state.putInt("scrollY",scroll.getScrollY());}
 void close(){closed=true;generation++;if(configuration!=null)configuration.dismiss();if(passwordField!=null)passwordField.setText("");if(client!=null)client.close();worker.shutdownNow();}
}
