package com.hydradroid.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.EntityUpsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Exception;
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
public final class LibraryDao_Impl implements LibraryDao {
  private final RoomDatabase __db;

  private final SharedSQLiteStatement __preparedStmtOfFlipFavorite;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  private final SharedSQLiteStatement __preparedStmtOfDeleteCollection;

  private final SharedSQLiteStatement __preparedStmtOfClearCollection;

  private final SharedSQLiteStatement __preparedStmtOfRemoveGameFromAllCollections;

  private final SharedSQLiteStatement __preparedStmtOfRemoveFromCollection;

  private final SharedSQLiteStatement __preparedStmtOfDequeue;

  private final SharedSQLiteStatement __preparedStmtOfUpdateProgress;

  private final SharedSQLiteStatement __preparedStmtOfDeleteSource;

  private final EntityUpsertionAdapter<LibraryGameEntity> __upsertionAdapterOfLibraryGameEntity;

  private final EntityUpsertionAdapter<CollectionEntity> __upsertionAdapterOfCollectionEntity;

  private final EntityUpsertionAdapter<CollectionGameCrossRef> __upsertionAdapterOfCollectionGameCrossRef;

  private final EntityUpsertionAdapter<AssetsCacheEntity> __upsertionAdapterOfAssetsCacheEntity;

  private final EntityUpsertionAdapter<DownloadEntity> __upsertionAdapterOfDownloadEntity;

  private final EntityUpsertionAdapter<DownloadSourceEntity> __upsertionAdapterOfDownloadSourceEntity;

