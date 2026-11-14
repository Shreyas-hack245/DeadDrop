package com.deaddrop.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.navigation.compose.composable
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deaddrop.core.theme.*
import com.deaddrop.data.model.Treasure
import com.deaddrop.data.repository.DemoTreasureRepository

private val repo = DemoTreasureRepository()
private enum class Page { MAP, COLLECTION, MARKET, PROFILE, DETAIL, HUNT, SCAN, PUZZLE, REWARD, WALLET, HISTORY, AUTH }

@Composable
fun DeadDropApp() {
    val navController = androidx.navigation.compose.rememberNavController()
    val viewModel: DeadDropViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val treasures by viewModel.treasures.collectAsState()
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "map"
    var selected by remember { mutableStateOf(treasures.first()) }
    var answer by remember { mutableStateOf("") }
    val isDemo = true
    var toast by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(toast) { toast?.let { snackbar.showSnackbar(it); toast = null } }
    fun open(route: String) { navController.navigate(route) { launchSingleTop = true } }

    Scaffold(containerColor = Ink, snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
        val selectedTab = when (route) { "collection" -> Page.COLLECTION; "market" -> Page.MARKET; "profile" -> Page.PROFILE; else -> Page.MAP }
        if (route in listOf("map", "collection", "market", "profile")) BottomBar(selectedTab) { target ->
            val targetRoute = when(target) { Page.MAP -> "map"; Page.COLLECTION -> "collection"; Page.MARKET -> "market"; else -> "profile" }
            navController.navigate(targetRoute) { popUpTo("map") { saveState = true }; launchSingleTop = true; restoreState = true }
        }
    }) { padding ->
        androidx.navigation.compose.NavHost(navController = navController, startDestination = "onboarding", modifier = Modifier.fillMaxSize()) {
            composable("onboarding") { OnboardingScreen(onStart = { open("map") }, onSignIn = { open("auth") }) }
            composable("map") { HomeScreen(treasures, padding, onSelect = { selected = it; open("detail") }, onProfile = { open("profile") }) }
            composable("collection") { CollectionScreen(treasures.filter { it.unlocked }, padding, onOpen = { selected = it; open("reward") }, onMarket = { open("market") }) }
            composable("market") { MarketScreen(padding, onWallet = { open("wallet") }, onToast = { toast = it }) }
            composable("profile") { ProfileScreen(padding, isDemo, onWallet = { open("wallet") }, onHistory = { open("history") }, onAuth = { open("auth") }, onMode = { toast = "Live services are not configured; demo mode stays on" }) }
            composable("detail") { DetailScreen(selected, onBack = { navController.popBackStack() }, onStart = { open("hunt") }) }
            composable("hunt") { HuntScreen(selected, onBack = { navController.popBackStack() }, onScan = { open("scan") }) }
            composable("scan") { ScanScreen(selected, onBack = { navController.popBackStack() }, onPuzzle = { open("puzzle") }) }
            composable("puzzle") { PuzzleScreen(selected, answer, { answer = it }, onBack = { navController.popBackStack() }, onWin = { viewModel.unlock(selected.id); open("reward") }) }
            composable("reward") { RewardScreen(selected, onCollection = { open("collection") }, onMap = { navController.navigate("map") { popUpTo("map") { inclusive = false }; launchSingleTop = true } }) }
            composable("wallet") { WalletScreen(isDemo, onBack = { navController.popBackStack() }, onToast = { toast = it }) }
            composable("history") { HistoryScreen(onBack = { navController.popBackStack() }) }
            composable("auth") { AuthScreen(onBack = { navController.popBackStack() }, onContinue = { navController.popBackStack(); toast = "Continuing with the local demo profile" }) }
        }
    }
}

