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
  System.out.println("Parcel rules checks passed");
 }
}
