package cn.courselens;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import java.util.*;
import java.io.*;
import android.graphics.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Host-side protocol boundary; real model checks run in the optional plugin APK. */
final class ParcelOcrChecks {
 static void run(Instrumentation runner,Bundle args){
  Bundle result=new Bundle();int status=Activity.RESULT_CANCELED;
  File fixture=null;
  try{
   Context context=runner.getTargetContext();if(!context.getPackageName().endsWith(".qa"))throw new AssertionError("Requires QA package");
   List<ParcelRules.OcrLine> lines=ParcelOcr.lines("{\"protocol\":1,\"lines\":[{\"text\":\"合成快递取件码 6-2-2088\",\"left\":0,\"top\":0,\"right\":800,\"bottom\":80}]}");
   List<ParcelRules.Candidate> found=ParcelRules.extractImage(lines);if(found.stream().noneMatch(c->c.code.equals("6-2-2088"))||found.stream().anyMatch(ParcelRules.Candidate::autoSave))throw new AssertionError("Plugin result parsing or confirmation failed");
   if(LocalOcr.AVAILABLE){
    File folder=new File(context.getFilesDir(),"ocr");if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException();fixture=File.createTempFile("qa-image-",".png",folder);
    Bitmap bitmap=Bitmap.createBitmap(900,600,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(Color.WHITE);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.setTextSize(44);canvas.drawText("合成北门快递站",60,200,paint);canvas.drawText("取件码 6-2-2088",60,320,paint);
    try(OutputStream out=new FileOutputStream(fixture)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}finally{bitmap.recycle();}
    CountDownLatch done=new CountDownLatch(1);AtomicReference<String> payload=new AtomicReference<>(),failure=new AtomicReference<>();
    LocalOcr.read(context,androidx.core.content.FileProvider.getUriForFile(context,context.getPackageName()+".ocr.files",fixture),(value,error)->{payload.set(value);failure.set(error);done.countDown();});
    if(!done.await(45,TimeUnit.SECONDS)||failure.get()!=null)throw new AssertionError("Bundled OCR failed");
    if(ParcelRules.extractImage(ParcelOcr.lines(payload.get())).stream().noneMatch(c->c.code.equals("6-2-2088")))throw new AssertionError("Bundled OCR did not extract synthetic code");
   }
   result.putString("stream",LocalOcr.AVAILABLE?"PASS: full edition bundled OCR -> synthetic image -> pickup extraction; no plugin required, no database writes.":"PASS: lite host OCR protocol and confirmation boundary. Model and cross-app checks require the separate QA plugin.");status=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("stream","FAIL: "+e.getClass().getSimpleName()+": "+e.getMessage());}
  finally{if(fixture!=null)fixture.delete();}
  runner.finish(status,result);
 }
}
