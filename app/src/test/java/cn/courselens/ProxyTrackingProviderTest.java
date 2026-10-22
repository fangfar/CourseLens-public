package cn.courselens;

import org.junit.Test;
import static org.junit.Assert.*;

public class ProxyTrackingProviderTest {
 @Test public void validatesEndpoint(){
  assertEquals("https://proxy.example/track",ProxyTrackingProvider.validateEndpoint("https://proxy.example/track"));
  for(String bad:new String[]{"http://proxy.example","https://user:secret@proxy.example/","https://proxy.example/?key=secret","https://proxy.example/#secret","https://proxy.example:8443/","file:///private"})try{ProxyTrackingProvider.validateEndpoint(bad);fail("unsafe endpoint");}catch(IllegalArgumentException expected){}
 }
 @Test public void parsesMockAndRejectsInvalidResponse(){
  TrackingProvider.Result r=ProxyTrackingProvider.parse("{\"schema\":1,\"state\":\"READY\",\"events\":[{\"time\":\"2026-10-22 12:00\",\"description\":\"合成包裹到达取件点\"}]}");
  assertEquals(TrackingProvider.Error.NONE,r.error);assertEquals("READY",r.state);assertEquals(1,r.events.size());
  assertEquals(TrackingProvider.Error.EMPTY,ProxyTrackingProvider.parse("{\"schema\":1,\"state\":\"UNKNOWN\",\"events\":[]}").error);
  for(String bad:new String[]{"{}","not json","{\"schema\":2,\"state\":\"READY\",\"events\":[]}","{\"schema\":1,\"state\":\"PAID\",\"events\":[]}","{\"schema\":1,\"state\":\"READY\",\"events\":[{\"time\":1,\"description\":\"x\"}]}"})assertEquals(TrackingProvider.Error.INVALID_RESPONSE,ProxyTrackingProvider.parse(bad).error);
 }
 @Test public void boundsBackoff(){assertEquals(60000,ProxyTrackingProvider.retryAfter("0"));assertEquals(86400000,ProxyTrackingProvider.retryAfter("999999"));assertEquals(900000,ProxyTrackingProvider.retryAfter("invalid"));assertEquals(900000,ProxyTrackingProvider.retryAfter("9999999999999999999"));}
}
