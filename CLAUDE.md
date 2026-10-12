# Ave-Müzik — CLAUDE.md

Offline yerel müzik çalar. Paket `com.avemuzik.player`, tek modül `:app`. Yanıtlar Türkçe.
Motto: "Telefonumdaki müzik. Reklamsız. İnternetsiz. Hızlı."

## Değişmez kurallar
- **İnternet yok:** `INTERNET`/`ACCESS_NETWORK_STATE` manifestte `tools:node="remove"`; ağ kütüphanesi, Firebase, analitik, online kapak EKLEME.
- Görünen ad tam olarak **Ave-Müzik**. Koyu tema varsayılan (`ThemeMode.Default = DARK`).
- Performans öncelikli: büyük framework ekleme (Coil/Glide/Hilt yok). Ana thread'de MediaStore/dosya işi yok.
- Gereksiz özellik ekleme; tek geliştiricinin anlayacağı sadelikte kal.
- UI metinleri yalnızca `res/values/strings.xml`'de (Türkçe). Sayılar `plurals` + `pluralStringResource`.
- Compose'da metin okumak için `LocalResources.current` kullan; `LocalContext...getString` kullanma (lint hatası).
- Media3 `@UnstableApi` kullanan sınıflara `@OptIn(UnstableApi::class)` ekle (`androidx.annotation.OptIn`); eklemezsen release lint'i hata verir.
- Deprecated API kullanma. Değişiklik sonrası build + lint temiz kalmalı.

## Token / context kuralları
- Projeyi baştan tarama. Aşağıdaki haritadan ilgili dosyayı seç, yalnızca onu oku. Emin değilsen önce `Grep` kullan, sonra hedef aralığı `Read` ile oku.
- `build/`, `.gradle/`, `app/schemas/`, `gradle/wrapper/*.jar` klasörlerini okuma.
- Küçük değişikliklerde `Edit` kullan, dosyayı baştan yazma. Düzenlediğin dosyayı doğrulamak için yeniden okuma.
- Gradle çıktısını dosyaya yönlendir; yalnızca özetini oku (`BUILD`, `e: `, `w: `, `What went wrong`).
- README'yi yalnızca kullanıcıya dönük bir değişiklik olduğunda güncelle.

