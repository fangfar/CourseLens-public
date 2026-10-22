package cn.courselens;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import java.util.*;
import java.util.concurrent.*;

@android.annotation.SuppressLint("ViewConstructor")
final class TaskPage extends LinearLayout {
 private final MainActivity activity;
 private final SecondaryUi ui;
 private final ChaoxingAccount account;
 private final ExecutorService worker=Executors.newSingleThreadExecutor(),network=Executors.newSingleThreadExecutor();
 private final TextView[] filters=new TextView[3];
 private final TextView platform,status;
 private final ScrollView[] scrolls=new ScrollView[3];
 private final LinearLayout[] lists=new LinearLayout[3];
 private final ViewPager pager;
 private final View capsule;
 private final LinearLayout choices;
 private float pagePosition;
 private int[] restoredScrollY,renderRestoreY;
 private Runnable renderNext;private int renderGeneration;
 private TextView refresh,configure,add;private View settings;
 private MainActivity.NavigationOverlay actionMenu;
 private final android.os.Handler timer=new android.os.Handler(android.os.Looper.getMainLooper());
 private final Runnable autoRefresh=this::autoRefresh;
 private boolean localReady,platformReady;private long retryAfter;
 private final ProgressBar progress;
 private ChaoxingClient client;
 private volatile ChaoxingClient.Result snapshot;private ChaoxingClient.Result localTasks=new ChaoxingClient.Result();
 private AlertDialog configuration;
 private EditText passwordField;
 private int filter;private volatile int generation;
 private boolean busy,refreshing;private volatile boolean closed;

