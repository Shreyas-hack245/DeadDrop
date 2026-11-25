package com.deaddrop.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.deaddrop.core.theme.*
import com.deaddrop.data.model.Treasure

private enum class Page { MAP, COLLECTION, MARKET, PROFILE, DETAIL, HUNT, SCAN, PUZZLE, REWARD, WALLET, HISTORY, AUTH }

@Composable
fun DeadDropApp() {
    val navController=rememberNavController()
    val viewModel:DeadDropViewModel=androidx.lifecycle.viewmodel.compose.viewModel()
    val treasures by viewModel.treasures.collectAsState()
    val entry by navController.currentBackStackEntryAsState()
    val route=entry?.destination?.route ?: "onboarding"
    var selected by remember { mutableStateOf(treasures.first()) }
    var answer by remember { mutableStateOf("") }
    val isDemo=true
    var toast by remember { mutableStateOf<String?>(null) }
    val snackbar=remember { SnackbarHostState() }
    LaunchedEffect(toast){toast?.let{snackbar.showSnackbar(it);toast=null}}
    fun open(route:String){navController.navigate(route){launchSingleTop=true}}

    Scaffold(containerColor=Ink,snackbarHost={SnackbarHost(snackbar,snackbar={data->Snackbar(data,containerColor=TextPrimary,contentColor=Panel)})} ,bottomBar={
        val selectedTab=when(route){"collection"->Page.COLLECTION;"market"->Page.MARKET;"profile"->Page.PROFILE;else->Page.MAP}
        if(route in listOf("map","collection","market","profile")) PremiumBottomBar(selectedTab){target->
            val dest=when(target){Page.MAP->"map";Page.COLLECTION->"collection";Page.MARKET->"market";else->"profile"}
            navController.navigate(dest){popUpTo("map"){saveState=true};launchSingleTop=true;restoreState=true}
        }
    }){padding->
        androidx.navigation.compose.NavHost(navController=navController,startDestination="onboarding",modifier=Modifier.fillMaxSize()){
            composable("onboarding"){OnboardingScreen(onStart={open("map")},onSignIn={open("auth")})}
            composable("map"){HomeScreen(treasures,padding,onSelect={selected=it;open("detail")},onProfile={open("profile")})}
            composable("collection"){CollectionScreen(treasures.filter{it.unlocked},padding,onOpen={selected=it;open("artifact")},onMarket={open("market")})}
            composable("artifact"){ArtifactDetailScreen(selected,onBack={navController.popBackStack()},onMarket={open("market")})}
            composable("market"){MarketScreen(padding,onWallet={open("wallet")},onToast={toast=it})}
            composable("profile"){ProfileScreen(padding,isDemo,onWallet={open("wallet")},onHistory={open("history")},onAuth={open("auth")},onMode={toast="Live services are not configured; demo mode stays on"})}
            composable("detail"){DetailScreen(selected,onBack={navController.popBackStack()},onStart={open("hunt")})}
            composable("hunt"){HuntScreen(selected,onBack={navController.popBackStack()},onScan={open("scan")})}
            composable("scan"){ScanScreen(selected,onBack={navController.popBackStack()},onPuzzle={open("puzzle")})}
            composable("puzzle"){PuzzleScreen(selected,answer,{answer=it},onBack={navController.popBackStack()},onWin={viewModel.unlock(selected.id);open("reward")})}
            composable("reward"){RewardScreen(selected,onCollection={open("collection")},onMap={navController.navigate("map"){popUpTo("map"){inclusive=false};launchSingleTop=true}})}
            composable("wallet"){WalletScreen(isDemo,onBack={navController.popBackStack()},onToast={toast=it})}
            composable("history"){HistoryScreen(onBack={navController.popBackStack()})}
            composable("auth"){AuthScreen(onBack={navController.popBackStack()},onContinue={navController.popBackStack();toast="Continuing with the local demo profile"})}
        }
    }
}

