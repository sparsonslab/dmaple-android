from ij.IJ import Roi
from ij.plugin.frame import RoiManager
from ij.io import OpenDialog
import json

# A Jython script for creating ImageJ ROIs from a DMapLE-Android metadata.json.
# https://imagej.net/ij/developer/api/allclasses.html

# Open dialog to select the metadata json
dialog = OpenDialog("Select metadata")
path = dialog.getPath()

# Open the metadata file.
with open(path, 'r') as fs:
    metadata = json.load(fs)
    # Loop through the ROIs
    for roi in metadata["rois"]:
        # Create an ROI object.
        x0, y0 = roi['c0']['x'], roi['c0']['y']
        x1, y1 = roi['c1']['x'], roi['c1']['y']
        obj = Roi(x0, y0, x1 - x0, y1 - y0)
        obj.setName(roi['uid'])
        # Add to the ROI manager.
        mng = RoiManager.getInstance()
        mng.addRoi(obj)
