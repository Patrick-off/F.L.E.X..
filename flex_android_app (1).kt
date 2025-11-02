// MainActivity.kt
package com.flex.ai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Theme Colors
object FlexTheme {
    val Primary = Color(0xFF22D3EE)
    val Secondary = Color(0xFFA78BFA)
    val DarkBg = Color(0xFF0A0E1A)
    val CardBg = Color(0xFF1A1F35)
    val TextPrimary = Color(0xFFE0E0E0)
    val TextSecondary = Color(0xFFA0A0A0)
    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    
    val GradientBrush = Brush.linearGradient(
        colors = listOf(Primary, Secondary)
    )
}

// Data Models
data class Query(
    val id: String,
    val question: String,
    val status: QueryStatus,
    val timestamp: Long,
    val consensus: Consensus? = null,
    val aiResponses: List<AIResponse> = emptyList()
)

enum class QueryStatus {
    PROCESSING, COMPLETED, FAILED
}

data class Consensus(
    val summary: String,
    val confidence: Float,
    val convergencePoints: List<String>,
    val divergencePoints: List<String>
)

data class AIResponse(
    val model: AIModel,
    val response: String,
    val confidence: Float,
    val reasoning: List<String>
)

enum class AIModel(val displayName: String, val color: Color) {
    GPT5("GPT-5", Color(0xFF10A37F)),
    CLAUDE("Claude", Color(0xFFCC785C)),
    GEMINI("Gemini", Color(0xFF4285F4)),
    GROK("Grok", Color(0xFF1DA1F2))
}

// ViewModel
class FlexViewModel : ViewModel() {
    private val _queries = mutableStateListOf<Query>()
    val queries: List<Query> = _queries
    
    private val _currentQuery = mutableStateOf<Query?>(null)
    val currentQuery: State<Query?> = _currentQuery
    
    private val _isProcessing = mutableStateOf(false)
    val isProcessing: State<Boolean> = _isProcessing
    
    fun submitQuery(question: String) {
        val query = Query(
            id = "qry_${System.currentTimeMillis()}",
            question = question,
            status = QueryStatus.PROCESSING,
            timestamp = System.currentTimeMillis()
        )
        _queries.add(0, query)
        _currentQuery.value = query
        _isProcessing.value = true
        
        // Simulate API call
        simulateQueryProcessing(query)
    }
    
    private fun simulateQueryProcessing(query: Query) {
        // This would be replaced with actual API call
        kotlinx.coroutines.GlobalScope.launch {
            delay(3000) // Simulate processing
            
            val completedQuery = query.copy(
                status = QueryStatus.COMPLETED,
                consensus = Consensus(
                    summary = "Le changement climatique a un impact économique significatif. Les IA convergent sur la nécessité d'une transition rapide vers les énergies renouvelables.",
                    confidence = 0.87f,
                    convergencePoints = listOf(
                        "Augmentation des coûts d'assurance",
                        "Migration économique vers énergies vertes",
                        "Risques pour le secteur agricole"
                    ),
                    divergencePoints = listOf(
                        "Vitesse de la transition énergétique",
                        "Impact sur l'emploi à court terme"
                    )
                ),
                aiResponses = listOf(
                    AIResponse(
                        model = AIModel.GPT5,
                        response = "Le changement climatique représente un risque systémique majeur pour l'économie mondiale...",
                        confidence = 0.92f,
                        reasoning = listOf("Données historiques", "Modèles économiques")
                    ),
                    AIResponse(
                        model = AIModel.CLAUDE,
                        response = "L'impact économique est multidimensionnel et nécessite une approche nuancée...",
                        confidence = 0.85f,
                        reasoning = listOf("Analyse comparative", "Perspectives sectorielles")
                    ),
                    AIResponse(
                        model = AIModel.GEMINI,
                        response = "Les données suggèrent une accélération des coûts liés aux catastrophes naturelles...",
                        confidence = 0.88f,
                        reasoning = listOf("Big data", "Tendances actuelles")
                    ),
                    AIResponse(
                        model = AIModel.GROK,
                        response = "Le marché réagit déjà à ces changements avec une réallocation massive des capitaux...",
                        confidence = 0.84f,
                        reasoning = listOf("Données marché", "Investissements ESG")
                    )
                )
            )
            
            val index = _queries.indexOfFirst { it.id == query.id }
            if (index != -1) {
                _queries[index] = completedQuery
                _currentQuery.value = completedQuery
            }
            _isProcessing.value = false
        }
    }
    
