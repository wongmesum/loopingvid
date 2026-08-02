import re

with open('app/src/main/java/com/example/core/ui/ExportViewModel.kt', 'r') as f:
    content = f.read()

# Remove all bad insertions
content = re.sub(r',\s*audioMetadata = config\.audioMetadata', '', content)

# Now find the place where ExportJobConfig.MasteringJob -> com.example.core.work.BatchExportRequest is created.
# We need to insert `audioMetadata = config.audioMetadata` inside it.
pattern_mastering = r'(is ExportJobConfig\.MasteringJob -> com\.example\.core\.work\.BatchExportRequest\([^)]+)(bitrate = state\.selectedBitrate)(\s*\))'
content = re.sub(pattern_mastering, r'\1\2,\n                audioMetadata = config.audioMetadata\3', content)

# Find executeMasteringJob call in ExportViewModel
pattern_execute = r'(mediaProcessor\.executeMasteringJob\([^)]+)(fadeOutSec = config\.fadeOutSec)(\s*\))'
content = re.sub(pattern_execute, r'\1\2,\n                            audioMetadata = config.audioMetadata\3', content)

with open('app/src/main/java/com/example/core/ui/ExportViewModel.kt', 'w') as f:
    f.write(content)
