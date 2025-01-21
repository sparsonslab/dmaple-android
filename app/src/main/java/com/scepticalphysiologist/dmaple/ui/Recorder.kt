package com.scepticalphysiologist.dmaple.ui

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.view.MotionEvent
import androidx.core.content.ContextCompat.checkSelfPermission
import com.scepticalphysiologist.dmaple.R
import com.scepticalphysiologist.dmaple.databinding.RecorderBinding
import com.scepticalphysiologist.dmaple.ui.camera.Point


class Recorder : DMapLEPage<RecorderBinding>(RecorderBinding::inflate) {

    @SuppressLint("ClickableViewAccessibility")
    override fun createUI() {

        // Permissions
        val permissionsToAsk = PermissionSets.allPermissions().filter {
            (checkSelfPermission(binding.root.context, it)  == PackageManager.PERMISSION_DENIED)
        }.toSet()
        requestPermissions(permissionsToAsk.toTypedArray(), 6543)

        // Start/stop recording.
        binding.recordButton.setOnClickListener {
            // Try to start/stop recording.
            val isRecording = binding.cameraAndRoi.startStop()

            // Reset map view.
            binding.maps.reset()

            // Button icon
            val icon = if(isRecording) R.drawable.stop_5f6368 else R.drawable.play_arrow
            binding.recordButton.setImageResource(icon)

            // Set camera view.
            if(isRecording) {
                var extent = Point.ofViewExtent(binding.root) * 0.5f
                binding.cameraAndRoi.resize(extent.x.toInt(), extent.y.toInt())
            } else binding.cameraAndRoi.fullSize()
        }

        // When recording, allow the camera view to be resized by dragging near its
        // far (bottom right) corner.
        // Listen to the touch event in the child (map) view, as the event will register here first.
        // If there the camera is not dragged, then pass the event to the map view to be processed.
        binding.maps.setOnTouchListener { _, event ->
            if(!dragCameraView(event)) binding.maps.processMotionEvent(event) else true
        }

        // Map update
        binding.cameraAndRoi.upDateMap.observe(viewLifecycleOwner) {
            binding.cameraAndRoi.getAnalyser(0)?.let {binding.maps.updateMap(it)}
        }

    }

    private fun dragCameraView(event: MotionEvent): Boolean {
        if(binding.cameraAndRoi.isRecording() && (event.action == MotionEvent.ACTION_MOVE)) {
            val d = (Point.ofViewExtent(binding.cameraAndRoi) - Point.ofMotionEvent(event)).l2()
            if (d < 100) {
                binding.cameraAndRoi.resize(event.x.toInt(), event.y.toInt())
                return true
            }
        }
        return false
    }


}
