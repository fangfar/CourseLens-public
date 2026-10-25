package cn.courselens;

import android.app.*;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Runs the bundled OCR model; an optional private fixture remains outside the repository. */
final class ParcelOcrChecks {
 static void run(Instrumentation runner,Bundle args){
  Bundle result=new Bundle();int status=Activity.RESULT_CANCELED;Bitmap bitmap=null;
  try{
   Context context=runner.getTargetContext();if(!context.getPackageName().endsWith(".qa"))throw new AssertionError("Requires QA package");
   String name=args.getString("parcelOcrImage","");boolean privateFixture=!name.isEmpty();
   if(privateFixture){File file=new File(context.getFilesDir(),name);if(!file.getCanonicalFile().getParentFile().equals(context.getFilesDir().getCanonicalFile()))throw new AssertionError("Fixture must be directly inside QA files");bitmap=BitmapFactory.decodeFile(file.getPath());}
   else{bitmap=Bitmap.createBitmap(900,1000,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(Color.WHITE);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.setTypeface(Typeface.DEFAULT);paint.setTextSize(36);canvas.drawText("合成北门京东快递站",60,350,paint);canvas.drawText("取件码",60,480,paint);paint.setTextSize(64);canvas.drawText("6-2-2088",230,480,paint);paint.setTextSize(30);canvas.drawText("复制",650,480,paint);canvas.drawText("菜鸟身份码",60,100,paint);canvas.drawText("运单号 YT00000000001",60,250,paint);}
   if(bitmap==null)throw new AssertionError("Fixture cannot be decoded");
   CountDownLatch completed=new CountDownLatch(1);AtomicReference<List<ParcelRules.Candidate>> candidates=new AtomicReference<>();AtomicReference<String> error=new AtomicReference<>();
   ParcelOcr.recognize(bitmap,(text,found,failure)->{candidates.set(found);error.set(failure);completed.countDown();});bitmap=null;
   if(!completed.await(45,TimeUnit.SECONDS))throw new AssertionError("OCR callback timed out");if(error.get()!=null)throw new AssertionError(error.get());
   List<ParcelRules.Candidate> found=candidates.get();String expected=args.getString("parcelOcrExpectedCode",privateFixture?"":"6-2-2088");
   if(found==null||found.isEmpty())throw new AssertionError("No pickup information extracted from image");
   if(!expected.isEmpty()&&found.stream().noneMatch(c->c.code.equals(expected)))throw new AssertionError("Expected pickup code was not extracted");
   if(found.stream().anyMatch(ParcelRules.Candidate::autoSave))throw new AssertionError("Screenshot must require confirmation");
   if(found.stream().noneMatch(c->!c.station.isEmpty()))throw new AssertionError("Pickup station was not extracted");
   if(!privateFixture&&!found.get(0).tracking.equals("YT00000000001"))throw new AssertionError("Waybill field was not extracted");
   result.putString("stream","PASS: bundled Chinese ML Kit -> pickup code and station extraction; confirmation required; "+(privateFixture?"private screenshot":"synthetic screenshot")+". No raw image/text in output, no parcel database writes.");status=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("stream","FAIL: "+e.getClass().getSimpleName()+": "+e.getMessage());}
  finally{if(bitmap!=null&&!bitmap.isRecycled())bitmap.recycle();runner.finish(status,result);}
 }
}
