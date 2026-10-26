package cn.courselens.ocr;

import android.app.*;
import android.graphics.*;
import android.os.Bundle;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.json.*;

public final class OcrChecks extends Instrumentation {
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){
  Bundle result=new Bundle();int status=Activity.RESULT_CANCELED;
  try{
   if(!getTargetContext().getPackageName().endsWith(".qa"))throw new AssertionError("Requires QA plugin");
   Bitmap bitmap=Bitmap.createBitmap(900,600,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(Color.WHITE);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.setTextSize(44);canvas.drawText("合成北门快递站",60,200,paint);canvas.drawText("取件码 6-2-2088",60,320,paint);
   CountDownLatch done=new CountDownLatch(1);AtomicReference<String> value=new AtomicReference<>(),error=new AtomicReference<>();
   OcrEngine.recognize(bitmap,(text,failure)->{value.set(text);error.set(failure);done.countDown();});
   if(!done.await(45,TimeUnit.SECONDS))throw new AssertionError("OCR timeout");if(error.get()!=null)throw new AssertionError(error.get());
   JSONObject payload=new JSONObject(value.get());if(payload.getInt("protocol")!=1||!value.get().contains("6-2-2088"))throw new AssertionError("Synthetic pickup code not recognized");
   result.putString("stream","PASS: optional bundled Chinese OCR engine -> bounded text/coordinates. Synthetic image only; no database writes.");status=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("stream","FAIL: "+e.getClass().getSimpleName()+": "+e.getMessage());}
  finish(status,result);
 }
}