  public LibraryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__preparedStmtOfFlipFavorite = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE library_games SET favorite = NOT favorite WHERE `key` = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM library_games WHERE `key` = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteCollection = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM collections WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearCollection = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM collection_games WHERE collectionId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfRemoveGameFromAllCollections = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM collection_games WHERE gameKey = ?";
        return _query;
      }
    };
    this.__preparedStmtOfRemoveFromCollection = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM collection_games WHERE gameKey = ? AND collectionId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDequeue = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM download_queue WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateProgress = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE download_queue SET status = ?, progress = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteSource = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM download_sources WHERE id = ?";
        return _query;
      }
    };
    this.__upsertionAdapterOfLibraryGameEntity = new EntityUpsertionAdapter<LibraryGameEntity>(new EntityInsertionAdapter<LibraryGameEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `library_games` (`key`,`objectId`,`shop`,`title`,`coverUrl`,`iconUrl`,`heroUrl`,`logoUrl`,`playTimeMs`,`lastPlayed`,`favorite`,`pinned`,`addedAt`,`localPath`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LibraryGameEntity entity) {
        statement.bindString(1, entity.getKey());
        statement.bindString(2, entity.getObjectId());
        statement.bindString(3, entity.getShop());
        statement.bindString(4, entity.getTitle());
        if (entity.getCoverUrl() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getCoverUrl());
        }
        if (entity.getIconUrl() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getIconUrl());
        }
        if (entity.getHeroUrl() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getHeroUrl());
        }
        if (entity.getLogoUrl() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getLogoUrl());
        }
        statement.bindLong(9, entity.getPlayTimeMs());
        if (entity.getLastPlayed() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getLastPlayed());
        }
        final int _tmp = entity.getFavorite() ? 1 : 0;
        statement.bindLong(11, _tmp);
        final int _tmp_1 = entity.getPinned() ? 1 : 0;
        statement.bindLong(12, _tmp_1);
        statement.bindLong(13, entity.getAddedAt());
        if (entity.getLocalPath() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getLocalPath());
        }
      }
    }, new EntityDeletionOrUpdateAdapter<LibraryGameEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `library_games` SET `key` = ?,`objectId` = ?,`shop` = ?,`title` = ?,`coverUrl` = ?,`iconUrl` = ?,`heroUrl` = ?,`logoUrl` = ?,`playTimeMs` = ?,`lastPlayed` = ?,`favorite` = ?,`pinned` = ?,`addedAt` = ?,`localPath` = ? WHERE `key` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LibraryGameEntity entity) {
        statement.bindString(1, entity.getKey());
        statement.bindString(2, entity.getObjectId());
        statement.bindString(3, entity.getShop());
        statement.bindString(4, entity.getTitle());
        if (entity.getCoverUrl() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getCoverUrl());
        }
        if (entity.getIconUrl() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getIconUrl());
        }
        if (entity.getHeroUrl() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getHeroUrl());
        }
        if (entity.getLogoUrl() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getLogoUrl());
        }
        statement.bindLong(9, entity.getPlayTimeMs());
        if (entity.getLastPlayed() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getLastPlayed());
        }
        final int _tmp = entity.getFavorite() ? 1 : 0;
        statement.bindLong(11, _tmp);
        final int _tmp_1 = entity.getPinned() ? 1 : 0;
        statement.bindLong(12, _tmp_1);
        statement.bindLong(13, entity.getAddedAt());
        if (entity.getLocalPath() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getLocalPath());
        }
        statement.bindString(15, entity.getKey());
      }
    });
    this.__upsertionAdapterOfCollectionEntity = new EntityUpsertionAdapter<CollectionEntity>(new EntityInsertionAdapter<CollectionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `collections` (`id`,`name`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CollectionEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
      }
    }, new EntityDeletionOrUpdateAdapter<CollectionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `collections` SET `id` = ?,`name` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CollectionEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getId());
      }
    });
    this.__upsertionAdapterOfCollectionGameCrossRef = new EntityUpsertionAdapter<CollectionGameCrossRef>(new EntityInsertionAdapter<CollectionGameCrossRef>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `collection_games` (`gameKey`,`collectionId`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CollectionGameCrossRef entity) {
        statement.bindString(1, entity.getGameKey());
        statement.bindString(2, entity.getCollectionId());
      }
    }, new EntityDeletionOrUpdateAdapter<CollectionGameCrossRef>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `collection_games` SET `gameKey` = ?,`collectionId` = ? WHERE `gameKey` = ? AND `collectionId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CollectionGameCrossRef entity) {
        statement.bindString(1, entity.getGameKey());
        statement.bindString(2, entity.getCollectionId());
        statement.bindString(3, entity.getGameKey());
        statement.bindString(4, entity.getCollectionId());
      }
    });
    this.__upsertionAdapterOfAssetsCacheEntity = new EntityUpsertionAdapter<AssetsCacheEntity>(new EntityInsertionAdapter<AssetsCacheEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `assets_cache` (`key`,`json`,`cachedAt`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AssetsCacheEntity entity) {
        statement.bindString(1, entity.getKey());
        statement.bindString(2, entity.getJson());
        statement.bindLong(3, entity.getCachedAt());
      }
    }, new EntityDeletionOrUpdateAdapter<AssetsCacheEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `assets_cache` SET `key` = ?,`json` = ?,`cachedAt` = ? WHERE `key` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AssetsCacheEntity entity) {
        statement.bindString(1, entity.getKey());
        statement.bindString(2, entity.getJson());
        statement.bindLong(3, entity.getCachedAt());
        statement.bindString(4, entity.getKey());
      }
    });
    this.__upsertionAdapterOfDownloadEntity = new EntityUpsertionAdapter<DownloadEntity>(new EntityInsertionAdapter<DownloadEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `download_queue` (`id`,`objectId`,`shop`,`title`,`fileSize`,`sourceName`,`uri`,`downloader`,`status`,`progress`,`kind`,`magnet`,`torrentFilePath`,`saveDir`,`fileName`,`totalBytes`,`doneBytes`,`uploadBytes`,`downSpeed`,`upSpeed`,`etaSec`,`peers`,`seeds`,`infoHash`,`error`,`queuedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DownloadEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getObjectId());
        statement.bindString(3, entity.getShop());
        statement.bindString(4, entity.getTitle());
        if (entity.getFileSize() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getFileSize());
        }
        statement.bindString(6, entity.getSourceName());
        statement.bindString(7, entity.getUri());
        statement.bindString(8, entity.getDownloader());
        statement.bindString(9, entity.getStatus());
        statement.bindDouble(10, entity.getProgress());
        statement.bindString(11, entity.getKind());
        if (entity.getMagnet() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getMagnet());
        }
        if (entity.getTorrentFilePath() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getTorrentFilePath());
        }
        if (entity.getSaveDir() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getSaveDir());
        }
        if (entity.getFileName() == null) {
          statement.bindNull(15);
        } else {
          statement.bindString(15, entity.getFileName());
        }
        statement.bindLong(16, entity.getTotalBytes());
        statement.bindLong(17, entity.getDoneBytes());
        statement.bindLong(18, entity.getUploadBytes());
        statement.bindLong(19, entity.getDownSpeed());
        statement.bindLong(20, entity.getUpSpeed());
        statement.bindLong(21, entity.getEtaSec());
        statement.bindLong(22, entity.getPeers());
        statement.bindLong(23, entity.getSeeds());
        if (entity.getInfoHash() == null) {
          statement.bindNull(24);
        } else {
          statement.bindString(24, entity.getInfoHash());
        }
        if (entity.getError() == null) {
          statement.bindNull(25);
        } else {
          statement.bindString(25, entity.getError());
        }
        statement.bindLong(26, entity.getQueuedAt());
      }
    }, new EntityDeletionOrUpdateAdapter<DownloadEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `download_queue` SET `id` = ?,`objectId` = ?,`shop` = ?,`title` = ?,`fileSize` = ?,`sourceName` = ?,`uri` = ?,`downloader` = ?,`status` = ?,`progress` = ?,`kind` = ?,`magnet` = ?,`torrentFilePath` = ?,`saveDir` = ?,`fileName` = ?,`totalBytes` = ?,`doneBytes` = ?,`uploadBytes` = ?,`downSpeed` = ?,`upSpeed` = ?,`etaSec` = ?,`peers` = ?,`seeds` = ?,`infoHash` = ?,`error` = ?,`queuedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DownloadEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getObjectId());
        statement.bindString(3, entity.getShop());
        statement.bindString(4, entity.getTitle());
        if (entity.getFileSize() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getFileSize());
        }
        statement.bindString(6, entity.getSourceName());
        statement.bindString(7, entity.getUri());
        statement.bindString(8, entity.getDownloader());
        statement.bindString(9, entity.getStatus());
        statement.bindDouble(10, entity.getProgress());
        statement.bindString(11, entity.getKind());
        if (entity.getMagnet() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getMagnet());
        }
        if (entity.getTorrentFilePath() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getTorrentFilePath());
        }
        if (entity.getSaveDir() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getSaveDir());
        }
        if (entity.getFileName() == null) {
          statement.bindNull(15);
        } else {
          statement.bindString(15, entity.getFileName());
        }
        statement.bindLong(16, entity.getTotalBytes());
        statement.bindLong(17, entity.getDoneBytes());
        statement.bindLong(18, entity.getUploadBytes());
        statement.bindLong(19, entity.getDownSpeed());
        statement.bindLong(20, entity.getUpSpeed());
        statement.bindLong(21, entity.getEtaSec());
        statement.bindLong(22, entity.getPeers());
        statement.bindLong(23, entity.getSeeds());
        if (entity.getInfoHash() == null) {
          statement.bindNull(24);
        } else {
          statement.bindString(24, entity.getInfoHash());
        }
        if (entity.getError() == null) {
          statement.bindNull(25);
        } else {
          statement.bindString(25, entity.getError());
        }
        statement.bindLong(26, entity.getQueuedAt());
        statement.bindString(27, entity.getId());
      }
    });
    this.__upsertionAdapterOfDownloadSourceEntity = new EntityUpsertionAdapter<DownloadSourceEntity>(new EntityInsertionAdapter<DownloadSourceEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `download_sources` (`id`,`name`,`url`,`fingerprint`,`enabled`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DownloadSourceEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getUrl());
        if (entity.getFingerprint() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getFingerprint());
        }
        final int _tmp = entity.getEnabled() ? 1 : 0;
        statement.bindLong(5, _tmp);
      }
    }, new EntityDeletionOrUpdateAdapter<DownloadSourceEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `download_sources` SET `id` = ?,`name` = ?,`url` = ?,`fingerprint` = ?,`enabled` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DownloadSourceEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getUrl());
        if (entity.getFingerprint() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getFingerprint());
        }
        final int _tmp = entity.getEnabled() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindString(6, entity.getId());
      }
    });
  }

  @Override
  public Object flipFavorite(final String key, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfFlipFavorite.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, key);
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
          __preparedStmtOfFlipFavorite.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final String key, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDelete.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, key);
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
          __preparedStmtOfDelete.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteCollection(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteCollection.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfDeleteCollection.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearCollection(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearCollection.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfClearCollection.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object removeGameFromAllCollections(final String gameKey,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfRemoveGameFromAllCollections.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, gameKey);
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
          __preparedStmtOfRemoveGameFromAllCollections.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object removeFromCollection(final String gameKey, final String collectionId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfRemoveFromCollection.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, gameKey);
        _argIndex = 2;
        _stmt.bindString(_argIndex, collectionId);
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
          __preparedStmtOfRemoveFromCollection.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object dequeue(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDequeue.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfDequeue.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateProgress(final String id, final String status, final float progress,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateProgress.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        _stmt.bindDouble(_argIndex, progress);
        _argIndex = 3;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfUpdateProgress.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteSource(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteSource.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfDeleteSource.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object upsert(final LibraryGameEntity game, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfLibraryGameEntity.upsert(game);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertCollection(final CollectionEntity c,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfCollectionEntity.upsert(c);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object addToCollection(final CollectionGameCrossRef ref,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfCollectionGameCrossRef.upsert(ref);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object putAssets(final AssetsCacheEntity e, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfAssetsCacheEntity.upsert(e);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object enqueue(final DownloadEntity e, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfDownloadEntity.upsert(e);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertSource(final DownloadSourceEntity s,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfDownloadSourceEntity.upsert(s);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAll(final Continuation<? super List<LibraryGameEntity>> $completion) {
    final String _sql = "SELECT * FROM library_games ORDER BY pinned DESC, lastPlayed DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LibraryGameEntity>>() {
      @Override
      @NonNull
      public List<LibraryGameEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfKey = CursorUtil.getColumnIndexOrThrow(_cursor, "key");
          final int _cursorIndexOfObjectId = CursorUtil.getColumnIndexOrThrow(_cursor, "objectId");
          final int _cursorIndexOfShop = CursorUtil.getColumnIndexOrThrow(_cursor, "shop");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfCoverUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "coverUrl");
          final int _cursorIndexOfIconUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "iconUrl");
          final int _cursorIndexOfHeroUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "heroUrl");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfPlayTimeMs = CursorUtil.getColumnIndexOrThrow(_cursor, "playTimeMs");
          final int _cursorIndexOfLastPlayed = CursorUtil.getColumnIndexOrThrow(_cursor, "lastPlayed");
          final int _cursorIndexOfFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "favorite");
          final int _cursorIndexOfPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "pinned");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final int _cursorIndexOfLocalPath = CursorUtil.getColumnIndexOrThrow(_cursor, "localPath");
          final List<LibraryGameEntity> _result = new ArrayList<LibraryGameEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LibraryGameEntity _item;
            final String _tmpKey;
            _tmpKey = _cursor.getString(_cursorIndexOfKey);
            final String _tmpObjectId;
            _tmpObjectId = _cursor.getString(_cursorIndexOfObjectId);
            final String _tmpShop;
            _tmpShop = _cursor.getString(_cursorIndexOfShop);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpCoverUrl;
            if (_cursor.isNull(_cursorIndexOfCoverUrl)) {
              _tmpCoverUrl = null;
            } else {
              _tmpCoverUrl = _cursor.getString(_cursorIndexOfCoverUrl);
            }
            final String _tmpIconUrl;
            if (_cursor.isNull(_cursorIndexOfIconUrl)) {
              _tmpIconUrl = null;
            } else {
              _tmpIconUrl = _cursor.getString(_cursorIndexOfIconUrl);
            }
            final String _tmpHeroUrl;
            if (_cursor.isNull(_cursorIndexOfHeroUrl)) {
              _tmpHeroUrl = null;
            } else {
              _tmpHeroUrl = _cursor.getString(_cursorIndexOfHeroUrl);
            }
            final String _tmpLogoUrl;
            if (_cursor.isNull(_cursorIndexOfLogoUrl)) {
              _tmpLogoUrl = null;
            } else {
              _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
            }
            final long _tmpPlayTimeMs;
            _tmpPlayTimeMs = _cursor.getLong(_cursorIndexOfPlayTimeMs);
            final Long _tmpLastPlayed;
            if (_cursor.isNull(_cursorIndexOfLastPlayed)) {
              _tmpLastPlayed = null;
            } else {
              _tmpLastPlayed = _cursor.getLong(_cursorIndexOfLastPlayed);
            }
            final boolean _tmpFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfFavorite);
            _tmpFavorite = _tmp != 0;
            final boolean _tmpPinned;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfPinned);
            _tmpPinned = _tmp_1 != 0;
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            final String _tmpLocalPath;
            if (_cursor.isNull(_cursorIndexOfLocalPath)) {
              _tmpLocalPath = null;
            } else {
              _tmpLocalPath = _cursor.getString(_cursorIndexOfLocalPath);
            }
            _item = new LibraryGameEntity(_tmpKey,_tmpObjectId,_tmpShop,_tmpTitle,_tmpCoverUrl,_tmpIconUrl,_tmpHeroUrl,_tmpLogoUrl,_tmpPlayTimeMs,_tmpLastPlayed,_tmpFavorite,_tmpPinned,_tmpAddedAt,_tmpLocalPath);
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

  @Override
  public Object getCollectionCounts(final Continuation<? super List<CollectionCount>> $completion) {
    final String _sql = "SELECT collectionId AS id, COUNT(*) AS c FROM collection_games GROUP BY collectionId";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CollectionCount>>() {
      @Override
      @NonNull
      public List<CollectionCount> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = 0;
          final int _cursorIndexOfC = 1;
          final List<CollectionCount> _result = new ArrayList<CollectionCount>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CollectionCount _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final int _tmpC;
            _tmpC = _cursor.getInt(_cursorIndexOfC);
            _item = new CollectionCount(_tmpId,_tmpC);
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

  @Override
  public Object getFavorites(final Continuation<? super List<LibraryGameEntity>> $completion) {
    final String _sql = "SELECT * FROM library_games WHERE favorite = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LibraryGameEntity>>() {
      @Override
      @NonNull
      public List<LibraryGameEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfKey = CursorUtil.getColumnIndexOrThrow(_cursor, "key");
          final int _cursorIndexOfObjectId = CursorUtil.getColumnIndexOrThrow(_cursor, "objectId");
          final int _cursorIndexOfShop = CursorUtil.getColumnIndexOrThrow(_cursor, "shop");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfCoverUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "coverUrl");
          final int _cursorIndexOfIconUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "iconUrl");
          final int _cursorIndexOfHeroUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "heroUrl");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfPlayTimeMs = CursorUtil.getColumnIndexOrThrow(_cursor, "playTimeMs");
          final int _cursorIndexOfLastPlayed = CursorUtil.getColumnIndexOrThrow(_cursor, "lastPlayed");
          final int _cursorIndexOfFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "favorite");
          final int _cursorIndexOfPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "pinned");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final int _cursorIndexOfLocalPath = CursorUtil.getColumnIndexOrThrow(_cursor, "localPath");
          final List<LibraryGameEntity> _result = new ArrayList<LibraryGameEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LibraryGameEntity _item;
            final String _tmpKey;
            _tmpKey = _cursor.getString(_cursorIndexOfKey);
            final String _tmpObjectId;
            _tmpObjectId = _cursor.getString(_cursorIndexOfObjectId);
            final String _tmpShop;
            _tmpShop = _cursor.getString(_cursorIndexOfShop);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpCoverUrl;
            if (_cursor.isNull(_cursorIndexOfCoverUrl)) {
              _tmpCoverUrl = null;
            } else {
              _tmpCoverUrl = _cursor.getString(_cursorIndexOfCoverUrl);
            }
            final String _tmpIconUrl;
            if (_cursor.isNull(_cursorIndexOfIconUrl)) {
              _tmpIconUrl = null;
            } else {
              _tmpIconUrl = _cursor.getString(_cursorIndexOfIconUrl);
            }
            final String _tmpHeroUrl;
            if (_cursor.isNull(_cursorIndexOfHeroUrl)) {
              _tmpHeroUrl = null;
            } else {
              _tmpHeroUrl = _cursor.getString(_cursorIndexOfHeroUrl);
            }
            final String _tmpLogoUrl;
            if (_cursor.isNull(_cursorIndexOfLogoUrl)) {
              _tmpLogoUrl = null;
            } else {
              _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
            }
            final long _tmpPlayTimeMs;
            _tmpPlayTimeMs = _cursor.getLong(_cursorIndexOfPlayTimeMs);
            final Long _tmpLastPlayed;
            if (_cursor.isNull(_cursorIndexOfLastPlayed)) {
              _tmpLastPlayed = null;
            } else {
              _tmpLastPlayed = _cursor.getLong(_cursorIndexOfLastPlayed);
            }
            final boolean _tmpFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfFavorite);
            _tmpFavorite = _tmp != 0;
            final boolean _tmpPinned;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfPinned);
            _tmpPinned = _tmp_1 != 0;
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            final String _tmpLocalPath;
            if (_cursor.isNull(_cursorIndexOfLocalPath)) {
              _tmpLocalPath = null;
            } else {
              _tmpLocalPath = _cursor.getString(_cursorIndexOfLocalPath);
            }
            _item = new LibraryGameEntity(_tmpKey,_tmpObjectId,_tmpShop,_tmpTitle,_tmpCoverUrl,_tmpIconUrl,_tmpHeroUrl,_tmpLogoUrl,_tmpPlayTimeMs,_tmpLastPlayed,_tmpFavorite,_tmpPinned,_tmpAddedAt,_tmpLocalPath);
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

  @Override
  public Object exists(final String key, final Continuation<? super Boolean> $completion) {
    final String _sql = "SELECT EXISTS(SELECT 1 FROM library_games WHERE `key` = ?)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, key);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Boolean>() {
      @Override
      @NonNull
      public Boolean call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Boolean _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp != 0;
          } else {
            _result = false;
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
  public Object getCollections(final Continuation<? super List<CollectionEntity>> $completion) {
    final String _sql = "SELECT * FROM collections";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CollectionEntity>>() {
      @Override
      @NonNull
      public List<CollectionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final List<CollectionEntity> _result = new ArrayList<CollectionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CollectionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            _item = new CollectionEntity(_tmpId,_tmpName);
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

  @Override
  public Object getGameCollections(final String gameKey,
      final Continuation<? super List<String>> $completion) {
    final String _sql = "SELECT collectionId FROM collection_games WHERE gameKey = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, gameKey);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
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

  @Override
  public Object getCollectionGames(final String id,
      final Continuation<? super List<LibraryGameEntity>> $completion) {
    final String _sql = "SELECT * FROM library_games WHERE `key` IN (SELECT gameKey FROM collection_games WHERE collectionId = ?)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LibraryGameEntity>>() {
      @Override
      @NonNull
      public List<LibraryGameEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfKey = CursorUtil.getColumnIndexOrThrow(_cursor, "key");
          final int _cursorIndexOfObjectId = CursorUtil.getColumnIndexOrThrow(_cursor, "objectId");
          final int _cursorIndexOfShop = CursorUtil.getColumnIndexOrThrow(_cursor, "shop");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfCoverUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "coverUrl");
          final int _cursorIndexOfIconUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "iconUrl");
          final int _cursorIndexOfHeroUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "heroUrl");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfPlayTimeMs = CursorUtil.getColumnIndexOrThrow(_cursor, "playTimeMs");
          final int _cursorIndexOfLastPlayed = CursorUtil.getColumnIndexOrThrow(_cursor, "lastPlayed");
          final int _cursorIndexOfFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "favorite");
          final int _cursorIndexOfPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "pinned");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final int _cursorIndexOfLocalPath = CursorUtil.getColumnIndexOrThrow(_cursor, "localPath");
          final List<LibraryGameEntity> _result = new ArrayList<LibraryGameEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LibraryGameEntity _item;
            final String _tmpKey;
            _tmpKey = _cursor.getString(_cursorIndexOfKey);
            final String _tmpObjectId;
            _tmpObjectId = _cursor.getString(_cursorIndexOfObjectId);
            final String _tmpShop;
            _tmpShop = _cursor.getString(_cursorIndexOfShop);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpCoverUrl;
            if (_cursor.isNull(_cursorIndexOfCoverUrl)) {
              _tmpCoverUrl = null;
            } else {
              _tmpCoverUrl = _cursor.getString(_cursorIndexOfCoverUrl);
            }
            final String _tmpIconUrl;
            if (_cursor.isNull(_cursorIndexOfIconUrl)) {
              _tmpIconUrl = null;
            } else {
              _tmpIconUrl = _cursor.getString(_cursorIndexOfIconUrl);
            }
            final String _tmpHeroUrl;
            if (_cursor.isNull(_cursorIndexOfHeroUrl)) {
              _tmpHeroUrl = null;
            } else {
              _tmpHeroUrl = _cursor.getString(_cursorIndexOfHeroUrl);
            }
            final String _tmpLogoUrl;
            if (_cursor.isNull(_cursorIndexOfLogoUrl)) {
              _tmpLogoUrl = null;
            } else {
              _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
            }
            final long _tmpPlayTimeMs;
            _tmpPlayTimeMs = _cursor.getLong(_cursorIndexOfPlayTimeMs);
            final Long _tmpLastPlayed;
            if (_cursor.isNull(_cursorIndexOfLastPlayed)) {
              _tmpLastPlayed = null;
            } else {
              _tmpLastPlayed = _cursor.getLong(_cursorIndexOfLastPlayed);
            }
            final boolean _tmpFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfFavorite);
            _tmpFavorite = _tmp != 0;
            final boolean _tmpPinned;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfPinned);
            _tmpPinned = _tmp_1 != 0;
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            final String _tmpLocalPath;
            if (_cursor.isNull(_cursorIndexOfLocalPath)) {
              _tmpLocalPath = null;
            } else {
              _tmpLocalPath = _cursor.getString(_cursorIndexOfLocalPath);
            }
            _item = new LibraryGameEntity(_tmpKey,_tmpObjectId,_tmpShop,_tmpTitle,_tmpCoverUrl,_tmpIconUrl,_tmpHeroUrl,_tmpLogoUrl,_tmpPlayTimeMs,_tmpLastPlayed,_tmpFavorite,_tmpPinned,_tmpAddedAt,_tmpLocalPath);
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

  @Override
  public Object getAssets(final String key,
      final Continuation<? super AssetsCacheEntity> $completion) {
    final String _sql = "SELECT * FROM assets_cache WHERE `key` = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, key);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<AssetsCacheEntity>() {
      @Override
      @Nullable
      public AssetsCacheEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfKey = CursorUtil.getColumnIndexOrThrow(_cursor, "key");
          final int _cursorIndexOfJson = CursorUtil.getColumnIndexOrThrow(_cursor, "json");
          final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
          final AssetsCacheEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpKey;
            _tmpKey = _cursor.getString(_cursorIndexOfKey);
            final String _tmpJson;
            _tmpJson = _cursor.getString(_cursorIndexOfJson);
            final long _tmpCachedAt;
            _tmpCachedAt = _cursor.getLong(_cursorIndexOfCachedAt);
            _result = new AssetsCacheEntity(_tmpKey,_tmpJson,_tmpCachedAt);
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
  public Object getQueue(final Continuation<? super List<DownloadEntity>> $completion) {
    final String _sql = "SELECT * FROM download_queue ORDER BY queuedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DownloadEntity>>() {
      @Override
      @NonNull
      public List<DownloadEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfObjectId = CursorUtil.getColumnIndexOrThrow(_cursor, "objectId");
          final int _cursorIndexOfShop = CursorUtil.getColumnIndexOrThrow(_cursor, "shop");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
          final int _cursorIndexOfSourceName = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceName");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfDownloader = CursorUtil.getColumnIndexOrThrow(_cursor, "downloader");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "progress");
          final int _cursorIndexOfKind = CursorUtil.getColumnIndexOrThrow(_cursor, "kind");
          final int _cursorIndexOfMagnet = CursorUtil.getColumnIndexOrThrow(_cursor, "magnet");
          final int _cursorIndexOfTorrentFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "torrentFilePath");
          final int _cursorIndexOfSaveDir = CursorUtil.getColumnIndexOrThrow(_cursor, "saveDir");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfTotalBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "totalBytes");
          final int _cursorIndexOfDoneBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "doneBytes");
          final int _cursorIndexOfUploadBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "uploadBytes");
          final int _cursorIndexOfDownSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "downSpeed");
          final int _cursorIndexOfUpSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "upSpeed");
          final int _cursorIndexOfEtaSec = CursorUtil.getColumnIndexOrThrow(_cursor, "etaSec");
          final int _cursorIndexOfPeers = CursorUtil.getColumnIndexOrThrow(_cursor, "peers");
          final int _cursorIndexOfSeeds = CursorUtil.getColumnIndexOrThrow(_cursor, "seeds");
          final int _cursorIndexOfInfoHash = CursorUtil.getColumnIndexOrThrow(_cursor, "infoHash");
          final int _cursorIndexOfError = CursorUtil.getColumnIndexOrThrow(_cursor, "error");
          final int _cursorIndexOfQueuedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "queuedAt");
          final List<DownloadEntity> _result = new ArrayList<DownloadEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DownloadEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpObjectId;
            _tmpObjectId = _cursor.getString(_cursorIndexOfObjectId);
            final String _tmpShop;
            _tmpShop = _cursor.getString(_cursorIndexOfShop);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpFileSize;
            if (_cursor.isNull(_cursorIndexOfFileSize)) {
              _tmpFileSize = null;
            } else {
              _tmpFileSize = _cursor.getString(_cursorIndexOfFileSize);
            }
            final String _tmpSourceName;
            _tmpSourceName = _cursor.getString(_cursorIndexOfSourceName);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpDownloader;
            _tmpDownloader = _cursor.getString(_cursorIndexOfDownloader);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final float _tmpProgress;
            _tmpProgress = _cursor.getFloat(_cursorIndexOfProgress);
            final String _tmpKind;
            _tmpKind = _cursor.getString(_cursorIndexOfKind);
            final String _tmpMagnet;
            if (_cursor.isNull(_cursorIndexOfMagnet)) {
              _tmpMagnet = null;
            } else {
              _tmpMagnet = _cursor.getString(_cursorIndexOfMagnet);
            }
            final String _tmpTorrentFilePath;
            if (_cursor.isNull(_cursorIndexOfTorrentFilePath)) {
              _tmpTorrentFilePath = null;
            } else {
              _tmpTorrentFilePath = _cursor.getString(_cursorIndexOfTorrentFilePath);
            }
            final String _tmpSaveDir;
            if (_cursor.isNull(_cursorIndexOfSaveDir)) {
              _tmpSaveDir = null;
            } else {
              _tmpSaveDir = _cursor.getString(_cursorIndexOfSaveDir);
            }
            final String _tmpFileName;
            if (_cursor.isNull(_cursorIndexOfFileName)) {
              _tmpFileName = null;
            } else {
              _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            }
            final long _tmpTotalBytes;
            _tmpTotalBytes = _cursor.getLong(_cursorIndexOfTotalBytes);
            final long _tmpDoneBytes;
            _tmpDoneBytes = _cursor.getLong(_cursorIndexOfDoneBytes);
            final long _tmpUploadBytes;
            _tmpUploadBytes = _cursor.getLong(_cursorIndexOfUploadBytes);
            final long _tmpDownSpeed;
            _tmpDownSpeed = _cursor.getLong(_cursorIndexOfDownSpeed);
            final long _tmpUpSpeed;
            _tmpUpSpeed = _cursor.getLong(_cursorIndexOfUpSpeed);
            final long _tmpEtaSec;
            _tmpEtaSec = _cursor.getLong(_cursorIndexOfEtaSec);
            final int _tmpPeers;
            _tmpPeers = _cursor.getInt(_cursorIndexOfPeers);
            final int _tmpSeeds;
            _tmpSeeds = _cursor.getInt(_cursorIndexOfSeeds);
            final String _tmpInfoHash;
            if (_cursor.isNull(_cursorIndexOfInfoHash)) {
              _tmpInfoHash = null;
            } else {
              _tmpInfoHash = _cursor.getString(_cursorIndexOfInfoHash);
            }
            final String _tmpError;
            if (_cursor.isNull(_cursorIndexOfError)) {
              _tmpError = null;
            } else {
              _tmpError = _cursor.getString(_cursorIndexOfError);
            }
            final long _tmpQueuedAt;
            _tmpQueuedAt = _cursor.getLong(_cursorIndexOfQueuedAt);
            _item = new DownloadEntity(_tmpId,_tmpObjectId,_tmpShop,_tmpTitle,_tmpFileSize,_tmpSourceName,_tmpUri,_tmpDownloader,_tmpStatus,_tmpProgress,_tmpKind,_tmpMagnet,_tmpTorrentFilePath,_tmpSaveDir,_tmpFileName,_tmpTotalBytes,_tmpDoneBytes,_tmpUploadBytes,_tmpDownSpeed,_tmpUpSpeed,_tmpEtaSec,_tmpPeers,_tmpSeeds,_tmpInfoHash,_tmpError,_tmpQueuedAt);
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

  @Override
  public Flow<List<DownloadEntity>> queueFlow() {
    final String _sql = "SELECT * FROM download_queue ORDER BY queuedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"download_queue"}, new Callable<List<DownloadEntity>>() {
      @Override
      @NonNull
      public List<DownloadEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfObjectId = CursorUtil.getColumnIndexOrThrow(_cursor, "objectId");
          final int _cursorIndexOfShop = CursorUtil.getColumnIndexOrThrow(_cursor, "shop");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
          final int _cursorIndexOfSourceName = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceName");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfDownloader = CursorUtil.getColumnIndexOrThrow(_cursor, "downloader");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "progress");
          final int _cursorIndexOfKind = CursorUtil.getColumnIndexOrThrow(_cursor, "kind");
          final int _cursorIndexOfMagnet = CursorUtil.getColumnIndexOrThrow(_cursor, "magnet");
          final int _cursorIndexOfTorrentFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "torrentFilePath");
          final int _cursorIndexOfSaveDir = CursorUtil.getColumnIndexOrThrow(_cursor, "saveDir");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfTotalBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "totalBytes");
          final int _cursorIndexOfDoneBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "doneBytes");
          final int _cursorIndexOfUploadBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "uploadBytes");
          final int _cursorIndexOfDownSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "downSpeed");
          final int _cursorIndexOfUpSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "upSpeed");
          final int _cursorIndexOfEtaSec = CursorUtil.getColumnIndexOrThrow(_cursor, "etaSec");
          final int _cursorIndexOfPeers = CursorUtil.getColumnIndexOrThrow(_cursor, "peers");
          final int _cursorIndexOfSeeds = CursorUtil.getColumnIndexOrThrow(_cursor, "seeds");
          final int _cursorIndexOfInfoHash = CursorUtil.getColumnIndexOrThrow(_cursor, "infoHash");
          final int _cursorIndexOfError = CursorUtil.getColumnIndexOrThrow(_cursor, "error");
          final int _cursorIndexOfQueuedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "queuedAt");
          final List<DownloadEntity> _result = new ArrayList<DownloadEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DownloadEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpObjectId;
            _tmpObjectId = _cursor.getString(_cursorIndexOfObjectId);
            final String _tmpShop;
            _tmpShop = _cursor.getString(_cursorIndexOfShop);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpFileSize;
            if (_cursor.isNull(_cursorIndexOfFileSize)) {
              _tmpFileSize = null;
            } else {
              _tmpFileSize = _cursor.getString(_cursorIndexOfFileSize);
            }
            final String _tmpSourceName;
            _tmpSourceName = _cursor.getString(_cursorIndexOfSourceName);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpDownloader;
            _tmpDownloader = _cursor.getString(_cursorIndexOfDownloader);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final float _tmpProgress;
            _tmpProgress = _cursor.getFloat(_cursorIndexOfProgress);
            final String _tmpKind;
            _tmpKind = _cursor.getString(_cursorIndexOfKind);
            final String _tmpMagnet;
            if (_cursor.isNull(_cursorIndexOfMagnet)) {
              _tmpMagnet = null;
            } else {
              _tmpMagnet = _cursor.getString(_cursorIndexOfMagnet);
            }
            final String _tmpTorrentFilePath;
            if (_cursor.isNull(_cursorIndexOfTorrentFilePath)) {
              _tmpTorrentFilePath = null;
            } else {
              _tmpTorrentFilePath = _cursor.getString(_cursorIndexOfTorrentFilePath);
            }
            final String _tmpSaveDir;
            if (_cursor.isNull(_cursorIndexOfSaveDir)) {
              _tmpSaveDir = null;
            } else {
              _tmpSaveDir = _cursor.getString(_cursorIndexOfSaveDir);
            }
            final String _tmpFileName;
            if (_cursor.isNull(_cursorIndexOfFileName)) {
              _tmpFileName = null;
            } else {
              _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            }
            final long _tmpTotalBytes;
            _tmpTotalBytes = _cursor.getLong(_cursorIndexOfTotalBytes);
            final long _tmpDoneBytes;
            _tmpDoneBytes = _cursor.getLong(_cursorIndexOfDoneBytes);
            final long _tmpUploadBytes;
            _tmpUploadBytes = _cursor.getLong(_cursorIndexOfUploadBytes);
            final long _tmpDownSpeed;
            _tmpDownSpeed = _cursor.getLong(_cursorIndexOfDownSpeed);
            final long _tmpUpSpeed;
            _tmpUpSpeed = _cursor.getLong(_cursorIndexOfUpSpeed);
            final long _tmpEtaSec;
            _tmpEtaSec = _cursor.getLong(_cursorIndexOfEtaSec);
            final int _tmpPeers;
            _tmpPeers = _cursor.getInt(_cursorIndexOfPeers);
            final int _tmpSeeds;
            _tmpSeeds = _cursor.getInt(_cursorIndexOfSeeds);
            final String _tmpInfoHash;
            if (_cursor.isNull(_cursorIndexOfInfoHash)) {
              _tmpInfoHash = null;
            } else {
              _tmpInfoHash = _cursor.getString(_cursorIndexOfInfoHash);
            }
            final String _tmpError;
            if (_cursor.isNull(_cursorIndexOfError)) {
              _tmpError = null;
            } else {
              _tmpError = _cursor.getString(_cursorIndexOfError);
            }
            final long _tmpQueuedAt;
            _tmpQueuedAt = _cursor.getLong(_cursorIndexOfQueuedAt);
            _item = new DownloadEntity(_tmpId,_tmpObjectId,_tmpShop,_tmpTitle,_tmpFileSize,_tmpSourceName,_tmpUri,_tmpDownloader,_tmpStatus,_tmpProgress,_tmpKind,_tmpMagnet,_tmpTorrentFilePath,_tmpSaveDir,_tmpFileName,_tmpTotalBytes,_tmpDoneBytes,_tmpUploadBytes,_tmpDownSpeed,_tmpUpSpeed,_tmpEtaSec,_tmpPeers,_tmpSeeds,_tmpInfoHash,_tmpError,_tmpQueuedAt);
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
  public Object getDownload(final String id,
      final Continuation<? super DownloadEntity> $completion) {
    final String _sql = "SELECT * FROM download_queue WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DownloadEntity>() {
      @Override
      @Nullable
      public DownloadEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfObjectId = CursorUtil.getColumnIndexOrThrow(_cursor, "objectId");
          final int _cursorIndexOfShop = CursorUtil.getColumnIndexOrThrow(_cursor, "shop");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
          final int _cursorIndexOfSourceName = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceName");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfDownloader = CursorUtil.getColumnIndexOrThrow(_cursor, "downloader");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "progress");
          final int _cursorIndexOfKind = CursorUtil.getColumnIndexOrThrow(_cursor, "kind");
          final int _cursorIndexOfMagnet = CursorUtil.getColumnIndexOrThrow(_cursor, "magnet");
          final int _cursorIndexOfTorrentFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "torrentFilePath");
          final int _cursorIndexOfSaveDir = CursorUtil.getColumnIndexOrThrow(_cursor, "saveDir");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfTotalBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "totalBytes");
          final int _cursorIndexOfDoneBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "doneBytes");
          final int _cursorIndexOfUploadBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "uploadBytes");
          final int _cursorIndexOfDownSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "downSpeed");
          final int _cursorIndexOfUpSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "upSpeed");
          final int _cursorIndexOfEtaSec = CursorUtil.getColumnIndexOrThrow(_cursor, "etaSec");
          final int _cursorIndexOfPeers = CursorUtil.getColumnIndexOrThrow(_cursor, "peers");
          final int _cursorIndexOfSeeds = CursorUtil.getColumnIndexOrThrow(_cursor, "seeds");
          final int _cursorIndexOfInfoHash = CursorUtil.getColumnIndexOrThrow(_cursor, "infoHash");
          final int _cursorIndexOfError = CursorUtil.getColumnIndexOrThrow(_cursor, "error");
          final int _cursorIndexOfQueuedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "queuedAt");
          final DownloadEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpObjectId;
            _tmpObjectId = _cursor.getString(_cursorIndexOfObjectId);
            final String _tmpShop;
            _tmpShop = _cursor.getString(_cursorIndexOfShop);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpFileSize;
            if (_cursor.isNull(_cursorIndexOfFileSize)) {
              _tmpFileSize = null;
            } else {
              _tmpFileSize = _cursor.getString(_cursorIndexOfFileSize);
            }
            final String _tmpSourceName;
            _tmpSourceName = _cursor.getString(_cursorIndexOfSourceName);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpDownloader;
            _tmpDownloader = _cursor.getString(_cursorIndexOfDownloader);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final float _tmpProgress;
            _tmpProgress = _cursor.getFloat(_cursorIndexOfProgress);
            final String _tmpKind;
            _tmpKind = _cursor.getString(_cursorIndexOfKind);
            final String _tmpMagnet;
            if (_cursor.isNull(_cursorIndexOfMagnet)) {
              _tmpMagnet = null;
            } else {
              _tmpMagnet = _cursor.getString(_cursorIndexOfMagnet);
            }
            final String _tmpTorrentFilePath;
            if (_cursor.isNull(_cursorIndexOfTorrentFilePath)) {
              _tmpTorrentFilePath = null;
            } else {
              _tmpTorrentFilePath = _cursor.getString(_cursorIndexOfTorrentFilePath);
            }
            final String _tmpSaveDir;
            if (_cursor.isNull(_cursorIndexOfSaveDir)) {
              _tmpSaveDir = null;
            } else {
              _tmpSaveDir = _cursor.getString(_cursorIndexOfSaveDir);
            }
            final String _tmpFileName;
            if (_cursor.isNull(_cursorIndexOfFileName)) {
              _tmpFileName = null;
            } else {
              _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            }
            final long _tmpTotalBytes;
            _tmpTotalBytes = _cursor.getLong(_cursorIndexOfTotalBytes);
            final long _tmpDoneBytes;
            _tmpDoneBytes = _cursor.getLong(_cursorIndexOfDoneBytes);
            final long _tmpUploadBytes;
            _tmpUploadBytes = _cursor.getLong(_cursorIndexOfUploadBytes);
            final long _tmpDownSpeed;
            _tmpDownSpeed = _cursor.getLong(_cursorIndexOfDownSpeed);
            final long _tmpUpSpeed;
            _tmpUpSpeed = _cursor.getLong(_cursorIndexOfUpSpeed);
            final long _tmpEtaSec;
            _tmpEtaSec = _cursor.getLong(_cursorIndexOfEtaSec);
            final int _tmpPeers;
            _tmpPeers = _cursor.getInt(_cursorIndexOfPeers);
            final int _tmpSeeds;
            _tmpSeeds = _cursor.getInt(_cursorIndexOfSeeds);
            final String _tmpInfoHash;
            if (_cursor.isNull(_cursorIndexOfInfoHash)) {
              _tmpInfoHash = null;
            } else {
              _tmpInfoHash = _cursor.getString(_cursorIndexOfInfoHash);
            }
            final String _tmpError;
            if (_cursor.isNull(_cursorIndexOfError)) {
              _tmpError = null;
            } else {
              _tmpError = _cursor.getString(_cursorIndexOfError);
            }
            final long _tmpQueuedAt;
            _tmpQueuedAt = _cursor.getLong(_cursorIndexOfQueuedAt);
            _result = new DownloadEntity(_tmpId,_tmpObjectId,_tmpShop,_tmpTitle,_tmpFileSize,_tmpSourceName,_tmpUri,_tmpDownloader,_tmpStatus,_tmpProgress,_tmpKind,_tmpMagnet,_tmpTorrentFilePath,_tmpSaveDir,_tmpFileName,_tmpTotalBytes,_tmpDoneBytes,_tmpUploadBytes,_tmpDownSpeed,_tmpUpSpeed,_tmpEtaSec,_tmpPeers,_tmpSeeds,_tmpInfoHash,_tmpError,_tmpQueuedAt);
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
  public Object getSources(final Continuation<? super List<DownloadSourceEntity>> $completion) {
    final String _sql = "SELECT * FROM download_sources ORDER BY name";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DownloadSourceEntity>>() {
      @Override
      @NonNull
      public List<DownloadSourceEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfFingerprint = CursorUtil.getColumnIndexOrThrow(_cursor, "fingerprint");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final List<DownloadSourceEntity> _result = new ArrayList<DownloadSourceEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DownloadSourceEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpFingerprint;
            if (_cursor.isNull(_cursorIndexOfFingerprint)) {
              _tmpFingerprint = null;
            } else {
              _tmpFingerprint = _cursor.getString(_cursorIndexOfFingerprint);
            }
            final boolean _tmpEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEnabled);
            _tmpEnabled = _tmp != 0;
            _item = new DownloadSourceEntity(_tmpId,_tmpName,_tmpUrl,_tmpFingerprint,_tmpEnabled);
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

  @Override
  public Object getGamesRaw(final SupportSQLiteQuery query,
      final Continuation<? super List<LibraryGameEntity>> $completion) {
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LibraryGameEntity>>() {
      @Override
      @NonNull
      public List<LibraryGameEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, query, false, null);
        try {
          final List<LibraryGameEntity> _result = new ArrayList<LibraryGameEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LibraryGameEntity _item;
            _item = __entityCursorConverter_comHydradroidDataLocalLibraryGameEntity(_cursor);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private LibraryGameEntity __entityCursorConverter_comHydradroidDataLocalLibraryGameEntity(
      @NonNull final Cursor cursor) {
    final LibraryGameEntity _entity;
    final int _cursorIndexOfKey = CursorUtil.getColumnIndex(cursor, "key");
    final int _cursorIndexOfObjectId = CursorUtil.getColumnIndex(cursor, "objectId");
    final int _cursorIndexOfShop = CursorUtil.getColumnIndex(cursor, "shop");
    final int _cursorIndexOfTitle = CursorUtil.getColumnIndex(cursor, "title");
    final int _cursorIndexOfCoverUrl = CursorUtil.getColumnIndex(cursor, "coverUrl");
    final int _cursorIndexOfIconUrl = CursorUtil.getColumnIndex(cursor, "iconUrl");
    final int _cursorIndexOfHeroUrl = CursorUtil.getColumnIndex(cursor, "heroUrl");
    final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndex(cursor, "logoUrl");
    final int _cursorIndexOfPlayTimeMs = CursorUtil.getColumnIndex(cursor, "playTimeMs");
    final int _cursorIndexOfLastPlayed = CursorUtil.getColumnIndex(cursor, "lastPlayed");
    final int _cursorIndexOfFavorite = CursorUtil.getColumnIndex(cursor, "favorite");
    final int _cursorIndexOfPinned = CursorUtil.getColumnIndex(cursor, "pinned");
    final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndex(cursor, "addedAt");
    final int _cursorIndexOfLocalPath = CursorUtil.getColumnIndex(cursor, "localPath");
    final String _tmpKey;
    if (_cursorIndexOfKey == -1) {
      _tmpKey = null;
    } else {
      _tmpKey = cursor.getString(_cursorIndexOfKey);
    }
    final String _tmpObjectId;
    if (_cursorIndexOfObjectId == -1) {
      _tmpObjectId = null;
    } else {
      _tmpObjectId = cursor.getString(_cursorIndexOfObjectId);
    }
    final String _tmpShop;
    if (_cursorIndexOfShop == -1) {
      _tmpShop = null;
    } else {
      _tmpShop = cursor.getString(_cursorIndexOfShop);
    }
    final String _tmpTitle;
    if (_cursorIndexOfTitle == -1) {
      _tmpTitle = null;
    } else {
      _tmpTitle = cursor.getString(_cursorIndexOfTitle);
    }
    final String _tmpCoverUrl;
    if (_cursorIndexOfCoverUrl == -1) {
      _tmpCoverUrl = null;
    } else {
      if (cursor.isNull(_cursorIndexOfCoverUrl)) {
        _tmpCoverUrl = null;
      } else {
        _tmpCoverUrl = cursor.getString(_cursorIndexOfCoverUrl);
      }
    }
    final String _tmpIconUrl;
    if (_cursorIndexOfIconUrl == -1) {
      _tmpIconUrl = null;
    } else {
      if (cursor.isNull(_cursorIndexOfIconUrl)) {
        _tmpIconUrl = null;
      } else {
        _tmpIconUrl = cursor.getString(_cursorIndexOfIconUrl);
      }
    }
    final String _tmpHeroUrl;
    if (_cursorIndexOfHeroUrl == -1) {
      _tmpHeroUrl = null;
    } else {
      if (cursor.isNull(_cursorIndexOfHeroUrl)) {
        _tmpHeroUrl = null;
      } else {
        _tmpHeroUrl = cursor.getString(_cursorIndexOfHeroUrl);
      }
    }
    final String _tmpLogoUrl;
    if (_cursorIndexOfLogoUrl == -1) {
      _tmpLogoUrl = null;
    } else {
      if (cursor.isNull(_cursorIndexOfLogoUrl)) {
        _tmpLogoUrl = null;
      } else {
        _tmpLogoUrl = cursor.getString(_cursorIndexOfLogoUrl);
      }
    }
    final long _tmpPlayTimeMs;
    if (_cursorIndexOfPlayTimeMs == -1) {
      _tmpPlayTimeMs = 0;
    } else {
      _tmpPlayTimeMs = cursor.getLong(_cursorIndexOfPlayTimeMs);
    }
    final Long _tmpLastPlayed;
    if (_cursorIndexOfLastPlayed == -1) {
      _tmpLastPlayed = null;
    } else {
      if (cursor.isNull(_cursorIndexOfLastPlayed)) {
        _tmpLastPlayed = null;
      } else {
        _tmpLastPlayed = cursor.getLong(_cursorIndexOfLastPlayed);
      }
    }
    final boolean _tmpFavorite;
    if (_cursorIndexOfFavorite == -1) {
      _tmpFavorite = false;
    } else {
      final int _tmp;
      _tmp = cursor.getInt(_cursorIndexOfFavorite);
      _tmpFavorite = _tmp != 0;
    }
    final boolean _tmpPinned;
    if (_cursorIndexOfPinned == -1) {
      _tmpPinned = false;
    } else {
      final int _tmp_1;
      _tmp_1 = cursor.getInt(_cursorIndexOfPinned);
      _tmpPinned = _tmp_1 != 0;
    }
    final long _tmpAddedAt;
    if (_cursorIndexOfAddedAt == -1) {
      _tmpAddedAt = 0;
    } else {
      _tmpAddedAt = cursor.getLong(_cursorIndexOfAddedAt);
    }
    final String _tmpLocalPath;
    if (_cursorIndexOfLocalPath == -1) {
      _tmpLocalPath = null;
    } else {
      if (cursor.isNull(_cursorIndexOfLocalPath)) {
        _tmpLocalPath = null;
      } else {
        _tmpLocalPath = cursor.getString(_cursorIndexOfLocalPath);
      }
    }
    _entity = new LibraryGameEntity(_tmpKey,_tmpObjectId,_tmpShop,_tmpTitle,_tmpCoverUrl,_tmpIconUrl,_tmpHeroUrl,_tmpLogoUrl,_tmpPlayTimeMs,_tmpLastPlayed,_tmpFavorite,_tmpPinned,_tmpAddedAt,_tmpLocalPath);
    return _entity;
  }
}
