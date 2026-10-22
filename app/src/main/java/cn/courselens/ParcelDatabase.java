package cn.courselens;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.*;
import java.util.List;

@Database(entities={ParcelRecord.class,ParcelDatabase.Seen.class},version=1,exportSchema=true)
public abstract class ParcelDatabase extends RoomDatabase {
 @Entity(tableName="parcel_seen") public static class Seen {
  @PrimaryKey @NonNull public String fingerprint="";
  public long expiresAt;
 }
 @Dao public interface Parcels {
  @Query("SELECT * FROM parcels ORDER BY confirmed, station, createdAt DESC") List<ParcelRecord> all();
  @Query("SELECT * FROM parcels WHERE id=:id") ParcelRecord get(long id);
  @Query("SELECT * FROM parcels WHERE id!=:id AND ((:tracking!='' AND trackingNumber=:tracking) OR (:code!='' AND code=:code AND station=:station AND (state!=2 OR updatedAt>:since))) LIMIT 1")
  ParcelRecord duplicate(long id,String code,String station,String tracking,long since);
  @Insert long insert(ParcelRecord record);
  @Update int update(ParcelRecord record);
  @Query("DELETE FROM parcels WHERE id=:id") void delete(long id);
  @Query("DELETE FROM parcels") void clear();
  @Query("SELECT COUNT(*) FROM parcel_seen WHERE fingerprint=:key AND expiresAt>:now") int seen(String key,long now);
  @Insert(onConflict=OnConflictStrategy.REPLACE) void remember(Seen seen);
  @Query("DELETE FROM parcel_seen WHERE expiresAt<=:now") void prune(long now);
  @Query("DELETE FROM parcel_seen") void forget();
 }
 public abstract Parcels parcels();
 private static volatile ParcelDatabase instance;
 public static ParcelDatabase get(Context context){
  if(instance==null)synchronized(ParcelDatabase.class){if(instance==null)instance=Room.databaseBuilder(context.getApplicationContext(),ParcelDatabase.class,"parcels.db").build();}
  return instance;
 }
}
