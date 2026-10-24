package cn.courselens;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

@android.annotation.SuppressLint("ViewConstructor")
final class TaskPage extends LinearLayout {
 private final MainActivity activity;
 private final SecondaryUi ui;
 private final ChaoxingAccount account;
 private final ExecutorService worker=Executors.newSingleThreadExecutor(),network=Executors.newSingleThreadExecutor();
 private final TextView platform,status;
 private final ScrollView[] scrolls=new ScrollView[1];
 private final LinearLayout[] lists=new LinearLayout[1];
 private final ScrollView pager;
 private int[] restoredScrollY,renderRestoreY;
 private Runnable renderNext;private int renderGeneration;
 private ChaoxingClient.Work expandedTask;
 private TaskRow expandedRow;
 private AlertDialog deleteDialog;
 private TextView add;private View settings;
 private final LinearLayout banner,snack;
 private final TextView bannerDetails;
 private final android.os.Handler feedbackTimer=new android.os.Handler(android.os.Looper.getMainLooper());
 private final Runnable hideSnack=this::hideSnack,hideNotice=this::hideNotice;

 private final android.os.Handler timer=new android.os.Handler(android.os.Looper.getMainLooper());
 private final Runnable autoRefresh=this::autoRefresh;
 private boolean localReady,platformReady;private long retryAfter;
 private final ProgressBar progress;
 private ChaoxingClient client;
 private volatile ChaoxingClient.Result snapshot;private ChaoxingClient.Result localTasks=new ChaoxingClient.Result();
 private AlertDialog configuration,deadlineDialog;
 private EditText passwordField;
 private volatile int generation;
 private boolean busy,refreshing;private volatile boolean closed;

