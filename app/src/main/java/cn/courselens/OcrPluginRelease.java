package cn.courselens;

import java.io.IOException;
import org.json.*;

final class OcrPluginRelease {
 static final long MAX_BYTES=80L*1024*1024;
 final String url,sha256;final long bytes,versionCode;
 private OcrPluginRelease(JSONObject value)throws Exception {
  if(integer(value,"schemaVersion")!=1||integer(value,"protocol")!=ParcelOcr.PROTOCOL)throw new IOException("插件版本不兼容");
  if(!Boolean.TRUE.equals(value.get("enabled")))throw new IOException("图片识别插件尚未开放下载，请稍后重试");
  if(!"cn.courselens.ocr".equals(value.get("packageName")))throw new IOException("插件包名无效");
  Object link=value.get("url"),hash=value.get("sha256");
  if(!(link instanceof String)||((String)link).length()>2048||!RemoteConfig.validLink((String)link)||!(hash instanceof String)||!((String)hash).matches("[0-9a-f]{64}"))throw new IOException("插件下载配置无效");
  url=(String)link;sha256=(String)hash;bytes=integer(value,"bytes");versionCode=integer(value,"versionCode");
  if(bytes<1||bytes>MAX_BYTES||versionCode<1)throw new IOException("插件大小或版本无效");
 }
 static OcrPluginRelease parse(String text)throws Exception {
  if(text==null||text.length()>16384)throw new IOException("插件下载配置过大");
  JSONTokener reader=new JSONTokener(text);JSONObject value=new JSONObject(reader);if(reader.nextClean()!=0)throw new IOException("插件下载配置无效");return new OcrPluginRelease(value);
 }
 private static long integer(JSONObject value,String key)throws Exception {Object raw=value.get(key);if(!(raw instanceof Integer)&&!(raw instanceof Long))throw new IOException("插件下载配置无效");return ((Number)raw).longValue();}
}
