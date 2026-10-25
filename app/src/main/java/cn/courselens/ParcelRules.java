package cn.courselens;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

/** Local, conservative rules. Scores express rule strength, not statistical probability. */
final class ParcelRules {
 static final int VERSION=2,MAX_TEXT=12000;
 static final class Candidate {
  final String code,station,tracking,rule;
  final int confidence;
  Candidate(String code,String station,String tracking,String rule,int confidence){this.code=code;this.station=station;this.tracking=tracking;this.rule=rule;this.confidence=confidence;}
  boolean autoSave(){return confidence>=95;}
 }
 private static final class Rule {
  final String name;final Pattern pattern;
  Rule(String name,String regex){this.name=name;pattern=Pattern.compile(regex,Pattern.CASE_INSENSITIVE);}
 }
 // Pickup aliases and address prefixes adapted from shareven/parcel SmsParser.kt (MIT).
 // Copyright (c) 2025 shareven; full notice: assets/licenses/shareven-parcel.txt.
 private static final String LABEL="取件码|提货码|取货码|提取码|取件编码|提货号|提货编码|提货编号|取件编号|快件编号|快件编码|快件码|签收码|操作码|收货编码|签收编码|取件編號|提貨號碼";
 private static final String CODE="([A-Z0-9]+(?:\\s*-\\s*[A-Z0-9]+)*)(?![A-Z0-9-])";
 private static final Pattern PICKUP_LABEL=Pattern.compile(LABEL);
 private static final Pattern SENSITIVE=Pattern.compile("身份码|动态码|出库码|付款码|支付码|登录|验证码|校验码");
 private static final Rule[] RULES={
  new Rule("label-v2","(?:"+LABEL+")\\s*(?:为|是)?\\s*[:：]?\\s*[【\\[（(\"“『]?"+CODE),
  new Rule("voucher-v2","(?:凭|请用)\\s*[【\\[（(\"“『]?"+CODE+"[】\\]）)\"”』]?\\s*(?=到|至|去|领取|取件|提货|取货)")
 };
 private static final Pattern NEXT_CODE=Pattern.compile("\\s*[、,，]\\s*"+CODE);
 private static final Pattern STATION=Pattern.compile("(?:取件地址|收货地址|送货地址|地址|位于|已到达|已送达|已放至|已存放至|已存放|已放入|放入|放至|到达|已到|存放在|放在|请到|到|至)[:：]?\\s*([^，,。；;！!\\n【】\\[\\]]{2,100}?)(?=取件|领取|取货|[，,。；;！!\\n【】\\[\\]]|$)");
 private static final Pattern NAMED_STATION=Pattern.compile("([\\p{IsHan}A-Za-z0-9]{2,50}(?:菜鸟驿站|驿站|快递服务站|快递站|快递点|代收点|快递柜|自提柜|自提点|丰巢柜))");
 private static final Pattern TRACKING=Pattern.compile("(?:运单号|快递单号|物流单号|(?:邮政|中通|圆通|申通|韵达|顺丰|京东)快递包裹)\\s*[:：]?\\s*([A-Z0-9]{6,40})(?![A-Z0-9])",Pattern.CASE_INSENSITIVE);
 private static final Pattern STRONG_CODE=Pattern.compile("(?:[0-9]{4,8}|[A-Z][0-9]{2,6}|(?:[A-Z][0-9]{0,3}|[0-9]{1,6})(?:-[0-9]{1,6}){1,3})");
 private static String normalize(String input){return Normalizer.normalize(input,Normalizer.Form.NFKC).replaceAll("[‐‑‒–—−]","-").toUpperCase(Locale.ROOT);}
 static List<Candidate> extract(String input){
  if(input==null||input.length()>MAX_TEXT)return Collections.emptyList();
  String text=normalize(input);
  if(SENSITIVE.matcher(text).find())return Collections.emptyList();
  boolean parcel=Pattern.compile("快递|包裹|驿站|快递柜|取件|提货|取货").matcher(text).find();
  if(!parcel)return Collections.emptyList();
  Map<Integer,Match> matches=new TreeMap<>();
  for(Rule rule:RULES){Matcher matcher=rule.pattern.matcher(text);while(matcher.find()){
   String code=matcher.group(1).replaceAll("\\s","");
   if(!validCode(code))continue;
   matches.putIfAbsent(matcher.start(),new Match(matcher.start(),matcher.end(),code,rule.name));
   Matcher more=NEXT_CODE.matcher(text);int end=matcher.end();while(more.region(end,text.length()).lookingAt()){String next=more.group(1).replaceAll("\\s","");if(validCode(next))matches.putIfAbsent(more.start(),new Match(more.start(),more.end(),next,rule.name));end=more.end();}
  }}
  List<Match> ordered=new ArrayList<>(matches.values());Map<String,Candidate> results=new LinkedHashMap<>();
  for(int i=0;i<ordered.size();i++){
   Match match=ordered.get(i);int left=i==0?0:ordered.get(i-1).end,right=i+1==ordered.size()?text.length():ordered.get(i+1).start;
   // Shared text with several codes cannot prove which address belongs to each one.
   String window=text.substring(Math.max(left,match.start-160),Math.min(right,match.end+200));
   String station="",tracking="";Matcher address=STATION.matcher(window);if(address.find())station=address.group(1).trim();
   if(station.isEmpty()){Matcher named=NAMED_STATION.matcher(window);if(named.find())station=named.group(1);}
   Matcher waybill=TRACKING.matcher(window);if(waybill.find())tracking=waybill.group(1);
   int score=!station.isEmpty()&&ordered.size()==1&&STRONG_CODE.matcher(match.code).matches()?95:75;
   if(ordered.size()>1){station="";tracking="";}
   results.putIfAbsent(match.code+"\n"+station,new Candidate(match.code,station,tracking,match.rule,score));
  }
  return new ArrayList<>(results.values());
 }
 static final class OcrLine {
  final String text;final int left,top,right,bottom;
  OcrLine(String text,int left,int top,int right,int bottom){this.text=normalize(text).replaceAll("(?<=[\\p{IsHan}])\\s+(?=[\\p{IsHan}])","");this.left=left;this.top=top;this.right=right;this.bottom=bottom;}
  int height(){return Math.max(1,bottom-top);}
 }
 static List<Candidate> extractImage(List<OcrLine> lines){
  if(lines==null||lines.size()>1000)return Collections.emptyList();int length=0;for(OcrLine line:lines){length+=line.text.length();if(length>MAX_TEXT)return Collections.emptyList();}
  Map<String,Candidate> found=new LinkedHashMap<>();
  for(OcrLine label:lines){
   if(!PICKUP_LABEL.matcher(label.text).find()||SENSITIVE.matcher(label.text).find())continue;
   List<Candidate> codes=extract("快递\n"+label.text);OcrLine codeLine=label;
   if(codes.isEmpty()){
    double nearest=Double.MAX_VALUE;
    for(OcrLine line:lines){
     if(line==label||SENSITIVE.matcher(line.text).find()||PICKUP_LABEL.matcher(line.text).find())continue;
     int h=Math.max(label.height(),line.height());boolean sameRow=Math.min(label.bottom,line.bottom)-Math.max(label.top,line.top)>Math.min(label.height(),line.height())/2;
     if(sameRow?line.left<label.right||line.left-label.right>h*6:line.top<label.bottom||line.top-label.bottom>label.height()*3||Math.abs(line.left-label.left)>h*4)continue;
     String value=line.text.replaceAll("\\s","");if(!STRONG_CODE.matcher(value).matches()||!validCode(value))continue;
     double distance=sameRow?Math.max(0,line.left-label.right):Math.max(0,line.top-label.bottom)*4+Math.abs(line.left-label.left);
     if(distance<nearest){nearest=distance;codeLine=line;codes=Collections.singletonList(new Candidate(value,"","","ocr-layout-v2",85));}
    }
   }
   String station="",tracking="";int best=Integer.MAX_VALUE;
   for(OcrLine line:lines){
    if(line.top>codeLine.bottom||codeLine.top-line.bottom>Math.max(label.height(),codeLine.height())*10)continue;
    Matcher named=NAMED_STATION.matcher(line.text);if(named.find()&&line.text.length()<=80){int distance=Math.abs(codeLine.top-line.bottom);if(distance<best){best=distance;station=named.group(1);}}
    Matcher waybill=TRACKING.matcher(line.text);if(waybill.find())tracking=waybill.group(1);
   }
   for(Candidate c:codes){Candidate value=new Candidate(c.code,codes.size()==1?station:"",codes.size()==1?tracking:"","ocr-layout-v2",85);found.putIfAbsent(c.code+"\n"+value.station,value);}
  }
  if(found.isEmpty()&&lines.stream().noneMatch(line->PICKUP_LABEL.matcher(line.text).find())){StringBuilder text=new StringBuilder();for(OcrLine line:lines)text.append(line.text).append('\n');for(Candidate c:extract(text.toString()))found.put(c.code+"\n"+c.station,new Candidate(c.code,c.station,c.tracking,"ocr-text-v2",85));}
  return new ArrayList<>(found.values());
 }
 private static boolean validCode(String code){
  if(code.length()<3||code.length()>20||!code.matches(".*[0-9].*"))return false;
  if(code.matches("[0-9]+"))return code.length()>=4&&code.length()<=8;
  return !code.matches("(?:19|20)[0-9]{2}-[0-9]{1,2}-[0-9]{1,2}")&&code.matches("[A-Z0-9]+(?:-[A-Z0-9]+)*");
 }
 private static final class Match {
  final int start,end;final String code,rule;
  Match(int start,int end,String code,String rule){this.start=start;this.end=end;this.code=code;this.rule=rule;}
 }
}
