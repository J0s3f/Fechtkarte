package at.j0s.meyercard.app.adapter.ui.train

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlin.math.sqrt

/**
 * Registers an accelerometer listener for as long as this is in
 * composition and [enabled] is true, calling [onShake] on each debounced
 * shake ([ShakeDebouncer]). Composed into
 * [at.j0s.meyercard.app.adapter.ui.FechtkarteApp]'s Train route only, so
 * it's active exactly while Train is on screen — leaving Train (Compose
 * Navigation disposes the previous destination) unregisters the listener
 * automatically via [DisposableEffect]'s `onDispose`.
 *
 * Keyed on [enabled], not `Unit`: a user who turns this off in Configure
 * shouldn't have to leave and re-enter Train (or generate a new card) for
 * it to actually stop listening — the effect tears down and, if still
 * enabled, re-registers whenever the value changes.
 */
@Composable
fun ShakeToGenerate(enabled: Boolean, onShake: () -> Unit) {
    val context = LocalContext.current
    val currentOnShake = rememberUpdatedState(onShake)

    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val debouncer = ShakeDebouncer()

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = event.values
                val magnitude = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
                if (debouncer.onAccelerationSample(magnitude, System.currentTimeMillis())) {
                    currentOnShake.value()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }
}
