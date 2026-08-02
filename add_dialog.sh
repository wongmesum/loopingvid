sed -i 's/var passedLiveSourceUri/val exportState by androidx.compose.runtime.collectAsState(exportViewModel.uiState, kotlin.coroutines.EmptyCoroutineContext)\n    var passedLiveSourceUri/g' app/src/main/java/com/example/ui/navigation/MainScreen.kt

cat << 'INNER_EOF' >> app/src/main/java/com/example/ui/navigation/MainScreen.kt
// Extension function or end of MainScreen to ensure no conflict. We can inject ExportDialog right before the end of Scaffold or outside it.
INNER_EOF
