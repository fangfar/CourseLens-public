package cn.courselens;

import android.app.*;
import android.os.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.text.TextUtils;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.content.*;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.PagerAdapter;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
 private final SecondaryUi secondaryUi=new SecondaryUi(this);
 private static final String[] START_TIMES=NextClass.START_TIMES,END_TIMES=NextClass.END_TIMES;
 private final int INK=0xff172132,MUTED=0xff68788f,BLUE=0xff5c78c6,PRIMARY=0xff4266ad;
 private final int SURFACE=SecondaryUi.SURFACE,AQUA=0xff9aebf2,SOFT=0xffdff8fa,NAVY=0xff082630;
 private AlertDialog widgetDialog,unsavedDialog;
 private AlertDialog remoteDialog;private RemoteConfigClient remote;private TextView remoteStatus;private final Handler remoteHandler=new Handler(Looper.getMainLooper());
 private boolean remoteResumed,touching,manualUpdatePending;private int pagerState,manualCheckScreen=-1;private long interactionAt;private final Runnable remoteNotice=this::showRemoteNotice;private final RemoteConfigClient.Listener remoteListener=this::remoteChanged;
 private final ViewTreeObserver.OnScrollChangedListener remoteScroll=()->{interactionAt=SystemClock.uptimeMillis();scheduleRemoteNotice();};
 private static final String ADD_WIDGET="cn.courselens.ADD_WIDGET";
 private Set<String> displayChoices=new HashSet<>();
 private String adjustments="";
 private String cachedData,cachedAdjustments,cachedTerm;private List<Course> cachedCourses;
 private Map<String,CourseAdjustment> cachedMoves=Collections.emptyMap();
 private Map<Course,String> cachedCardText=new HashMap<>();
 private HomePage homePage;
 private TaskPage taskPage;private Bundle taskState;private boolean tasksVisible;
 private LinearLayout bottomNavigation;private ImageButton timetableTab,tasksTab;
 private LinearLayout settingsPage;private int settingsScrollY;
 private ScrollView timetableScroll;private int timetableScrollY;
 private final PageMotion pageMotion=new PageMotion();private android.window.OnBackInvokedCallback systemBack;private boolean backRegistered;
 private ViewPager pager;private TextView weekLabel,returnWeekButton;private FrameLayout daysBar; private LinearLayout root;private FrameLayout stage;private boolean returningPage;private int week=6;private String data="",notes="",term="2026-08-31";
 private final SchoolClient school=new SchoolClient();private final ExecutorService worker=Executors.newSingleThreadExecutor();private boolean busy=false,showingDetail=false;private int screen=0;private Runnable pageBack;private CourseChoiceSheet choiceSheet;private NavigationOverlay navigationOverlay;
 private EditText account,password,captcha;private TextView loginStatus;private ProgressBar loginProgress;private Button loginButton;private LinearLayout captchaBox;private ImageView captchaImage;
 private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}

 @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(SURFACE);getWindow().setNavigationBarColor(SURFACE);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);data=getPreferences(0).getString("courses","");notes=getPreferences(0).getString("notes","");if((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0)school.onDiagnostic=summary->{try(java.io.FileOutputStream out=openFileOutput("sync-diagnostic.txt",0)){out.write(summary.getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(Exception ignored){}};else deleteFile("sync-diagnostic.txt");week=getPreferences(0).getInt("week",6);term=getPreferences(0).getString("term",term);displayChoices=new HashSet<>(getPreferences(0).getStringSet("displayChoices",Collections.emptySet()));adjustments=getPreferences(0).getString("courseAdjustments","");home();openWidget(getIntent());widgetShortcut();
  if(state!=null){taskState=state.getBundle("taskPage");if(state.getBoolean("tasksVisible")&&!CourseWidget.OPEN.equals(getIntent().getAction())&&!ADD_WIDGET.equals(getIntent().getAction()))tasks();}
  View decor=getWindow().getDecorView();decor.getViewTreeObserver().addOnScrollChangedListener(remoteScroll);
  decor.getViewTreeObserver().addOnDrawListener(new ViewTreeObserver.OnDrawListener(){boolean drawn;public void onDraw(){if(drawn)return;drawn=true;decor.post(()->{if(decor.getViewTreeObserver().isAlive())decor.getViewTreeObserver().removeOnDrawListener(this);remoteHandler.postDelayed(()->{if(!isDestroyed()&&!isFinishing()){remote=RemoteConfigClient.get(MainActivity.this);remote.listen(remoteListener);remote.check(false);refreshRemoteStatus();}},1500);});}});
 }
 @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putBoolean("tasksVisible",tasksVisible);Bundle page=new Bundle();if(taskPage!=null)taskPage.saveState(page);else if(taskState!=null)page.putAll(taskState);state.putBundle("taskPage",page);}
 private void saveWeek(){getPreferences(0).edit().putInt("week",week).apply();}
 private void save(){getPreferences(0).edit().putString("courses",data).putString("notes",notes).putInt("week",week).putString("term",term).apply();CourseWidget.updateAll(this);}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 private GradientDrawable bg(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
 private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);return t;}
 private void base(boolean secondary){base(secondary,!secondary&&pageBack==null);}
 private void base(boolean secondary,boolean fadeThrough){base(secondary,fadeThrough,null);}
 private void base(boolean secondary,boolean fadeThrough,LinearLayout retained){
  manualUpdatePending=false;pagerState=ViewPager.SCROLL_STATE_IDLE;if(remoteDialog!=null)remoteDialog.dismiss();scheduleRemoteNotice();
  pageMotion.cancel();
  if(unsavedDialog!=null){unsavedDialog.dismiss();unsavedDialog=null;}
  while(navigationOverlay!=null)navigationOverlay.close(false);
  if(root==settingsPage&&settingsPage!=null)settingsScrollY=((ScrollView)settingsPage.getChildAt(1)).getScrollY();
  if(timetableScroll!=null){timetableScrollY=timetableScroll.getScrollY();timetableScroll=null;}
  if(choiceSheet!=null){choiceSheet.close(false);choiceSheet=null;}
  LinearLayout previous=root;boolean backwards=returningPage||!secondary;
  pageBack=null;updateSystemBack();showingDetail=false;screen++;pager=null;
  if(stage==null){stage=new FrameLayout(this){@Override public boolean dispatchTouchEvent(MotionEvent event){return pageMotion.isRunning()||super.dispatchTouchEvent(event);}};stage.setBackgroundColor(SURFACE);LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.addView(stage,new LinearLayout.LayoutParams(-1,0,1));bottomNavigation=createBottomNavigation();shell.addView(bottomNavigation,new LinearLayout.LayoutParams(-1,-2));setContentView(shell);}
  bottomNavigation.setVisibility(secondary?View.GONE:View.VISIBLE);updateBottomNavigation();
  for(int i=0;i<stage.getChildCount();i++)stage.getChildAt(i).animate().cancel();
  stage.removeAllViews();
  root=retained!=null?retained:new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setClickable(true);
  root.setBackgroundColor(SURFACE);
  boolean animate=previous!=null&&previous.getWidth()>0&&android.animation.ValueAnimator.areAnimatorsEnabled();
  if(animate){
   stage.addView(previous,new FrameLayout.LayoutParams(-1,-1));
  }
  LinearLayout incoming=root;stage.addView(incoming,new FrameLayout.LayoutParams(-1,-1));
  if(!animate)return;
  // shortcut: dynamic pages replace their source card; use Shared Axis until navigation retains both endpoints.
  pageMotion.start(stage,previous,incoming,backwards,fadeThrough,dp(32));
 }
 private void backPage(){Runnable back=pageBack;if(back==null)return;returningPage=true;try{back.run();}finally{returningPage=false;}}
 private void updateSystemBack(){
  if(Build.VERSION.SDK_INT<33)return;
  boolean needed=tasksVisible||navigationOverlay!=null||pageBack!=null||(choiceSheet!=null&&choiceSheet.getVisibility()==View.VISIBLE)||busy;
  if(needed==backRegistered)return;
  if(systemBack==null)systemBack=this::onBackPressed;
  if(needed)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,systemBack);
  else getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(systemBack);
  backRegistered=needed;
 }
 private void ripple(View view){view.setForeground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x225c78c6),null,bg(Color.WHITE,8)));}

 private LinearLayout createBottomNavigation(){
  LinearLayout bar=new LinearLayout(this);bar.setTag("bottom-navigation");bar.setOrientation(LinearLayout.VERTICAL);bar.setBackgroundColor(SURFACE);
  LinearLayout items=new LinearLayout(this);items.setPadding(dp(36),dp(4),dp(36),dp(4));bar.addView(items,new LinearLayout.LayoutParams(-1,-2));
  timetableTab=navigationTab("课表","nav-timetable",()->{if(tasksVisible)home();});tasksTab=navigationTab("任务","nav-tasks",()->{if(!tasksVisible)tasks();});
  for(ImageButton tab:new ImageButton[]{timetableTab,tasksTab}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(8),0,dp(8),0);items.addView(tab,lp);}return bar;
 }
 private ImageButton navigationTab(String label,String tag,Runnable action){ImageButton tab=new ImageButton(this);tab.setTag(tag);tab.setContentDescription(label);tab.setTooltipText(label);tab.setMinimumHeight(dp(48));tab.setPadding(dp(8),dp(10),dp(8),dp(10));tab.setScaleType(ImageView.ScaleType.CENTER);tab.setBackground(null);tab.setOnClickListener(v->action.run());ripple(tab);return tab;}
 private void updateBottomNavigation(){
  if(bottomNavigation==null)return;ImageButton[] tabs={timetableTab,tasksTab};int[] icons={R.drawable.ic_calendar,R.drawable.ic_tasks};
  for(int i=0;i<tabs.length;i++){ImageButton tab=tabs[i];boolean selected=tasksVisible==(i==1);tab.setSelected(selected);tab.setImageResource(icons[i]);tab.setImageTintList(android.content.res.ColorStateList.valueOf(selected?0xff171717:0xff999999));}
 }
 private void tasks(){getWindow().setStatusBarColor(SURFACE);getWindow().setNavigationBarColor(SURFACE);tasksVisible=true;if(taskPage==null)taskPage=new TaskPage(this,taskState);taskPage.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);base(false,true,taskPage);root.setBackgroundColor(SURFACE);}

 private Calendar date(){Calendar c=Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));c.set(Calendar.HOUR_OF_DAY,12);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c;}
 private Calendar monday(){Calendar c=date();String[] p=term.split("-");c.set(Integer.parseInt(p[0]),Integer.parseInt(p[1])-1,Integer.parseInt(p[2]));return c;}
 private int currentWeek(){return Math.max(1,Math.min(30,(int)((date().getTimeInMillis()-monday().getTimeInMillis())/86400000L/7)+1));}
 private TextView icon(String symbol,String description,Runnable action){TextView t=text(symbol,27,INK);t.setGravity(Gravity.CENTER);t.setContentDescription(description);t.setOnClickListener(v->action.run());t.setFocusable(true);ripple(t);return t;}
 private void changeWeek(int next){if(next<1||next>30)return;if(navigationOverlay!=null)navigationOverlay.close(true);if(pager!=null&&pager.isAttachedToWindow()){pager.setCurrentItem(next-1,true);}else{week=next;saveWeek();home();}}
 private LinearLayout secondary(String title,String description,Runnable back){base(true,root!=null&&title.equals(root.getTag()));root.setTag(title);pageBack=back;updateSystemBack();root.setBackgroundColor(SURFACE);getWindow().setStatusBarColor(SURFACE);getWindow().setNavigationBarColor(SURFACE);LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(12),dp(4),dp(12),dp(4));TextView close=icon("‹","返回",()->{if(busy){toast("正在同步，请稍候");return;}backPage();});close.setText("");android.graphics.drawable.Drawable backIcon=getDrawable(R.drawable.ic_back).mutate();backIcon.setTint(0xff171717);backIcon.setBounds(0,0,dp(24),dp(24));close.setCompoundDrawables(backIcon,null,null,null);close.setPadding(dp(12),0,dp(12),0);GradientDrawable circle=bg(Color.WHITE,24);circle.setStroke(dp(1),0xffe7e7e7);close.setBackground(new android.graphics.drawable.InsetDrawable(circle,dp(4)));close.setClipToOutline(true);bar.addView(close,new LinearLayout.LayoutParams(dp(48),dp(48)));TextView heading=text(title,17,0xff171717);heading.setGravity(Gravity.CENTER);bar.addView(heading,new LinearLayout.LayoutParams(0,-2,1));View spacer=new View(this);spacer.setTag("header-spacer");spacer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);bar.addView(spacer,new LinearLayout.LayoutParams(dp(48),dp(48)));root.addView(bar);ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),dp(8),dp(16),dp(20));scroll.addView(body);if(!description.isEmpty()){TextView intro=text(description,14,MUTED);intro.setLineSpacing(dp(4),1);intro.setPadding(dp(4),0,dp(4),dp(20));body.addView(intro);}return body;}
 private LinearLayout bottomSheet(String title,String description,Runnable back){
  if(returningPage&&navigationOverlay==null)back.run();
  NavigationOverlay overlay=new NavigationOverlay(false,title);LinearLayout body=overlay.body;
  if(!description.isEmpty()){TextView intro=text(description,14,0xff56677e);intro.setLineSpacing(dp(4),1);intro.setPadding(0,0,0,dp(16));body.addView(intro);}
  return body;
 }
 private void quickMenu(){
  if(navigationOverlay!=null&&navigationOverlay.corner){navigationOverlay.close(true);return;}
  NavigationOverlay overlay=new NavigationOverlay(true,"更多操作");
  String[] labels={"课程管理","完整设置","学校登录"};
  Runnable[] actions={()->edit(),()->menu(),()->login()};
  for(int i=0;i<labels.length;i++)menuItem(overlay,labels[i],"quick-action-"+i,actions[i]);
 }
 private void menuItem(NavigationOverlay overlay,String label,String tag,Runnable run){TextView item=text(label,14,INK);item.setTag(tag);item.setGravity(Gravity.CENTER_VERTICAL);item.setPadding(dp(12),dp(10),dp(12),dp(10));item.setMinHeight(dp(48));item.setFocusable(true);item.setOnClickListener(v->{overlay.close(false);run.run();});ripple(item);overlay.body.addView(item,new LinearLayout.LayoutParams(-1,-2));}
 private NavigationOverlay sheetMenu(String title){return new NavigationOverlay(true,title);}
 private class NavigationOverlay extends FrameLayout {
  final boolean corner;final LinearLayout panel,body;final View shade;final LinearLayout underneath;final Runnable underneathBack;final NavigationOverlay previous;
  final int accessibility;final boolean underneathDetail;boolean open=true;int generation;ViewTreeObserver observer;ViewTreeObserver.OnPreDrawListener pending;
  NavigationOverlay(boolean corner,String title){
   super(MainActivity.this);pageMotion.cancel();this.corner=corner;underneath=root;underneathBack=pageBack;underneathDetail=showingDetail;previous=navigationOverlay;accessibility=underneath.getImportantForAccessibility();
   if(previous!=null&&!corner)previous.setVisibility(INVISIBLE);underneath.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
   setFocusableInTouchMode(true);setClickable(true);setTag(corner?"quick-menu":"navigation-sheet");
   shade=new View(MainActivity.this);shade.setBackgroundColor(corner?0x22000000:0x55000000);shade.setTag("navigation-shade");shade.setOnClickListener(v->close(true));addView(shade,new FrameLayout.LayoutParams(-1,-1));
   panel=new LinearLayout(MainActivity.this);panel.setOrientation(LinearLayout.VERTICAL);panel.setTag(title);GradientDrawable surface=bg(Color.WHITE,20);if(!corner)surface.setCornerRadii(new float[]{dp(20),dp(20),dp(20),dp(20),0,0,0,0});panel.setBackground(surface);panel.setClickable(true);panel.setElevation(dp(8));
   FrameLayout.LayoutParams bounds=new FrameLayout.LayoutParams(-1,-2,corner?Gravity.TOP|Gravity.END:Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);bounds.setMargins(corner?dp(12):0,corner?dp(56):0,corner?dp(12):0,0);if(corner){View trigger=underneath.findViewWithTag("sheet-action-button");if(trigger==null)trigger=underneath.findViewWithTag("quick-menu-button");if(trigger!=null&&trigger.getHeight()>0){int[] at=new int[2],stageAt=new int[2];trigger.getLocationInWindow(at);stage.getLocationInWindow(stageAt);bounds.topMargin=at[1]-stageAt[1]+trigger.getHeight();}}addView(panel,bounds);
   if(!corner){View handle=new View(MainActivity.this);handle.setBackground(bg(0xffcfd9eb,2));handle.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);LinearLayout.LayoutParams grip=new LinearLayout.LayoutParams(dp(36),dp(4));grip.gravity=Gravity.CENTER_HORIZONTAL;grip.setMargins(0,dp(10),0,dp(4));panel.addView(handle,grip);}
   if(!corner){LinearLayout bar=new LinearLayout(MainActivity.this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(16),0,dp(8),0);TextView heading=text(title,18,INK);heading.setTag("navigation-title");heading.setTypeface(null,Typeface.BOLD);bar.addView(heading,new LinearLayout.LayoutParams(0,-2,1));TextView close=icon("⋮",title+"操作",()->{});close.setTag("sheet-action-button");close.setVisibility(View.GONE);close.setTextSize(22);close.setBackground(null);bar.addView(close,new LinearLayout.LayoutParams(dp(48),dp(48)));panel.addView(bar);}
   ScrollView scroll=new ScrollView(MainActivity.this);panel.addView(scroll,new LinearLayout.LayoutParams(-1,corner?-2:0,corner?0:1));body=new LinearLayout(MainActivity.this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(corner?dp(4):dp(16),dp(4),corner?dp(4):dp(16),dp(12));scroll.addView(body);
   navigationOverlay=this;root=panel;pageBack=null;showingDetail=corner&&underneathDetail;stage.addView(this,new FrameLayout.LayoutParams(-1,-1));updateSystemBack();requestFocus();if(Build.VERSION.SDK_INT>=28)setAccessibilityPaneTitle(title);
   int token=++generation;shade.setAlpha(0f);panel.setAlpha(corner?0f:1f);panel.setScaleX(corner?.86f:1f);panel.setScaleY(corner?.86f:1f);panel.setTranslationY(corner?0:dp(48));
   observer=getViewTreeObserver();pending=new ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){if(observer.isAlive())observer.removeOnPreDrawListener(this);pending=null;if(token!=generation||!open||!isAttachedToWindow())return true;panel.setPivotX(panel.getWidth());panel.setPivotY(0);if(android.animation.ValueAnimator.areAnimatorsEnabled()){panel.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0).setDuration(200).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();shade.animate().alpha(1f).setDuration(200).start();}else{panel.setAlpha(1f);panel.setScaleX(1f);panel.setScaleY(1f);panel.setTranslationY(0);shade.setAlpha(1f);}return true;}};observer.addOnPreDrawListener(pending);
  }
  // Keep readable text; long sheets scroll instead of shrinking their contents.
  @Override protected void onMeasure(int widthSpec,int heightSpec){
   FrameLayout.LayoutParams bounds=(FrameLayout.LayoutParams)panel.getLayoutParams();int height=MeasureSpec.getSize(heightSpec);
   bounds.width=Math.max(1,MeasureSpec.getSize(widthSpec)-(corner?dp(24):0));bounds.height=-2;
   if(corner){bounds.width=Math.min(bounds.width,dp(200));panel.measure(MeasureSpec.makeMeasureSpec(bounds.width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));bounds.height=Math.min(panel.getMeasuredHeight(),Math.max(1,Math.min(dp(440),height-dp(24))));bounds.topMargin=Math.max(dp(12),Math.min(bounds.topMargin,height-bounds.height-dp(12)));}
   else{panel.measure(MeasureSpec.makeMeasureSpec(bounds.width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));bounds.height=Math.max(1,Math.min(height*4/5,panel.getMeasuredHeight()));}
   super.onMeasure(widthSpec,heightSpec);
  }

  void close(boolean animate){
   scheduleRemoteNotice();
   if(animate&&!open)return;open=false;int token=++generation;panel.animate().cancel();shade.animate().cancel();underneath.setImportantForAccessibility(accessibility);if(navigationOverlay==this){navigationOverlay=previous;root=underneath;pageBack=underneathBack;showingDetail=underneathDetail;if(previous!=null)previous.setVisibility(VISIBLE);updateSystemBack();View focus=corner?underneath.findViewWithTag("sheet-action-button"):underneath;if(corner&&focus==null)focus=underneath.findViewWithTag("quick-menu-button");if(focus!=null)focus.requestFocus();}setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);Runnable finish=()->{if(token==generation)stage.removeView(this);};
   if(animate&&isAttachedToWindow()&&android.animation.ValueAnimator.areAnimatorsEnabled()){panel.animate().alpha(corner?0f:1f).scaleX(corner?.86f:1f).scaleY(corner?.86f:1f).translationY(corner?0:dp(48)).setDuration(180).withEndAction(finish).start();shade.animate().alpha(0f).setDuration(180).start();}else finish.run();
  }
  // Cold attachment replaces the floating observer with the window observer.
  @Override protected void onAttachedToWindow(){super.onAttachedToWindow();observer=getViewTreeObserver();}
  @Override protected void onDetachedFromWindow(){generation++;if(pending!=null&&observer.isAlive())observer.removeOnPreDrawListener(pending);pending=null;panel.animate().cancel();shade.animate().cancel();super.onDetachedFromWindow();}
 }
 private LinearLayout panel(LinearLayout body){
  LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(16),dp(16),dp(16));card.setBackground(bg(Color.WHITE,24));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(16);body.addView(card,lp);return card;
 }
 private Button action(LinearLayout body,String label,Runnable run){return secondaryUi.action(body,label,run);}
 private LinearLayout option(LinearLayout body,int drawable,String title,String description,Runnable run){
  LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(dp(52));row.setPadding(dp(16),dp(10),dp(16),dp(10));boolean grouped="settings-group".equals(body.getTag());if(grouped&&body.getChildCount()>0){View line=new View(this);line.setBackgroundColor(0xffe7e7e7);LinearLayout.LayoutParams divider=new LinearLayout.LayoutParams(-1,dp(1));divider.leftMargin=dp(16);divider.rightMargin=dp(16);body.addView(line,divider);}row.setBackground(bg(Color.WHITE,grouped?0:22));LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,-2);rowParams.bottomMargin=grouped?0:dp(10);body.addView(row,rowParams);
  ImageView symbol=new ImageView(this);symbol.setImageResource(drawable);symbol.setImageTintList(android.content.res.ColorStateList.valueOf(0xff171717));symbol.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);LinearLayout.LayoutParams glyph=new LinearLayout.LayoutParams(dp(20),dp(20));glyph.rightMargin=dp(14);row.addView(symbol,glyph);
  LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);TextView heading=text(title,15,0xff171717);copy.addView(heading);if(!description.isEmpty()){TextView note=text(description,12,0xff707070);note.setPadding(0,dp(3),0,0);copy.addView(note);}row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));TextView arrow=text("›",20,0xffaaaaaa);arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);arrow.setPadding(dp(12),0,0,0);row.addView(arrow);
  row.setFocusable(true);row.setContentDescription(description.isEmpty()?title:title+"，"+description);row.setOnClickListener(v->run.run());ripple(row);return row;
 }
 private void weekPicker(){weekPicker(()->home());}
 private void weekPicker(Runnable back){
  LinearLayout body=bottomSheet("选择教学周","当前教学周：第 "+currentWeek()+" 周 · 点选即可查看",back);
  for(int start=1;start<=30;start+=5){LinearLayout row=new LinearLayout(this);LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,-2);rowParams.bottomMargin=dp(8);body.addView(row,rowParams);for(int w=start;w<start+5;w++){final int chosen=w;TextView cell=text(""+w,16,NAVY);cell.setTag("week-choice-"+w);cell.setContentDescription("查看第 "+w+" 周"+(w==currentWeek()?"，当前教学周":""));cell.setGravity(Gravity.CENTER);cell.setMinHeight(dp(48));cell.setSelected(w==week);cell.setTypeface(null,w==week?Typeface.BOLD:Typeface.NORMAL);cell.setBackground(bg(w==week?AQUA:SURFACE,16));cell.setFocusable(true);cell.setOnClickListener(v->changeWeek(chosen));ripple(cell);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),1);if(w>start)lp.leftMargin=dp(8);row.addView(cell,lp);}}
  action(body,"回到本周",()->changeWeek(currentWeek())).setTag("week-current");
  root.findViewWithTag("sheet-action-button").setVisibility(View.VISIBLE);root.findViewWithTag("sheet-action-button").setOnClickListener(v->{NavigationOverlay menu=sheetMenu("教学周操作");for(int w=1;w<=30;w++){final int chosen=w;menuItem(menu,"第 "+w+" 周"+(w==week?" ✓":""),"week-"+w,()->changeWeek(chosen));}});
 }
 private void home(){
  settingsPage=null;
  getWindow().setStatusBarColor(SURFACE);getWindow().setNavigationBarColor(SURFACE);
  boolean fromTasks=tasksVisible;tasksVisible=false;
  if(homePage!=null&&(pageBack!=null||fromTasks)&&navigationOverlay==null&&homePage.matches()){
   HomePage page=homePage;page.view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);base(false,false,page.view);
   pager=page.pages;timetableScroll=page.scroll;daysBar=page.days;weekLabel=page.label;returnWeekButton=page.button;
   pager.setCurrentItem(week-1,false);updateHeader();timetableScroll.scrollTo(0,timetableScrollY);return;
  }
  base(false);LinearLayout header=new LinearLayout(this);header.setTag("home-header");header.setOrientation(LinearLayout.VERTICAL);root.addView(header);LinearLayout toolbar=new LinearLayout(this);toolbar.setGravity(Gravity.CENTER_VERTICAL);toolbar.setPadding(dp(16),dp(10),dp(8),dp(6));header.addView(toolbar);
  LinearLayout dates=new LinearLayout(this);dates.setOrientation(LinearLayout.VERTICAL);Calendar today=date();TextView title=text(today.get(Calendar.YEAR)+"/"+(today.get(Calendar.MONTH)+1)+"/"+today.get(Calendar.DAY_OF_MONTH),25,INK);title.setTypeface(null,Typeface.BOLD);dates.addView(title);TextView sub=text("第 "+week+" 周  当前为第 "+currentWeek()+" 周",14,INK);weekLabel=sub;LinearLayout.LayoutParams subp=new LinearLayout.LayoutParams(-1,-2);subp.topMargin=dp(4);dates.addView(sub,subp);dates.setTag("home-date");toolbar.addView(dates,new LinearLayout.LayoutParams(0,-2,1));
  returnWeekButton=icon("↶","回到本周",()->changeWeek(currentWeek()));returnWeekButton.setTag("return-current-week");returnWeekButton.setVisibility(Math.abs(week-currentWeek())>=2?View.VISIBLE:View.GONE);toolbar.addView(returnWeekButton,new LinearLayout.LayoutParams(dp(48),dp(48)));
  TextView more=icon("⋮","更多操作",()->quickMenu());more.setTag("quick-menu-button");toolbar.addView(more,new LinearLayout.LayoutParams(dp(48),dp(48)));
  daysBar=new FrameLayout(this);daysBar.setTag("week-days-viewport");root.addView(daysBar,new LinearLayout.LayoutParams(-1,dp(55)));
  TimetableScrollView scroll=new TimetableScrollView(this);timetableScroll=scroll;scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(false);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);
   pager=scroll.new WeekPager(this);pager.setOffscreenPageLimit(1);pager.setAdapter(new PagerAdapter(){public int getCount(){return 30;}public boolean isViewFromObject(View v,Object o){return v==o;}public Object instantiateItem(ViewGroup container,int position){WeekGrid grid=new WeekGrid();grid.shownWeek=position+1;grid.setTag("week-grid-"+grid.shownWeek);grid.days=weekDays(grid.shownWeek);daysBar.addView(grid.days,new FrameLayout.LayoutParams(-1,dp(55)));daysBar.post(daysBar::requestLayout);grid.addOnLayoutChangeListener((v,left,top,right,bottom,oldLeft,oldTop,oldRight,oldBottom)->syncWeekDays());container.addView(grid);syncWeekDays();return grid;}public void destroyItem(ViewGroup container,int position,Object object){daysBar.removeView(((WeekGrid)object).days);container.removeView((View)object);}});pager.setCurrentItem(week-1,false);pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){public void onPageScrolled(int position,float offset,int pixels){syncWeekDays();}public void onPageSelected(int position){week=position+1;saveWeek();updateHeader();}public void onPageScrollStateChanged(int state){pagerState=state;scheduleRemoteNotice();}});content.addView(pager,new LinearLayout.LayoutParams(-1,dp(12*66)));
  if(!notes.isEmpty()){TextView note=text(notes,12,MUTED);note.setPadding(dp(16),dp(16),dp(16),dp(24));content.addView(note);}
  final int restoreY=timetableScrollY;scroll.addOnLayoutChangeListener(new View.OnLayoutChangeListener(){public void onLayoutChange(View v,int l,int t,int r,int b,int oldL,int oldT,int oldR,int oldB){scroll.removeOnLayoutChangeListener(this);scroll.scrollTo(0,restoreY);}});
  homePage=new HomePage();
 }
 @Override protected void onResume(){super.onResume();remoteResumed=true;scheduleRemoteNotice();if(pager!=null&&pager.isAttachedToWindow())updateHeader();Context context=getApplicationContext();worker.execute(()->CourseWidget.updateAll(context));getWindow().getDecorView().postDelayed(()->checkWidgetPin(),250);}
 @Override protected void onPause(){remoteResumed=false;touching=false;remoteHandler.removeCallbacks(remoteNotice);super.onPause();}
 @Override protected void onStop(){if(remoteDialog!=null)remoteDialog.dismiss();super.onStop();}
 @Override public boolean dispatchTouchEvent(MotionEvent event){if(event.getActionMasked()==MotionEvent.ACTION_DOWN)touching=true;interactionAt=SystemClock.uptimeMillis();try{return super.dispatchTouchEvent(event);}finally{if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL){touching=false;scheduleRemoteNotice();}}}
 private void updateHeader(){Calendar today=date();((TextView)((LinearLayout)weekLabel.getParent()).getChildAt(0)).setText(String.format(Locale.ROOT,"%d/%d/%d",today.get(Calendar.YEAR),today.get(Calendar.MONTH)+1,today.get(Calendar.DAY_OF_MONTH)));returnWeekButton.setVisibility(Math.abs(week-currentWeek())>=2?View.VISIBLE:View.GONE);weekLabel.setText("第 "+week+" 周  当前为第 "+currentWeek()+" 周");for(int i=0;i<pager.getChildCount();i++)if(pager.getChildAt(i) instanceof WeekGrid){WeekGrid grid=(WeekGrid)pager.getChildAt(i);updateDays(grid.days,grid.shownWeek);}syncWeekDays();}
 private LinearLayout weekDays(int shownWeek){
  LinearLayout days=new LinearLayout(this);days.setTag("week-days-"+shownWeek);days.setPadding(0,dp(3),0,dp(10));
  for(int i=0;i<8;i++){TextView cell=text("",i==0?14:12,i==0?INK:MUTED);cell.setGravity(Gravity.CENTER);if(i==0)cell.setTypeface(null,Typeface.BOLD);else cell.setLineSpacing(dp(5),1);days.addView(cell,i==0?new LinearLayout.LayoutParams(dp(34),dp(42)):new LinearLayout.LayoutParams(0,dp(42),1));}
  updateDays(days,shownWeek);return days;
 }
 private void updateDays(LinearLayout days,int shownWeek){Calendar start=monday(),today=date();start.add(Calendar.DATE,(shownWeek-1)*7);((TextView)days.getChildAt(0)).setText((start.get(Calendar.MONTH)+1)+"\n月");String[] labels={"一","二","三","四","五","六","日"};for(int i=0;i<7;i++){TextView d=(TextView)days.getChildAt(i+1);d.setText(labels[i]+"\n"+(start.get(Calendar.MONTH)+1)+"/"+start.get(Calendar.DAY_OF_MONTH));boolean isToday=start.get(Calendar.YEAR)==today.get(Calendar.YEAR)&&start.get(Calendar.DAY_OF_YEAR)==today.get(Calendar.DAY_OF_YEAR);d.setTextColor(isToday?BLUE:MUTED);d.setTypeface(null,isToday?Typeface.BOLD:Typeface.NORMAL);start.add(Calendar.DATE,1);}}
 private void syncWeekDays(){if(pager==null)return;for(int i=0;i<pager.getChildCount();i++)if(pager.getChildAt(i) instanceof WeekGrid){WeekGrid grid=(WeekGrid)pager.getChildAt(i);grid.days.setTranslationX(grid.getLeft()-pager.getScrollX());}}
 private void buildGrid(WeekGrid grid,int shownWeek){grid.shownWeek=shownWeek;int week=shownWeek;grid.removeAllViews();List<Course> courses;try{courses=effectiveCourses();}catch(Exception e){toast("课表或调整记录格式异常，请检查课程与调整记录。");return;}int row=dp(66),axis=dp(34),width=grid.contentWidth>0?grid.contentWidth:grid.getWidth();float column=(width-axis)/7f;grid.contentWidth=width;
  String[] begin=START_TIMES,end=END_TIMES;
  for(int i=0;i<12;i++){LinearLayout label=new LinearLayout(this);label.setOrientation(LinearLayout.VERTICAL);label.setGravity(Gravity.CENTER_HORIZONTAL);TextView n=text(""+(i+1),14,INK);label.addView(n);TextView time=text(begin[i]+"\n"+end[i],10,INK);time.setGravity(Gravity.CENTER);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=dp(4);label.addView(time,tp);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(axis,row);lp.topMargin=i*row+dp(8);grid.addView(label,lp);}
  List<CourseDisplay.Group> groups=CourseDisplay.groups(courses,week,displayChoices);
  if(groups.isEmpty()){LinearLayout empty=new LinearLayout(this);empty.setOrientation(LinearLayout.VERTICAL);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(16),0,dp(16),0);TextView msg=text(data.trim().isEmpty()?"还没有导入课表":"行到水穷处，坐看云起时",data.trim().isEmpty()?17:21,0xff435776);msg.setTag("timetable-empty-message");msg.setGravity(Gravity.CENTER);msg.setMinHeight(row);if(!data.trim().isEmpty()){msg.setTypeface(Typeface.create("serif",Typeface.NORMAL));msg.setLetterSpacing(.04f);msg.setLineSpacing(dp(4),1);}empty.addView(msg);if(data.trim().isEmpty()){TextView link=text("点击右上角菜单中的学校登录，登录学校账号后导入课表",14,0xff526d9c);link.setTag("timetable-import");link.setGravity(Gravity.CENTER);link.setLineSpacing(dp(4),1);link.setPadding(0,dp(12),0,0);link.setMinHeight(dp(48));link.setFocusable(true);link.setOnClickListener(v->quickMenu());empty.addView(link);}FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(width-axis,-2);lp.leftMargin=axis;lp.topMargin=3*row;grid.addView(empty,lp);return;}
  for(CourseDisplay.Group group:groups){Course c=group.selected;String content=c.displayDetails();boolean conflict=group.courses.size()>1,adjusted=movedAdjustment(c,week)!=null;
   String cardText=cachedCardText.computeIfAbsent(c,key->SchoolParser.clean(c.name)+"\n@"+SchoolParser.clean(c.room)+(c.teacher.isEmpty()?"":"\n"+SchoolParser.clean(c.teacher))+(content.isEmpty()?"":"\n"+SchoolParser.clean(content)));
   TextView card=text(cardText,12,Color.WHITE);card.setTypeface(null,Typeface.BOLD);card.setLineSpacing(dp(1),1);card.setPadding(dp(3),dp(4),dp(2),dp(3));card.setEllipsize(TextUtils.TruncateAt.END);card.setMaxLines(Math.max(2,(c.end-c.start+1)*4-(conflict||adjusted?1:0)));
   View tile=card;
   if(conflict||adjusted){LinearLayout stack=new LinearLayout(this);stack.setOrientation(LinearLayout.VERTICAL);TextView badge=text(conflict?(adjusted?"冲突 · 调课":"冲突"):"调课",10,courseColor(c.name));badge.setTag(conflict?"conflict-badge":"adjustment-badge");badge.setTypeface(null,Typeface.BOLD);badge.setPadding(dp(3),dp(1),dp(3),dp(1));badge.setBackground(bg(Color.WHITE,3));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-2,-2);bp.setMargins(dp(3),dp(4),dp(3),0);stack.addView(badge,bp);stack.addView(card,new LinearLayout.LayoutParams(-1,0,1));tile=stack;}
   GradientDrawable background=bg(courseColor(c.name),5);background.setStroke(dp(1.2f),0xfff0edf7);tile.setBackground(background);ripple(tile);tile.setContentDescription(c.name+"，星期"+c.day+"，第"+c.start+"到"+c.end+"节，"+c.room+"，"+c.teacher+(content.isEmpty()?"":"，"+content)+(adjusted?"，单次调课":"")+(conflict?"，有冲突，共 "+group.courses.size()+" 门，点击更换显示课程":""));tile.setFocusable(true);tile.setTag("course-card");tile.setOnClickListener(v->{if(conflict)conflictPicker(group,week);else detail(c,week,()->home());});
   FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(Math.max(dp(14),(int)column-dp(3)),(c.end-c.start+1)*row-dp(4));lp.leftMargin=axis+Math.round((c.day-1)*column)+dp(1);lp.topMargin=(c.start-1)*row+dp(2);grid.addView(tile,lp);
  }
 }
 private void conflictPicker(CourseDisplay.Group group,int shownWeek){conflictPicker(group,shownWeek,group.courses.indexOf(group.selected));}
 private void conflictPicker(CourseDisplay.Group group,int shownWeek,int selected){
  String baseline=data,adjustmentBaseline=adjustments;int[] choice={selected};Course c=group.courses.get(selected);
  detail(c,shownWeek,()->home());NavigationOverlay sheet=navigationOverlay;sheet.panel.setTag("冲突课程");
  Runnable reopen=()->conflictPicker(group,shownWeek,choice[0]),conflictDetails=()->conflictComparison(group,shownWeek,choice[0]);detailActions(c,shownWeek,reopen,conflictDetails);
  java.util.function.IntConsumer pick=index->{
   if(!data.equals(baseline)||!adjustments.equals(adjustmentBaseline)){toast("课表已更新，请重新选择");home();return;}
   Course next=group.courses.get(index);choice[0]=index;CourseDisplay.prefer(next,displayChoices);getPreferences(0).edit().putStringSet("displayChoices",new HashSet<>(displayChoices)).apply();CourseWidget.updateAll(this);
   if(pager!=null&&pager.isAttachedToWindow())for(int i=0;i<pager.getChildCount();i++)if(pager.getChildAt(i) instanceof WeekGrid){WeekGrid grid=(WeekGrid)pager.getChildAt(i);buildGrid(grid,grid.shownWeek);}
   sheet.body.removeAllViews();courseInformation(sheet.body,next,shownWeek);detailActions(next,shownWeek,reopen,conflictDetails);
  };
  TextView change=icon("⇄","切换冲突课程；点击循环切换",()->pick.accept((choice[0]+1)%group.courses.size()));change.setTag("conflict-switch");change.setTextSize(24);change.setBackground(bg(0xffeaf0fa,24));
  LinearLayout bar=(LinearLayout)sheet.panel.findViewWithTag("sheet-action-button").getParent();LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(48),dp(48));lp.rightMargin=dp(8);bar.addView(change,bar.getChildCount()-1,lp);
 }
 private void conflictComparison(CourseDisplay.Group group,int shownWeek,int selected){
  LinearLayout body=bottomSheet("冲突课程对比","",()->conflictPicker(group,shownWeek,selected));
  for(int i=0;i<group.courses.size();i++){Course c=group.courses.get(i);LinearLayout card=panel(body);card.setPadding(dp(12),dp(8),dp(12),dp(8));card.setTag("conflict-comparison-"+i);TextView name=text(SchoolParser.clean(c.name),16,INK);name.setTypeface(null,Typeface.BOLD);card.addView(name);if(i==selected)card.addView(text("当前显示",12,MUTED));
   courseInformationRows(card,c,shownWeek);
  }
 }
 private int courseColor(String name){if(name.contains("数据结构")||name.contains("马克思"))return 0xffd9809a;if(name.contains("模拟电子"))return 0xffdb947f;if(name.contains("电路"))return 0xffa597df;if(name.contains("英语"))return 0xff7d9fc5;if(name.contains("复变"))return 0xffd7b984;if(name.contains("物理")||name.contains("体育")||name.contains("羽毛球"))return 0xff87a6e6;return new int[]{0xff87cabe,0xffc598ba,0xff84a9c8,0xffa29bce}[Math.floorMod(name.hashCode(),4)];}
 private void detail(Course c){detail(c,()->home());}
 private void detail(Course c,Runnable back){detail(c,week,back);}
 private void detail(Course c,int shownWeek,Runnable back){
  LinearLayout body=bottomSheet("课程详情","",back);showingDetail=true;
  courseInformation(body,c,shownWeek);detailActions(c,shownWeek,()->detail(c,shownWeek,back),null);
 }
 private void courseInformation(LinearLayout body,Course c,int shownWeek){
  TextView title=root.findViewWithTag("navigation-title");title.setText(SchoolParser.clean(c.name));title.setTextSize(18);title.setTypeface(null,Typeface.BOLD);
  courseInformationRows(body,c,shownWeek);
 }
 private void courseInformationRows(LinearLayout body,Course c,int shownWeek){
  String content=c.displayDetails();CourseAdjustment moved=movedAdjustment(c,shownWeek);
  TextView when=text("第 "+shownWeek+" 周 · 周"+"一二三四五六日".charAt(c.day-1)+" · 第 "+c.start+"–"+c.end+" 节（"+START_TIMES[c.start-1]+"–"+END_TIMES[c.end-1]+"）",12,MUTED);when.setPadding(0,0,0,dp(8));body.addView(when);
  courseInfoRow(body,R.drawable.ic_calendar,"教学周次",prettyWeeks(c)+(c.active(shownWeek)?"":" · 本周无课"));
  courseInfoRow(body,R.drawable.ic_person,"任课老师",SchoolParser.clean(c.teacher));
  courseInfoRow(body,R.drawable.ic_location,"上课教室",SchoolParser.clean(c.room));
  if(!content.isEmpty())courseInfoRow(body,R.drawable.ic_notes,"实验内容",SchoolParser.clean(content));
  if(moved!=null)courseInfoRow(body,R.drawable.ic_calendar,"单次调课",arrangement(moved.original,moved.week)+" → "+arrangement(c,shownWeek));
 }
 private void courseInfoRow(LinearLayout body,int drawable,String label,String value){
  if(body.getTag()!=null&&body.getTag().toString().startsWith("conflict-comparison-")){sheetRow(body,label,value);return;}
  View divider=new View(this);divider.setBackgroundColor(0xffe8ebf0);body.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
  LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(10),0,dp(10));
  ImageView symbol=new ImageView(this);symbol.setImageResource(drawable);symbol.setImageTintList(android.content.res.ColorStateList.valueOf(0xff171717));symbol.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);LinearLayout.LayoutParams iconBounds=new LinearLayout.LayoutParams(dp(22),dp(22));iconBounds.rightMargin=dp(12);symbol.setTag("course-info-icon");row.addView(symbol,iconBounds);
  LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);copy.addView(text(label,11,MUTED));TextView valueView=text(value,14,INK);valueView.setPadding(0,dp(2),0,0);valueView.setContentDescription(label+"，"+value);copy.addView(valueView);row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));body.addView(row);
 }
 private void detailActions(Course c,int shownWeek,Runnable reopen,Runnable conflictDetails){
  CourseAdjustment moved=movedAdjustment(c,shownWeek);Course source=moved==null?originalCourse(c,shownWeek):moved.original;int sourceWeek=moved==null?shownWeek:moved.week;
  root.findViewWithTag("sheet-action-button").setVisibility(View.VISIBLE);root.findViewWithTag("sheet-action-button").setOnClickListener(v->{NavigationOverlay actions=sheetMenu("课程操作");
  if(conflictDetails!=null)menuItem(actions,"冲突详情","conflict-course-details",conflictDetails);
  if(source!=null&&source.active(sourceWeek)){menuItem(actions,"本次调课","adjust-course",()->adjustmentForm(source,sourceWeek,false,reopen));menuItem(actions,"本次停课","cancel-course",()->adjustmentForm(source,sourceWeek,true,reopen));}
  menuItem(actions,"校对课程","edit-course",()->{List<Course> courses=Course.parse(data);int index=-1;for(int i=0;i<courses.size();i++)if(source!=null&&courses.get(i).encode().equals(source.encode())){index=i;break;}if(index<0){toast("课程已更新，请重新打开详情");home();return;}courseForm(source,index,reopen);});
  });
 }
 private void sheetRow(LinearLayout card,String label,String value){
  if(card.getChildCount()>0){View divider=new View(this);divider.setBackgroundColor(0xffedf0f6);card.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));}
  LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(7),0,dp(7));row.addView(text(label,12,MUTED),new LinearLayout.LayoutParams(dp(92),-2));TextView content=text(value.isEmpty()?"学校暂未提供":value,14,INK);row.addView(content,new LinearLayout.LayoutParams(0,-2,1));card.addView(row);
 }
 private String prettyWeeks(Course c){StringBuilder out=new StringBuilder();for(int w=1;w<=30;w++){if(!c.active(w))continue;int first=w;while(w<30&&c.active(w+1))w++;if(out.length()>0)out.append("、");out.append(first);if(w>first)out.append("–").append(w);}return "第 "+out+" 周";}
 private void detailRow(LinearLayout card,String label,String value){if(card.getChildCount()>0){View divider=new View(this);divider.setBackgroundColor(0xffedf0f6);card.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));}LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(0,dp(9),0,dp(9));row.addView(text(label,12,0xff56677e));TextView content=text(value.isEmpty()?"学校暂未提供":value,16,INK);content.setTextIsSelectable(true);content.setLineSpacing(dp(2),1);content.setPadding(0,dp(3),0,0);row.addView(content);card.addView(row);}
 private LinearLayout settingsGroup(LinearLayout body,String title){
  TextView label=text(title,12,0xff707070);label.setPadding(dp(4),dp(4),0,dp(6));body.addView(label);
  LinearLayout group=new LinearLayout(this);group.setOrientation(LinearLayout.VERTICAL);group.setTag("settings-group");group.setBackground(bg(Color.WHITE,18));group.setClipToOutline(true);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);body.addView(group,lp);return group;
 }
 private void menu(){
  if(returningPage&&settingsPage!=null){
   settingsPage.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);base(true,false,settingsPage);
   root.setBackgroundColor(SURFACE);getWindow().setStatusBarColor(SURFACE);getWindow().setNavigationBarColor(SURFACE);
   pageBack=()->home();updateSystemBack();remoteStatus=root.findViewWithTag("update-status");refreshRemoteStatus();
   ((ScrollView)root.getChildAt(1)).scrollTo(0,settingsScrollY);return;
  }
  LinearLayout body=secondary("课表设置","",()->home());
  LinearLayout semester=settingsGroup(body,"学期与周次");
  option(semester,R.drawable.ic_calendar,"选择教学周","当前查看第 "+week+" 周",()->weekPicker(()->menu())).setTag("select-teaching-week");
  option(semester,R.drawable.ic_calendar,"开学日期",term,()->termPicker()).setTag("settings-term");
  LinearLayout courses=settingsGroup(body,"课程与桌面");
  option(courses,R.drawable.ic_notes,"课程管理","",()->edit(()->menu()));
  option(courses,R.drawable.ic_calendar,"单次调课 / 停课","",()->adjustmentManager()).setTag("adjustment-manager");
  option(courses,R.drawable.ic_grid,"桌面小组件","",()->widgetAddPage()).setTag("add-home-widget");
  LinearLayout software=settingsGroup(body,"软件与下载");
  LinearLayout update=option(software,R.drawable.ic_sync,"检查更新","当前版本 "+appVersion(),()->checkRemoteUpdates());update.setTag("check-app-update");
  remoteStatus=text("",13,MUTED);remoteStatus.setTag("update-status");remoteStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);remoteStatus.setPadding(dp(4),0,dp(4),dp(8));body.addView(remoteStatus);refreshRemoteStatus();
  option(software,R.drawable.ic_notes,"关于软件与下载","",this::about).setTag("about-app");
  LinearLayout caution=settingsGroup(body,"本地数据");TextView clear=text("退出并清空课表",15,0xffc52c47);clear.setTag("clear-timetable");clear.setGravity(Gravity.CENTER_VERTICAL);clear.setMinHeight(dp(52));clear.setPadding(dp(16),dp(10),dp(16),dp(10));clear.setFocusable(true);clear.setOnClickListener(v->confirmClear());ripple(clear);caution.addView(clear,new LinearLayout.LayoutParams(-1,-2));
  settingsPage=root;
 }

 private String appVersion(){try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(android.content.pm.PackageManager.NameNotFoundException e){throw new IllegalStateException(e);}}
 private long appVersionCode(){try{android.content.pm.PackageInfo info=getPackageManager().getPackageInfo(getPackageName(),0);return Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;}catch(android.content.pm.PackageManager.NameNotFoundException e){throw new IllegalStateException(e);}}
 private void about(){
  LinearLayout body=secondary("关于软件与下载","",this::menu);
  LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.VERTICAL);brand.setGravity(Gravity.CENTER);brand.setPadding(0,dp(24),0,dp(32));ImageView logo=new ImageView(this);logo.setImageResource(R.mipmap.ic_launcher);logo.setContentDescription("课表镜 App 图标");logo.setTag("about-app-icon");brand.addView(logo,new LinearLayout.LayoutParams(dp(80),dp(80)));TextView name=text("课表镜 CourseLens",23,NAVY);name.setTypeface(null,Typeface.BOLD);name.setPadding(0,dp(20),0,dp(10));brand.addView(name);brand.addView(text("版本 "+appVersion(),14,MUTED));body.addView(brand,new LinearLayout.LayoutParams(-1,-2));
  LinearLayout links=settingsGroup(body,"下载与项目");option(links,R.drawable.ic_notes,"网盘下载","通过系统浏览器打开下载页面",()->{String url=remote==null||remote.config==null?"":remote.config.diskUrl;openRemoteLink(url);}).setTag("disk-download");option(links,R.drawable.ic_notes,"GitHub 仓库","公开版本与使用说明",()->openRemoteLink(RemoteConfig.REPOSITORY)).setTag("public-repository");
  remoteStatus=text("",13,MUTED);remoteStatus.setTag("update-status");remoteStatus.setGravity(Gravity.CENTER);body.addView(remoteStatus);refreshRemoteStatus();
 }

 private void openRemoteLink(String url){if(url==null||url.isEmpty()){toast("暂未配置下载地址，请检查更新后重试");return;}if(!RemoteConfig.validLink(url)){toast("下载地址无效，请稍后重试");return;}try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));}catch(ActivityNotFoundException|SecurityException e){toast("没有可打开此链接的浏览器");}}
 private void checkRemoteUpdates(){if(remote==null){remote=RemoteConfigClient.get(this);remote.listen(remoteListener);}manualCheckScreen=screen;remote.check(true);refreshRemoteStatus();}
 private void refreshRemoteStatus(){if(remoteStatus==null||!remoteStatus.isAttachedToWindow())return;String status=remote==null||!remote.loaded?"尚未检查更新":remote.checking?"正在检查更新…":remote.config==null?"暂无可用更新信息":remote.config.newerThan(appVersionCode())?"发现新版本 "+remote.config.versionName:"当前版本 "+appVersion();if(remote!=null&&remote.config!=null){String time=new java.text.SimpleDateFormat("MM-dd HH:mm",Locale.CHINA).format(new Date(remote.validatedAt));status+=" · 缓存于 "+time;if(!remote.fresh(System.currentTimeMillis()))status+="（已过期）";}remoteStatus.setText(status);remoteStatus.setTextColor(remote!=null&&remote.config!=null&&remote.config.newerThan(appVersionCode())?PRIMARY:MUTED);}
 private void remoteChanged(String error,boolean manual){if(isDestroyed()||isFinishing())return;refreshRemoteStatus();if(manual&&remoteResumed&&screen==manualCheckScreen){if(error!=null)toast(error);else if(remote.config==null)toast("暂无可用更新信息");else if(remote.config.newerThan(appVersionCode()))manualUpdatePending=true;else toast("已是最新版本");}scheduleRemoteNotice();}
 private void scheduleRemoteNotice(){remoteHandler.removeCallbacks(remoteNotice);if(remoteResumed)remoteHandler.postDelayed(remoteNotice,1500);}
 private boolean canShowRemote(){return remoteResumed&&hasWindowFocus()&&!isFinishing()&&!isDestroyed()&&!touching&&SystemClock.uptimeMillis()-interactionAt>=1400&&pagerState==ViewPager.SCROLL_STATE_IDLE&&!pageMotion.isRunning()&&!busy&&navigationOverlay==null&&!showingDetail&&(choiceSheet==null||choiceSheet.getVisibility()!=View.VISIBLE)&&(unsavedDialog==null||!unsavedDialog.isShowing())&&(widgetDialog==null||!widgetDialog.isShowing())&&(remoteDialog==null||!remoteDialog.isShowing());}
 private void showRemoteNotice(){
  if(remote==null||remote.config==null||remote.checking)return;if(pageMotion.isRunning()){scheduleRemoteNotice();return;}if(!canShowRemote())return;RemoteConfig config=remote.config;
  if(manualUpdatePending){manualUpdatePending=false;if(config.newerThan(appVersionCode()))showUpdate(config);return;}
  if(pageBack!=null||!remote.fresh(System.currentTimeMillis()))return;
  String updateKey="update:"+config.versionCode;if(config.newerThan(appVersionCode())&&!remote.snoozedVersions.contains(Long.toString(config.versionCode))&&remote.sessionShown.add(updateKey)){showUpdate(config);return;}
  for(RemoteConfig.Announcement item:config.announcements)if(item.enabled&&!remote.readIds.contains(item.id)&&remote.sessionShown.add("announcement:"+item.id)){showAnnouncement(item);return;}
 }
 private AlertDialog remoteDialog(String title,String content,String date,int icon){
  LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(24),dp(20),dp(24),dp(8));LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);ImageView image=new ImageView(this);image.setImageResource(icon);image.setColorFilter(PRIMARY);image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);heading.addView(image,new LinearLayout.LayoutParams(dp(24),dp(24)));TextView label=text(title,20,INK);label.setTypeface(null,Typeface.BOLD);label.setMaxLines(3);label.setEllipsize(TextUtils.TruncateAt.END);label.setPadding(dp(12),0,0,0);heading.addView(label,new LinearLayout.LayoutParams(0,-2,1));box.addView(heading);
  if(!date.isEmpty()){TextView time=text(date,12,MUTED);time.setPadding(0,dp(10),0,0);box.addView(time);}ScrollView scroll=new ScrollView(this){@Override protected void onMeasure(int width,int height){int max=Math.max(dp(80),(int)(getResources().getDisplayMetrics().heightPixels*.4));super.onMeasure(width,MeasureSpec.makeMeasureSpec(max,MeasureSpec.AT_MOST));}};TextView message=text(content,15,INK);message.setTextIsSelectable(true);message.setLineSpacing(dp(4),1);message.setPadding(0,dp(16),0,dp(8));message.setTag("remote-message");scroll.addView(message);box.addView(scroll,new LinearLayout.LayoutParams(-1,-2));return new AlertDialog.Builder(this,R.style.SecondaryDialog).setView(box).create();
 }
 private void styleDialog(AlertDialog dialog,boolean destructive){secondaryUi.styleDialog(dialog,destructive);}

 private void finishRemoteDialog(AlertDialog dialog){remoteDialog=dialog;dialog.setOnDismissListener(value->{if(remoteDialog==dialog)remoteDialog=null;scheduleRemoteNotice();});dialog.show();styleDialog(dialog,false);dialog.getWindow().setWindowAnimations(android.R.style.Animation_Dialog);}
 private void showAnnouncement(RemoteConfig.Announcement item){AlertDialog dialog=remoteDialog(item.title,item.body,item.publishedAt.isEmpty()?"":item.publishedAt.replace('T',' ').replace("Z"," UTC"),R.drawable.ic_notes);dialog.setButton(AlertDialog.BUTTON_POSITIVE,"我知道了",(d,w)->remote.read(item.id));finishRemoteDialog(dialog);}
 private void showUpdate(RemoteConfig config){remote.sessionShown.add("update:"+config.versionCode);AlertDialog dialog=remoteDialog("发现新版本 "+config.versionName,config.changelog.isEmpty()?"新版课表镜已发布，可前往下载。":config.changelog,"",R.drawable.ic_sync);dialog.setButton(AlertDialog.BUTTON_NEGATIVE,"稍后更新",(d,w)->remote.snooze(config.versionCode));dialog.setButton(AlertDialog.BUTTON_POSITIVE,"立即更新",(d,w)->{remote.snooze(config.versionCode);openRemoteLink(config.downloadUrl());});finishRemoteDialog(dialog);}

 private AlertDialog confirmClear(){if(busy){toast("请等待同步结束");return null;}AlertDialog dialog=new AlertDialog.Builder(this,R.style.SecondaryDialog).setTitle("清空本机课表？").setMessage("课表、备注和单次调整会从这台手机删除。需要再次登录学校才能重新同步。").setNegativeButton("保留课表",null).setPositiveButton("清空",(d,w)->{if(busy){toast("请等待同步结束");return;}school.logout();data="";notes="";adjustments="";getPreferences(0).edit().remove("courseAdjustments").apply();displayChoices.clear();getPreferences(0).edit().remove("displayChoices").apply();save();home();toast("已退出并清空课表。");}).create();dialog.show();styleDialog(dialog,true);return dialog;}
 private void termPicker(){LinearLayout body=secondary("开学日期","选择第 1 周的开始日期，用于计算当前周次。",()->menu());LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setBackground(bg(Color.WHITE,16));card.setClipToOutline(true);card.setPadding(0,0,0,dp(16));LinearLayout.LayoutParams bounds=new LinearLayout.LayoutParams(-2,-2);bounds.gravity=Gravity.CENTER_HORIZONTAL;body.addView(card,bounds);DatePicker picker=new DatePicker(new android.view.ContextThemeWrapper(this,R.style.SecondaryPicker));Calendar c=monday();picker.init(c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH),null);picker.setTag("term-date");card.addView(picker,new LinearLayout.LayoutParams(-2,-2));Button button=action(body,"保存开学日期",()->{picker.clearFocus();term=String.format(Locale.ROOT,"%04d-%02d-%02d",picker.getYear(),picker.getMonth()+1,picker.getDayOfMonth());save();home();});button.setTag("save-term");saveHeader(body,button);}
 private void edit(){edit(()->home());}
 @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused)scheduleRemoteNotice();if(focused&&ADD_WIDGET.equals(getIntent().getAction())){getIntent().setAction(Intent.ACTION_MAIN);getWindow().getDecorView().post(()->pinWidget());}else if(focused)getWindow().getDecorView().postDelayed(()->checkWidgetPin(),250);}
 private void widgetShortcut(){
  android.content.pm.ShortcutManager manager=getSystemService(android.content.pm.ShortcutManager.class);
  manager.addDynamicShortcuts(Collections.singletonList(new android.content.pm.ShortcutInfo.Builder(this,"add-widget").setShortLabel("添加小部件").setIcon(android.graphics.drawable.Icon.createWithResource(this,R.drawable.ic_widget)).setIntent(new Intent(this,MainActivity.class).setAction(ADD_WIDGET)).build()));
 }
 private android.content.SharedPreferences widgetAddPrefs(){return getSharedPreferences("widget_add",MODE_PRIVATE);}
 private boolean isXiaomi(){return Build.MANUFACTURER.equalsIgnoreCase("Xiaomi");}
 private void widgetAddPage(){
  LinearLayout body=secondary("桌面小组件","保留 Android 原生添加方式，也可以从桌面的小组件列表手动添加。",()->menu());
  action(body,"添加到桌面",()->pinWidget()).setTag("pin-home-widget");
  option(body,R.drawable.ic_grid,"手动添加","从桌面选择 2×2、4×2 或 4×4",()->widgetManualGuide());
  if(isXiaomi())option(body,R.drawable.ic_notes,"权限教程","一键添加没有反应时查看",()->widgetPermissionGuide());
 }
 private void pinWidget(){
  if(isXiaomi()&&!widgetAddPrefs().getBoolean("guideSeen",false)){widgetAddPage();widgetPermissionGuide();return;}
  android.appwidget.AppWidgetManager manager=android.appwidget.AppWidgetManager.getInstance(this);
  if(manager.isRequestPinAppWidgetSupported())try{
   String token=UUID.randomUUID().toString();
   widgetAddPrefs().edit().putString("requestToken",token).putInt("pendingCount",CourseWidget.count(this)).putLong("requestedAt",SystemClock.elapsedRealtime()).apply();
   Intent callback=new Intent(this,WidgetPinReceiver.class).setAction("cn.courselens.WIDGET_PINNED").setData(android.net.Uri.parse("courselens-widget:"+token)).putExtra("requestToken",token);
   PendingIntent success=PendingIntent.getBroadcast(this,0,callback,PendingIntent.FLAG_ONE_SHOT|(Build.VERSION.SDK_INT>=31?PendingIntent.FLAG_MUTABLE:0));
   Bundle preview=new Bundle();preview.putParcelable(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_PREVIEW,CourseWidget.views(this,0,java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai"))));
   if(manager.requestPinAppWidget(new ComponentName(this,CourseWidget.class),preview,success)){
    getWindow().getDecorView().postDelayed(()->{if(token.equals(widgetAddPrefs().getString("requestToken","")))checkWidgetPin();},1500);return;
   }
   success.cancel();
  }catch(IllegalArgumentException|IllegalStateException|SecurityException e){/* Launcher did not accept this request. */}
  widgetAddPrefs().edit().remove("pendingCount").apply();widgetGuide();
 }
 private void checkWidgetPin(){
  if(!hasWindowFocus()||isFinishing()||isDestroyed())return;
  android.content.SharedPreferences prefs=widgetAddPrefs();String token=prefs.getString("requestToken","");
  boolean confirmed=!token.isEmpty()&&token.equals(prefs.getString("confirmedToken",""))&&!token.equals(prefs.getString("consumedToken",""));
  int before=prefs.getInt("pendingCount",-1);
  if(confirmed||(before>=0&&CourseWidget.count(this)>before)){
   prefs.edit().remove("pendingCount").putString("consumedToken",token).apply();if(widgetDialog!=null)widgetDialog.dismiss();toast("已添加到手机桌面");return;
  }
  if(before<0)return;
  long remaining=1500-(SystemClock.elapsedRealtime()-prefs.getLong("requestedAt",0));
  if(remaining>0){getWindow().getDecorView().postDelayed(()->checkWidgetPin(),remaining);return;}
  prefs.edit().remove("pendingCount").apply();widgetGuide();
 }
 private String widgetManualPath(){return isXiaomi()?"长按桌面空白处 → 小部件 → 全部应用 → 安卓小部件，找到“课表镜”，选择 2×2、4×2 或 4×4。":"长按桌面空白处，打开小组件列表，找到“课表镜”，选择 2×2、4×2 或 4×4。";}
 private void widgetManualGuide(){
  if(widgetDialog!=null)widgetDialog.dismiss();
  widgetDialog=new AlertDialog.Builder(this,R.style.SecondaryDialog).setTitle("手动添加小组件").setMessage(widgetManualPath()+"\n\n手动添加无需授予创建桌面快捷方式权限。").setPositiveButton("知道了",null).show();styleDialog(widgetDialog,false);
 }
 private void widgetPermissionGuide(){
  widgetAddPrefs().edit().putBoolean("guideSeen",true).apply();if(widgetDialog!=null)widgetDialog.dismiss();
  widgetDialog=new AlertDialog.Builder(this,R.style.SecondaryDialog).setTitle("桌面添加说明").setMessage("部分手机的一键添加可能需要“创建桌面快捷方式”权限。\n\n添加没有反应时，可到应用详情 → 权限管理（或其他权限）开启“创建桌面快捷方式 / 桌面快捷方式”。返回后可再次尝试。\n\n也可不授权，从桌面手动添加。").setPositiveButton("前往设置",(d,w)->widgetPermissionSettings()).setNegativeButton("手动添加",(d,w)->widgetManualGuide()).setNeutralButton("继续添加",(d,w)->pinWidget()).show();styleDialog(widgetDialog,false);
 }
 private void widgetPermissionSettings(){
  toast("权限管理（或其他权限）→ 创建桌面快捷方式；返回后可再次添加");
  // No public shortcut-permission settings action; use the supported application details page.
  try{startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())));}
  catch(ActivityNotFoundException|SecurityException e){toast("无法打开应用详情，请在系统设置中找到课表镜；也可以手动添加小组件");}
 }
 private void widgetGuide(){
  if(widgetDialog!=null)widgetDialog.dismiss();
  if(isXiaomi()&&!widgetAddPrefs().getBoolean("guideSeen",false)){widgetPermissionGuide();return;}
  AlertDialog.Builder guide=new AlertDialog.Builder(this,R.style.SecondaryDialog).setTitle("还未确认添加到桌面").setMessage("未收到添加确认。可能已取消、桌面空间不足或启动器限制了一键添加。"+(isXiaomi()?"\n可在权限教程中查看“创建桌面快捷方式”设置。":"")).setPositiveButton("重新添加",(d,w)->pinWidget()).setNegativeButton("手动添加",(d,w)->widgetManualGuide());
  if(isXiaomi())guide.setNeutralButton("权限教程",(d,w)->{widgetAddPage();widgetPermissionGuide();});
  widgetDialog=guide.show();styleDialog(widgetDialog,false);
 }
 private void edit(Runnable back){
  LinearLayout body=secondary("课程管理","按课程归组，每门课程可包含多个上课时段。",back);action(body,"＋ 添加课程",()->courseForm(null,-1,()->edit(back))).setTag("manage-add-course");
  List<Course> courses=Course.parse(data);Map<String,List<Course>> groups=new LinkedHashMap<>();for(Course c:courses)groups.computeIfAbsent(c.name,key->new ArrayList<>()).add(c);
  if(courses.isEmpty()){TextView empty=text("暂无课程，点击上方按钮开始添加。",14,0xff56677e);empty.setPadding(0,dp(24),0,0);body.addView(empty);}
  for(List<Course> group:groups.values()){Course first=group.get(0);option(body,R.drawable.ic_notes,SchoolParser.clean(first.name)+" · "+group.size()+" 个时段","",()->courseForm(first,courses.indexOf(first),()->edit(back))).setTag("manage-course-"+courses.indexOf(first));}
 }
 private List<Course> effectiveCourses(){
  if(cachedCourses!=null&&data.equals(cachedData)&&adjustments.equals(cachedAdjustments)&&term.equals(cachedTerm))return cachedCourses;
  List<Course> raw=Course.parse(data);List<CourseAdjustment> changes=CourseAdjustment.parse(adjustments);
  List<Course> effective=Collections.unmodifiableList(CourseAdjustment.apply(raw,term,changes));Map<String,CourseAdjustment> moves=new HashMap<>();
  for(CourseAdjustment a:changes)if(a.replacement!=null&&a.applies(raw,term))for(int w=1;w<=30;w++)if(a.replacement.active(w))moves.putIfAbsent(CourseDisplay.key(w,a.replacement),a);
  cachedData=data;cachedAdjustments=adjustments;cachedTerm=term;cachedMoves=moves;cachedCardText.clear();cachedCourses=effective;return effective;
 }
 private CourseAdjustment movedAdjustment(Course course,int shownWeek){
  effectiveCourses();return cachedMoves.get(CourseDisplay.key(shownWeek,course));
 }
 private Course originalCourse(Course course,int shownWeek){for(Course c:Course.parse(data))if(c.encode().equals(course.encode())||(c.active(shownWeek)&&CourseDisplay.key(shownWeek,c).equals(CourseDisplay.key(shownWeek,course))))return c;return null;}
 private String arrangement(Course c,int shownWeek){return "第 "+shownWeek+" 周 · 星期"+"一二三四五六日".charAt(c.day-1)+" · 第 "+c.start+"–"+c.end+" 节 · "+(c.room.isEmpty()?"地点未填写":SchoolParser.clean(c.room));}
 private void storeAdjustment(CourseAdjustment next){
  List<CourseAdjustment> rows=CourseAdjustment.parse(adjustments);rows.removeIf(a->a.key().equals(next.key()));rows.add(next);List<String> encoded=new ArrayList<>();for(CourseAdjustment a:rows)encoded.add(a.encode());
  String value=String.join("\n",encoded);if(!getPreferences(0).edit().putString("courseAdjustments",value).commit())throw new IllegalStateException("本机保存失败，请重试");adjustments=value;CourseWidget.updateAll(this);
 }
 private void adjustmentForm(Course original,int sourceWeek,boolean cancel,Runnable back){
  String baseline=data,adjustmentBaseline=adjustments,termBaseline=term;
  LinearLayout body=secondary(cancel?"本次停课":"本次调课","仅调整下面这一次课程，其他教学周和原始课表保留。重新同步后，原课程未改变的调整继续生效。",back);
  LinearLayout card=panel(body);card.addView(text(SchoolParser.clean(original.name),20,INK));detailRow(card,"原安排",arrangement(original,sourceWeek));
  CourseAdjustment previous=null;for(CourseAdjustment a:CourseAdjustment.parse(adjustments))if(a.key().equals(term+":"+CourseDisplay.key(sourceWeek,original)))previous=a;
  Course initial=previous!=null&&previous.replacement!=null?previous.replacement:original;
  Spinner targetWeek=null,day=null,start=null,end=null;EditText room=null;
  if(!cancel){
   String[] weeks=new String[30];for(int i=0;i<30;i++)weeks[i]="第 "+(i+1)+" 周";
   targetWeek=courseChoice(card,"目标教学周",weeks,previous!=null&&previous.replacement!=null?Integer.parseInt(initial.weeks)-1:sourceWeek-1,"adjustment-week");
   if(initial!=null&&initial.teacher.isEmpty()&&!initial.room.isEmpty()){TextView legacy=text("旧课表的老师可能包含在地点信息中，可在此手动分开填写。",12,0xff56677e);legacy.setPadding(0,dp(12),0,0);card.addView(legacy);}
   day=courseChoice(card,"星期",new String[]{"星期一","星期二","星期三","星期四","星期五","星期六","星期日"},initial.day-1,"adjustment-day");String[] units=new String[12];for(int i=0;i<12;i++)units[i]="第 "+(i+1)+" 节";
   start=courseChoice(card,"开始节次",units,initial.start-1,"adjustment-start");end=courseChoice(card,"结束节次",units,initial.end-1,"adjustment-end");room=courseField(card,"上课教室",SchoolParser.clean(initial.room),"教室 / 线上（选填）","adjustment-room");
  }else detailRow(card,"停课范围","仅取消原安排这一次；可在设置的调整记录中恢复。");
  Spinner chosenWeek=targetWeek,chosenDay=day,chosenStart=start,chosenEnd=end;EditText chosenRoom=room;
  TextView error=text("",13,0xff9a3849);error.setTag("adjustment-error");error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);body.addView(error);
  Button submit=action(body,cancel?"确认本次停课":"保存本次调课",()->{
   if(!data.equals(baseline)||!adjustments.equals(adjustmentBaseline)||!term.equals(termBaseline)){error.setText("课表或调整记录已更新，请返回后重新打开");return;}
   try{
    Course replacement=cancel?null:new Course(chosenDay.getSelectedItemPosition()+1,chosenStart.getSelectedItemPosition()+1,chosenEnd.getSelectedItemPosition()+1,original.name,SchoolParser.clean(chosenRoom.getText().toString()),""+(chosenWeek.getSelectedItemPosition()+1),original.teacher,original.details,original.className);
    CourseAdjustment next=new CourseAdjustment(term,sourceWeek,original,replacement);if(!next.applies(Course.parse(data),term))throw new IllegalArgumentException("原课程已更新，请重新打开");storeAdjustment(next);
    ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);home();toast(cancel?"本次课程已停课":"本次课程已调课");
   }catch(Exception e){error.setText("未保存："+e.getMessage());}
  });submit.setTag("save-adjustment");if(cancel){body.removeView(submit);LinearLayout.LayoutParams footer=new LinearLayout.LayoutParams(-2,-2);footer.gravity=Gravity.CENTER_HORIZONTAL;footer.setMargins(dp(20),dp(8),dp(20),dp(16));root.addView(submit,footer);}else saveHeader(body,submit);
  if(!cancel){choiceSheet=new CourseChoiceSheet();((FrameLayout)root.getParent()).addView(choiceSheet,new FrameLayout.LayoutParams(-1,-1));}
 }
 private void adjustmentManager(){
  LinearLayout body=secondary("单次调课 / 停课","调整仅保存在本机。恢复后按原课表显示；原课程或开学日期变化的记录不生效。",()->menu());List<CourseAdjustment> rows;
  try{rows=CourseAdjustment.parse(adjustments);}catch(Exception e){body.addView(text("调整记录格式异常，原记录保留，请先修复数据。",14,0xff9a3849));return;}
  if(rows.isEmpty()){body.addView(text("暂无单次调整。点击课表中的课程，在详情中选择调课或停课。",14,0xff56677e));return;}
  List<Course> courses=Course.parse(data);
  for(int i=0;i<rows.size();i++){CourseAdjustment a=rows.get(i);boolean valid=a.applies(courses,term);LinearLayout card=panel(body);card.setTag("adjustment-record-"+i);card.addView(text(SchoolParser.clean(a.original.name)+" · "+(a.replacement==null?"停课":"调课"),18,INK));detailRow(card,"原安排",arrangement(a.original,a.week));if(a.replacement!=null)detailRow(card,"调至",arrangement(a.replacement,Integer.parseInt(a.replacement.weeks)));if(!valid)detailRow(card,"状态","原课程或学期已变更，本记录不生效");
   if(valid)action(card,"修改为调课",()->adjustmentForm(a.original,a.week,false,()->adjustmentManager())).setTag("edit-adjustment-"+i);
   String snapshot=adjustments;action(card,valid?"恢复原安排":"移除失效记录",()->{
    if(!adjustments.equals(snapshot)){toast("调整记录已更新，请重新打开");adjustmentManager();return;}
    List<String> remaining=new ArrayList<>();for(CourseAdjustment other:rows)if(!other.key().equals(a.key()))remaining.add(other.encode());String value=String.join("\n",remaining);if(!getPreferences(0).edit().putString("courseAdjustments",value).commit()){toast("本机保存失败，请重试");return;}adjustments=value;CourseWidget.updateAll(this);home();toast(valid?"已恢复原安排":"已移除失效记录");
   }).setTag("restore-adjustment-"+i);
  }
 }
 private void formValues(View view,List<Object> values){
  if(view instanceof DatePicker){DatePicker date=(DatePicker)view;values.add(date.getYear()+"-"+date.getMonth()+"-"+date.getDayOfMonth());}
  if(view instanceof EditText)values.add(((EditText)view).getText().toString());
  else if(view instanceof Spinner)values.add(((Spinner)view).getSelectedItemPosition());
  else if(view instanceof CompoundButton)values.add(((CompoundButton)view).isChecked());
  if(view.getTag()!=null&&view.getTag().toString().startsWith("course-slot-"))values.add(view.getTag());
  if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)formValues(group.getChildAt(i),values);}
 }
 private void saveHeader(LinearLayout body,Button button){
  body.removeView(button);button.setText("保存");button.setContentDescription("保存");button.setMinWidth(0);button.setMinimumWidth(0);button.setMinHeight(dp(48));button.setMinimumHeight(dp(48));button.setPadding(dp(12),0,dp(12),0);
  LinearLayout bar=(LinearLayout)root.getChildAt(0);bar.removeView(bar.findViewWithTag("header-spacer"));bar.addView(button,new LinearLayout.LayoutParams(dp(64),-2));
  List<Object> original=new ArrayList<>();formValues(body,original);Runnable back=pageBack;
  Runnable discard=()->{returningPage=true;try{back.run();}finally{returningPage=false;}};
  pageBack=()->{
   List<Object> current=new ArrayList<>();formValues(body,current);
   if(current.equals(original)){back.run();return;}
   if(unsavedDialog!=null&&unsavedDialog.isShowing())return;
   AlertDialog prompt=new AlertDialog.Builder(this,R.style.SecondaryDialog).setTitle("修改尚未保存").setMessage("退出前是否保存？").setPositiveButton("保存并退出",null).setNegativeButton("不保存退出",null).create();
   unsavedDialog=prompt;prompt.setOnDismissListener(dialog->{if(unsavedDialog==prompt)unsavedDialog=null;});prompt.show();styleDialog(prompt,false);
   prompt.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{prompt.dismiss();button.performClick();});
   prompt.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v->{prompt.dismiss();discard.run();});
  };
 }
 private EditText courseField(LinearLayout card,String label,String value,String hint,String tag){TextView caption=text(label,14,0xff56677e);caption.setPadding(0,dp(14),0,dp(8));card.addView(caption);EditText input=field(hint,false);input.setText(value);input.setContentDescription(label);input.setTag(tag);card.addView(input,new LinearLayout.LayoutParams(-1,dp(54)));return input;}
 private CourseChoiceSheet courseChoicePicker(Spinner choice,String label,String[] values){
  ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(choice.getWindowToken(),0);
  choiceSheet.open(choice,label,values);return choiceSheet;
 }
 private class CourseChoiceSheet extends FrameLayout {
  final View shade;final LinearLayout panel;final TextView title,hint;final ScrollView scroll;final LinearLayout rows;final RadioButton[] options=new RadioButton[30];
  Spinner selected;boolean open;int generation;final int formAccessibility=root.getImportantForAccessibility();
  CourseChoiceSheet(){
   super(MainActivity.this);setVisibility(INVISIBLE);setFocusableInTouchMode(true);setClickable(true);
   shade=new View(MainActivity.this);shade.setBackgroundColor(0x66000000);shade.setTag("choice-shade");addView(shade,new FrameLayout.LayoutParams(-1,-1));shade.setOnClickListener(v->close(true));
   panel=new LinearLayout(MainActivity.this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(20),dp(12),dp(20),dp(12));panel.setBackground(bg(Color.WHITE,28));panel.setClickable(true);FrameLayout.LayoutParams panelBounds=new FrameLayout.LayoutParams(-1,dp(700),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);panelBounds.bottomMargin=dp(16);addView(panel,panelBounds);
   View handle=new View(MainActivity.this);handle.setBackground(bg(0xffd4dfe2,2));handle.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);LinearLayout.LayoutParams grip=new LinearLayout.LayoutParams(dp(36),dp(4));grip.gravity=Gravity.CENTER_HORIZONTAL;grip.bottomMargin=dp(16);panel.addView(handle,grip);
   title=text("",21,NAVY);title.setTypeface(null,Typeface.BOLD);panel.addView(title);
   hint=text("",14,MUTED);hint.setPadding(0,dp(8),0,dp(16));panel.addView(hint);
   scroll=new ScrollView(MainActivity.this);scroll.setClipToPadding(false);panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));rows=new LinearLayout(MainActivity.this);rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);
   for(int i=0;i<options.length;i++){
    final int position=i;RadioButton option=new RadioButton(MainActivity.this);options[i]=option;option.setButtonDrawable(null);option.setGravity(Gravity.CENTER);option.setTextSize(15);option.setPadding(dp(8),dp(10),dp(8),dp(10));option.setTag("choice-option-"+i);option.setTextColor(NAVY);option.setLineSpacing(dp(3),1);option.setOnClickListener(v->{if(!open)return;selected.setSelection(position);close(true);});
   }
   Button cancel=action(panel,"取消",()->close(true));cancel.setTag("choice-cancel");cancel.setTextColor(NAVY);cancel.setBackgroundTintList(android.content.res.ColorStateList.valueOf(SURFACE));LinearLayout.LayoutParams footer=(LinearLayout.LayoutParams)cancel.getLayoutParams();footer.topMargin=dp(16);footer.bottomMargin=0;cancel.setStateListAnimator(null);
  }
  @Override protected void onMeasure(int widthSpec,int heightSpec){
   FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)panel.getLayoutParams();lp.width=Math.max(1,MeasureSpec.getSize(widthSpec)-dp(24));rows.measure(MeasureSpec.makeMeasureSpec(Math.max(1,lp.width-dp(40)),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));int chrome=dp(154)+(int)(title.getTextSize()+hint.getTextSize());lp.height=Math.max(1,Math.min(MeasureSpec.getSize(heightSpec)*4/5,rows.getMeasuredHeight()+chrome));super.onMeasure(widthSpec,heightSpec);
  }
  void open(Spinner choice,String label,String[] values){
   if(open&&selected==choice)return;int token=++generation;open=true;selected=choice;panel.animate().cancel();shade.animate().cancel();title.setText("选择"+label);hint.setText("当前："+values[choice.getSelectedItemPosition()]+" · 点选后返回");
   boolean units=values.length==12,weeks=values.length==30;int columns=weeks?5:2;
   for(RadioButton option:options)if(option.getParent()!=null)((ViewGroup)option.getParent()).removeView(option);rows.removeAllViews();
   LinearLayout row=null;
   for(int i=0;i<options.length;i++){
    RadioButton option=options[i];option.setVisibility(i<values.length?VISIBLE:GONE);if(i>=values.length)continue;
    if(i%columns==0){row=new LinearLayout(MainActivity.this);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(8);rows.addView(row,rp);}
    boolean current=i==choice.getSelectedItemPosition();String caption=weeks?""+(i+1):values[i];if(units)caption+="\n"+START_TIMES[i]+"–"+END_TIMES[i];if(current)caption+="  ✓";option.setText(caption);option.setChecked(current);option.setTypeface(null,current?Typeface.BOLD:Typeface.NORMAL);option.setMinHeight(dp(weeks?48:units?72:56));GradientDrawable surface=bg(current?SOFT:SURFACE,16);if(current)surface.setStroke(dp(1.5f),0xff4f8b94);option.setBackground(surface);ripple(option);option.setContentDescription(label+"，"+values[i]+(units?"，"+START_TIMES[i]+"至"+END_TIMES[i]:"")+(current?"，已选中":""));LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(0,-2,1);if(i%columns>0)cell.leftMargin=dp(8);row.addView(option,cell);
   }
   if(values.length%columns!=0)for(int i=values.length%columns;i<columns;i++){Space space=new Space(MainActivity.this);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,1,1);lp.leftMargin=dp(8);row.addView(space,lp);}
   root.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);setVisibility(VISIBLE);updateSystemBack();requestFocus();if(Build.VERSION.SDK_INT>=28)setAccessibilityPaneTitle("选择"+label);requestLayout();panel.setTranslationY(dp(48));shade.setAlpha(0f);
   getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){getViewTreeObserver().removeOnPreDrawListener(this);if(token!=generation||!open||!isAttachedToWindow())return true;RadioButton option=options[choice.getSelectedItemPosition()];android.graphics.Rect rect=new android.graphics.Rect(0,0,option.getWidth(),option.getHeight());rows.offsetDescendantRectToMyCoords(option,rect);scroll.scrollTo(0,Math.max(0,rect.top-dp(60)));if(android.animation.ValueAnimator.areAnimatorsEnabled()){panel.animate().translationY(0).setDuration(200).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();shade.animate().alpha(1f).setDuration(200).start();}else{panel.setTranslationY(0);shade.setAlpha(1f);}return true;}});
  }

  void close(boolean animate){
   scheduleRemoteNotice();
   if(animate&&!open)return;open=false;int token=++generation;panel.animate().cancel();shade.animate().cancel();Runnable finish=()->{if(token!=generation)return;setVisibility(INVISIBLE);updateSystemBack();root.setImportantForAccessibility(formAccessibility);if(selected!=null&&selected.isAttachedToWindow())selected.requestFocus();};
   if(animate&&isAttachedToWindow()&&android.animation.ValueAnimator.areAnimatorsEnabled()){panel.animate().translationY(dp(48)).setDuration(180).setInterpolator(new android.view.animation.AccelerateInterpolator()).withEndAction(finish).start();shade.animate().alpha(0f).setDuration(180).setInterpolator(new android.view.animation.AccelerateInterpolator()).start();}else finish.run();
  }
  @Override protected void onDetachedFromWindow(){generation++;panel.animate().cancel();shade.animate().cancel();super.onDetachedFromWindow();}
 }

 private Spinner courseChoice(LinearLayout card,String label,String[] values,int selected,String tag){TextView caption=text(label,14,0xff56677e);caption.setPadding(0,dp(14),0,dp(8));card.addView(caption);Spinner choice=new Spinner(this){@Override public boolean performClick(){courseChoicePicker(this,label,values);return true;}};ArrayAdapter<String> adapter=new ArrayAdapter<>(this,R.layout.course_choice,values);choice.setAdapter(adapter);choice.setSelection(selected);choice.setBackgroundResource(R.drawable.form_field);choice.setContentDescription(label);choice.setTag(tag);card.addView(choice,new LinearLayout.LayoutParams(-1,dp(54)));return choice;}
 private final class CourseSlot {
  final Course original;final int number;final LinearLayout card;final EditText teacher,room,details;final Spinner day,start,end;final ToggleButton[] selected=new ToggleButton[30];
  CourseSlot(LinearLayout body,Course initial,int number){
   original=initial;this.number=number;String suffix=number==1?"":"-slot-"+number;card=panel(body);card.setTag("course-slot-"+number);card.addView(text("上课时段 "+number,18,INK));
   room=courseField(card,"上课教室",initial==null?"":SchoolParser.clean(initial.room),"教室 / 线上（选填）","course-room"+suffix);
   teacher=courseField(card,"任课老师",initial==null?"":SchoolParser.clean(initial.teacher),"老师（选填）","course-teacher"+suffix);
   details=courseField(card,"课程内容 / 实验项目",initial==null?"":SchoolParser.clean(initial.displayDetails()),"具体内容（选填）","course-details"+suffix);
   if(initial!=null&&initial.teacher.isEmpty()&&!initial.room.isEmpty()){TextView legacy=text("旧课表的老师可能包含在地点信息中，可在此手动分开填写。",12,0xff56677e);legacy.setPadding(0,dp(12),0,0);card.addView(legacy);}
   day=courseChoice(card,"星期",new String[]{"星期一","星期二","星期三","星期四","星期五","星期六","星期日"},initial==null?0:initial.day-1,"course-day"+suffix);
   String[] units=new String[12];for(int i=0;i<12;i++)units[i]="第 "+(i+1)+" 节";
   start=courseChoice(card,"开始节次",units,initial==null?0:initial.start-1,"course-start"+suffix);end=courseChoice(card,"结束节次",units,initial==null?1:initial.end-1,"course-end"+suffix);
   LinearLayout weeks=card;weeks.addView(text("具体周次 · 可多选",18,INK));TextView summary=text("",13,0xff56677e);summary.setTag("course-weeks-summary"+suffix);summary.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);summary.setPadding(0,dp(12),0,dp(8));weeks.addView(summary);Runnable update=()->{StringBuilder value=new StringBuilder();for(int w=1;w<=30;w++)if(selected[w-1]!=null&&selected[w-1].isChecked()){if(value.length()>0)value.append(",");value.append(w);}summary.setText(value.length()==0?"请选择至少一个教学周":prettyWeeks(new Course(1,1,1,"","",value.toString())));summary.setTextColor(value.length()==0?0xff9a3849:0xff56677e);};LinearLayout quick=new LinearLayout(MainActivity.this);weeks.addView(quick);String[] shortcuts={"全选","单周","双周","清空"};for(int i=0;i<4;i++){final int mode=i;Button button=new Button(MainActivity.this);button.setText(shortcuts[i]);button.setTextSize(12);button.setAllCaps(false);button.setPadding(0,0,0,0);button.setMinWidth(0);button.setTextColor(0xff355db0);button.setBackgroundResource(R.drawable.form_field);button.setTag("course-weeks-"+i+suffix);button.setOnClickListener(v->{for(int w=1;w<=30;w++)selected[w-1].setChecked(mode==0||(mode==1&&w%2==1)||(mode==2&&w%2==0));update.run();});LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(0,dp(48),1);if(i>0)qp.leftMargin=dp(6);quick.addView(button,qp);}for(int first=1;first<=30;first+=5){LinearLayout row=new LinearLayout(MainActivity.this);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=dp(8);weeks.addView(row,rp);for(int w=first;w<first+5;w++){ToggleButton button=new ToggleButton(MainActivity.this);selected[w-1]=button;button.setTextOn(""+w);button.setTextOff(""+w);button.setTextColor(INK);button.setTextSize(14);button.setMinWidth(0);button.setPadding(0,0,0,0);button.setTag("course-week-"+w+suffix);button.setContentDescription("第"+w+"周");button.setBackgroundResource(R.drawable.form_field);button.setOnCheckedChangeListener((v,on)->{if(on)button.setBackground(bg(PRIMARY,10));else button.setBackgroundResource(R.drawable.form_field);button.setTextColor(on?Color.WHITE:INK);update.run();});button.setChecked(initial==null?w==week:initial.active(w));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(48),1);if(w>first)bp.leftMargin=dp(6);row.addView(button,bp);}}update.run();
  }
  Course value(String name){
   if(end.getSelectedItemPosition()<start.getSelectedItemPosition())throw new IllegalArgumentException("结束节次不能早于开始节次");
   StringJoiner weeks=new StringJoiner(",");for(int w=1;w<=30;w++)if(selected[w-1].isChecked())weeks.add(""+w);
   if(weeks.length()==0)throw new IllegalArgumentException("请选择至少一个教学周");
   String content=SchoolParser.clean(details.getText().toString()),classes=original==null?"":original.className;
   if(original!=null&&content.equals(original.displayDetails())){
    content=original.details;
    if(!name.equals(original.name)){List<String> visible=new ArrayList<>();for(String part:content.split("；"))if(Course.isTeachingClassName(original.name,part))classes=Course.optionalText(classes+"；"+part);else visible.add(part);content=String.join("；",visible);}
   }
   Course next=new Course(day.getSelectedItemPosition()+1,start.getSelectedItemPosition()+1,end.getSelectedItemPosition()+1,name,SchoolParser.clean(room.getText().toString()),weeks.toString(),SchoolParser.clean(teacher.getText().toString()),content,classes);
   Course.parse(next.encode());return next;
  }
 }
 private void courseForm(Course initial,int index,Runnable back){
  String baseline=data;List<Course> courses=Course.parse(baseline);
  if(initial!=null&&(index<0||index>=courses.size()||!courses.get(index).encode().equals(initial.encode()))){toast("课程已更新，请重新打开课程");home();return;}
  LinearLayout body=secondary(initial==null?"添加课程":"校对课程","同一课程可有多个上课时段，分别设置教室、节次与周次。重新同步会覆盖本机修改。",back);
  LinearLayout info=panel(body);EditText name=courseField(info,"课程名称 · 必填",initial==null?"":SchoolParser.clean(initial.name),"例如：数据结构","course-name");
  LinearLayout schedules=new LinearLayout(this);schedules.setOrientation(LinearLayout.VERTICAL);body.addView(schedules);List<CourseSlot> slots=new ArrayList<>();int[] serial={0};
  java.util.function.Consumer<Course> addSlot=source->{CourseSlot slot=new CourseSlot(schedules,source,++serial[0]);slots.add(slot);Button remove=action(slot.card,"移除此时段",()->{if(slots.size()==1){toast("请至少保留一个上课时段");return;}slots.remove(slot);schedules.removeView(slot.card);});remove.setTag("remove-course-slot-"+serial[0]);};
  if(initial==null)addSlot.accept(null);else for(Course c:courses)if(c.name.equals(initial.name))addSlot.accept(c);
  Button add=action(body,"＋ 添加上课时段",()->addSlot.accept(null));add.setTag("add-course-slot");
  TextView error=text("",13,0xff9a3849);error.setTag("course-error");error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);body.addView(error);
  Button saveButton=action(body,initial==null?"添加并保存":"保存修改",()->{
   if(name.getText().toString().trim().isEmpty()){name.setError("请填写课程名称");name.requestFocus();return;}
   if(!data.equals(baseline)){error.setText("课表已更新，请返回后重新打开课程");return;}
   try{
    List<Course> next=new ArrayList<>();for(int i=0;i<slots.size();i++)try{next.add(slots.get(i).value(SchoolParser.clean(name.getText().toString())));}catch(IllegalArgumentException e){throw new IllegalArgumentException("时段 "+slots.get(i).number+"："+e.getMessage());}
    List<String> rows=new ArrayList<>();for(Course c:courses){if(initial!=null&&c.name.equals(initial.name)){for(int i=0;i<slots.size();i++)if(slots.get(i).original==c)rows.add(next.get(i).encode());}else rows.add(c.encode());}
    for(int i=0;i<slots.size();i++)if(slots.get(i).original==null)rows.add(next.get(i).encode());
    String updated=String.join("\n",rows);Course.parse(updated);data=updated;save();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(name.getWindowToken(),0);home();toast(initial==null?"课程已添加":"课程已更新");
   }catch(Exception e){error.setText("课程未保存："+e.getMessage());}
  });
  saveButton.setTag("save-course");saveHeader(body,saveButton);choiceSheet=new CourseChoiceSheet();((FrameLayout)root.getParent()).addView(choiceSheet,new FrameLayout.LayoutParams(-1,-1));
 }
 private EditText field(String hint,boolean secret){EditText e=new EditText(this);e.setSingleLine();e.setTextSize(16);e.setHint(hint);e.setTextColor(INK);e.setHintTextColor(0xff56677e);e.setPadding(dp(16),0,dp(16),0);e.setBackgroundResource(R.drawable.form_field);e.setInputType(secret?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT);e.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);if(secret)e.setSaveEnabled(false);return e;}
 private void login(){String filledAccount=account!=null&&account.isAttachedToWindow()?account.getText().toString():"",filledPassword=password!=null&&password.isAttachedToWindow()?password.getText().toString():"";if(busy){toast("同步正在进行中");return;}LinearLayout body=secondary("同步课表","连接宿迁学院，导入整学期课程。",()->{if(password!=null)password.getText().clear();home();});LinearLayout card=panel(body);TextView schoolTitle=text("宿迁学院",23,INK);schoolTitle.setTypeface(null,Typeface.BOLD);card.addView(schoolTitle);TextView desc=text("使用学校 VPN / 网上办事大厅账号",13,0xff56677e);desc.setPadding(0,dp(8),0,dp(22));card.addView(desc);card.addView(text("学校账号",13,0xff56677e));account=field("学号 / 学校账号",false);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(54));ap.topMargin=dp(8);card.addView(account,ap);TextView passwordLabel=text("登录密码",13,0xff56677e);passwordLabel.setPadding(0,dp(20),0,dp(8));card.addView(passwordLabel);password=field("请输入密码",true);card.addView(password,new LinearLayout.LayoutParams(-1,dp(54)));account.setText(filledAccount);password.setText(filledPassword);account.setContentDescription("学校账号");password.setContentDescription("学校登录密码");CheckBox show=new CheckBox(this);show.setText("显示密码");show.setTextSize(13);show.setTextColor(0xff56677e);show.setMinHeight(dp(48));show.setButtonTintList(android.content.res.ColorStateList.valueOf(0xff355db0));show.setOnCheckedChangeListener((b,on)->{password.setInputType(InputType.TYPE_CLASS_TEXT|(on?InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:InputType.TYPE_TEXT_VARIATION_PASSWORD));password.setSelection(password.length());});card.addView(show);
  captchaBox=new LinearLayout(this);captchaBox.setGravity(Gravity.CENTER_VERTICAL);captchaBox.setVisibility(View.GONE);captcha=field("验证码",false);captcha.setContentDescription("学校验证码");captchaImage=new ImageView(this);captchaImage.setContentDescription("点击刷新学校验证码");captchaImage.setFocusable(true);captchaImage.setOnClickListener(v->refreshCaptcha());captchaBox.addView(captcha,new LinearLayout.LayoutParams(0,dp(54),1));LinearLayout.LayoutParams cip=new LinearLayout.LayoutParams(dp(100),dp(54));cip.leftMargin=dp(8);captchaBox.addView(captchaImage,cip);card.addView(captchaBox);
  loginStatus=text("正在连接学校…",13,0xff56677e);loginStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);loginStatus.setPadding(0,dp(14),0,dp(8));card.addView(loginStatus);loginProgress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);loginProgress.setIndeterminate(true);loginProgress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(BLUE));loginProgress.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);card.addView(loginProgress,new LinearLayout.LayoutParams(-1,dp(4)));loginButton=action(card,"登录并同步课表",()->submitLogin());loginButton.setEnabled(false);LinearLayout privacy=panel(body);privacy.addView(text("仅用于学校认证",15,INK));TextView note=text("密码不保存到手机。\n同步失败时会保留原课表，已导入的课程可以离线查看。",13,0xff56677e);note.setLineSpacing(dp(4),1);note.setPadding(0,dp(10),0,0);privacy.addView(note);
  int token=screen;worker.execute(()->{try{school.prepare();if(!school.cachedCourses.isEmpty()){String synced=school.cachedCourses;runOnUiThread(()->{if(screen!=token||isDestroyed())return;data=synced;notes=school.courseNotes;save();home();toast("课表已更新");});return;}byte[] image=school.captchaRequired?school.captcha():null;runOnUiThread(()->{if(screen!=token)return;loginProgress.setVisibility(View.GONE);loginStatus.setText("学校连接正常，请输入账号密码。");loginButton.setEnabled(true);if(image!=null){captchaBox.setVisibility(View.VISIBLE);captchaImage.setImageBitmap(BitmapFactory.decodeByteArray(image,0,image.length));}});}catch(Exception e){runOnUiThread(()->{if(screen!=token)return;loginProgress.setVisibility(View.GONE);loginStatus.setText(e.getMessage());loginStatus.setTextColor(0xffb45266);loginButton.setEnabled(true);loginButton.setText("重新连接");loginButton.setOnClickListener(v->login());});}});
 }
 private void refreshCaptcha(){int token=screen;worker.execute(()->{try{byte[] b=school.captcha();runOnUiThread(()->{if(screen==token)captchaImage.setImageBitmap(BitmapFactory.decodeByteArray(b,0,b.length));});}catch(Exception e){runOnUiThread(()->{if(screen==token)loginStatus.setText("验证码加载失败，请重试。");});}});}
 private void submitLogin(){String user=account.getText().toString().trim(),pass=password.getText().toString(),code=captcha.getText().toString().trim();if(user.isEmpty()){account.setError("请输入学校账号");return;}if(pass.isEmpty()){password.setError("请输入密码");return;}if(school.captchaRequired&&code.isEmpty()){captcha.setError("请输入图片验证码");return;}busy=true;loginProgress.setVisibility(View.VISIBLE);loginButton.setEnabled(false);loginStatus.setText("正在认证并同步课程…");((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(password.getWindowToken(),0);int token=screen;worker.execute(()->{try{String next=school.login(user,pass,code);int count=Course.parse(next).size();if(count==0)throw new IllegalArgumentException("学校未返回课程，已保留原课表。");runOnUiThread(()->{busy=false;if(isDestroyed())return;data=next;notes="已从学校同步 · "+count+"条课程\n"+school.courseNotes;week=currentWeek();save();home();toast("课表同步成功");});}catch(Exception e){runOnUiThread(()->{busy=false;if(screen!=token){toast(e.getMessage());return;}loginProgress.setVisibility(View.GONE);loginButton.setEnabled(true);if(SchoolClient.passwordRejected(e.getMessage())){password.getText().clear();password.setError("密码错误，请重新输入");}loginStatus.setText(e.getMessage());loginStatus.setTextColor(0xffb45266);if(school.captchaRequired){captchaBox.setVisibility(View.VISIBLE);refreshCaptcha();}});}});}
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);openWidget(intent);if(ADD_WIDGET.equals(intent.getAction())&&hasWindowFocus()){intent.setAction(Intent.ACTION_MAIN);getWindow().getDecorView().post(()->pinWidget());}}
 private void openWidget(Intent intent){
  if(intent==null||!CourseWidget.OPEN.equals(intent.getAction()))return;
  if(busy){toast("正在同步，请稍候再打开课程");return;}
  try{
   int shownWeek=intent.getIntExtra("widgetWeek",0);String key=intent.getStringExtra("widgetCourse");
   if(key==null){home();return;}
   if(shownWeek>=1&&shownWeek<=30&&term.equals(intent.getStringExtra("widgetTerm")))for(CourseDisplay.Group group:CourseDisplay.groups(effectiveCourses(),shownWeek,displayChoices))if(CourseDisplay.key(shownWeek,group.selected).equals(key)){week=shownWeek;saveWeek();if(group.courses.size()>1)conflictPicker(group,shownWeek);else detail(group.selected,shownWeek,()->home());return;}
   home();toast("课程安排已更新，请查看最新课表");
  }catch(RuntimeException e){home();toast("请检查课表及开学日期设置");}
 }
  @Override public void onBackPressed(){if(navigationOverlay!=null){navigationOverlay.close(true);return;}if(choiceSheet!=null&&choiceSheet.getVisibility()==View.VISIBLE){choiceSheet.close(true);return;}if(busy){toast("正在同步，请稍候");return;}if(pageBack!=null){backPage();return;}if(tasksVisible){home();return;}super.onBackPressed();}
 @Override protected void onDestroy(){if(taskPage!=null)taskPage.close();remoteResumed=false;if(remote!=null)remote.unlisten(remoteListener);if(remoteDialog!=null)remoteDialog.dismiss();remoteHandler.removeCallbacksAndMessages(null);if(getWindow().getDecorView().getViewTreeObserver().isAlive())getWindow().getDecorView().getViewTreeObserver().removeOnScrollChangedListener(remoteScroll);if(unsavedDialog!=null)unsavedDialog.dismiss();if(widgetDialog!=null)widgetDialog.dismiss();screen++;pageMotion.cancel();while(navigationOverlay!=null)navigationOverlay.close(false);if(Build.VERSION.SDK_INT>=33&&backRegistered)getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(systemBack);if(stage!=null)for(int i=0;i<stage.getChildCount();i++)stage.getChildAt(i).animate().cancel();worker.shutdownNow();homePage=null;if(password!=null)password.getText().clear();super.onDestroy();}
 // Retain one Activity-owned home only; data/term/notes/choices changes replace it.
 private class HomePage {
  final LinearLayout view=root;final ViewPager pages=pager;final ScrollView scroll=timetableScroll;final FrameLayout days=daysBar;
  final TextView label=weekLabel,button=returnWeekButton;final String courses=data,changes=adjustments,start=term,note=notes;
  final Set<String> choices=new HashSet<>(displayChoices);
  boolean matches(){return courses.equals(data)&&changes.equals(adjustments)&&start.equals(term)&&note.equals(notes)&&choices.equals(displayChoices);}
 }
 private class WeekGrid extends FrameLayout {
  int shownWeek,contentWidth;LinearLayout days;
  WeekGrid(){super(MainActivity.this);}
  @Override protected void onMeasure(int widthSpec,int heightSpec){
   int width=MeasureSpec.getSize(widthSpec);
   if(width>0&&width!=contentWidth){contentWidth=width;buildGrid(this,shownWeek);}
   super.onMeasure(widthSpec,heightSpec);
  }
 }

}
