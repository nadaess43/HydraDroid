package com.hydradroid.data.remote

import com.hydradroid.data.model.*
import retrofit2.http.*

// Прямой порт HydraApi + catalogue endpoints (§2 отчёта).
// baseUrl = BuildConfig.HYDRA_API_URL, User-Agent: HydraDroid vX (аналог Hydra Launcher vX)
interface HydraService {
    @POST("/catalogue/search")
    suspend fun searchCatalogue(@Body payload: CatalogueSearchPayload): CatalogueSearchResponse

    @GET("/catalogue/{category}")
    suspend fun getCatalogueCategory(
        @Path("category") category: String, // hot | weekly | achievements
        @Query("take") take: Int = 12,
        @Query("skip") skip: Int = 0,
        // Важно: скобки! axios оригинала сериализует массивы как ids[]=.. —
        // без скобок сервер молча игнорирует фильтр и отдаёт [].
        @Query("downloadSourceIds[]") downloadSourceIds: List<String> = emptyList()
    ): List<CatalogueSearchResult>

    @GET("/catalogue/search/suggestions")
    suspend fun getSearchSuggestions(
        @Query("query") query: String,
        @Query("limit") limit: Int = 5
    ): List<SearchSuggestion>

    @GET("/catalogue/steam/genres")
    suspend fun getGenres(): List<String>

    @GET("/catalogue/steam/tags")
    suspend fun getTags(): List<Map<String, Any>>

    @GET("/games/{shop}/{objectId}/assets")
    suspend fun getGameAssets(@Path("shop") shop: String, @Path("objectId") objectId: String): ShopAssets?

    @GET("/games/{shop}/{objectId}/stats")
    suspend fun getGameStats(@Path("shop") shop: String, @Path("objectId") objectId: String): GameStats

    @GET("/games/{shop}/{objectId}/download-sources")
    suspend fun getDownloadSources(
        @Path("shop") shop: String, @Path("objectId") objectId: String,
        @Query("take") take: Int = 100,
        @Query("skip") skip: Int = 0,
        @Query("downloadSourceIds[]") downloadSourceIds: List<String> = emptyList()
    ): List<GameRepack>

    @GET("/games/{shop}/{objectId}/how-long-to-beat")
    suspend fun getHltb(@Path("shop") shop: String, @Path("objectId") objectId: String): List<HowLongToBeatCategory>

    @GET("/games/{shop}/{objectId}/protondb")
    suspend fun getProtonDb(@Path("shop") shop: String, @Path("objectId") objectId: String): ProtonDBData?

    @POST("/games/shop-details")
    suspend fun getShopDetails(@Body body: Map<String, Any>): List<ShopDetailsResponse>

    @GET("/games/{shop}/{objectId}/reviews")
    suspend fun getReviews(
        @Path("shop") shop: String, @Path("objectId") objectId: String,
        @Query("take") take: Int = 10, @Query("skip") skip: Int = 0
    ): ReviewsResponse

    @GET("/profile/me")
    suspend fun getMe(): UserDetails

    @GET("/profile/friends")
    suspend fun getFriends(): FriendsResponse

    @GET("/profile/notifications")
    suspend fun getNotifications(@Query("locale") locale: String = "en"): NotificationsResponse

    @POST("/download-sources/changes")
    suspend fun checkDownloadChanges(@Body body: Map<String, Any>): List<Map<String, Any>>

    // Порт addDownloadSource: POST {url} → канонический источник с fingerprint (needsAuth false).
    @POST("/download-sources")
    suspend fun addDownloadSource(@Body body: Map<String, String>): DownloadSource
}