@Composable
private fun HomeScreen(treasures: List<Treasure>, padding: PaddingValues, onSelect: (Treasure) -> Unit, onProfile: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("DEADDROP", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.6.sp); Text("Good evening, Alex", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold) }
            IconButton(onClick = onProfile, modifier = Modifier.size(44.dp).clip(CircleShape).background(Panel)) { Icon(Icons.Default.Person, null, tint = Violet) }
        }
        Row(Modifier.padding(horizontal = 22.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("SAN FRANCISCO", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.MyLocation, null, tint = Cyan, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(5.dp)); Text("DEMO MAP", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 14.dp, vertical = 12.dp).clip(RoundedCornerShape(28.dp))) {
            TreasureMap(treasures, Modifier.fillMaxSize(), onSelect)
            Column(Modifier.align(Alignment.TopStart).padding(15.dp)) { Chip("✦  3 SIGNALS NEARBY", Cyan.copy(alpha=.12f), Cyan) }
            Card(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp), shape=RoundedCornerShape(22.dp), colors=CardDefaults.cardColors(containerColor = Panel.copy(alpha=.96f))) {
                Row(Modifier.fillMaxWidth().clickable { treasures.firstOrNull()?.let(onSelect) }.padding(16.dp), verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Violet.copy(alpha=.16f)), contentAlignment=Alignment.Center) { Text("🌙", fontSize=21.sp) }
                    Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("THE MOONWELL CACHE", color=TextPrimary, fontSize=11.sp, fontWeight=FontWeight.Bold, letterSpacing=1.sp); Spacer(Modifier.height(4.dp)); Text("Epic relic  ·  0.8 km away", color=Muted, fontSize=12.sp) }
                    Icon(Icons.Default.ArrowForward, null, tint=Cyan)
                }
            }
        }
        Row(Modifier.padding(horizontal=22.dp, vertical=5.dp), verticalAlignment=Alignment.CenterVertically) { Text("Nearby signals", color=TextPrimary, fontSize=16.sp, fontWeight=FontWeight.SemiBold, modifier=Modifier.weight(1f)); Text("OPEN SIGNAL", color=Violet, fontSize=10.sp, fontWeight=FontWeight.Bold, modifier=Modifier.clickable { treasures.firstOrNull()?.let(onSelect) }) }
        Row(Modifier.fillMaxWidth().height(108.dp).padding(start=16.dp, end=16.dp, top=8.dp, bottom=12.dp)) {
            treasures.take(3).forEach { t -> SmallTreasureCard(t, Modifier.weight(1f).padding(horizontal=5.dp), onSelect) }
        }
    }
}

