package cn.courselens;

import android.annotation.SuppressLint;
import android.app.*;
import android.os.Bundle;
import android.view.View;
import android.webkit.*;
import android.widget.*;
import org.json.*;

/** Local research preview; reads only list text after an explicit user action. */
public final class ChaoxingActivity extends Activity {
 private WebView web;
 private TextView message;
 private Button read;
 private long navigation;
 // shortcut: known list containers and work links only; replace with verified adapters after account testing.
 private static final String EXTRACT="(function(){var rows=[],seen=new Set(),docs=[document];for(var n=0;n<docs.length&&n<20;n++){var d=docs[n];for(var f of d.querySelectorAll('iframe')){try{if(f.contentDocument)docs.push(f.contentDocument);}catch(e){}}for(var e of d.querySelectorAll('[data-workid],.work-list li,.workList li,.work-list-item,li')){if(rows.length>=200)break;if(!e.matches('[data-workid],.work-list li,.workList li,.work-list-item')&&!e.querySelector('a[href*=work],a[href*=Work]'))continue;var t=(e.innerText||'').trim();if(t&&t.length<=8000&&!seen.has(t)){seen.add(t);rows.push(t);}}}return JSON.stringify(rows);})()";

 @SuppressLint("SetJavaScriptEnabled")
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);setContentView(root);
  message=new TextView(this);message.setPadding(20,16,20,16);message.setText("在官方页面登录，进入课程的作业列表后，点击读取。仅预览当前已加载页面，不代表全部课程；结果暂不保存。");message.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);root.addView(message);
  LinearLayout actions=new LinearLayout(this);root.addView(actions);
  Button back=new Button(this);back.setText("返回");actions.addView(back);back.setOnClickListener(v->{if(web.canGoBack())web.goBack();else finish();});
  read=new Button(this);read.setText("读取当前作业页");actions.addView(read,new LinearLayout.LayoutParams(0,-2,1));read.setOnClickListener(v->readPage());
  Button logout=new Button(this);logout.setText("退出登录");actions.addView(logout);logout.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("清除本预研 App 内的学习通登录状态？").setNegativeButton("取消",null).setPositiveButton("退出",(d,w)->logout()).show());
  web=new WebView(this);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
  WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setSupportMultipleWindows(false);
  CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){if(ChaoxingHomework.trusted(request.getUrl().toString()))return false;message.setText("该链接不在学习通 HTTPS 页面范围内，未打开。");return true;}
   @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap icon){navigation++;read.setEnabled(false);if(!ChaoxingHomework.trusted(url)){view.stopLoading();message.setText("已停止打开非学习通页面。");}}
   @Override public void onPageFinished(WebView view,String url){read.setEnabled(ChaoxingHomework.trusted(url));}
   @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error){if(request.isForMainFrame())message.setText("页面加载失败，请检查网络后重新打开登录页。");}
   @Override public void onReceivedSslError(WebView view,SslErrorHandler handler,android.net.http.SslError error){handler.cancel();message.setText("网页证书验证失败，已停止加载。");}
  });
  web.loadUrl("https://passport2.chaoxing.com/login?newversion=true&refer=https%3A%2F%2Fi.chaoxing.com");
 }
 private void readPage(){
  if(!ChaoxingHomework.trusted(web.getUrl()))return;
  long source=navigation;read.setEnabled(false);
  web.evaluateJavascript(EXTRACT,value->{
   if(isFinishing()||isDestroyed()||source!=navigation)return;
   read.setEnabled(true);
   try{
    if(value==null||value.length()>2000000)throw new IllegalArgumentException();
    Object decoded=new JSONTokener(value).nextValue();if(!(decoded instanceof String))throw new IllegalArgumentException();
    JSONArray rows=new JSONArray((String)decoded);if(rows.length()>200)throw new IllegalArgumentException();
    if(rows.length()==0){message.setText("未识别到作业条目。请确认已进入作业列表；也可能是页面结构或跨域框架尚未适配，不能据此判断没有作业。");return;}
    StringBuilder preview=new StringBuilder();for(int i=0;i<rows.length();i++){ChaoxingHomework item=ChaoxingHomework.parse(rows.getString(i));preview.append(i+1).append(". ").append(item.title).append("\n").append(item.status).append(" · ").append(item.deadline).append("\n原文：").append(item.details).append("\n\n");}
    TextView text=new TextView(this);text.setPadding(24,16,24,16);text.setText(preview);text.setTextIsSelectable(true);ScrollView scroll=new ScrollView(this);scroll.addView(text);
    new AlertDialog.Builder(this).setTitle("当前页面候选作业（"+rows.length()+"）").setView(scroll).setPositiveButton("关闭",null).show();message.setText("已预览当前页面候选作业，请对照原文核对。未自动遍历课程、翻页或保存结果。");
   }catch(JSONException|IllegalArgumentException e){message.setText("读取结果无法解析，请重新进入作业列表。当前未保存任何结果。");}
  });
 }
 private void logout(){navigation++;read.setEnabled(false);web.stopLoading();CookieManager.getInstance().removeAllCookies(done->{if(isFinishing()||isDestroyed())return;CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData();web.clearCache(true);web.clearHistory();web.loadUrl("https://passport2.chaoxing.com/login");message.setText("本预研 App 的网页登录状态已清除。");});}
 @Override protected void onPause(){web.onPause();CookieManager.getInstance().flush();super.onPause();}
 @Override protected void onResume(){super.onResume();if(web!=null)web.onResume();}
 @Override protected void onDestroy(){navigation++;web.stopLoading();((android.view.ViewGroup)web.getParent()).removeView(web);web.destroy();super.onDestroy();}
}
