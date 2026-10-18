package cn.courselens;

import android.content.*;
import android.security.keystore.*;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

/** Only called after successful platform authentication; no plaintext credentials on disk. */
final class ChaoxingAccount {
 static final class Saved {final String account,password;Saved(String account,String password){this.account=account;this.password=password;}}
 private static final String ALIAS="courselens.chaoxing.credentials.v1";
 private final SharedPreferences prefs;
 private final byte[] context;
 ChaoxingAccount(Context activity){prefs=activity.getSharedPreferences("chaoxing_account",Context.MODE_PRIVATE);context=(activity.getPackageName()+":chaoxing:v1").getBytes(StandardCharsets.UTF_8);}
 boolean configured(){return prefs.contains("credentials")&&prefs.contains("iv");}
 private SecretKey key(boolean create)throws Exception {
  KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
  if(store.containsAlias(ALIAS))return (SecretKey)store.getKey(ALIAS,null);
  if(!create)throw new GeneralSecurityException("保存的配置无法解密，请重新配置。");
  KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build());return generator.generateKey();
 }
 void save(String account,String password)throws Exception {
  if(account.trim().isEmpty()||password.isEmpty())throw new IllegalArgumentException("账号密码不能为空。");
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key(true));cipher.updateAAD(context);
  byte[] data=new JSONObject().put("account",account).put("password",password).toString().getBytes(StandardCharsets.UTF_8);
  try{String encrypted=Base64.encodeToString(cipher.doFinal(data),Base64.NO_WRAP),iv=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP);String previous=prefs.getString("credentials",null),previousIv=prefs.getString("iv",null);if(!prefs.edit().putString("credentials",encrypted).putString("iv",iv).commit()){SharedPreferences.Editor rollback=prefs.edit().remove("credentials").remove("iv");if(previous!=null)rollback.putString("credentials",previous);if(previousIv!=null)rollback.putString("iv",previousIv);rollback.commit();throw new java.io.IOException("密码配置保存失败，请重试。");}}finally{java.util.Arrays.fill(data,(byte)0);}
 }
 Saved load()throws Exception {
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(false),new GCMParameterSpec(128,Base64.decode(prefs.getString("iv",""),Base64.NO_WRAP)));cipher.updateAAD(context);
  byte[] data=cipher.doFinal(Base64.decode(prefs.getString("credentials",""),Base64.NO_WRAP));
  try{JSONObject value=new JSONObject(new String(data,StandardCharsets.UTF_8));String account=value.getString("account"),password=value.getString("password");if(account.isEmpty()||password.isEmpty())throw new GeneralSecurityException("保存的配置不完整。");return new Saved(account,password);}finally{java.util.Arrays.fill(data,(byte)0);}
 }
}
