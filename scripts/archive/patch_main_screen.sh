sed -i 's/composable(NavDestination.Loop.route) {/composable(NavDestination.Loop.route) {\n                LoopScreen(\n                    exportViewModel = exportViewModel,/g' app/src/main/java/com/example/ui/navigation/MainScreen.kt
sed -i '/LoopScreen(/,+1d' app/src/main/java/com/example/ui/navigation/MainScreen.kt

sed -i 's/composable(NavDestination.Mastering.route) {/composable(NavDestination.Mastering.route) {\n                MasteringScreen(\n                    exportViewModel = exportViewModel,/g' app/src/main/java/com/example/ui/navigation/MainScreen.kt
sed -i '/MasteringScreen(/,+1d' app/src/main/java/com/example/ui/navigation/MainScreen.kt

sed -i 's/composable(NavDestination.Editor.route) {/composable(NavDestination.Editor.route) {\n                EditorScreen(\n                    exportViewModel = exportViewModel,/g' app/src/main/java/com/example/ui/navigation/MainScreen.kt
sed -i '/EditorScreen(/,+1d' app/src/main/java/com/example/ui/navigation/MainScreen.kt
