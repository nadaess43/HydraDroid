# HydraDroid 0.0.1 (alpha) — порт Hydra Launcher на Android

Порт дизайна и функционала оригинала `hydra/`, адаптированный под телефон
(не копия 1:1 попиксельно, а именно порт: то же поведение, удобное на тач-экране).

## Что скопировано точь-в-точь

| Оригинал (PC) | Android-порт | Файл |
|---|---|---|
| `scss/globals.scss` токены | `HydraColors` + `HydraDimens` | `ui/theme/HydraColors.kt` |
| Noto Sans 400/500/700, 14/13/12 | `HydraTypography` | `ui/theme/HydraType.kt` + `HydraTheme.kt` |
| `App` layout: Sidebar 250 + Header + Outlet + BottomPanel | `Scaffold`: BottomBar-табы (телефон) + Header с back+search + BottomPanel-статус | `MainActivity.kt` |
| Sidebar 5 роутов + active `rgba(255,255,255,0.1)` | Те же 5 табов, `indicatorColor = ActiveMenuItem` | `MainActivity.kt` |
| Header search debounce 180/250мс, 255 char | `delay(250)`, `take(255)`, автопереход в каталог | `MainActivity.kt` |
| `Hero` 180→400px, logo, gradient | `HeroBanner` 220dp + gradient + logo/title | `ui/components/HydraComponents.kt` |
| `GameCard` 100%x180 r4, cover scale, `translateY` reveal, Badge max3+N, ⬇/👥/★ | `GameCard` (reveal всегда виден — мобильное удобство) | `HydraComponents.kt` |
| `Button` primary/outline/dark/cloud/danger 40px r8 | `HydraButton(kind)` | `HydraComponents.kt` |
| `Badge` blur 10px | `HydraBadge` | `HydraComponents.kt` |
| `Skeleton #1c1c1c/#444` | `SkeletonCard` | `HydraComponents.kt` |
| Home: Hero + Hot/Weekly/Achievements + Surprise me + grid 1→2→3→4 | `HomeScreen` | `ui/screens/HomeCatalogue.kt` |
| Catalogue: result_count + sort + chips + grid + filters + pagination | `CatalogueScreen` (sort Dropdown, +кнопка) | `HomeCatalogue.kt` |
| Library: CategoryFilter + Collections + sort + grid/compact/large + empty states | `LibraryScreen` | `ui/screens/LibraryDownloads.kt` |
| Downloads: 3 группы + прогресс/speed/ETA/peers | `DownloadsScreen` (модель очереди сохранена) | `LibraryDownloads.kt` |
| GameDetails: hero full-bleed + HeroPanel + 2-col + Gallery + Sidebar(repacks/HLTB/achievements) | `GameDetailsScreen` | `ui/screens/GameDetails.kt` |
| Profile: Hero banner+avatar96+friend-code+табы | `ProfileScreen` | `ui/screens/ProfileEtc.kt` |
| Achievements: Panel Earned points + List | `AchievementsScreen` | `ProfileEtc.kt` |
| Notifications take 20 | `NotificationsScreen` | `ProfileEtc.kt` |
| Settings aside 10 секций | `SettingsScreen` (Rail + секции, Desktop-only скрыты) | `ProfileEtc.kt` |
| `HydraApi` axios + User-Agent + 401→signout | `HydraApiClient` Retrofit/Moshi/OkHttp | `data/remote/` |
| Endpoints catalogue/search/:category/suggestions/assets/stats/download-sources/hltb/protondb/shop-details/profile/* | `HydraService` 1:1 | `data/remote/HydraService.kt` |
| LevelDB games/collections/assetsCache TTL 8ч | Room `HydraDatabase` | `data/local/HydraDatabase.kt` |
| UserPreferences ~50 полей + language | DataStore `HydraPrefs` | `data/local/HydraPrefs.kt` |
| Auth deep-link `handleExternalAuth` | `hydradroid://auth` intent-filter | `AndroidManifest.xml` |

## Адаптации под телефон (порт, а не копия)

- **Запуск игр** — кнопки запуска нет (сторона ПК); всё остальное — каталог, торренты, библиотека — работает.
- **Торрент-движок** — настоящий libtorrent 2.0 через jlibtorrent (DHT, magnet, .torrent, раздача, выбор файлов) + foreground-сервис с докачкой после рестарта.
- **HTTP** — свой загрузчик с Range-докачкой + Debrid unrestrict (Real-Debrid/TorBox/Premiumize).
- **Распаковщик** — zip/7z/rar/tar с прогрессом (ISO/многотомные — внешним приложением).
- **Hover-эффекты** (cover scale 1.05, content translateY) → на тач-экране метаданные видны всегда.
- **Sidebar 250px** → BottomBar (тот же порядок и иконки); настройки — одна колонка.
- **Главная** — Hero уезжает вверх при скролле как в оригинале + бесконечная автодогрузка ленты.

## Что работает по-настоящему (не заглушки)

- Библиотека на Room: добавление из каталога/страницы игры/скана папки, избранное, коллекции, удаление, сортировка, фильтр, категории PC/Ретро, кнопка «Скачать» на карточке
- Каталог: бесконечная кнопка «Показать ещё», подсказки-объекты, история поиска, фильтр жанров + fingerprint источников, сортировка
- Главная: Hero скроллится, бесконечная лента по табам (hot/weekly/achievements), «Мне повезёт»
- Страница игры: чищеное описание (без HTML-мусора), галерея с fullscreen, отзывы (ru-переводы), HLTB, ProtonDB, «Скачать» (диалог движка) + «В очередь»
- Загрузки: группы активные/ожидают/ошибки/готовые, прогресс/скорости/ETA/пиры/рейтео, пауза, выбор файлов торрента, распаковка, открытие файлов
- Настройки: язык, Debrid-токены, папка (SAF), лимиты, сидирование, трекеры, источники (импорт 28 готовых из ПК-Hydra + по URL); вход через Hydra, токены в EncryptedSharedPreferences, выход с чисткой
- Профиль: гостевое состояние с кнопкой входа, друзья с онлайном

## Открыть проект (кнопка Run сверху)

1. Android Studio Ladybug+ → Open → `HydraDroid/`
2. `local.properties`: `sdk.dir=...`
3. Sync Gradle → Run на API 26+.

## API база

`app/build.gradle.kts` → `BuildConfig.HYDRA_API_URL` (`https://hydra-api-us-east-1.losbroxas.org`, auth `https://auth.hydra.losbroxas.org` — извлечены из установленной Hydra, т.к. старые хосты не резолвятся).