## Build / kurulum (PowerShell)
```powershell
$env:JAVA_HOME = (Get-ChildItem "$env:USERPROFILE\.jdks" -Directory -Filter 'jdk-17*' | Select-Object -First 1).FullName
.\gradlew.bat assembleDebug            # hızlı derleme kontrolü
.\gradlew.bat testDebugUnitTest lintDebug assembleRelease   # tam doğrulama
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\release\app-release.apk
```
- SDK: `%LOCALAPPDATA%\Android\Sdk` (platform 36, build-tools 36.0.0). `sdkmanager` yerine `cmdline-tools\latest\bin\android.exe --no-metrics sdk install "<paket>"` kullanılır.
- GitHub indirmeleri bu makinede çok yavaş (~200 KB/s). Yeni bir Gradle sürümü gerekirse `mirrors.cloud.tencent.com/gradle/` aynasından indir ve resmî SHA-256 ile doğrula.
- Emülatör yok (BIOS'ta sanallaştırma kapalı). Gerçek test cihazı: Galaxy A55, Android 16, USB/adb ile.
- Telefonda dosya yöneticisinden APK kurulumu "Uygulama yüklenmedi" hatası veriyor. Bunun yerine `adb install -r` kullan.
- Release APK, `keystore.properties` yoksa debug anahtarıyla imzalanır.

## Sürümler (`gradle/libs.versions.toml`)
AGP 8.13.2 · Gradle 8.14.3 · Kotlin 2.2.21 · KSP 2.2.21-2.0.5 · Compose BOM 2025.12.01 · Media3 1.9.4 · Room 2.8.5 · minSdk 29 · target/compileSdk 36.
AGP 9'a geçiş bilinçli olarak yapılmadı: built-in Kotlin ve DSL değişiklikleri getiriyor. Gerekirse ayrı bir iş olarak ele al.

## Mimari haritası (`app/src/main/java/com/avemuzik/player/`)
| Yol | Görev |
|---|---|
| `AppContainer.kt` | Elle DI, her şey `lazy`. Yeni repository'yi buraya ekle |
| `MainActivity.kt` | Splash (tema okunana kadar), edge-to-edge, `PlayerController` lifecycle bağlantısı |
| `model/Models.kt` | Song, Album, Artist, Folder, Playlist, Library, ThemeMode |
| `data/media/` | `MediaStoreScanner` (IS_MUSIC, RELATIVE_PATH), `LibraryBuilder` (gruplama, Collator ile sıralama), `SearchNormalizer` |
| `data/artwork/` | `ArtworkDecoder` (`loadThumbnail` + gömülü kapağa küçültülmüş geri dönüş), `ArtworkLoader` (LRU, 200/480/1080 px kovaları, kapağı olmayanları hatırlar) |
| `data/database/` | Room: `favorites`, `playlists`, `playlist_songs` (position alanı var). Şema `app/schemas` altında. Değişiklikte version artır ve migration yaz |
| `data/repository/` | `MusicRepository` (kütüphane bir kez taranır, `StateFlow<LibraryState>`, ContentObserver ile 2 sn gecikmeli yeniler), Favorites, Playlist, Settings (DataStore) |
| `player/PlaybackService.kt` | `MediaSessionService`: ExoPlayer, bildirim, ses odağı, hata alınca sonraki şarkıya geçme |
| `player/QueuePlayer.kt` + `QueueShuffleOrder.kt` | Karışık çalma seçilen şarkıdan başlar; "Sıradaki olarak çal" karışık modda da doğru çalışır (testli) |
| `player/PlayerController.kt` | UI ile servis arası MediaController köprüsü. Komutlar, `PlaybackState`, `queue()` |
| `player/ArtworkBitmapLoader.kt` | Bildirim ve kilit ekranı kapağı (512 px'e küçültür) |
| `navigation/` | Tip güvenli `@Serializable` rotalar, `AppNavigator`, `AveNavHost` |
| `ui/AveMuzikRoot.kt` | İzin kontrolü, Scaffold, MiniPlayer, Snackbar (`LocalMessenger`), `SongActionsHost` |
| `ui/viewmodel/` | Activity kapsamında: `LibraryViewModel` (arama), `PlayerViewModel` (konum akışı), `CollectionsViewModel` (favori/liste CRUD) |
| `ui/components/` | SongRow, ArtworkImage, MiniPlayer, FastScroller, Dialogs, SongActions (ortak şarkı menüsü), LibraryItems, Locals |
| `ui/home/` | Sekmeler: Şarkılar, Albümler, Sanatçılar, Listeler, Klasörler |
| `ui/collection/` | Albüm/sanatçı/klasör/liste/favori için tek ortak detay ekranı + `CollectionViewModel` (`CollectionSource`) |
| `ui/nowplaying/` | Çalma ekranı (kapağa göre renklenen arka plan, SeekBar) + `QueueSheet` |
| `ui/search/`, `ui/settings/`, `ui/permission/` | Arama, tema ve yeniden tarama, izin ekranı (`AudioPermission`) |
| `src/test/` | QueueShuffleOrder, SearchNormalizer, LibraryBuilder testleri |

## Kalıplar
- Yeni şarkı listesi ekranı → `SongRow` + `LocalSongActions.current.open(song, playlistId?)`.
- Konum yalnızca `PlayerViewModel.position`'dan okunur; okuyan bileşeni küçük tut veya draw aşamasında oku.
- Satırlar için "şu an çalan" bilgisi: `state.map { it.songId }.distinctUntilChanged()`.
- Kütüphanede henüz olmayan detay ekranları için yeni bir `CollectionSource` ekle; yeni ekran yazma.

## Durum
Uygulamanın cihazda açıldığı doğrulandı; özellikler tek tek cihazda test edilmedi, yalnızca build ve birim testlerle doğrulandı. Uygulanmış özellikler: tarama, listeler, arama, mini oynatıcı ve çalma ekranı, karışık çalma, tekrar modları, arka planda çalma, bildirim, Bluetooth, favoriler, listeler, kuyruk, klasörler, tema.
Eksik: çalma sırasının uygulama yeniden açılınca geri yüklenmesi (`onPlaybackResumption`), sürükle-bırak sıralama, sıralama seçenekleri, İngilizce çeviri.