 TaskPage(Context context,Bundle state){
  super(context);activity=(MainActivity)context;ui=new SecondaryUi(activity);account=new ChaoxingAccount(context);setTag("tasks-page");setOrientation(VERTICAL);LinearLayout pinned=new LinearLayout(context);pinned.setOrientation(VERTICAL);
  LinearLayout header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(ui.dp(16),ui.dp(12),ui.dp(16),0);pinned.addView(header,new LayoutParams(-1,-2));ui.menuAction(header,activity::openSideMenu);
  LinearLayout heading=new LinearLayout(context);heading.setOrientation(VERTICAL);heading.setGravity(Gravity.CENTER);header.addView(heading,new LayoutParams(0,-2,1));TextView title=ui.text("任务",24,ui.INK);title.setTypeface(null,Typeface.BOLD);heading.addView(title);
  platform=ui.text("未连接平台",12,ui.MUTED);platform.setTag("task-connected-platforms");
  ui.iconAction(header,R.drawable.ic_more,"任务操作",this::showActions).setTag("quick-menu-button");
  LinearLayout summary=new LinearLayout(context);summary.setPadding(ui.dp(20),ui.dp(4),ui.dp(20),ui.dp(8));pinned.addView(summary,new LayoutParams(-1,-2));summary.addView(platform,new LayoutParams(-2,-2));TextView separator=ui.text("|",12,ui.MUTED);separator.setPadding(ui.dp(8),0,ui.dp(8),0);separator.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);summary.addView(separator);
  status=ui.text("请先配置学习通账号密码，登录验证成功后才会保存。",12,ui.MUTED);status.setTag("chaoxing-status");status.setLineSpacing(ui.dp(3),1);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);summary.addView(status,new LayoutParams(0,-2,1));
  progress=new ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(ui.NAVY));progress.setVisibility(GONE);pinned.addView(progress,new LayoutParams(-1,ui.dp(3)));
  FrameLayout selector=new FrameLayout(context);selector.setPadding(ui.dp(4),ui.dp(4),ui.dp(4),ui.dp(4));selector.setBackground(ui.bg(ui.SOFT,999));LayoutParams choicesLayout=new LayoutParams(-1,-2);choicesLayout.setMargins(ui.dp(16),ui.dp(8),ui.dp(16),ui.dp(4));pinned.addView(selector,choicesLayout);
  capsule=new View(context);capsule.setTag("task-filter-capsule");capsule.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);capsule.setBackground(ui.bg(ui.CARD,999));selector.addView(capsule,new FrameLayout.LayoutParams(0,0));
  choices=new LinearLayout(context);selector.addView(choices,new FrameLayout.LayoutParams(-1,-2));
  String[] labels={"待处理","已完成","全部"};for(int i=0;i<labels.length;i++){final int selected=i;TextView tab=ui.text(labels[i],14,ui.MUTED);tab.setTag("task-filter-"+i);tab.setGravity(Gravity.CENTER);tab.setMinHeight(ui.dp(48));tab.setPadding(ui.dp(4),ui.dp(10),ui.dp(4),ui.dp(10));tab.setFocusable(true);tab.setOnClickListener(v->selectFilter(selected));filters[i]=tab;choices.addView(tab,new LayoutParams(0,-2,1));}
  for(int i=0;i<scrolls.length;i++){ScrollView scroll=new ScrollView(context);scroll.setTag("task-filter-page-"+i);scroll.setFillViewport(true);scroll.setClipToPadding(false);scrolls[i]=scroll;LinearLayout list=new LinearLayout(context);list.setOrientation(VERTICAL);list.setPadding(ui.dp(16),0,ui.dp(16),ui.dp(16));list.setTag("chaoxing-results");lists[i]=list;scroll.addView(list,new ScrollView.LayoutParams(-1,-2));}
  pager=new ViewPager(context);pager.setTag("task-filter-pager");pager.setOffscreenPageLimit(2);addView(new ScrollHeader(context,pinned,pager,ui.SURFACE,scrolls),new LayoutParams(-1,0,1));pager.setAdapter(new PagerAdapter(){
   @Override public int getCount(){return scrolls.length;}
   @Override public boolean isViewFromObject(View view,Object page){return view==page;}
   @Override public Object instantiateItem(ViewGroup parent,int position){parent.addView(scrolls[position]);return scrolls[position];}
   @Override public void destroyItem(ViewGroup parent,int position,Object page){parent.removeView((View)page);}
  });
  pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
   @Override public void onPageScrolled(int position,float offset,int pixels){pagePosition=position+offset;moveCapsule();}
   @Override public void onPageSelected(int position){markFilter(position);}
  });
  // ViewPager scrollX can use a relative origin after restoring a nonzero page.
  choices.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->moveCapsule());
  int initial=Math.max(0,Math.min(2,state==null?0:state.getInt("filter",0)));pagePosition=initial;pager.setCurrentItem(initial,false);markFilter(pager.getCurrentItem());
  if(state!=null){restoredScrollY=state.getIntArray("filterScrollY");if(restoredScrollY==null||restoredScrollY.length!=3){restoredScrollY=new int[3];restoredScrollY[filter]=state.getInt("scrollY",0);}}render();
  connected(false);if(account.configured()){platform.setText("学习通 · 已配置");platform.setContentDescription("已配置平台：学习通");}status.setText("正在加载本机任务…");loadLocal();
 }
 // Detached measurement has no window token, so ViewPager must populate again when attached.
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();pager.requestLayout();}
 private void showActions(){
  if(actionMenu!=null&&actionMenu.open){actionMenu.close(true);return;}
  actionMenu=activity.new NavigationOverlay(true,"任务操作");
  add=activity.menuItem(actionMenu,"添加任务","task-add",()->editTask(null));
  refresh=activity.menuItem(actionMenu,"刷新作业","chaoxing-refresh",this::refresh);
  configure=activity.menuItem(actionMenu,"账号密码配置","chaoxing-connect",this::configure);
  controls();
 }
 private void selectFilter(int value){pager.setCurrentItem(value>=0&&value<filters.length?value:0,android.animation.ValueAnimator.areAnimatorsEnabled());}
 private void markFilter(int value){filter=value;for(int i=0;i<filters.length;i++){boolean selected=i==filter;filters[i].setSelected(selected);filters[i].setTextColor(selected?ui.INK:ui.MUTED);filters[i].setTypeface(null,selected?Typeface.BOLD:Typeface.NORMAL);scrolls[i].setImportantForAccessibility(selected?IMPORTANT_FOR_ACCESSIBILITY_AUTO:IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);}}
 private void moveCapsule(){if(choices.getWidth()==0)return;int width=choices.getWidth()/filters.length;ViewGroup.LayoutParams size=capsule.getLayoutParams();if(size.width!=width||size.height!=choices.getHeight()){size.width=width;size.height=choices.getHeight();capsule.setLayoutParams(size);}capsule.setTranslationX(pagePosition*choices.getWidth()/filters.length);}
 private void render(){
  int token=++renderGeneration;View host=activity.getWindow().getDecorView();if(renderNext!=null)host.removeCallbacks(renderNext);
  if(getParent()==null&&getWidth()>0&&getHeight()>0){int width=getWidth(),height=getHeight();measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY));layout(0,0,width,height);}
  int[] positions={-1,-1,-1};for(int i=0;i<3;i++)if(restoredScrollY!=null)positions[i]=restoredScrollY[i];else if(renderRestoreY!=null)positions[i]=renderRestoreY[i];renderRestoreY=positions;if(snapshot!=null)restoredScrollY=null;
  List<ArrayDeque<Runnable>> pending=Arrays.asList(new ArrayDeque<>(),new ArrayDeque<>(),new ArrayDeque<>());
  if(!allTasks().isEmpty())for(LinearLayout list:lists){TextView empty=list.findViewWithTag("tasks-empty-title");if(empty!=null)empty.setText("正在加载作业…");}
  for(int index=0;index<3;index++){final int page=index;ArrayDeque<Runnable> steps=pending.get(page);LinearLayout next=new LinearLayout(getContext());next.setOrientation(VERTICAL);next.setPadding(ui.dp(16),0,ui.dp(16),ui.dp(16));next.setTag("chaoxing-results");ScrollView measuring=render(next,page,steps);
   steps.add(()->{ScrollView scroll=scrolls[page];int y=positions[page]>=0?positions[page]:scroll.getScrollY();scroll.removeAllViews();measuring.removeView(next);lists[page]=next;scroll.addView(next,new ScrollView.LayoutParams(-1,-2));scroll.addOnLayoutChangeListener(new OnLayoutChangeListener(){public void onLayoutChange(View v,int l,int t,int r,int b,int ol,int ot,int or,int ob){scroll.removeOnLayoutChangeListener(this);if(token==renderGeneration){scroll.scrollTo(0,y);positions[page]=-1;}}});});
  }
  // Build and measure rows in small frame budgets; keep the previous lists visible until each replacement is ready.
  renderNext=new Runnable(){public void run(){if(closed||token!=renderGeneration)return;long end=System.nanoTime()+6000000;do{
   ArrayDeque<Runnable> steps=pending.get(filter);if(steps.isEmpty()){steps=null;for(ArrayDeque<Runnable> other:pending)if(!other.isEmpty()){steps=other;break;}}
   if(steps==null){renderNext=null;return;}steps.removeFirst().run();
  }while(System.nanoTime()<end);host.postOnAnimation(this);}};
  if(allTasks().isEmpty()){for(ArrayDeque<Runnable> steps:pending)while(!steps.isEmpty())steps.removeFirst().run();renderNext=null;}else host.postOnAnimation(renderNext);
 }
 private void measurePending(ScrollView measuring,int page){int width=scrolls[page].getWidth(),height=scrolls[page].getHeight();if(width==0)width=pager.getMeasuredWidth();if(height==0)height=pager.getMeasuredHeight();if(width>0&&height>0){measuring.measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY));measuring.layout(0,0,width,height);}}
 private ScrollView render(LinearLayout list,int filter,ArrayDeque<Runnable> steps){
  ScrollView measuring=new ScrollView(getContext());measuring.setFillViewport(true);measuring.setClipToPadding(false);measuring.addView(list,new ScrollView.LayoutParams(-1,-2));
  Map<String,List<ChaoxingClient.Work>> groups=new LinkedHashMap<>();
  for(ChaoxingClient.Work work:allTasks()){if(!work.local()&&!ChaoxingCache.visible(snapshot,work)||filter==0&&work.completed()||filter==1&&!work.completed())continue;groups.computeIfAbsent(work.course,key->new ArrayList<>()).add(work);}
  for(Map.Entry<String,List<ChaoxingClient.Work>> group:groups.entrySet()){
   LinearLayout[] card={null};steps.add(()->{TextView course=ui.text(group.getKey().isEmpty()?"课程名称未识别":group.getKey(),13,ui.MUTED);course.setTag("task-course-label");course.setPadding(ui.dp(4),ui.dp(16),ui.dp(4),ui.dp(8));course.setLineSpacing(ui.dp(2),1);if(android.os.Build.VERSION.SDK_INT>=28)course.setAccessibilityHeading(true);list.addView(course,new LayoutParams(-1,-2));
    card[0]=new LinearLayout(getContext());card[0].setTag("task-course-group");card[0].setOrientation(VERTICAL);card[0].setBackground(ui.bg(ui.CARD,14));list.addView(card[0],new LayoutParams(-1,-2));measurePending(measuring,filter);});
   for(ChaoxingClient.Work work:group.getValue())steps.add(()->{addWork(card[0],work);measurePending(measuring,filter);});
  }
  if(groups.isEmpty())steps.add(()->{String[] titles={"还没有待处理任务","还没有已完成任务","还没有任务"};TextView emptyTitle=ui.text(snapshot==null?"尚未读取作业":snapshot.items.isEmpty()&&!snapshot.warnings.isEmpty()?"部分课程尚未读取完成":titles[filter],18,ui.INK);emptyTitle.setTag("tasks-empty-title");emptyTitle.setGravity(Gravity.CENTER);emptyTitle.setTypeface(null,Typeface.BOLD);emptyTitle.setPadding(ui.dp(16),ui.dp(64),ui.dp(16),ui.dp(16));list.addView(emptyTitle,new LayoutParams(-1,-2));measurePending(measuring,filter);});
  if(snapshot!=null&&!snapshot.warnings.isEmpty()){List<String> warnings=new ArrayList<>(snapshot.warnings);steps.add(()->{TextView heading=ui.text("本次读取说明",13,ui.MUTED);heading.setTag("task-read-notes");heading.setPadding(ui.dp(4),ui.dp(20),ui.dp(4),ui.dp(4));list.addView(heading);for(String warning:warnings){TextView note=ui.text(warning,13,ui.WARNING);note.setPadding(ui.dp(4),ui.dp(8),ui.dp(4),ui.dp(8));list.addView(note);}measurePending(measuring,filter);});}
  return measuring;
 }
 private void addWork(LinearLayout card,ChaoxingClient.Work work){
    if(card.getChildCount()>0){View divider=new View(getContext());divider.setBackgroundColor(ui.SOFT);LayoutParams line=new LayoutParams(-1,ui.dp(1));line.setMargins(ui.dp(16),0,ui.dp(16),0);card.addView(divider,line);}
    LinearLayout row=new LinearLayout(getContext());row.setTag("task-work");row.setOrientation(VERTICAL);row.setPadding(ui.dp(16),ui.dp(14),ui.dp(16),ui.dp(14));card.addView(row,new LayoutParams(-1,-2));
    LinearLayout heading=new LinearLayout(getContext());heading.setGravity(Gravity.TOP);row.addView(heading,new LayoutParams(-1,-2));
    TextView name=ui.text(work.title,16,ui.INK);name.setTag("task-work-title");name.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));name.setLineSpacing(ui.dp(2),1);heading.addView(name,new LayoutParams(0,-2,1));
    TextView state=ui.text(work.status.isEmpty()?"状态未知":work.status,13,ui.MUTED);state.setTag("task-work-status");state.setMaxWidth(ui.dp(96));state.setLineSpacing(ui.dp(2),1);state.setPadding(ui.dp(12),ui.dp(2),0,0);heading.addView(state,new LayoutParams(-2,-2));
    TextView deadline=ui.text(work.deadline,13,ui.MUTED);deadline.setTag("task-work-deadline");deadline.setLineSpacing(ui.dp(2),1);deadline.setContentDescription("截止："+work.deadline);android.graphics.drawable.Drawable clock=activity.getDrawable(R.drawable.ic_clock).mutate();clock.setTint(ui.MUTED);clock.setBounds(0,0,ui.dp(16),ui.dp(16));deadline.setCompoundDrawables(clock,null,null,null);deadline.setCompoundDrawablePadding(ui.dp(6));LayoutParams time=new LayoutParams(-1,-2);time.topMargin=ui.dp(8);row.addView(deadline,time);row.setFocusable(true);row.setOnLongClickListener(v->{taskActions(work);return true;});ui.ripple(row);
 }
 private void connected(boolean value){platform.setText(value?"学习通 · 已连接":account.configured()?"学习通 · 待验证":"未连接平台");platform.setTextColor(value?ui.NAVY:ui.MUTED);platform.setContentDescription(platform.getText());}
 private void busy(boolean value){busy=value;controls();}
 private void refreshing(boolean value){refreshing=value;controls();}
 private void controls(){if(configure!=null)configure.setEnabled(!busy);platform.setEnabled(!busy);if(refresh!=null)refresh.setEnabled(!busy&&!refreshing&&account.configured());if(settings!=null)settings.setEnabled(!busy&&localReady);if(add!=null)add.setEnabled(!busy&&localReady);if(busy||refreshing)pauseRefresh();else scheduleRefresh();progress.setVisibility(busy||refreshing?VISIBLE:GONE);}
 private boolean current(int token){return !closed&&token==generation&&!activity.isFinishing()&&!activity.isDestroyed();}
 private void configure(){
  if(busy||closed)return;if(refreshing){generation++;if(client!=null)client.close();refreshing(false);}LinearLayout form=new LinearLayout(getContext());form.setOrientation(VERTICAL);form.setPadding(ui.dp(24),ui.dp(8),ui.dp(24),ui.dp(8));
  TextView intro=ui.text("输入学习通账号密码。验证登录成功后，在本机加密保存；失败时保留原配置。",14,ui.MUTED);intro.setLineSpacing(ui.dp(4),1);form.addView(intro);
  EditText username=field(form,"学习通账号","手机号 / 超星账号",false);EditText password=field(form,"登录密码","请输入密码",true);passwordField=password;
  TextView feedback=ui.text("",13,ui.MUTED);feedback.setPadding(0,ui.dp(12),0,ui.dp(8));feedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);form.addView(feedback);
  ScrollView scroll=new ScrollView(getContext());scroll.addView(form);
  AlertDialog dialog=new AlertDialog.Builder(activity,R.style.SecondaryDialog).setTitle("学习通密码配置").setView(scroll).setNegativeButton("取消",null).setPositiveButton("验证并保存",null).create();configuration=dialog;dialog.setOnDismissListener(d->{password.setText("");passwordField=null;configuration=null;});dialog.show();dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);ui.styleDialog(dialog,false);
  if(account.configured())try{username.setText(account.load().account);}catch(Exception e){feedback.setText("原配置无法解密，请重新输入账号密码。");}
  dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String name=username.getText().toString().trim(),secret=password.getText().toString();if(name.isEmpty()||secret.isEmpty()){feedback.setText("请输入账号和密码。");return;}password.setText("");username.setEnabled(false);password.setEnabled(false);dialog.setCancelable(false);dialog.getButton(-1).setEnabled(false);dialog.getButton(-2).setEnabled(false);feedback.setText("正在验证登录，请稍候…");busy(true);int token=++generation;
   if(client!=null)client.close();client=new ChaoxingClient();ChaoxingClient connection=client;
   worker.execute(()->{try{connection.login(name,secret);if(!current(token))return;account.save(name,secret);activity.runOnUiThread(()->{if(current(token)){connected(true);dialog.dismiss();busy(false);status.setText("登录验证成功，密码配置已加密保存。");snapshot=null;localReady=false;render();loadLocal();}});connection.close();}catch(Exception e){activity.runOnUiThread(()->{if(current(token)){busy(false);dialog.setCancelable(true);username.setEnabled(true);password.setEnabled(true);dialog.getButton(-1).setEnabled(true);dialog.getButton(-2).setEnabled(true);feedback.setText("登录验证或保存失败，未更新配置。请核对账号密码和网络；验证码或机构认证需先在学习通完成。");status.setText("配置未更新，原配置及作业保留。");}});}});
  });
 }
 private EditText field(LinearLayout form,String label,String hint,boolean secret){TextView caption=ui.text(label,13,ui.MUTED);caption.setPadding(0,ui.dp(16),0,ui.dp(8));form.addView(caption);EditText input=new EditText(getContext());input.setTextSize(15);input.setSingleLine(true);input.setHint(hint);input.setContentDescription(label);input.setTextColor(ui.INK);input.setHintTextColor(ui.MUTED);input.setPadding(ui.dp(16),0,ui.dp(16),0);input.setBackgroundResource(R.drawable.form_field);input.setInputType(InputType.TYPE_CLASS_TEXT|(secret?InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_TEXT_VARIATION_NORMAL));input.setSaveEnabled(false);input.setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_NO);form.addView(input,new LayoutParams(-1,ui.dp(54)));return input;}
 private void refresh(){refresh(new ChaoxingClient());}
 private void refresh(ChaoxingClient connection){
  if(busy||refreshing||closed){connection.close();return;}if(!account.configured()){connection.close();configure();return;}refreshing(true);retryAfter=System.currentTimeMillis()+60000;int token=++generation;status.setText("正在使用保存的配置登录学习通…");if(client!=null)client.close();client=connection;
  worker.execute(()->{try{ChaoxingAccount.Saved saved=account.load();ChaoxingCache cache=new ChaoxingCache(getContext(),saved.account);ChaoxingClient.Result before=ChaoxingCache.copy(snapshot==null||!platformReady?cache.load():snapshot);before.lastAttempt=System.currentTimeMillis();if(!current(token))return;cache.save(before);activity.runOnUiThread(()->{if(current(token)){snapshot=before;platformReady=true;network.execute(()->{try{connection.login(saved.account,saved.password);activity.runOnUiThread(()->{if(current(token))connected(true);});sync(connection,token,saved.account);}catch(Exception e){activity.runOnUiThread(()->{if(current(token)){refreshing(false);connected(false);status.setText("更新未成功，请检查网络或重新配置密码。已有作业保留。");}});}finally{connection.close();}});}});}catch(Exception e){activity.runOnUiThread(()->{if(current(token)){refreshing(false);status.setText("本机缓存暂时无法保存，已有任务保留，请重试。");}});connection.close();}});
 }
 private void loadLocal(){
  busy(true);int token=++generation;worker.execute(()->{try{ChaoxingClient.Result local=new ChaoxingCache(getContext(),"").load(),platform=null;boolean readable=true;try{if(account.configured())platform=new ChaoxingCache(getContext(),account.load().account).load();}catch(Exception e){readable=false;}ChaoxingClient.Result savedPlatform=platform;boolean ready=readable;activity.runOnUiThread(()->{if(current(token)){localTasks=local==null?new ChaoxingClient.Result():local;snapshot=savedPlatform==null?new ChaoxingClient.Result():savedPlatform;localReady=true;platformReady=ready;busy(false);status.setText(!ready?"学习通缓存暂时无法读取，手动任务仍可用，原文件已保留。":account.configured()?"本机已保存 "+allTasks().size()+" 份任务":"可直接添加任务，配置学习通后可同步作业。");render();}});}catch(Exception e){activity.runOnUiThread(()->{if(current(token)){busy(false);status.setText("本机任务暂时无法读取，原文件未删除，请重试。");}});}});
 }
 private List<ChaoxingClient.Work> allTasks(){List<ChaoxingClient.Work> items=new ArrayList<>(localTasks.items);if(snapshot!=null)items.addAll(snapshot.items);return items;}
 private void sync(ChaoxingClient connection,int token,String username){
  try{
   ChaoxingClient.Result start=snapshot;Set<String> disabled=start==null?Collections.emptySet():new LinkedHashSet<>(start.disabledCourses);ChaoxingClient.Result fresh=connection.sync(message->activity.runOnUiThread(()->{if(current(token))status.setText(message);}),disabled);if(!current(token))return;
   // Serialize commits with local edits and merge against the cache after the request returns.
   worker.execute(()->{if(!current(token))return;try{ChaoxingCache cache=new ChaoxingCache(getContext(),username);ChaoxingClient.Result result=ChaoxingCache.merge(cache.load(),fresh);boolean persisted;try{cache.save(result);persisted=true;}catch(Exception e){persisted=false;}boolean saved=persisted;
    activity.runOnUiThread(()->{if(current(token)){snapshot=result;refreshing(false);status.setText("已更新 "+result.checked+" / "+result.total+" 门课程"+(saved?"":" · 本机保存失败，退出后可能无法保留"));render();}});
   }catch(Exception e){activity.runOnUiThread(()->{if(current(token)){refreshing(false);status.setText("刷新结果暂时无法保存，原有任务和本地修改已保留。");}});}});
  }catch(Exception e){activity.runOnUiThread(()->{if(current(token)){refreshing(false);status.setText("作业读取失败（"+ChaoxingClient.describe(e)+"）。原有作业和密码配置已保留。");}});}finally{connection.close();}
 }
 void pauseRefresh(){timer.removeCallbacks(autoRefresh);}
 void scheduleRefresh(){pauseRefresh();if(closed||busy||refreshing||!localReady||!platformReady||snapshot==null||!account.configured()||!activity.taskRefreshActive())return;long now=System.currentTimeMillis();timer.postDelayed(autoRefresh,Math.max(Math.max(0,retryAfter-now),TaskRefresh.delay(snapshot.lastAttempt,snapshot.intervalMinutes,now)));}
 private void autoRefresh(){if(closed)return;if(!activity.taskRefreshActive()||configuration!=null){timer.postDelayed(autoRefresh,60000);return;}if(!busy&&!refreshing&&localReady){if(TaskRefresh.delay(snapshot.lastAttempt,snapshot.intervalMinutes,System.currentTimeMillis())==0)refresh();else scheduleRefresh();}}
 private void saveChange(java.util.function.Consumer<ChaoxingClient.Result> change,String message){saveChange(change,message,false);}
 private void saveChange(java.util.function.Consumer<ChaoxingClient.Result> change,String message,boolean local){
  if(busy||closed)return;busy(true);int token=generation;status.setText("正在保存任务设置…");worker.execute(()->{if(!current(token))return;try{ChaoxingCache cache=new ChaoxingCache(getContext(),local?"":account.load().account);ChaoxingClient.Result saved=cache.load(),next=saved==null?ChaoxingCache.copy(local?localTasks:snapshot):saved;change.accept(next);cache.save(next);activity.runOnUiThread(()->{if(current(token)){if(local)localTasks=next;else snapshot=next;busy(false);status.setText(message);render();}});}catch(Exception e){activity.runOnUiThread(()->{if(current(token)){busy(false);status.setText("保存失败，原有任务和设置已保留，请重新打开后重试。");}});}});
 }
 private void delete(ChaoxingClient.Work work){
  if(busy||closed||snapshot==null)return;saveChange(next->{ChaoxingClient.Work removed=ChaoxingCache.find(next.items,work);if(removed!=null)next.items.remove(removed);else removed=ChaoxingCache.copy(work);if(ChaoxingCache.find(next.deleted,work)==null){removed.deletedAt=System.currentTimeMillis();next.deleted.add(removed);}},"任务已删除，可在任务设置的删除历史中恢复。",work.local());
 }
 void settingsEntry(LinearLayout parent){settings=activity.option(parent,R.drawable.ic_tasks,"任务设置","课程加载、删除历史与自动刷新",this::settings);settings.setTag("task-settings");controls();}
 void settings(){
  if(busy||closed||!localReady)return;MainActivity.NavigationOverlay sheet=activity.new NavigationOverlay(false,"任务设置");
  View courses=activity.option(sheet.body,R.drawable.ic_tasks,"加载的课程",account.configured()?"选择要加载和展示任务的课程":"配置学习通后可选择课程",()->{sheet.close(false);courses();});courses.setTag("task-course-settings");courses.setEnabled(account.configured()&&platformReady);courses.setAlpha(account.configured()&&platformReady?1f:.45f);
  activity.option(sheet.body,R.drawable.ic_notes,"删除历史","已删除 "+(snapshot.deleted.size()+localTasks.deleted.size())+" 份任务，可恢复",()->{sheet.close(false);history();}).setTag("task-deletion-history");
  View interval=activity.option(sheet.body,R.drawable.ic_clock,"自动刷新",!account.configured()?"配置学习通后可设置":snapshot.intervalMinutes==0?"仅每天首次打开时刷新":"每 "+snapshot.intervalMinutes+" 分钟 · 每天首次打开也刷新",()->{sheet.close(false);interval();});interval.setTag("task-refresh-settings");interval.setEnabled(account.configured()&&platformReady);interval.setAlpha(account.configured()&&platformReady?1f:.45f);
 }
 private void courses(){
  if(busy||closed)return;MainActivity.NavigationOverlay sheet=activity.new NavigationOverlay(false,"加载的课程");sheet.body.setTag("task-course-selection");
  TextView intro=ui.text("关闭后停止加载并隐藏该课程任务，已保存的任务仍保留。重新开启后可点击刷新。",14,ui.MUTED);intro.setPadding(0,0,0,ui.dp(12));sheet.body.addView(intro);
  Set<String> disabled=new LinkedHashSet<>(snapshot.disabledCourses);
  if(snapshot.courses.isEmpty())sheet.body.addView(ui.text("请先刷新一次，获取可选择的课程。",14,ui.MUTED));
  Map<String,Integer> names=new HashMap<>();for(ChaoxingClient.Subject course:snapshot.courses)names.put(course.name,names.getOrDefault(course.name,0)+1);
  for(ChaoxingClient.Subject course:snapshot.courses){CheckBox choice=new CheckBox(getContext());choice.setText(course.name+(names.get(course.name)>1?"（"+course.key()+"）":""));choice.setTextColor(ui.INK);choice.setTextSize(15);choice.setMinHeight(ui.dp(48));choice.setTag("task-course-"+course.key());choice.setChecked(!disabled.contains(course.key()));choice.setOnCheckedChangeListener((button,checked)->{if(checked)disabled.remove(course.key());else disabled.add(course.key());});sheet.body.addView(choice,new LayoutParams(-1,-2));}
  ui.action(sheet.body,"保存",()->{if(busy)return;sheet.close(false);saveChange(next->{next.disabledCourses.clear();next.disabledCourses.addAll(disabled);},"课程加载设置已保存。");}).setTag("task-courses-save");
 }
 private void history(){
  if(busy||closed)return;MainActivity.NavigationOverlay sheet=activity.new NavigationOverlay(false,"删除历史");sheet.body.setTag("task-history");
  if(snapshot.deleted.isEmpty()&&localTasks.deleted.isEmpty())sheet.body.addView(ui.text("还没有删除记录。",14,ui.MUTED));
  List<ChaoxingClient.Work> removed=new ArrayList<>(snapshot.deleted);removed.addAll(localTasks.deleted);removed.sort((a,b)->Long.compare(b.deletedAt,a.deletedAt));for(ChaoxingClient.Work work:removed){String when=work.deletedAt==0?"":new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date(work.deletedAt));
   activity.option(sheet.body,R.drawable.ic_tasks,work.title,work.course+" · "+when+" · 点击恢复",()->{if(busy)return;sheet.close(false);saveChange(next->{ChaoxingClient.Work restored=ChaoxingCache.find(next.deleted,work);if(restored==null)throw new IllegalStateException("Task changed");next.deleted.remove(restored);restored.deletedAt=0;if(ChaoxingCache.find(next.items,restored)==null)next.items.add(restored);},"任务已恢复；若该课程关闭了加载，开启后即可显示。",work.local());}).setTag("task-restore");
  }
 }
 private void taskActions(ChaoxingClient.Work work){
  if(busy||closed)return;MainActivity.NavigationOverlay sheet=activity.new NavigationOverlay(false,"任务操作");activity.option(sheet.body,R.drawable.ic_notes,"编辑",work.title,()->{sheet.close(false);editTask(work);}).setTag("task-edit");activity.option(sheet.body,R.drawable.ic_tasks,"删除","移入删除历史，可恢复",()->{sheet.close(false);delete(work);}).setTag("task-delete");
 }
 private void editTask(ChaoxingClient.Work work){
  if(busy||closed||!localReady)return;boolean local=work==null||work.local();MainActivity.NavigationOverlay sheet=activity.new NavigationOverlay(false,work==null?"添加任务":"编辑任务");sheet.body.setTag("task-editor");
  EditText title=field(sheet.body,"任务名称","请输入任务名称",false);title.setTag("task-editor-title");title.setText(work==null?"":work.title);
  EditText course=null;if(local){course=field(sheet.body,"课程 / 分类","选填，默认为手动任务",false);course.setTag("task-editor-course");course.setText(work==null?"":work.course);}else sheet.body.addView(ui.text(work.course,14,ui.MUTED));
  EditText deadline=field(sheet.body,"截止时间","选填，格式：2026-10-21 23:59",false);deadline.setTag("task-editor-deadline");deadline.setText(work==null||work.deadline.equals("截止时间未知")?"":work.deadline);
  CheckBox done=new CheckBox(getContext());done.setText("已完成");done.setTextColor(ui.INK);done.setMinHeight(ui.dp(48));done.setTag("task-editor-completed");done.setChecked(work!=null&&work.completed());sheet.body.addView(done,new LayoutParams(-1,-2));EditText category=course;
  if(!local)sheet.body.addView(ui.text("本地编辑会保留，刷新不会覆盖。",13,ui.MUTED));
  ui.action(sheet.body,"保存",()->{if(busy)return;String name=title.getText().toString().trim(),time=deadline.getText().toString().trim(),group=category==null?work.course:category.getText().toString().trim();if(name.isEmpty()||name.length()>200){title.setError("请输入 1–200 个字的任务名称");return;}if(category!=null&&group.length()>100){category.setError("分类最多 100 个字");return;}if(!time.isEmpty()&&(work==null||!time.equals(work.deadline)))try{java.time.LocalDateTime.parse(time,java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(java.time.format.ResolverStyle.STRICT));}catch(java.time.DateTimeException e){deadline.setError("请使用完整日期和时间，例如 2026-10-21 23:59");return;}
   String state=work!=null&&done.isChecked()==work.completed()?work.status:done.isChecked()?"已完成":"未完成";ChaoxingClient.Work item=new ChaoxingClient.Work(group.isEmpty()?"手动任务":group,name,state,"");item.deadline=time.isEmpty()?"截止时间未知":time;item.courseKey=work==null?"local":work.courseKey;item.workKey=work==null?"local:"+UUID.randomUUID():work.workKey;item.edited=!local;item.originalTitle=local?"":work.originalTitle.isEmpty()?work.title:work.originalTitle;
   sheet.close(false);saveChange(next->{if(work==null)next.items.add(item);else{ChaoxingClient.Work current=ChaoxingCache.find(next.items,work);if(current==null){if(ChaoxingCache.find(next.deleted,work)!=null)throw new IllegalStateException("Task deleted");next.items.add(item);}else{item.courseKey=current.courseKey;item.workKey=current.workKey;next.items.set(next.items.indexOf(current),item);}}},work==null?"任务已添加。":"任务修改已保存。",local);
  }).setTag("task-editor-save");
 }
 private void interval(){
  if(busy||closed)return;MainActivity.NavigationOverlay sheet=activity.new NavigationOverlay(false,"自动刷新");sheet.body.setTag("task-refresh-interval");TextView intro=ui.text("软件使用期间按间隔刷新，每天第一次打开时也会自动刷新。失败时保留已有任务。",14,ui.MUTED);sheet.body.addView(intro);
  EditText minutes=field(sheet.body,"刷新间隔（分钟）","1–10080；0 表示仅每天首次打开刷新",false);minutes.setTag("task-interval-minutes");minutes.setInputType(InputType.TYPE_CLASS_NUMBER);minutes.setText(String.valueOf(snapshot.intervalMinutes));
  ui.action(sheet.body,"保存",()->{if(busy)return;int value;try{value=Integer.parseInt(minutes.getText().toString().trim());if(value<0||value>TaskRefresh.MAX_MINUTES)throw new NumberFormatException();}catch(NumberFormatException e){minutes.setError("请输入 0–10080 之间的整数分钟");return;}int selected=value;sheet.close(false);saveChange(next->next.intervalMinutes=selected,"自动刷新间隔已保存。");}).setTag("task-interval-save");
 }
 void saveState(Bundle state){state.putInt("filter",filter);int[] positions=new int[scrolls.length];for(int i=0;i<positions.length;i++)positions[i]=renderRestoreY!=null&&renderRestoreY[i]>=0?renderRestoreY[i]:scrolls[i].getScrollY();state.putIntArray("filterScrollY",restoredScrollY==null?positions:restoredScrollY);}
 void close(){closed=true;pauseRefresh();generation++;renderGeneration++;if(renderNext!=null)activity.getWindow().getDecorView().removeCallbacks(renderNext);renderNext=null;if(configuration!=null)configuration.dismiss();if(passwordField!=null)passwordField.setText("");if(client!=null)client.close();worker.shutdownNow();network.shutdownNow();}
}
