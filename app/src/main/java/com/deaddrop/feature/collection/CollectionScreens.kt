package com.deaddrop.feature.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deaddrop.core.theme.*
import com.deaddrop.data.model.Treasure

@Composable fun CollectionScreen(items:List<Treasure>,padding:PaddingValues,onOpen:(Treasure)->Unit,onMarket:()->Unit){
    var filter by remember{mutableStateOf("All")};var sortByRarity by remember{mutableStateOf(false)}
    val filtered=items.filter{filter=="All"||it.rarity==filter}.let{if(sortByRarity)it.sortedByDescending{item->rarityRank(item.rarity)}else it}
    Column(Modifier.fillMaxSize().background(Ink).padding(bottom=padding.calculateBottomPadding())){
        ScreenHeader("Your collection","THE FIELD ARCHIVE",trailing={Chip("${items.size} FOUND",SoftGreen,Forest)})
        Row(Modifier.padding(horizontal=24.dp).fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Panel).border(1.dp,Border,RoundedCornerShape(17.dp)).padding(17.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("${items.size.toString().padStart(2,'0')} / 12",style=MaterialTheme.typography.headlineMedium,color=TextPrimary);Spacer(Modifier.height(3.dp));Text("ARTIFACTS DISCOVERED",color=Muted,fontSize=9.sp,fontWeight=FontWeight.SemiBold,letterSpacing=.8.sp)};Icon(Icons.Default.AutoAwesome,null,tint=Gold,modifier=Modifier.size(25.dp))}
        Row(Modifier.fillMaxWidth().padding(start=24.dp,end=24.dp,top=16.dp,bottom=10.dp),verticalAlignment=Alignment.CenterVertically){Text("Your finds",style=MaterialTheme.typography.titleLarge,color=TextPrimary,modifier=Modifier.weight(1f));TextButton(onClick={sortByRarity=!sortByRarity}){Icon(Icons.Default.SwapVert,null,tint=Forest,modifier=Modifier.size(16.dp));Spacer(Modifier.width(4.dp));Text(if(sortByRarity)"Rarity" else "Recent",color=Forest,fontSize=11.sp)}}
        Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)){listOf("All","LEGENDARY","EPIC","RARE").forEach{value->FilterChip(selected=filter==value,onClick={filter=value},label={Text(value,fontSize=9.sp)},shape=RoundedCornerShape(30.dp),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=SoftGreen,selectedLabelColor=Forest,containerColor=Panel,labelColor=Muted))}}
        if(filtered.isEmpty())Box(Modifier.fillMaxSize().weight(1f),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.padding(30.dp)){Icon(Icons.Default.AutoAwesome,null,tint=Gold,modifier=Modifier.size(35.dp));Spacer(Modifier.height(13.dp));Text(if(items.isEmpty())"Your archive is ready" else "No finds in this filter",style=MaterialTheme.typography.titleLarge,color=TextPrimary);Spacer(Modifier.height(6.dp));Text(if(items.isEmpty())"Follow a nearby clue to bring home your first artifact." else "Try another rarity to see your collected pieces.",color=Muted,fontSize=12.sp,textAlign=TextAlign.Center);if(items.isEmpty()){Spacer(Modifier.height(12.dp));TextButton(onClick=onMarket){Text("Explore the preview market",color=Forest)}}}}
        else LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(horizontal=20.dp,vertical=14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){items(filtered,key={it.id}){t->ArtifactCard(t,Modifier.fillMaxWidth(),onClick={onOpen(t)})}}
    }
}

@Composable private fun ArtifactCard(t:Treasure,modifier:Modifier,onClick:()->Unit){
    Row(modifier.clip(RoundedCornerShape(17.dp)).background(Panel).border(1.dp,Border,RoundedCornerShape(17.dp)).clickable(onClick=onClick).padding(11.dp),verticalAlignment=Alignment.CenterVertically){
        CollectibleArtwork(t.id,Modifier.size(92.dp));Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){
            Chip(t.rarity,SoftGreen,Gold);Spacer(Modifier.height(8.dp));Text(t.rewardName,color=TextPrimary,fontSize=15.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Spacer(Modifier.height(3.dp));Text("Found in demo · off chain",color=Muted,fontSize=11.sp);Spacer(Modifier.height(7.dp));Text("View artifact",color=Forest,fontSize=11.sp,fontWeight=FontWeight.Medium)
        };Icon(Icons.Default.ChevronRight,null,tint=Muted)
    }
}