    fun selectQuery(query: Query) {
        _currentQuery.value = query
    }
}

// Main Activity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlexApp()
        }
    }
}

@Composable
fun FlexApp() {
    val viewModel: FlexViewModel = viewModel()
    var selectedTab by remember { mutableStateOf(0) }
    
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = FlexTheme.Primary,
            secondary = FlexTheme.Secondary,
            background = FlexTheme.DarkBg,
            surface = FlexTheme.CardBg
        )
    ) {
        Scaffold(
            containerColor = FlexTheme.DarkBg,
            topBar = { FlexTopBar() },
            bottomBar = { 
                FlexBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (selectedTab) {
                    0 -> HomeScreen(viewModel)
                    1 -> QueryHistoryScreen(viewModel)
                    2 -> SettingsScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlexTopBar() {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Logo
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(FlexTheme.GradientBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "F",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column {
                    Text(
                        "F.L.E.X.",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlexTheme.TextPrimary
                    )
                    Text(
                        "Intelligence Collective",
                        fontSize = 10.sp,
                        color = FlexTheme.TextSecondary
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = { /* Notifications */ }) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = FlexTheme.TextPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = FlexTheme.CardBg
        )
    )
}

@Composable
fun FlexBottomBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = FlexTheme.CardBg,
        contentColor = FlexTheme.TextPrimary
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = { Icon(Icons.Default.Home, "Home") },
            label = { Text("Accueil") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = FlexTheme.Primary,
                selectedTextColor = FlexTheme.Primary,
                unselectedIconColor = FlexTheme.TextSecondary,
                unselectedTextColor = FlexTheme.TextSecondary,
                indicatorColor = FlexTheme.Primary.copy(alpha = 0.2f)
            )
        )
        
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = { Icon(Icons.Default.History, "Historique") },
            label = { Text("Historique") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = FlexTheme.Primary,
                selectedTextColor = FlexTheme.Primary,
                unselectedIconColor = FlexTheme.TextSecondary,
                unselectedTextColor = FlexTheme.TextSecondary,
                indicatorColor = FlexTheme.Primary.copy(alpha = 0.2f)
            )
        )
        
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = { Icon(Icons.Default.Settings, "Paramètres") },
            label = { Text("Paramètres") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = FlexTheme.Primary,
                selectedTextColor = FlexTheme.Primary,
                unselectedIconColor = FlexTheme.TextSecondary,
                unselectedTextColor = FlexTheme.TextSecondary,
                indicatorColor = FlexTheme.Primary.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
fun HomeScreen(viewModel: FlexViewModel) {
    var queryText by remember { mutableStateOf("") }
    val currentQuery by viewModel.currentQuery
    val isProcessing by viewModel.isProcessing
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FlexTheme.DarkBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Requêtes",
                value = "10/100",
                icon = Icons.Default.QuestionAnswer
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Consensus",
                value = "87%",
                icon = Icons.Default.CheckCircle
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Query Input Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = FlexTheme.CardBg
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Posez votre question",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlexTheme.TextPrimary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { 
                        Text(
                            "Ex: Quel est l'impact du changement climatique?",
                            color = FlexTheme.TextSecondary
                        )
                    },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlexTheme.Primary,
                        unfocusedBorderColor = FlexTheme.TextSecondary.copy(alpha = 0.3f),
                        focusedTextColor = FlexTheme.TextPrimary,
                        unfocusedTextColor = FlexTheme.TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        if (queryText.isNotBlank()) {
                            viewModel.submitQuery(queryText)
                            queryText = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FlexTheme.Primary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessing && queryText.isNotBlank()
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Traitement en cours...")
                    } else {
                        Icon(Icons.Default.Send, "Envoyer")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lancer le débat Multi-IA")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Current Query Result
        currentQuery?.let { query ->
            when (query.status) {
                QueryStatus.PROCESSING -> ProcessingView()
                QueryStatus.COMPLETED -> QueryResultView(query)
                QueryStatus.FAILED -> ErrorView()
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = FlexTheme.CardBg
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = FlexTheme.Primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = FlexTheme.TextPrimary
            )
            Text(
                title,
                fontSize = 12.sp,
                color = FlexTheme.TextSecondary
            )
        }
    }
}

@Composable
fun ProcessingView() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = FlexTheme.CardBg
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated AI Network
            AINetworkAnimation()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Débat Multi-IA en cours...",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = FlexTheme.TextPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "GPT-5, Claude, Gemini et Grok analysent votre question",
                fontSize = 14.sp,
                color = FlexTheme.TextSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = FlexTheme.Primary,
                trackColor = FlexTheme.TextSecondary.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun AINetworkAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "network")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Center node
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(FlexTheme.GradientBrush)
                .rotate(rotation),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "F",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Orbiting AI nodes
        AIModel.values().forEachIndexed { index, model ->
            val angle = (index * 90f) + rotation
            val radius = 60.dp.value
            val x = radius * kotlin.math.cos(Math.toRadians(angle.toDouble()))
            val y = radius * kotlin.math.sin(Math.toRadians(angle.toDouble()))
            
            Box(
                modifier = Modifier
                    .offset(x = x.dp, y = y.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(model.color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    model.displayName.first().toString(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun QueryResultView(query: Query) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Consensus Card
        query.consensus?.let { consensus ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = FlexTheme.CardBg
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🎯 Consensus",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = FlexTheme.TextPrimary
                        )
                        
                        // Confidence Badge
                        Surface(
                            color = FlexTheme.Success.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                "${(consensus.confidence * 100).toInt()}% confiance",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = FlexTheme.Success
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        consensus.summary,
                        fontSize = 15.sp,
                        color = FlexTheme.TextPrimary,
                        lineHeight = 22.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Convergence Points
                    Text(
                        "✅ Points de convergence",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlexTheme.Success
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    consensus.convergencePoints.forEach { point ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text("• ", color = FlexTheme.Success)
                            Text(
                                point,
                                fontSize = 14.sp,
                                color = FlexTheme.TextSecondary
                            )
                        }
                    }
                }
            }
        }
        
        // AI Responses
        Text(
            "Réponses individuelles des IA",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = FlexTheme.TextPrimary
        )
        
        query.aiResponses.forEach { aiResponse ->
            AIResponseCard(aiResponse)
        }
    }
}

@Composable
fun AIResponseCard(aiResponse: AIResponse) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = FlexTheme.CardBg
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(aiResponse.model.color)
                    )
                    
                    Text(
                        aiResponse.model.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlexTheme.TextPrimary
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "${(aiResponse.confidence * 100).toInt()}%",
                        fontSize = 14.sp,
                        color = FlexTheme.TextSecondary
                    )
                    
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = FlexTheme.TextSecondary
                    )
                }
            }
            
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        aiResponse.response,
                        fontSize = 14.sp,
                        color = FlexTheme.TextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorView() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = FlexTheme.CardBg
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = "Error",
                tint = Color.Red,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Erreur lors du traitement",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = FlexTheme.TextPrimary
            )
        }
    }
}