@Composable private fun HomeScreen(treasures:List<Treasure>,padding:PaddingValues,onSelect:(Treasure)->Unit,onProfile:()->Unit){
    var query by remember{mutableStateOf("")}
    val visible=treasures.filter{it.name.contains(query,true)||it.category.contains(query,true)}
    Column(Modifier.fillMaxSize().background(Ink).padding(bottom=padding.calculateBottomPadding()).verticalScroll(rememberScrollState())){
        Row(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){Text("Explore",style=MaterialTheme.typography.titleLarge,color=TextPrimary);Text("SAN FRANCISCO  ·  DEMO MAP",color=Muted,fontSize=9.sp,fontWeight=FontWeight.SemiBold,letterSpacing=1.sp)}
            IconButton(onClick=onProfile,modifier=Modifier.size(42.dp).clip(CircleShape).background(Panel).border(1.dp,Border,CircleShape)){Icon(Icons.Default.PersonOutline,"Profile",tint=Forest)}
        }
        Row(Modifier.padding(start=24.dp,end=24.dp,bottom=12.dp),verticalAlignment=Alignment.Bottom){Text("A little farther.\nA little wonder.",style=MaterialTheme.typography.headlineMedium,color=TextPrimary,modifier=Modifier.weight(1f));Text("03\nNEARBY",color=Muted,fontSize=9.sp,lineHeight=15.sp,fontWeight=FontWeight.SemiBold,textAlign=androidx.compose.ui.text.style.TextAlign.End)}
        Row(Modifier.padding(horizontal=24.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(13.dp)).background(Panel).border(1.dp,Border,RoundedCornerShape(13.dp)).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Search,null,tint=Muted,modifier=Modifier.size(19.dp));Spacer(Modifier.width(10.dp));BasicTextField(query,{query=it},modifier=Modifier.weight(1f),singleLine=true,textStyle=MaterialTheme.typography.bodyMedium.copy(color=TextPrimary),decorationBox={inner->Box{if(query.isEmpty())Text("Search treasures",color=Muted,fontSize=13.sp);inner()}});if(query.isNotEmpty())IconButton(onClick={query=""},modifier=Modifier.size(30.dp)){Icon(Icons.Default.Close,"Clear search",tint=Muted,modifier=Modifier.size(17.dp))}}
        Spacer(Modifier.height(13.dp))
        Box(Modifier.fillMaxWidth().height(300.dp).padding(horizontal=16.dp).clip(RoundedCornerShape(20.dp)).border(1.dp,Border,RoundedCornerShape(20.dp))){
            TreasureMap(treasures,Modifier.fillMaxSize(),onSelect)
            Chip("DEMO MAP",Panel.copy(alpha=.92f),Cyan)
            Box(Modifier.align(Alignment.BottomStart).padding(13.dp).clip(RoundedCornerShape(10.dp)).background(Panel.copy(alpha=.94f)).padding(horizontal=11.dp,vertical=8.dp)){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(7.dp).background(Cyan,CircleShape));Spacer(Modifier.width(7.dp));Text("Sample locations · San Francisco",color=TextPrimary,fontSize=10.sp,fontWeight=FontWeight.Medium)}}
        }
        Row(Modifier.fillMaxWidth().padding(start=24.dp,end=24.dp,top=18.dp,bottom=9.dp),verticalAlignment=Alignment.CenterVertically){Text(if(query.isBlank())"Nearby discoveries" else "Search results",style=MaterialTheme.typography.titleMedium,color=TextPrimary,modifier=Modifier.weight(1f));Text("${visible.size.toString().padStart(2,'0')} PLACES",color=Muted,fontSize=9.sp,fontWeight=FontWeight.SemiBold,letterSpacing=.7.sp)}
        if(visible.isEmpty()) Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center){Text("No discoveries match that search.",color=Muted,fontSize=13.sp)}
        else LazyRow(contentPadding=PaddingValues(start=19.dp,end=19.dp,bottom=12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){
            items(visible,key={it.id}){treasure->DiscoveryCard(treasure,Modifier.width(244.dp),onClick={onSelect(treasure)})}
        }
    }
}

