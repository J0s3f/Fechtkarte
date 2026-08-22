package at.j0s.meyercard.app.adapter.ui.train

/**
 * Pure shake-detection debounce logic: given consecutive accelerometer
 * magnitude readings and when they arrived, decides whether a shake should
 * actually fire. No `android.hardware` dependency at all — [ShakeToGenerate]
 * is the thin Composable wiring this to a real `SensorManager`. Debounced
 * because a single shake gesture produces many sensor events well above the
 * threshold in quick succession, not just one — regenerating on every event
 * would be twitchy (docs/PLAN.md, T5.4).
 */
class ShakeDebouncer(
    private val magnitudeThreshold: Float = 12f,
    private val minIntervalMillis: Long = 1000L,
) {
    private var lastShakeAtMillis: Long? = null

    /** Call once per accelerometer sample. Returns `true` exactly when a debounced shake fires. */
    fun onAccelerationSample(magnitude: Float, nowMillis: Long): Boolean {
        if (magnitude < magnitudeThreshold) return false
        val last = lastShakeAtMillis
        if (last != null && nowMillis - last < minIntervalMillis) return false
        lastShakeAtMillis = nowMillis
        return true
    }
}
