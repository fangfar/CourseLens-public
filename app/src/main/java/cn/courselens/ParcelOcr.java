package cn.courselens;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import java.io.*;

final class ParcelOcr {
 interface Callback {void result(String text,String error);}
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
    Bitmap owned=bitmap;var recognizer=TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
    recognizer.process(InputImage.fromBitmap(owned,0)).addOnSuccessListener(text->callback.result(text.getText(),null)).addOnFailureListener(error->callback.result(null,"图片识别失败，请重试或手动添加")).addOnCompleteListener(task->{recognizer.close();owned.recycle();});
   }catch(IOException|RuntimeException e){if(bitmap!=null)bitmap.recycle();callback.result(null,"无法读取图片（限 10 MB），请重新选择截图");}
  });
 }
}