@Composable
private fun TreasureMap(treasures: List<Treasure>, modifier: Modifier = Modifier, onSelect: (Treasure) -> Unit) {
    BoxWithConstraints(modifier.background(Color(0xFF10192A))) {
        Canvas(Modifier.fillMaxSize()) {
            val w=size.width; val h=size.height
            drawRect(brush=Brush.linearGradient(listOf(Color(0xFF111C2D),Color(0xFF172438),Color(0xFF101A28)), Offset.Zero, Offset(w,h)))
            // soft park shapes and street grid
            val parks=listOf(Triple(.15f,.19f,.2f),Triple(.79f,.29f,.21f),Triple(.2f,.73f,.24f),Triple(.82f,.79f,.25f))
            parks.forEach { (x,y,r) -> drawCircle(Color(0xFF1C3B3A).copy(alpha=.55f), size.minDimension*r, Offset(w*x,h*y)) }
            val street=Color(0xFF637083).copy(alpha=.26f)
            for (i in 0..8) { val x=w*i/8f; drawLine(street,Offset(x,-30f),Offset(x+70f,h+30),4.dp.toPx()) }
            for (i in 0..10) { val y=h*i/10f; drawLine(street,Offset(-30f,y),Offset(w+30f,y-36f),3.dp.toPx()) }
            drawLine(Color(0xFF356B83).copy(alpha=.54f),Offset(w*.08f,h*.65f),Offset(w*.9f,h*.47f),14.dp.toPx())
            drawLine(Color(0xFF284F68).copy(alpha=.45f),Offset(w*.08f,h*.65f),Offset(w*.9f,h*.47f),7.dp.toPx())
            // faint map labels
            val p=android.graphics.Paint().apply { color=android.graphics.Color.argb(100,180,195,211); textSize=10.dp.toPx(); letterSpacing=2f }
            drawContext.canvas.nativeCanvas.drawText("NORTH BEACH",w*.1f,h*.36f,p)
            drawContext.canvas.nativeCanvas.drawText("PACIFIC HEIGHTS",w*.55f,h*.22f,p)
            drawContext.canvas.nativeCanvas.drawText("THE EMBARCADERO",w*.25f,h*.88f,p)
        }
        val points=listOf(Triple(.32f,.28f,treasures.getOrNull(0)),Triple(.73f,.43f,treasures.getOrNull(1)),Triple(.49f,.68f,treasures.getOrNull(2)))
        points.forEachIndexed { index, (x,y,t) -> if(t!=null) {
            val pulse by rememberInfiniteTransition(label="signal$index").animateFloat(0f,1f, infiniteRepeatable(tween(1900,delayMillis=index*300),RepeatMode.Restart),label="pulse")
            Box(Modifier.offset(maxWidth*x-22.dp,maxHeight*y-24.dp).size(50.dp).clickable { onSelect(t) },contentAlignment=Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) { drawCircle(Cyan.copy(alpha=.08f*(1-pulse)),size.minDimension*.5f,style=Stroke(2.dp.toPx())); drawCircle(Violet.copy(alpha=.12f*(1-pulse)),size.minDimension*(.34f+.1f*pulse),style=Stroke(1.dp.toPx())) }
                Box(Modifier.size(33.dp).border(1.dp,Cyan.copy(alpha=.8f),CircleShape).background(Panel,CircleShape),contentAlignment=Alignment.Center){Text(t.emoji,fontSize=16.sp)}
            }
        } }
        Box(Modifier.align(Alignment.Center).offset((-12).dp,20.dp).size(16.dp).border(2.dp,Color.White.copy(alpha=.7f),CircleShape).background(Cyan,CircleShape))
        Box(Modifier.align(Alignment.BottomStart).padding(16.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha=.32f)).padding(horizontal=9.dp,vertical=5.dp)){Text("MAP DATA · DEMO",color=Muted,fontSize=9.sp,letterSpacing=1.sp)}
    }
}

@Composable
private fun SmallTreasureCard(t: Treasure, modifier: Modifier, onClick: (Treasure)->Unit) {
    Card(modifier.fillMaxHeight().clickable { onClick(t) }, shape=RoundedCornerShape(17.dp), colors=CardDefaults.cardColors(containerColor=Panel)) {
        Row(Modifier.fillMaxSize().padding(10.dp), verticalAlignment=Alignment.CenterVertically) { Text(t.emoji,fontSize=21.sp); Spacer(Modifier.width(8.dp)); Column { Text(t.name.substringBefore(" "),color=TextPrimary,fontSize=11.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis); Text(t.distance,color=Muted,fontSize=10.sp) } }
    }
}

@Composable private fun BottomBar(selected: Page,onSelect:(Page)->Unit) {
    NavigationBar(containerColor=Color(0xFF0D1220),contentColor=Muted,tonalElevation=0.dp) {
        listOf(Triple(Page.MAP,Icons.Default.Explore,"Hunt"),Triple(Page.COLLECTION,Icons.Default.GridView,"Collection"),Triple(Page.MARKET,Icons.Default.Storefront,"Market"),Triple(Page.PROFILE,Icons.Default.Person,"Profile")).forEach { (page,icon,label) ->
            NavigationBarItem(selected==page,onClick={onSelect(page)},icon={Icon(icon,null)},label={Text(label,fontSize=10.sp)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Cyan,selectedTextColor=Cyan,indicatorColor=PanelLight,unselectedIconColor=Muted,unselectedTextColor=Muted))
        }
    }
}
