package cn.courselens;

import android.content.*;
import android.security.keystore.*;
import android.util.Base64;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

/** Encrypted per-timetable school credentials; only written after a successful school login. */
final class SchoolAccount {
 static final String PREFS="school_accounts";
 static final class Saved {final String school,account,password;Saved(String school,String account,String password){this.school=school;this.account=account;this.password=password;}}
 private static final String ALIAS="courselens.school.credentials.v1";
 private final SharedPreferences prefs;
 private final String packageName;
 SchoolAccount(Context context){prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);packageName=context.getPackageName();}
 private static String credentialsKey(String timetable){return "credentials."+timetable;}
 private static String ivKey(String timetable){return "iv."+timetable;}
 boolean configured(String timetable){return timetable!=null&&prefs.contains(credentialsKey(timetable))&&prefs.contains(ivKey(timetable));}
 private synchronized SecretKey key(boolean create)throws Exception {
  KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
  if(store.containsAlias(ALIAS))return (SecretKey)store.getKey(ALIAS,null);
  if(!create)throw new GeneralSecurityException("保存的学校账号无法解密，请重新登录。");
  KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build());return generator.generateKey();
 }
 private byte[] context(String timetable){return (packageName+":school:"+timetable+":v1").getBytes(StandardCharsets.UTF_8);}
 synchronized void save(String timetable,String school,String account,String password)throws Exception {
  if(timetable==null||school.isEmpty()||account.trim().isEmpty()||password.isEmpty())throw new IllegalArgumentException("账号密码不能为空。");
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key(true));cipher.updateAAD(context(timetable));
  byte[] data=new JSONObject().put("school",school).put("account",account).put("password",password).toString().getBytes(StandardCharsets.UTF_8);
  try{
   String encrypted=Base64.encodeToString(cipher.doFinal(data),Base64.NO_WRAP),iv=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP);
   String previous=prefs.getString(credentialsKey(timetable),null),previousIv=prefs.getString(ivKey(timetable),null);
   if(!prefs.edit().putString(credentialsKey(timetable),encrypted).putString(ivKey(timetable),iv).commit()){
    SharedPreferences.Editor rollback=prefs.edit();if(previous==null)rollback.remove(credentialsKey(timetable));else rollback.putString(credentialsKey(timetable),previous);if(previousIv==null)rollback.remove(ivKey(timetable));else rollback.putString(ivKey(timetable),previousIv);rollback.commit();
    throw new IOException("自动刷新账号保存失败，请重试。");
   }
  }finally{java.util.Arrays.fill(data,(byte)0);}
 }
 synchronized Saved load(String timetable)throws Exception {
  if(!configured(timetable))return null;
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(false),new GCMParameterSpec(128,Base64.decode(prefs.getString(ivKey(timetable),""),Base64.NO_WRAP)));cipher.updateAAD(context(timetable));
  byte[] data=cipher.doFinal(Base64.decode(prefs.getString(credentialsKey(timetable),""),Base64.NO_WRAP));
  try{
   JSONObject value=new JSONObject(new String(data,StandardCharsets.UTF_8));String school=value.getString("school"),account=value.getString("account"),password=value.getString("password");
   if(school.isEmpty()||account.isEmpty()||password.isEmpty())throw new GeneralSecurityException("保存的学校账号不完整，请重新登录。");
   return new Saved(school,account,password);
  }finally{java.util.Arrays.fill(data,(byte)0);}
 }
 synchronized void remove(String timetable)throws IOException {
  if(timetable==null)return;
  if(!prefs.edit().remove(credentialsKey(timetable)).remove(ivKey(timetable)).commit())throw new IOException("已保存的学校账号未能删除。");
 }
}
