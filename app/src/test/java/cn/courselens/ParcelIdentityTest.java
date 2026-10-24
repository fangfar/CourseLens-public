package cn.courselens;

import android.app.Activity;
import android.content.Intent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
public class ParcelIdentityTest {
 @Test public void pinduoduoOpensIdentityTabInsteadOfPickup(){
  try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
   Activity activity=controller.get();ParcelIdentity.open(activity,2);
   Intent intent=shadowOf(activity).getNextStartedActivity();
   assertEquals(Intent.ACTION_VIEW,intent.getAction());assertEquals("com.xunmeng.pinduoduo",intent.getPackage());
   assertEquals("/mdkd/package",intent.getData().getPath());assertEquals("ID_CODE",intent.getData().getQueryParameter("tab"));assertEquals("11",intent.getData().getQueryParameter("entry_source"));
  }
 }
}
