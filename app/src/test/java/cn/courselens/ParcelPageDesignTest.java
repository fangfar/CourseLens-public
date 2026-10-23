package cn.courselens;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.viewpager.widget.ViewPager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import java.util.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ParcelPageDesignTest {
 @Test public void groupedPickupAndEditor()throws Exception{checkPage("w380dp-h800dp-notnight",1f,"pickup");}
 @Test public void narrowMediumText()throws Exception{checkPage("w320dp-h640dp-notnight",1.25f,"narrow-medium");}
 @Test public void narrowDarkLargeText()throws Exception{checkPage("w320dp-h640dp-night",2f,"dark-large");}
 @Test public void landscapeLayout()throws Exception{checkPage("w800dp-h380dp-land-notnight",1f,"landscape");}
 private void checkPage(String qualifiers,float font,String image)throws Exception{
  RuntimeEnvironment.setQualifiers(qualifiers);RuntimeEnvironment.setFontScale(font);
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
   ParcelActivity activity=controller.get();java.lang.reflect.Field field=ParcelActivity.class.getDeclaredField("page");field.setAccessible(true);ParcelPage page=(ParcelPage)field.get(activity);page.pause();
   ParcelRepository.IO.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idle();
   ParcelRecord ready=new ParcelRecord();ready.id=101;ready.code="6-2-2088";ready.station="合成东门驿站";ready.carrier="中通快递";
   ParcelRecord pending=new ParcelRecord();pending.id=102;pending.code="8-3-1099";pending.station=ready.station;pending.confirmed=false;pending.source="合成通知";
   ParcelRecord transit=new ParcelRecord();transit.id=103;transit.state=ParcelRecord.TRANSIT;transit.trackingNumber="JD00000000001";transit.carrier="京东快递";transit.trackingEvents="[{\"time\":\"合成更新时间\",\"description\":\"已到达合成转运中心\"}]";
   java.lang.reflect.Method render=ParcelPage.class.getDeclaredMethod("render",List.class);render.setAccessible(true);render.invoke(page,Arrays.asList(ready,pending,transit));
   View root=activity.findViewById(android.R.id.content);layout(root,activity);
   assertEquals("待取件 2",((TextView)root.findViewWithTag("parcel-filter-0")).getText().toString());
   View pickup=root.findViewWithTag("parcel-record-101");assertNotNull(findText(pickup,"合成东门驿站"));assertNotNull(findText(pickup,"已取件"));assertFalse(hasText(pickup,"规则评分"));
   View confirmation=root.findViewWithTag("parcel-record-102");assertNotNull(findText(confirmation,"确认无误"));assertTrue(confirmation.getTop()<pickup.getTop());
   assertNotNull(findText(root,"身份码"));assertNotNull(findText(root,"已到达合成转运中心"));assertNotNull(findText(root,"去配置"));
   for(int i=0;i<3;i++)assertEquals("identity labels should not wrap",1,((TextView)root.findViewWithTag("parcel-identity-"+i)).getLineCount());
   snapshot(root,image);
   root.findViewWithTag("parcel-filter-1").performClick();assertEquals(1,((ViewPager)root.findViewWithTag("parcel-filter-pager")).getCurrentItem());
   View shipping=root.findViewWithTag("parcel-record-103");shipping.performClick();assertNotNull(findText(shipping,"收起物流记录"));shipping.performClick();assertNotNull(findText(shipping,"展开物流记录"));
   Bundle state=new Bundle();page.saveState(state);assertEquals(1,state.getInt("filter"));assertEquals(3,state.getIntArray("filterScrollY").length);
   root.findViewWithTag("quick-menu-button").performClick();AlertDialog add=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();add.getWindow().getDecorView().findViewWithTag("parcel-add").performClick();
   AlertDialog editor=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(editor.getWindow().getDecorView().findViewWithTag("parcel-editor-code"));assertFalse(hasSpinner(editor.getWindow().getDecorView()));
   layout(editor.getWindow().getDecorView(),activity);
   java.lang.reflect.Field tracking=ParcelPage.class.getDeclaredField("trackingField");tracking.setAccessible(true);((EditText)tracking.get(page)).setText("CL12345678901");editor.getWindow().getDecorView().findViewWithTag("parcel-editor-save").performClick();
   ParcelRepository.IO.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idle();
   ParcelRecord saved=ParcelRepository.IO.submit(()->ParcelDatabase.get(activity).parcels().all().stream().filter(p->p.trackingNumber.equals("CL12345678901")).findFirst().orElse(null)).get();assertNotNull(saved);assertEquals(ParcelRecord.TRANSIT,saved.state);
   ParcelRepository.IO.submit(()->new ParcelRepository(activity).delete(saved.id)).get();
  }
  ParcelRepository.IO.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idle();
 }
 private void layout(View root,android.app.Activity activity){android.util.DisplayMetrics dm=activity.getResources().getDisplayMetrics();root.measure(View.MeasureSpec.makeMeasureSpec(dm.widthPixels,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(dm.heightPixels,View.MeasureSpec.EXACTLY));root.layout(0,0,dm.widthPixels,dm.heightPixels);}
 private View findText(View view,String text){if(view instanceof TextView&&((TextView)view).getText().toString().equals(text))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=findText(((ViewGroup)view).getChildAt(i),text);if(found!=null)return found;}return null;}
 private boolean hasText(View view,String text){if(view instanceof TextView&&((TextView)view).getText().toString().contains(text))return true;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(hasText(((ViewGroup)view).getChildAt(i),text))return true;return false;}
 private boolean hasSpinner(View view){if(view instanceof Spinner)return true;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(hasSpinner(((ViewGroup)view).getChildAt(i)))return true;return false;}
 private void snapshot(View root,String name)throws Exception{String directory=System.getProperty("courselens.parcelPreviewDir");if(directory==null)return;java.io.File folder=new java.io.File(directory);folder.mkdirs();Bitmap bitmap=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(folder,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}
}
