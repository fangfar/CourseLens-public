package cn.courselens;

import java.io.IOException;
import java.util.*;
import org.json.*;

/** The host parses layout data only; the optional APK owns the OCR engine. */
final class ParcelOcr {
 interface Callback {void result(String value,String error);}
 static final int PROTOCOL=1;
 static List<ParcelRules.OcrLine> lines(String value)throws Exception {
  if(value==null||value.length()>128*1024)throw new IOException("识别结果过大");
  JSONTokener reader=new JSONTokener(value);JSONObject result=new JSONObject(reader);
  Object protocol=result.get("protocol");if(reader.nextClean()!=0||!(protocol instanceof Integer)||((Integer)protocol)!=PROTOCOL)throw new IOException("识别插件版本不兼容");
  JSONArray rows=result.getJSONArray("lines");if(rows.length()>1000)throw new IOException("识别结果过多");
  List<ParcelRules.OcrLine> lines=new ArrayList<>();int length=0;
  for(int i=0;i<rows.length();i++){
   JSONObject row=rows.getJSONObject(i);Object raw=row.get("text");if(!(raw instanceof String))throw new IOException("识别结果无效");String text=(String)raw;
   length+=text.length();if(length>ParcelRules.MAX_TEXT)throw new IOException("识别文字过长");
   int left=coordinate(row,"left"),top=coordinate(row,"top"),right=coordinate(row,"right"),bottom=coordinate(row,"bottom");
   if(right<left||bottom<top)throw new IOException("识别位置无效");lines.add(new ParcelRules.OcrLine(text,left,top,right,bottom));
  }
  return lines;
 }
 private static int coordinate(JSONObject value,String key)throws Exception {
  Object raw=value.get(key);if(!(raw instanceof Integer)||((Integer)raw)<0||((Integer)raw)>2400)throw new IOException("识别位置无效");return (Integer)raw;
 }
 static String text(List<ParcelRules.OcrLine> lines){StringBuilder value=new StringBuilder();for(ParcelRules.OcrLine line:lines){if(value.length()>0)value.append('\n');value.append(line.text);}return value.toString();}
}
