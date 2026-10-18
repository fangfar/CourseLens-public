package cn.courselens;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/** Read-only platform adapter; credentials are used for one login, never persisted. */
final class ChaoxingClient implements AutoCloseable {
 interface Transport {String request(String url,String form)throws Exception;}
 private static final class ReadFailure extends IOException {ReadFailure(String stage,String reason){super(stage+"："+reason);}}
 private static final class RequestFailure extends IOException {RequestFailure(String reason){super(reason);}}
 private static final class HttpFailure extends IOException {final int status;HttpFailure(int status){this.status=status;}}
 static String describe(Throwable error){if(error instanceof ReadFailure)return error.getMessage();return "读取过程异常，请重试";}
 private String fetch(String stage,String url)throws Exception {
  try{return request(url,null);}catch(InterruptedIOException e){if(closed||Thread.currentThread().isInterrupted())throw e;throw new ReadFailure(stage,"网络请求超时");}catch(RequestFailure e){throw new ReadFailure(stage,e.getMessage());}catch(HttpFailure e){throw new ReadFailure(stage,"平台返回 HTTP "+e.status);}catch(UnknownHostException e){throw new ReadFailure(stage,"网络地址解析失败");}catch(javax.net.ssl.SSLException e){throw new ReadFailure(stage,"安全连接失败");}catch(Exception e){if(closed||Thread.currentThread().isInterrupted())throw new InterruptedIOException("读取已取消");String kind=e.getClass().getSimpleName();throw new ReadFailure(stage,"连接未完成（"+(kind.matches("[A-Za-z0-9]+")?kind:"NetworkError")+"）");}
 }
 static String institution(String html){
  Matcher match=Pattern.compile("(?:[\"']fid[\"']\\s*:|(?:var|let|const)\\s+fid\\s*=|\\bfid\\s*=)\\s*[\"']?([0-9]+)").matcher(html);if(match.find())return match.group(1);
  Document page=Jsoup.parse(html);Element input=page.selectFirst("input#fid,input[name=fid]");return input!=null&&input.val().matches("[0-9]+")?input.val():"";
 }