@Composable
fun QueryHistoryScreen(viewModel: FlexViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(FlexTheme.DarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Historique des requêtes",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = FlexTheme.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        items(viewModel.queries) { query ->
            HistoryQueryCard(query) {
                viewModel.selectQuery(query)
            }
        }
    }
}

@Composable
fun HistoryQueryCard(query: Query, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = FlexTheme.CardBg
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    query.question,
                    fontSize = 15.sp,
                    color = FlexTheme.TextPrimary,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE)
                        .format(java.util.Date(query.timestamp)),
                    fontSize = 12.sp,
                    color = FlexTheme.TextSecondary
                )
            }
            
            when (query.status) {
                QueryStatus.COMPLETED -> Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = FlexTheme.Success
                )
                QueryStatus.PROCESSING -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = FlexTheme.Primary,
                    strokeWidth = 2.dp
                )
                QueryStatus.FAILED -> Icon(
                    Icons.Default.Error,
                    contentDescription = "Failed",
                    tint = Color.Red
                )
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FlexTheme.DarkBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Paramètres",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = FlexTheme.TextPrimary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        SettingsSection(title = "Compte") {
            SettingsItem(
                icon = Icons.Default.Person,
                title = "Profil",
                subtitle = "Gérer votre compte"
            )
            SettingsItem(
                icon = Icons.Default.VpnKey,
                title = "API Key",
                subtitle = "Gérer vos clés d'accès"
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        SettingsSection(title = "Préférences") {
            SettingsItem(
                icon = Icons.Default.Language,
                title = "Langue",
                subtitle = "Français"
            )
            SettingsItem(
                icon = Icons.Default.DarkMode,
                title = "Thème",
                subtitle = "Sombre"
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        SettingsSection(title = "À propos") {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Version",
                subtitle = "1.2.0"
            )
            SettingsItem(
                icon = Icons.Default.Description,
                title = "Documentation",
                subtitle = "Guide d'utilisation"
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Logout Button
        Button(
            onClick = { /* Logout */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red.copy(alpha = 0.2f),
                contentColor = Color.Red
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Logout, "Déconnexion")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Se déconnecter")
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = FlexTheme.Primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = FlexTheme.CardBg
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = FlexTheme.Primary
            )
            
            Column {
                Text(
                    title,
                    fontSize = 16.sp,
                    color = FlexTheme.TextPrimary
                )
                Text(
                    subtitle,
                    fontSize = 14.sp,
                    color = FlexTheme.TextSecondary
                )
            }
        }
        
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "Navigate",
            tint = FlexTheme.TextSecondary
        )
    }
}

