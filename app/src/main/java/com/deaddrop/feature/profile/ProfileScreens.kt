package com.deaddrop.feature.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deaddrop.core.theme.*

@Composable fun ProfileScreen(padding:PaddingValues,isDemo:Boolean,onWallet:()->Unit,onHistory:()->Unit,onAuth:()->Unit,onMode:()->Unit) {
    Column(Modifier.fillMaxSize().background(Ink).padding(bottom=padding.calculateBottomPadding()).verticalScroll(rememberScrollState())) {
        ScreenHeader("Your profile","EXPLORER ACCOUNT",trailing={Chip(if(isDemo)"DEMO PROFILE" else "ACCOUNT",SoftGreen,Forest)})
        Row(Modifier.padding(horizontal=24.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel).border(1.dp,Border,RoundedCornerShape(20.dp)).padding(18.dp),verticalAlignment=Alignment.CenterVertically){
            Box(Modifier.size(58.dp).clip(CircleShape).background(SoftGreen),contentAlignment=Alignment.Center){Text("A",color=Forest,fontSize=22.sp,fontWeight=FontWeight.Medium)}
            Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text("Alex Morgan",color=TextPrimary,fontSize=18.sp,fontWeight=FontWeight.SemiBold);Text("Explorer since Nov ’26",color=Muted,fontSize=12.sp)}
            Icon(Icons.Default.ChevronRight,null,tint=Muted)
        }
        Spacer(Modifier.height(14.dp));Row(Modifier.padding(horizontal=24.dp).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)){Metric("01","ARTIFACTS",Modifier.weight(1f));Metric("03","HUNTS",Modifier.weight(1f));Metric("02","LEVEL",Modifier.weight(1f))}
        Spacer(Modifier.height(24.dp));Text("YOUR FIELD NOTES",color=Muted,fontSize=10.sp,fontWeight=FontWeight.SemiBold,letterSpacing=1.3.sp,modifier=Modifier.padding(horizontal=25.dp));Spacer(Modifier.height(5.dp))
        ProfileRow(Icons.Default.Explore,"Discovered treasures","1 artifact in your archive",onHistory)
        ProfileRow(Icons.Default.EmojiEvents,"Achievements","First Signal · Curious Mind"){onMode()}
        Spacer(Modifier.height(15.dp));Text("ACCOUNT & SETTINGS",color=Muted,fontSize=10.sp,fontWeight=FontWeight.SemiBold,letterSpacing=1.3.sp,modifier=Modifier.padding(horizontal=25.dp));Spacer(Modifier.height(5.dp))
        ProfileRow(Icons.Default.AccountBalanceWallet,"Wallet & network","Not connected · Polygon Amoy",onWallet)
        ProfileRow(Icons.AutoMirrored.Filled.ReceiptLong,"Transaction history","No on-chain activity",onHistory)
        ProfileRow(Icons.Default.PersonOutline,"Sign in & sync","Account service unavailable in demo",onAuth)
        ProfileRow(Icons.Default.SportsEsports,"Demo experience","Seeded locations · off-chain items",onMode)
        Spacer(Modifier.height(18.dp));Text("DeadDrop · Made for curious minds",color=Muted,fontSize=10.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(bottom=18.dp))
    }
}

@Composable private fun Metric(value:String,label:String,modifier:Modifier){Column(modifier.clip(RoundedCornerShape(15.dp)).background(Panel).border(1.dp,Border,RoundedCornerShape(15.dp)).padding(horizontal=13.dp,vertical=14.dp)){Text(value,color=Forest,fontSize=20.sp,fontWeight=FontWeight.Medium);Spacer(Modifier.height(5.dp));Text(label,color=Muted,fontSize=9.sp,fontWeight=FontWeight.SemiBold,letterSpacing=.45.sp)} }
@Composable private fun ProfileRow(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String,onClick:()->Unit){Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=24.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Panel).border(1.dp,Border,RoundedCornerShape(13.dp)),contentAlignment=Alignment.Center){Icon(icon,null,tint=Forest,modifier=Modifier.size(19.dp))};Spacer(Modifier.width(13.dp));Column(Modifier.weight(1f)){Text(title,color=TextPrimary,fontSize=14.sp,fontWeight=FontWeight.Medium);Text(subtitle,color=Muted,fontSize=11.sp)};Icon(Icons.Default.ChevronRight,null,tint=Muted,modifier=Modifier.size(19.dp))} }

