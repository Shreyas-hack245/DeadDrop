package com.deaddrop.feature.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deaddrop.core.theme.*
import com.deaddrop.data.model.Treasure

@Composable fun CollectionScreen(items:List<Treasure>,padding:PaddingValues,onOpen:(Treasure)->Unit,onMarket:()->Unit) {
    Column(Modifier.fillMaxSize().padding(bottom=padding.calculateBottomPadding())) {
        ScreenHeader("Your collection","THE ARCHIVE",trailing={Icon(Icons.Default.Tune,null,tint=Muted)})
        Row(Modifier.padding(horizontal=22.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF28214A),Panel))).padding(18.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("${items.size.toString().padStart(2,'0')} / 12",color=TextPrimary,fontSize=26.sp,fontWeight=FontWeight.Bold);Text("ARTIFACTS FOUND",color=Muted,fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.5.sp)};Icon(Icons.Default.AutoAwesome,null,tint=Gold,modifier=Modifier.size(31.dp))}
        Spacer(Modifier.height(16.dp));Row(Modifier.padding(horizontal=22.dp),verticalAlignment=Alignment.CenterVertically){Text("RECENTLY FOUND",color=Muted,fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.5.sp,modifier=Modifier.weight(1f));Text("SORT  ↓",color=Violet,fontSize=9.sp,fontWeight=FontWeight.Bold)}
        if(items.isEmpty()) Box(Modifier.fillMaxSize().weight(1f),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Text("✧",fontSize=55.sp,color=Violet);Spacer(Modifier.height(12.dp));Text("Your story starts out there",color=TextPrimary,fontSize=18.sp,fontWeight=FontWeight.SemiBold);Text("Find a signal to discover your first artifact.",color=Muted,fontSize=12.sp);Spacer(Modifier.height(18.dp));TextButton(onClick=onMarket){Text("Explore the market",color=Cyan)}}}
        else LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){items(items){t->ArtifactCard(t,Modifier.fillMaxWidth(),onClick={onOpen(t)})}}
    }
}
@Composable private fun ArtifactCard(t:Treasure,modifier:Modifier,onClick:()->Unit) {
    Card(modifier.clickable(onClick=onClick),shape=RoundedCornerShape(23.dp),colors=CardDefaults.cardColors(containerColor=Panel)) { Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(84.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Color(0xFF353060),Color(0xFF15243A)))),contentAlignment=Alignment.Center){Text(t.emoji,fontSize=39.sp)}
        Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Chip(t.rarity,Gold.copy(alpha=.12f),Gold);Spacer(Modifier.height(8.dp));Text(t.rewardName,color=TextPrimary,fontSize=14.sp,fontWeight=FontWeight.SemiBold);Text("Found today · Demo collectible",color=Muted,fontSize=10.sp)};Icon(Icons.Default.ChevronRight,null,tint=Muted)
    } }
}

@Composable fun MarketScreen(padding:PaddingValues,onWallet:()->Unit,onToast:(String)->Unit) {
    Column(Modifier.fillMaxSize().padding(bottom=padding.calculateBottomPadding())) {
        ScreenHeader("The exchange","COLLECTOR MARKET",trailing={IconButton(onClick=onWallet){Icon(Icons.Default.AccountBalanceWallet,null,tint=Cyan)}})
        Row(Modifier.padding(horizontal=22.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF13323B),Panel))).padding(18.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("TESTNET MARKET",color=Cyan,fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.5.sp);Spacer(Modifier.height(6.dp));Text("Collect stories,\nnot promises.",color=TextPrimary,fontSize=19.sp,fontWeight=FontWeight.SemiBold,lineHeight=24.sp)};Text("◇",color=Cyan,fontSize=42.sp)}
        Row(Modifier.padding(horizontal=22.dp,vertical=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Chip("FEATURED",Violet.copy(alpha=.2f));Chip("ARTIFACTS",PanelLight,Muted);Chip("ACTIVITY",PanelLight,Muted)}
        val listings=listOf("The Moonwell Sigil" to "EPIC", "Tideglass Archive" to "LEGENDARY", "Foxglove Beacon" to "RARE")
        LazyColumn(contentPadding=PaddingValues(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){itemsIndexed(listings){idx,item->
            Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Panel)){Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Violet.copy(alpha=.25f),Cyan.copy(alpha=.1f)))),contentAlignment=Alignment.Center){Text(listOf("🌙","💎","🦊")[idx],fontSize=34.sp)};Spacer(Modifier.width(13.dp));Column(Modifier.weight(1f)){Text(item.first,color=TextPrimary,fontSize=13.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(5.dp));Chip(item.second,Gold.copy(alpha=.12f),Gold);Spacer(Modifier.height(6.dp));Text("Creator listing · Polygon Amoy",color=Muted,fontSize=9.sp)};Text("···",color=Muted,fontSize=22.sp,modifier=Modifier.clickable{onToast("Listing actions are unavailable in demo mode")}.padding(6.dp))} }
        }}
        Text("Listings shown are sample data. No assets or prices are for sale.",color=Muted,fontSize=10.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(14.dp))
    }
}
