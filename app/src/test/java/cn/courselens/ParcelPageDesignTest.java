package cn.courselens;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
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
 @Test public void notificationSettingsShowSmsAndAvoidRepeatedAuthorization()throws Exception{
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
   ParcelActivity activity=controller.get();ParcelPage page=org.robolectric.util.ReflectionHelpers.getField(activity,"page");page.pause();
   android.provider.Settings.Secure.putString(activity.getContentResolver(),"enabled_notification_listeners",new android.content.ComponentName(activity,ParcelNotificationListener.class).flattenToString());
   java.lang.reflect.Method settings=ParcelPage.class.getDeclaredMethod("settingsSection",int.class);settings.setAccessible(true);settings.invoke(page,0);
   AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNull(findText(dialog.getWindow().getDecorView(),"选择通知来源"));Switch toggle=(Switch)findText(dialog.getWindow().getDecorView(),"后台通知识别");toggle.setChecked(true);
   AlertDialog consent=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();consent.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertNull(shadowOf(activity).getNextStartedActivity());
  }
 }
 @Test public void systemSmsFallbackIsAcceptedWithoutSelection()throws Exception{
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
   ParcelActivity activity=controller.get();ParcelPage page=org.robolectric.util.ReflectionHelpers.getField(activity,"page");page.pause();
   android.content.pm.PackageInfo sms=new android.content.pm.PackageInfo();sms.packageName="com.android.mms";shadowOf(activity.getPackageManager()).installPackage(sms);
   ParcelNotificationListener.settings(activity).edit().putStringSet("sources",Collections.emptySet()).commit();assertTrue(ParcelNotificationListener.accepts(activity,"com.android.mms"));for(String pkg:ParcelNotificationListener.PACKAGES)assertTrue(ParcelNotificationListener.accepts(activity,pkg));assertFalse(ParcelNotificationListener.accepts(activity,"other.app"));
  }
 }
 @Test public void sharePickupCodeUsesSystemChooser()throws Exception{
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
   ParcelActivity activity=controller.get();ParcelPage page=org.robolectric.util.ReflectionHelpers.getField(activity,"page");page.pause();drain();
   ParcelRecord record=new ParcelRecord();record.id=101;record.code="6-2-2088";record.station="合成驿站";record.trackingNumber="CL12345678901";
   java.lang.reflect.Method render=ParcelPage.class.getDeclaredMethod("render",List.class);render.setAccessible(true);render.invoke(page,Collections.singletonList(record));
   page.findViewWithTag("parcel-content-101").performClick();page.findViewWithTag("parcel-share-101").performClick();
   android.content.Intent chooser=shadowOf(activity).getNextStartedActivity();assertEquals(android.content.Intent.ACTION_CHOOSER,chooser.getAction());android.content.Intent send=chooser.getParcelableExtra(android.content.Intent.EXTRA_INTENT);assertEquals(android.content.Intent.ACTION_SEND,send.getAction());assertEquals("text/plain",send.getType());assertEquals(record.code,send.getStringExtra(android.content.Intent.EXTRA_TEXT));
  }
 }
 @Test public void reminderPickerKeepsInvalidTimeOpenAndAllowsCancel()throws Exception{
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
   ParcelActivity activity=controller.get();ParcelPage page=org.robolectric.util.ReflectionHelpers.getField(activity,"page");page.pause();ParcelRecord record=new ParcelRecord();record.id=101;record.remindAt=System.currentTimeMillis()+86400000L;
   java.lang.reflect.Method pick=ParcelPage.class.getDeclaredMethod("pickReminder",ParcelRecord.class);pick.setAccessible(true);pick.invoke(page,record);AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();View decor=dialog.getWindow().getDecorView();android.widget.DatePicker date=decor.findViewWithTag("parcel-reminder-date");android.widget.TimePicker time=decor.findViewWithTag("parcel-reminder-time");assertNotNull(date);assertNotNull(time);layoutDialog(dialog,activity);snapshot(decor,"reminder-picker");date.updateDate(2000,0,1);dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertTrue(dialog.isShowing());assertEquals("请选择未来的提醒时间",((android.widget.TextView)decor.findViewWithTag("parcel-reminder-error")).getText().toString());dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1));assertFalse(dialog.isShowing());
  }
 }
 @Test public void singleListAndEditor()throws Exception{checkPage("w380dp-h800dp-notnight",1f,"pickup");}
 @Test public void narrowMediumText()throws Exception{checkPage("w320dp-h640dp-notnight",1.25f,"narrow-medium");}
 @Test public void narrowDarkLargeText()throws Exception{checkPage("w320dp-h640dp-night",2f,"dark-large");}
 @Test public void landscapeLayout()throws Exception{checkPage("w800dp-h380dp-land-notnight",1f,"landscape");}
 @Test public void parcelSheetsKeepActionsVisible()throws Exception{
  for(String qualifiers:new String[]{"w380dp-h800dp-notnight","w320dp-h640dp-night"}){
   RuntimeEnvironment.setQualifiers(qualifiers);RuntimeEnvironment.setFontScale(qualifiers.contains("night")&&!qualifiers.contains("notnight")?2f:1f);
   try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
    ParcelActivity activity=controller.get();ParcelPage page=org.robolectric.util.ReflectionHelpers.getField(activity,"page");page.pause();layout(activity.findViewById(android.R.id.content),activity);
    ParcelRecord record=new ParcelRecord();record.code="6-2-2088";record.station="合成东门驿站";record.trackingNumber="CL12345678901";record.trackingEvents="[{\"time\":\"10-24 18:30\",\"description\":\"已到达合成转运中心\"}]";
    String[] methods={"importSheet","actions","tracking","reminder"};
    for(String name:methods){
     boolean withRecord=Arrays.asList("actions","tracking","reminder").contains(name);java.lang.reflect.Method method=ParcelPage.class.getDeclaredMethod(name,withRecord?new Class<?>[]{ParcelRecord.class}:new Class<?>[0]);method.setAccessible(true);method.invoke(page,withRecord?new Object[]{record}:new Object[0]);
     AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();View decor=dialog.getWindow().getDecorView();layoutDialog(dialog,activity);View shell=decor.findViewWithTag("parcel-sheet");assertNotNull(shell);assertContained((ViewGroup)shell,findText(shell,"关闭"));

     assertFalse(hasText(shell,"物流代理"));assertFalse(hasText(shell,"查询物流"));
     snapshot(decor,(qualifiers.contains("notnight")?"normal":"large-dark")+"-"+name);dialog.dismiss();
    }
    for(int section=0;section<3;section++){java.lang.reflect.Method method=ParcelPage.class.getDeclaredMethod("settingsSection",int.class);method.setAccessible(true);method.invoke(page,section);AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();layoutDialog(dialog,activity);snapshot(dialog.getWindow().getDecorView(),"settings-"+section+"-"+qualifiers);dialog.dismiss();}
   }
  }
 }
 @Test public void manualEditorWrapsContentInsteadOfFillingScreen()throws Exception{
  RuntimeEnvironment.setQualifiers("w380dp-h900dp-notnight");RuntimeEnvironment.setFontScale(1f);
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
   ParcelActivity activity=controller.get();ParcelPage page=org.robolectric.util.ReflectionHelpers.getField(activity,"page");page.pause();layout(activity.findViewById(android.R.id.content),activity);
   java.lang.reflect.Method edit=ParcelPage.class.getDeclaredMethod("edit",ParcelRecord.class);edit.setAccessible(true);edit.invoke(page,new ParcelRecord());
   AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();layoutDialog(dialog,activity);View shell=dialog.getWindow().getDecorView().findViewWithTag("parcel-sheet");
   assertTrue("Manual entry should wrap its fields, not fill the screen: "+shell.getHeight(),shell.getHeight()<activity.getResources().getDisplayMetrics().heightPixels*.85f);
   assertContained((ViewGroup)shell,shell.findViewWithTag("parcel-editor-save"));
  }
 }
 @Test public void repeatedPickupKeepsUndoableOriginalState()throws Exception{checkPickupUndo(ParcelRecord.READY);}
 @Test public void manualEditorKeepsActionsAboveKeyboardAtLargeFont()throws Exception{
  RuntimeEnvironment.setQualifiers("w320dp-h640dp-night");RuntimeEnvironment.setFontScale(2f);
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
   ParcelActivity activity=controller.get();ParcelPage page=org.robolectric.util.ReflectionHelpers.getField(activity,"page");page.pause();layout(activity.findViewById(android.R.id.content),activity);
   java.lang.reflect.Method edit=ParcelPage.class.getDeclaredMethod("edit",ParcelRecord.class);edit.setAccessible(true);edit.invoke(page,new ParcelRecord());MotionSheetDialog dialog=(MotionSheetDialog)org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
   int ime=Math.round(240*activity.getResources().getDisplayMetrics().density);androidx.core.view.ViewCompat.dispatchApplyWindowInsets(dialog.overlay,new androidx.core.view.WindowInsetsCompat.Builder().setInsets(androidx.core.view.WindowInsetsCompat.Type.ime(),androidx.core.graphics.Insets.of(0,0,0,ime)).setVisible(androidx.core.view.WindowInsetsCompat.Type.ime(),true).build());layoutDialog(dialog,activity);
   ViewGroup shell=dialog.getWindow().getDecorView().findViewWithTag("parcel-sheet");assertContained(shell,shell.findViewWithTag("parcel-editor-save"));assertContained(shell,shell.findViewWithTag("parcel-editor-actions"));assertTrue(shell.getHeight()<=activity.getResources().getDisplayMetrics().heightPixels-ime);
  }
 }
 @Test public void trackingOnlyPickupCanCompleteAndUndo()throws Exception{checkPickupUndo(ParcelRecord.TRANSIT);}
 private void checkPickupUndo(int initial)throws Exception{
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
   ParcelActivity activity=controller.get();ParcelPage page=org.robolectric.util.ReflectionHelpers.getField(activity,"page");page.pause();
   ParcelRecord record=new ParcelRecord();record.state=initial;record.code=initial==ParcelRecord.READY?"6-2-2088":"";record.trackingNumber="CL00000000002";record.station="合成防重复取件驿站";record.remindAt=System.currentTimeMillis()+3600000;
   long id=ParcelRepository.IO.submit(()->new ParcelRepository(activity).save(record)).get();
   java.lang.reflect.Method complete=ParcelPage.class.getDeclaredMethod("complete",long.class);complete.setAccessible(true);complete.invoke(page,id);complete.invoke(page,id);
   ParcelRepository.IO.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idle();
   assertEquals(ParcelRecord.DONE,(int)ParcelRepository.IO.submit(()->ParcelDatabase.get(activity).parcels().get(id).state).get());
   findText(page,"撤销").performClick();ParcelRepository.IO.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idle();
   ParcelRecord restored=ParcelRepository.IO.submit(()->ParcelDatabase.get(activity).parcels().get(id)).get();assertEquals(initial,restored.state);assertEquals(record.remindAt,restored.remindAt);
   complete.invoke(page,id);drain();View completed=page.findViewWithTag("parcel-record-"+id);assertNotNull(completed);assertNull(findText(completed,"已取件"));assertNull(completed.findViewWithTag("parcel-complete-"+id));page.findViewWithTag("parcel-content-"+id).performClick();page.findViewWithTag("parcel-restore-"+id).performClick();drain();
   ParcelRecord reopened=ParcelRepository.IO.submit(()->ParcelDatabase.get(activity).parcels().get(id)).get();assertEquals(initial,reopened.state);assertEquals(0,reopened.remindAt);TextView activeCode=page.findViewWithTag("parcel-code-"+id);assertEquals(activity.getColor(R.color.parcel_ink),activeCode.getCurrentTextColor());assertEquals(0,activeCode.getPaintFlags()&android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
   ParcelRepository.IO.submit(()->new ParcelRepository(activity).delete(id)).get();
  }
 }
 @Test public void readTextRequiresConfirmationBeforeCompletion()throws Exception{
  try(ActivityController<ParcelActivity> controller=Robolectric.buildActivity(ParcelActivity.class).setup()){
   ParcelActivity activity=controller.get();ParcelPage page=org.robolectric.util.ReflectionHelpers.getField(activity,"page");page.pause();drain();
   page.findViewWithTag("parcel-read-info").performClick();AlertDialog read=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();((EditText)read.getWindow().getDecorView().findViewWithTag("parcel-import-text")).setText("包裹已到达合成读取驿站，取件码：7-8-4567");read.getWindow().getDecorView().findViewWithTag("parcel-read-text").performClick();drain();
   ParcelRecord record=ParcelRepository.IO.submit(()->ParcelDatabase.get(activity).parcels().all().stream().filter(p->p.code.equals("7-8-4567")).findFirst().orElse(null)).get();assertNotNull(record);assertFalse(record.confirmed);assertNotNull(page.findViewWithTag("parcel-record-"+record.id));assertNull(page.findViewWithTag("parcel-complete-"+record.id));
   page.findViewWithTag("parcel-content-"+record.id).performClick();findText(page.findViewWithTag("parcel-inline-actions-"+record.id),"核对并确认").performClick();AlertDialog editor=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();editor.getWindow().getDecorView().findViewWithTag("parcel-editor-save").performClick();drain();
   assertTrue(page.findViewWithTag("parcel-complete-"+record.id) instanceof Button);page.findViewWithTag("parcel-complete-"+record.id).performClick();drain();assertNotNull(page.findViewWithTag("parcel-record-"+record.id));assertNull(findText(page.findViewWithTag("parcel-record-"+record.id),"已取件"));assertEquals(ParcelRecord.DONE,(int)ParcelRepository.IO.submit(()->ParcelDatabase.get(activity).parcels().get(record.id).state).get());
   findText(page,"撤销").performClick();drain();assertNotNull(page.findViewWithTag("parcel-record-"+record.id));ParcelRepository.IO.submit(()->new ParcelRepository(activity).delete(record.id)).get();
  }
 }
 private void drain()throws Exception{for(int i=0;i<3;i++){ParcelRepository.IO.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(400));}}
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
   assertNull(root.findViewWithTag("parcel-filter-pager"));assertNull(root.findViewWithTag("parcel-filter-0"));
   View pickup=root.findViewWithTag("parcel-record-101"),confirmation=root.findViewWithTag("parcel-record-102"),shipping=root.findViewWithTag("parcel-record-103");assertNotNull(pickup);assertNotNull(confirmation);assertNotNull(shipping);View completed=root.findViewWithTag("parcel-record-104");assertNotNull(completed);assertTrue(completed.getTop()>shipping.getTop());TextView completedCode=root.findViewWithTag("parcel-code-104");assertEquals(activity.getColor(R.color.parcel_muted),completedCode.getCurrentTextColor());assertTrue((completedCode.getPaintFlags()&android.graphics.Paint.STRIKE_THRU_TEXT_FLAG)!=0);assertNull(findText(completed,"已取件"));assertNull(completed.findViewWithTag("parcel-complete-104"));assertNotNull(completed.findViewWithTag("parcel-restore-104"));
   assertNull(root.findViewWithTag("parcel-complete-102"));assertTrue(root.findViewWithTag("parcel-complete-103") instanceof Button);assertFalse(hasCheckBox(root));
   View firstActions=root.findViewWithTag("parcel-inline-actions-101"),secondActions=root.findViewWithTag("parcel-inline-actions-102");assertEquals(View.GONE,firstActions.getVisibility());root.findViewWithTag("parcel-content-101").performClick();assertEquals(View.VISIBLE,firstActions.getVisibility());root.findViewWithTag("parcel-content-102").performClick();assertEquals(View.GONE,firstActions.getVisibility());assertEquals(View.VISIBLE,secondActions.getVisibility());root.findViewWithTag("parcel-content-102").performClick();assertEquals(View.GONE,secondActions.getVisibility());
   assertNotNull(findText(root,"身份码"));assertNotNull(root.findViewWithTag("parcel-manual-add"));assertNotNull(root.findViewWithTag("parcel-read-info"));assertFalse(hasText(root,"查询物流"));
   ViewGroup identity=root.findViewWithTag("parcel-identity");
   for(int i=0;i<3;i++){TextView entry=root.findViewWithTag("parcel-identity-"+i);assertEquals("identity labels should not wrap",1,entry.getLineCount());assertContained(identity,entry);assertTrue("identity touch target",entry.getHeight()>=Math.round(48*activity.getResources().getDisplayMetrics().density));assertTrue("identity text must not be vertically clipped",entry.getLayout().getHeight()+entry.getCompoundPaddingTop()+entry.getCompoundPaddingBottom()<=entry.getHeight());}
   android.graphics.drawable.Drawable addBackground=root.findViewWithTag("quick-menu-button").getBackground();
   assertTrue("add button should keep the shared light-circle icon style",addBackground instanceof android.graphics.drawable.GradientDrawable&&((android.graphics.drawable.GradientDrawable)addBackground).getColor().getDefaultColor()==activity.getColor(R.color.app_card));
   layout(root,activity);snapshot(root,image);root.findViewWithTag("parcel-content-101").performClick();shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(400));layout(root,activity);ScrollView parcelScroll=root.findViewWithTag("parcel-scroll");if(font>1f)parcelScroll.scrollTo(0,((View)pickup.getParent()).getTop());snapshot(root,image+"-expanded");parcelScroll.scrollTo(0,parcelScroll.getChildAt(0).getHeight());shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(400));snapshot(root,image+"-completed");
   Bundle state=new Bundle();page.saveState(state);assertTrue(state.containsKey("scrollY"));assertFalse(state.containsKey("filter"));
   root.findViewWithTag("parcel-read-info").performClick();AlertDialog read=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(read.getWindow().getDecorView().findViewWithTag("parcel-import-text"));assertNull(read.getWindow().getDecorView().findViewWithTag("parcel-settings-menu"));read.getWindow().getDecorView().findViewWithTag("parcel-read-text").performClick();assertEquals(View.VISIBLE,read.getWindow().getDecorView().findViewWithTag("parcel-import-feedback").getVisibility());
   read.getWindow().getDecorView().findViewWithTag("parcel-notification-settings").performClick();AlertDialog settings=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(findText(settings.getWindow().getDecorView(),"后台通知识别"));assertNull(settings.getWindow().getDecorView().findViewWithTag("parcel-settings-menu"));
   root.findViewWithTag("parcel-manual-add").performClick();
   AlertDialog editor=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(editor.getWindow().getDecorView().findViewWithTag("parcel-editor-code"));assertFalse(hasSpinner(editor.getWindow().getDecorView()));
   layoutDialog(editor,activity);
   View footer=editor.getWindow().getDecorView().findViewWithTag("parcel-editor-actions");assertNotNull(footer);assertFalse(footer.getParent() instanceof ScrollView);assertTrue(footer.getHeight()>0);if(((LinearLayout)footer).getOrientation()==LinearLayout.HORIZONTAL)assertEquals("footer buttons must align",findText(footer,"取消").getTop(),findText(footer,"保存").getTop());
   Button submit=editor.getWindow().getDecorView().findViewWithTag("parcel-editor-save");submit.performClick();ParcelRepository.IO.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(editor.isShowing());assertTrue(submit.isEnabled());assertTrue(hasText(editor.getWindow().getDecorView(),"请填写取件码或运单号"));
   layoutDialog(editor,activity);View validation=findText(editor.getWindow().getDecorView(),"请填写取件码或运单号");assertTrue(validation.getHeight()>0);assertTrue(validation.getBottom()<=footer.getTop());snapshot(editor.getWindow().getDecorView(),image+"-editor");
   java.lang.reflect.Field tracking=ParcelPage.class.getDeclaredField("trackingField");tracking.setAccessible(true);((EditText)tracking.get(page)).setText("CL12345678901");editor.getWindow().getDecorView().findViewWithTag("parcel-editor-save").performClick();
   ParcelRepository.IO.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idle();
   ParcelRecord saved=ParcelRepository.IO.submit(()->ParcelDatabase.get(activity).parcels().all().stream().filter(p->p.trackingNumber.equals("CL12345678901")).findFirst().orElse(null)).get();assertNotNull(saved);assertEquals(ParcelRecord.TRANSIT,saved.state);assertNull(root.findViewWithTag("parcel-filter-pager"));
   ParcelRepository.IO.submit(()->new ParcelRepository(activity).delete(saved.id)).get();
  }
  ParcelRepository.IO.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idle();
 }
 private void layout(View root,android.app.Activity activity){android.util.DisplayMetrics dm=activity.getResources().getDisplayMetrics();root.measure(View.MeasureSpec.makeMeasureSpec(dm.widthPixels,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(dm.heightPixels,View.MeasureSpec.EXACTLY));root.layout(0,0,dm.widthPixels,dm.heightPixels);}
 private void layoutDialog(AlertDialog dialog,android.app.Activity activity){android.util.DisplayMetrics dm=activity.getResources().getDisplayMetrics();View root=dialog.getWindow().getDecorView();int height=dialog.getWindow().getAttributes().height;root.measure(View.MeasureSpec.makeMeasureSpec(dm.widthPixels,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height>0?height:dm.heightPixels,height>0?View.MeasureSpec.EXACTLY:View.MeasureSpec.AT_MOST));root.layout(0,0,root.getMeasuredWidth(),root.getMeasuredHeight());}
 private void assertContained(ViewGroup parent,View child){assertNotNull(child);android.graphics.Rect bounds=new android.graphics.Rect(0,0,child.getWidth(),child.getHeight());parent.offsetDescendantRectToMyCoords(child,bounds);assertTrue("child must have height",bounds.height()>0);assertTrue("child must fit parent: "+bounds+" in "+parent.getWidth()+"x"+parent.getHeight(),bounds.left>=0&&bounds.top>=0&&bounds.right<=parent.getWidth()&&bounds.bottom<=parent.getHeight());}
 private View findText(View view,String text){if(view instanceof TextView&&((TextView)view).getText().toString().equals(text))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=findText(((ViewGroup)view).getChildAt(i),text);if(found!=null)return found;}return null;}
 private boolean hasText(View view,String text){if(view instanceof TextView&&((TextView)view).getText().toString().contains(text))return true;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(hasText(((ViewGroup)view).getChildAt(i),text))return true;return false;}
 private boolean hasCheckBox(View view){if(view instanceof CheckBox)return true;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(hasCheckBox(((ViewGroup)view).getChildAt(i)))return true;return false;}
 private boolean hasSpinner(View view){if(view instanceof Spinner)return true;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(hasSpinner(((ViewGroup)view).getChildAt(i)))return true;return false;}
 private void snapshot(View root,String name)throws Exception{String directory=System.getProperty("courselens.parcelPreviewDir");if(directory==null)return;root.getViewTreeObserver().dispatchOnPreDraw();shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(600));java.io.File folder=new java.io.File(directory);folder.mkdirs();Bitmap bitmap=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(folder,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}
}
