# Keep app workers and Hilt entry points discoverable after shrinking.
-keep class * extends androidx.work.ListenableWorker { <init>(...); }
-keep class com.airgf.app.AirGfApplication
-keep class dagger.hilt.** { *; }
-keep class hilt_aggregated_deps.** { *; }

# Keep generated serializers used by kotlinx.serialization.
-keep class kotlinx.serialization.** { *; }
-keep class com.airgf.app.**$$serializer { *; }

# LiteRT-LM and the model loader rely on JNI/native integrations.
-keep class com.google.ai.edge.litertlm.** { *; }

# MediaPipe image generation uses reflection and JNI.
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.auto.value.extension.memoized.Memoized
-dontwarn com.google.mediapipe.proto.CalculatorProfileProto$CalculatorProfile
-dontwarn com.google.mediapipe.proto.GraphTemplateProto$CalculatorGraphTemplate

# Filament / SceneView 3D renderer uses JNI.
-keep class com.google.android.filament.** { *; }
-keep class io.github.sceneview.** { *; }

# OkHttp - keep platform adapters used via reflection.
-dontwarn okhttp3.internal.platform.**
-keep class okhttp3.internal.platform.** { *; }

# Firebase Crashlytics / Performance / Analytics - preserve stack traces and SDK classes.
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Play Billing Library uses a different package prefix than GMS.
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# UMP (User Messaging Platform) consent SDK.
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.ump.**
