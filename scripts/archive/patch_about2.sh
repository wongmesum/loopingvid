sed -i 's/    onNavigateToSupport: () -> Unit = {}/    onNavigateToSupport: () -> Unit = {},\n    onNavigateToPrivacy: () -> Unit = {}/g' app/src/main/java/com/example/feature/about/AboutScreen.kt

sed -i 's/.clickable { openWebsite(context, "https:\/\/ulas.tech\/privacy") }/.clickable { onNavigateToPrivacy() }/g' app/src/main/java/com/example/feature/about/AboutScreen.kt

sed -i 's/                AboutScreen(/                AboutScreen(\n                    onNavigateToPrivacy = {\n                        navController.navigate(NavDestination.Privacy.route)\n                    },/g' app/src/main/java/com/example/ui/navigation/MainScreen.kt