 TaskPage(Context context,Bundle state){
  super(context);activity=(MainActivity)context;ui=new SecondaryUi(activity);account=new ChaoxingAccount(context);setTag("tasks-page");setOrientation(VERTICAL);LinearLayout pinned=new LinearLayout(context);pinned.setOrientation(VERTICAL);
  LinearLayout header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(ui.dp(16),ui.dp(12),ui.dp(16),0);pinned.addView(header,new LayoutParams(-1,-2));ui.menuAction(header,activity::openSideMenu);header.addView(new View(context),new LayoutParams(ui.dp(56),1));
  LinearLayout heading=new LinearLayout(context);heading.setOrientation(VERTICAL);heading.setGravity(Gravity.CENTER);header.addView(heading,new LayoutParams(0,-2,1));TextView title=ui.text("任务",24,ui.INK);title.setGravity(Gravity.CENTER);title.setTypeface(Typeface.create("serif",Typeface.BOLD));title.setTag("primary-page-title");heading.addView(title);
  add=ui.iconAction(header,R.drawable.ic_add,"添加任务",()->editTask(null));add.setTag("task-add");settings=ui.iconAction(header,R.drawable.ic_more,"任务设置",this::settings);settings.setTag("task-settings");ui.fitHeader(header,heading);
  platform=ui.text("学习通 · 未配置",13,ui.INK);platform.setTag("task-connected-platforms");platform.setPadding(ui.dp(12),ui.dp(8),ui.dp(12),ui.dp(8));platform.setMinHeight(ui.dp(48));platform.setGravity(Gravity.CENTER_VERTICAL);platform.setBackground(new android.graphics.drawable.InsetDrawable(ui.bg(ui.SOFT,999),0,ui.dp(4),0,ui.dp(4)));platform.setOnClickListener(v->{if(account.configured())refresh();else configure();});platform.setContentDescription("同步状态，点击刷新学习通作业");
  LinearLayout summary=new LinearLayout(context);summary.setPadding(ui.dp(20),ui.dp(4),ui.dp(20),ui.dp(8));summary.addView(platform,new LayoutParams(-2,-2));pinned.addView(summary);
  banner=new LinearLayout(context);banner.setTag("task-sync-banner");banner.setOrientation(VERTICAL);banner.setPadding(ui.dp(12),ui.dp(4),ui.dp(8),ui.dp(4));banner.setBackground(ui.bg(ui.SOFT,14));banner.setVisibility(GONE);LayoutParams bannerSize=new LayoutParams(-1,-2);bannerSize.setMargins(ui.dp(16),0,ui.dp(16),ui.dp(8));pinned.addView(banner,bannerSize);
  LinearLayout bannerRow=new LinearLayout(context);bannerRow.setGravity(Gravity.CENTER_VERTICAL);banner.addView(bannerRow);
  status=ui.text("",13,ui.INK);status.setTag("chaoxing-status");status.setLineSpacing(ui.dp(3),1);status.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);bannerRow.addView(status,new LayoutParams(0,-2,1));
  TextView expand=smallAction(bannerRow,"展开",()->{toggleBannerDetails();});expand.setTag("task-banner-expand");
  smallAction(bannerRow,"重试",this::refresh).setTag("task-banner-retry");smallAction(bannerRow,"关闭",()->banner.setVisibility(GONE)).setTag("task-banner-close");
  bannerDetails=ui.text("",13,ui.MUTED);bannerDetails.setLineSpacing(ui.dp(3),1);bannerDetails.setPadding(0,ui.dp(8),0,ui.dp(8));bannerDetails.setTag("task-read-notes");bannerDetails.setVisibility(GONE);banner.addView(bannerDetails);
  progress=new ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(ui.NAVY));progress.setVisibility(GONE);pinned.addView(progress,new LayoutParams(-1,ui.dp(3)));
  for(int i=0;i<scrolls.length;i++){ScrollView scroll=new ScrollView(context);scroll.setTag("task-filter-page-"+i);scroll.setFillViewport(true);scroll.setClipToPadding(false);scrolls[i]=scroll;LinearLayout list=new LinearLayout(context);list.setOrientation(VERTICAL);list.setPadding(ui.dp(16),0,ui.dp(16),ui.dp(16));list.setTag("chaoxing-results");lists[i]=list;scroll.addView(list,new ScrollView.LayoutParams(-1,-2));}
  pager=scrolls[0];ScrollHeader content=new ScrollHeader(context,pinned,pager,ui.SURFACE,scrolls);addView(content,new LayoutParams(-1,0,1));
  snack=new LinearLayout(context);snack.setPadding(ui.dp(16),ui.dp(6),ui.dp(8),ui.dp(6));snack.setGravity(Gravity.CENTER_VERTICAL);snack.setBackground(ui.bg(ui.INK,16));snack.setVisibility(GONE);snack.setTag("task-undo-bar");FrameLayout.LayoutParams snackSize=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);snackSize.setMargins(ui.dp(16),0,ui.dp(16),ui.dp(20));content.addView(snack,snackSize);
  if(state!=null){int[] old=state.getIntArray("filterScrollY");int index=state.getInt("filter",0);restoredScrollY=new int[]{old!=null&&old.length>0?old[Math.max(0,Math.min(old.length-1,index))]:state.getInt("scrollY",0)};}render();
  connected(false);if(account.configured()){chip("学习通 · 已配置",false);}feedback("正在加载本机任务…");loadLocal();
 }
 private Button taskButton(LinearLayout body,String label,Runnable action){Button v=ui.action(body,label,action);v.setBackgroundTintList(null);v.setBackground(ui.bg(ui.INK,24));v.setTextColor(ui.CARD);return v;}
 private TextView smallAction(LinearLayout body,String label,Runnable action){TextView v=ui.text(label,12,ui.INK);v.setGravity(Gravity.CENTER);v.setMinHeight(ui.dp(48));v.setMinWidth(ui.dp(48));v.setPadding(ui.dp(6),0,ui.dp(6),0);v.setFocusable(true);v.setOnClickListener(w->action.run());body.addView(v);return v;}
 private void rowAction(LinearLayout body,String text,Runnable action){TextView button=smallAction(body,text,action);button.setBackground(new android.graphics.drawable.InsetDrawable(ui.bg(ui.SOFT,999),0,ui.dp(6),0,ui.dp(6)));button.setTypeface(null,Typeface.BOLD);button.setPadding(ui.dp(14),ui.dp(10),ui.dp(14),ui.dp(10));LayoutParams size=new LayoutParams(-2,-2);size.rightMargin=ui.dp(8);button.setLayoutParams(size);ui.focusOutline(button);}
 private final class TaskMetadata extends LinearLayout {
  TaskMetadata(){super(TaskPage.this.getContext());setGravity(Gravity.CENTER_VERTICAL);}
  @Override protected void onMeasure(int width,int height){float needed=getPaddingLeft()+getPaddingRight();for(int i=0;i<getChildCount();i++){TextView text=(TextView)getChildAt(i);LayoutParams size=(LayoutParams)text.getLayoutParams();needed+=Math.max(text.getMinimumWidth(),text.getPaint().measureText(text.getText().toString())+text.getCompoundPaddingLeft()+text.getCompoundPaddingRight())+size.rightMargin+(i>0?ui.dp(8):0);}boolean wrap=needed>MeasureSpec.getSize(width);setOrientation(wrap?VERTICAL:HORIZONTAL);for(int i=1;i<getChildCount();i++){LayoutParams size=(LayoutParams)getChildAt(i).getLayoutParams();size.leftMargin=wrap?0:ui.dp(8);size.topMargin=wrap?ui.dp(4):0;}super.onMeasure(width,height);}
 }
 private final class TaskRow extends LinearLayout {
  final ChaoxingClient.Work work;View content,actions,divider;
  TaskRow(ChaoxingClient.Work work){super(TaskPage.this.getContext());this.work=work;setOrientation(VERTICAL);}
  void expanded(boolean open){View heading=(View)content.getParent();LayoutParams spacing=(LayoutParams)heading.getLayoutParams();spacing.bottomMargin=ui.dp(open?0:12);heading.setLayoutParams(spacing);actions.setVisibility(open?VISIBLE:GONE);content.setContentDescription(work.title+(open?"，点击收起操作":"，点击展开编辑与删除"));if(android.os.Build.VERSION.SDK_INT>=30)content.setStateDescription(open?"已展开":"已收起");}
 }
 private android.transition.Transition taskTransition(boolean opening){
  return new android.transition.TransitionSet().setOrdering(android.transition.TransitionSet.ORDERING_TOGETHER).addTransition(new android.transition.ChangeBounds()).addTransition(new android.transition.Fade()).setDuration(opening?280:220).setInterpolator(new android.view.animation.PathInterpolator(.4f,0f,.2f,1f));
 }
 private void toggleTask(TaskRow row){
  boolean open=expandedRow!=row;
  if(android.animation.ValueAnimator.areAnimatorsEnabled()&&lists[0].isLaidOut())android.transition.TransitionManager.beginDelayedTransition(lists[0],taskTransition(open));
  if(expandedRow!=null)expandedRow.expanded(false);
  expandedTask=open?row.work:null;expandedRow=open?row:null;row.expanded(open);
 }
 private void restoreExpanded(LinearLayout list){
  expandedRow=null;for(int i=0;i<list.getChildCount();i++)if(list.getChildAt(i) instanceof LinearLayout){LinearLayout group=(LinearLayout)list.getChildAt(i);for(int j=0;j<group.getChildCount();j++)if(group.getChildAt(j) instanceof TaskRow){TaskRow row=(TaskRow)group.getChildAt(j);boolean open=expandedTask!=null&&ChaoxingCache.find(Collections.singletonList(row.work),expandedTask)!=null;row.expanded(open);if(open)expandedRow=row;}}
  if(expandedRow==null)expandedTask=null;
 }
 private void hideNotice(){banner.setVisibility(GONE);}
 private void hideSnack(){snack.setVisibility(GONE);}
 private void feedback(String message){
  feedbackTimer.removeCallbacks(hideNotice);status.setText(message);boolean temporary=message.startsWith("正在")||message.startsWith("本机已保存")||message.startsWith("可直接添加");
  if(temporary)return;banner.setVisibility(VISIBLE);bannerDetails.setText(message);bannerDetails.setVisibility(GONE);
  boolean failure=message.contains("失败")||message.contains("未成功")||message.contains("无法");
  banner.findViewWithTag("task-banner-retry").setVisibility(failure&&account.configured()?VISIBLE:GONE);
  banner.findViewWithTag("task-banner-expand").setVisibility(GONE);
  if(!failure)feedbackTimer.postDelayed(hideNotice,3000);
  if(failure&&(message.startsWith("作业读取")||message.startsWith("更新未成功")))chip("学习通 · 同步失败",false);
 }
 private void readingNotes(){if(snapshot==null||snapshot.warnings.isEmpty())return;feedbackTimer.removeCallbacks(hideNotice);banner.setVisibility(VISIBLE);status.setText(snapshot.warnings.size()+" 条读取说明 · 其余任务继续显示");bannerDetails.setText(String.join("\n",snapshot.warnings));bannerDetails.setVisibility(GONE);TextView expand=banner.findViewWithTag("task-banner-expand");expand.setText("展开");expand.setContentDescription("展开读取说明");expand.setVisibility(VISIBLE);}
 private void toggleBannerDetails(){boolean open=bannerDetails.getVisibility()!=VISIBLE;bannerDetails.setVisibility(open?VISIBLE:GONE);TextView expand=banner.findViewWithTag("task-banner-expand");expand.setText(open?"收起":"展开");expand.setContentDescription(open?"收起读取说明":"展开读取说明");}
 private void undoBar(ChaoxingClient.Work work){
  feedbackTimer.removeCallbacks(hideSnack);snack.removeAllViews();TextView label=ui.text("已删除任务",14,ui.CARD);label.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);snack.addView(label,new LayoutParams(0,-2,1));TextView undo=smallAction(snack,"撤销",()->saveChange(next->{ChaoxingClient.Work removed=ChaoxingCache.find(next.deleted,work);if(removed==null)throw new IllegalStateException("Task changed");next.deleted.remove(removed);removed.deletedAt=0;if(ChaoxingCache.find(next.items,removed)==null)next.items.add(removed);},"任务已恢复。",work.local(),this::hideSnack));undo.setTextColor(ui.CARD);undo.setTag("task-undo");snack.setVisibility(VISIBLE);feedbackTimer.postDelayed(hideSnack,5000);
 }
 private void pickDeadline(EditText field){pickDeadline(field,()->{});}
 private void pickDeadline(EditText field,Runnable selected){
  java.time.LocalDateTime old=TaskDeadline.parse(field.getText().toString());java.time.LocalDateTime date=old==null?java.time.LocalDateTime.now():old;
  LinearLayout box=new LinearLayout(getContext());box.setOrientation(VERTICAL);DatePicker picker=new DatePicker(new android.view.ContextThemeWrapper(activity,R.style.SecondaryPicker));picker.init(date.getYear(),date.getMonthValue()-1,date.getDayOfMonth(),null);picker.setTag("task-date-picker");box.addView(picker);TimePicker time=new TimePicker(new android.view.ContextThemeWrapper(activity,R.style.TaskTimePickerTheme));time.setTag("task-time-picker");time.setIs24HourView(true);time.setHour(date.getHour());time.setMinute(date.getMinute());box.addView(time);ScrollView scroll=new ScrollView(getContext());scroll.addView(box);
  AlertDialog dialog=new AlertDialog.Builder(activity,R.style.SecondaryDialog).setTitle("截止日期与时间").setView(scroll).setNegativeButton("取消",null).setPositiveButton("确定",(d,w)->{picker.clearFocus();time.clearFocus();field.setText(String.format(Locale.ROOT,"%04d-%02d-%02d %02d:%02d",picker.getYear(),picker.getMonth()+1,picker.getDayOfMonth(),time.getHour(),time.getMinute()));selected.run();}).create();deadlineDialog=dialog;dialog.setOnDismissListener(d->{if(deadlineDialog==dialog)deadlineDialog=null;});dialog.show();ui.styleDialog(dialog,false);
 }
 private android.graphics.drawable.Drawable completionIcon(){
  android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();states.addState(new int[]{android.R.attr.state_checked},new CompletionIcon(true));states.addState(new int[]{},new CompletionIcon(false));return states;
 }
 private final class CompletionIcon extends android.graphics.drawable.Drawable {
  private final boolean checked;private final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
  CompletionIcon(boolean checked){this.checked=checked;}
  @Override public void draw(android.graphics.Canvas canvas){android.graphics.Rect b=getBounds();float x=b.exactCenterX(),y=b.exactCenterY(),r=ui.dp(12);paint.setColor(checked?ui.INK:ui.MUTED);paint.setStrokeWidth(ui.dp(2));paint.setStyle(checked?android.graphics.Paint.Style.FILL:android.graphics.Paint.Style.STROKE);canvas.drawCircle(x,y,r,paint);if(checked){paint.setColor(ui.CARD);paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);android.graphics.Path path=new android.graphics.Path();path.moveTo(x-ui.dp(5),y);path.lineTo(x-ui.dp(1),y+ui.dp(4));path.lineTo(x+ui.dp(6),y-ui.dp(4));canvas.drawPath(path,paint);}}
  @Override public int getIntrinsicWidth(){return ui.dp(26);}@Override public int getIntrinsicHeight(){return ui.dp(26);}@Override public void setAlpha(int value){paint.setAlpha(value);}@Override public void setColorFilter(android.graphics.ColorFilter value){paint.setColorFilter(value);}@Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
 }
 private void render(){
  int token=++renderGeneration;View host=activity.getWindow().getDecorView();if(renderNext!=null)host.removeCallbacks(renderNext);
  if(getParent()==null&&getWidth()>0&&getHeight()>0){int width=getWidth(),height=getHeight();measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY));layout(0,0,width,height);}
  int[] positions={-1};for(int i=0;i<scrolls.length;i++)if(restoredScrollY!=null)positions[i]=restoredScrollY[i];else if(renderRestoreY!=null)positions[i]=renderRestoreY[i];renderRestoreY=positions;if(snapshot!=null)restoredScrollY=null;
  List<ArrayDeque<Runnable>> pending=Collections.singletonList(new ArrayDeque<>());
  if(!allTasks().isEmpty())for(LinearLayout list:lists){TextView empty=list.findViewWithTag("tasks-empty-title");if(empty!=null)empty.setText("正在加载作业…");}
  for(int index=0;index<scrolls.length;index++){final int page=index;ArrayDeque<Runnable> steps=pending.get(page);LinearLayout next=new LinearLayout(getContext());next.setOrientation(VERTICAL);next.setPadding(ui.dp(16),0,ui.dp(16),ui.dp(80));next.setTag("chaoxing-results");ScrollView measuring=render(next,page,steps);
   steps.add(()->{ScrollView scroll=scrolls[page];int y=positions[page]>=0?positions[page]:scroll.getScrollY();scroll.removeAllViews();measuring.removeView(next);lists[page]=next;restoreExpanded(next);scroll.addView(next,new ScrollView.LayoutParams(-1,-2));scroll.addOnLayoutChangeListener(new OnLayoutChangeListener(){public void onLayoutChange(View v,int l,int t,int r,int b,int ol,int ot,int or,int ob){scroll.removeOnLayoutChangeListener(this);if(token==renderGeneration){scroll.scrollTo(0,y);positions[page]=-1;}}});});
  }
  // Build and measure rows in small frame budgets; keep the previous lists visible until each replacement is ready.
  renderNext=new Runnable(){public void run(){if(closed||token!=renderGeneration)return;long end=System.nanoTime()+6000000;do{
   ArrayDeque<Runnable> steps=pending.get(0);if(steps.isEmpty()){steps=null;for(ArrayDeque<Runnable> other:pending)if(!other.isEmpty()){steps=other;break;}}
   if(steps==null){renderNext=null;return;}steps.removeFirst().run();
  }while(System.nanoTime()<end);host.postOnAnimation(this);}};
  if(allTasks().isEmpty()){for(ArrayDeque<Runnable> steps:pending)while(!steps.isEmpty())steps.removeFirst().run();renderNext=null;}else host.postOnAnimation(renderNext);
 }
 private void measurePending(ScrollView measuring,int page){int width=scrolls[page].getWidth(),height=scrolls[page].getHeight();if(width==0)width=pager.getMeasuredWidth();if(height==0)height=pager.getMeasuredHeight();if(width>0&&height>0){measuring.measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY));measuring.layout(0,0,width,height);}}
 private ScrollView render(LinearLayout list,int filter,ArrayDeque<Runnable> steps){
  ScrollView measuring=new ScrollView(getContext());measuring.setFillViewport(true);measuring.setClipToPadding(false);measuring.addView(list,new ScrollView.LayoutParams(-1,-2));
  Map<String,List<ChaoxingClient.Work>> groups=new LinkedHashMap<>();
  for(ChaoxingClient.Work work:TaskDeadline.sorted(allTasks())){if(work.completed()||!work.local()&&!ChaoxingCache.visible(snapshot,work))continue;groups.computeIfAbsent(work.course,key->new ArrayList<>()).add(work);}
  if(!groups.isEmpty())steps.add(()->{addDeadlineHero(list);measurePending(measuring,filter);});
  for(Map.Entry<String,List<ChaoxingClient.Work>> group:groups.entrySet()){
   LinearLayout[] card={null};steps.add(()->{LinearLayout courseHead=new LinearLayout(getContext());courseHead.setGravity(Gravity.CENTER_VERTICAL);courseHead.setPadding(ui.dp(2),ui.dp(16),ui.dp(2),ui.dp(8));TextView course=ui.text(group.getKey().isEmpty()?"课程名称未识别":group.getKey(),12,ui.MUTED);course.setTag("task-course-label");course.setTypeface(null,Typeface.BOLD);course.setBackground(ui.bg(ui.SOFT,999));course.setPadding(ui.dp(10),ui.dp(4),ui.dp(10),ui.dp(4));if(android.os.Build.VERSION.SDK_INT>=28)course.setAccessibilityHeading(true);FrameLayout courseSlot=new FrameLayout(getContext());courseSlot.addView(course,new FrameLayout.LayoutParams(-2,-2));courseHead.addView(courseSlot,new LayoutParams(0,-2,1));TextView count=ui.text(group.getValue().size()+" 项待交",12,ui.MUTED);LayoutParams countSize=new LayoutParams(-2,-2);countSize.leftMargin=ui.dp(12);courseHead.addView(count,countSize);list.addView(courseHead);
    card[0]=new LinearLayout(getContext());card[0].setTag("task-course-group");card[0].setOrientation(VERTICAL);card[0].setBackground(ui.bg(ui.CARD,24));card[0].setClipToOutline(true);list.addView(card[0],new LayoutParams(-1,-2));measurePending(measuring,filter);});
   for(ChaoxingClient.Work work:group.getValue())steps.add(()->{addWork(card[0],work,2);measurePending(measuring,filter);});
  }
  if(groups.isEmpty())steps.add(()->{TextView emptyTitle=ui.text(!localReady?"正在读取本机任务…":snapshot!=null&&snapshot.items.isEmpty()&&!snapshot.warnings.isEmpty()?"部分课程尚未读取完成":"暂无未完成任务",18,ui.INK);emptyTitle.setTag("tasks-empty-title");emptyTitle.setGravity(Gravity.CENTER);emptyTitle.setTypeface(null,Typeface.BOLD);emptyTitle.setPadding(ui.dp(16),ui.dp(64),ui.dp(16),ui.dp(16));list.addView(emptyTitle,new LayoutParams(-1,-2));TextView hint=ui.text(!account.configured()?"手动任务可直接添加；连接学习通后同步作业。":"这里只显示未完成任务，点击圆钮完成后自动隐藏。",14,ui.MUTED);hint.setGravity(Gravity.CENTER);list.addView(hint);Button emptyAction=taskButton(list,"添加任务",()->editTask(null));emptyAction.setTag("task-empty-action");if(!account.configured())smallAction(list,"配置学习通",this::configure);measurePending(measuring,filter);});
  return measuring;
 }
 private void addDeadlineHero(LinearLayout list){
  java.time.LocalDateTime now=java.time.LocalDateTime.now();ChaoxingClient.Work nearest=null;java.time.LocalDateTime earliest=null;
  for(ChaoxingClient.Work work:allTasks()){if(work.completed()||!work.local()&&!ChaoxingCache.visible(snapshot,work))continue;java.time.LocalDateTime due=TaskDeadline.parse(work.deadline);if(due!=null&&(earliest==null||due.isBefore(earliest))){earliest=due;nearest=work;}}
  if(nearest==null)return;ChaoxingClient.Work work=nearest;
  LinearLayout hero=new LinearLayout(getContext());hero.setOrientation(VERTICAL);hero.setPadding(ui.dp(18),ui.dp(18),ui.dp(18),ui.dp(18));hero.setBackground(ui.bg(ui.CARD,24));hero.setTag("task-deadline-hero");
  TextView label=ui.text("最近截止 · "+work.course,12,ui.MUTED);hero.addView(label);
  TextView title=ui.text(work.title,22,ui.INK);title.setTypeface(Typeface.create("serif",Typeface.BOLD));title.setPadding(0,ui.dp(12),0,ui.dp(8));hero.addView(title);
  hero.addView(ui.text(work.deadline+" 截止",13,ui.MUTED));TextView remaining=ui.text(TaskDeadline.countdown(earliest,now),30,ui.INK);remaining.setPadding(0,ui.dp(16),0,ui.dp(12));hero.addView(remaining);
  ProgressBar bar=new ProgressBar(getContext(),null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);long left=java.time.Duration.between(now,earliest).getSeconds();bar.setProgress((int)Math.max(0,Math.min(100,100-left*100/(7*86400))));bar.setProgressTintList(android.content.res.ColorStateList.valueOf(ui.INK));bar.setContentDescription("七天窗口内的时间流逝进度");hero.addView(bar,new LayoutParams(-1,ui.dp(6)));
  TextView note=ui.text("按 7 天窗口计 · 时间流逝进度",11,ui.MUTED);note.setPadding(0,ui.dp(8),0,0);hero.addView(note);hero.setOnClickListener(v->taskActions(work));ui.focusOutline(hero);LayoutParams size=new LayoutParams(-1,-2);size.topMargin=ui.dp(16);size.bottomMargin=ui.dp(4);list.addView(hero,size);
 }
 private void toggleCompleted(ChaoxingClient.Work work){
  if(busy||closed)return;saveChange(next->{ChaoxingClient.Work current=ChaoxingCache.find(next.items,work);if(current==null)throw new IllegalStateException("Task changed");ChaoxingClient.Work changed=new ChaoxingClient.Work(current.course,current.title,current.completed()?"未完成":"已完成",current.url);changed.courseKey=current.courseKey;changed.workKey=current.workKey;changed.deadline=current.deadline;changed.deadlineEdited=current.deadlineEdited;changed.originalTitle=current.originalTitle.isEmpty()?current.title:current.originalTitle;changed.edited=!current.local();next.items.set(next.items.indexOf(current),changed);},work.local()?"任务状态已保存。":"本地标记已保存，不代表已向学习通提交作业。",work.local());
 }
 private void addWork(LinearLayout card,ChaoxingClient.Work work,int page){
  TaskRow row=new TaskRow(work);row.setTag("task-work");row.setPadding(ui.dp(8),0,ui.dp(16),0);row.divider=new View(getContext());row.divider.setBackgroundColor(ui.DIVIDER);row.divider.setTag("task-divider");LayoutParams line=new LayoutParams(-1,ui.dp(1));line.leftMargin=ui.dp(48);row.addView(row.divider,line);row.divider.setVisibility(card.getChildCount()==0?GONE:VISIBLE);card.addView(row,new LayoutParams(-1,-2));
  LinearLayout heading=new LinearLayout(getContext());heading.setGravity(Gravity.TOP);LayoutParams headingSize=new LayoutParams(-1,-2);headingSize.topMargin=ui.dp(12);headingSize.bottomMargin=ui.dp(12);row.addView(heading,headingSize);
  CheckBox check=new CheckBox(getContext());check.setChecked(work.completed());check.setTag("task-complete-toggle");check.setContentDescription((work.completed()?"标记待处理：":"标记本地完成：")+work.title);check.setButtonDrawable(completionIcon());check.setButtonTintList(null);heading.addView(check,new LayoutParams(ui.dp(48),ui.dp(48)));check.setOnClickListener(v->{check.setChecked(work.completed());toggleCompleted(work);});
  LinearLayout content=new LinearLayout(getContext());content.setOrientation(VERTICAL);content.setPadding(0,ui.dp(4),0,ui.dp(6));heading.addView(content,new LayoutParams(0,-2,1));
  TextView name=ui.text(work.title,16,work.completed()?ui.MUTED:ui.INK);name.setTag("task-work-title");name.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));name.setLineSpacing(ui.dp(2),1);if(work.completed())name.setPaintFlags(name.getPaintFlags()|android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);content.addView(name);
  java.time.LocalDateTime due=TaskDeadline.parse(work.deadline);TextView deadline=ui.text(TaskDeadline.display(work.deadline),13,ui.MUTED);deadline.setTag("task-work-deadline");deadline.setContentDescription(TaskDeadline.display(work.deadline).equals("截止时间未知")?"截止时间未知，可补录":"截止："+TaskDeadline.display(work.deadline));deadline.setLineSpacing(ui.dp(2),1);android.graphics.drawable.Drawable clock=activity.getDrawable(R.drawable.ic_clock).mutate();clock.setTint(ui.MUTED);clock.setBounds(0,0,ui.dp(13),ui.dp(13));deadline.setCompoundDrawables(clock,null,null,null);deadline.setCompoundDrawablePadding(ui.dp(4));TaskMetadata metadata=new TaskMetadata();LayoutParams metadataSize=new LayoutParams(-1,-2);metadataSize.topMargin=ui.dp(6);content.addView(metadata,metadataSize);metadata.addView(deadline,new LayoutParams(-2,-2));boolean hasDeadline=!TaskDeadline.display(work.deadline).equals("截止时间未知")&&!work.deadline.equals("未提供截止时间");metadata.setVisibility(hasDeadline?VISIBLE:GONE);deadline.setVisibility(hasDeadline?VISIBLE:GONE);
  if(!work.completed()&&due!=null){boolean urgent=java.time.Duration.between(java.time.LocalDateTime.now(),due).toHours()<48;TextView relative=ui.text((urgent?"• ":"")+TaskDeadline.relative(due,java.time.LocalDateTime.now()),12,urgent?ui.INK:ui.MUTED);relative.setTypeface(null,Typeface.BOLD);relative.setBackground(ui.bg(ui.SOFT,999));relative.setPadding(ui.dp(8),ui.dp(3),ui.dp(8),ui.dp(3));metadata.addView(relative,new LayoutParams(-2,-2));}
  TaskMetadata actions=new TaskMetadata();actions.setPadding(ui.dp(48),0,0,ui.dp(12));actions.setTag("task-inline-actions");actions.setVisibility(GONE);row.addView(actions);rowAction(actions,"编辑",()->editTask(work));rowAction(actions,"删除",()->confirmDelete(work));
  row.content=content;row.actions=actions;row.expanded(false);content.setMinimumHeight(ui.dp(48));content.setOnClickListener(v->toggleTask(row));content.setFocusable(true);row.setOnLongClickListener(v->{taskActions(work);return true;});ui.focusOutline(content);
 }
 private void chip(String value,boolean healthy){platform.setText(value);platform.setContentDescription(value+"，点击"+(account.configured()?"刷新作业":"配置学习通"));android.graphics.drawable.GradientDrawable dot=ui.bg(healthy?0xff2b8a65:ui.MUTED,999);dot.setBounds(0,0,ui.dp(6),ui.dp(6));platform.setCompoundDrawables(dot,null,null,null);platform.setCompoundDrawablePadding(ui.dp(6));}
 private void connected(boolean value){chip(value?"学习通 · 已验证":account.configured()?"学习通 · 已配置":"学习通 · 未配置",value);}
 private void busy(boolean value){busy=value;controls();}
 private void refreshing(boolean value){refreshing=value;if(value){banner.setVisibility(GONE);feedbackTimer.removeCallbacks(hideNotice);chip("学习通 · 同步中",false);}controls();}
 private void controls(){platform.setEnabled(!busy&&!refreshing);if(settings!=null)settings.setEnabled(!busy&&localReady);if(add!=null)add.setEnabled(!busy&&localReady);if(busy||refreshing)pauseRefresh();else scheduleRefresh();progress.setVisibility(busy||refreshing?VISIBLE:GONE);}
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
   worker.execute(()->{try{connection.login(name,secret);if(!current(token))return;account.save(name,secret);activity.runOnUiThread(()->{if(current(token)){connected(true);dialog.dismiss();busy(false);feedback("登录验证成功，密码配置已加密保存。");snapshot=null;localReady=false;render();loadLocal();}});connection.close();}catch(Exception e){activity.runOnUiThread(()->{if(current(token)){busy(false);dialog.setCancelable(true);username.setEnabled(true);password.setEnabled(true);dialog.getButton(-1).setEnabled(true);dialog.getButton(-2).setEnabled(true);feedback.setText("登录验证或保存失败，未更新配置。请核对账号密码和网络；验证码或机构认证需先在学习通完成。");feedback("配置未更新，原配置及作业保留。");}});}});
  });
 }
 private EditText field(LinearLayout form,String label,String hint,boolean secret){TextView caption=ui.text(label,13,ui.MUTED);caption.setTypeface(null,Typeface.BOLD);caption.setPadding(0,ui.dp(16),0,ui.dp(8));form.addView(caption);EditText input=new EditText(getContext());input.setTextSize(15);input.setSingleLine(true);input.setHint(hint);input.setContentDescription(label);input.setTextColor(ui.INK);input.setHintTextColor(ui.MUTED);input.setPadding(ui.dp(16),0,ui.dp(16),0);android.graphics.drawable.GradientDrawable fieldSurface=ui.bg(ui.SURFACE,14);fieldSurface.setStroke(ui.dp(1),ui.DIVIDER);input.setBackground(fieldSurface);input.setInputType(InputType.TYPE_CLASS_TEXT|(secret?InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_TEXT_VARIATION_NORMAL));input.setSaveEnabled(false);input.setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_NO);form.addView(input,new LayoutParams(-1,ui.dp(54)));return input;}
 private void refresh(){refresh(new ChaoxingClient());}
 private void refresh(ChaoxingClient connection){
  if(busy||refreshing||closed){connection.close();return;}if(!account.configured()){connection.close();configure();return;}refreshing(true);retryAfter=System.currentTimeMillis()+60000;int token=++generation;feedback("正在使用保存的配置登录学习通…");if(client!=null)client.close();client=connection;
  worker.execute(()->{try{ChaoxingAccount.Saved saved=account.load();ChaoxingCache cache=new ChaoxingCache(getContext(),saved.account);ChaoxingClient.Result before=ChaoxingCache.copy(snapshot==null||!platformReady?cache.load():snapshot);before.lastAttempt=System.currentTimeMillis();if(!current(token))return;cache.save(before);activity.runOnUiThread(()->{if(current(token)){snapshot=before;platformReady=true;network.execute(()->{try{connection.login(saved.account,saved.password);activity.runOnUiThread(()->{if(current(token))connected(true);});sync(connection,token,saved.account);}catch(Exception e){activity.runOnUiThread(()->{if(current(token)){refreshing(false);connected(false);feedback("更新未成功，请检查网络或重新配置密码。已有作业保留。");}});}finally{connection.close();}});}});}catch(Exception e){activity.runOnUiThread(()->{if(current(token)){refreshing(false);feedback("本机缓存暂时无法保存，已有任务保留，请重试。");}});connection.close();}});
 }
 private void loadLocal(){
  busy(true);int token=++generation;worker.execute(()->{try{ChaoxingClient.Result local=new ChaoxingCache(getContext(),"").load(),platform=null;boolean readable=true;try{if(account.configured())platform=new ChaoxingCache(getContext(),account.load().account).load();}catch(Exception e){readable=false;}ChaoxingClient.Result savedPlatform=platform;boolean ready=readable;activity.runOnUiThread(()->{if(current(token)){localTasks=local==null?new ChaoxingClient.Result():local;snapshot=savedPlatform==null?new ChaoxingClient.Result():savedPlatform;localReady=true;platformReady=ready;busy(false);feedback(!ready?"学习通缓存暂时无法读取，手动任务仍可用，原文件已保留。":account.configured()?"本机已保存 "+allTasks().size()+" 份任务":"可直接添加任务，配置学习通后可同步作业。");render();readingNotes();}});}catch(Exception e){activity.runOnUiThread(()->{if(current(token)){busy(false);feedback("本机任务暂时无法读取，原文件未删除，请重试。");}});}});
 }
 private List<ChaoxingClient.Work> allTasks(){List<ChaoxingClient.Work> items=new ArrayList<>(localTasks.items);if(snapshot!=null)items.addAll(snapshot.items);return items;}
 private void sync(ChaoxingClient connection,int token,String username){
  try{
   ChaoxingClient.Result start=snapshot;Set<String> disabled=start==null?Collections.emptySet():new LinkedHashSet<>(start.disabledCourses);Set<String> pendingDetails=new HashSet<>();if(start!=null)for(ChaoxingClient.Work work:start.items)if(!work.completed()&&!work.deadlineEdited&&work.deadline.equals("截止时间未知")&&!work.workKey.isEmpty())pendingDetails.add(work.courseKey+":"+work.workKey);ChaoxingClient.Result fresh=connection.sync(message->activity.runOnUiThread(()->{if(current(token))feedback(message);}),disabled,pendingDetails);if(!current(token))return;
   // Serialize commits with local edits and merge against the cache after the request returns.
   worker.execute(()->{if(!current(token))return;try{ChaoxingCache cache=new ChaoxingCache(getContext(),username);ChaoxingClient.Result before=cache.load();ChaoxingClient.Result result=ChaoxingCache.merge(before,fresh);int added=0,updated=0;for(ChaoxingClient.Work item:result.items){ChaoxingClient.Work previous=before==null?null:ChaoxingCache.find(before.items,item);if(previous==null)added++;else if(!previous.title.equals(item.title)||!previous.deadline.equals(item.deadline)||!previous.status.equals(item.status))updated++;}int additions=added,updates=updated;boolean persisted;try{cache.save(result);persisted=true;}catch(Exception e){persisted=false;}boolean saved=persisted;
    activity.runOnUiThread(()->{if(current(token)){snapshot=result;refreshing(false);String lastSyncLabel="学习通 · 已同步 "+new java.text.SimpleDateFormat("HH:mm",Locale.CHINA).format(new Date());chip(lastSyncLabel,true);if(additions>0||updates>0||!saved)feedback("同步完成：新增 "+additions+" · 更新 "+updates+(saved?"":" · 本机保存失败，退出后可能无法保留"));render();readingNotes();}});
   }catch(Exception e){activity.runOnUiThread(()->{if(current(token)){refreshing(false);feedback("刷新结果暂时无法保存，原有任务和本地修改已保留。");}});}});
  }catch(Exception e){activity.runOnUiThread(()->{if(current(token)){refreshing(false);feedback("作业读取失败（"+ChaoxingClient.describe(e)+"）。原有作业和密码配置已保留。");}});}finally{connection.close();}
 }
 void pauseRefresh(){timer.removeCallbacks(autoRefresh);}
 void scheduleRefresh(){pauseRefresh();if(closed||busy||refreshing||!localReady||!platformReady||snapshot==null||!account.configured()||!activity.taskRefreshActive())return;long now=System.currentTimeMillis();timer.postDelayed(autoRefresh,Math.max(Math.max(0,retryAfter-now),TaskRefresh.delay(snapshot.lastAttempt,snapshot.intervalMinutes,now)));}
 private void autoRefresh(){if(closed)return;if(!activity.taskRefreshActive()||configuration!=null){timer.postDelayed(autoRefresh,60000);return;}if(!busy&&!refreshing&&localReady){if(TaskRefresh.delay(snapshot.lastAttempt,snapshot.intervalMinutes,System.currentTimeMillis())==0)refresh();else scheduleRefresh();}}
 private void saveChange(java.util.function.Consumer<ChaoxingClient.Result> change,String message){saveChange(change,message,false);}
 private void saveChange(java.util.function.Consumer<ChaoxingClient.Result> change,String message,boolean local){
  saveChange(change,message,local,()->{});
 }
 private void saveChange(java.util.function.Consumer<ChaoxingClient.Result> change,String message,boolean local,Runnable savedAction){
  saveChange(change,message,local,savedAction,this::render);
 }
 private void saveChange(java.util.function.Consumer<ChaoxingClient.Result> change,String message,boolean local,Runnable savedAction,Runnable refreshUi){
  if(busy||closed)return;busy(true);int token=generation;feedback("正在保存任务设置…");worker.execute(()->{if(!current(token))return;try{ChaoxingCache cache=new ChaoxingCache(getContext(),local?"":account.load().account);ChaoxingClient.Result saved=cache.load(),next=saved==null?ChaoxingCache.copy(local?localTasks:snapshot):saved;change.accept(next);cache.save(next);activity.runOnUiThread(()->{if(current(token)){if(local)localTasks=next;else snapshot=next;busy(false);feedback(message);if(getParent()==null)Toast.makeText(activity,message,Toast.LENGTH_LONG).show();refreshUi.run();savedAction.run();}});}catch(Exception e){activity.runOnUiThread(()->{if(current(token)){busy(false);feedback("保存失败，原有任务和设置已保留，请重新打开后重试。");if(getParent()==null)Toast.makeText(activity,"保存失败，原有任务和设置已保留。",Toast.LENGTH_LONG).show();}});}});
 }
 private void confirmDelete(ChaoxingClient.Work work){
  if(busy||closed||deleteDialog!=null)return;
  AlertDialog dialog=new AlertDialog.Builder(activity,R.style.SecondaryDialog).setTitle("删除任务？").setMessage(work.title+"\n任务将移入本机删除历史，可恢复；不会删除学校平台上的作业。").setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->delete(work)).create();deleteDialog=dialog;dialog.setOnDismissListener(d->deleteDialog=null);dialog.show();ui.styleDialog(dialog,true);
 }
 private void removeTask(ChaoxingClient.Work work){
  LinearLayout list=lists[0];TaskRow removed=null;for(int i=0;i<list.getChildCount();i++)if(list.getChildAt(i) instanceof LinearLayout){LinearLayout group=(LinearLayout)list.getChildAt(i);for(int j=0;j<group.getChildCount();j++)if(group.getChildAt(j) instanceof TaskRow){TaskRow row=(TaskRow)group.getChildAt(j);if(ChaoxingCache.find(Collections.singletonList(row.work),work)!=null)removed=row;}}
  if(removed==null||!list.isLaidOut()||!android.animation.ValueAnimator.areAnimatorsEnabled()){render();return;}
  // A pending expansion on this root would otherwise swallow the removal listener.
  android.transition.TransitionManager.endTransitions(list);
  int token=renderGeneration;android.transition.Transition transition=taskTransition(false);transition.addListener(new android.transition.Transition.TransitionListener(){public void onTransitionStart(android.transition.Transition t){}public void onTransitionEnd(android.transition.Transition t){if(!closed&&token==renderGeneration)render();}public void onTransitionCancel(android.transition.Transition t){}public void onTransitionPause(android.transition.Transition t){}public void onTransitionResume(android.transition.Transition t){}});
  android.transition.TransitionManager.beginDelayedTransition(list,transition);LinearLayout group=(LinearLayout)removed.getParent();group.removeView(removed);
  if(group.getChildCount()==0){int index=list.indexOfChild(group);list.removeViewAt(index-1);list.removeView(group);}else ((TaskRow)group.getChildAt(0)).divider.setVisibility(GONE);
  if(expandedRow==removed){expandedRow=null;expandedTask=null;}
 }
 private void delete(ChaoxingClient.Work work){
  if(busy||closed||snapshot==null)return;saveChange(next->{ChaoxingClient.Work removed=ChaoxingCache.find(next.items,work);if(removed!=null)next.items.remove(removed);else removed=ChaoxingCache.copy(work);if(ChaoxingCache.find(next.deleted,work)==null){removed.deletedAt=System.currentTimeMillis();next.deleted.add(removed);}},"任务已删除，可在总设置的数据管理中恢复。",work.local(),()->undoBar(work),()->removeTask(work));
 }
 void clearData(){
  if(busy||closed){Toast.makeText(activity,"任务正在加载或保存，请稍后重试。",Toast.LENGTH_SHORT).show();return;}
  AlertDialog dialog=new AlertDialog.Builder(activity,R.style.SecondaryDialog).setTitle("删除全部任务数据？").setMessage("将删除所有手动任务、所有学习通账号的本机任务缓存、删除历史和任务设置，并退出学习通账号。学校平台上的作业不会删除，课表和快递不受影响。此操作无法撤销。").setNegativeButton("取消",null).setPositiveButton("全部删除",(d,w)->{
   if(busy||closed)return;int token=++generation;if(client!=null)client.close();busy(true);refreshing(false);hideSnack();
   worker.execute(()->{String message;try{account.clear();ChaoxingCache.clearAll(getContext());message="任务数据已删除，学习通账号已退出。";}catch(Exception e){message="部分任务数据未能删除，请重试；已删除的数据无法恢复。";}String result=message;activity.runOnUiThread(()->{if(current(token)){connected(false);loadLocal();Toast.makeText(activity,result,Toast.LENGTH_LONG).show();}});});
  }).create();dialog.show();ui.styleDialog(dialog,true);
 }
 void settings(){
  if(busy||closed||!localReady)return;MainActivity.NavigationOverlay sheet=activity.new NavigationOverlay(true,"任务设置",settings);
  activity.menuItem(sheet,"学习通账号","chaoxing-connect",this::configure);ui.popupDivider(sheet.body);
  View courses=activity.menuItem(sheet,"加载的课程","task-course-settings",this::courses);courses.setEnabled(account.configured()&&platformReady);
  View interval=activity.menuItem(sheet,"自动刷新","task-refresh-settings",this::interval);interval.setEnabled(account.configured()&&platformReady);
 }

 private void courses(){
  if(busy||closed)return;MainActivity.NavigationOverlay sheet=activity.new NavigationOverlay(false,"加载的课程");sheet.body.setTag("task-course-selection");
  TextView intro=ui.text("关闭后停止加载并隐藏该课程任务，已保存的任务仍保留。重新开启后可点击刷新。",14,ui.MUTED);intro.setPadding(0,0,0,ui.dp(12));sheet.body.addView(intro);
  Set<String> disabled=new LinkedHashSet<>(snapshot.disabledCourses);
  if(snapshot.courses.isEmpty())sheet.body.addView(ui.text("请先刷新一次，获取可选择的课程。",14,ui.MUTED));
  Map<String,Integer> names=new HashMap<>();for(ChaoxingClient.Subject course:snapshot.courses)names.put(course.name,names.getOrDefault(course.name,0)+1);
  for(ChaoxingClient.Subject course:snapshot.courses){CheckBox choice=new CheckBox(getContext());choice.setText(course.name+(names.get(course.name)>1?"（"+course.key()+"）":""));choice.setTextColor(ui.INK);choice.setTextSize(15);choice.setMinHeight(ui.dp(48));choice.setTag("task-course-"+course.key());choice.setChecked(!disabled.contains(course.key()));choice.setOnCheckedChangeListener((button,checked)->{if(checked)disabled.remove(course.key());else disabled.add(course.key());});sheet.body.addView(choice,new LayoutParams(-1,-2));}
  taskButton(sheet.body,"保存",()->{if(busy)return;sheet.close(false);saveChange(next->{next.disabledCourses.clear();next.disabledCourses.addAll(disabled);},"课程加载设置已保存。");}).setTag("task-courses-save");
 }
 void history(){
  if(busy||closed||!localReady){Toast.makeText(activity,"任务正在加载或保存，请稍后重试。",Toast.LENGTH_SHORT).show();return;}MainActivity.NavigationOverlay sheet=activity.new NavigationOverlay(false,"删除历史");sheet.body.setTag("task-history");
  if(snapshot.deleted.isEmpty()&&localTasks.deleted.isEmpty())sheet.body.addView(ui.text("还没有删除记录。",14,ui.MUTED));
  List<ChaoxingClient.Work> removed=new ArrayList<>(snapshot.deleted);removed.addAll(localTasks.deleted);removed.sort((a,b)->Long.compare(b.deletedAt,a.deletedAt));for(ChaoxingClient.Work work:removed){String when=work.deletedAt==0?"":new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date(work.deletedAt));
   activity.option(sheet.body,R.drawable.ic_tasks,work.title,work.course+" · "+when+" · 点击恢复",()->{if(busy)return;sheet.close(false);saveChange(next->{ChaoxingClient.Work restored=ChaoxingCache.find(next.deleted,work);if(restored==null)throw new IllegalStateException("Task changed");next.deleted.remove(restored);restored.deletedAt=0;if(ChaoxingCache.find(next.items,restored)==null)next.items.add(restored);},"任务已恢复；若该课程关闭了加载，开启后即可显示。",work.local());}).setTag("task-restore");
  }
 }
 private void taskActions(ChaoxingClient.Work work){
  if(busy||closed)return;MainActivity.NavigationOverlay sheet=activity.new NavigationOverlay(false,"任务操作");activity.option(sheet.body,R.drawable.ic_notes,"编辑",work.title,()->{sheet.close(false);editTask(work);}).setTag("task-edit");activity.option(sheet.body,R.drawable.ic_tasks,"删除","移入删除历史，可恢复",()->{sheet.close(false);confirmDelete(work);}).setTag("task-delete");
 }
 private AutoCompleteTextView categoryField(LinearLayout body){TextView label=ui.text("课程 / 分类",13,ui.MUTED);label.setTypeface(null,Typeface.BOLD);label.setPadding(0,ui.dp(16),0,ui.dp(8));body.addView(label);AutoCompleteTextView input=new AutoCompleteTextView(getContext());input.setHint("选择课程或输入分类");input.setTextSize(15);input.setTextColor(ui.INK);input.setHintTextColor(ui.MUTED);android.graphics.drawable.GradientDrawable fieldSurface=ui.bg(ui.SURFACE,14);fieldSurface.setStroke(ui.dp(1),ui.DIVIDER);input.setBackground(fieldSurface);input.setPadding(ui.dp(16),0,ui.dp(12),0);input.setSingleLine(true);input.setContentDescription("课程 / 分类");java.util.Set<String> names=new LinkedHashSet<>();if(snapshot!=null)for(ChaoxingClient.Subject course:snapshot.courses)names.add(course.name);for(ChaoxingClient.Work work:allTasks())if(!work.course.isEmpty())names.add(work.course);names.add("手动任务");ArrayAdapter<String> adapter=new ArrayAdapter<>(getContext(),android.R.layout.simple_dropdown_item_1line,new ArrayList<>(names));input.setAdapter(adapter);input.setThreshold(0);input.setDropDownBackgroundDrawable(ui.popupSurface());input.setOnClickListener(v->adapter.getFilter().filter(null,count->input.showDropDown()));android.graphics.drawable.Drawable arrow=activity.getDrawable(R.drawable.ic_expand).mutate();arrow.setTint(ui.MUTED);arrow.setBounds(0,0,ui.dp(20),ui.dp(20));input.setCompoundDrawables(null,null,arrow,null);body.addView(input,new LayoutParams(-1,ui.dp(54)));return input;}
 private void editTask(ChaoxingClient.Work work){
  if(busy||closed||!localReady)return;boolean local=work==null||work.local();MainActivity.NavigationOverlay sheet=activity.new NavigationOverlay(false,work==null?"添加任务":"编辑任务");sheet.body.setTag("task-editor");sheet.fitContent=true;sheet.body.addView(ui.text(work==null?"手动添加的任务只保存在本机，不会被同步覆盖。":"本地编辑会保留，刷新不会覆盖。",13,ui.MUTED));
  EditText title=field(sheet.body,"任务名称","例如：第五章课后习题",false);title.setTag("task-editor-title");title.setText(work==null?"":work.title);
  EditText course=null;if(local){course=categoryField(sheet.body);course.setTag("task-editor-course");course.setText(work==null?"":work.course);}else sheet.body.addView(ui.text(work.course,14,ui.MUTED));
  EditText deadline=field(sheet.body,"截止时间","选填，点击选择日期与时间",false);deadline.setTag("task-editor-deadline");deadline.setText(work==null||work.deadline.equals("截止时间未知")||work.deadline.equals("未提供截止时间")?"":work.deadline);
  deadline.setFocusable(false);deadline.setInputType(InputType.TYPE_NULL);deadline.setOnClickListener(v->pickDeadline(deadline));smallAction(sheet.body,"清除截止时间",()->deadline.setText(""));EditText category=course;
  sheet.body.addView(ui.text("截止时间可留空；完成状态请回列表点行首圆钮切换。",12,ui.MUTED));
  Button save=taskButton(sheet.body,work==null?"添加":"保存",()->{if(busy)return;String name=title.getText().toString().trim(),time=deadline.getText().toString().trim(),group=category==null?work.course:category.getText().toString().trim();if(name.isEmpty()||name.length()>200){title.setError("请输入 1–200 个字的任务名称");return;}if(category!=null&&group.length()>100){category.setError("分类最多 100 个字");return;}if(!time.isEmpty()&&(work==null||!time.equals(work.deadline)))try{java.time.LocalDateTime.parse(time,java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(java.time.format.ResolverStyle.STRICT));}catch(java.time.DateTimeException e){deadline.setError("请使用完整日期和时间，例如 2026-10-21 23:59");return;}
   String state=work==null?"未完成":work.status;ChaoxingClient.Work item=new ChaoxingClient.Work(group.isEmpty()?"手动任务":group,name,state,"");item.deadline=time.isEmpty()?"截止时间未知":time;item.deadlineEdited=work!=null&&(work.deadlineEdited||!item.deadline.equals(work.deadline)&&!(item.deadline.equals("截止时间未知")&&work.deadline.equals("未提供截止时间")));item.courseKey=work==null?"local":work.courseKey;item.workKey=work==null?"local:"+UUID.randomUUID():work.workKey;item.edited=!local;item.originalTitle=local?"":work.originalTitle.isEmpty()?work.title:work.originalTitle;
   saveChange(next->{if(work==null)next.items.add(item);else{ChaoxingClient.Work current=ChaoxingCache.find(next.items,work);if(current==null){if(ChaoxingCache.find(next.deleted,work)!=null)throw new IllegalStateException("Task deleted");next.items.add(item);}else{item.courseKey=current.courseKey;item.workKey=current.workKey;next.items.set(next.items.indexOf(current),item);}}},work==null?"任务已添加。":"任务修改已保存。",local,()->{sheet.close(false);});
  });save.setTag("task-editor-save");sheet.body.removeView(save);LinearLayout footer=new LinearLayout(getContext());footer.setGravity(Gravity.CENTER_VERTICAL);footer.setPadding(ui.dp(16),ui.dp(8),ui.dp(16),ui.dp(24));Button cancel=ui.action(footer,"取消",()->sheet.close(false));cancel.setBackgroundTintList(null);cancel.setBackground(ui.bg(ui.SOFT,24));cancel.setTextColor(ui.INK);LayoutParams cancelSize=new LayoutParams(0,-2,1);cancelSize.rightMargin=ui.dp(4);cancel.setLayoutParams(cancelSize);LayoutParams saveSize=new LayoutParams(0,-2,1);saveSize.leftMargin=ui.dp(4);footer.addView(save,saveSize);sheet.panel.addView(footer);
 }
 private void interval(){
  if(busy||closed)return;MainActivity.NavigationOverlay sheet=activity.new NavigationOverlay(false,"自动刷新");sheet.body.setTag("task-refresh-interval");TextView intro=ui.text("软件使用期间按间隔刷新，每天第一次打开时也会自动刷新。失败时保留已有任务。",14,ui.MUTED);sheet.body.addView(intro);
  int[] values={0,30,60,180};String[] labels={"仅每天首次打开","每 30 分钟","每小时","每 3 小时"};
  if(java.util.Arrays.stream(values).noneMatch(v->v==snapshot.intervalMinutes))sheet.body.addView(ui.text("当前为旧设置：每 "+snapshot.intervalMinutes+" 分钟；选择下方档位后替换。",13,ui.MUTED));
  for(int i=0;i<values.length;i++){int value=values[i];RadioButton option=new RadioButton(getContext());option.setText(labels[i]);option.setChecked(snapshot.intervalMinutes==value);option.setTextColor(ui.INK);option.setMinHeight(ui.dp(52));option.setTag("task-interval-"+value);sheet.body.addView(option);option.setOnClickListener(v->saveChange(next->next.intervalMinutes=value,"自动刷新已设置为"+labels[java.util.Arrays.binarySearch(values,value)]+"。",false,()->sheet.close(false)));}
 }
 void saveState(Bundle state){state.putInt("filter",0);int[] positions=new int[scrolls.length];for(int i=0;i<positions.length;i++)positions[i]=renderRestoreY!=null&&renderRestoreY[i]>=0?renderRestoreY[i]:scrolls[i].getScrollY();state.putIntArray("filterScrollY",restoredScrollY==null?positions:restoredScrollY);}
 void close(){closed=true;android.transition.TransitionManager.endTransitions(lists[0]);pauseRefresh();generation++;renderGeneration++;if(renderNext!=null)activity.getWindow().getDecorView().removeCallbacks(renderNext);renderNext=null;if(configuration!=null)configuration.dismiss();if(deadlineDialog!=null)deadlineDialog.dismiss();if(deleteDialog!=null)deleteDialog.dismiss();if(passwordField!=null)passwordField.setText("");if(client!=null)client.close();feedbackTimer.removeCallbacksAndMessages(null);worker.shutdownNow();network.shutdownNow();}
}