@Composable fun WalletScreen(isDemo:Boolean,onBack:()->Unit,onToast:(String)->Unit){
    Column(Modifier.fillMaxSize().background(Ink)){
        ScreenHeader("Wallet","WEB3 CONNECTIVITY",onBack)
        Column(Modifier.padding(horizontal=24.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ArtifactNight).padding(22.dp)){
            Text("NETWORK",color=Color(0xFFC6C7BD),fontSize=10.sp,fontWeight=FontWeight.SemiBold,letterSpacing=1.3.sp)
            Spacer(Modifier.height(9.dp));Text("Polygon Amoy",color=Color.White,style=MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(18.dp));Chip("NOT CONNECTED",Color(0xFF313832),Color(0xFFD8C6A2))
        }
        Spacer(Modifier.height(16.dp));DarkCard(Modifier.padding(horizontal=24.dp).fillMaxWidth()) { Column(Modifier.padding(20.dp)){
            Text("Wallet connection",color=TextPrimary,fontSize=16.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(8.dp))
            Text("A wallet connector and contract address are not configured. Demo collectibles remain off chain.",color=Muted,fontSize=13.sp,lineHeight=19.sp)
            Spacer(Modifier.height(17.dp));PrimaryButton("Wallet setup unavailable",Modifier.fillMaxWidth(),onClick={onToast("No wallet connector is configured. No transaction was attempted.")})
        } }
        Spacer(Modifier.height(13.dp));Text("No signing requests or transactions have been made.",color=Muted,fontSize=11.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(horizontal=24.dp))
    }
}

@Composable fun HistoryScreen(onBack:()->Unit){Column(Modifier.fillMaxSize().background(Ink)){ScreenHeader("Activity","TRANSACTION HISTORY",onBack);Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.padding(36.dp)){Icon(Icons.AutoMirrored.Filled.ReceiptLong,null,tint=Forest,modifier=Modifier.size(40.dp));Spacer(Modifier.height(16.dp));Text("No on-chain activity",color=TextPrimary,style=MaterialTheme.typography.titleLarge);Spacer(Modifier.height(7.dp));Text("Demo discoveries are saved locally and do not appear as blockchain transactions.",color=Muted,fontSize=13.sp,lineHeight=19.sp,textAlign=TextAlign.Center)}}} }

@Composable fun AuthScreen(onBack:()->Unit,onContinue:()->Unit){
    var email by remember{mutableStateOf("")};var pass by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize().background(Ink)){
        ScreenHeader("Welcome back","EXPLORER ACCOUNT",onBack)
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=24.dp)){
            Spacer(Modifier.height(20.dp));Text("Your next story\nstarts nearby.",style=MaterialTheme.typography.headlineLarge,color=TextPrimary)
            Spacer(Modifier.height(12.dp));Text("Sign in to sync your discoveries across devices.",color=Muted,fontSize=14.sp,lineHeight=21.sp)
            Spacer(Modifier.height(28.dp));Text("Email address",color=TextPrimary,fontSize=12.sp,fontWeight=FontWeight.Medium);Spacer(Modifier.height(7.dp))
            OutlinedTextField(email,{email=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("you@example.com",color=Muted)},singleLine=true,shape=RoundedCornerShape(13.dp),colors=inputColors())
            Spacer(Modifier.height(15.dp));Text("Password",color=TextPrimary,fontSize=12.sp,fontWeight=FontWeight.Medium);Spacer(Modifier.height(7.dp))
            OutlinedTextField(pass,{pass=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("Enter your password",color=Muted)},visualTransformation=PasswordVisualTransformation(),singleLine=true,shape=RoundedCornerShape(13.dp),colors=inputColors())
            Spacer(Modifier.height(18.dp));Text("Firebase authentication is not configured. These fields are not submitted or stored.",color=Muted,fontSize=11.sp,lineHeight=17.sp)
        }
        Column(Modifier.padding(horizontal=24.dp,vertical=14.dp)){PrimaryButton("Continue in demo mode",Modifier.fillMaxWidth(),onClick=onContinue);Spacer(Modifier.height(7.dp));TextButton(onClick=onContinue,modifier=Modifier.align(Alignment.CenterHorizontally)){Text("Continue as guest",color=Forest)}}
    }
}

@Composable private fun inputColors()=OutlinedTextFieldDefaults.colors(focusedBorderColor=Forest,unfocusedBorderColor=Border,focusedTextColor=TextPrimary,unfocusedTextColor=TextPrimary,cursorColor=Forest,focusedContainerColor=Panel,unfocusedContainerColor=Panel)
