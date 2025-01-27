package com.scepticalphysiologist.dmaple.ui

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.text.format.DateUtils
import android.view.MotionEvent
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.ViewModelProvider
import com.scepticalphysiologist.dmaple.R
import com.scepticalphysiologist.dmaple.databinding.RecorderBinding
import com.scepticalphysiologist.dmaple.ui.camera.Point
import java.time.Duration
import java.time.Instant


class Recorder : DMapLEPage<RecorderBinding>(RecorderBinding::inflate) {

    private lateinit var model: RecorderModel

    @SuppressLint("ClickableViewAccessibility")
    override fun createUI() {

        // Permissions
        val permissionsToAsk = PermissionSets.allPermissions().filter {
            (checkSelfPermission(binding.root.context, it)  == PackageManager.PERMISSION_DENIED)
        }.toSet()
        requestPermissions(permissionsToAsk.toTypedArray(), 6543)

        // Get the view model.
        model = ViewModelProvider(this.requireActivity()).get(RecorderModel::class.java)

        // Keep the screen on, so that the camera stays on.
        //binding.root.keepScreenOn = true

        // Once the view is inflated, connect the mapping service, passing it the camera preview.
        binding.root.post {
            model.connectMappingService(binding.root.context)
        }

        // Once the mapping service is connected: set it's preview; restore any saved ROIs from it;
        // and set the UI state.
        model.mappingServiceConnected.observe(viewLifecycleOwner) { isConnected ->
            if(isConnected) {
                model.setPreviewView(binding.cameraAndRoi.getCameraPreview())
                binding.cameraAndRoi.setSavedRois(model.getSavedRois())
                setUIState()
            }
        }

        // Whenever the saved ROIs have changed in the view, transfer these to the mapping
        // service so they are persisted.
        binding.cameraAndRoi.savedRoisHaveChanged().observe(viewLifecycleOwner) { haveChanged ->
            if(haveChanged) model.setSavedRois(binding.cameraAndRoi.getSavedRois())
        }

        // Start/stop recording.
        binding.recordButton.setOnClickListener {
            model.startStop()
            binding.maps.updateCreator(model.currentMapCreator())
            setUIState()
        }

        // Show warnings upon start/stop.
        model.warnings.observe(viewLifecycleOwner) { it.show(binding.root.context) }

        // When recording, allow the camera view to be resized by dragging near its
        // far (bottom right) corner.
        // Listen to the touch event in the child (map) view, as the event will register here first.
        // If there the camera is not dragged, then pass the event to the map view to be processed.
        binding.maps.setOnTouchListener { _, event ->
            if(!dragCameraView(event)) binding.maps.processMotionEvent(event) else true
        }

        // When recording, select the ROI of the map being shown.
        binding.cameraAndRoi.roiHasBeenSelected().observe(viewLifecycleOwner) { i ->
            // todo - use IDs for ROIs rather than index? Tried this but problems can come
            //     with copying and transforming
            if(model.isCreatingMaps()){
                model.setCurrentMap(i)
                binding.maps.updateCreator(model.currentMapCreator())
            }
        }

        // Update the timer shown during recording.
        model.timer.observe(viewLifecycleOwner) { elapsedSec ->
            binding.cameraTimer.text = DateUtils.formatElapsedTime(elapsedSec)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    /** Set the UI appearance depending on whether maps are being created. */
    private fun setUIState() {
        val isRecording = model.isCreatingMaps()

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
            binding.maps.start()
        } else {
            binding.cameraAndRoi.fullSize()
            binding.cameraTimer.text = ""
            binding.maps.stop()
        }
    }

    /** While recording, resize the camera view by dragging its lower-right corner. */
    private fun dragCameraView(event: MotionEvent): Boolean {
        if(model.isCreatingMaps() && (event.action == MotionEvent.ACTION_MOVE)) {
            val d = (Point.ofViewExtent(binding.cameraAndRoi) - Point.ofMotionEvent(event)).l2()
            if (d < 100) {
                binding.cameraAndRoi.resize(event.x.toInt(), event.y.toInt())
                return true
            }
        }
        return false
    }

}
