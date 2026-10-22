package cn.courselens;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

/** Local, conservative rules. Scores express rule strength, not statistical probability. */
final class ParcelRules {
 static final int VERSION=1,MAX_TEXT=12000;
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
 private static final String CODE="([A-Z0-9]+(?:[ \\t]*-[ \\t]*[A-Z0-9]+)*)(?![A-Z0-9-])";
 private static final Rule[] RULES={
  new Rule("label-v1","(?:取件码|提货码|取货码|提取码|取件编码)\\s*(?:为|是)?\\s*[:：]?\\s*[【\\[（(\"“]?"+CODE),
  new Rule("voucher-v1","凭\\s*[【\\[（(\"“]?"+CODE+"[】\\]）)\"”]?\\s*(?=到|至|去|领取|取件)")
 };
 private static final Pattern STATION=Pattern.compile("(?:取件地址|地址|已到达|已送达|已放至|已到|存放在|放在|请到|到|至)[:：]?\\s*([^，,。；;！!\\n]{2,100}?)(?=取件|领取|取货|[，,。；;！!\\n]|$)");
 private static final Pattern NAMED_STATION=Pattern.compile("([\\p{IsHan}A-Za-z0-9]{2,35}(?:菜鸟驿站|驿站|快递柜|自提柜))");
 private static final Pattern TRACKING=Pattern.compile("(?:运单号|快递单号|物流单号)\\s*[:：]?\\s*([A-Z0-9]{6,40})(?![A-Z0-9])",Pattern.CASE_INSENSITIVE);
 private static final Pattern STRONG_CODE=Pattern.compile("(?:[0-9]{4,8}|[A-Z][0-9]{2,6}|(?:[A-Z][0-9]{0,3}|[0-9]{1,6})(?:-[0-9]{1,6}){1,3})");
 static List<Candidate> extract(String input){
  if(input==null||input.length()>MAX_TEXT)return Collections.emptyList();
  String text=Normalizer.normalize(input,Normalizer.Form.NFKC).replaceAll("[‐‑‒–—−]","-").toUpperCase(Locale.ROOT);
  if(Pattern.compile("身份码|动态码|出库码|付款码|支付码|登录|验证码|校验码").matcher(text).find())return Collections.emptyList();
  boolean parcel=Pattern.compile("快递|包裹|驿站|快递柜|取件|提货|取货").matcher(text).find();
  if(!parcel)return Collections.emptyList();
  Map<Integer,Match> matches=new TreeMap<>();
  for(Rule rule:RULES){Matcher matcher=rule.pattern.matcher(text);while(matcher.find()){
   String code=matcher.group(1).replaceAll("[ \\t]","");
   if(!validCode(code))continue;
   matches.putIfAbsent(matcher.start(),new Match(matcher.start(),matcher.end(),code,rule.name));
  }}
  List<Match> ordered=new ArrayList<>(matches.values());Map<String,Candidate> results=new LinkedHashMap<>();
  for(int i=0;i<ordered.size();i++){
   Match match=ordered.get(i);int left=i==0?0:ordered.get(i-1).end,right=i+1==ordered.size()?text.length():ordered.get(i+1).start;
   // shortcut: multi-code images need human address matching; use geometric OCR blocks if this becomes common.
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
