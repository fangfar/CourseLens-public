package cn.courselens;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import java.io.*;
import java.util.*;

final class ParcelOcr {
 interface Callback {void result(String text,List<ParcelRules.Candidate> candidates,String error);}
 static void read(Context context,Uri uri,Callback callback){
  ParcelRepository.IO.execute(()->{
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
   }catch(IOException|RuntimeException e){if(bitmap!=null)bitmap.recycle();callback.result(null,Collections.emptyList(),"无法读取图片（限 10 MB），请重新选择截图");}
  });
 }
 static void recognize(Bitmap bitmap,Callback callback){
  com.google.mlkit.vision.text.TextRecognizer recognizer=TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
  try{recognizer.process(InputImage.fromBitmap(bitmap,0)).addOnSuccessListener(text->{
   List<ParcelRules.OcrLine> lines=new ArrayList<>();for(var block:text.getTextBlocks())for(var line:block.getLines()){Rect box=line.getBoundingBox();if(box!=null)lines.add(new ParcelRules.OcrLine(line.getText(),box.left,box.top,box.right,box.bottom));}
   callback.result(text.getText(),ParcelRules.extractImage(lines),null);
  }).addOnFailureListener(error->callback.result(null,Collections.emptyList(),"图片识别失败，请重试或手动添加")).addOnCompleteListener(task->{recognizer.close();bitmap.recycle();});}
  catch(RuntimeException e){recognizer.close();bitmap.recycle();callback.result(null,Collections.emptyList(),"图片识别失败，请重试或手动添加");}
 }

}