@Composable fun ArtifactDetailScreen(t:Treasure,onBack:()->Unit,onMarket:()->Unit){
    Column(Modifier.fillMaxSize().background(Ink)){
        ScreenHeader("Artifact details","YOUR COLLECTION",onBack)
        Column(Modifier.weight(1f).verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal=24.dp)){
            Box(Modifier.fillMaxWidth().height(330.dp).clip(RoundedCornerShape(22.dp))){CollectibleArtwork(t.id,Modifier.fillMaxSize(),dark=true);Chip("${t.rarity} · DEMO COLLECTIBLE",Color.Black.copy(alpha=.58f),Color(0xFFD8C6A2),Modifier.align(Alignment.TopStart).padding(15.dp))}
            Spacer(Modifier.height(22.dp));Text(t.rewardName,style=MaterialTheme.typography.headlineLarge,color=TextPrimary);Spacer(Modifier.height(8.dp));Text(t.category,color=Muted,fontSize=10.sp,fontWeight=FontWeight.SemiBold,letterSpacing=1.1.sp)
            Spacer(Modifier.height(17.dp));Text(t.rewardDescription,color=Muted,fontSize=14.sp,lineHeight=22.sp)
            Spacer(Modifier.height(20.dp));DarkCard(Modifier.fillMaxWidth()){Column(Modifier.padding(17.dp)){Text("COLLECTIBLE DETAILS",color=Muted,fontSize=9.sp,fontWeight=FontWeight.SemiBold,letterSpacing=.8.sp);Spacer(Modifier.height(9.dp));DetailLine("Rarity",t.rarity);DetailLine("Discovery",t.name);DetailLine("Ownership","Local demo collection");DetailLine("Network","Not minted")}}
            Spacer(Modifier.height(20.dp))
        }
        Column(Modifier.padding(horizontal=24.dp,vertical=12.dp)){PrimaryButton("Explore the market preview",Modifier.fillMaxWidth(),onClick=onMarket);Text("Selling and transferring are unavailable for demo artifacts.",color=Muted,fontSize=10.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(top=8.dp))}
    }
}
@Composable private fun DetailLine(label:String,value:String){Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){Text(label,color=Muted,fontSize=12.sp,modifier=Modifier.weight(1f));Text(value,color=TextPrimary,fontSize=12.sp,fontWeight=FontWeight.Medium)}}

@Composable fun MarketScreen(padding:PaddingValues,onWallet:()->Unit,onToast:(String)->Unit){
    var filter by remember{mutableStateOf("All")}
    val listings=listOf(Triple("moonwell","Moonwell Sigil","EPIC"),Triple("tideglass","Tideglass Archive","LEGENDARY"),Triple("foxglove","Foxglove Beacon","RARE"))
    val visible=listings.filter{filter=="All"||it.third==filter}
    Column(Modifier.fillMaxSize().background(Ink).padding(bottom=padding.calculateBottomPadding())){
        ScreenHeader("The exchange","COLLECTOR MARKET",trailing={IconButton(onClick=onWallet,modifier=Modifier.clip(RoundedCornerShape(12.dp)).background(SoftGreen)){Icon(Icons.Default.AccountBalanceWallet,"Wallet",tint=Forest)}})
        Row(Modifier.padding(horizontal=24.dp).fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(ArtifactNight).padding(19.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("COLLECTOR PREVIEW",color=Color(0xFFD8C6A2),fontSize=9.sp,fontWeight=FontWeight.SemiBold,letterSpacing=1.2.sp);Spacer(Modifier.height(8.dp));Text("Objects with\na story to tell.",color=Color.White,style=MaterialTheme.typography.headlineMedium,lineHeight=30.sp)};Column(horizontalAlignment=Alignment.End){Text("WALLET",color=Color(0xFFB7B9B1),fontSize=9.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(5.dp));Text("Not connected",color=Color.White,fontSize=11.sp)}}
        Row(Modifier.fillMaxWidth().padding(start=19.dp,end=19.dp,top=14.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("All","LEGENDARY","EPIC","RARE").forEach{value->FilterChip(selected=filter==value,onClick={filter=value},label={Text(value,fontSize=9.sp)},shape=RoundedCornerShape(30.dp),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=SoftGreen,selectedLabelColor=Forest,containerColor=Panel,labelColor=Muted))}}
        Text("FEATURED OBJECTS",color=Muted,fontSize=9.sp,fontWeight=FontWeight.SemiBold,letterSpacing=1.1.sp,modifier=Modifier.padding(start=24.dp,top=14.dp,bottom=8.dp))
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(horizontal=20.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(11.dp)){items(visible,key={it.first}){item->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).border(1.dp,Border,RoundedCornerShape(16.dp)).padding(10.dp),verticalAlignment=Alignment.CenterVertically){CollectibleArtwork(item.first,Modifier.size(86.dp),dark=true);Spacer(Modifier.width(13.dp));Column(Modifier.weight(1f)){Text(item.second,color=TextPrimary,fontSize=14.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Spacer(Modifier.height(5.dp));Chip(item.third,SoftGreen,Gold);Spacer(Modifier.height(6.dp));Text("Price not configured",color=Muted,fontSize=10.sp);Text("Creator · unavailable",color=Muted,fontSize=10.sp)};TextButton(onClick={onToast("Marketplace actions require a connected wallet and live listing service")}){Text("Details",color=Forest,fontSize=10.sp)}}
        }}
        Text("Sample listings only · no items or prices are offered for sale.",color=Muted,fontSize=10.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=9.dp))
    }
}
private fun rarityRank(value:String)=when(value){"LEGENDARY"->3;"EPIC"->2;"RARE"->1;else->0}
