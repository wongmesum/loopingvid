import re

with open('app/src/main/java/com/example/feature/mastering/MasteringScreen.kt', 'r') as f:
    content = f.read()

# Add when branch
when_branch = """
                    MasteringToolCategory.AUDIO_METADATA -> {
                        AudioMetadataEditorCard(
                            metadata = uiState.audioMetadata,
                            onMetadataChanged = { viewModel.updateAudioMetadata(it) }
                        )
                    }
                    MasteringToolCategory.AUDIO_FADES -> {
"""

content = content.replace("MasteringToolCategory.AUDIO_FADES -> {", when_branch.lstrip('\n'))

all_tools = """
                        AudioMetadataEditorCard(
                            metadata = uiState.audioMetadata,
                            onMetadataChanged = { viewModel.updateAudioMetadata(it) }
                        )
                        AudioFadeControlCard(
"""

content = content.replace("AudioFadeControlCard(", all_tools.lstrip('\n'), 1) # Only replace the first one which is inside ALL_TOOLS or AUDIO_FADES? 
# Wait, replacing `AudioFadeControlCard(` directly might replace the wrong one. Let's do it more safely.

with open('app/src/main/java/com/example/feature/mastering/MasteringScreen.kt', 'w') as f:
    f.write(content)

