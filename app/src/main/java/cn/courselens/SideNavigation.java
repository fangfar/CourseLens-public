package cn.courselens;

import android.animation.*;
import android.app.Activity;
import android.graphics.Typeface;
import android.os.Build;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import java.util.function.IntConsumer;

@android.annotation.SuppressLint("ViewConstructor")
final class SideNavigation extends FrameLayout {
 private final Activity activity;
 private final SecondaryUi ui;
 private final View content,shade;
 private final FrameLayout foreground;
 private final LinearLayout panel;
 private final TextView[] destinations=new TextView[3];
 private final int surface,ink;
 private Object back;
 private View previousFocus;
 private int contentAccessibility,drawerWidth;
 private float progress;
 private boolean open,routing,backRegistered;
 private ValueAnimator animator;

 SideNavigation(Activity activity,View content,int surface,int card,int ink,IntConsumer navigate,Runnable settings){
  super(activity);this.activity=activity;this.content=content;this.surface=surface;this.ink=ink;ui=new SecondaryUi(activity);setTag("side-navigation");setBackgroundColor(card);
  panel=new LinearLayout(activity);panel.setOrientation(LinearLayout.VERTICAL);panel.setBackgroundColor(card);panel.setPadding(ui.dp(16),ui.dp(20),ui.dp(8),ui.dp(16));panel.setTag("side-navigation-panel");panel.setVisibility(GONE);if(Build.VERSION.SDK_INT>=28)panel.setAccessibilityPaneTitle("导航菜单");addView(panel,new LayoutParams(0,-1,Gravity.LEFT));
  panel.setOnApplyWindowInsetsListener((view,insets)->{view.setPadding(ui.dp(16),insets.getSystemWindowInsetTop()+ui.dp(20),ui.dp(8),insets.getSystemWindowInsetBottom()+ui.dp(16));return insets;});
  TextView title=ui.text("CourseLens",28,ink);title.setPadding(ui.dp(8),ui.dp(24),0,ui.dp(36));panel.addView(title);
  ScrollView scroll=new ScrollView(activity);panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout items=new LinearLayout(activity);items.setOrientation(LinearLayout.VERTICAL);scroll.addView(items);
  TextView label=ui.text("选项卡",13,ink);label.setAlpha(.6f);label.setPadding(ui.dp(12),0,0,ui.dp(12));items.addView(label);
  String[] names={"课表","任务","快递"},tags={"drawer-timetable","drawer-tasks","drawer-parcels"};int[] icons={R.drawable.ic_calendar,R.drawable.ic_tasks,R.drawable.ic_parcel};
  for(int i=0;i<names.length;i++){int index=i;destinations[i]=item(names[i],icons[i],tags[i],()->choose(()->navigate.accept(index)));items.addView(destinations[i],new LinearLayout.LayoutParams(-1,-2));}
  LinearLayout footer=new LinearLayout(activity);footer.setPadding(ui.dp(8),ui.dp(12),0,0);panel.addView(footer,new LinearLayout.LayoutParams(-1,-2));Button preferences=ui.iconAction(footer,R.drawable.ic_settings,"设置",()->choose(settings));preferences.setTag("drawer-settings");preferences.getCompoundDrawables()[0].setTint(ink);preferences.setBackground(ui.bg(surface,24));((LinearLayout.LayoutParams)preferences.getLayoutParams()).leftMargin=0;
  foreground=new FrameLayout(activity);foreground.setBackground(ui.bg(surface,28));foreground.addView(content,new LayoutParams(-1,-1));addView(foreground,new LayoutParams(-1,-1));
  shade=new View(activity);shade.setTag("drawer-shade");shade.setBackgroundColor(android.graphics.Color.BLACK);shade.setContentDescription("关闭导航菜单");shade.setFocusable(true);shade.setOnClickListener(v->close(true));shade.setVisibility(GONE);foreground.addView(shade,new LayoutParams(-1,-1));
 }
 private TextView item(String title,int drawable,String tag,Runnable action){
  TextView item=ui.text(title,20,ink);item.setTag(tag);item.setGravity(Gravity.CENTER_VERTICAL);item.setPadding(ui.dp(16),ui.dp(14),ui.dp(12),ui.dp(14));item.setMinHeight(ui.dp(56));item.setFocusable(true);
  android.graphics.drawable.Drawable icon=activity.getDrawable(drawable).mutate();icon.setTint(ink);icon.setBounds(0,0,ui.dp(24),ui.dp(24));item.setCompoundDrawables(icon,null,null,null);item.setCompoundDrawablePadding(ui.dp(16));item.setOnClickListener(v->action.run());ui.ripple(item);return item;
 }
 void select(int selected){
  for(int i=0;i<destinations.length;i++){boolean active=i==selected;destinations[i].setSelected(active);destinations[i].setTypeface(null,active?Typeface.BOLD:Typeface.NORMAL);destinations[i].setBackground(active?ui.bg(surface,16):null);}
 }
 boolean isOpen(){return panel.getVisibility()==VISIBLE;}
 void open(int selected){
  if(open||routing)return;select(selected);open=true;if(!isOpen()){previousFocus=activity.getCurrentFocus();contentAccessibility=content.getImportantForAccessibility();}content.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);panel.setVisibility(VISIBLE);panel.requestApplyInsets();shade.setVisibility(VISIBLE);
  if(Build.VERSION.SDK_INT>=33&&!backRegistered){back=Api33.register(activity,()->close(true));backRegistered=true;}
  destinations[selected].requestFocus();animateTo(1f,true);
 }
 boolean close(boolean animate){
  // While a destination route owns the close, back/shade taps must not finish the host page.
  if(routing)return true;
  if(!open)return false;open=false;animateTo(0f,animate);return true;
 }
 // Route in the same frame so the page transition overlaps the drawer rebound; later taps are
 // ignored until the drawer finishes closing, preventing a second launch while it retracts.
 private void choose(Runnable action){
  if(routing)return;
  if(!isOpen()&&progress<=0f){action.run();return;}
  routing=true;open=false;animateTo(0f,true);action.run();
 }
 private void animateTo(float target,boolean animate){
  if(animator!=null){animator.removeAllListeners();animator.cancel();animator=null;}
  if(!animate||!ValueAnimator.areAnimatorsEnabled()){render(target);if(target==0f)closed();return;}
  animator=ValueAnimator.ofFloat(progress,target);animator.setDuration(target==1f?260:220);animator.setInterpolator(new DecelerateInterpolator());
  animator.addUpdateListener(value->render((float)value.getAnimatedValue()));
  animator.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator value){animator=null;if(target==0f)closed();}});
  animator.start();
 }
 private void render(float value){progress=value;foreground.setTranslationX(drawerWidth*value);foreground.setClipToOutline(value>0);shade.setAlpha(.18f*value);}
 private void closed(){
  routing=false;panel.setVisibility(GONE);shade.setVisibility(GONE);content.setImportantForAccessibility(contentAccessibility);
  if(Build.VERSION.SDK_INT>=33&&backRegistered){Api33.unregister(activity,back);back=null;backRegistered=false;}
  if(previousFocus!=null&&previousFocus.isAttachedToWindow())previousFocus.requestFocus();previousFocus=null;
 }
 void dismiss(){
  routing=false;open=false;
  if(animator!=null){animator.removeAllListeners();animator.cancel();animator=null;}
  render(0f);closed();
 }
 @Override protected void onMeasure(int width,int height){drawerWidth=Math.max(1,Math.min(MeasureSpec.getSize(width)*4/5,MeasureSpec.getSize(width)-ui.dp(64)));panel.getLayoutParams().width=drawerWidth;super.onMeasure(width,height);}
 @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){super.onLayout(changed,left,top,right,bottom);render(progress);}
 @Override protected void onDetachedFromWindow(){dismiss();super.onDetachedFromWindow();}
 // Keep API 33 callback classes out of the drawer's initialization on older Android versions.
 @androidx.annotation.RequiresApi(33)
 private static final class Api33 {
  static Object register(Activity activity,Runnable close){android.window.OnBackInvokedCallback callback=close::run;activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY,callback);return callback;}
  static void unregister(Activity activity,Object callback){activity.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback((android.window.OnBackInvokedCallback)callback);}
 }

}
