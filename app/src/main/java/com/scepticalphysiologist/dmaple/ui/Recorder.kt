package com.scepticalphysiologist.dmaple.ui

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.DateUtils
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.ViewModelProvider
import com.scepticalphysiologist.dmaple.MainActivity
import com.scepticalphysiologist.dmaple.R
import com.scepticalphysiologist.dmaple.databinding.RecorderBinding
import com.scepticalphysiologist.dmaple.etc.PermissionSets
import com.scepticalphysiologist.dmaple.etc.Point


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

        // Once the view is inflated.
        binding.root.post {
            MainActivity.setCameraPreviewSurface(binding.cameraAndRoi.getCameraPreview())
            binding.cameraAndRoi.setExposureSlider(0.5f)
            binding.cameraAndRoi.setSavedRois(model.getMappingRois())
            setUIState()
        }


        // Whenever the saved ROIs have changed in the view, transfer these to the mapping
        // service (via the view model) so they are persisted.
        binding.cameraAndRoi.savedRoisHaveChanged().observe(viewLifecycleOwner) { haveChanged ->
            if(haveChanged) model.setMappingRois(binding.cameraAndRoi.getSavedRois())
        }

        // Exposure control.
        binding.cameraAndRoi.exposure.observe(viewLifecycleOwner) { model.setExposure(it) }

        // Start/stop recording.
        binding.recordButton.setOnClickListener {
            model.updateState()
            setUIState()
        }

        // Show warnings upon start/stop.
        var ignore = true // Prevent the last messages being shown at start-up.
        model.messages.observe(viewLifecycleOwner) {
            if(!ignore) it?.show(binding.root.context)
            ignore = false
        }

        // When recording, allow the camera view to be resized by dragging near its
        // far (bottom right) corner.
        // Listen to the touch event in the child (map) view, as the event will register here first.
        // If there the camera is not dragged, then pass the event to the map view to be processed.
        binding.maps.setOnTouchListener { _, event ->
            if(!dragCameraView(event)) binding.maps.processMotionEvent(event) else true
        }

        // When recording, select the ROI of the map being shown.
        binding.cameraAndRoi.roiHasBeenSelected().observe(viewLifecycleOwner) { selectedRoiIndex ->
            // todo - use IDs for ROIs rather than index? Tried this but problems can come
            //     with copying and transforming
            roiSelected(selectedRoiIndex)
        }

        // Update the timer shown during recording.
        model.timer.observe(viewLifecycleOwner) { elapsedSec ->
            binding.cameraTimer.text = DateUtils.formatElapsedTime(elapsedSec)
        }
    }

    /** Set the UI appearance depending on whether maps are being created. */
    private fun setUIState() {
        when(model.getState()) {
            0 -> {
                binding.recordButton.setImageResource(R.drawable.play_arrow)
                binding.maps.reset()
                binding.cameraAndRoi.allowEditing(true)
                binding.cameraAndRoi.fullSize()
                binding.cameraTimer.text = ""
                binding.maps.stop()
            }
            1 -> {
                binding.recordButton.setImageResource(R.drawable.stop_5f6368)
                binding.cameraAndRoi.allowEditing(false)
                val extent = Point.ofViewExtent(binding.root) * 0.5f
                binding.cameraAndRoi.resize(extent.x.toInt(), extent.y.toInt())
                binding.maps.updateCreator(model.creatorOfCurrentlyShownMap())
                binding.maps.start()
            }
            2 -> {
                binding.recordButton.setImageResource(R.drawable.eject_arrow)
                binding.cameraAndRoi.allowEditing(false)
            }
        }
    }

    private fun stateShowsMap(): Boolean{
        val state = model.getState()
        return (state == 1) || (state == 2)
    }

    private fun roiSelected(roiIndex: Int) {
        if(stateShowsMap()){
            model.setCurrentlyShownMap(roiIndex)
            binding.maps.updateCreator(model.creatorOfCurrentlyShownMap())
        }
    }

    /** While recording, resize the camera view by dragging its lower-right corner. */
    private fun dragCameraView(event: MotionEvent): Boolean {
        if(stateShowsMap() && (event.action == MotionEvent.ACTION_MOVE)) {
            val d = (Point.ofViewExtent(binding.cameraAndRoi) - Point.ofMotionEvent(event)).l2()
            if (d < 100) {
                binding.cameraAndRoi.resize(event.x.toInt(), event.y.toInt())
                return true
            }
        }
        return false
    }

}
