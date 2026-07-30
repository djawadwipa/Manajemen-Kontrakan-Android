# Room, Hilt, and kotlinx.serialization publish consumer ProGuard rules.
# Remove verbose Android logging from optimized release builds.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
