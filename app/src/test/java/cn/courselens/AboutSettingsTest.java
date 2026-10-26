package cn.courselens;

import android.content.Intent;
import android.view.View;
import android.widget.Switch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.util.ReflectionHelpers.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=34)
public class AboutSettingsTest {
 @Test public void aboutLinksAndNestedUpdateSettings(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();callInstanceMethod(activity,"menu");
   View settings=getField(activity,"root");settings.findViewWithTag("about-app").performClick();
   View about=getField(activity,"root");assertEquals("关于软件",about.getTag());
   assertNull(about.findViewWithTag("disk-download"));assertNull(about.findViewWithTag("check-app-update"));
   assertNull(about.findViewWithTag("auto-check-updates"));assertNull(about.findViewWithTag("update-status"));
   assertNotNull(about.findViewWithTag("public-repository"));
   about.findViewWithTag("official-website").performClick();Intent website=shadowOf(activity).getNextStartedActivity();
   assertEquals(Intent.ACTION_VIEW,website.getAction());assertEquals("https://morrow.xvyan.cn/",website.getDataString());
   about.findViewWithTag("update-settings").performClick();View updates=getField(activity,"root");
   assertEquals("软件更新",updates.getTag());assertTrue(updates.findViewWithTag("check-app-update").hasOnClickListeners());
   assertNotNull(updates.findViewWithTag("update-status"));Switch automatic=updates.findViewWithTag("auto-check-updates");
   boolean previous=automatic.isChecked();automatic.setChecked(!previous);
   assertEquals(!previous,activity.getSharedPreferences("appSettings",0).getBoolean("autoCheckUpdates",previous));
   ((Runnable)getField(activity,"pageBack")).run();about=getField(activity,"root");
   assertEquals("关于软件",about.getTag());assertNotNull(about.findViewWithTag("official-website"));
   about.findViewWithTag("update-settings").performClick();updates=getField(activity,"root");
   assertEquals(!previous,((Switch)updates.findViewWithTag("auto-check-updates")).isChecked());
  }
 }
}