 static final class Subject {
  final String name,id,clazz,cpi;
  Subject(String name,String id,String clazz,String cpi){this.name=name;this.id=id;this.clazz=clazz;this.cpi=cpi;}
 }
 static final class Work {
  final String course,title,status,url;String deadline="截止时间未知";
  Work(String course,String title,String status,String url){this.course=course;this.title=title;this.status=status;this.url=url;}
  boolean completed(){return status.matches(".*(已完成|已提交|已批阅|待批阅|已批改).*");}
 }
 static final class Result {
  final List<Work> items=new ArrayList<>();final List<String> warnings=new ArrayList<>();int total,checked;
 }
 private final CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ORIGINAL_SERVER);
 private final Transport transport;
 private final Function<String,String> browserCookies;
 private volatile boolean closed;
 private volatile HttpURLConnection active;
 private long lastRequest;
 ChaoxingClient(){browserCookies=null;transport=this::http;}
 ChaoxingClient(Function<String,String> browserCookies){this.browserCookies=browserCookies;transport=this::http;}
 ChaoxingClient(Transport transport){browserCookies=null;this.transport=transport;}
 static String encrypt(String text)throws GeneralSecurityException {
  byte[] key="u2oh6Vu^HWe4_AES".getBytes(StandardCharsets.UTF_8);
  Cipher cipher=Cipher.getInstance("AES/CBC/PKCS5Padding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new IvParameterSpec(key));
  return Base64.getEncoder().encodeToString(cipher.doFinal(text.getBytes(StandardCharsets.UTF_8)));
 }
 private static String encode(String value){try{return URLEncoder.encode(value,"UTF-8");}catch(UnsupportedEncodingException e){throw new AssertionError(e);}}
 private static String decode(String value){try{return URLDecoder.decode(value,"UTF-8");}catch(UnsupportedEncodingException e){throw new AssertionError(e);}}
 static String form(String... pairs){StringJoiner data=new StringJoiner("&");for(int i=0;i<pairs.length;i+=2)data.add(encode(pairs[i])+"="+encode(pairs[i+1]));return data.toString();}
 void login(String account,String password)throws Exception {
  String response=request("https://passport2.chaoxing.com/fanyalogin",form("fid","-1","uname",encrypt(account),"password",encrypt(password),"refer","https%3A%2F%2Fi.chaoxing.com","t","true","forbidotherlogin","0","validate","","doubleFactorLogin","0","independentId","0"));
  JSONObject result;
  try{result=new JSONObject(response);}catch(Exception e){throw new IOException("登录响应无法识别，请使用官方验证入口。");}
  if(!Boolean.TRUE.equals(result.opt("status")))throw new IOException("登录未成功，请核对账号密码；如需验证码或机构认证，请使用官方验证入口。");
 }
 private String request(String url,String body)throws Exception {
  if(closed||Thread.currentThread().isInterrupted())throw new InterruptedIOException("读取已取消");
  if(!ChaoxingHomework.trusted(url))throw new IOException("平台返回了不可信链接，已停止读取。");
  return transport.request(url,body);
 }
 static List<Subject> subjects(String html)throws IOException {
  Document page=Jsoup.parse(html);List<Subject> subjects=new ArrayList<>();Set<String> seen=new HashSet<>();
  for(Element link:page.select("a[href*=stucoursemiddle],[onclick*=stucoursemiddle]")){
   String source=link.attr("href")+" "+link.attr("onclick");Matcher path=Pattern.compile("stucoursemiddle\\?([^\\s\"'<>]+)").matcher(source);if(!path.find())continue;
   Map<String,String> args=query("stucoursemiddle?"+path.group(1));String id=args.getOrDefault("courseid",""),clazz=args.getOrDefault("clazzid",""),cpi=args.getOrDefault("cpi","");
   if(!id.matches("[0-9]+")||!clazz.matches("[0-9]+")||!cpi.matches("[0-9]+"))continue;
   if(!seen.add(id+":"+clazz+":"+cpi))continue;
   Element container=link.closest("li");if(container==null)container=link.parent();Element name=container==null?null:container.selectFirst(".course-name");
   String title=name==null?link.attr("title"):name.hasAttr("title")?name.attr("title"):name.text();
   subjects.add(new Subject(title.isBlank()?"课程 "+id:title,id,clazz,cpi));
  }
  if(subjects.isEmpty()&&!page.text().matches(".*(暂无课程|还没有课程|没有相关课程|暂无相关课程).*"))throw new IOException("课程列表未识别，可能登录已失效或平台页面变化。");
  return subjects;
 }
 private static Map<String,String> query(String url){Map<String,String> result=new HashMap<>();int at=url.indexOf('?');if(at<0)return result;for(String part:url.substring(at+1).split("&")){String[] kv=part.split("=",2);if(kv.length==2)try{result.put(decode(kv[0]),decode(kv[1]));}catch(IllegalArgumentException ignored){}}return result;}
 static String token(Document page,String name){Element field=page.getElementById(name);return field==null?"":field.val();}
 static List<Work> works(String html,String course)throws IOException {
  Document page=Jsoup.parse(html);List<Work> result=new ArrayList<>();Set<String> seen=new HashSet<>();
  for(Element row:page.select("li[data][onclick*=goTask]")){
   Element title=row.selectFirst("p.overHidden2"),status=row.selectFirst("p.status");String path=row.attr("data");
   if(!path.startsWith("http")&&!path.startsWith("/"))try{path=decode(path);}catch(IllegalArgumentException e){throw new IOException("作业链接无法解析。");}
   String url;try{url=URI.create("https://mooc1.chaoxing.com/").resolve(path).toString();}catch(IllegalArgumentException e){throw new IOException("作业链接无法解析。");}
   if(title==null||title.text().isBlank()||!safeDetail(url))throw new IOException("作业条目结构变化，已停止该课程读取。");
   if(seen.add(url))result.add(new Work(course,title.text(),status==null?"状态未知":status.text(),url));
  }
  if(result.isEmpty()&&!page.text().matches(".*(暂无作业|没有作业|还没有作业|暂无相关作业|0/0).*"))throw new IOException("作业列表未识别，不能确认该课程没有作业。");
  return result;
 }
 static boolean safeDetail(String url){if(!ChaoxingHomework.trusted(url))return false;String path=URI.create(url).getPath();return path.equals("/mooc-ans/mooc2/work/task")||path.equals("/mooc2/work/task");}
 static String deadline(String html){
  String text=Jsoup.parse(html).text();String time="(?:[0-9]{4}[-/.年])?[0-9]{1,2}[-/.月][0-9]{1,2}日?\\s+[0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?";
  Matcher direct=Pattern.compile("(?:截止时间|截止日期|截止)\\s*[:：]?\\s*("+time+")").matcher(text);if(direct.find())return direct.group(1);
  Matcher range=Pattern.compile("作答时间\\s*[:：]\\s*"+time+"\\s*(?:至|到|~|—)\\s*("+time+")").matcher(text);
  return range.find()?range.group(1):"截止时间未知";
 }
 Result sync(Consumer<String> progress)throws Exception {
  progress.accept("正在读取账号信息…");String base=fetch("账号信息","https://i.chaoxing.com/base"),fid=institution(base);
  if(fid.isEmpty())throw new ReadFailure("账号信息","页面未提供机构编号");
  progress.accept("正在读取课程列表…");String courseUrl="https://mooc2-ans.chaoxing.com/mooc2-ans/visit/courselistdata?"+form("courseType","1","courseFid",fid,"courseTeacherid","0","courseOrderBy","0");
  String courseHtml=fetch("课程列表",courseUrl);List<Subject> courses;try{courses=subjects(courseHtml);}catch(IOException e){throw new ReadFailure("课程列表","页面结构未识别，可能需要重新登录或适配页面");}Result result=new Result();result.total=courses.size();if(paged(courseHtml))result.warnings.add("课程列表存在分页，尚未读取后续课程。");
  int visited=0;for(Subject course:courses){
   progress.accept("正在识别 "+(++visited)+" / "+result.total+"："+course.name);
   try{
    Document page=Jsoup.parse(fetch("课程访问参数","https://mooc1.chaoxing.com/visit/stucoursemiddle?"+form("courseid",course.id,"clazzid",course.clazz,"cpi",course.cpi,"ismooc2","1","v","2")));
    String enc=token(page,"workEnc"),stuenc=token(page,"enc"),openc=token(page,"openc");
    if(enc.isEmpty()||stuenc.isEmpty())throw new ReadFailure("课程访问参数","未找到作业访问参数，课程可能未开放作业");
    String listUrl="https://mooc1.chaoxing.com/mooc2/work/list?"+form("courseId",course.id,"classId",course.clazz,"cpi",course.cpi,"ut","s","openc",openc,"enc",enc,"t",String.valueOf(System.currentTimeMillis()),"stuenc",stuenc,"isdisplaytable","2");
    String html=fetch("作业列表",listUrl);List<Work> items;try{items=works(html,course.name);}catch(IOException e){throw new ReadFailure("作业列表","页面未识别或无访问权限");}
    // shortcut: unknown pagination is reported as partial; add a verified paging adapter when encountered.
    if(paged(html))result.warnings.add(course.name+"：列表存在分页，当前仅识别首批作业。");
    for(Work work:items){
     if(!work.completed())try{work.deadline=deadline(fetch("截止时间",work.url));}catch(InterruptedIOException e){throw e;}catch(Exception e){result.warnings.add(course.name+" · "+work.title+"："+describe(e));}
     result.items.add(work);
    }
    result.checked++;
   }catch(InterruptedIOException e){throw e;}catch(Exception e){result.warnings.add(course.name+"："+describe(e));}
  }
  return result;
 }
 static boolean paged(String html){Document page=Jsoup.parse(html);return page.selectFirst("a[rel=next],.pageNext,.nextPage")!=null||Pattern.compile("(?:总共|共)\\s*(?:[2-9]|[1-9][0-9]+)\\s*页").matcher(page.text()).find();}
 static String redirected(String source,String location)throws IOException {
  if(location==null)throw new RequestFailure("跳转缺少地址（R01）");
  try{
   String target=new URL(new URL(source),location).toString().replace(" ","%20");URI uri=URI.create(target);
   if("http".equalsIgnoreCase(uri.getScheme())&&uri.getRawUserInfo()==null&&(uri.getPort()==-1||uri.getPort()==80)){
    String host=uri.getHost();if(host==null)throw new RequestFailure("跳转地址无效（R03）");
    target="https://"+host+(uri.getRawPath()==null?"":uri.getRawPath())+(uri.getRawQuery()==null?"":"?"+uri.getRawQuery());
   }
   if(!ChaoxingHomework.trusted(target))throw new RequestFailure("平台跳转超出学习通 HTTPS 范围（R02）");
   return target;
  }catch(IllegalArgumentException|MalformedURLException e){throw new RequestFailure("跳转地址格式无法识别（R03）");}
 }
 private String http(String initial,String body)throws Exception {
  long wait=300-(System.currentTimeMillis()-lastRequest);if(wait>0)Thread.sleep(wait);lastRequest=System.currentTimeMillis();
  String url=initial;
  for(int redirects=0;redirects<8;redirects++){
   if(closed||Thread.currentThread().isInterrupted())throw new InterruptedIOException("读取已取消");
   if(!ChaoxingHomework.trusted(url))throw new RequestFailure("平台跳转超出学习通 HTTPS 范围（R02）");
   HttpURLConnection connection=(HttpURLConnection)URI.create(url).toURL().openConnection();active=connection;
   try{
    connection.setConnectTimeout(15000);connection.setReadTimeout(20000);connection.setInstanceFollowRedirects(false);
    connection.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/130.0.0.0 Safari/537.36");connection.setRequestProperty("Referer","https://i.chaoxing.com/");
    String value="";for(Map.Entry<String,List<String>> header:cookies.get(URI.create(url),Collections.emptyMap()).entrySet())if("Cookie".equalsIgnoreCase(header.getKey()))value=String.join("; ",header.getValue());String external=browserCookies==null?null:browserCookies.apply(url);if(external!=null&&!external.isBlank())value=value.isEmpty()?external:value+"; "+external;if(!value.isEmpty())connection.setRequestProperty("Cookie",value);
    if(body!=null){connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");try(OutputStream out=connection.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));}}
    int code=connection.getResponseCode();cookies.put(URI.create(url),connection.getHeaderFields());
    if(code>=300&&code<400){String location=connection.getHeaderField("Location");url=redirected(url,location);if(body!=null&&(code==307||code==308))throw new RequestFailure("登录请求发生保留表单的跳转，已停止（R04）");body=null;continue;}
    if(code!=200)throw new HttpFailure(code);
    try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1){if(closed||Thread.currentThread().isInterrupted())throw new InterruptedIOException("读取已取消");if(out.size()+count>3*1024*1024)throw new RequestFailure("平台响应过大，已停止读取（R05）");out.write(buffer,0,count);}return out.toString(StandardCharsets.UTF_8.name());}
   }finally{connection.disconnect();active=null;}
  }
  throw new RequestFailure("平台跳转次数过多，可能登录会话未建立（R06）");
 }
 @Override public void close(){closed=true;HttpURLConnection connection=active;if(connection!=null)connection.disconnect();cookies.getCookieStore().removeAll();}
}
