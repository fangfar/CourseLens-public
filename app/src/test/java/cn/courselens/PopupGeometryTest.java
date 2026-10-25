package cn.courselens;

import android.app.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;
import static org.robolectric.util.ReflectionHelpers.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class) @Config(sdk=34) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PopupGeometryTest {
 @Test public void lightSheetsJoinTheGestureArea(){check("w380dp-h800dp-notnight");}
 @Test public void darkSheetsJoinTheGestureArea(){check("w380dp-h800dp-night");}
 @Test public void allFormSelectorsUseTheOuterSheetHostAndApplyInsetsOnce(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity a=controller.get();View content=a.findViewById(android.R.id.content);SecondaryUi ui=new SecondaryUi(a);layout(content);
   for(String form:new String[]{"course","adjustment","timetable"}){
    if(form.equals("course"))callInstanceMethod(a,"courseForm",ClassParameter.from(Course.class,(Course)null),ClassParameter.from(int.class,-1),ClassParameter.from(Runnable.class,()->{}));
    else if(form.equals("adjustment"))callInstanceMethod(a,"adjustmentForm",ClassParameter.from(Course.class,new Course(1,1,2,"合成课程","教室","1-30","老师")),ClassParameter.from(int.class,1),ClassParameter.from(boolean.class,false),ClassParameter.from(Runnable.class,()->{}));
    else callInstanceMethod(a,"newTimetable");
    android.widget.FrameLayout stage=getField(a,"stage"),host=getField(a,"windowContent");stage.setPadding(0,ui.dp(24),0,ui.dp(32));MotionOverlay choice=getField(a,"choiceSheet");assertSame(form+": selector must not inherit page insets or page motion",host,choice.getParent());
    String[] tags=form.equals("course")?new String[]{"course-day","course-start","course-end"}:form.equals("adjustment")?new String[]{"adjustment-week","adjustment-day","adjustment-start","adjustment-end"}:new String[]{"timetable-source"};for(String tag:tags){((View)getField(a,"root")).findViewWithTag(tag).performClick();
    for(int keyboard:new int[]{0,ui.dp(280)}){
     androidx.core.view.WindowInsetsCompat insets=new androidx.core.view.WindowInsetsCompat.Builder().setInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars(),androidx.core.graphics.Insets.of(0,ui.dp(24),0,ui.dp(32))).setInsets(androidx.core.view.WindowInsetsCompat.Type.ime(),androidx.core.graphics.Insets.of(0,0,0,keyboard)).build();androidx.core.view.ViewCompat.dispatchApplyWindowInsets(choice,insets);layout(content);choice.motion.set(1);View panel=getField(choice,"panel");Rect bounds=new Rect(0,0,panel.getWidth(),panel.getHeight());host.offsetDescendantRectToMyCoords(panel,bounds);assertEquals(host.getHeight()-keyboard,bounds.bottom);assertEquals((choice.getHeight()-keyboard)/2,panel.getHeight());squareBottom(panel.getBackground(),ui);
    }callInstanceMethod(choice,"close",ClassParameter.from(boolean.class,false));}
    callInstanceMethod(a,"home");assertNull(choice.getParent());assertNull(getField(a,"choiceSheet"));assertEquals(1,host.getChildCount());
   }
  }
 }
 private void check(String qualifiers){
  RuntimeEnvironment.setQualifiers(qualifiers);
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity a=controller.get();SecondaryUi ui=new SecondaryUi(a);View content=a.findViewById(android.R.id.content);layout(content);
   assertEquals("Status bar must be transparent for edge-to-edge",Color.TRANSPARENT,a.getWindow().getStatusBarColor());
   assertEquals("Navigation bar must be transparent for edge-to-edge",Color.TRANSPARENT,a.getWindow().getNavigationBarColor());
   MainActivity.NavigationOverlay sheet=a.new NavigationOverlay(false,"合成面板");layout(content);assertEquals(((View)sheet.panel.getParent()).getHeight(),sheet.panel.getBottom());assertEquals(Color.TRANSPARENT,a.getWindow().getNavigationBarColor());
   squareBottom(sheet.panel.getBackground(),ui);
   MainActivity.NavigationOverlay menu=a.new NavigationOverlay(true,"合成菜单");assertEquals(Color.TRANSPARENT,a.getWindow().getNavigationBarColor());menu.close(false);assertEquals(Color.TRANSPARENT,a.getWindow().getNavigationBarColor());sheet.close(false);assertEquals(Color.TRANSPARENT,a.getWindow().getNavigationBarColor());
   callInstanceMethod(a,"newTimetable");View form=getField(a,"root");form.findViewWithTag("timetable-source").performClick();Object choice=getField(a,"choiceSheet");View panel=getField(choice,"panel");squareBottom(panel.getBackground(),ui);assertEquals(Color.TRANSPARENT,a.getWindow().getNavigationBarColor());callInstanceMethod(choice,"close",ClassParameter.from(boolean.class,false));assertEquals(Color.TRANSPARENT,a.getWindow().getNavigationBarColor());
   callInstanceMethod(a,"home");callInstanceMethod(a,"parcels");ParcelPage parcel=getField(a,"parcelPage");callInstanceMethod(parcel,"showActions");AlertDialog dialog=getField(parcel,"currentSheet");assertEquals(Gravity.BOTTOM,dialog.getWindow().getAttributes().gravity);assertEquals(0,dialog.getWindow().getDecorView().getPaddingBottom());assertEquals(Color.TRANSPARENT,a.getWindow().getNavigationBarColor());squareBottom(dialog.getWindow().getDecorView().findViewWithTag("parcel-sheet").getBackground(),ui);
   parcel.settings();AlertDialog dropdown=getField(parcel,"currentSheet");assertEquals(Color.TRANSPARENT,a.getWindow().getNavigationBarColor());assertEquals(0,dropdown.getWindow().getAttributes().windowAnimations);assertTrue(dropdown instanceof MotionSheetDialog);dropdown.dismiss();shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(Color.TRANSPARENT,a.getWindow().getNavigationBarColor());
   assertEquals(Motion.MENU_IN,a.getResources().getInteger(R.integer.popup_menu_enter));assertEquals(Motion.MENU_OUT,a.getResources().getInteger(R.integer.popup_menu_exit));
  }
 }
 private void squareBottom(Drawable drawable,SecondaryUi ui){int size=ui.dp(100);Bitmap pixels=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);drawable.setBounds(0,0,size,size);drawable.draw(new Canvas(pixels));assertEquals(Color.TRANSPARENT,pixels.getPixel(ui.dp(2),0));for(int x:new int[]{ui.dp(2),size/2,size-ui.dp(2)-1})assertEquals("Bottom edge must stay opaque without a corner gap or divider",ui.CARD,pixels.getPixel(x,size-1));pixels.recycle();}
 private void layout(View view){int width=view.getResources().getDisplayMetrics().widthPixels,height=view.getResources().getDisplayMetrics().heightPixels;view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,width,height);}
}
