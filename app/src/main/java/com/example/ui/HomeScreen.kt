package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.HistorySheet
import com.example.ui.components.PageSelectorSheet
import com.example.ui.components.PostPreviewCard
import com.example.ui.theme.FacebookBlue
import com.example.ui.theme.SparkleYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PostViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val masterPrompt by viewModel.masterPrompt.collectAsStateWithLifecycle()
    val todayTopic by viewModel.todayTopic.collectAsStateWithLifecycle()
    val selectedPage by viewModel.selectedPage.collectAsStateWithLifecycle()
    val pages by viewModel.pages.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    val primaryLang by viewModel.primaryLang.collectAsStateWithLifecycle()
    val secondaryLang by viewModel.secondaryLang.collectAsStateWithLifecycle()

    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val generationStage by viewModel.generationStage.collectAsStateWithLifecycle()
    val isPosting by viewModel.isPosting.collectAsStateWithLifecycle()
    val postedId by viewModel.postedId.collectAsStateWithLifecycle()

    val caption by viewModel.generatedCaption.collectAsStateWithLifecycle()
    val bitmap by viewModel.generatedBitmap.collectAsStateWithLifecycle()
    val notification by viewModel.notification.collectAsStateWithLifecycle()

    val showPageDialog by viewModel.showPageDialog.collectAsStateWithLifecycle()
    val showHistoryDialog by viewModel.showHistoryDialog.collectAsStateWithLifecycle()

    var isMasterPromptExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(notification) {
        notification?.let {
            snackbarHostState.showSnackbar(it.message)
            viewModel.clearNotification()
        }
    }

    val topicInspirations = listOf(
        "5 Daily Habits for Peak Focus",
        "How Generative AI Transforms Modern Work",
        "Simple Steps for a Mindful Morning",
        "Behind the Scenes of Our Studio",
        "Sustainable Living Hacks for Everyday Life"
    )

    val masterPromptPresets = listOf(
        "⚡ Tech & AI" to "Tech Authority Voice: Analytical, visionary, future-forward. Visual style: High-tech minimalism, cybernetic glow, clean geometric framing, cinematic lighting.",
        "🌿 Wellness" to "Mindful & Warm Voice: Compassionate, calming, grounded. Visual style: Soft natural morning light, organic textures, serene botanical composition, earthy tones.",
        "💼 Business" to "Executive & Growth Voice: Decisive, strategic, high-value actionable insights. Visual style: Modern architectural workspace, sleek minimalist editorial photography, crisp lines.",
        "🎨 Creative" to "Inspiring & Artistic Voice: Bold, imaginative, playful. Visual style: Vibrant surrealism, vivid colors, artistic depth, studio lighting."
    )

    val languagePairs = listOf(
        Pair("English", "Spanish"),
        Pair("English", "French"),
        Pair("English", "German"),
        Pair("English", "Japanese"),
        Pair("English", "Arabic")
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(FacebookBlue, MaterialTheme.colorScheme.tertiary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SocialPulse",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Imagen 3 + Gemini Facebook Studio",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.setShowHistoryDialog(true) },
                        modifier = Modifier.testTag("history_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Post History & Drafts",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { viewModel.setShowPageDialog(true) },
                        modifier = Modifier.testTag("pages_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Facebook Pages",
                            tint = FacebookBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding()
                .widthIn(max = 680.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Target Facebook Page Selector Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setShowPageDialog(true) }
                    .testTag("target_page_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(FacebookBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = selectedPage?.pageName?.firstOrNull()?.uppercase() ?: "F",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Target Facebook Page",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = selectedPage?.pageName ?: "No Page Selected (Tap to configure)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (selectedPage != null) "ID: ${selectedPage?.pageId}" else "Configure Page ID & Access Token",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.setShowPageDialog(true) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp).testTag("switch_page_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Switch", fontSize = 12.sp)
                    }
                }
            }

            // 2. Master Prompt Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("master_prompt_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Master Prompt & Brand Guidelines",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { isMasterPromptExpanded = !isMasterPromptExpanded },
                            modifier = Modifier.size(32.dp).testTag("toggle_master_prompt_button")
                        ) {
                            Icon(
                                imageVector = if (isMasterPromptExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isMasterPromptExpanded) "Collapse" else "Expand"
                            )
                        }
                    }

                    Text(
                        text = "Controls the brand voice, visual aesthetics for Imagen 3, and bilingual tone for Gemini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = masterPrompt,
                        onValueChange = { viewModel.updateMasterPrompt(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("master_prompt_input"),
                        maxLines = if (isMasterPromptExpanded) 8 else 3,
                        minLines = if (isMasterPromptExpanded) 4 else 2,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Quick Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        masterPromptPresets.forEach { (label, preset) ->
                            AssistChip(
                                onClick = { viewModel.applyPromptPreset(preset) },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }

                        AssistChip(
                            onClick = { viewModel.applyPromptPreset(viewModel.defaultMasterPrompt) },
                            label = { Text("Reset Default", fontSize = 12.sp) }
                        )
                    }
                }
            }

            // 3. Today's Topic Input Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_topic_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = SparkleYellow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Today's Topic",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = todayTopic,
                        onValueChange = { viewModel.updateTodayTopic(it) },
                        placeholder = { Text("Enter post theme, idea, or announcement...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("today_topic_input"),
                        singleLine = false,
                        maxLines = 3,
                        trailingIcon = {
                            if (todayTopic.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateTodayTopic("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear topic")
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Topic Suggestions Chips
                    Text(
                        text = "Quick Topic Inspirations:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        topicInspirations.forEach { idea ->
                            AssistChip(
                                onClick = { viewModel.applyTopicPreset(idea) },
                                label = { Text(idea, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bilingual Language Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bilingual Pairing:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        languagePairs.forEach { pair ->
                            val isSelected = primaryLang == pair.first && secondaryLang == pair.second
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateLanguages(pair.first, pair.second) },
                                label = { Text("${pair.first} + ${pair.second}", fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // 4. Generate Button with Progress Indicators
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { viewModel.generatePost() },
                    enabled = !isGenerating && todayTopic.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("generate_post_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Generating with Imagen 3 & Gemini...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = SparkleYellow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (caption.isNotBlank()) "Regenerate Visual & Caption" else "Generate Post (Imagen 3 + Gemini)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                AnimatedVisibility(visible = isGenerating) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = generationStage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 5. Preview & Direct Facebook Posting Section
            if (caption.isNotBlank() || bitmap != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Post Preview & Graph API Publishing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                PostPreviewCard(
                    page = selectedPage,
                    caption = caption,
                    imageBitmap = bitmap,
                    isPosting = isPosting,
                    postedId = postedId,
                    onCaptionChange = { viewModel.updateGeneratedCaption(it) },
                    onPostToFacebook = { viewModel.postToFacebook() }
                )
            }
        }
    }

    // Modal Sheets
    if (showPageDialog) {
        PageSelectorSheet(
            pages = pages,
            selectedPage = selectedPage,
            onSelectPage = { viewModel.updateSelectedPage(it) },
            onAddNewPage = { name, pageId, token, isDefault ->
                viewModel.addNewPage(name, pageId, token, isDefault)
            },
            onDeletePage = { viewModel.deletePage(it) },
            onVerifyToken = { pageId, token, callback ->
                viewModel.verifyPageToken(pageId, token, callback)
            },
            onDismiss = { viewModel.setShowPageDialog(false) }
        )
    }

    if (showHistoryDialog) {
        HistorySheet(
            historyList = history,
            onLoadHistoryItem = { viewModel.loadFromHistory(it) },
            onDeleteItem = { viewModel.deleteHistoryItem(it) },
            onDismiss = { viewModel.setShowHistoryDialog(false) }
        )
    }
}
