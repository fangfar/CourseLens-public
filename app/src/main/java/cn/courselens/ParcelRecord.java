package cn.courselens;

import androidx.annotation.NonNull;
import androidx.room.*;

@Entity(tableName="parcels",indices={@Index("trackingNumber"),@Index(value={"code","station"})})
public class ParcelRecord {
 public static final int READY=0,TRANSIT=1,DONE=2;
 @PrimaryKey(autoGenerate=true) public long id;
 @NonNull public String code="",station="",trackingNumber="",carrier="",source="手动",rule="manual";
 public int state=READY,confidence=100;
 public boolean confirmed=true;
 public long createdAt,updatedAt,remindAt,remindedAt,lastQueryAt,retryAt;
 @ColumnInfo(defaultValue="0") public long deletedAt;
 @NonNull public String trackingState="",trackingEvents="",trackingError="";
 public String group(){return station.isEmpty()?"地址待补充":station;}
 public String stateLabel(){return state==TRANSIT?"运输中":state==DONE?"已完成":"待取件";}
}
