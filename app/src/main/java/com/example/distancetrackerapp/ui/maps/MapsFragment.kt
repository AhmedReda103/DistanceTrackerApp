package com.example.distancetrackerapp.ui.maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.distancetrackerapp.R
import com.example.distancetrackerapp.databinding.FragmentMapsBinding
import com.example.distancetrackerapp.model.Result
import com.example.distancetrackerapp.service.TrackerService
import com.example.distancetrackerapp.ui.maps.MapUtil.setCameraPosition
import com.example.distancetrackerapp.utils.Constants
import com.example.distancetrackerapp.utils.Constants.ACTION_SERVICE_STARTED
import com.example.distancetrackerapp.utils.Constants.ACTION_SERVICE_STOP
import com.example.distancetrackerapp.utils.ExtensionFunction.disable
import com.example.distancetrackerapp.utils.ExtensionFunction.enable
import com.example.distancetrackerapp.utils.ExtensionFunction.hasBackgroundLocationPermission
import com.example.distancetrackerapp.utils.ExtensionFunction.hide
import com.example.distancetrackerapp.utils.ExtensionFunction.show
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MapsFragment : Fragment(), OnMapReadyCallback, OnMyLocationButtonClickListener , OnMarkerClickListener {

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!
    lateinit var map: GoogleMap
    private var locationList = mutableListOf<LatLng>()
    private var startTime: Long = 0
    private var stopTime: Long = 0

    val markerList = mutableListOf<Marker>()

     val started = MutableLiveData(false)

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val polylineList  = mutableListOf<Polyline>()

    private var backgroundResultLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(requireContext(), "granted ", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "not Granted ", Toast.LENGTH_SHORT).show()
                Constants.showRationaleDialog(
                    "App requires background location access all time ",
                    "Background location permission is essential to this application. " +
                            "without it we will not be able to provide you with our service ",
                    requireContext()
                )
            }
        }

    private var notificationResultLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(requireContext(), "granted ", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "not Granted ", Toast.LENGTH_SHORT).show()
                Constants.showRationaleDialog(
                    "App requires sending you notification ",
                    "notification permission is essential to this application. " +
                            "without it we will not be able to provide you with our service ",
                    requireContext()
                )
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.tracking = this

        binding.startBtn.setOnClickListener { onStartButtonClicked() }
        binding.stopBtn.setOnClickListener {
            onStopClickButton()
        }
        binding.resetBtn.setOnClickListener {
            onResetButtonClicked()
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        return binding.root
    }

    private fun onResetButtonClicked() {
        onResetMap()
    }

    @SuppressLint("MissingPermission")
    private fun onResetMap() {
        fusedLocationProviderClient.lastLocation.addOnCompleteListener {
            val lastKnowLocation = LatLng(it.result.latitude , it.result.longitude)

            for(polyline in polylineList){
                polyline.remove()
            }
            for (marker in markerList){
                marker.remove()
            }
            markerList.clear()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(setCameraPosition(lastKnowLocation)))
            locationList.clear()
            binding.resetBtn.hide()
            binding.startBtn.show()
        }

    }

    private fun onStopClickButton() {
        stopForegroundService()
        binding.stopBtn.hide()
        binding.startBtn.show()
        //binding.startBtn.enable()
    }

    private fun stopForegroundService() {
        binding.startBtn.disable()
        sendActionCommandToService(ACTION_SERVICE_STOP)
    }

    private fun onStartButtonClicked() {
        if (requireContext().hasBackgroundLocationPermission()) {
            startCountDown()
            binding.startBtn.hide()
            binding.startBtn.disable()
            binding.stopBtn.show()
            //sendActionCommandToService(ACTION_SERVICE_STARTED)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundResultLauncher.launch(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionForApi33()
        }
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }


    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMyLocationButtonClickListener(this)
        //val sydney = LatLng(-34.0, 151.0)
        //map.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        // map.moveCamera(CameraUpdateFactory.newLatLng(sydney))
        map.isMyLocationEnabled = true

        map.setOnMarkerClickListener(this)
        map.uiSettings.apply {
            isZoomControlsEnabled = false
            isZoomGesturesEnabled = false
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = false
            isCompassEnabled = false
            isScrollGesturesEnabled = false
        }
        observeTrackerService()
    }


    private fun observeTrackerService() {
        TrackerService.locationList.observe(viewLifecycleOwner) {
            if (it != null) {
                locationList = it
                if (locationList.size > 1) {
                    binding.stopBtn.enable()
                }
                Log.d("MAPS", "observeTrackerService:${locationList.toString()} ")
                drawPolyLine()
                followPolyline()
            }
        }

        TrackerService.started.observe(viewLifecycleOwner){
            started.value = it
        }

        TrackerService.startTime.observe(viewLifecycleOwner) {
            startTime = it
        }
        TrackerService.stopTime.observe(viewLifecycleOwner) {
            stopTime = it
            if (stopTime != 0L) {
                showBiggerPicture()
                displayResult()
            }
        }
    }

    private fun displayResult(){
        val result = Result(MapUtil.calculateDistance(locationList) , MapUtil.calculateElapsedTime(startTime , stopTime))
        lifecycleScope.launch {
            delay(2500)
            val directions = MapsFragmentDirections.actionMapsFragmentToResultFragment(result)
            findNavController().navigate(directions)
            binding.startBtn.apply {
                hide()
                enable()
            }
            binding.stopBtn.hide()
            binding.resetBtn.show()
        }

    }

    private fun showBiggerPicture() {

        val bounds = LatLngBounds.builder()
        for (location in locationList) {
            bounds.include(location)
        }
        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(bounds.build(), 100),
            2000,
            null
        )
        addMarker(locationList.first())
        addMarker(locationList.last())
    }

    private fun addMarker(position : LatLng){
       val marker =  map.addMarker(MarkerOptions().position(position))
        if (marker != null) {
            markerList.add(marker)
        }
    }

    private fun drawPolyLine() {
        val polyLine = map.addPolyline(
            PolylineOptions().apply {
                startCap(ButtCap())
                endCap(ButtCap())
                color(Color.BLUE)
                width(10f)
                jointType(JointType.ROUND)
                addAll(locationList)
            }
        )
        polylineList.add(polyLine)
    }

    private fun followPolyline() {
        if (locationList.isNotEmpty()) {
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                   setCameraPosition(
                        locationList.last()
                    )
                )
            )
        }
    }


    private fun startCountDown() {
        binding.timerTextview.show()
        binding.stopBtn.disable()
        val timer = object : CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val currentSecond = millisUntilFinished / 1000
                if (currentSecond.toString() == "0") {
                    binding.timerTextview.text = "GO"
                    binding.timerTextview.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.black
                        )
                    )
                } else {
                    binding.timerTextview.text = currentSecond.toString()
                    binding.timerTextview.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.red
                        )
                    )
                }
            }

            override fun onFinish() {
                sendActionCommandToService(ACTION_SERVICE_STARTED)
                binding.timerTextview.hide()
            }
        }
        timer.start()
    }


    private fun sendActionCommandToService(action: String) {
        Intent(requireContext(), TrackerService::class.java).apply {
            this.action = action
            requireContext().startService(this)
        }
    }


    override fun onMyLocationButtonClick(): Boolean {
        binding.hintTextview.animate().alpha(0f).duration = 1500
        lifecycleScope.launch {
            delay(2500)
            binding.hintTextview.hide()
            binding.startBtn.show()
        }
        return false
    }


    @SuppressLint("InlinedApi")
    private fun requestNotificationPermissionForApi33() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            Constants.showRationaleDialog(
                "Distance Tracker App requires notification access ",
                "app cannot work without notification permission ",
                requireContext()
            )
        } else {
            notificationResultLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        return true
    }
}

