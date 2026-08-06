package com.example.feature.project

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.database.ProjectEntity

@Composable
fun ProjectManagerScreen(
    viewModel: ProjectManagerViewModel,
    onOpenHistory: () -> Unit,
    onOpenLoop: () -> Unit,
    onOpenSlideshow: () -> Unit,
    onOpenVisualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("project_manager_screen")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ProjectManagerHeader(
                projectCount = uiState.projects.size,
                onOpenHistory = onOpenHistory
            )

            if (uiState.projects.isEmpty()) {
                EmptyProjectsCard(
                    onCreateProject = { showCreateDialog = true },
                    onOpenLoop = onOpenLoop,
                    onOpenSlideshow = onOpenSlideshow,
                    onOpenVisualizer = onOpenVisualizer
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.projects, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            onDelete = { viewModel.deleteProject(project.id) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .testTag("create_project_fab")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Buat Proyek")
        }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, type ->
                viewModel.createProject(name, type)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun ProjectManagerHeader(projectCount: Int, onOpenHistory: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.FolderSpecial, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text("Project Manager", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("$projectCount proyek tersimpan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedButton(onClick = onOpenHistory, modifier = Modifier.testTag("open_history_button")) {
                Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Riwayat", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun EmptyProjectsCard(
    onCreateProject: () -> Unit,
    onOpenLoop: () -> Unit,
    onOpenSlideshow: () -> Unit,
    onOpenVisualizer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("empty_projects_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Rounded.FolderSpecial, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
            Text("Belum ada proyek", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Buat proyek baru atau mulai dari tool Studio.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onCreateProject, modifier = Modifier.testTag("empty_create_project_button")) {
                Text("Buat Proyek")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProjectShortcut(Icons.Rounded.Loop, "Loop", onOpenLoop)
                ProjectShortcut(Icons.Rounded.Slideshow, "Slideshow", onOpenSlideshow)
                ProjectShortcut(Icons.Rounded.GraphicEq, "Visualizer", onOpenVisualizer)
            }
        }
    }
}

@Composable
private fun ProjectShortcut(icon: ImageVector, label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(label, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun ProjectCard(project: ProjectEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("project_card_${project.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(projectIcon(project.projectType), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${project.type} • ${project.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "Hapus Proyek")
            }
        }
    }
}

@Composable
private fun CreateProjectDialog(onDismiss: () -> Unit, onCreate: (String, ProjectType) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ProjectType.LOOP) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat Proyek") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama proyek") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProjectType.values().forEach { option ->
                        OutlinedButton(onClick = { type = option }, enabled = option != type) {
                            Text(option.value.replaceFirstChar { it.uppercase() })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name.ifBlank { "Proyek Baru" }, type) }) {
                Text("Simpan")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

private fun projectIcon(type: ProjectType): ImageVector = when (type) {
    ProjectType.LOOP -> Icons.Rounded.Loop
    ProjectType.SLIDESHOW -> Icons.Rounded.Slideshow
    ProjectType.VISUALIZER -> Icons.Rounded.GraphicEq
}
