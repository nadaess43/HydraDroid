package com.hydradroid.data.model

import com.squareup.moshi.JsonClass

// ── Порт src/types + src/shared: точные поля оригинала ──
enum class GameShop { steam, custom, launchbox }

@JsonClass(generateAdapter = true)
data class ShopAssets(
    val objectId: String,
    val shop: String,
    val title: String,
    val iconUrl: String? = null,
    val libraryHeroImageUrl: String? = null,
    val libraryImageUrl: String? = null,
    val logoImageUrl: String? = null,
    val logoPosition: String? = null,
    val coverImageUrl: String? = null,
    val downloadSources: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CatalogueSearchResult(
    // Внимание: /catalogue/* НЕ отдаёт id — дефолт обязателен, иначе Moshi уронит весь список.
    val id: String = "",
    val objectId: String,
    val title: String,
    val shop: String,
    val genres: List<String> = emptyList(),
    val releaseYear: Int? = null,
    val developers: List<String> = emptyList(),
    val publishers: List<String> = emptyList(),
    val libraryImageUrl: String? = null,
    val coverImageUrl: String? = null,
    val downloadSources: List<String> = emptyList(),
    // Внимание: сервер шлёт СТРОКУ ("verified"), а не список — List ронял бы парсинг.
    val deckCompatibility: String? = null
)

@JsonClass(generateAdapter = true)
data class CatalogueSearchResponse(val edges: List<CatalogueSearchResult>, val count: Int)

@JsonClass(generateAdapter = true)
data class CatalogueSearchPayload(
    val title: String = "",
    val sortBy: String = "popularity",
    val sortOrder: String = "desc",
    val tags: List<Int> = emptyList(),
    val genres: List<String> = emptyList(),
    val developers: List<String> = emptyList(),
    val publishers: List<String> = emptyList(),
    val downloadSourceFingerprints: List<String> = emptyList(),
    val protondbSupportBadges: List<String> = emptyList(),
    val deckCompatibility: List<String> = emptyList(),
    val platforms: List<String> = emptyList(),
    val take: Int = 30,
    val skip: Int = 0
)

@JsonClass(generateAdapter = true)
data class ReleaseYearFilter(val gte: Int? = null, val lte: Int? = null)

@JsonClass(generateAdapter = true)
data class GameStats(val downloadCount: Int = 0, val playerCount: Int = 0, val averageScore: Double? = null, val reviewCount: Int = 0)

@JsonClass(generateAdapter = true)
data class GameRepack(
    val id: String,
    val title: String,
    val fileSize: String? = null,
    val uris: List<String> = emptyList(),
    val unavailableUris: List<String> = emptyList(),
    val uploadDate: String? = null,
    val downloadSourceId: String,
    val downloadSourceName: String,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class DownloadSource(val id: String, val name: String, val url: String, val status: String = "MATCHED", val downloadCount: Int = 0, val fingerprint: String? = null)

// Порт use-search-suggestions: GET /catalogue/search/suggestions?query=&limit=
// возвращает объекты, а не строки.
@JsonClass(generateAdapter = true)
data class SearchSuggestion(
    val title: String,
    val objectId: String,
    val shop: String,
    val iconUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class HowLongToBeatCategory(val title: String, val duration: String, val accuracy: String)

@JsonClass(generateAdapter = true)
data class ProtonDBData(
    val tier: String,
    val confidence: String? = null,
    // Сервер шлёт дробь 0..1 (0.9), а не целое — Int ронял бы парсинг.
    val score: Double? = null,
    val total: Int = 0,
    val deckCompatibility: String? = null
)

@JsonClass(generateAdapter = true)
data class SteamAchievement(val name: String, val displayName: String, val description: String? = null, val icon: String? = null, val icongray: String? = null, val hidden: Int = 0)

@JsonClass(generateAdapter = true)
data class UserAchievement(
    val name: String, val displayName: String, val description: String? = null,
    val icon: String? = null, val unlocked: Boolean = false,
    val unlockTime: Long? = null, val points: Int = 0, val imageUrl: String? = null
)

// Точь-в-точь по @types: friendCode НЕТ (есть username), картинки nullable,
// подписка — объект subscription (null = нет).
@JsonClass(generateAdapter = true)
data class UserDetails(
    val id: String = "",
    val username: String = "",
    val email: String? = null,
    val displayName: String = "",
    val profileImageUrl: String? = null,
    val backgroundImageUrl: String? = null,
    val bio: String? = null,
    val subscription: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class FriendCurrentGame(val title: String? = null)

// currentGame — ОБЪЕКТ|null (не строка!), profileImageUrl nullable, isOnline опционален.
@JsonClass(generateAdapter = true)
data class Friend(
    val id: String = "",
    val displayName: String = "",
    val profileImageUrl: String? = null,
    val isOnline: Boolean = false,
    val currentGame: FriendCurrentGame? = null
)

@JsonClass(generateAdapter = true)
data class FriendsResponse(
    val totalFriends: Int = 0,
    val onlineFriends: Int = 0,
    val friends: List<Friend> = emptyList()
)

@JsonClass(generateAdapter = true)
data class NotificationsResponse(
    val notifications: List<HydraNotification> = emptyList()
)

@JsonClass(generateAdapter = true)
data class HydraNotification(
    val id: String = "",
    val type: String = "unknown",
    val variables: Map<String, String> = emptyMap(),
    val pictureUrl: String? = null,
    val url: String? = null,
    val isRead: Boolean = false,
    val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class ReviewsResponse(
    val reviews: List<GameReview> = emptyList(),
    val totalCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class ReviewUser(val displayName: String = "?")

// Реальный отзыв: author/content НЕТ — есть reviewHtml + user.displayName + translations{ru,…}.
@JsonClass(generateAdapter = true)
data class GameReview(
    val id: String,
    val score: Int,
    val reviewHtml: String = "",
    val createdAt: String = "",
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val user: ReviewUser? = null,
    val translations: Map<String, String> = emptyMap()
)

// shop-details: [{objectId, shop, data: {success, data: SteamAppDetails}}] — двойная вложенность.
@JsonClass(generateAdapter = true)
data class ShopDetailsResponse(
    val objectId: String,
    val shop: String,
    val data: ShopDetailsData? = null
)

@JsonClass(generateAdapter = true)
data class ShopDetailsData(
    val success: Boolean = false,
    val data: SteamAppDetails? = null
)

@JsonClass(generateAdapter = true)
data class SteamGenre(val description: String = "")

@JsonClass(generateAdapter = true)
data class SteamShot(val path_full: String = "")

@JsonClass(generateAdapter = true)
data class SteamAppDetails(
    val name: String? = null,
    val detailed_description: String? = null,
    val about_the_game: String? = null,
    val short_description: String? = null,
    val developers: List<String> = emptyList(),
    val publishers: List<String> = emptyList(),
    val genres: List<SteamGenre> = emptyList(),
    val header_image: String? = null,
    val screenshots: List<SteamShot> = emptyList(),
    val website: String? = null
)
data class LibraryGame(
    val objectId: String,
    val shop: String,
    val title: String,
    val coverUrl: String? = null,
    val iconUrl: String? = null,
    val heroUrl: String? = null,
    val logoUrl: String? = null,
    val playTimeMs: Long = 0,
    val lastPlayed: Long? = null,
    val favorite: Boolean = false,
    val isPinned: Boolean = false,
    val installed: Boolean = false, // на Android = "в каталоге/отслеживается", запуск запрещён
    val achievementCount: Int = 0,
    val unlockedCount: Int = 0,
    val collectionIds: List<String> = emptyList()
)
