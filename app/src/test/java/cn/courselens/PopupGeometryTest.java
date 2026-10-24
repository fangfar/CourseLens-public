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
   parcel.settings();AlertDialog dropdown=getField(parcel,"currentSheet");assertEquals(Color.TRANSPARENT,a.getWindow().getNavigationBarColor());assertEquals(R.style.DropdownPopupMotion,dropdown.getWindow().getAttributes().windowAnimations);dropdown.dismiss();shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(Color.TRANSPARENT,a.getWindow().getNavigationBarColor());
   assertEquals(300,a.getResources().getInteger(R.integer.popup_menu_enter));assertEquals(220,a.getResources().getInteger(R.integer.popup_menu_exit));
  }
 }
 private void squareBottom(Drawable drawable,SecondaryUi ui){int size=ui.dp(100);Bitmap pixels=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);drawable.setBounds(0,0,size,size);drawable.draw(new Canvas(pixels));assertEquals(Color.TRANSPARENT,pixels.getPixel(ui.dp(2),0));for(int x:new int[]{ui.dp(2),size/2,size-ui.dp(2)-1})assertEquals("Bottom edge must stay opaque without a corner gap or divider",ui.CARD,pixels.getPixel(x,size-1));pixels.recycle();}
 private void layout(View view){int width=view.getResources().getDisplayMetrics().widthPixels,height=view.getResources().getDisplayMetrics().heightPixels;view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,width,height);}
}
