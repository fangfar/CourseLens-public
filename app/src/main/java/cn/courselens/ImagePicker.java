package cn.courselens;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.provider.MediaStore;

final class ImagePicker {
 static Intent intent(int sdk){
  if(sdk>=33)return new Intent(MediaStore.ACTION_PICK_IMAGES).setType("image/*");
  return new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
 }
 static void open(Activity activity,int request){
  if(Build.VERSION.SDK_INT>=33){
   try{activity.startActivityForResult(intent(33),request);return;}catch(ActivityNotFoundException ignored){}
  }
  activity.startActivityForResult(intent(26),request);
 }
}
