package cn.courselens;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;
import javax.crypto.Cipher;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

public final class XzitClient extends SchoolClient {
 static final String BASE="http://jwglxt.xzit.edu.cn/jwglxt",LOGIN=BASE+"/xtgl/login_slogin.html",TABLE=BASE+"/kbcx/xskbcx_cxXskbcxIndex.html?gnmkdm=N2151&layout=default";
 private final CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ORIGINAL_SERVER);
 private final Map<String,String> fields=new LinkedHashMap<>();
 private String action="",modulus="",exponent="";private boolean authenticated;
 public XzitClient(){classTimes=ClassTimes.XZIT;}
 static boolean trusted(String address){try{URL u=new URL(address);return u.getHost().equals("jwglxt.xzit.edu.cn")&&u.getUserInfo()==null&&((u.getProtocol().equals("http")&&(u.getPort()==-1||u.getPort()==80))||(u.getProtocol().equals("https")&&(u.getPort()==-1||u.getPort()==443)));}catch(Exception e){return false;}}
 private byte[] request(String address,String method,String body,int redirects)throws Exception {
  if(!trusted(address)||redirects>8)throw new IOException("学校认证跳转异常，已停止请求。");
  HttpURLConnection connection=(HttpURLConnection)new URL(address).openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(15000);connection.setReadTimeout(20000);connection.setRequestMethod(method);
  connection.setRequestProperty("User-Agent","Mozilla/5.0");connection.setRequestProperty("Referer",LOGIN);URI uri=URI.create(address);
  for(var h:cookies.get(uri,Collections.emptyMap()).entrySet())connection.setRequestProperty(h.getKey(),String.join("; ",h.getValue()));
  try{
   if(body!=null){connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");try(OutputStream out=connection.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));}}
   int status=connection.getResponseCode();cookies.put(uri,connection.getHeaderFields());
   if(status>=300&&status<400){String target=new URL(new URL(address),connection.getHeaderField("Location")).toString();return request(target,status==307||status==308?method:"GET",status==307||status==308?body:null,redirects+1);}
   if(status!=200)throw new IOException("学校接口返回 "+status+"，请稍后重试。");
   try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>8*1024*1024)throw new IOException("学校返回内容过大。");out.write(buffer,0,n);}return out.toByteArray();}
  }catch(SocketTimeoutException e){throw new IOException("徐州工程学院连接超时，请稍后重试。",e);}finally{connection.disconnect();}
 }
 private String get(String url)throws Exception{return new String(request(url,"GET",null,0),StandardCharsets.UTF_8);}
 private String post(String url,Map<String,String> values)throws Exception {List<String> body=new ArrayList<>();for(var e:values.entrySet())body.add(URLEncoder.encode(e.getKey(),"UTF-8")+"="+URLEncoder.encode(e.getValue(),"UTF-8"));return new String(request(url,"POST",String.join("&",body),0),StandardCharsets.UTF_8);}
 @Override public void prepare()throws Exception {
  cachedCourses="";
  if(authenticated)try{cachedCourses=sync();return;}catch(AuthRequired e){authenticated=false;}
  Document doc=Jsoup.parse(get(LOGIN),LOGIN);Element form=doc.selectFirst("form");
  if(form==null||form.selectFirst("#yhm")==null){cachedCourses=sync();authenticated=true;return;}
  readForm(doc);
  JSONObject key=new JSONObject(get(BASE+"/xtgl/login_getPublicKey.html?time="+System.currentTimeMillis()));modulus=key.getString("modulus");exponent=key.getString("exponent");
 }
 private void readForm(Document doc)throws Exception {
  Element form=doc.selectFirst("form");if(form==null||form.selectFirst("#yhm")==null)throw new IOException("学校登录表单已变化，请稍后重试。");
  action=form.absUrl("action");if(action.isEmpty())action=LOGIN;if(!trusted(action))throw new IOException("学校认证地址异常。");fields.clear();
  for(Element input:form.select("input[type=hidden][name]"))fields.put(input.attr("name"),input.attr("value"));
  Element required=doc.selectFirst("#dlsfbxyzm"),captcha=doc.selectFirst("#yzmDiv");captchaRequired=(required!=null&&required.attr("value").equals("1"))||(captcha!=null&&!captcha.attr("style").replace(" ","").contains("display:none"));
 }
 static String encryptPassword(String password,String modulus,String exponent)throws Exception {
  Base64.Decoder decoder=Base64.getDecoder();PublicKey key=KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(new BigInteger(1,decoder.decode(modulus)),new BigInteger(1,decoder.decode(exponent))));
  Cipher cipher=Cipher.getInstance("RSA/ECB/PKCS1Padding");cipher.init(Cipher.ENCRYPT_MODE,key);return Base64.getEncoder().encodeToString(cipher.doFinal(password.getBytes(StandardCharsets.UTF_8)));
 }
 @Override public byte[] captcha()throws Exception{return request(BASE+"/kaptcha?time="+System.currentTimeMillis(),"GET",null,0);}
 @Override public String login(String username,String password,String code)throws Exception {
  if(modulus.isEmpty())prepare();Map<String,String> body=new LinkedHashMap<>(fields);body.put("yhm",username);body.put("mm",encryptPassword(password,modulus,exponent));body.put("language","zh_CN");if(!code.isEmpty())body.put("yzm",code);
  Document response=Jsoup.parse(post(action,body),LOGIN);
  if(response.selectFirst("#yhm")!=null){readForm(response);Element tips=response.selectFirst("#tips");String text=tips==null?"":tips.text();if(text.contains("验证码"))throw new AuthRequired("学校验证码错误或已过期，请重新输入。");if(text.contains("密码"))throw new AuthRequired("用户名或密码错误，请重新输入。");throw new AuthRequired("学校登录未成功，请在学校网页确认账号状态后重试。");}
  String next=sync();authenticated=true;return next;
 }
 @Override public String sync()throws Exception {
  Document doc=Jsoup.parse(get(TABLE),TABLE);if(doc.selectFirst("#yhm")!=null){authenticated=false;throw new AuthRequired("学校登录会话已失效，请重新登录。");}
  Map<String,String> query=new LinkedHashMap<>();for(String field:new String[]{"xnm","xqm"}){Element selected=doc.selectFirst("#"+field+" option[selected]");if(selected==null||!selected.attr("value").matches("\\d{1,4}"))throw new IOException("学校当前学期未识别，已保留原课表。");query.put(field,selected.attr("value"));}query.put("kzlx","ck");
  String json=post(BASE+"/kbcx/xskbcx_cxXsgrkb.html?gnmkdm=N2151",query);JSONObject root=new JSONObject(json);
  if(root.optString("xnxqsfkz").equals("true"))throw new IOException("学校尚未开放当前学期课表，已保留原课表。");
  String courses=SchoolParser.xzitCourses(json);if(courses.isEmpty())throw new IOException("学校未返回已选课程，已保留原课表。");
  Set<String> campuses=new LinkedHashSet<>();org.json.JSONArray items=root.getJSONArray("kbList");for(int i=0;i<items.length();i++)if(!items.getJSONObject(i).optString("sxbj").equals("0"))campuses.add(items.getJSONObject(i).optString("xqh_id",""));
  if(campuses.size()>8)throw new IOException("学校返回的校区过多，已保留原课表。");ClassTimes times=null;
  for(String campus:campuses){query.put("xqh_id",campus);ClassTimes next=SchoolParser.xzitTimes(post(BASE+"/kbcx/xskbcx_cxRjc.html",query));if(times!=null&&!times.encode().equals(next.encode()))throw new IOException("不同校区的作息不一致，暂不能合并导入。已保留原课表。");times=next;}
  if(times==null)throw new IOException("学校未返回作息，已保留原课表。");times.validate(Course.parse(courses));classTimes=times;courseNotes=SchoolParser.xzitNotes(json);return courses;
 }
 @Override public void logout(){cookies.getCookieStore().removeAll();fields.clear();action="";modulus="";exponent="";authenticated=false;captchaRequired=false;cachedCourses="";}
}
