package com.madinatti.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.math.*

class QiblaFragment : Fragment(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var compassView: QiblaCompassView? = null
    private var tvBearing: TextView? = null
    private var tvDirection: TextView? = null
    private var tvDistance: TextView? = null
    private var tvAccuracy: TextView? = null
    private var tvGpsStatus: TextView? = null
    private var tvCoords: TextView? = null
    private var gpsIndicator: View? = null

    private var qiblaBearing = 0f
    private var distanceKm = 0.0
    private var hasLocation = false

    private val meccaLat = 21.4225
    private val meccaLon = 39.8262

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)

    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(
            requireActivity()
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { loc ->
                onLocationReceived(loc.latitude, loc.longitude)
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) {
            requestLocation()
        } else {
            tvGpsStatus?.text = getString(R.string.qibla_no_gps)
            tvGpsStatus?.setTextColor(Color.parseColor("#CC5555"))
            gpsIndicator?.setBackgroundColor(
                Color.parseColor("#CC5555")
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_qibla, container, false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Status bar spacer
        val statusBarHeight = requireContext()
            .getSharedPreferences("ui_prefs", 0)
            .getInt("status_bar_height", 0)
        view.findViewById<View>(R.id.statusBarSpacer)?.apply {
            layoutParams.height = statusBarHeight
            requestLayout()
        }

        // Back button
        view.findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        sensorManager = requireContext().getSystemService(
            Context.SENSOR_SERVICE
        ) as SensorManager

        compassView = view.findViewById(R.id.qiblaCompass)
        tvBearing = view.findViewById(R.id.tvQiblaBearing)
        tvDirection = view.findViewById(R.id.tvQiblaDirection)
        tvDistance = view.findViewById(R.id.tvQiblaDistance)
        tvAccuracy = view.findViewById(R.id.tvAccuracy)
        tvGpsStatus = view.findViewById(R.id.tvGpsStatus)
        tvCoords = view.findViewById(R.id.tvCoords)
        gpsIndicator = view.findViewById(R.id.gpsIndicator)

        checkAndRequestLocation()
    }

    private fun checkAndRequestLocation() {
        val fine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarse = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fine == PackageManager.PERMISSION_GRANTED ||
            coarse == PackageManager.PERMISSION_GRANTED
        ) {
            requestLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocation() {
        tvGpsStatus?.text = getString(R.string.qibla_loading)
        tvGpsStatus?.setTextColor(Color.parseColor("#F39C12"))

        // Try last known first
        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                onLocationReceived(loc.latitude, loc.longitude)
            }
        }

        // Also request fresh updates
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000
        ).setMinUpdateIntervalMillis(2000)
            .setMaxUpdates(5)
            .build()

        fusedClient.requestLocationUpdates(
            request, locationCallback,
            requireActivity().mainLooper
        )
    }

    private fun onLocationReceived(lat: Double, lon: Double) {
        hasLocation = true
        qiblaBearing = calcBearing(lat, lon).toFloat()
        distanceKm = calcDistance(lat, lon)

        compassView?.setQiblaBearing(qiblaBearing)

        val dir = directionName(qiblaBearing)
        tvBearing?.text = String.format("%.1f°", qiblaBearing)
        tvDirection?.text = dir
        tvDistance?.text = String.format(
            "%.0f km %s", distanceKm,
            getString(R.string.qibla_towards_mecca)
        )

        tvCoords?.text = String.format(
            "%.4f, %.4f", lat, lon
        )

        tvGpsStatus?.text = " GPS actif"
        tvGpsStatus?.setTextColor(Color.parseColor("#2ECC71"))
        gpsIndicator?.background?.setTint(
            Color.parseColor("#2ECC71")
        )
    }

    override fun onResume() {
        super.onResume()
        val rotation = sensorManager.getDefaultSensor(
            Sensor.TYPE_ROTATION_VECTOR
        )
        if (rotation != null) {
            sensorManager.registerListener(
                this, rotation,
                SensorManager.SENSOR_DELAY_GAME
            )
        } else {
            val accel = sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER
            )
            val mag = sensorManager.getDefaultSensor(
                Sensor.TYPE_MAGNETIC_FIELD
            )
            accel?.let {
                sensorManager.registerListener(
                    this, it,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }
            mag?.let {
                sensorManager.registerListener(
                    this, it,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }
            if (accel == null || mag == null) {
                tvAccuracy?.text = getString(
                    R.string.qibla_compass_unavailable
                )
                tvAccuracy?.setTextColor(
                    Color.parseColor("#CC5555")
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        fusedClient.removeLocationUpdates(locationCallback)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                val rm = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(
                    rm, event.values
                )
                val orient = FloatArray(3)
                SensorManager.getOrientation(rm, orient)
                val az = Math.toDegrees(
                    orient[0].toDouble()
                ).toFloat()
                compassView?.updateAzimuth((az + 360) % 360)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(
                    event.values, 0, gravity, 0, 3
                )
                updateFallback()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(
                    event.values, 0, geomagnetic, 0, 3
                )
                updateFallback()
            }
        }
    }

    private fun updateFallback() {
        val r = FloatArray(9)
        val i = FloatArray(9)
        if (SensorManager.getRotationMatrix(
                r, i, gravity, geomagnetic
            )
        ) {
            val orient = FloatArray(3)
            SensorManager.getOrientation(r, orient)
            val az = Math.toDegrees(
                orient[0].toDouble()
            ).toFloat()
            compassView?.updateAzimuth((az + 360) % 360)
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?, accuracy: Int
    ) {
        when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> {
                tvAccuracy?.text = getString(
                    R.string.qibla_accuracy_high
                )
                tvAccuracy?.setTextColor(
                    Color.parseColor("#2ECC71")
                )
            }
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> {
                tvAccuracy?.text = getString(
                    R.string.qibla_accuracy_medium
                )
                tvAccuracy?.setTextColor(
                    Color.parseColor("#F39C12")
                )
            }
            SensorManager.SENSOR_STATUS_ACCURACY_LOW,
            SensorManager.SENSOR_STATUS_UNRELIABLE -> {
                tvAccuracy?.text = getString(
                    R.string.qibla_accuracy_low
                )
                tvAccuracy?.setTextColor(
                    Color.parseColor("#CC5555")
                )
            }
        }
    }


    private fun calcBearing(lat: Double, lon: Double): Double {
        val p1 = Math.toRadians(lat)
        val p2 = Math.toRadians(meccaLat)
        val dl = Math.toRadians(meccaLon - lon)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    private fun calcDistance(lat: Double, lon: Double): Double {
        val R = 6371.0
        val p1 = Math.toRadians(lat)
        val p2 = Math.toRadians(meccaLat)
        val dp = Math.toRadians(meccaLat - lat)
        val dl = Math.toRadians(meccaLon - lon)
        val a = sin(dp / 2).pow(2) +
                cos(p1) * cos(p2) * sin(dl / 2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun directionName(bearing: Float): String {
        return when {
            bearing < 22.5 || bearing >= 337.5 -> "Nord"
            bearing < 67.5 -> "Nord-Est"
            bearing < 112.5 -> "Est"
            bearing < 157.5 -> "Sud-Est"
            bearing < 202.5 -> "Sud"
            bearing < 247.5 -> "Sud-Ouest"
            bearing < 292.5 -> "Ouest"
            else -> "Nord-Ouest"
        }
    }
}