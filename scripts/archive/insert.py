with open('app/src/main/java/com/example/feature/mastering/MasteringScreen.kt', 'r') as f:
    content = f.read()

target = "MasteringToolCategory.ALL_TOOLS -> {"
replacement = """MasteringToolCategory.ALL_TOOLS -> {
                        AudioMetadataEditorCard(
                            metadata = uiState.audioMetadata,
                            onMetadataChanged = { viewModel.updateAudioMetadata(it) }
                        )"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/feature/mastering/MasteringScreen.kt', 'w') as f:
    f.write(content)
