package com.moneytracker.data.local;

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
import com.moneytracker.data.local.entity.CategorySummary;
import com.moneytracker.data.local.entity.RecurrenceFrequency;
import com.moneytracker.data.local.entity.TransactionEntity;
import com.moneytracker.data.local.entity.TransactionType;
import com.moneytracker.data.local.entity.TransactionWithCategory;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Integer;
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
public final class TransactionDao_Impl implements TransactionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TransactionEntity> __insertionAdapterOfTransactionEntity;

  private final TransactionTypeConverter __transactionTypeConverter = new TransactionTypeConverter();

  private final RecurrenceFrequencyConverter __recurrenceFrequencyConverter = new RecurrenceFrequencyConverter();

  private final EntityDeletionOrUpdateAdapter<TransactionEntity> __deletionAdapterOfTransactionEntity;

  private final EntityDeletionOrUpdateAdapter<TransactionEntity> __updateAdapterOfTransactionEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateSortOrder;

  public TransactionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTransactionEntity = new EntityInsertionAdapter<TransactionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `transactions` (`id`,`amount`,`type`,`categoryId`,`date`,`note`,`sortOrder`,`subCategory`,`isRecurring`,`recurrenceFrequency`,`recurTillDate`,`recurCount`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TransactionEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindDouble(2, entity.getAmount());
        final String _tmp = __transactionTypeConverter.fromType(entity.getType());
        statement.bindString(3, _tmp);
        statement.bindLong(4, entity.getCategoryId());
        statement.bindLong(5, entity.getDate());
        statement.bindString(6, entity.getNote());
        statement.bindLong(7, entity.getSortOrder());
        statement.bindString(8, entity.getSubCategory());
        final int _tmp_1 = entity.isRecurring() ? 1 : 0;
        statement.bindLong(9, _tmp_1);
        final String _tmp_2 = __recurrenceFrequencyConverter.fromFrequency(entity.getRecurrenceFrequency());
        if (_tmp_2 == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, _tmp_2);
        }
        if (entity.getRecurTillDate() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getRecurTillDate());
        }
        if (entity.getRecurCount() == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, entity.getRecurCount());
        }
      }
    };
    this.__deletionAdapterOfTransactionEntity = new EntityDeletionOrUpdateAdapter<TransactionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `transactions` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TransactionEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfTransactionEntity = new EntityDeletionOrUpdateAdapter<TransactionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `transactions` SET `id` = ?,`amount` = ?,`type` = ?,`categoryId` = ?,`date` = ?,`note` = ?,`sortOrder` = ?,`subCategory` = ?,`isRecurring` = ?,`recurrenceFrequency` = ?,`recurTillDate` = ?,`recurCount` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TransactionEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindDouble(2, entity.getAmount());
        final String _tmp = __transactionTypeConverter.fromType(entity.getType());
        statement.bindString(3, _tmp);
        statement.bindLong(4, entity.getCategoryId());
        statement.bindLong(5, entity.getDate());
        statement.bindString(6, entity.getNote());
        statement.bindLong(7, entity.getSortOrder());
        statement.bindString(8, entity.getSubCategory());
        final int _tmp_1 = entity.isRecurring() ? 1 : 0;
        statement.bindLong(9, _tmp_1);
        final String _tmp_2 = __recurrenceFrequencyConverter.fromFrequency(entity.getRecurrenceFrequency());
        if (_tmp_2 == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, _tmp_2);
        }
        if (entity.getRecurTillDate() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getRecurTillDate());
        }
        if (entity.getRecurCount() == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, entity.getRecurCount());
        }
        statement.bindLong(13, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateSortOrder = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE transactions SET sortOrder = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final TransactionEntity transaction,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfTransactionEntity.insertAndReturnId(transaction);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final TransactionEntity transaction,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfTransactionEntity.handle(transaction);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final TransactionEntity transaction,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfTransactionEntity.handle(transaction);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSortOrder(final long id, final int sortOrder,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateSortOrder.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, sortOrder);
        _argIndex = 2;
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
          __preparedStmtOfUpdateSortOrder.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<TransactionWithCategory>> observeAllWithCategory() {
    final String _sql = "\n"
            + "        SELECT t.id, t.amount, t.type, t.categoryId, t.date, t.note, t.sortOrder, t.subCategory,\n"
            + "               c.name AS categoryName, c.iconName AS categoryIconName\n"
            + "        FROM transactions t\n"
            + "        INNER JOIN categories c ON t.categoryId = c.id\n"
            + "        ORDER BY t.sortOrder ASC, t.date DESC, t.id DESC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions",
        "categories"}, new Callable<List<TransactionWithCategory>>() {
      @Override
      @NonNull
      public List<TransactionWithCategory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = 0;
          final int _cursorIndexOfAmount = 1;
          final int _cursorIndexOfType = 2;
          final int _cursorIndexOfCategoryId = 3;
          final int _cursorIndexOfDate = 4;
          final int _cursorIndexOfNote = 5;
          final int _cursorIndexOfSortOrder = 6;
          final int _cursorIndexOfSubCategory = 7;
          final int _cursorIndexOfCategoryName = 8;
          final int _cursorIndexOfCategoryIconName = 9;
          final List<TransactionWithCategory> _result = new ArrayList<TransactionWithCategory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionWithCategory _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final TransactionType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __transactionTypeConverter.toType(_tmp);
            final long _tmpCategoryId;
            _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final String _tmpSubCategory;
            _tmpSubCategory = _cursor.getString(_cursorIndexOfSubCategory);
            final String _tmpCategoryName;
            _tmpCategoryName = _cursor.getString(_cursorIndexOfCategoryName);
            final String _tmpCategoryIconName;
            _tmpCategoryIconName = _cursor.getString(_cursorIndexOfCategoryIconName);
            _item = new TransactionWithCategory(_tmpId,_tmpAmount,_tmpType,_tmpCategoryId,_tmpDate,_tmpNote,_tmpSortOrder,_tmpSubCategory,_tmpCategoryName,_tmpCategoryIconName);
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
  public Flow<List<TransactionWithCategory>> observeByDateRange(final long startDate,
      final long endDate) {
    final String _sql = "\n"
            + "        SELECT t.id, t.amount, t.type, t.categoryId, t.date, t.note, t.sortOrder, t.subCategory,\n"
            + "               c.name AS categoryName, c.iconName AS categoryIconName\n"
            + "        FROM transactions t\n"
            + "        INNER JOIN categories c ON t.categoryId = c.id\n"
            + "        WHERE t.date >= ? AND t.date < ?\n"
            + "        ORDER BY t.sortOrder ASC, t.date DESC, t.id DESC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions",
        "categories"}, new Callable<List<TransactionWithCategory>>() {
      @Override
      @NonNull
      public List<TransactionWithCategory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = 0;
          final int _cursorIndexOfAmount = 1;
          final int _cursorIndexOfType = 2;
          final int _cursorIndexOfCategoryId = 3;
          final int _cursorIndexOfDate = 4;
          final int _cursorIndexOfNote = 5;
          final int _cursorIndexOfSortOrder = 6;
          final int _cursorIndexOfSubCategory = 7;
          final int _cursorIndexOfCategoryName = 8;
          final int _cursorIndexOfCategoryIconName = 9;
          final List<TransactionWithCategory> _result = new ArrayList<TransactionWithCategory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionWithCategory _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final TransactionType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __transactionTypeConverter.toType(_tmp);
            final long _tmpCategoryId;
            _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final String _tmpSubCategory;
            _tmpSubCategory = _cursor.getString(_cursorIndexOfSubCategory);
            final String _tmpCategoryName;
            _tmpCategoryName = _cursor.getString(_cursorIndexOfCategoryName);
            final String _tmpCategoryIconName;
            _tmpCategoryIconName = _cursor.getString(_cursorIndexOfCategoryIconName);
            _item = new TransactionWithCategory(_tmpId,_tmpAmount,_tmpType,_tmpCategoryId,_tmpDate,_tmpNote,_tmpSortOrder,_tmpSubCategory,_tmpCategoryName,_tmpCategoryIconName);
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
  public Object getById(final long id, final Continuation<? super TransactionEntity> $completion) {
    final String _sql = "SELECT * FROM transactions WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TransactionEntity>() {
      @Override
      @Nullable
      public TransactionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfSubCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "subCategory");
          final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
          final int _cursorIndexOfRecurrenceFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "recurrenceFrequency");
          final int _cursorIndexOfRecurTillDate = CursorUtil.getColumnIndexOrThrow(_cursor, "recurTillDate");
          final int _cursorIndexOfRecurCount = CursorUtil.getColumnIndexOrThrow(_cursor, "recurCount");
          final TransactionEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final TransactionType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __transactionTypeConverter.toType(_tmp);
            final long _tmpCategoryId;
            _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final String _tmpSubCategory;
            _tmpSubCategory = _cursor.getString(_cursorIndexOfSubCategory);
            final boolean _tmpIsRecurring;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsRecurring);
            _tmpIsRecurring = _tmp_1 != 0;
            final RecurrenceFrequency _tmpRecurrenceFrequency;
            final String _tmp_2;
            if (_cursor.isNull(_cursorIndexOfRecurrenceFrequency)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getString(_cursorIndexOfRecurrenceFrequency);
            }
            _tmpRecurrenceFrequency = __recurrenceFrequencyConverter.toFrequency(_tmp_2);
            final Long _tmpRecurTillDate;
            if (_cursor.isNull(_cursorIndexOfRecurTillDate)) {
              _tmpRecurTillDate = null;
            } else {
              _tmpRecurTillDate = _cursor.getLong(_cursorIndexOfRecurTillDate);
            }
            final Integer _tmpRecurCount;
            if (_cursor.isNull(_cursorIndexOfRecurCount)) {
              _tmpRecurCount = null;
            } else {
              _tmpRecurCount = _cursor.getInt(_cursorIndexOfRecurCount);
            }
            _result = new TransactionEntity(_tmpId,_tmpAmount,_tmpType,_tmpCategoryId,_tmpDate,_tmpNote,_tmpSortOrder,_tmpSubCategory,_tmpIsRecurring,_tmpRecurrenceFrequency,_tmpRecurTillDate,_tmpRecurCount);
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
  public Flow<Double> observeTotalByTypeAndDateRange(final TransactionType type,
      final long startDate, final long endDate) {
    final String _sql = "\n"
            + "        SELECT COALESCE(SUM(amount), 0)\n"
            + "        FROM transactions\n"
            + "        WHERE type = ? AND date >= ? AND date < ?\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    final String _tmp = __transactionTypeConverter.fromType(type);
    _statement.bindString(_argIndex, _tmp);
    _argIndex = 2;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 3;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<Double>() {
      @Override
      @NonNull
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final double _tmp_1;
            _tmp_1 = _cursor.getDouble(0);
            _result = _tmp_1;
          } else {
            _result = 0.0;
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
  public Flow<List<CategorySummary>> observeCategorySummaries(final TransactionType type,
      final long startDate, final long endDate) {
    final String _sql = "\n"
            + "        SELECT c.id AS categoryId, c.name AS categoryName, COALESCE(SUM(t.amount), 0) AS total\n"
            + "        FROM categories c\n"
            + "        LEFT JOIN transactions t ON t.categoryId = c.id\n"
            + "            AND t.type = ?\n"
            + "            AND t.date >= ?\n"
            + "            AND t.date < ?\n"
            + "        WHERE c.type = ?\n"
            + "        GROUP BY c.id, c.name\n"
            + "        HAVING total > 0\n"
            + "        ORDER BY total DESC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 4);
    int _argIndex = 1;
    final String _tmp = __transactionTypeConverter.fromType(type);
    _statement.bindString(_argIndex, _tmp);
    _argIndex = 2;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 3;
    _statement.bindLong(_argIndex, endDate);
    _argIndex = 4;
    final String _tmp_1 = __transactionTypeConverter.fromType(type);
    _statement.bindString(_argIndex, _tmp_1);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"categories",
        "transactions"}, new Callable<List<CategorySummary>>() {
      @Override
      @NonNull
      public List<CategorySummary> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfCategoryId = 0;
          final int _cursorIndexOfCategoryName = 1;
          final int _cursorIndexOfTotal = 2;
          final List<CategorySummary> _result = new ArrayList<CategorySummary>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CategorySummary _item;
            final long _tmpCategoryId;
            _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            final String _tmpCategoryName;
            _tmpCategoryName = _cursor.getString(_cursorIndexOfCategoryName);
            final double _tmpTotal;
            _tmpTotal = _cursor.getDouble(_cursorIndexOfTotal);
            _item = new CategorySummary(_tmpCategoryId,_tmpCategoryName,_tmpTotal);
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
  public Object nextSortOrder(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COALESCE(MIN(sortOrder), 0) - 1 FROM transactions";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
}
