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
 @Test public void parcelSheetsKeepActionsVisible()throws Exception{
  for(String qualifiers:new String[]{"w380dp-h800dp-notnight","w320dp-h640dp-night"}){
   RuntimeEnvironment.setQualifiers(qualifiers);RuntimeEnvironment.setFontScale(qualifiers.contains("night")&&!qualifiers.contains("notnight")?2f:1f);
   try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
    ParcelActivity activity=controller.get();ParcelPage page=org.robolectric.util.ReflectionHelpers.getField(activity,"page");page.pause();layout(activity.findViewById(android.R.id.content),activity);
    ParcelRecord record=new ParcelRecord();record.code="6-2-2088";record.station="合成东门驿站";record.trackingNumber="CL12345678901";record.trackingEvents="[{\"time\":\"10-24 18:30\",\"description\":\"已到达合成转运中心\"}]";
    String[] methods={"showActions","importSheet","sources","proxySettings","actions","tracking","reminder"};
    for(String name:methods){
     boolean withRecord=Arrays.asList("actions","tracking","reminder").contains(name);java.lang.reflect.Method method=ParcelPage.class.getDeclaredMethod(name,withRecord?new Class<?>[]{ParcelRecord.class}:new Class<?>[0]);method.setAccessible(true);method.invoke(page,withRecord?new Object[]{record}:new Object[0]);
     AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();View decor=dialog.getWindow().getDecorView();layoutDialog(dialog,activity);View shell=decor.findViewWithTag("parcel-sheet");assertNotNull(shell);assertContained((ViewGroup)shell,findText(shell,"关闭"));
     if(name.equals("proxySettings")){View footer=decor.findViewWithTag("parcel-proxy-actions");assertContained((ViewGroup)shell,footer);((Switch)findText(shell,"后台查询（约每 6 小时）")).setChecked(true);findText(footer,"保存").performClick();assertTrue(dialog.isShowing());assertTrue(hasText(shell,"后台查询需要先配置代理地址"));layoutDialog(dialog,activity);assertContained((ViewGroup)shell,findText(shell,"后台查询需要先配置代理地址"));}
     snapshot(decor,(qualifiers.contains("notnight")?"normal":"large-dark")+"-"+name);dialog.dismiss();
    }
    for(int section=0;section<4;section++){java.lang.reflect.Method method=ParcelPage.class.getDeclaredMethod("settingsSection",int.class);method.setAccessible(true);method.invoke(page,section);AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();layoutDialog(dialog,activity);snapshot(dialog.getWindow().getDecorView(),"settings-"+section+"-"+qualifiers);dialog.dismiss();}
   }
  }
 }
 @Test public void repeatedPickupKeepsUndoableOriginalState()throws Exception{
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
   ParcelActivity activity=controller.get();ParcelPage page=org.robolectric.util.ReflectionHelpers.getField(activity,"page");page.pause();
   ParcelRecord record=new ParcelRecord();record.code="6-2-2088";record.station="合成防重复取件驿站";record.remindAt=System.currentTimeMillis()+3600000;
   long id=ParcelRepository.IO.submit(()->new ParcelRepository(activity).save(record)).get();
   java.lang.reflect.Method complete=ParcelPage.class.getDeclaredMethod("complete",long.class);complete.setAccessible(true);complete.invoke(page,id);complete.invoke(page,id);
   ParcelRepository.IO.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idle();
   assertEquals(ParcelRecord.DONE,(int)ParcelRepository.IO.submit(()->ParcelDatabase.get(activity).parcels().get(id).state).get());
   findText(page,"撤销").performClick();ParcelRepository.IO.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idle();
   ParcelRecord restored=ParcelRepository.IO.submit(()->ParcelDatabase.get(activity).parcels().get(id)).get();assertEquals(ParcelRecord.READY,restored.state);assertEquals(record.remindAt,restored.remindAt);
   ParcelRepository.IO.submit(()->new ParcelRepository(activity).delete(id)).get();
  }
 }
 private void checkPage(String qualifiers,float font,String image)throws Exception{
  RuntimeEnvironment.setQualifiers(qualifiers);RuntimeEnvironment.setFontScale(font);
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
   ParcelActivity activity=controller.get();java.lang.reflect.Field field=ParcelActivity.class.getDeclaredField("page");field.setAccessible(true);ParcelPage page=(ParcelPage)field.get(activity);page.pause();
   ParcelRepository.IO.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idle();
   ParcelRecord ready=new ParcelRecord();ready.id=101;ready.code="6-2-2088";ready.station="合成东门驿站";ready.carrier="中通快递";
   ParcelRecord pending=new ParcelRecord();pending.id=102;pending.code="8-3-1099";pending.station=ready.station;pending.confirmed=false;pending.source="合成通知";
   ParcelRecord transit=new ParcelRecord();transit.id=103;transit.state=ParcelRecord.TRANSIT;transit.trackingNumber="JD00000000001";transit.carrier="京东快递";transit.trackingEvents="[{\"time\":\"合成更新时间\",\"description\":\"已到达合成转运中心\"}]";
   ParcelRecord done=new ParcelRecord();done.id=104;done.state=ParcelRecord.DONE;done.code="2-5-1010";done.station="合成西门驿站";done.carrier="圆通快递";done.updatedAt=System.currentTimeMillis();
   java.lang.reflect.Method render=ParcelPage.class.getDeclaredMethod("render",List.class);render.setAccessible(true);render.invoke(page,Arrays.asList(ready,pending,transit,done));
   View root=activity.findViewById(android.R.id.content);layout(root,activity);
   assertEquals("待取件 2",((TextView)root.findViewWithTag("parcel-filter-0")).getText().toString());
   View pickup=root.findViewWithTag("parcel-record-101");assertNotNull(findText(pickup,"合成东门驿站"));assertNotNull(findText(pickup,"已取件"));assertFalse(hasText(pickup,"规则评分"));
   View confirmation=root.findViewWithTag("parcel-record-102");assertNotNull(findText(confirmation,"确认无误"));assertTrue(confirmation.getTop()<pickup.getTop());assertEquals(1,((TextView)findText(confirmation,"确认无误")).getLineCount());assertEquals(1,((TextView)findText(pickup,"已取件")).getLineCount());
   assertNotNull(findText(root,"身份码"));assertNotNull(findText(root,"已到达合成转运中心"));assertNotNull(findText(root,"去配置"));
   ViewGroup identity=root.findViewWithTag("parcel-identity");
   for(int i=0;i<3;i++){TextView entry=root.findViewWithTag("parcel-identity-"+i);assertEquals("identity labels should not wrap",1,entry.getLineCount());assertContained(identity,entry);assertTrue("identity touch target",entry.getHeight()>=Math.round(48*activity.getResources().getDisplayMetrics().density));assertTrue("identity text must not be vertically clipped",entry.getLayout().getHeight()+entry.getCompoundPaddingTop()+entry.getCompoundPaddingBottom()<=entry.getHeight());}
   android.graphics.drawable.Drawable addBackground=root.findViewWithTag("quick-menu-button").getBackground();
   assertTrue("add button should keep the shared light-circle icon style",addBackground instanceof android.graphics.drawable.GradientDrawable&&((android.graphics.drawable.GradientDrawable)addBackground).getColor().getDefaultColor()==activity.getColor(R.color.app_card));
   snapshot(root,image);
   root.findViewWithTag("parcel-filter-1").performClick();assertEquals(1,((ViewPager)root.findViewWithTag("parcel-filter-pager")).getCurrentItem());
   View shipping=root.findViewWithTag("parcel-record-103");assertNull(findText(shipping,"查询物流"));shipping.performClick();assertNotNull(findText(shipping,"收起物流记录"));shipping.performClick();assertNotNull(findText(shipping,"展开物流记录"));layout(root,activity);snapshot(root,image+"-transit");
   Bundle state=new Bundle();page.saveState(state);assertEquals(1,state.getInt("filter"));assertEquals(3,state.getIntArray("filterScrollY").length);
   root.findViewWithTag("parcel-filter-2").performClick();layout(root,activity);assertNotNull(root.findViewWithTag("parcel-record-104"));snapshot(root,image+"-done");root.findViewWithTag("quick-menu-button").performClick();AlertDialog add=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();add.getWindow().getDecorView().findViewWithTag("parcel-add").performClick();
   AlertDialog editor=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(editor.getWindow().getDecorView().findViewWithTag("parcel-editor-code"));assertFalse(hasSpinner(editor.getWindow().getDecorView()));
   layoutDialog(editor,activity);
   View footer=editor.getWindow().getDecorView().findViewWithTag("parcel-editor-actions");assertNotNull(footer);assertFalse(footer.getParent() instanceof ScrollView);assertTrue(footer.getHeight()>0);if(((LinearLayout)footer).getOrientation()==LinearLayout.HORIZONTAL)assertEquals("footer buttons must align",findText(footer,"取消").getTop(),findText(footer,"保存").getTop());
   Button submit=editor.getWindow().getDecorView().findViewWithTag("parcel-editor-save");submit.performClick();ParcelRepository.IO.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(editor.isShowing());assertTrue(submit.isEnabled());assertTrue(hasText(editor.getWindow().getDecorView(),"请填写取件码或运单号"));
   layoutDialog(editor,activity);View validation=findText(editor.getWindow().getDecorView(),"请填写取件码或运单号");assertTrue(validation.getHeight()>0);assertTrue(validation.getBottom()<=footer.getTop());snapshot(editor.getWindow().getDecorView(),image+"-editor");
   java.lang.reflect.Field tracking=ParcelPage.class.getDeclaredField("trackingField");tracking.setAccessible(true);((EditText)tracking.get(page)).setText("CL12345678901");editor.getWindow().getDecorView().findViewWithTag("parcel-editor-save").performClick();
   ParcelRepository.IO.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idle();
   ParcelRecord saved=ParcelRepository.IO.submit(()->ParcelDatabase.get(activity).parcels().all().stream().filter(p->p.trackingNumber.equals("CL12345678901")).findFirst().orElse(null)).get();assertNotNull(saved);assertEquals(ParcelRecord.TRANSIT,saved.state);assertEquals(1,((ViewPager)root.findViewWithTag("parcel-filter-pager")).getCurrentItem());
   ParcelRepository.IO.submit(()->new ParcelRepository(activity).delete(saved.id)).get();
  }
  ParcelRepository.IO.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idle();
 }
 private void layout(View root,android.app.Activity activity){android.util.DisplayMetrics dm=activity.getResources().getDisplayMetrics();root.measure(View.MeasureSpec.makeMeasureSpec(dm.widthPixels,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(dm.heightPixels,View.MeasureSpec.EXACTLY));root.layout(0,0,dm.widthPixels,dm.heightPixels);}
 private void layoutDialog(AlertDialog dialog,android.app.Activity activity){android.util.DisplayMetrics dm=activity.getResources().getDisplayMetrics();View root=dialog.getWindow().getDecorView();int height=dialog.getWindow().getAttributes().height;root.measure(View.MeasureSpec.makeMeasureSpec(dm.widthPixels,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height>0?height:dm.heightPixels,height>0?View.MeasureSpec.EXACTLY:View.MeasureSpec.AT_MOST));root.layout(0,0,root.getMeasuredWidth(),root.getMeasuredHeight());}
 private void assertContained(ViewGroup parent,View child){assertNotNull(child);android.graphics.Rect bounds=new android.graphics.Rect(0,0,child.getWidth(),child.getHeight());parent.offsetDescendantRectToMyCoords(child,bounds);assertTrue("child must have height",bounds.height()>0);assertTrue("child must fit parent: "+bounds+" in "+parent.getWidth()+"x"+parent.getHeight(),bounds.left>=0&&bounds.top>=0&&bounds.right<=parent.getWidth()&&bounds.bottom<=parent.getHeight());}
 private View findText(View view,String text){if(view instanceof TextView&&((TextView)view).getText().toString().equals(text))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=findText(((ViewGroup)view).getChildAt(i),text);if(found!=null)return found;}return null;}
 private boolean hasText(View view,String text){if(view instanceof TextView&&((TextView)view).getText().toString().contains(text))return true;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(hasText(((ViewGroup)view).getChildAt(i),text))return true;return false;}
 private boolean hasSpinner(View view){if(view instanceof Spinner)return true;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(hasSpinner(((ViewGroup)view).getChildAt(i)))return true;return false;}
 private void snapshot(View root,String name)throws Exception{String directory=System.getProperty("courselens.parcelPreviewDir");if(directory==null)return;java.io.File folder=new java.io.File(directory);folder.mkdirs();Bitmap bitmap=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(folder,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}
}
