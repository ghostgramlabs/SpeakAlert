package com.ghostgramlabs.speakalert.data.database;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.ghostgramlabs.speakalert.data.model.MissedReminderEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MissedReminderDao_Impl implements MissedReminderDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MissedReminderEntity> __insertionAdapterOfMissedReminderEntity;

  private final EntityDeletionOrUpdateAdapter<MissedReminderEntity> __deletionAdapterOfMissedReminderEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByReminderId;

  public MissedReminderDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMissedReminderEntity = new EntityInsertionAdapter<MissedReminderEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `missed_reminders` (`id`,`reminderId`,`title`,`scheduledTime`,`detectedTime`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MissedReminderEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getReminderId());
        statement.bindString(3, entity.getTitle());
        statement.bindLong(4, entity.getScheduledTime());
        statement.bindLong(5, entity.getDetectedTime());
      }
    };
    this.__deletionAdapterOfMissedReminderEntity = new EntityDeletionOrUpdateAdapter<MissedReminderEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `missed_reminders` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MissedReminderEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM missed_reminders WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteByReminderId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM missed_reminders WHERE reminderId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final MissedReminderEntity missedReminder,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMissedReminderEntity.insert(missedReminder);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final MissedReminderEntity missedReminder,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfMissedReminderEntity.handle(missedReminder);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByReminderId(final long reminderId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByReminderId.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, reminderId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteByReminderId.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MissedReminderEntity>> getAllMissedReminders() {
    final String _sql = "SELECT * FROM missed_reminders ORDER BY detectedTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"missed_reminders"}, new Callable<List<MissedReminderEntity>>() {
      @Override
      @NonNull
      public List<MissedReminderEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfReminderId = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfScheduledTime = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduledTime");
          final int _cursorIndexOfDetectedTime = CursorUtil.getColumnIndexOrThrow(_cursor, "detectedTime");
          final List<MissedReminderEntity> _result = new ArrayList<MissedReminderEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MissedReminderEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpReminderId;
            _tmpReminderId = _cursor.getLong(_cursorIndexOfReminderId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final long _tmpScheduledTime;
            _tmpScheduledTime = _cursor.getLong(_cursorIndexOfScheduledTime);
            final long _tmpDetectedTime;
            _tmpDetectedTime = _cursor.getLong(_cursorIndexOfDetectedTime);
            _item = new MissedReminderEntity(_tmpId,_tmpReminderId,_tmpTitle,_tmpScheduledTime,_tmpDetectedTime);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
