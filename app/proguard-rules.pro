# =================================
#    Mysa Money ProGuard Rules
# =================================

# --- Base Android & Kotlin ---
-keep class kotlin.coroutines.jvm.internal.BaseContinuationImpl { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# --- Room (Database) ---
# This keeps all your @Entity, @Dao, etc. classes
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# --- Data Models (Entities & API Models) ---
# This stops R8 from renaming your data classes, which would break
# Room, GSON (Gemini), and the Billing library.
-keep class com.giantnovadevs.mysamoney.data.** { *; }
-keep class com.giantnovadevs.mysamoney.viewmodel.GeminiRequest { *; }
-keep class com.giantnovadevs.mysamoney.viewmodel.Content { *; }
-keep class com.giantnovadevs.mysamoney.viewmodel.Part { *; }
-keep class com.giantnovadevs.mysamoney.viewmodel.GeminiResponse { *; }
-keep class com.giantnovadevs.mysamoney.viewmodel.Candidate { *; }
-keep class com.giantnovadevs.mysamoney.viewmodel.ChatMessage { *; }

# --- Google Gemini / GSON (AI Coach) ---
# Keeps the @SerializedName annotations used by GSON
-keepattributes *Annotation*
-keep class com.google.gson.annotations.** { *; }
-keep class com.google.ai.client.generativeai.** { *; }

# --- Google Drive API (Backup/Restore) ---
# These are the rules to prevent the GSON/HTTP client from crashing
-keep class com.google.api.client.util.Data { *; }
-keep class com.google.api.services.drive.** { *; }
-keepclassmembers class com.google.api.services.drive.model.** { *; }
-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}

# --- Google Sign-In & Auth ---
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.api.client.googleapis.extensions.android.gms.auth.** { *; }

# --- Google Play Billing (In-App Purchase) ---
-keep class com.android.vending.billing.** { *; }
-dontwarn com.android.vending.billing.**

# --- Google AdMob (Ads) ---
-keep class com.google.android.gms.ads.** { *; }
-keep public class com.google.android.gms.ads.internal.ClientApi { *; }

# --- iText7 (PDF Export) ---
# Keeps the PDF library's core classes
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# --- MPAndroidChart (Charts) ---
# Keeps the chart library's classes
-keep class com.github.mikephil.charting.** { *; }

# --- OkHttp (Used by Gemini & Google APIs) ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# --- Google Guava (Pulled in by Google APIs) ---
# This rule is critical for minSdk 24
-keep class com.google.common.** { *; }
-dontwarn com.google.common.**