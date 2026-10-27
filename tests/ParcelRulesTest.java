package cn.courselens;

import java.util.*;

public class ParcelRulesTest {
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[] args){
  List<ParcelRules.Candidate> found=ParcelRules.extract("【合成快递】包裹已到达东门驿站，取件码：Ａ１—２—３４５６，运单号：SF00000000001。");
  check(found.size()==1,"explicit pickup missing");ParcelRules.Candidate p=found.get(0);
  check(p.code.equals("A1-2-3456")&&p.station.equals("东门驿站")&&p.tracking.equals("SF00000000001")&&p.autoSave(),"normalization or labelled fields failed");
  check(ParcelRules.extract("请凭123456到南门快递柜取件").get(0).station.equals("南门快递柜"),"voucher sentence failed");
  check(!ParcelRules.extract("快递取件码:123456").get(0).autoSave(),"incomplete record auto confirmed");
  check(!ParcelRules.extract("快递已到达合成驿站，取件码:123456COPY").get(0).autoSave(),"ambiguous suffix auto confirmed");
  for(String text:new String[]{"登录验证码123456，快递取件码123456","菜鸟身份码 取件码:123456","快递取件码:13800000000","快递取件码:2026-10-22","明天12:30到站 订单12345678","取件码:ABC","https://example.com/123456",""})check(ParcelRules.extract(text).isEmpty(),"unsafe/non-parcel input accepted");
  found=ParcelRules.extract("东门驿站快递取件码:1-2-3456\n西门驿站快递取件码:9-8-7654");
  check(found.size()==2&&found.stream().noneMatch(ParcelRules.Candidate::autoSave)&&found.stream().allMatch(c->c.station.isEmpty()),"multi-code address crossed");
  check(ParcelRules.extract("快递取件码:123456".repeat(2000)).isEmpty(),"oversized input accepted");
  for(String label:new String[]{"提货号","签收码","快件编号","提货编码","取件編號"})check(ParcelRules.extract("合成快递已到北门快递站，"+label+"：6-2-2088").get(0).code.equals("6-2-2088"),"supported pickup alias missing: "+label);
  check(ParcelRules.extract("合成快递取件码：6-2-\n2088").get(0).code.equals("6-2-2088"),"OCR line break inside code lost");
  check(ParcelRules.extract("快递取件码：6-2-2088、9-8-7654").size()==2,"multiple codes after one label lost");
  found=ParcelRules.extract("【合成快递】取件码2-4-1111，您的快件尾号1234已送达合成学院京东校园服务站，地址：合成地址，取件和寄件可咨询。");
  check(found.size()==1&&found.get(0).code.equals("2-4-1111")&&found.get(0).station.equals("合成学院京东校园服务站"),"parcel SMS notification text not recognized");
  imageLayout();
  System.out.println("Parcel rules checks passed");
 }
 private static ParcelRules.OcrLine line(String value,int left,int top,int right,int bottom){return new ParcelRules.OcrLine(value,left,top,right,bottom);}
 private static void imageLayout(){
  List<ParcelRules.OcrLine> lines=Arrays.asList(
   line("复制",500,780,540,810),line("6-2-2088",170,770,400,815),line("取 件 码",90,785,155,810),
   line("合成北门京东快递站",90,690,430,725),line("邮政快递包裹 YT00000000001",40,490,500,520),
   line("菜鸟身份码",40,100,210,130),line("13800000000",90,1020,300,1050));
  List<ParcelRules.Candidate> result=ParcelRules.extractImage(lines);check(result.size()==1,"image label geometry failed");ParcelRules.Candidate c=result.get(0);
  check(c.code.equals("6-2-2088")&&c.station.equals("合成北门京东快递站")&&c.tracking.equals("YT00000000001")&&!c.autoSave(),"image fields or confirmation boundary failed");
  List<ParcelRules.OcrLine> reversed=new ArrayList<>(lines);Collections.reverse(reversed);check(ParcelRules.extractImage(reversed).get(0).code.equals(c.code),"block enumeration order changed result");
  check(ParcelRules.extractImage(Arrays.asList(line("取件码",0,0,60,30),line("13800000000",70,0,300,30))).isEmpty(),"phone recognized as pickup code");
  check(ParcelRules.extractImage(Arrays.asList(line("身份码",0,0,60,30),line("123456",70,0,200,30))).isEmpty(),"identity code recognized as pickup code");
  check(ParcelRules.extractImage(Arrays.asList(line("取件码",0,0,60,30),line("123456",70,250,200,280))).isEmpty(),"unrelated distant number linked to label");
  result=ParcelRules.extractImage(Arrays.asList(line("合成东门驿站",0,0,200,30),line("取件码",0,40,60,70),line("1-2-3456",70,40,200,70),line("合成西门驿站",0,400,200,430),line("取件码",0,440,60,470),line("9-8-7654",70,440,200,470)));
  check(result.size()==2&&result.get(0).station.equals("合成东门驿站")&&result.get(1).station.equals("合成西门驿站"),"two parcel cards crossed addresses");
  result=ParcelRules.extractImage(Arrays.asList(line("【快递】取件码2-4-1111，您",0,0,400,30),line("的快件尾号1234已送达合成学院京",0,40,400,70),line("东校园服务站，地址：合成地址",0,80,400,110),line("【快递】取件码2-4-2222，您",0,200,400,230),line("的快件尾号5678已送达合成西门",0,240,400,270),line("校园服务站，地址：合成地址",0,280,400,310)));
  check(result.size()==2&&result.get(0).station.equals("合成学院京东校园服务站")&&result.get(1).station.equals("合成西门校园服务站"),"wrapped station below code not associated with its parcel");
  result=ParcelRules.extractImage(Arrays.asList(line("【快递】取件码2-4-1111",0,0,400,30),line("【快递】取件码2-4-2222",0,80,400,110),line("已送达合成西门校园服务站，地址：合成地址",0,120,400,150)));
  check(result.size()==2&&result.get(0).station.isEmpty()&&result.get(1).station.equals("合成西门校园服务站"),"following parcel station crossed into earlier parcel");
 }

}