// API Service (Network Layer)
object FlexApiService {
    private const val BASE_URL = "https://api.flex.ai/v1"
    
    suspend fun submitQuery(
        apiKey: String,
        query: String,
        models: List<String> = listOf("gpt5", "claude", "gemini", "grok"),
        debateRounds: Int = 2
    ): ApiResponse<QueryResponse> {
        // This would use Retrofit or Ktor for actual HTTP calls
        return try {
            // Simulated API call
            delay(2000)
            ApiResponse.Success(
                QueryResponse(
                    queryId = "qry_${System.currentTimeMillis()}",
                    status = "processing",
                    estimatedTime = 15
                )
            )
        } catch (e: Exception) {
            ApiResponse.Error(e.message ?: "Unknown error")
        }
    }
    
    suspend fun getQueryResults(
        apiKey: String,
        queryId: String
    ): ApiResponse<QueryResultResponse> {
        return try {
            delay(1000)
            ApiResponse.Success(
                QueryResultResponse(
                    queryId = queryId,
                    status = "completed",
                    consensus = ConsensusResponse(
                        summary = "Le changement climatique a un impact économique significatif...",
                        confidence = 0.87f,
                        convergencePoints = listOf(
                            "Augmentation des coûts d'assurance",
                            "Migration économique vers énergies vertes"
                        ),
                        divergencePoints = listOf(
                            "Vitesse de la transition énergétique"
                        )
                    ),
                    individualResponses = mapOf(
                        "gpt5" to AIResponseData(
                            response = "Le changement climatique représente...",
                            confidence = 0.92f
                        ),
                        "claude" to AIResponseData(
                            response = "L'impact économique est multidimensionnel...",
                            confidence = 0.85f
                        )
                    )
                )
            )
        } catch (e: Exception) {
            ApiResponse.Error(e.message ?: "Unknown error")
        }
    }
}

// API Response Models
sealed class ApiResponse<out T> {
    data class Success<T>(val data: T) : ApiResponse<T>()
    data class Error(val message: String) : ApiResponse<Nothing>()
}

data class QueryResponse(
    val queryId: String,
    val status: String,
    val estimatedTime: Int
)

data class QueryResultResponse(
    val queryId: String,
    val status: String,
    val consensus: ConsensusResponse,
    val individualResponses: Map<String, AIResponseData>
)

data class ConsensusResponse(
    val summary: String,
    val confidence: Float,
    val convergencePoints: List<String>,
    val divergencePoints: List<String>
)

data class AIResponseData(
    val response: String,
    val confidence: Float
)

// Repository Pattern
class FlexRepository {
    private val apiKey = "your_api_key_here" // Should be stored securely
    
    suspend fun submitQuery(question: String): Result<QueryResponse> {
        return when (val response = FlexApiService.submitQuery(apiKey, question)) {
            is ApiResponse.Success -> Result.success(response.data)
            is ApiResponse.Error -> Result.failure(Exception(response.message))
        }
    }
    
