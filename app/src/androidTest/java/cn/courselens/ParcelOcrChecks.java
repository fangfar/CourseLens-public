package cn.courselens;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import java.util.*;

/** Host-side protocol boundary; real model checks run in the optional plugin APK. */
final class ParcelOcrChecks {
 static void run(Instrumentation runner,Bundle args){
  Bundle result=new Bundle();int status=Activity.RESULT_CANCELED;
  try{
   Context context=runner.getTargetContext();if(!context.getPackageName().endsWith(".qa"))throw new AssertionError("Requires QA package");
   List<ParcelRules.OcrLine> lines=ParcelOcr.lines("{\"protocol\":1,\"lines\":[{\"text\":\"合成快递取件码 6-2-2088\",\"left\":0,\"top\":0,\"right\":800,\"bottom\":80}]}");
   List<ParcelRules.Candidate> found=ParcelRules.extractImage(lines);if(found.stream().noneMatch(c->c.code.equals("6-2-2088"))||found.stream().anyMatch(ParcelRules.Candidate::autoSave))throw new AssertionError("Plugin result parsing or confirmation failed");
   result.putString("stream","PASS: host OCR protocol and confirmation boundary. Model and cross-app checks require the separate QA plugin.");status=Activity.RESULT_OK;
  }catch(Throwable e){result.putString("stream","FAIL: "+e.getClass().getSimpleName()+": "+e.getMessage());}
  runner.finish(status,result);
 }
}
