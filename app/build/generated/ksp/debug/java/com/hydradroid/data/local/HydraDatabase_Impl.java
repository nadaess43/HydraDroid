package com.hydradroid.data.local;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class HydraDatabase_Impl extends HydraDatabase {
  private volatile LibraryDao _libraryDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(4) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `library_games` (`key` TEXT NOT NULL, `objectId` TEXT NOT NULL, `shop` TEXT NOT NULL, `title` TEXT NOT NULL, `coverUrl` TEXT, `iconUrl` TEXT, `heroUrl` TEXT, `logoUrl` TEXT, `playTimeMs` INTEGER NOT NULL, `lastPlayed` INTEGER, `favorite` INTEGER NOT NULL, `pinned` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, `localPath` TEXT, PRIMARY KEY(`key`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `collections` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `collection_games` (`gameKey` TEXT NOT NULL, `collectionId` TEXT NOT NULL, PRIMARY KEY(`gameKey`, `collectionId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `assets_cache` (`key` TEXT NOT NULL, `json` TEXT NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`key`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `download_queue` (`id` TEXT NOT NULL, `objectId` TEXT NOT NULL, `shop` TEXT NOT NULL, `title` TEXT NOT NULL, `fileSize` TEXT, `sourceName` TEXT NOT NULL, `uri` TEXT NOT NULL, `status` TEXT NOT NULL, `progress` REAL NOT NULL, `kind` TEXT NOT NULL, `magnet` TEXT, `torrentFilePath` TEXT, `saveDir` TEXT, `fileName` TEXT, `totalBytes` INTEGER NOT NULL, `doneBytes` INTEGER NOT NULL, `uploadBytes` INTEGER NOT NULL, `downSpeed` INTEGER NOT NULL, `upSpeed` INTEGER NOT NULL, `etaSec` INTEGER NOT NULL, `peers` INTEGER NOT NULL, `seeds` INTEGER NOT NULL, `infoHash` TEXT, `error` TEXT, `queuedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `download_sources` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `url` TEXT NOT NULL, `fingerprint` TEXT, `enabled` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '2ac0b03df36e2d1a69e01ae4620d161d')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `library_games`");
        db.execSQL("DROP TABLE IF EXISTS `collections`");
        db.execSQL("DROP TABLE IF EXISTS `collection_games`");
        db.execSQL("DROP TABLE IF EXISTS `assets_cache`");
        db.execSQL("DROP TABLE IF EXISTS `download_queue`");
        db.execSQL("DROP TABLE IF EXISTS `download_sources`");
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
        final HashMap<String, TableInfo.Column> _columnsLibraryGames = new HashMap<String, TableInfo.Column>(14);
        _columnsLibraryGames.put("key", new TableInfo.Column("key", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLibraryGames.put("objectId", new TableInfo.Column("objectId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLibraryGames.put("shop", new TableInfo.Column("shop", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLibraryGames.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLibraryGames.put("coverUrl", new TableInfo.Column("coverUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLibraryGames.put("iconUrl", new TableInfo.Column("iconUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLibraryGames.put("heroUrl", new TableInfo.Column("heroUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLibraryGames.put("logoUrl", new TableInfo.Column("logoUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLibraryGames.put("playTimeMs", new TableInfo.Column("playTimeMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLibraryGames.put("lastPlayed", new TableInfo.Column("lastPlayed", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLibraryGames.put("favorite", new TableInfo.Column("favorite", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLibraryGames.put("pinned", new TableInfo.Column("pinned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLibraryGames.put("addedAt", new TableInfo.Column("addedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLibraryGames.put("localPath", new TableInfo.Column("localPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLibraryGames = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLibraryGames = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLibraryGames = new TableInfo("library_games", _columnsLibraryGames, _foreignKeysLibraryGames, _indicesLibraryGames);
        final TableInfo _existingLibraryGames = TableInfo.read(db, "library_games");
        if (!_infoLibraryGames.equals(_existingLibraryGames)) {
          return new RoomOpenHelper.ValidationResult(false, "library_games(com.hydradroid.data.local.LibraryGameEntity).\n"
                  + " Expected:\n" + _infoLibraryGames + "\n"
                  + " Found:\n" + _existingLibraryGames);
        }
        final HashMap<String, TableInfo.Column> _columnsCollections = new HashMap<String, TableInfo.Column>(2);
        _columnsCollections.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCollections.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCollections = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCollections = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCollections = new TableInfo("collections", _columnsCollections, _foreignKeysCollections, _indicesCollections);
        final TableInfo _existingCollections = TableInfo.read(db, "collections");
        if (!_infoCollections.equals(_existingCollections)) {
          return new RoomOpenHelper.ValidationResult(false, "collections(com.hydradroid.data.local.CollectionEntity).\n"
                  + " Expected:\n" + _infoCollections + "\n"
                  + " Found:\n" + _existingCollections);
        }
        final HashMap<String, TableInfo.Column> _columnsCollectionGames = new HashMap<String, TableInfo.Column>(2);
        _columnsCollectionGames.put("gameKey", new TableInfo.Column("gameKey", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCollectionGames.put("collectionId", new TableInfo.Column("collectionId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCollectionGames = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCollectionGames = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCollectionGames = new TableInfo("collection_games", _columnsCollectionGames, _foreignKeysCollectionGames, _indicesCollectionGames);
        final TableInfo _existingCollectionGames = TableInfo.read(db, "collection_games");
        if (!_infoCollectionGames.equals(_existingCollectionGames)) {
          return new RoomOpenHelper.ValidationResult(false, "collection_games(com.hydradroid.data.local.CollectionGameCrossRef).\n"
                  + " Expected:\n" + _infoCollectionGames + "\n"
                  + " Found:\n" + _existingCollectionGames);
        }
        final HashMap<String, TableInfo.Column> _columnsAssetsCache = new HashMap<String, TableInfo.Column>(3);
        _columnsAssetsCache.put("key", new TableInfo.Column("key", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAssetsCache.put("json", new TableInfo.Column("json", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAssetsCache.put("cachedAt", new TableInfo.Column("cachedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAssetsCache = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAssetsCache = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAssetsCache = new TableInfo("assets_cache", _columnsAssetsCache, _foreignKeysAssetsCache, _indicesAssetsCache);
        final TableInfo _existingAssetsCache = TableInfo.read(db, "assets_cache");
        if (!_infoAssetsCache.equals(_existingAssetsCache)) {
          return new RoomOpenHelper.ValidationResult(false, "assets_cache(com.hydradroid.data.local.AssetsCacheEntity).\n"
                  + " Expected:\n" + _infoAssetsCache + "\n"
                  + " Found:\n" + _existingAssetsCache);
        }
        final HashMap<String, TableInfo.Column> _columnsDownloadQueue = new HashMap<String, TableInfo.Column>(25);
        _columnsDownloadQueue.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("objectId", new TableInfo.Column("objectId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("shop", new TableInfo.Column("shop", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("fileSize", new TableInfo.Column("fileSize", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("sourceName", new TableInfo.Column("sourceName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("uri", new TableInfo.Column("uri", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("progress", new TableInfo.Column("progress", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("kind", new TableInfo.Column("kind", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("magnet", new TableInfo.Column("magnet", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("torrentFilePath", new TableInfo.Column("torrentFilePath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("saveDir", new TableInfo.Column("saveDir", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("fileName", new TableInfo.Column("fileName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("totalBytes", new TableInfo.Column("totalBytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("doneBytes", new TableInfo.Column("doneBytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("uploadBytes", new TableInfo.Column("uploadBytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("downSpeed", new TableInfo.Column("downSpeed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("upSpeed", new TableInfo.Column("upSpeed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("etaSec", new TableInfo.Column("etaSec", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("peers", new TableInfo.Column("peers", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("seeds", new TableInfo.Column("seeds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("infoHash", new TableInfo.Column("infoHash", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("error", new TableInfo.Column("error", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadQueue.put("queuedAt", new TableInfo.Column("queuedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDownloadQueue = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDownloadQueue = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDownloadQueue = new TableInfo("download_queue", _columnsDownloadQueue, _foreignKeysDownloadQueue, _indicesDownloadQueue);
        final TableInfo _existingDownloadQueue = TableInfo.read(db, "download_queue");
        if (!_infoDownloadQueue.equals(_existingDownloadQueue)) {
          return new RoomOpenHelper.ValidationResult(false, "download_queue(com.hydradroid.data.local.DownloadEntity).\n"
                  + " Expected:\n" + _infoDownloadQueue + "\n"
                  + " Found:\n" + _existingDownloadQueue);
        }
        final HashMap<String, TableInfo.Column> _columnsDownloadSources = new HashMap<String, TableInfo.Column>(5);
        _columnsDownloadSources.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadSources.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadSources.put("url", new TableInfo.Column("url", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadSources.put("fingerprint", new TableInfo.Column("fingerprint", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadSources.put("enabled", new TableInfo.Column("enabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDownloadSources = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDownloadSources = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDownloadSources = new TableInfo("download_sources", _columnsDownloadSources, _foreignKeysDownloadSources, _indicesDownloadSources);
        final TableInfo _existingDownloadSources = TableInfo.read(db, "download_sources");
        if (!_infoDownloadSources.equals(_existingDownloadSources)) {
          return new RoomOpenHelper.ValidationResult(false, "download_sources(com.hydradroid.data.local.DownloadSourceEntity).\n"
                  + " Expected:\n" + _infoDownloadSources + "\n"
                  + " Found:\n" + _existingDownloadSources);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "2ac0b03df36e2d1a69e01ae4620d161d", "a19d0b0f740c34bd63067b2b77dcfd07");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "library_games","collections","collection_games","assets_cache","download_queue","download_sources");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `library_games`");
      _db.execSQL("DELETE FROM `collections`");
      _db.execSQL("DELETE FROM `collection_games`");
      _db.execSQL("DELETE FROM `assets_cache`");
      _db.execSQL("DELETE FROM `download_queue`");
      _db.execSQL("DELETE FROM `download_sources`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
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
    _typeConvertersMap.put(LibraryDao.class, LibraryDao_Impl.getRequiredConverters());
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
  public LibraryDao libraryDao() {
    if (_libraryDao != null) {
      return _libraryDao;
    } else {
      synchronized(this) {
        if(_libraryDao == null) {
          _libraryDao = new LibraryDao_Impl(this);
        }
        return _libraryDao;
      }
    }
  }
}
