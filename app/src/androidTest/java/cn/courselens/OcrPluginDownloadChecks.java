package cn.courselens;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.widget.TextView;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises the real download/installer without opening or writing business data. */
final class OcrPluginDownloadChecks {
 static void run(Instrumentation runner){
  Bundle report=new Bundle();int result=Activity.RESULT_CANCELED;Activity page=null;
  try{
   if(!runner.getTargetContext().getPackageName().endsWith(".qa"))throw new AssertionError("Requires QA host");
   if(OcrPluginActivity.installed(runner.getTargetContext()))throw new AssertionError("QA plugin is already installed");
   page=runner.startActivitySync(new Intent(runner.getTargetContext(),OcrPluginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Activity screen=page;
   runner.runOnMainSync(()->screen.findViewById(android.R.id.content).findViewWithTag("ocr-plugin-action").performClick());
   long until=SystemClock.elapsedRealtime()+180000;String previous="";
   while(SystemClock.elapsedRealtime()<until){
    if(OcrPluginActivity.installed(runner.getTargetContext())){report.putString("stream","PASS: DownloadManager -> verified same-signed QA APK -> system installer -> installed plugin detected. No business database writes.");result=Activity.RESULT_OK;break;}
    AtomicReference<String> status=new AtomicReference<>("");runner.runOnMainSync(()->{TextView view=screen.findViewById(android.R.id.content).findViewWithTag("ocr-plugin-status");if(view!=null)status.set(view.getText().toString());});
    if(!status.get().equals(previous)){previous=status.get();Bundle update=new Bundle();update.putString("stream",previous);runner.sendStatus(0,update);}Thread.sleep(500);
   }
   if(result!=Activity.RESULT_OK)throw new AssertionError("Download/install timed out; inspect system permission or installer prompt");
  }catch(Throwable error){report.putString("stream","FAIL: "+error.getClass().getSimpleName()+": "+error.getMessage());}
  finally{if(page!=null){Activity screen=page;runner.runOnMainSync(screen::finish);}runner.finish(result,report);}
 }
}
