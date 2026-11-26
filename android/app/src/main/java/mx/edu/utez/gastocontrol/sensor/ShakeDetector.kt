package mx.edu.utez.gastocontrol.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.sqrt

//aqui detectamos cuando el usuario agita el celular
//solo avisa mediante el callback onShake(), el start y stop los llama HomeScreen segun el ciclo de vida
//si el dispositivo no tiene acelerometro, no truena asi que simplemente no hace nada

class ShakeDetector(
    context : Context,
    private val onShake: () -> Unit
): SensorEventListener {
    //el servicio del sistema es el que da acceso a todos los sensores
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE)
            as SensorManager

    //sera nullo si el equipo no tuviera el acelerometro
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastShakeTime = 0L

    // Evita registrar el listener dos veces (start() puede llamarse más de una vez)
    private var escuchando = false

    val isAvailable: Boolean
        get() = accelerometer != null

    //empieza a escuchar aqui el sensor, si no existiera, simplemente no hace nada
    fun start() {
        if (escuchando) return
        accelerometer?.let {
            escuchando = sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    //deja de escuchar el sistema, libera el sensor para no gastar bateria
    fun stop() {
        if (!escuchando) return
        sensorManager.unregisterListener(this)
        escuchando = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH

        //El umbral lo hacemos para ignorar movimientos leves
        if (gForce > SHAKE_THRESHOLD_G) {
            val now = SystemClock.elapsedRealtime()

            //Una sola sacudida, un solo aviso
            if (now - lastShakeTime > COOLDOWN_MS) {
                lastShakeTime = now
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    companion object {
        const val SHAKE_THRESHOLD_G = 2.5f
        const val COOLDOWN_MS = 1500L
    }
}