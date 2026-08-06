import re

with open('app/src/main/java/com/example/core/work/ExportQueueViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '            VideoExportWorker.KEY_OVERLAY_POSITION to request.overlayPosition\n        )',
    '            VideoExportWorker.KEY_OVERLAY_POSITION to request.overlayPosition,\n            VideoExportWorker.KEY_RESOLUTION to request.resolution,\n            VideoExportWorker.KEY_FRAME_RATE to request.frameRate,\n            VideoExportWorker.KEY_BITRATE to request.bitrate,\n            VideoExportWorker.KEY_ASPECT_RATIO to request.aspectRatio\n        )'
)

with open('app/src/main/java/com/example/core/work/ExportQueueViewModel.kt', 'w') as f:
    f.write(content)
