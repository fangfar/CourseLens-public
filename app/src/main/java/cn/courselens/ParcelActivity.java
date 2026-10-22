package cn.courselens;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.widget.LinearLayout;

public class ParcelActivity extends Activity {
 static final String OPEN_SETTINGS="cn.courselens.PARCEL_SETTINGS";
 private SideNavigation sideNavigation;
 private ParcelPage page;
 private boolean navigatingMain;
 @Override protected void attachBaseContext(Context base){super.attachBaseContext(Appearance.wrap(base));}
 @Override public void onCreate(Bundle state){
  super.onCreate(state);SecondaryUi ui=new SecondaryUi(this);int ink=getColor(R.color.parcel_ink),muted=getColor(R.color.parcel_muted),card=getColor(R.color.parcel_card),surface=getColor(R.color.parcel_surface);
  LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.setBackgroundColor(surface);
  page=new ParcelPage(this,state,()->sideNavigation.open(2));
  shell.addView(page,new LinearLayout.LayoutParams(-1,0,1));
  shell.addView(new AppNavigation(this,surface,ink,muted,2,this::selectBottomPage),new LinearLayout.LayoutParams(-1,-2));
  shell.setOnApplyWindowInsetsListener((view,insets)->{view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
  sideNavigation=new SideNavigation(this,shell,surface,card,ink,this::selectPrimaryPage,this::openMainSettings);
  sideNavigation.select(2);setContentView(sideNavigation);
  getWindow().setStatusBarColor(surface);getWindow().setNavigationBarColor(surface);Appearance.systemBars(this);
  if(state==null)page.handleIntent(getIntent());
 }
 private void selectPrimaryPage(int selected){if(selected!=2)openMain(selected);}
 private void selectBottomPage(int selected){if(selected==2)return;openMain(selected);}
 private void openMain(int selected){
  if(navigatingMain)return;navigatingMain=true;
  startActivity(new Intent(this,MainActivity.class).setAction(MainActivity.OPEN_TAB).putExtra("primaryTab",selected).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
 }
 private void openMainSettings(){
  startActivity(new Intent(this,MainActivity.class).setAction(MainActivity.OPEN_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
 }
 @Override public void onBackPressed(){if(sideNavigation!=null&&sideNavigation.close(true))return;if(navigatingMain)return;super.onBackPressed();}
 @Override protected void onResume(){super.onResume();navigatingMain=false;page.resume();}
 @Override protected void onPause(){page.pause();super.onPause();}
 @Override protected void onDestroy(){page.close();sideNavigation.dismiss();super.onDestroy();}
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);page.saveState(out);}
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);page.handleIntent(intent);}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);page.onActivityResult(request,result,data);}
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);page.onRequestPermissionsResult(request,permissions,grants);}
}
