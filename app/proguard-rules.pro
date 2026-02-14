# Speak Alert Proguard Rules

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao
-keep class * extends androidx.room.TypeConverter

# --- Gson ---
# Keep model classes used for JSON serialization to prevent field obfuscation
-keep class com.ghostgramlabs.speakalert.data.model.** { *; }
-keep class com.ghostgramlabs.speakalert.domain.models.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# --- Jetpack Compose ---
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.ui.** { *; }

# --- Media3 / ExoPlayer ---
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }

# --- General Android ---
-keepattributes SourceFile,LineNumberTable
-keep public class * extends android.app.Service
-keep public class * extends android.app.Application
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
