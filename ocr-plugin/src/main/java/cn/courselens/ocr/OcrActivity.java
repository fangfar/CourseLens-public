package cn.courselens.ocr;

import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Bundle;

/** Only the matching, same-signed host may supply a temporary image grant. */
public final class OcrActivity extends Activity {
 private final android.os.Handler timer=new android.os.Handler(android.os.Looper.getMainLooper());
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  String host=getPackageName().endsWith(".qa")?"cn.courselens.qa":"cn.courselens";
  if(!host.equals(getCallingPackage())||getPackageManager().checkSignatures(host,getPackageName())!=PackageManager.SIGNATURE_MATCH){finish();return;}
  if(getIntent().getIntExtra("protocol",0)!=1){complete(null,"识别插件版本不兼容");return;}
  timer.postDelayed(()->complete(null,"图片识别超时，请重试或手动添加"),45000);
  OcrEngine.read(getApplicationContext(),getIntent().getData(),(value,error)->runOnUiThread(()->complete(value,error)));
 }
 private void complete(String value,String error){if(isFinishing()||isDestroyed())return;Intent result=new Intent();if(error!=null)result.putExtra("error",error);else result.putExtra("ocrResult",value);setResult(error==null?RESULT_OK:RESULT_CANCELED,result);finish();}
 @Override protected void onDestroy(){timer.removeCallbacksAndMessages(null);super.onDestroy();}
}
