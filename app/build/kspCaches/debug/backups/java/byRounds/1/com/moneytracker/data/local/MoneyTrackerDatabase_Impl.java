package com.moneytracker.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MoneyTrackerDatabase_Impl extends MoneyTrackerDatabase {
  private volatile CategoryDao _categoryDao;

  private volatile SubCategoryDao _subCategoryDao;

  private volatile DetailDao _detailDao;

  private volatile TransactionDao _transactionDao;

  private volatile GroceryDao _groceryDao;

  private volatile TaxiFareDao _taxiFareDao;

  private volatile ProfileDao _profileDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(13) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `iconName` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `sub_categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, `name` TEXT NOT NULL, `categoryId` INTEGER, `iconName` TEXT NOT NULL, `type` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `details` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, `name` TEXT NOT NULL, `categoryId` INTEGER, `subCategoryId` INTEGER, `iconName` TEXT NOT NULL, `type` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, `profileId` INTEGER NOT NULL, `amount` REAL NOT NULL, `type` TEXT NOT NULL, `categoryId` INTEGER NOT NULL, `note` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `subCategory` TEXT NOT NULL, `detail` TEXT NOT NULL, `isRecurring` INTEGER NOT NULL, `recurrenceFrequency` TEXT, `recurTillDate` INTEGER, `recurCount` INTEGER, FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_date` ON `transactions` (`date`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_sortOrder` ON `transactions` (`sortOrder`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_profileId` ON `transactions` (`profileId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `grocery_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `itemName` TEXT NOT NULL, `size` REAL NOT NULL, `sizeUnit` TEXT NOT NULL, `category` TEXT NOT NULL, `subCategory` TEXT NOT NULL, `unitPrice` REAL NOT NULL, `quantity` INTEGER NOT NULL, `totalPrice` REAL NOT NULL, `isChecked` INTEGER NOT NULL, `transactionId` INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `taxi_fares` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, `routeName` TEXT NOT NULL, `farePerTrip` REAL NOT NULL, `tripsPerDay` INTEGER NOT NULL, `workingDaysPerMonth` INTEGER NOT NULL, `monthlyTotal` REAL NOT NULL, `date` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `username` TEXT NOT NULL, `isGuest` INTEGER NOT NULL, `isPasswordProtected` INTEGER NOT NULL, `passwordHash` TEXT, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '8faa027c26f87e27a6b375a8a911a9c3')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `categories`");
        db.execSQL("DROP TABLE IF EXISTS `sub_categories`");
        db.execSQL("DROP TABLE IF EXISTS `details`");
        db.execSQL("DROP TABLE IF EXISTS `transactions`");
        db.execSQL("DROP TABLE IF EXISTS `grocery_items`");
        db.execSQL("DROP TABLE IF EXISTS `taxi_fares`");
        db.execSQL("DROP TABLE IF EXISTS `profiles`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsCategories = new HashMap<String, TableInfo.Column>(5);
        _columnsCategories.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("profileId", new TableInfo.Column("profileId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("iconName", new TableInfo.Column("iconName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCategories = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCategories = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCategories = new TableInfo("categories", _columnsCategories, _foreignKeysCategories, _indicesCategories);
        final TableInfo _existingCategories = TableInfo.read(db, "categories");
        if (!_infoCategories.equals(_existingCategories)) {
          return new RoomOpenHelper.ValidationResult(false, "categories(com.moneytracker.data.local.entity.CategoryEntity).\n"
                  + " Expected:\n" + _infoCategories + "\n"
                  + " Found:\n" + _existingCategories);
        }
        final HashMap<String, TableInfo.Column> _columnsSubCategories = new HashMap<String, TableInfo.Column>(6);
        _columnsSubCategories.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSubCategories.put("profileId", new TableInfo.Column("profileId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSubCategories.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSubCategories.put("categoryId", new TableInfo.Column("categoryId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSubCategories.put("iconName", new TableInfo.Column("iconName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSubCategories.put("type", new TableInfo.Column("type", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSubCategories = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSubCategories = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSubCategories = new TableInfo("sub_categories", _columnsSubCategories, _foreignKeysSubCategories, _indicesSubCategories);
        final TableInfo _existingSubCategories = TableInfo.read(db, "sub_categories");
        if (!_infoSubCategories.equals(_existingSubCategories)) {
          return new RoomOpenHelper.ValidationResult(false, "sub_categories(com.moneytracker.data.local.entity.SubCategoryEntity).\n"
                  + " Expected:\n" + _infoSubCategories + "\n"
                  + " Found:\n" + _existingSubCategories);
        }
        final HashMap<String, TableInfo.Column> _columnsDetails = new HashMap<String, TableInfo.Column>(7);
        _columnsDetails.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDetails.put("profileId", new TableInfo.Column("profileId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDetails.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDetails.put("categoryId", new TableInfo.Column("categoryId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDetails.put("subCategoryId", new TableInfo.Column("subCategoryId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDetails.put("iconName", new TableInfo.Column("iconName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDetails.put("type", new TableInfo.Column("type", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDetails = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDetails = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDetails = new TableInfo("details", _columnsDetails, _foreignKeysDetails, _indicesDetails);
        final TableInfo _existingDetails = TableInfo.read(db, "details");
        if (!_infoDetails.equals(_existingDetails)) {
          return new RoomOpenHelper.ValidationResult(false, "details(com.moneytracker.data.local.entity.DetailEntity).\n"
                  + " Expected:\n" + _infoDetails + "\n"
                  + " Found:\n" + _existingDetails);
        }
        final HashMap<String, TableInfo.Column> _columnsTransactions = new HashMap<String, TableInfo.Column>(14);
        _columnsTransactions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("date", new TableInfo.Column("date", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("profileId", new TableInfo.Column("profileId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("categoryId", new TableInfo.Column("categoryId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("subCategory", new TableInfo.Column("subCategory", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("detail", new TableInfo.Column("detail", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("isRecurring", new TableInfo.Column("isRecurring", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("recurrenceFrequency", new TableInfo.Column("recurrenceFrequency", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("recurTillDate", new TableInfo.Column("recurTillDate", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("recurCount", new TableInfo.Column("recurCount", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTransactions = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysTransactions.add(new TableInfo.ForeignKey("categories", "CASCADE", "NO ACTION", Arrays.asList("categoryId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesTransactions = new HashSet<TableInfo.Index>(4);
        _indicesTransactions.add(new TableInfo.Index("index_transactions_categoryId", false, Arrays.asList("categoryId"), Arrays.asList("ASC")));
        _indicesTransactions.add(new TableInfo.Index("index_transactions_date", false, Arrays.asList("date"), Arrays.asList("ASC")));
        _indicesTransactions.add(new TableInfo.Index("index_transactions_sortOrder", false, Arrays.asList("sortOrder"), Arrays.asList("ASC")));
        _indicesTransactions.add(new TableInfo.Index("index_transactions_profileId", false, Arrays.asList("profileId"), Arrays.asList("ASC")));
        final TableInfo _infoTransactions = new TableInfo("transactions", _columnsTransactions, _foreignKeysTransactions, _indicesTransactions);
        final TableInfo _existingTransactions = TableInfo.read(db, "transactions");
        if (!_infoTransactions.equals(_existingTransactions)) {
          return new RoomOpenHelper.ValidationResult(false, "transactions(com.moneytracker.data.local.entity.TransactionEntity).\n"
                  + " Expected:\n" + _infoTransactions + "\n"
                  + " Found:\n" + _existingTransactions);
        }
        final HashMap<String, TableInfo.Column> _columnsGroceryItems = new HashMap<String, TableInfo.Column>(13);
        _columnsGroceryItems.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroceryItems.put("profileId", new TableInfo.Column("profileId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroceryItems.put("date", new TableInfo.Column("date", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroceryItems.put("itemName", new TableInfo.Column("itemName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroceryItems.put("size", new TableInfo.Column("size", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroceryItems.put("sizeUnit", new TableInfo.Column("sizeUnit", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroceryItems.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroceryItems.put("subCategory", new TableInfo.Column("subCategory", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroceryItems.put("unitPrice", new TableInfo.Column("unitPrice", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroceryItems.put("quantity", new TableInfo.Column("quantity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroceryItems.put("totalPrice", new TableInfo.Column("totalPrice", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroceryItems.put("isChecked", new TableInfo.Column("isChecked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroceryItems.put("transactionId", new TableInfo.Column("transactionId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGroceryItems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGroceryItems = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGroceryItems = new TableInfo("grocery_items", _columnsGroceryItems, _foreignKeysGroceryItems, _indicesGroceryItems);
        final TableInfo _existingGroceryItems = TableInfo.read(db, "grocery_items");
        if (!_infoGroceryItems.equals(_existingGroceryItems)) {
          return new RoomOpenHelper.ValidationResult(false, "grocery_items(com.moneytracker.data.local.entity.GroceryItemEntity).\n"
                  + " Expected:\n" + _infoGroceryItems + "\n"
                  + " Found:\n" + _existingGroceryItems);
        }
        final HashMap<String, TableInfo.Column> _columnsTaxiFares = new HashMap<String, TableInfo.Column>(8);
        _columnsTaxiFares.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTaxiFares.put("profileId", new TableInfo.Column("profileId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTaxiFares.put("routeName", new TableInfo.Column("routeName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTaxiFares.put("farePerTrip", new TableInfo.Column("farePerTrip", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTaxiFares.put("tripsPerDay", new TableInfo.Column("tripsPerDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTaxiFares.put("workingDaysPerMonth", new TableInfo.Column("workingDaysPerMonth", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTaxiFares.put("monthlyTotal", new TableInfo.Column("monthlyTotal", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTaxiFares.put("date", new TableInfo.Column("date", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTaxiFares = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTaxiFares = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTaxiFares = new TableInfo("taxi_fares", _columnsTaxiFares, _foreignKeysTaxiFares, _indicesTaxiFares);
        final TableInfo _existingTaxiFares = TableInfo.read(db, "taxi_fares");
        if (!_infoTaxiFares.equals(_existingTaxiFares)) {
          return new RoomOpenHelper.ValidationResult(false, "taxi_fares(com.moneytracker.data.local.entity.TaxiFareEntity).\n"
                  + " Expected:\n" + _infoTaxiFares + "\n"
                  + " Found:\n" + _existingTaxiFares);
        }
        final HashMap<String, TableInfo.Column> _columnsProfiles = new HashMap<String, TableInfo.Column>(6);
        _columnsProfiles.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfiles.put("username", new TableInfo.Column("username", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfiles.put("isGuest", new TableInfo.Column("isGuest", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfiles.put("isPasswordProtected", new TableInfo.Column("isPasswordProtected", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfiles.put("passwordHash", new TableInfo.Column("passwordHash", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfiles.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysProfiles = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesProfiles = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoProfiles = new TableInfo("profiles", _columnsProfiles, _foreignKeysProfiles, _indicesProfiles);
        final TableInfo _existingProfiles = TableInfo.read(db, "profiles");
        if (!_infoProfiles.equals(_existingProfiles)) {
          return new RoomOpenHelper.ValidationResult(false, "profiles(com.moneytracker.data.local.entity.ProfileEntity).\n"
                  + " Expected:\n" + _infoProfiles + "\n"
                  + " Found:\n" + _existingProfiles);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "8faa027c26f87e27a6b375a8a911a9c3", "d4811983b605da2d41bf3d253f5bdb52");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "categories","sub_categories","details","transactions","grocery_items","taxi_fares","profiles");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `categories`");
      _db.execSQL("DELETE FROM `sub_categories`");
      _db.execSQL("DELETE FROM `details`");
      _db.execSQL("DELETE FROM `transactions`");
      _db.execSQL("DELETE FROM `grocery_items`");
      _db.execSQL("DELETE FROM `taxi_fares`");
      _db.execSQL("DELETE FROM `profiles`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(CategoryDao.class, CategoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SubCategoryDao.class, SubCategoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DetailDao.class, DetailDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TransactionDao.class, TransactionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(GroceryDao.class, GroceryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TaxiFareDao.class, TaxiFareDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ProfileDao.class, ProfileDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public CategoryDao categoryDao() {
    if (_categoryDao != null) {
      return _categoryDao;
    } else {
      synchronized(this) {
        if(_categoryDao == null) {
          _categoryDao = new CategoryDao_Impl(this);
        }
        return _categoryDao;
      }
    }
  }

  @Override
  public SubCategoryDao subCategoryDao() {
    if (_subCategoryDao != null) {
      return _subCategoryDao;
    } else {
      synchronized(this) {
        if(_subCategoryDao == null) {
          _subCategoryDao = new SubCategoryDao_Impl(this);
        }
        return _subCategoryDao;
      }
    }
  }

  @Override
  public DetailDao detailDao() {
    if (_detailDao != null) {
      return _detailDao;
    } else {
      synchronized(this) {
        if(_detailDao == null) {
          _detailDao = new DetailDao_Impl(this);
        }
        return _detailDao;
      }
    }
  }

  @Override
  public TransactionDao transactionDao() {
    if (_transactionDao != null) {
      return _transactionDao;
    } else {
      synchronized(this) {
        if(_transactionDao == null) {
          _transactionDao = new TransactionDao_Impl(this);
        }
        return _transactionDao;
      }
    }
  }

  @Override
  public GroceryDao groceryDao() {
    if (_groceryDao != null) {
      return _groceryDao;
    } else {
      synchronized(this) {
        if(_groceryDao == null) {
          _groceryDao = new GroceryDao_Impl(this);
        }
        return _groceryDao;
      }
    }
  }

  @Override
  public TaxiFareDao taxiFareDao() {
    if (_taxiFareDao != null) {
      return _taxiFareDao;
    } else {
      synchronized(this) {
        if(_taxiFareDao == null) {
          _taxiFareDao = new TaxiFareDao_Impl(this);
        }
        return _taxiFareDao;
      }
    }
  }

  @Override
  public ProfileDao profileDao() {
    if (_profileDao != null) {
      return _profileDao;
    } else {
      synchronized(this) {
        if(_profileDao == null) {
          _profileDao = new ProfileDao_Impl(this);
        }
        return _profileDao;
      }
    }
  }
}