    suspend fun getResults(queryId: String): Result<QueryResultResponse> {
        return when (val response = FlexApiService.getQueryResults(apiKey, queryId)) {
            is ApiResponse.Success -> Result.success(response.data)
            is ApiResponse.Error -> Result.failure(Exception(response.message))
        }
    }
}

// Local Database (Room) - For offline support
/*
@Entity(tableName = "queries")
data class QueryEntity(
    @PrimaryKey val id: String,
    val question: String,
    val status: String,
    val timestamp: Long,
    val consensusSummary: String?,
    val confidence: Float?,
    val aiResponses: String? // JSON string
)

@Dao
interface QueryDao {
    @Query("SELECT * FROM queries ORDER BY timestamp DESC")
    fun getAllQueries(): Flow<List<QueryEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuery(query: QueryEntity)
    
    @Query("SELECT * FROM queries WHERE id = :queryId")
    suspend fun getQueryById(queryId: String): QueryEntity?
    
    @Query("DELETE FROM queries WHERE id = :queryId")
    suspend fun deleteQuery(queryId: String)
}

@Database(entities = [QueryEntity::class], version = 1)
abstract class FlexDatabase : RoomDatabase() {
    abstract fun queryDao(): QueryDao
    
    companion object {
        @Volatile
        private var INSTANCE: FlexDatabase? = null
        
        fun getDatabase(context: Context): FlexDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FlexDatabase::class.java,
                    "flex_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
*/

// Notification Manager
object FlexNotificationManager {
    fun showQueryCompleteNotification(
        context: android.content.Context,
        queryId: String,
        summary: String
    ) {
        // This would create and show a notification using NotificationCompat
        val notificationId = queryId.hashCode()
        
        // Implementation would include:
        // - NotificationCompat.Builder
        // - PendingIntent to open app
        // - Custom notification layout
        // - Sound/vibration
    }
    
    fun createNotificationChannel(context: android.content.Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channelId = "flex_queries"
            val channelName = "F.L.E.X. Queries"
            val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
            
            val channel = android.app.NotificationChannel(
                channelId,
                channelName,
                importance
            ).apply {
                description = "Notifications pour les requêtes F.L.E.X."
            }
            
            val notificationManager = context.getSystemService(
                android.content.Context.NOTIFICATION_SERVICE
            ) as android.app.NotificationManager
            
            notificationManager.createNotificationChannel(channel)
        }
    }
}

// Dependency Injection (Manual or with Hilt)
object FlexDependencies {
    private var repository: FlexRepository? = null
    
    fun provideRepository(): FlexRepository {
        return repository ?: FlexRepository().also { repository = it }
    }
}

// Application Class
class FlexApplication : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize notification channel
        FlexNotificationManager.createNotificationChannel(this)
        
        // Initialize analytics, crash reporting, etc.
        // Firebase, Crashlytics, etc.
    }
}

// Extension Functions
fun Query.toEntity(): String {
    // Convert Query to JSON for Room storage
    return """
        {
            "id": "$id",
            "question": "$question",
            "status": "${status.name}",
            "timestamp": $timestamp
        }
    """.trimIndent()
}

// Utility Functions
object DateUtils {
    fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "À l'instant"
            diff < 3600_000 -> "${diff / 60_000} min"
            diff < 86400_000 -> "${diff / 3600_000}h"
            else -> java.text.SimpleDateFormat(
                "dd/MM/yyyy",
                java.util.Locale.FRANCE
            ).format(java.util.Date(timestamp))
        }
    }
}

object ValidationUtils {
    fun isValidQuery(query: String): Boolean {
        return query.isNotBlank() && query.length >= 10
    }
    
    fun isValidApiKey(apiKey: String): Boolean {
        return apiKey.matches(Regex("^flex_[a-zA-Z0-9]{32}$"))
    }
}

// Shared Preferences Manager
class PreferencesManager(private val context: android.content.Context) {
    private val prefs = context.getSharedPreferences("flex_prefs", android.content.Context.MODE_PRIVATE)
    
    var apiKey: String?
        get() = prefs.getString("api_key", null)
        set(value) = prefs.edit().putString("api_key", value).apply()
    
