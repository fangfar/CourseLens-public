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
    TextView deadline=card.findViewWithTag("task-work-deadline");assertEquals("截止时间未知，可补录",deadline.getContentDescription());
    work.deadline="10-29 10:26";LinearLayout partial=new LinearLayout(controller.get());callInstanceMethod(page,"addWork",ClassParameter.from(LinearLayout.class,partial),ClassParameter.from(ChaoxingClient.Work.class,work),ClassParameter.from(int.class,0));assertTrue(((TextView)partial.findViewWithTag("task-work-deadline")).getText().toString().contains("10-29 10:26"));
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
    assertNull(root.findViewWithTag("task-filter-0"));assertNull(root.findViewWithTag("task-filter-pager"));LinearLayout[] lists=getField(page,"lists");assertEquals(1,lists.length);assertEquals(3,countRows(lists[0]));assertNotNull(root.findViewWithTag("task-deadline-hero"));assertNotNull(root.findViewWithTag("task-work-title"));snapshot(root,font==1f?"task-cards":"task-dark-large");
   }
  }
 }
 private int countRows(View view){int count="task-work".equals(view.getTag())?1:0;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)count+=countRows(((ViewGroup)view).getChildAt(i));return count;}
 private void layout(View root,android.app.Activity activity){android.util.DisplayMetrics dm=activity.getResources().getDisplayMetrics();root.measure(View.MeasureSpec.makeMeasureSpec(dm.widthPixels,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(dm.heightPixels,View.MeasureSpec.EXACTLY));root.layout(0,0,dm.widthPixels,dm.heightPixels);}
 private void snapshot(View root,String name)throws Exception{String directory=System.getProperty("courselens.parcelPreviewDir");if(directory==null)return;java.io.File folder=new java.io.File(directory);folder.mkdirs();android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(root.getWidth(),root.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);root.draw(new android.graphics.Canvas(bitmap));try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(folder,name+".png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}
}
