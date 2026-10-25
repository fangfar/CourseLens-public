package cn.courselens;

import android.view.*;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import java.util.concurrent.ExecutorService;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.util.ReflectionHelpers.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
public class TaskPageDesignTest {
 @Test public void emptyPageOffersLocalTaskAndNotesToggleResets()throws Exception{
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();activity.findViewById(android.R.id.content).findViewWithTag("nav-tasks").performClick();TaskPage page=activity.findViewById(android.R.id.content).findViewWithTag("tasks-page");
   try{
    ExecutorService worker=getField(page,"worker");worker.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idle();
    callInstanceMethod(page,"render");layout(activity.findViewById(android.R.id.content),activity);TextView action=page.findViewWithTag("task-empty-action");assertEquals("添加任务",action.getText().toString());snapshot(activity.findViewById(android.R.id.content),"task-empty");
    ChaoxingClient.Result result=new ChaoxingClient.Result();result.warnings.add("合成课程读取失败，原任务保留");setField(page,"snapshot",result);callInstanceMethod(page,"readingNotes");
    TextView expand=page.findViewWithTag("task-banner-expand");View details=page.findViewWithTag("task-read-notes");assertEquals(View.GONE,details.getVisibility());
    expand.performClick();assertEquals("收起",expand.getText().toString());assertEquals(View.VISIBLE,details.getVisibility());
    expand.performClick();assertEquals("展开",expand.getText().toString());assertEquals(View.GONE,details.getVisibility());
    expand.performClick();callInstanceMethod(page,"readingNotes");assertEquals("展开",expand.getText().toString());assertEquals(View.GONE,details.getVisibility());
    action.performClick();assertNotNull(activity.findViewById(android.R.id.content).findViewWithTag("task-editor-title"));
   }finally{page.close();}
  }
 }
 @Test public void taskRowAnnouncesExpandedActions(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   TaskPage page=new TaskPage(controller.get(),null);
   try{
    LinearLayout card=new LinearLayout(controller.get());card.setOrientation(LinearLayout.VERTICAL);ChaoxingClient.Work work=new ChaoxingClient.Work("合成课程","合成任务","未完成","");
    callInstanceMethod(page,"addWork",ClassParameter.from(LinearLayout.class,card),ClassParameter.from(ChaoxingClient.Work.class,work),ClassParameter.from(int.class,0));
    TextView deadline=card.findViewWithTag("task-work-deadline");assertEquals("截止时间未知，可补录",deadline.getContentDescription());assertEquals(View.GONE,((View)deadline.getParent()).getVisibility());
    work.deadline="10-29 10:26";LinearLayout partial=new LinearLayout(controller.get());callInstanceMethod(page,"addWork",ClassParameter.from(LinearLayout.class,partial),ClassParameter.from(ChaoxingClient.Work.class,work),ClassParameter.from(int.class,0));assertTrue(((TextView)partial.findViewWithTag("task-work-deadline")).getText().toString().contains("10-29 10:26"));assertEquals(View.VISIBLE,((View)partial.findViewWithTag("task-work-deadline").getParent()).getVisibility());
    View content=(View)card.findViewWithTag("task-work-title").getParent();content.performClick();assertTrue(content.getContentDescription().toString().contains("收起"));content.performClick();assertTrue(content.getContentDescription().toString().contains("展开"));
   }finally{page.close();}
  }
 }

 @Test public void taskCardsRenderAtNormalAndLargeFont()throws Exception{
  for(float font:new float[]{1f,2f}){
   RuntimeEnvironment.setQualifiers(font==1f?"w380dp-h800dp-notnight":"w320dp-h640dp-night");RuntimeEnvironment.setFontScale(font);
   try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
    MainActivity activity=controller.get();View root=activity.findViewById(android.R.id.content);root.findViewWithTag("nav-tasks").performClick();TaskPage page=root.findViewWithTag("tasks-page");
    ExecutorService worker=getField(page,"worker");worker.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idle();
    ChaoxingClient.Result data=new ChaoxingClient.Result();ChaoxingClient.Work work=new ChaoxingClient.Work("合成课程","第五章课后练习与实验报告","未完成","");work.deadline=java.time.LocalDateTime.now().plusDays(2).withSecond(0).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));data.items.add(work);data.items.add(new ChaoxingClient.Work("合成课程","待补录截止时间的任务","未完成",""));data.items.add(new ChaoxingClient.Work("合成课程","已经完成的任务","已完成",""));setField(page,"snapshot",data);
    layout(root,activity);callInstanceMethod(page,"render");shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1));layout(root,activity);
    assertNull(root.findViewWithTag("task-complete-toggle"));assertNull(root.findViewWithTag("task-filter-0"));assertNull(root.findViewWithTag("task-filter-pager"));LinearLayout[] lists=getField(page,"lists");assertEquals(1,lists.length);assertEquals(2,countRows(lists[0]));assertEquals(3,data.items.size());assertNotNull(root.findViewWithTag("task-deadline-hero"));assertNotNull(root.findViewWithTag("task-work-title"));snapshot(root,font==1f?"task-cards":"task-dark-large");
    LinearLayout group=root.findViewWithTag("task-course-group");assertNotNull(group.getBackground());assertTrue(group.getClipToOutline());assertEquals(2,group.getChildCount());View first=group.getChildAt(0),second=group.getChildAt(1);assertNull(first.getBackground());assertEquals(View.GONE,first.findViewWithTag("task-divider").getVisibility());assertEquals(View.VISIBLE,second.findViewWithTag("task-divider").getVisibility());
    View firstContent=(View)first.findViewWithTag("task-work-title").getParent(),secondContent=(View)second.findViewWithTag("task-work-title").getParent();View firstActions=first.findViewWithTag("task-inline-actions"),secondActions=second.findViewWithTag("task-inline-actions");assertEquals(View.GONE,firstActions.getVisibility());int collapsed=group.getHeight();if(font>1f){ScrollView[] scrolls=getField(page,"scrolls");scrolls[0].scrollTo(0,group.getTop()-(int)(36*activity.getResources().getDisplayMetrics().density));layout(root,activity);snapshot(root,"task-dark-large");}
    firstContent.performClick();settleActions(first);layout(root,activity);assertEquals(View.VISIBLE,firstActions.getVisibility());assertTrue(group.getHeight()>collapsed);assertTrue(firstActions.getBottom()<=first.getHeight());assertSame(lists[0],group.getParent());assertNull(first.findViewWithTag("task-work-status"));snapshot(root,font==1f?"task-cards-expanded":"task-dark-large-expanded");
    secondContent.performClick();settleActions(first);settleActions(second);assertEquals(View.GONE,firstActions.getVisibility());assertEquals(View.VISIBLE,secondActions.getVisibility());secondContent.performClick();settleActions(first);settleActions(second);assertEquals(View.GONE,secondActions.getVisibility());
    firstContent.performClick();callInstanceMethod(page,"render");shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1));layout(root,activity);assertEquals(View.VISIBLE,root.findViewWithTag("task-inline-actions").getVisibility());
   }
  }
 }
 @Test public void deadlineHeroAcceptsMonthDayWithoutInventingCountdown()throws Exception{
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   TaskPage page=new TaskPage(controller.get(),null);
   try{
    ExecutorService worker=getField(page,"worker");worker.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idle();
    ChaoxingClient.Result data=new ChaoxingClient.Result();ChaoxingClient.Work later=new ChaoxingClient.Work("合成课程","较晚的月日任务","未完成","");later.deadline="11-02 12:00";ChaoxingClient.Work earlier=new ChaoxingClient.Work("合成课程","较早的月日任务","未完成","");earlier.deadline="10-29 10:25";data.items.add(later);data.items.add(earlier);setField(page,"snapshot",data);
    LinearLayout list=new LinearLayout(controller.get());callInstanceMethod(page,"addDeadlineHero",ClassParameter.from(LinearLayout.class,list));LinearLayout hero=list.findViewWithTag("task-deadline-hero");assertNotNull(hero);assertEquals(earlier.title,((TextView)hero.getChildAt(1)).getText().toString());assertEquals("10-29 10:25 截止",((TextView)hero.getChildAt(2)).getText().toString());for(int i=0;i<hero.getChildCount();i++){assertFalse(hero.getChildAt(i) instanceof ProgressBar);if(hero.getChildAt(i) instanceof TextView)assertFalse(((TextView)hero.getChildAt(i)).getText().toString().contains("还剩"));}
    ChaoxingClient.Work full=new ChaoxingClient.Work("合成课程","完整日期任务","未完成","");full.deadline="2026-12-01 12:00";data.items.add(full);list.removeAllViews();callInstanceMethod(page,"addDeadlineHero",ClassParameter.from(LinearLayout.class,list));hero=list.findViewWithTag("task-deadline-hero");assertEquals(earlier.title,((TextView)hero.getChildAt(1)).getText().toString());data.items.clear();data.items.add(full);list.removeAllViews();callInstanceMethod(page,"addDeadlineHero",ClassParameter.from(LinearLayout.class,list));hero=list.findViewWithTag("task-deadline-hero");assertEquals("12-01 12:00 截止",((TextView)hero.getChildAt(2)).getText().toString());assertTrue(hero.getChildAt(4) instanceof ProgressBar);
    data.items.clear();ChaoxingClient.Work invalid=new ChaoxingClient.Work("合成课程","无有效时间","未完成","");invalid.deadline="02-30 12:00";data.items.add(invalid);list.removeAllViews();callInstanceMethod(page,"addDeadlineHero",ClassParameter.from(LinearLayout.class,list));assertNull(list.findViewWithTag("task-deadline-hero"));
   }finally{page.close();}
  }
 }
 @Test public void expansionIsExclusiveAcrossCoursesAndDeleteCanBeCancelled()throws Exception{
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
   TaskPage page=new TaskPage(controller.get(),null);
   try{
    ExecutorService worker=getField(page,"worker");worker.submit(()->{}).get();shadowOf(android.os.Looper.getMainLooper()).idle();
    LinearLayout first=new LinearLayout(controller.get()),second=new LinearLayout(controller.get());ChaoxingClient.Work a=new ChaoxingClient.Work("课程甲","任务甲","未完成",""),b=new ChaoxingClient.Work("课程乙","任务乙","未完成","");
    callInstanceMethod(page,"addWork",ClassParameter.from(LinearLayout.class,first),ClassParameter.from(ChaoxingClient.Work.class,a),ClassParameter.from(int.class,0));callInstanceMethod(page,"addWork",ClassParameter.from(LinearLayout.class,second),ClassParameter.from(ChaoxingClient.Work.class,b),ClassParameter.from(int.class,0));
    View ac=(View)first.findViewWithTag("task-work-title").getParent(),bc=(View)second.findViewWithTag("task-work-title").getParent();for(int i=0;i<5;i++){ac.performClick();bc.performClick();settleActions(first.getChildAt(0));settleActions(second.getChildAt(0));assertEquals(View.GONE,first.findViewWithTag("task-inline-actions").getVisibility());assertEquals(View.VISIBLE,second.findViewWithTag("task-inline-actions").getVisibility());}
    ViewGroup actions=second.findViewWithTag("task-inline-actions");assertEquals(2,actions.getChildCount());actions.getChildAt(1).performClick();android.app.AlertDialog dialog=getField(page,"deleteDialog");assertTrue(dialog.isShowing());dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick();shadowOf(android.os.Looper.getMainLooper()).idle();assertNull(getField(page,"deleteDialog"));assertEquals(View.VISIBLE,actions.getVisibility());assertSame(b,getField(page,"expandedTask"));assertFalse((boolean)getField(page,"busy"));
   }finally{page.close();}
  }
 }
 // Layout assertions describe settled states; the device suite samples the intervening heights.
 private void settleActions(View row){Motion.Progress expansion=getField(row,"expansion");android.animation.ValueAnimator animator=getField(expansion,"animator");if(animator!=null)animator.end();shadowOf(android.os.Looper.getMainLooper()).idle();}
 private int countRows(View view){int count="task-work".equals(view.getTag())?1:0;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)count+=countRows(((ViewGroup)view).getChildAt(i));return count;}
 private void layout(View root,android.app.Activity activity){android.util.DisplayMetrics dm=activity.getResources().getDisplayMetrics();root.measure(View.MeasureSpec.makeMeasureSpec(dm.widthPixels,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(dm.heightPixels,View.MeasureSpec.EXACTLY));root.layout(0,0,dm.widthPixels,dm.heightPixels);}
 private void snapshot(View root,String name)throws Exception{String directory=System.getProperty("courselens.parcelPreviewDir");if(directory==null)return;java.io.File folder=new java.io.File(directory);folder.mkdirs();android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(root.getWidth(),root.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);root.draw(new android.graphics.Canvas(bitmap));try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(folder,name+".png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}
}
