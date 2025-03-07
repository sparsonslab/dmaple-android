package com.scepticalphysiologist.dmaple.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.text.format.DateUtils
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.scepticalphysiologist.dmaple.R
import com.scepticalphysiologist.dmaple.SettingsActivity
import com.scepticalphysiologist.dmaple.databinding.RecorderBinding
import com.scepticalphysiologist.dmaple.etc.PermissionSets
import com.scepticalphysiologist.dmaple.etc.Point
import com.scepticalphysiologist.dmaple.map.creator.MapCreator
import com.scepticalphysiologist.dmaple.map.field.FieldImage


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

        // A record index has been provided (by navigation from from explorer fragment).
        // Load the record.
        arguments?.getInt("recordIdx")?.let { model.loadRecord(it) }

        // Keep the screen on, so that the camera stays on.
        //binding.root.keepScreenOn = true

        // Once the view is inflated, set the mapping service's camera surface.
        binding.root.post {
            model.setCameraPreview(binding.cameraAndRoi.getCameraPreview())
            binding.cameraAndRoi.setExposureSlider(0.5f)
            binding.cameraAndRoi.setRoisAndRuler(model.getRoisAndRuler())
            setUIState()
        }

        // Exposure control.
        binding.cameraAndRoi.exposure.observe(viewLifecycleOwner) { model.setExposure(it) }

        // Start/stop recording.
        binding.recordButton.setOnClickListener {
            println("PRESSED!!!")
            model.updateState(binding.cameraAndRoi.getRoisAndRuler())
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

        // When ROI has been selected, update the map shown.
        binding.cameraAndRoi.roiHasBeenSelected().observe(viewLifecycleOwner) { selectedRoiUID ->
            if(stateShowsMap()){
                model.updateCurrentlyShownMap(selectedRoiUID)
                showMapAndCreator(model.getCurrentlyShownMap())
            }
        }

        // Update the timer shown during recording.
        model.timer.observe(viewLifecycleOwner) { elapsedSec ->
            binding.cameraTimer.text = DateUtils.formatElapsedTime(elapsedSec)
        }

        // Got to records.
        binding.toRecordsButton.setOnClickListener {
            findNavController().navigate(R.id.recorder_to_explorer)
        }

        // Go to settings
        binding.toSettingsButton.setOnClickListener {
            activity?.startActivity(Intent(activity, SettingsActivity::class.java))
        }

    }

    /** Set the UI appearance depending on whether maps are being created. */
    private fun setUIState() {
        println("STATE = ${model.getState()}")
        when(model.getState()) {
            RecState.PRE_RECORD -> {
                binding.recordButton.setImageResource(R.drawable.play_arrow)
                setMappingUI(show = false, recording = false)
                binding.maps.reset()
            }
            RecState.RECORDING -> {
                binding.recordButton.setImageResource(R.drawable.stop_5f6368)
                setMappingUI(show = true, recording = true)
            }
            RecState.POST_RECORD -> {
                binding.recordButton.setImageResource(R.drawable.eject_arrow)
                setMappingUI(show = true, recording = false, field = model.getLastFieldImage())
            }
            RecState.OLD_RECORD -> {
                binding.recordButton.setImageResource(R.drawable.eject_arrow)
                setMappingUI(show = true, recording = false, field = model.getLastFieldImage())
            }
        }
    }

    /** Set the state of the mapping UI.
     * @param show The maps should be shown.
     * @param recording The maps are being recorded.
     * @param field The field image to show in the camera view or null (to show the camera feed).
     * */
    private fun setMappingUI(show: Boolean, recording: Boolean, field: FieldImage? = null) {
        // Set the camera and map views.
        binding.cameraAndRoi.freezeField(field)
        if(show) {
            binding.toRecordsButton.visibility = View.INVISIBLE
            binding.cameraAndRoi.allowEditing(false)
            val extent = Point.ofViewExtent(binding.root) * 0.5f
            binding.cameraAndRoi.resize(extent.x.toInt(), extent.y.toInt())
            showMapAndCreator(model.getCurrentlyShownMap())
        } else {
            binding.toRecordsButton.visibility = View.VISIBLE
            binding.cameraAndRoi.allowEditing(true)
            binding.cameraAndRoi.fullSize()
            binding.cameraTimer.text = ""
        }

        // Set the live update status of maps and spines.
        // THIS HAS TO BE DONE AFTER THE ABOVE
        // otherwise the UI state can get out-of-sync.
       // binding.cameraAndRoi.startSpine(recording)
        if(recording) binding.maps.start() else binding.maps.stop()
    }

    /** Set the creator and map being shown in the map and camera field views. */
    private fun showMapAndCreator(creatorAndMapIdx: Pair<MapCreator?, Int>) {
        binding.maps.updateCreator(creatorAndMapIdx)
        binding.cameraAndRoi.updateCreator(creatorAndMapIdx)
    }

    /** If the fragment showing maps? */
    private fun stateShowsMap(): Boolean{
        val state = model.getState()
        return state != RecState.PRE_RECORD
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
