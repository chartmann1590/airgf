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
