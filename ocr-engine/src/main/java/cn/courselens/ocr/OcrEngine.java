package cn.courselens.ocr;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.*;

public final class OcrEngine {
 public interface Callback {void result(String value,String error);}
 private static final ExecutorService IO=Executors.newSingleThreadExecutor();
 public static void read(Context context,Uri uri,Callback callback){
  IO.execute(()->{
   Bitmap bitmap=null;
   try{
    if(uri==null||!"content".equals(uri.getScheme()))throw new IOException();
    byte[] bytes;
    try(InputStream in=context.getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){
     if(in==null)throw new IOException();byte[] buffer=new byte[16384];int count;
     while((count=in.read(buffer))!=-1){if(out.size()+count>10*1024*1024)throw new IOException();out.write(buffer,0,count);}bytes=out.toByteArray();
    }
    BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
    if(options.outWidth<=0||options.outHeight<=0||(long)options.outWidth*options.outHeight>80000000)throw new IOException();
    options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>2400)options.inSampleSize*=2;
    options.inJustDecodeBounds=false;bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(bitmap==null)throw new IOException();
    recognize(bitmap,callback);
   }catch(IOException|RuntimeException e){if(bitmap!=null&&!bitmap.isRecycled())bitmap.recycle();callback.result(null,"无法读取图片（限 10 MB），请重新选择图片");}
  });
 }
 public static void recognize(Bitmap bitmap,Callback callback){
  com.google.mlkit.vision.text.TextRecognizer recognizer=null;
  try{
   recognizer=TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
   com.google.mlkit.vision.text.TextRecognizer engine=recognizer;
   engine.process(InputImage.fromBitmap(bitmap,0)).addOnSuccessListener(text->{
    try{
     JSONArray lines=new JSONArray();int length=0;
     for(var block:text.getTextBlocks())for(var line:block.getLines()){
      Rect box=line.getBoundingBox();if(box==null)continue;length+=line.getText().length();if(length>12000||lines.length()>=1000)throw new IOException();
      lines.put(new JSONObject().put("text",line.getText()).put("left",Math.max(0,box.left)).put("top",Math.max(0,box.top)).put("right",Math.min(bitmap.getWidth(),box.right)).put("bottom",Math.min(bitmap.getHeight(),box.bottom)));
     }
     String value=new JSONObject().put("protocol",1).put("lines",lines).toString();if(value.length()>128*1024)throw new IOException();callback.result(value,null);
    }catch(Exception e){callback.result(null,"图片文字过多，请裁剪后重试");}
   }).addOnFailureListener(error->callback.result(null,"图片识别失败，请重试或手动添加")).addOnCompleteListener(task->{engine.close();bitmap.recycle();});
  }catch(RuntimeException e){if(recognizer!=null)recognizer.close();if(!bitmap.isRecycled())bitmap.recycle();callback.result(null,"图片识别失败，请重试或手动添加");}
 }
}
