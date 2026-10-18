package cn.courselens;

import android.annotation.SuppressLint;
import android.app.*;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.util.concurrent.*;

/** Native login and automatic read-only homework aggregation. */
public final class ChaoxingActivity extends Activity {
 private final SecondaryUi ui=new SecondaryUi(this);
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private EditText account,password;
 private TextView message;
 private Button login,refresh,verify,disconnect;
 private ProgressBar progress;
 private LinearLayout results;
 private ChaoxingClient client;
 private AlertDialog verification;
 private WebView browser;
 private int generation;
 private boolean busy;

 @Override public void onCreate(Bundle state){
  super.onCreate(state);getWindow().setStatusBarColor(SecondaryUi.SURFACE);getWindow().setNavigationBarColor(SecondaryUi.SURFACE);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(SecondaryUi.SURFACE);root.setTag("chaoxing-page");setContentView(root);
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(ui.dp(12),ui.dp(8),ui.dp(20),ui.dp(8));root.addView(header);
  TextView back=ui.text("‹",27,SecondaryUi.INK);back.setGravity(Gravity.CENTER);back.setContentDescription("返回任务");back.setFocusable(true);back.setOnClickListener(v->finish());ui.ripple(back);header.addView(back,new LinearLayout.LayoutParams(ui.dp(48),ui.dp(48)));
  TextView heading=ui.text("学习通作业",23,SecondaryUi.NAVY);heading.setTypeface(null,Typeface.BOLD);header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(ui.dp(20),ui.dp(12),ui.dp(20),ui.dp(28));scroll.addView(body);
  TextView intro=ui.text("登录后自动获取各课程的作业、提交状态和截止时间。",14,SecondaryUi.MUTED);intro.setLineSpacing(ui.dp(4),1);intro.setPadding(ui.dp(4),0,ui.dp(4),ui.dp(20));body.addView(intro);
  LinearLayout form=card(body);TextView title=ui.text("连接学习通",23,SecondaryUi.INK);title.setTypeface(null,Typeface.BOLD);form.addView(title);
  account=field(form,"学习通账号","手机号 / 超星账号",false);account.setTag("chaoxing-account");password=field(form,"登录密码","请输入密码",true);password.setTag("chaoxing-password");
  login=ui.action(form,"登录并自动识别",this::login);login.setTag("chaoxing-login");
  message=ui.text("密码仅用于本次登录，不保存在手机。读取失败时会标出未完成的课程。",13,SecondaryUi.MUTED);message.setLineSpacing(ui.dp(4),1);message.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);message.setTag("chaoxing-status");form.addView(message);
  progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(SecondaryUi.NAVY));progress.setVisibility(View.GONE);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,ui.dp(4));pp.topMargin=ui.dp(12);form.addView(progress,pp);
  refresh=ui.action(body,"刷新作业",()->start(false));refresh.setTag("chaoxing-refresh");refresh.setEnabled(false);
  verify=ui.action(body,"官方登录与验证",this::verify);verify.setTag("chaoxing-verify");verify.setVisibility(View.GONE);
  disconnect=ui.action(body,"退出学习通",this::disconnect);disconnect.setTag("chaoxing-disconnect");disconnect.setVisibility(View.GONE);
  results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);results.setTag("chaoxing-results");body.addView(results);
 }
 private LinearLayout card(LinearLayout parent){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(ui.dp(20),ui.dp(20),ui.dp(20),ui.dp(20));card.setBackground(ui.bg(android.graphics.Color.WHITE,22));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=ui.dp(16);parent.addView(card,lp);return card;}
 private EditText field(LinearLayout parent,String label,String hint,boolean secret){TextView caption=ui.text(label,13,SecondaryUi.MUTED);caption.setPadding(0,ui.dp(20),0,ui.dp(8));parent.addView(caption);EditText field=new EditText(this);field.setTextSize(15);field.setSingleLine(true);field.setHint(hint);field.setContentDescription(label);field.setTextColor(SecondaryUi.INK);field.setHintTextColor(SecondaryUi.MUTED);field.setPadding(ui.dp(16),0,ui.dp(16),0);field.setBackgroundResource(R.drawable.form_field);field.setInputType(InputType.TYPE_CLASS_TEXT|(secret?InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_TEXT_VARIATION_NORMAL));field.setSaveEnabled(false);field.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);parent.addView(field,new LinearLayout.LayoutParams(-1,ui.dp(54)));return field;}
 private void login(){if(busy)return;if(account.getText().toString().trim().isEmpty()||password.length()==0){message.setText("请输入学习通账号和密码。");return;}if(client!=null)client.close();client=new ChaoxingClient();results.removeAllViews();start(true);}
 private void busy(boolean value){busy=value;disconnect.setVisibility(client==null?View.GONE:View.VISIBLE);login.setEnabled(!value);account.setEnabled(!value);password.setEnabled(!value);refresh.setEnabled(!value&&client!=null);verify.setEnabled(!value);progress.setVisibility(value?View.VISIBLE:View.GONE);}
 private void start(boolean authenticate){
  if(busy||client==null)return;
  int token=++generation;ChaoxingClient connection=client;String username=authenticate?account.getText().toString().trim():"",secret=authenticate?password.getText().toString():"";password.setText("");busy(true);verify.setVisibility(View.GONE);message.setText(authenticate?"正在登录学习通…":"正在刷新作业…");
  worker.execute(()->{
   try{
    if(authenticate)connection.login(username,secret);
    ChaoxingClient.Result snapshot=connection.sync(status->runOnUiThread(()->{if(current(token))message.setText(status);}));
    runOnUiThread(()->{if(current(token)){busy(false);showResult(snapshot);}});
   }catch(Exception e){runOnUiThread(()->{if(current(token)){busy(false);message.setText("读取未完成。请检查账号密码或网络；如需验证码、机构登录，请使用官方验证入口。已有读取结果保留。");verify.setVisibility(View.VISIBLE);}});}
  });
 }
 private boolean current(int token){return token==generation&&!isFinishing()&&!isDestroyed();}
 private void showResult(ChaoxingClient.Result snapshot){
  results.removeAllViews();message.setText("已读取 "+snapshot.checked+" / "+snapshot.total+" 门课程，识别到 "+snapshot.items.size()+" 份作业。"+(snapshot.warnings.isEmpty()?"":"部分信息未完成，请查看下方说明。"));
  for(String warning:snapshot.warnings){TextView note=ui.text(warning,13,0xff885564);note.setPadding(ui.dp(4),0,ui.dp(4),ui.dp(12));results.addView(note);}
  if(snapshot.items.isEmpty()){LinearLayout empty=card(results);empty.addView(ui.text(snapshot.warnings.isEmpty()?"当前课程没有作业":"暂未读取到作业",18,SecondaryUi.INK));empty.addView(ui.text(snapshot.warnings.isEmpty()?"之后可以点击刷新作业。":"这不代表没有作业，请检查未完成的课程后重试。",14,SecondaryUi.MUTED));}
  for(ChaoxingClient.Work work:snapshot.items){LinearLayout item=card(results);TextView course=ui.text(work.course,13,SecondaryUi.MUTED);item.addView(course);TextView title=ui.text(work.title,18,SecondaryUi.INK);title.setTypeface(null,Typeface.BOLD);title.setPadding(0,ui.dp(10),0,ui.dp(10));title.setTextIsSelectable(true);item.addView(title);item.addView(ui.text(work.status.isEmpty()?"状态未知":work.status,14,SecondaryUi.NAVY));TextView due=ui.text("截止："+work.deadline,14,SecondaryUi.MUTED);due.setPadding(0,ui.dp(8),0,0);item.addView(due);}
 }
 @SuppressLint("SetJavaScriptEnabled")
 private void verify(){
  if(busy)return;browser=new WebView(this);WebSettings settings=browser.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);CookieManager.getInstance().setAcceptThirdPartyCookies(browser,false);
  browser.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return !ChaoxingHomework.trusted(request.getUrl().toString());}
   @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap icon){if(!ChaoxingHomework.trusted(url))view.stopLoading();}
   @Override public void onPageFinished(WebView view,String url){if(ChaoxingHomework.trusted(url)&&"i.chaoxing.com".equals(android.net.Uri.parse(url).getHost()))verified();}
   @Override public void onReceivedSslError(WebView view,SslErrorHandler handler,android.net.http.SslError error){handler.cancel();}
  });
  LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.addView(browser,new LinearLayout.LayoutParams(-1,Math.max(ui.dp(160),(int)(getResources().getDisplayMetrics().heightPixels*.5))));
  verification=new AlertDialog.Builder(this,R.style.SecondaryDialog).setTitle("学习通官方验证").setView(box).setNegativeButton("取消",null).setPositiveButton("验证完成后读取",(d,w)->verified()).create();verification.setOnDismissListener(d->{if(browser!=null){box.removeView(browser);browser.stopLoading();browser.destroy();browser=null;}verification=null;});verification.show();ui.styleDialog(verification,false);int token=generation;CookieManager.getInstance().removeAllCookies(done->{if(current(token)&&browser!=null){WebStorage.getInstance().deleteAllData();browser.loadUrl("https://passport2.chaoxing.com/login?newversion=true&refer=https%3A%2F%2Fi.chaoxing.com");}});
 }
 private void verified(){if(busy||isFinishing()||isDestroyed())return;CookieManager.getInstance().flush();if(client!=null)client.close();client=new ChaoxingClient(url->CookieManager.getInstance().getCookie(url));results.removeAllViews();if(verification!=null)verification.dismiss();start(false);}
 private void disconnect(){generation++;if(client!=null)client.close();client=null;password.setText("");account.setText("");results.removeAllViews();busy(false);message.setText("已退出学习通，当前读取结果已清除。");CookieManager.getInstance().removeAllCookies(done->CookieManager.getInstance().flush());WebStorage.getInstance().deleteAllData();}
 @Override protected void onDestroy(){generation++;password.setText("");if(verification!=null)verification.dismiss();if(client!=null)client.close();worker.shutdownNow();super.onDestroy();}
}
