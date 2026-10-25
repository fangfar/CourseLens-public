package cn.courselens;

import android.os.Build;
import android.view.Window;
import java.util.function.Consumer;

/** Keep newer callback types behind API boundaries, including on API 26-32. */
final class MotionBack {
 static Object register(Window window,int priority,Runnable start,Consumer<Float> progress,Runnable cancel,Runnable commit){
  if(Build.VERSION.SDK_INT>=34)return Api34.register(window,priority,start,progress,cancel,commit);
  if(Build.VERSION.SDK_INT>=33)return Api33.register(window,priority,commit);return null;
 }
 static void unregister(Window window,Object callback){if(Build.VERSION.SDK_INT>=33&&callback!=null)Api33.unregister(window,callback);}
 @androidx.annotation.RequiresApi(33) private static class Api33 {
  static Object register(Window window,int priority,Runnable action){android.window.OnBackInvokedCallback callback=action::run;window.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(priority,callback);return callback;}
  static void unregister(Window window,Object callback){window.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback((android.window.OnBackInvokedCallback)callback);}
 }
 @androidx.annotation.RequiresApi(34) private static class Api34 {
  static Object register(Window window,int priority,Runnable start,Consumer<Float> progress,Runnable cancel,Runnable commit){android.window.OnBackAnimationCallback callback=new android.window.OnBackAnimationCallback(){public void onBackStarted(android.window.BackEvent event){start.run();}public void onBackProgressed(android.window.BackEvent event){progress.accept(event.getProgress());}public void onBackCancelled(){cancel.run();}public void onBackInvoked(){commit.run();}};window.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(priority,callback);return callback;}
 }
}
