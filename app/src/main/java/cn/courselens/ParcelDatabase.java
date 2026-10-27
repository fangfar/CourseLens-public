package cn.courselens;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.*;
import java.util.List;

@Database(entities={ParcelRecord.class,ParcelDatabase.Seen.class},version=2,exportSchema=true)
public abstract class ParcelDatabase extends RoomDatabase {
 @Entity(tableName="parcel_seen") public static class Seen {
  @PrimaryKey @NonNull public String fingerprint="";
  public long expiresAt;
 }
 @Dao public interface Parcels {
  @Query("SELECT * FROM parcels WHERE deletedAt=0 ORDER BY confirmed, station, createdAt DESC") List<ParcelRecord> all();
  @Query("SELECT * FROM parcels WHERE id=:id AND deletedAt=0") ParcelRecord get(long id);
  @Query("SELECT * FROM parcels WHERE deletedAt>0 ORDER BY deletedAt DESC") List<ParcelRecord> deleted();
  @Query("SELECT * FROM parcels WHERE id=:id AND deletedAt>0") ParcelRecord deleted(long id);
  @Query("SELECT * FROM parcels WHERE deletedAt=0 AND id!=:id AND ((:tracking!='' AND trackingNumber=:tracking) OR (:code!='' AND code=:code AND station=:station AND (state!=2 OR updatedAt>:since))) LIMIT 1")
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
 static final androidx.room.migration.Migration MIGRATION_1_2=new androidx.room.migration.Migration(1,2){@Override public void migrate(@NonNull androidx.sqlite.db.SupportSQLiteDatabase db){db.execSQL("ALTER TABLE parcels ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0");}};
 private static volatile ParcelDatabase instance;
 public static ParcelDatabase get(Context context){
  if(instance==null)synchronized(ParcelDatabase.class){if(instance==null)instance=Room.databaseBuilder(context.getApplicationContext(),ParcelDatabase.class,"parcels.db").addMigrations(MIGRATION_1_2).build();}
  return instance;
 }
}
