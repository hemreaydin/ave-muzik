# Ave-Müzik R8 kuralları.
# Media3, Room, DataStore ve kotlinx.serialization kendi consumer kurallarını getirir.

# Navigation Compose tip güvenli rotaları (@Serializable) korunur.
-keep class com.avemuzik.player.navigation.** { *; }

# Hata raporlarında okunabilir stack trace için satır bilgisi.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
