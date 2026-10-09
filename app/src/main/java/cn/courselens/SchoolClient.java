package cn.courselens;

import java.net.*;
import javax.net.ssl.HttpsURLConnection;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.json.*;

public final class SchoolClient {
 public static final String HOST="https://vpn.squ.edu.cn",BASE=HOST+"/https/webvpne219e91d3788da04dcec4cdddadd1b9e/student",TABLE=BASE+"/for-std/course-table";
 private final CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ORIGINAL_SERVER);
 static boolean passwordRejected(String message){return message!=null&&!message.contains("验证码")&&message.matches("(?is).*(?:密码错误|密码不正确|密码有误|用户名或密码错误|invalid password|incorrect password).*");}
 private Response casPage;private String captchaUrl=HOST+"/enlink/sso/login/getVerifyCodeImage";private static class AuthNeeded extends IOException {AuthNeeded(){super("还需完成学校统一身份认证。");}}
 private String key="",action=""; private final Map<String,String> hidden=new LinkedHashMap<>();public boolean captchaRequired;public String cachedCourses="",courseNotes="";
 public static final class Response {public final String url,text;Response(String u,String t){url=u;text=t;}}
 public static boolean allowed(String address){try{URL u=new URL(address);return "https".equals(u.getProtocol())&&"vpn.squ.edu.cn".equals(u.getHost())&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null;}catch(Exception e){return false;}}
 private byte[] request(String address,String method,byte[] body,String type,int redirects)throws Exception {
  if(redirects>12)throw new IOException("学校登录重定向过多。");if(!allowed(address))throw new IOException("学校要求跳转到非HTTPS认证入口，已停止发送账号密码。请联系学校确认安全登录方式。");
  HttpsURLConnection conn=(HttpsURLConnection)new URL(address).openConnection();conn.setConnectTimeout(15000);conn.setReadTimeout(20000);conn.setInstanceFollowRedirects(false);conn.setRequestMethod(method);conn.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36");conn.setRequestProperty("Referer",HOST+"/enlink/sso/login");
  URI uri=URI.create(address);for(var h:cookies.get(uri,Collections.emptyMap()).entrySet())conn.setRequestProperty(h.getKey(),String.join("; ",h.getValue()));
  try {
   if(body!=null){conn.setDoOutput(true);conn.setRequestProperty("Content-Type",type);try(OutputStream os=conn.getOutputStream()){os.write(body);}}
   int status=conn.getResponseCode();cookies.put(uri,conn.getHeaderFields());
   if(status>=300&&status<400){String target=new URL(new URL(address),conn.getHeaderField("Location")).toString();if(status==307||status==308)return request(target,method,body,type,redirects+1);return request(target,"GET",null,null,redirects+1);}
   if(status>=400)throw new IOException("学校接口返回 "+status+"，请稍后重试。");
   try(InputStream in=conn.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>8*1024*1024)throw new IOException("学校返回内容过大。");out.write(buffer,0,n);}lastUrl=address;return out.toByteArray();}
  } finally {conn.disconnect();}
 }
 public java.util.function.Consumer<String> onDiagnostic=summary->{};
 private String lastUrl;
 private Response get(String url)throws Exception {byte[] b=request(url,"GET",null,null,0);return new Response(lastUrl,new String(b,StandardCharsets.UTF_8));}
 private Response post(String url,String body,String type)throws Exception {byte[] b=request(url,"POST",body.getBytes(StandardCharsets.UTF_8),type,0);return new Response(lastUrl,new String(b,StandardCharsets.UTF_8));}
 public void prepare()throws Exception {
  cachedCourses="";Response page=get(TABLE);Document doc=Jsoup.parse(page.text,page.url);Element form=doc.selectFirst("#upLoignForm");if(form==null){if(doc.selectFirst("#casLoginForm")!=null){casPage=page;captchaRequired=false;return;}cachedCourses=sync();return;}action=form.absUrl("action");key=new JSONObject(SchoolParser.scriptJson(page.text,"indexConfig")).getString("key");
  hidden.clear();for(Element input:form.select("input[type=hidden][name]"))hidden.put(input.attr("name"),input.attr("value"));JSONObject diy=new JSONObject(SchoolParser.scriptJson(page.text,"diyConfig"));captchaRequired=!diy.getJSONObject("hide").optBoolean("captcha",true);
 }
 public byte[] captcha()throws Exception{return request(captchaUrl+(captchaUrl.contains("?")?"&":"?")+"ts="+System.currentTimeMillis(),"GET",null,null,0);}
 public static String encrypt(String pass,String key)throws Exception {
  Cipher cipher=Cipher.getInstance("AES/CBC/PKCS5Padding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),"AES"),new IvParameterSpec(new StringBuilder(key).reverse().toString().getBytes(StandardCharsets.UTF_8)));return java.util.Base64.getEncoder().encodeToString(cipher.doFinal(pass.getBytes(StandardCharsets.UTF_8)));
 }
 private static String form(Map<String,String> values)throws Exception {List<String> rows=new ArrayList<>();for(var e:values.entrySet())rows.add(URLEncoder.encode(e.getKey(),"UTF-8")+"="+URLEncoder.encode(e.getValue(),"UTF-8"));return String.join("&",rows);}
 public String login(String username,String password,String captcha)throws Exception {
  if(casPage!=null)return casLogin(username,password,captcha);if(key.isEmpty())prepare();if(casPage!=null)return casLogin(username,password,captcha);Map<String,String> fields=new LinkedHashMap<>(hidden);fields.put("username",username);fields.put("password",encrypt(password,key));fields.put("token",key);fields.put("language","zh-CN");fields.put("verifyCode",captcha);
  Response response=post(action,form(fields),"application/x-www-form-urlencoded; charset=UTF-8");
  if(response.text.contains("id=\"upLoignForm\"")||response.text.contains("id='upLoignForm'")){
   Matcher error=Pattern.compile("var\\s+errMsg\\s*=\\s*(.*?);").matcher(response.text);String message="登录未成功，请检查学校VPN账号密码。";if(error.find()&&!error.group(1).equals("null")){try{Object value=new JSONTokener(error.group(1)).nextValue();message=value instanceof String?(String)value:message;}catch(Exception ignored){}}
   key="";throw new IOException(message);
  }
  try{return sync();}catch(AuthNeeded e){return casLogin(username,password,captcha);}
 }
 private String casLogin(String username,String password,String code)throws Exception {
  Document doc=Jsoup.parse(casPage.text,casPage.url);Element login=doc.selectFirst("#casLoginForm");if(login==null)throw new IOException("学校认证表单未找到。");String target=login.absUrl("action");if(target.isEmpty())target=casPage.url;
  captchaUrl=new URL(new URL(casPage.url),"captcha.html").toString();
  String needs=get(new URL(new URL(casPage.url),"needCaptcha.html?username="+URLEncoder.encode(username,"UTF-8")).toString()).text.trim();captchaRequired=needs.equals("true");if(captchaRequired&&code.isEmpty())throw new IOException("学校统一认证需要验证码，请输入下方图片中的字符，然后再次点击登录。");
  Map<String,String> fields=new LinkedHashMap<>();for(Element input:login.select("input[type=hidden][name]"))fields.put(input.attr("name"),input.attr("value"));fields.put("username",username);fields.put("password",password);fields.put("captchaResponse",code);
  Response response=post(target,form(fields),"application/x-www-form-urlencoded; charset=UTF-8");Document after=Jsoup.parse(response.text,response.url);if(after.selectFirst("#casLoginForm")!=null){casPage=response;Element error=after.selectFirst("#msg,.auth_error,.errors,#errorMsg,#formErrorTip,.error-msg");String message=error==null?"学校统一认证未成功，请检查密码或验证码。":error.text();throw new IOException(message.isEmpty()?"学校统一认证未成功，请检查密码或验证码。":message);}
  casPage=null;captchaRequired=false;return sync();
 }
 public String sync()throws Exception {
  Response page=get(TABLE);Document doc=Jsoup.parse(page.text,page.url);onDiagnostic.accept(describe(page));if(doc.selectFirst("#casLoginForm")!=null){casPage=page;throw new AuthNeeded();}String courses;
  if(doc.selectFirst("#upLoignForm")!=null)throw new IOException("学校登录会话已失效，请重新登录。");
  // Use the same print-data endpoint as the school’s native course-table script.
  Matcher id=Pattern.compile("/course-table/(?:info|index)/(\\d+)").matcher(page.url);String dataId=id.find()?id.group(1):"";
  String semester=value(doc,page.text,"semesterId"),biz=value(doc,page.text,"bizTypeId");if(semester.isEmpty()){Matcher current=Pattern.compile("(?s)(?:var|let|const)\\s+currentSemester\\s*=\\s*(\\{.*?\\})\\s*;").matcher(page.text);if(current.find())semester=""+new JSONObject(current.group(1)).getInt("id");}if(dataId.isEmpty())dataId=value(doc,page.text,"dataId");if(dataId.isEmpty())dataId=value(doc,page.text,"studentId");if(dataId.isEmpty()){Matcher student=Pattern.compile("[\"']?studentIds[\"']?\\s*[:=]\\s*\\[\\s*[\"']?(\\d+)").matcher(page.text);if(student.find())dataId=student.group(1);}
  if(!dataId.isEmpty()&&!semester.isEmpty()&&!biz.isEmpty()){
   String summary=get(TABLE+"/get-data?bizTypeId="+biz+"&semesterId="+semester+"&dataId="+dataId).text;onDiagnostic.accept(describe(page)+"\nsummary="+shape(new JSONTokener(summary).nextValue(),0));JSONObject json=new JSONObject(summary);JSONArray ids=json.optJSONArray("lessonIds");if(ids!=null&&ids.length()>0){String schedule=get(TABLE+"/semester/"+semester+"/print-data?semesterId="+semester+"&hasExperiment=true").text;onDiagnostic.accept(describe(page)+"\nsummary="+shape(new JSONTokener(summary).nextValue(),0)+"\nschedule="+shape(new JSONTokener(schedule).nextValue(),0));courses=SchoolParser.jsonCourses(schedule,summary);courseNotes=SchoolParser.scheduleNotes(schedule);if(!courses.isEmpty())return courses;}
  }
  courses=SchoolParser.htmlCourses(page.text);if(!courses.isEmpty())return courses;
  for(Element frame:doc.select("iframe[src]")){String url=frame.absUrl("src");if(allowed(url)){String content=get(url).text;courses=SchoolParser.htmlCourses(content);if(!courses.isEmpty())return courses;}}
  throw new IOException("已连接学校，但课程数据格式尚未匹配。已保留原课表。请将此提示反馈给开发者。");
 }
 private static String shape(Object value,int depth)throws Exception {if(depth>6)return "…";if(value instanceof JSONObject){JSONObject o=(JSONObject)value;StringBuilder s=new StringBuilder("{");Iterator<String> keys=o.keys();while(keys.hasNext()){String key=keys.next();if(key.toLowerCase(Locale.ROOT).matches(".*(password|token|cookie|ticket|csrf|session).*") )continue;s.append(key).append(':').append(shape(o.get(key),depth+1)).append(',');}return s.append('}').toString();}if(value instanceof JSONArray){JSONArray a=(JSONArray)value;return "[length="+a.length()+","+(a.length()>0?shape(a.get(0),depth+1):"")+"]";}return value instanceof Number?"number":value instanceof Boolean?"boolean":"string";}
 private static String describe(Response page){Document doc=Jsoup.parse(page.text,page.url);StringBuilder out=new StringBuilder("path="+java.net.URI.create(page.url).getPath()+"\ntitle="+doc.title()+"\n");for(Element form:doc.select("form")){out.append("form ").append(form.attr("id")).append(" ").append(form.attr("method")).append(" ").append(form.attr("action").split("\\?")[0]).append('\n');for(Element in:form.select("input,select")){out.append("field ").append(in.attr("name")).append(" type=").append(in.attr("type"));String v=in.attr("value");if(in.attr("name").matches("semesterId|bizTypeId|dataId")&&v.matches("\\d+"))out.append(" value=").append(v);out.append('\n');}}for(Element script:doc.select("script")){if(script.hasAttr("src"))out.append("script=").append(script.attr("src").split("\\?")[0]).append('\n');else{Matcher m=Pattern.compile(".{0,60}(?:currentSemester|studentId|personId|semesterId|bizTypeId|dataId|course-table|schedule-table|lessonIds).{0,120}").matcher(script.data());while(m.find()){String line=m.group();if(!line.toLowerCase(Locale.ROOT).matches(".*(password|token|cookie|ticket|csrf|session).*"))out.append(line).append('\n');}}}for(Element f:doc.select("iframe[src]"))out.append("iframe=").append(f.attr("src").split("\\?")[0]).append('\n');return out.toString();}
 private static String value(Document doc,String html,String field){Element selected=doc.selectFirst("select[name="+field+"] option[selected],select#"+field+" option[selected],input[name="+field+"][value]");if(selected!=null&&selected.attr("value").matches("\\d+"))return selected.attr("value");Matcher m=Pattern.compile("[\"']?"+field+"[\"']?\\s*[:=]\\s*[\"']?(\\d+)").matcher(html);return m.find()?m.group(1):"";}
 public void logout(){cookies.getCookieStore().removeAll();key="";casPage=null;captchaRequired=false;captchaUrl=HOST+"/enlink/sso/login/getVerifyCodeImage";}
}
