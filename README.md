# Ave-Müzik

> **Telefonumdaki müzik. Reklamsız. İnternetsiz. Hızlı.**

Ave-Müzik, Android telefonundaki yerel müzik dosyalarını hızlıca listeleyen ve çalan, sade ve modern bir müzik oynatıcıdır.

- Reklam yok, hesap yok, takip yok
- **Tamamen offline**: uygulamanın `INTERNET` izni yok (Manifest'te açıkça kaldırılmıştır, hiçbir kütüphane ekleyemez)
- Uçak modunda, Wi-Fi ve mobil veri kapalıyken eksiksiz çalışır
- Hızlı açılış, binlerce şarkıda akıcı liste

---

## İçindekiler

1. [Özellikler](#özellikler)
2. [Kullanılan teknolojiler](#kullanılan-teknolojiler)
3. [Proje mimarisi](#proje-mimarisi)
4. [Android Studio ile açma](#android-studio-ile-açma)
5. [Komut satırından build](#komut-satırından-build)
6. [Debug APK](#debug-apk)
7. [Release APK](#release-apk)
8. [Telefona yükleme](#telefona-yükleme)
9. [Android izinleri](#android-izinleri)
10. [Bilinen eksikler](#bilinen-eksikler)
11. [Sonraki geliştirme fikirleri](#sonraki-geliştirme-fikirleri)

---

## Özellikler

| Alan | Durum |
|---|---|
| MediaStore ile müzik tarama (MP3, M4A, AAC, FLAC, WAV, OGG, OPUS) | ✅ |
| Şarkı adı, sanatçı, albüm, süre, kapak, dosya konumu | ✅ |
| Eksik metadata için yedek değerler (Bilinmeyen Sanatçı / Albüm, dosya adından başlık) | ✅ |
| Android 13+ `READ_MEDIA_AUDIO`, Android 10–12 `READ_EXTERNAL_STORAGE` | ✅ |
| İzin reddedilince açıklama ekranı, kalıcı retlerde "Ayarları aç" | ✅ |
| Sekmeler: Şarkılar, Albümler, Sanatçılar, Listeler, Klasörler | ✅ |
| Hızlı kaydırma tutamağı (binlerce şarkı için) | ✅ |
| Yerel arama (şarkı / sanatçı / albüm; aksan ve büyük-küçük harf duyarsız: "muzik" → "Müzik") | ✅ |
| Kalıcı mini oynatıcı (kapak, başlık, sanatçı, oynat/duraklat, sonraki, ilerleme çizgisi) | ✅ |
| Çalma ekranı: büyük kapak, kapağa göre renklenen arka plan, seek bar, süreler | ✅ |
| Shuffle (seçilen şarkıdan başlar, her şarkı bir kez çalar) | ✅ |
| Repeat: Kapalı → Tümü → Tek şarkı | ✅ |
| Önceki / sonraki, şarkı bitince otomatik geçiş | ✅ |
| Arka planda çalma (uygulama değişince, ekran kapanınca, kilitliyken) | ✅ |
| Medya bildirimi + kilit ekranı kontrolleri (kapak, başlık, sanatçı) | ✅ |
| Bluetooth / kulaklık tuşları, kulaklık çıkınca otomatik duraklatma, ses odağı | ✅ |
| Favoriler (kalp ikonu, Room'da kalıcı) | ✅ |
| Çalma listeleri: oluştur, yeniden adlandır, sil, şarkı ekle / çıkar | ✅ |
| Çalma sırası: görüntüle, dokunarak geç, sıradan çıkar, "Sıradaki olarak çal", "Sıraya ekle" | ✅ |
| Klasör görünümü (Music/, Download/, WhatsApp Audio/ …) — Scoped Storage uyumlu | ✅ |
| Tema: Koyu (varsayılan) / Açık / Sistem; tercih DataStore'da kalıcı | ✅ |
| Kısa splash ekranı (yalnızca tema tercihi okunana kadar) | ✅ |
| Silinmiş dosya / desteklenmeyen format: çökme yok, mesaj gösterilir, sonrakine geçilir | ✅ |

---

## Kullanılan teknolojiler

| Katman | Teknoloji |
|---|---|
| Dil | Kotlin 2.2 |
| Arayüz | Jetpack Compose (BOM 2025.12), Material 3, Navigation Compose (tip güvenli rotalar) |
| Oynatma | AndroidX Media3 1.9 — ExoPlayer, MediaSession, `MediaSessionService` |
| Müzik kaynağı | Android `MediaStore` (Scoped Storage uyumlu) |
| Kalıcılık | Room (favoriler, listeler), DataStore Preferences (tema) |
| Asenkron | Kotlin Coroutines, StateFlow |
| Mimari | ViewModel + Repository, elle yazılmış hafif DI (`AppContainer`) |
| Build | Gradle 8.14.3 (wrapper), Android Gradle Plugin 8.13, KSP |
| SDK | `minSdk 29` (Android 10), `targetSdk 36` / `compileSdk 36` (Android 16) |

**Bilinçli olarak kullanılmayanlar:** ağ kütüphanesi (OkHttp, Retrofit vb.), görsel yükleme kütüphanesi (Coil/Glide — kapaklar için kendi küçük ve bellek sınırlı yükleyicimiz var), Firebase, analitik, DI framework'ü (Hilt/Koin).

**Neden minSdk 29?** Android 10 ile gelen Scoped Storage ve `ContentResolver.loadThumbnail()` API'leri sayesinde kapak okuma ve klasör yapısı, deprecated API kullanmadan temiz biçimde yapılabiliyor. Aktif Android cihazlarının büyük çoğunluğu Android 10 ve üzerindedir.

---

## Proje mimarisi

```
app/src/main/java/com/avemuzik/player/
├── AveMuzikApplication.kt      Uygulama sınıfı, AppContainer'ı oluşturur
├── AppContainer.kt             Elle DI: repository'ler, veritabanı, oynatıcı köprüsü (hepsi lazy)
├── MainActivity.kt             Splash, edge-to-edge, tema, Compose kökü
│
├── model/                      Song, Album, Artist, Folder, Playlist, Library, ThemeMode
│
├── data/
│   ├── media/                  MediaStoreScanner (tarama), LibraryBuilder (albüm/sanatçı/klasör
│   │                           gruplama), SearchNormalizer (aksansız arama anahtarı)
│   ├── artwork/                ArtworkDecoder (yerel kapak okuma), ArtworkLoader (LRU önbellek)
│   ├── database/               Room: AveDatabase, Entity'ler, FavoriteDao, PlaylistDao
│   └── repository/             MusicRepository, FavoritesRepository, PlaylistRepository,
│                               SettingsRepository (DataStore)
│
├── player/
│   ├── PlaybackService.kt      MediaSessionService: ExoPlayer + MediaSession + bildirim
│   ├── QueuePlayer.kt          Shuffle'ın seçilen şarkıdan başlamasını sağlayan ForwardingPlayer
│   ├── QueueShuffleOrder.kt    "Sıradaki olarak çal" karışık modda da doğru çalışır
│   ├── ArtworkBitmapLoader.kt  Bildirim / kilit ekranı / Bluetooth için yerel kapak
│   ├── PlayerController.kt     Arayüz ↔ servis köprüsü (MediaController)
│   ├── PlaybackState.kt        Arayüz durumu, kuyruk öğesi, hata türleri
│   └── MediaItems.kt           Song → MediaItem dönüşümü
│
├── navigation/                 Tip güvenli rotalar, AppNavigator, AveNavHost
│
└── ui/
    ├── AveMuzikRoot.kt         İzin kontrolü, Scaffold, mini oynatıcı, snackbar
    ├── theme/                  Renkler (koyu/açık), tipografi
    ├── components/             SongRow, ArtworkImage, MiniPlayer, FastScroller, diyaloglar,
    │                           şarkı menüsü (SongActions) vb.
    ├── viewmodel/              LibraryViewModel, PlayerViewModel, CollectionsViewModel
    ├── permission/             İzin ekranı ve sürüme göre izin seçimi
    ├── home/                   Ana ekran ve sekmeler
    ├── search/                 Arama ekranı
    ├── collection/             Albüm / sanatçı / klasör / liste / favoriler detay ekranı
    ├── nowplaying/             Çalma ekranı ve çalma sırası
    └── settings/               Ayarlar (tema, yeniden tarama, hakkında)
```

### Veri akışı

```
MediaStore ──► MusicRepository (bellekte tek kütüphane, StateFlow)
                    │
Room ──────► Favorites / Playlist Repository (Flow)
                    │
                    ▼
               ViewModel'ler (StateFlow) ──► Compose ekranları
                                                  │ komutlar
                                                  ▼
                       PlayerController (MediaController)
                                                  │ IPC değil, aynı süreç; MediaSession protokolü
                                                  ▼
          PlaybackService (MediaSessionService + ExoPlayer) ──► Bildirim, kilit ekranı, Bluetooth
```

### Performans kararları

- Kütüphane **bir kez** taranır ve bellekte tutulur; ekran değişimlerinde yeniden taranmaz. MediaStore değişince (yeni/silinen dosya) 2 sn gecikmeli tek bir arka plan taraması yapılır.
- Tüm MediaStore ve dosya işlemleri `Dispatchers.IO`'da, gruplama/arama `Dispatchers.Default`'ta.
- Kapaklar 3 boyut kovasına yuvarlanır (200/480/1080 px), bellek sınırlı LRU önbellekte tutulur; kapağı olmayan şarkılar tekrar denenmez; aynı anda en fazla 3 decode.
- Çalma konumu sadece dinleyen ekran varken ve şarkı çalarken 500 ms'de bir okunur. Mini oynatıcı ilerleme çizgisi konumu çizim aşamasında okur (recomposition yok).
- Listeler `LazyColumn` / `LazyVerticalGrid`, kararlı `key` ve `contentType` ile; satırlar yalnızca "şu an çalan şarkı" değişince güncellenir.
- Release build'de R8 küçültme ve kaynak daraltma açık.

---

## Android Studio ile açma

1. [Android Studio](https://developer.android.com/studio)'yu kurun (güncel sürüm).
2. **File → Open** ile bu klasörü (`Ave`) seçin.
3. Android Studio Gradle senkronizasyonunu otomatik yapar. İlk açılışta bağımlılıklar indirilir (bu adım için bir kez internet gerekir; uygulamanın kendisi internet kullanmaz).
4. Üst çubuktan `app` yapılandırmasını ve bağlı telefonunuzu seçip **Run ▶** düğmesine basın.

> Gradle JDK olarak Android Studio'nun kendi JDK'sı (JBR 17+) kullanılabilir: *Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK*.

---

## Komut satırından build

Gereksinimler: **JDK 17+** ve **Android SDK (platform 36)**. `local.properties` dosyasında SDK yolu bulunmalı (Android Studio bunu otomatik oluşturur):

```properties
sdk.dir=C\:\\Users\\<kullanıcı>\\AppData\\Local\\Android\\Sdk
```

Windows (PowerShell):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"   # veya kendi JDK 17 yolunuz
.\gradlew.bat assembleDebug
```

macOS / Linux:

```bash
./gradlew assembleDebug
```

---

## Debug APK

```powershell
.\gradlew.bat assembleDebug
```

Çıktı: `app/build/outputs/apk/debug/app-debug.apk` (~68 MB — küçültülmemiş, hata ayıklama bilgili; günlük kullanım için release APK önerilir)

Android Studio'dan: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.

### Testler ve lint

```powershell
.\gradlew.bat testDebugUnitTest   # karışık çalma sırası, arama normalizasyonu, kütüphane gruplama
.\gradlew.bat lintDebug           # rapor: app/build/reports/lint-results-debug.html
```

---

## Release APK

```powershell
.\gradlew.bat assembleRelease
```

Çıktı: `app/build/outputs/apk/release/app-release.apk` (~4,7 MB, R8 ile küçültülmüş ve optimize edilmiş).

### İmzalama

- **Kişisel kullanım:** proje kökünde `keystore.properties` yoksa release APK otomatik olarak **debug anahtarıyla** imzalanır ve doğrudan telefona yüklenebilir.
- **Google Play / kalıcı imza:** bir kez kendi anahtarınızı oluşturun:

  ```powershell
  keytool -genkeypair -v -keystore ave-muzik-release.jks -keyalg RSA -keysize 4096 -validity 10000 -alias avemuzik
  ```

  Proje köküne `keystore.properties` ekleyin (bu dosya ve `.jks` **git'e eklenmez**, `.gitignore`'da):

  ```properties
  storeFile=ave-muzik-release.jks
  storePassword=********
  keyAlias=avemuzik
  keyPassword=********
  ```

  Play Store için APK yerine App Bundle önerilir:

  ```powershell
  .\gradlew.bat bundleRelease     # app/build/outputs/bundle/release/app-release.aab
  ```

> ⚠️ Debug anahtarıyla imzalanmış bir sürümün üzerine kendi anahtarınızla imzalanmış sürüm kurulamaz (imza uyuşmazlığı). Önce eski sürümü kaldırın — favoriler ve listeler de silinir.

---

## Telefona yükleme

### USB ile (adb)

1. Telefonda **Ayarlar → Telefon hakkında → Yapım numarası**'na 7 kez dokunup Geliştirici seçeneklerini açın.
2. **Geliştirici seçenekleri → USB hata ayıklama**'yı açın, telefonu USB ile bağlayın ve bilgisayara izin verin.
3. Yükleyin:

   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
   ```

   veya derleyip tek adımda yüklemek için: `.\gradlew.bat installDebug`

### APK dosyasıyla

APK'yı telefona kopyalayın (USB, kablosuz paylaşım vb.), dosya yöneticisinden açın ve "Bilinmeyen kaynaklardan yüklemeye" izin verin.

---

## Android izinleri

| İzin | Neden | Android sürümü |
|---|---|---|
| `READ_MEDIA_AUDIO` | Müzik dosyalarını listelemek ve çalmak | 13+ |
| `READ_EXTERNAL_STORAGE` (`maxSdkVersion=32`) | Aynı amaç, eski sürümler | 10–12 |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Arka planda çalma servisi | Tümü / 14+ |
| `WAKE_LOCK` | Ekran kapalıyken çalmanın kesilmemesi | Tümü |
| `POST_NOTIFICATIONS` | Medya bildirimi (isteğe bağlı; reddedilse de müzik çalar) | 13+ |

**Bulunmayan izinler:** `INTERNET` ve `ACCESS_NETWORK_STATE` — `tools:node="remove"` ile açıkça kaldırıldı. Doğrulamak için:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\aapt.exe" dump permissions app\build\outputs\apk\release\app-release.apk
```

---

## Bilinen eksikler

- **Kaldığı yerden devam:** uygulama tamamen kapatılıp yeniden açıldığında son çalma sırası geri yüklenmez; Android 13+ "medya devam ettirme" (sistem medya kartından eski oturumu başlatma) desteklenmez.
- **Sürükle-bırak sıralama** henüz yok (kuyruk ve listeler; mimari hazır: `PlaylistSongEntity.position`, `Player.moveMediaItem`).
- **Sıralama seçenekleri** yok: şarkılar başlığa, albümler albüm adına, albüm içi parça numarasına göre sıralanır.
- Şarkı kimlikleri MediaStore kimlikleridir: telefon sıfırlanır veya medya veritabanı yeniden oluşturulursa favori/listelerdeki eşleşme kaybolabilir.
- Android 10–12'de bildirim izni yoktur, Android 13+'da bildirim izni reddedilirse sistem bildirimi gizleyebilir (müzik yine çalar, kilit ekranı kontrolleri çalışır).
- Ekolayzer, şarkı sözü, uyku zamanlayıcısı, Android Auto yok.
- Arayüz metinleri yalnızca Türkçe (İngilizce çeviri için `values-en/strings.xml` eklemek yeterli).

---

## Sonraki geliştirme fikirleri

1. Son çalma sırasını ve konumu kaydedip açılışta geri yükleme + `onPlaybackResumption` (Bluetooth "play" ile uygulama kapalıyken başlatma).
2. Kuyrukta ve listelerde sürükle-bırak sıralama.
3. Sıralama seçenekleri (eklenme tarihi, sanatçı, süre) ve son eklenenler görünümü.
4. Uyku zamanlayıcısı, oynatma hızı, ReplayGain / ses normalizasyonu, sistem ekolayzerine bağlantı.
5. Yerel `.lrc` dosyalarından senkron şarkı sözü.
6. Ana ekran widget'ı ve Quick Settings kutucuğu.
7. `MediaLibraryService`'e geçerek Android Auto / Wear OS gezinme desteği.
8. Liste dışa/içe aktarma (M3U) — yine tamamen yerel.
9. Baseline Profile ile daha da hızlı soğuk açılış.
10. İngilizce çeviri ve Play Store mağaza sayfası metinleri.
