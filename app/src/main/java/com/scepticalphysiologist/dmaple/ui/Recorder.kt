package com.scepticalphysiologist.dmaple.ui

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.view.MotionEvent
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.ViewModelProvider
import com.scepticalphysiologist.dmaple.R
import com.scepticalphysiologist.dmaple.databinding.RecorderBinding
import com.scepticalphysiologist.dmaple.ui.camera.Point


class Recorder : DMapLEPage<RecorderBinding>(RecorderBinding::inflate) {

    private lateinit var model: RecorderModel

    @SuppressLint("ClickableViewAccessibility")
    override fun createUI() {

        // Permissions
        val permissionsToAsk = PermissionSets.allPermissions().filter {
            (checkSelfPermission(binding.root.context, it)  == PackageManager.PERMISSION_DENIED)
        }.toSet()
        requestPermissions(permissionsToAsk.toTypedArray(), 6543)

        // Set the view model.
        model = ViewModelProvider(this).get(RecorderModel::class.java)
        setState()

        // Keep the screen on, so that the camera stays on.
        // todo - It would be better to run the camera on a foreground service.
        binding.root.keepScreenOn = true

        // Ensure that camera view is set-up correctly each time the root view is created and
        // has its inflated size - either when the fragment is created or resumed.
        binding.root.post {
            model.setCameraPreview(binding.cameraAndRoi.getCameraPreview())
            //binding.cameraAndRoi.setRois(model.rois)
            setState()
        }

        // Start/stop recording.
        binding.recordButton.setOnClickListener {
            model.startStop(binding.cameraAndRoi.getRois())
            setState()
        }

        // When recording, allow the camera view to be resized by dragging near its
        // far (bottom right) corner.
        // Listen to the touch event in the child (map) view, as the event will register here first.
        // If there the camera is not dragged, then pass the event to the map view to be processed.
        binding.maps.setOnTouchListener { _, event ->
            if(!dragCameraView(event)) binding.maps.processMotionEvent(event) else true
        }

        // Map update
        model.cameraAnalyser.upDateMap.observe(viewLifecycleOwner) {
            model.getAnalyser(0)?.let {binding.maps.updateMap(it)}
        }

    }

    private fun setState() {
        val isRecording = model.isRecording()

        // Button icon
        val icon = if(isRecording) R.drawable.stop_5f6368 else R.drawable.play_arrow
        binding.recordButton.setImageResource(icon)

        // Map view.
        if(!isRecording) binding.maps.reset()

        // Set camera view.
        binding.cameraAndRoi.allowEditing(!isRecording)
        if(isRecording) {
            val extent = Point.ofViewExtent(binding.root) * 0.5f
            binding.cameraAndRoi.resize(extent.x.toInt(), extent.y.toInt())
        } else {
            binding.cameraAndRoi.fullSize()
        }
    }

    private fun dragCameraView(event: MotionEvent): Boolean {
        if(model.isRecording() && (event.action == MotionEvent.ACTION_MOVE)) {
            val d = (Point.ofViewExtent(binding.cameraAndRoi) - Point.ofMotionEvent(event)).l2()
            if (d < 100) {
                binding.cameraAndRoi.resize(event.x.toInt(), event.y.toInt())
                return true
            }
        }
        return false
    }


}
