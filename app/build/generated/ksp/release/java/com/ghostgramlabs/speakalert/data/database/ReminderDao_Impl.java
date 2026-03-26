package com.ghostgramlabs.speakalert.data.database;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.ghostgramlabs.speakalert.data.model.ReminderEntity;
import com.ghostgramlabs.speakalert.domain.models.MissedPolicy;
import com.ghostgramlabs.speakalert.domain.models.RecurrenceType;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.Long;
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
public final class ReminderDao_Impl implements ReminderDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ReminderEntity> __insertionAdapterOfReminderEntity;

  private final EntityDeletionOrUpdateAdapter<ReminderEntity> __deletionAdapterOfReminderEntity;

  private final EntityDeletionOrUpdateAdapter<ReminderEntity> __updateAdapterOfReminderEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteReminderById;

  public ReminderDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfReminderEntity = new EntityInsertionAdapter<ReminderEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `reminders` (`id`,`title`,`reminderText`,`transcript`,`audioPath`,`createdAt`,`nextTriggerAt`,`lastFiredAt`,`isCompleted`,`completedAt`,`recurrenceType`,`recurrenceJson`,`snoozeUntil`,`missedPolicy`,`loopPlayback`,`followUpCheckMinutes`,`pendingFollowUpAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ReminderEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getTitle() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getTitle());
        }
        if (entity.getReminderText() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getReminderText());
        }
        if (entity.getTranscript() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getTranscript());
        }
        if (entity.getAudioPath() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getAudioPath());
        }
        statement.bindLong(6, entity.getCreatedAt());
        statement.bindLong(7, entity.getNextTriggerAt());
        if (entity.getLastFiredAt() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getLastFiredAt());
        }
        final int _tmp = entity.isCompleted() ? 1 : 0;
        statement.bindLong(9, _tmp);
        if (entity.getCompletedAt() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getCompletedAt());
        }
        statement.bindString(11, __RecurrenceType_enumToString(entity.getRecurrenceType()));
        if (entity.getRecurrenceJson() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getRecurrenceJson());
        }
        if (entity.getSnoozeUntil() == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.getSnoozeUntil());
        }
        statement.bindString(14, __MissedPolicy_enumToString(entity.getMissedPolicy()));
        final int _tmp_1 = entity.getLoopPlayback() ? 1 : 0;
        statement.bindLong(15, _tmp_1);
        statement.bindLong(16, entity.getFollowUpCheckMinutes());
        if (entity.getPendingFollowUpAt() == null) {
          statement.bindNull(17);
        } else {
          statement.bindLong(17, entity.getPendingFollowUpAt());
        }
      }
    };
    this.__deletionAdapterOfReminderEntity = new EntityDeletionOrUpdateAdapter<ReminderEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `reminders` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ReminderEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfReminderEntity = new EntityDeletionOrUpdateAdapter<ReminderEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `reminders` SET `id` = ?,`title` = ?,`reminderText` = ?,`transcript` = ?,`audioPath` = ?,`createdAt` = ?,`nextTriggerAt` = ?,`lastFiredAt` = ?,`isCompleted` = ?,`completedAt` = ?,`recurrenceType` = ?,`recurrenceJson` = ?,`snoozeUntil` = ?,`missedPolicy` = ?,`loopPlayback` = ?,`followUpCheckMinutes` = ?,`pendingFollowUpAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ReminderEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getTitle() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getTitle());
        }
        if (entity.getReminderText() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getReminderText());
        }
        if (entity.getTranscript() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getTranscript());
        }
        if (entity.getAudioPath() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getAudioPath());
        }
        statement.bindLong(6, entity.getCreatedAt());
        statement.bindLong(7, entity.getNextTriggerAt());
        if (entity.getLastFiredAt() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getLastFiredAt());
        }
        final int _tmp = entity.isCompleted() ? 1 : 0;
        statement.bindLong(9, _tmp);
        if (entity.getCompletedAt() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getCompletedAt());
        }
        statement.bindString(11, __RecurrenceType_enumToString(entity.getRecurrenceType()));
        if (entity.getRecurrenceJson() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getRecurrenceJson());
        }
        if (entity.getSnoozeUntil() == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.getSnoozeUntil());
        }
        statement.bindString(14, __MissedPolicy_enumToString(entity.getMissedPolicy()));
        final int _tmp_1 = entity.getLoopPlayback() ? 1 : 0;
        statement.bindLong(15, _tmp_1);
        statement.bindLong(16, entity.getFollowUpCheckMinutes());
        if (entity.getPendingFollowUpAt() == null) {
          statement.bindNull(17);
        } else {
          statement.bindLong(17, entity.getPendingFollowUpAt());
        }
        statement.bindLong(18, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteReminderById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM reminders WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertReminder(final ReminderEntity reminder,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfReminderEntity.insertAndReturnId(reminder);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteReminder(final ReminderEntity reminder,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfReminderEntity.handle(reminder);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateReminder(final ReminderEntity reminder,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfReminderEntity.handle(reminder);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteReminderById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteReminderById.acquire();
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
          __preparedStmtOfDeleteReminderById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ReminderEntity>> getAllReminders() {
    final String _sql = "SELECT * FROM reminders ORDER BY nextTriggerAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"reminders"}, new Callable<List<ReminderEntity>>() {
      @Override
      @NonNull
      public List<ReminderEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfReminderText = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderText");
          final int _cursorIndexOfTranscript = CursorUtil.getColumnIndexOrThrow(_cursor, "transcript");
          final int _cursorIndexOfAudioPath = CursorUtil.getColumnIndexOrThrow(_cursor, "audioPath");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfNextTriggerAt = CursorUtil.getColumnIndexOrThrow(_cursor, "nextTriggerAt");
          final int _cursorIndexOfLastFiredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastFiredAt");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfRecurrenceType = CursorUtil.getColumnIndexOrThrow(_cursor, "recurrenceType");
          final int _cursorIndexOfRecurrenceJson = CursorUtil.getColumnIndexOrThrow(_cursor, "recurrenceJson");
          final int _cursorIndexOfSnoozeUntil = CursorUtil.getColumnIndexOrThrow(_cursor, "snoozeUntil");
          final int _cursorIndexOfMissedPolicy = CursorUtil.getColumnIndexOrThrow(_cursor, "missedPolicy");
          final int _cursorIndexOfLoopPlayback = CursorUtil.getColumnIndexOrThrow(_cursor, "loopPlayback");
          final int _cursorIndexOfFollowUpCheckMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "followUpCheckMinutes");
          final int _cursorIndexOfPendingFollowUpAt = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingFollowUpAt");
          final List<ReminderEntity> _result = new ArrayList<ReminderEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ReminderEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpReminderText;
            if (_cursor.isNull(_cursorIndexOfReminderText)) {
              _tmpReminderText = null;
            } else {
              _tmpReminderText = _cursor.getString(_cursorIndexOfReminderText);
            }
            final String _tmpTranscript;
            if (_cursor.isNull(_cursorIndexOfTranscript)) {
              _tmpTranscript = null;
            } else {
              _tmpTranscript = _cursor.getString(_cursorIndexOfTranscript);
            }
            final String _tmpAudioPath;
            if (_cursor.isNull(_cursorIndexOfAudioPath)) {
              _tmpAudioPath = null;
            } else {
              _tmpAudioPath = _cursor.getString(_cursorIndexOfAudioPath);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpNextTriggerAt;
            _tmpNextTriggerAt = _cursor.getLong(_cursorIndexOfNextTriggerAt);
            final Long _tmpLastFiredAt;
            if (_cursor.isNull(_cursorIndexOfLastFiredAt)) {
              _tmpLastFiredAt = null;
            } else {
              _tmpLastFiredAt = _cursor.getLong(_cursorIndexOfLastFiredAt);
            }
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            final RecurrenceType _tmpRecurrenceType;
            _tmpRecurrenceType = __RecurrenceType_stringToEnum(_cursor.getString(_cursorIndexOfRecurrenceType));
            final String _tmpRecurrenceJson;
            if (_cursor.isNull(_cursorIndexOfRecurrenceJson)) {
              _tmpRecurrenceJson = null;
            } else {
              _tmpRecurrenceJson = _cursor.getString(_cursorIndexOfRecurrenceJson);
            }
            final Long _tmpSnoozeUntil;
            if (_cursor.isNull(_cursorIndexOfSnoozeUntil)) {
              _tmpSnoozeUntil = null;
            } else {
              _tmpSnoozeUntil = _cursor.getLong(_cursorIndexOfSnoozeUntil);
            }
            final MissedPolicy _tmpMissedPolicy;
            _tmpMissedPolicy = __MissedPolicy_stringToEnum(_cursor.getString(_cursorIndexOfMissedPolicy));
            final boolean _tmpLoopPlayback;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfLoopPlayback);
            _tmpLoopPlayback = _tmp_1 != 0;
            final int _tmpFollowUpCheckMinutes;
            _tmpFollowUpCheckMinutes = _cursor.getInt(_cursorIndexOfFollowUpCheckMinutes);
            final Long _tmpPendingFollowUpAt;
            if (_cursor.isNull(_cursorIndexOfPendingFollowUpAt)) {
              _tmpPendingFollowUpAt = null;
            } else {
              _tmpPendingFollowUpAt = _cursor.getLong(_cursorIndexOfPendingFollowUpAt);
            }
            _item = new ReminderEntity(_tmpId,_tmpTitle,_tmpReminderText,_tmpTranscript,_tmpAudioPath,_tmpCreatedAt,_tmpNextTriggerAt,_tmpLastFiredAt,_tmpIsCompleted,_tmpCompletedAt,_tmpRecurrenceType,_tmpRecurrenceJson,_tmpSnoozeUntil,_tmpMissedPolicy,_tmpLoopPlayback,_tmpFollowUpCheckMinutes,_tmpPendingFollowUpAt);
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

  @Override
  public Flow<List<ReminderEntity>> getActiveReminders() {
    final String _sql = "SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY nextTriggerAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"reminders"}, new Callable<List<ReminderEntity>>() {
      @Override
      @NonNull
      public List<ReminderEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfReminderText = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderText");
          final int _cursorIndexOfTranscript = CursorUtil.getColumnIndexOrThrow(_cursor, "transcript");
          final int _cursorIndexOfAudioPath = CursorUtil.getColumnIndexOrThrow(_cursor, "audioPath");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfNextTriggerAt = CursorUtil.getColumnIndexOrThrow(_cursor, "nextTriggerAt");
          final int _cursorIndexOfLastFiredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastFiredAt");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfRecurrenceType = CursorUtil.getColumnIndexOrThrow(_cursor, "recurrenceType");
          final int _cursorIndexOfRecurrenceJson = CursorUtil.getColumnIndexOrThrow(_cursor, "recurrenceJson");
          final int _cursorIndexOfSnoozeUntil = CursorUtil.getColumnIndexOrThrow(_cursor, "snoozeUntil");
          final int _cursorIndexOfMissedPolicy = CursorUtil.getColumnIndexOrThrow(_cursor, "missedPolicy");
          final int _cursorIndexOfLoopPlayback = CursorUtil.getColumnIndexOrThrow(_cursor, "loopPlayback");
          final int _cursorIndexOfFollowUpCheckMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "followUpCheckMinutes");
          final int _cursorIndexOfPendingFollowUpAt = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingFollowUpAt");
          final List<ReminderEntity> _result = new ArrayList<ReminderEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ReminderEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpReminderText;
            if (_cursor.isNull(_cursorIndexOfReminderText)) {
              _tmpReminderText = null;
            } else {
              _tmpReminderText = _cursor.getString(_cursorIndexOfReminderText);
            }
            final String _tmpTranscript;
            if (_cursor.isNull(_cursorIndexOfTranscript)) {
              _tmpTranscript = null;
            } else {
              _tmpTranscript = _cursor.getString(_cursorIndexOfTranscript);
            }
            final String _tmpAudioPath;
            if (_cursor.isNull(_cursorIndexOfAudioPath)) {
              _tmpAudioPath = null;
            } else {
              _tmpAudioPath = _cursor.getString(_cursorIndexOfAudioPath);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpNextTriggerAt;
            _tmpNextTriggerAt = _cursor.getLong(_cursorIndexOfNextTriggerAt);
            final Long _tmpLastFiredAt;
            if (_cursor.isNull(_cursorIndexOfLastFiredAt)) {
              _tmpLastFiredAt = null;
            } else {
              _tmpLastFiredAt = _cursor.getLong(_cursorIndexOfLastFiredAt);
            }
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            final RecurrenceType _tmpRecurrenceType;
            _tmpRecurrenceType = __RecurrenceType_stringToEnum(_cursor.getString(_cursorIndexOfRecurrenceType));
            final String _tmpRecurrenceJson;
            if (_cursor.isNull(_cursorIndexOfRecurrenceJson)) {
              _tmpRecurrenceJson = null;
            } else {
              _tmpRecurrenceJson = _cursor.getString(_cursorIndexOfRecurrenceJson);
            }
            final Long _tmpSnoozeUntil;
            if (_cursor.isNull(_cursorIndexOfSnoozeUntil)) {
              _tmpSnoozeUntil = null;
            } else {
              _tmpSnoozeUntil = _cursor.getLong(_cursorIndexOfSnoozeUntil);
            }
            final MissedPolicy _tmpMissedPolicy;
            _tmpMissedPolicy = __MissedPolicy_stringToEnum(_cursor.getString(_cursorIndexOfMissedPolicy));
            final boolean _tmpLoopPlayback;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfLoopPlayback);
            _tmpLoopPlayback = _tmp_1 != 0;
            final int _tmpFollowUpCheckMinutes;
            _tmpFollowUpCheckMinutes = _cursor.getInt(_cursorIndexOfFollowUpCheckMinutes);
            final Long _tmpPendingFollowUpAt;
            if (_cursor.isNull(_cursorIndexOfPendingFollowUpAt)) {
              _tmpPendingFollowUpAt = null;
            } else {
              _tmpPendingFollowUpAt = _cursor.getLong(_cursorIndexOfPendingFollowUpAt);
            }
            _item = new ReminderEntity(_tmpId,_tmpTitle,_tmpReminderText,_tmpTranscript,_tmpAudioPath,_tmpCreatedAt,_tmpNextTriggerAt,_tmpLastFiredAt,_tmpIsCompleted,_tmpCompletedAt,_tmpRecurrenceType,_tmpRecurrenceJson,_tmpSnoozeUntil,_tmpMissedPolicy,_tmpLoopPlayback,_tmpFollowUpCheckMinutes,_tmpPendingFollowUpAt);
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

  @Override
  public Flow<List<ReminderEntity>> getCompletedReminders() {
    final String _sql = "SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY completedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"reminders"}, new Callable<List<ReminderEntity>>() {
      @Override
      @NonNull
      public List<ReminderEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfReminderText = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderText");
          final int _cursorIndexOfTranscript = CursorUtil.getColumnIndexOrThrow(_cursor, "transcript");
          final int _cursorIndexOfAudioPath = CursorUtil.getColumnIndexOrThrow(_cursor, "audioPath");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfNextTriggerAt = CursorUtil.getColumnIndexOrThrow(_cursor, "nextTriggerAt");
          final int _cursorIndexOfLastFiredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastFiredAt");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfRecurrenceType = CursorUtil.getColumnIndexOrThrow(_cursor, "recurrenceType");
          final int _cursorIndexOfRecurrenceJson = CursorUtil.getColumnIndexOrThrow(_cursor, "recurrenceJson");
          final int _cursorIndexOfSnoozeUntil = CursorUtil.getColumnIndexOrThrow(_cursor, "snoozeUntil");
          final int _cursorIndexOfMissedPolicy = CursorUtil.getColumnIndexOrThrow(_cursor, "missedPolicy");
          final int _cursorIndexOfLoopPlayback = CursorUtil.getColumnIndexOrThrow(_cursor, "loopPlayback");
          final int _cursorIndexOfFollowUpCheckMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "followUpCheckMinutes");
          final int _cursorIndexOfPendingFollowUpAt = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingFollowUpAt");
          final List<ReminderEntity> _result = new ArrayList<ReminderEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ReminderEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpReminderText;
            if (_cursor.isNull(_cursorIndexOfReminderText)) {
              _tmpReminderText = null;
            } else {
              _tmpReminderText = _cursor.getString(_cursorIndexOfReminderText);
            }
            final String _tmpTranscript;
            if (_cursor.isNull(_cursorIndexOfTranscript)) {
              _tmpTranscript = null;
            } else {
              _tmpTranscript = _cursor.getString(_cursorIndexOfTranscript);
            }
            final String _tmpAudioPath;
            if (_cursor.isNull(_cursorIndexOfAudioPath)) {
              _tmpAudioPath = null;
            } else {
              _tmpAudioPath = _cursor.getString(_cursorIndexOfAudioPath);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpNextTriggerAt;
            _tmpNextTriggerAt = _cursor.getLong(_cursorIndexOfNextTriggerAt);
            final Long _tmpLastFiredAt;
            if (_cursor.isNull(_cursorIndexOfLastFiredAt)) {
              _tmpLastFiredAt = null;
            } else {
              _tmpLastFiredAt = _cursor.getLong(_cursorIndexOfLastFiredAt);
            }
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            final RecurrenceType _tmpRecurrenceType;
            _tmpRecurrenceType = __RecurrenceType_stringToEnum(_cursor.getString(_cursorIndexOfRecurrenceType));
            final String _tmpRecurrenceJson;
            if (_cursor.isNull(_cursorIndexOfRecurrenceJson)) {
              _tmpRecurrenceJson = null;
            } else {
              _tmpRecurrenceJson = _cursor.getString(_cursorIndexOfRecurrenceJson);
            }
            final Long _tmpSnoozeUntil;
            if (_cursor.isNull(_cursorIndexOfSnoozeUntil)) {
              _tmpSnoozeUntil = null;
            } else {
              _tmpSnoozeUntil = _cursor.getLong(_cursorIndexOfSnoozeUntil);
            }
            final MissedPolicy _tmpMissedPolicy;
            _tmpMissedPolicy = __MissedPolicy_stringToEnum(_cursor.getString(_cursorIndexOfMissedPolicy));
            final boolean _tmpLoopPlayback;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfLoopPlayback);
            _tmpLoopPlayback = _tmp_1 != 0;
            final int _tmpFollowUpCheckMinutes;
            _tmpFollowUpCheckMinutes = _cursor.getInt(_cursorIndexOfFollowUpCheckMinutes);
            final Long _tmpPendingFollowUpAt;
            if (_cursor.isNull(_cursorIndexOfPendingFollowUpAt)) {
              _tmpPendingFollowUpAt = null;
            } else {
              _tmpPendingFollowUpAt = _cursor.getLong(_cursorIndexOfPendingFollowUpAt);
            }
            _item = new ReminderEntity(_tmpId,_tmpTitle,_tmpReminderText,_tmpTranscript,_tmpAudioPath,_tmpCreatedAt,_tmpNextTriggerAt,_tmpLastFiredAt,_tmpIsCompleted,_tmpCompletedAt,_tmpRecurrenceType,_tmpRecurrenceJson,_tmpSnoozeUntil,_tmpMissedPolicy,_tmpLoopPlayback,_tmpFollowUpCheckMinutes,_tmpPendingFollowUpAt);
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

  @Override
  public Object getReminderById(final long id,
      final Continuation<? super ReminderEntity> $completion) {
    final String _sql = "SELECT * FROM reminders WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ReminderEntity>() {
      @Override
      @Nullable
      public ReminderEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfReminderText = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderText");
          final int _cursorIndexOfTranscript = CursorUtil.getColumnIndexOrThrow(_cursor, "transcript");
          final int _cursorIndexOfAudioPath = CursorUtil.getColumnIndexOrThrow(_cursor, "audioPath");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfNextTriggerAt = CursorUtil.getColumnIndexOrThrow(_cursor, "nextTriggerAt");
          final int _cursorIndexOfLastFiredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastFiredAt");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfRecurrenceType = CursorUtil.getColumnIndexOrThrow(_cursor, "recurrenceType");
          final int _cursorIndexOfRecurrenceJson = CursorUtil.getColumnIndexOrThrow(_cursor, "recurrenceJson");
          final int _cursorIndexOfSnoozeUntil = CursorUtil.getColumnIndexOrThrow(_cursor, "snoozeUntil");
          final int _cursorIndexOfMissedPolicy = CursorUtil.getColumnIndexOrThrow(_cursor, "missedPolicy");
          final int _cursorIndexOfLoopPlayback = CursorUtil.getColumnIndexOrThrow(_cursor, "loopPlayback");
          final int _cursorIndexOfFollowUpCheckMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "followUpCheckMinutes");
          final int _cursorIndexOfPendingFollowUpAt = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingFollowUpAt");
          final ReminderEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpReminderText;
            if (_cursor.isNull(_cursorIndexOfReminderText)) {
              _tmpReminderText = null;
            } else {
              _tmpReminderText = _cursor.getString(_cursorIndexOfReminderText);
            }
            final String _tmpTranscript;
            if (_cursor.isNull(_cursorIndexOfTranscript)) {
              _tmpTranscript = null;
            } else {
              _tmpTranscript = _cursor.getString(_cursorIndexOfTranscript);
            }
            final String _tmpAudioPath;
            if (_cursor.isNull(_cursorIndexOfAudioPath)) {
              _tmpAudioPath = null;
            } else {
              _tmpAudioPath = _cursor.getString(_cursorIndexOfAudioPath);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpNextTriggerAt;
            _tmpNextTriggerAt = _cursor.getLong(_cursorIndexOfNextTriggerAt);
            final Long _tmpLastFiredAt;
            if (_cursor.isNull(_cursorIndexOfLastFiredAt)) {
              _tmpLastFiredAt = null;
            } else {
              _tmpLastFiredAt = _cursor.getLong(_cursorIndexOfLastFiredAt);
            }
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            final RecurrenceType _tmpRecurrenceType;
            _tmpRecurrenceType = __RecurrenceType_stringToEnum(_cursor.getString(_cursorIndexOfRecurrenceType));
            final String _tmpRecurrenceJson;
            if (_cursor.isNull(_cursorIndexOfRecurrenceJson)) {
              _tmpRecurrenceJson = null;
            } else {
              _tmpRecurrenceJson = _cursor.getString(_cursorIndexOfRecurrenceJson);
            }
            final Long _tmpSnoozeUntil;
            if (_cursor.isNull(_cursorIndexOfSnoozeUntil)) {
              _tmpSnoozeUntil = null;
            } else {
              _tmpSnoozeUntil = _cursor.getLong(_cursorIndexOfSnoozeUntil);
            }
            final MissedPolicy _tmpMissedPolicy;
            _tmpMissedPolicy = __MissedPolicy_stringToEnum(_cursor.getString(_cursorIndexOfMissedPolicy));
            final boolean _tmpLoopPlayback;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfLoopPlayback);
            _tmpLoopPlayback = _tmp_1 != 0;
            final int _tmpFollowUpCheckMinutes;
            _tmpFollowUpCheckMinutes = _cursor.getInt(_cursorIndexOfFollowUpCheckMinutes);
            final Long _tmpPendingFollowUpAt;
            if (_cursor.isNull(_cursorIndexOfPendingFollowUpAt)) {
              _tmpPendingFollowUpAt = null;
            } else {
              _tmpPendingFollowUpAt = _cursor.getLong(_cursorIndexOfPendingFollowUpAt);
            }
            _result = new ReminderEntity(_tmpId,_tmpTitle,_tmpReminderText,_tmpTranscript,_tmpAudioPath,_tmpCreatedAt,_tmpNextTriggerAt,_tmpLastFiredAt,_tmpIsCompleted,_tmpCompletedAt,_tmpRecurrenceType,_tmpRecurrenceJson,_tmpSnoozeUntil,_tmpMissedPolicy,_tmpLoopPlayback,_tmpFollowUpCheckMinutes,_tmpPendingFollowUpAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllActiveRemindersOnce(
      final Continuation<? super List<ReminderEntity>> $completion) {
    final String _sql = "SELECT * FROM reminders WHERE nextTriggerAt > 0 AND isCompleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ReminderEntity>>() {
      @Override
      @NonNull
      public List<ReminderEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfReminderText = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderText");
          final int _cursorIndexOfTranscript = CursorUtil.getColumnIndexOrThrow(_cursor, "transcript");
          final int _cursorIndexOfAudioPath = CursorUtil.getColumnIndexOrThrow(_cursor, "audioPath");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfNextTriggerAt = CursorUtil.getColumnIndexOrThrow(_cursor, "nextTriggerAt");
          final int _cursorIndexOfLastFiredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastFiredAt");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfRecurrenceType = CursorUtil.getColumnIndexOrThrow(_cursor, "recurrenceType");
          final int _cursorIndexOfRecurrenceJson = CursorUtil.getColumnIndexOrThrow(_cursor, "recurrenceJson");
          final int _cursorIndexOfSnoozeUntil = CursorUtil.getColumnIndexOrThrow(_cursor, "snoozeUntil");
          final int _cursorIndexOfMissedPolicy = CursorUtil.getColumnIndexOrThrow(_cursor, "missedPolicy");
          final int _cursorIndexOfLoopPlayback = CursorUtil.getColumnIndexOrThrow(_cursor, "loopPlayback");
          final int _cursorIndexOfFollowUpCheckMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "followUpCheckMinutes");
          final int _cursorIndexOfPendingFollowUpAt = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingFollowUpAt");
          final List<ReminderEntity> _result = new ArrayList<ReminderEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ReminderEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpReminderText;
            if (_cursor.isNull(_cursorIndexOfReminderText)) {
              _tmpReminderText = null;
            } else {
              _tmpReminderText = _cursor.getString(_cursorIndexOfReminderText);
            }
            final String _tmpTranscript;
            if (_cursor.isNull(_cursorIndexOfTranscript)) {
              _tmpTranscript = null;
            } else {
              _tmpTranscript = _cursor.getString(_cursorIndexOfTranscript);
            }
            final String _tmpAudioPath;
            if (_cursor.isNull(_cursorIndexOfAudioPath)) {
              _tmpAudioPath = null;
            } else {
              _tmpAudioPath = _cursor.getString(_cursorIndexOfAudioPath);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpNextTriggerAt;
            _tmpNextTriggerAt = _cursor.getLong(_cursorIndexOfNextTriggerAt);
            final Long _tmpLastFiredAt;
            if (_cursor.isNull(_cursorIndexOfLastFiredAt)) {
              _tmpLastFiredAt = null;
            } else {
              _tmpLastFiredAt = _cursor.getLong(_cursorIndexOfLastFiredAt);
            }
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            final RecurrenceType _tmpRecurrenceType;
            _tmpRecurrenceType = __RecurrenceType_stringToEnum(_cursor.getString(_cursorIndexOfRecurrenceType));
            final String _tmpRecurrenceJson;
            if (_cursor.isNull(_cursorIndexOfRecurrenceJson)) {
              _tmpRecurrenceJson = null;
            } else {
              _tmpRecurrenceJson = _cursor.getString(_cursorIndexOfRecurrenceJson);
            }
            final Long _tmpSnoozeUntil;
            if (_cursor.isNull(_cursorIndexOfSnoozeUntil)) {
              _tmpSnoozeUntil = null;
            } else {
              _tmpSnoozeUntil = _cursor.getLong(_cursorIndexOfSnoozeUntil);
            }
            final MissedPolicy _tmpMissedPolicy;
            _tmpMissedPolicy = __MissedPolicy_stringToEnum(_cursor.getString(_cursorIndexOfMissedPolicy));
            final boolean _tmpLoopPlayback;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfLoopPlayback);
            _tmpLoopPlayback = _tmp_1 != 0;
            final int _tmpFollowUpCheckMinutes;
            _tmpFollowUpCheckMinutes = _cursor.getInt(_cursorIndexOfFollowUpCheckMinutes);
            final Long _tmpPendingFollowUpAt;
            if (_cursor.isNull(_cursorIndexOfPendingFollowUpAt)) {
              _tmpPendingFollowUpAt = null;
            } else {
              _tmpPendingFollowUpAt = _cursor.getLong(_cursorIndexOfPendingFollowUpAt);
            }
            _item = new ReminderEntity(_tmpId,_tmpTitle,_tmpReminderText,_tmpTranscript,_tmpAudioPath,_tmpCreatedAt,_tmpNextTriggerAt,_tmpLastFiredAt,_tmpIsCompleted,_tmpCompletedAt,_tmpRecurrenceType,_tmpRecurrenceJson,_tmpSnoozeUntil,_tmpMissedPolicy,_tmpLoopPlayback,_tmpFollowUpCheckMinutes,_tmpPendingFollowUpAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private String __RecurrenceType_enumToString(@NonNull final RecurrenceType _value) {
    switch (_value) {
      case NONE: return "NONE";
      case DAILY: return "DAILY";
      case WEEKLY: return "WEEKLY";
      case MONTHLY: return "MONTHLY";
      case CUSTOM: return "CUSTOM";
      case YEARLY: return "YEARLY";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private String __MissedPolicy_enumToString(@NonNull final MissedPolicy _value) {
    switch (_value) {
      case FIRE_ON_RESUME: return "FIRE_ON_RESUME";
      case SKIP_TO_NEXT: return "SKIP_TO_NEXT";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private RecurrenceType __RecurrenceType_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "NONE": return RecurrenceType.NONE;
      case "DAILY": return RecurrenceType.DAILY;
      case "WEEKLY": return RecurrenceType.WEEKLY;
      case "MONTHLY": return RecurrenceType.MONTHLY;
      case "CUSTOM": return RecurrenceType.CUSTOM;
      case "YEARLY": return RecurrenceType.YEARLY;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }

  private MissedPolicy __MissedPolicy_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "FIRE_ON_RESUME": return MissedPolicy.FIRE_ON_RESUME;
      case "SKIP_TO_NEXT": return MissedPolicy.SKIP_TO_NEXT;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
