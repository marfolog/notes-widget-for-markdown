package com.marfolog.noteswidgetformarkdown.presentation.setup

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.marfolog.noteswidgetformarkdown.ui.theme.NotesWidgetForMarkdownTheme

@Preview(showBackground = true, showSystemUi = true, name = "Setup - Unconfigured")
@Composable
private fun SetupScreenUnconfiguredPreview() {
    NotesWidgetForMarkdownTheme(dynamicColor = false) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            SetupScreenContent(
                vaultName = null,
                onSelectVault = {},
                notesFolderName = null,
                onSelectNotesFolder = {},
                derivedPath = "",
                canSave = false,
                onSave = {},
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Setup - Configured")
@Composable
private fun SetupScreenConfiguredPreview() {
    NotesWidgetForMarkdownTheme(dynamicColor = false) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            SetupScreenContent(
                vaultName = "MyMind",
                onSelectVault = {},
                notesFolderName = "hanges",
                onSelectNotesFolder = {},
                derivedPath = "hanges",
                canSave = true,
                onSave = {},
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Setup - Dark - Unconfigured")
@Composable
private fun SetupScreenUnconfiguredDarkPreview() {
    NotesWidgetForMarkdownTheme(dynamicColor = false, darkTheme = true) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            SetupScreenContent(
                vaultName = null,
                onSelectVault = {},
                notesFolderName = null,
                onSelectNotesFolder = {},
                derivedPath = "",
                canSave = false,
                onSave = {},
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Setup - Dark - Configured")
@Composable
private fun SetupScreenConfiguredDarkPreview() {
    NotesWidgetForMarkdownTheme(dynamicColor = false, darkTheme = true) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            SetupScreenContent(
                vaultName = "MyMind",
                onSelectVault = {},
                notesFolderName = "hanges",
                onSelectNotesFolder = {},
                derivedPath = "hanges",
                canSave = true,
                onSave = {},
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