@Composable private fun TreasureMap(treasures:List<Treasure>,modifier:Modifier=Modifier,onSelect:(Treasure)->Unit){
    BoxWithConstraints(modifier.background(Color(0xFF101326))){
        Canvas(Modifier.fillMaxSize()){
            drawRect(Brush.verticalGradient(listOf(Color(0xFF18203C),Color(0xFF101326),Color(0xFF17142C))))
            val green=Color(0xFF202B3B)
            listOf(Triple(.14f,.2f,.22f),Triple(.82f,.24f,.2f),Triple(.17f,.77f,.25f),Triple(.86f,.76f,.22f)).forEach{(x,y,r)->drawCircle(green,size.minDimension*r,Offset(size.width*x,size.height*y))}
            val road=Color(0xFF34344C)
            for(i in 0..7){val x=size.width*i/7f;drawLine(road,Offset(x,-20f),Offset(x+55f,size.height+20f),5.dp.toPx())}
            for(i in 0..8){val y=size.height*i/8f;drawLine(road,Offset(-20f,y),Offset(size.width+20f,y-25f),4.dp.toPx())}
            val water=Color(0xFF304C6B)
            val path=androidx.compose.ui.graphics.Path().apply{moveTo(size.width*.02f,size.height*.72f);cubicTo(size.width*.25f,size.height*.51f,size.width*.46f,size.height*.85f,size.width*.72f,size.height*.56f);cubicTo(size.width*.83f,size.height*.43f,size.width*.9f,size.height*.53f,size.width*1.03f,size.height*.41f)}
            drawPath(path,water,style=Stroke(width=12.dp.toPx(),cap=androidx.compose.ui.graphics.StrokeCap.Round))
            val p=android.graphics.Paint().apply{color=android.graphics.Color.rgb(177,182,208);textSize=10.dp.toPx();letterSpacing=1.1f}
            drawContext.canvas.nativeCanvas.drawText("NORTH BEACH",size.width*.08f,size.height*.38f,p)
            drawContext.canvas.nativeCanvas.drawText("PACIFIC HEIGHTS",size.width*.57f,size.height*.27f,p)
            drawContext.canvas.nativeCanvas.drawText("THE EMBARCADERO",size.width*.27f,size.height*.88f,p)
        }
        val points=listOf(Triple(.32f,.30f,treasures.getOrNull(0)),Triple(.73f,.43f,treasures.getOrNull(1)),Triple(.49f,.68f,treasures.getOrNull(2)))
        points.forEach{(x,y,item)->if(item!=null){
            Box(Modifier.offset(maxWidth*x-21.dp,maxHeight*y-21.dp).size(42.dp).clip(CircleShape).background(Forest.copy(alpha=.18f)).border(1.dp,Violet.copy(alpha=.8f),CircleShape).clickable{onSelect(item)},contentAlignment=Alignment.Center){Box(Modifier.size(24.dp).clip(CircleShape).background(Violet),contentAlignment=Alignment.Center){Canvas(Modifier.size(12.dp)){drawCircle(Color.White);drawCircle(Violet,size.minDimension*.23f,Offset(size.width*.38f,size.height*.36f))}}}
        }}
        Box(Modifier.align(Alignment.Center).offset(9.dp,19.dp).size(15.dp).clip(CircleShape).background(Cyan).border(2.dp,Color.White,CircleShape))
    }
}

@Composable private fun DiscoveryCard(t:Treasure,modifier:Modifier=Modifier,onClick:()->Unit){
    Row(modifier.height(86.dp).clip(RoundedCornerShape(15.dp)).background(Panel).border(1.dp,Border,RoundedCornerShape(15.dp)).clickable(onClick=onClick).padding(9.dp),verticalAlignment=Alignment.CenterVertically){
        CollectibleArtwork(t.id,Modifier.size(66.dp));Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){
            Text(t.name,color=TextPrimary,fontSize=13.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp));Text(t.category.lowercase().replaceFirstChar{it.uppercase()},color=Muted,fontSize=10.sp)
            Spacer(Modifier.height(5.dp));Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Place,null,tint=Forest,modifier=Modifier.size(12.dp));Spacer(Modifier.width(3.dp));Text(t.distance,color=Forest,fontSize=10.sp,fontWeight=FontWeight.Medium)}
        }
        Icon(Icons.Default.ChevronRight,null,tint=Muted,modifier=Modifier.size(19.dp))
    }
}

@Composable private fun PremiumBottomBar(selected:Page,onSelect:(Page)->Unit){
    NavigationBar(containerColor=Panel,contentColor=Muted,tonalElevation=0.dp,modifier=Modifier.border(width=1.dp,color=Border)){
        listOf(Triple(Page.MAP,Icons.Default.Explore,"Explore"),Triple(Page.COLLECTION,Icons.Default.GridView,"Collection"),Triple(Page.MARKET,Icons.Default.Storefront,"Market"),Triple(Page.PROFILE,Icons.Default.PersonOutline,"Profile")).forEach{(page,icon,label)->
            NavigationBarItem(selected==page,onClick={onSelect(page)},icon={Icon(icon,null)},label={Text(label,fontSize=10.sp,fontWeight=FontWeight.Medium)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Forest,selectedTextColor=Forest,indicatorColor=SoftGreen,unselectedIconColor=Muted,unselectedTextColor=Muted))
        }
    }
}
