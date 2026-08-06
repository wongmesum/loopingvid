sed -i 's/composable(NavDestination.Loop.route) {/composable(NavDestination.Loop.route) {\n                LoopScreen(\n                    exportViewModel = exportViewModel,\n                    viewModel = loopViewModel,/g' app/src/main/java/com/example/ui/navigation/MainScreen.kt

sed -i 's/composable(NavDestination.Mastering.route) {/composable(NavDestination.Mastering.route) {\n                MasteringScreen(\n                    exportViewModel = exportViewModel,\n                    viewModel = masteringViewModel,/g' app/src/main/java/com/example/ui/navigation/MainScreen.kt

sed -i 's/composable(NavDestination.Editor.route) {/composable(NavDestination.Editor.route) {\n                EditorScreen(\n                    exportViewModel = exportViewModel,\n                    viewModel = editorViewModel,/g' app/src/main/java/com/example/ui/navigation/MainScreen.kt
