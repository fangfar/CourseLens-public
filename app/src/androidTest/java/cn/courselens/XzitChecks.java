package cn.courselens;

import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import javax.crypto.Cipher;
import org.json.*;

final class XzitChecks {
 static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 static void verify()throws Exception {
  check(XzitClient.trusted("http://jwglxt.xzit.edu.cn/jwglxt/"),"School HTTP endpoint rejected");
  for(String url:new String[]{"http://jwglxt.xzit.edu.cn.evil.test/","http://user@jwglxt.xzit.edu.cn/","http://jwglxt.xzit.edu.cn:8080/","https://example.org/","file:///jwglxt.xzit.edu.cn"})check(!XzitClient.trusted(url),"Untrusted URL accepted");
  KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);KeyPair pair=generator.generateKeyPair();RSAPublicKey key=(RSAPublicKey)pair.getPublic();Base64.Encoder encoder=Base64.getEncoder();
  String encrypted=XzitClient.encryptPassword("synthetic-password",encoder.encodeToString(key.getModulus().toByteArray()),encoder.encodeToString(key.getPublicExponent().toByteArray()));Cipher cipher=Cipher.getInstance("RSA/ECB/PKCS1Padding");cipher.init(Cipher.DECRYPT_MODE,pair.getPrivate());check(new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)),java.nio.charset.StandardCharsets.UTF_8).equals("synthetic-password"),"School RSA encryption differs");
  JSONObject row=new JSONObject().put("xqj","3").put("jcs","12-13").put("kcmc","合成晚课").put("xqmc","测试校区").put("cdmc","测试楼").put("xm","合成教师").put("zcd","1-3周,5-16周(单)").put("sxbj","1").put("jxbsftkbj","1");
  JSONObject root=new JSONObject().put("kbList",new JSONArray().put(row));List<Course> courses=Course.parse(SchoolParser.xzitCourses(root.toString()));check(courses.size()==1&&courses.get(0).end==13&&courses.get(0).active(3)&&courses.get(0).active(5)&&!courses.get(0).active(6)&&!courses.get(0).active(4),"Split/odd weeks or evening classes lost");check(courses.get(0).teacher.equals("合成教师")&&courses.get(0).schoolAdjusted()&&courses.get(0).displayDetails().isEmpty(),"School adjustment displayed as experiment content");
  row.put("zcd","第7周");check(Course.parse(SchoolParser.xzitCourses(root.toString())).get(0).weeks.equals("7"),"Single week lost");
  row.put("sxbj","0");check(SchoolParser.xzitCourses(root.toString()).isEmpty(),"Pending course imported");row.put("sxbj","1");row.put("jcs","1-31");boolean rejected=false;try{SchoolParser.xzitCourses(root.toString());}catch(Exception e){rejected=true;}check(rejected,"Invalid course accepted");
  JSONArray times=new JSONArray();for(int i=0;i<14;i++)times.put(new JSONObject().put("jcmc",""+(i+1)).put("qssj",ClassTimes.XZIT.starts[i]).put("jssj",ClassTimes.XZIT.ends[i]));check(SchoolParser.xzitTimes(times.toString()).encode().equals(ClassTimes.XZIT.encode()),"Dynamic school times lost");times.remove(0);rejected=false;try{SchoolParser.xzitTimes(times.toString());}catch(Exception e){rejected=true;}check(rejected,"Missing school period silently accepted");
  check(SchoolParser.xzitNotes("{sjkList:[{sjkcgs:'合成实践 第17周'},{qtkcgs:'合成其他课程'}]}").contains("第17周"),"Unscheduled practice notes lost");
  check(!android.security.NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted("example.org"),"Global HTTP enabled");check(android.security.NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted("jwglxt.xzit.edu.cn"),"School HTTP exemption missing");check(!android.security.NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted("child.jwglxt.xzit.edu.cn"),"School HTTP exemption expanded to subdomains");
 }
}