    var userId: String?
        get() = prefs.getString("user_id", null)
        set(value) = prefs.edit().putString("user_id", value).apply()
    
    var theme: String
        get() = prefs.getString("theme", "dark") ?: "dark"
        set(value) = prefs.edit().putString("theme", value).apply()
    
    var language: String
        get() = prefs.getString("language", "fr") ?: "fr"
        set(value) = prefs.edit().putString("language", value).apply()
    
    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications_enabled", true)
        set(value) = prefs.edit().putBoolean("notifications_enabled", value).apply()
    
    fun clear() {
        prefs.edit().clear().apply()
    }
}

// WebSocket Manager for real-time updates
class WebSocketManager {
    private var webSocket: okhttp3.WebSocket? = null
    private val client = okhttp3.OkHttpClient()
    
    fun connect(queryId: String, onMessage: (String) -> Unit) {
        val request = okhttp3.Request.Builder()
            .url("wss://api.flex.ai/v1/ws/$queryId")
            .build()
        
        webSocket = client.newWebSocket(request, object : okhttp3.WebSocketListener() {
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                onMessage(text)
            }
            
            override fun onFailure(
                webSocket: okhttp3.WebSocket,
                t: Throwable,
                response: okhttp3.Response?
            ) {
                // Handle connection failure
            }
        })
    }
    
    fun disconnect() {
        webSocket?.close(1000, "Client closing")
        webSocket = null
    }
}

// Analytics Manager
object AnalyticsManager {
    fun logQuerySubmitted(query: String, models: List<String>) {
        // Log to Firebase Analytics, Mixpanel, etc.
        // trackEvent("query_submitted", mapOf(
        //     "query_length" to query.length,
        //     "models_count" to models.size
        // ))
    }
    
    fun logQueryCompleted(queryId: String, confidence: Float, duration: Long) {
        // trackEvent("query_completed", mapOf(
        //     "query_id" to queryId,
        //     "confidence" to confidence,
        //     "duration_seconds" to duration / 1000
        // ))
    }
    
    fun logScreenView(screenName: String) {
        // trackScreenView(screenName)
    }
}

// Error Handler
object ErrorHandler {
    fun handleError(error: Throwable): String {
        return when (error) {
            is java.net.UnknownHostException -> "Pas de connexion Internet"
            is java.net.SocketTimeoutException -> "Délai d'attente dépassé"
            is javax.net.ssl.SSLException -> "Erreur de sécurité"
            else -> error.message ?: "Une erreur est survenue"
        }
    }
}

// Constants
object FlexConstants {
    const val API_BASE_URL = "https://api.flex.ai/v1"
    const val WS_BASE_URL = "wss://api.flex.ai/v1/ws"
    const val TIMEOUT_SECONDS = 30L
    const val MAX_QUERY_LENGTH = 1000
    const val MIN_QUERY_LENGTH = 10
    
    object Plans {
        const val FREE_DAILY_LIMIT = 10
        const val PRO_DAILY_LIMIT = 100
        const val RESEARCHER_DAILY_LIMIT = 500
    }
    
    object NotificationChannels {
        const val QUERIES = "flex_queries"
        const val UPDATES = "flex_updates"
        const val ALERTS = "flex_alerts"
    }
}

// Security Utils
object SecurityUtils {
    fun encryptApiKey(apiKey: String): String {
        // Use Android Keystore for secure storage
        // This is a simplified version
        return android.util.Base64.encodeToString(
            apiKey.toByteArray(),
            android.util.Base64.DEFAULT
        )
    }
    
    fun decryptApiKey(encryptedKey: String): String {
        return String(
            android.util.Base64.decode(encryptedKey, android.util.Base64.DEFAULT)
        )
    }
}

// Logging Utility
object Logger {
    private const val TAG = "FLEX"
    
    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(TAG, message)
        }
    }
    
    fun e(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            android.util.Log.e(TAG, message, throwable)
        }
    }
    
    fun i(message: String) {
        if (BuildConfig.DEBUG) {
            android.util.Log.i(TAG, message)
        }
    }
}

// Build Config Placeholder
object BuildConfig {
    const val DEBUG = true
    const val VERSION_NAME = "1.2.0"
    const val VERSION_CODE = 12
    const val APPLICATION_ID = "com.flex.ai.app"
}