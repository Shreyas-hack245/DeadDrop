package com.deaddrop.feature.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
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

@Composable fun ProfileScreen(padding:PaddingValues,isDemo:Boolean,onWallet:()->Unit,onHistory:()->Unit,onAuth:()->Unit,onMode:()->Unit) {
    Column(Modifier.fillMaxSize().padding(bottom=padding.calculateBottomPadding()).verticalScroll(rememberScrollState())) {
        ScreenHeader("Explorer profile","YOUR FIELD LOG",trailing={Icon(Icons.Default.Settings,null,tint=Muted)})
        Row(Modifier.padding(horizontal=22.dp).fillMaxWidth().clip(RoundedCornerShape(23.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF29214D),Panel))).padding(19.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(58.dp).clip(CircleShape).background(Violet.copy(alpha=.18f)),contentAlignment=Alignment.Center){Text("A",color=Violet,fontSize=24.sp,fontWeight=FontWeight.Bold)};Spacer(Modifier.width(13.dp));Column(Modifier.weight(1f)){Text("Alex Morgan",color=TextPrimary,fontSize=17.sp,fontWeight=FontWeight.SemiBold);Text("Explorer since Nov ’26",color=Muted,fontSize=11.sp)};Chip(if(isDemo)"DEMO" else "LIVE",if(isDemo)Violet.copy(alpha=.16f) else Cyan.copy(alpha=.13f),if(isDemo)Violet else Cyan)}
        Spacer(Modifier.height(15.dp));Row(Modifier.padding(horizontal=22.dp).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){Metric("01","RELICS",Modifier.weight(1f));Metric("03","HUNTS",Modifier.weight(1f));Metric("LV 2","RANK",Modifier.weight(1f))}
        Spacer(Modifier.height(21.dp));Text("ACCOUNT",color=Muted,fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.7.sp,modifier=Modifier.padding(horizontal=24.dp));Spacer(Modifier.height(6.dp))
        ProfileRow(Icons.Default.AccountBalanceWallet,"Wallet & network","Polygon Amoy",onWallet);ProfileRow(Icons.Default.ReceiptLong,"Transaction history","No real transactions",onHistory);ProfileRow(Icons.Default.PersonOutline,"Sign in / account","Firebase setup optional",onAuth)
        Spacer(Modifier.height(18.dp));Text("PREFERENCES",color=Muted,fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.7.sp,modifier=Modifier.padding(horizontal=24.dp));Spacer(Modifier.height(6.dp))
        ProfileRow(Icons.Default.SportsEsports,"Demo mode",if(isDemo)"Enabled · seeded experiences" else "Disabled",onMode)
        Spacer(Modifier.height(20.dp));Text("DeadDrop v1.0.0 · Built for curious minds",color=Muted,fontSize=9.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(bottom=15.dp))
    }
}
@Composable private fun Metric(value:String,label:String,modifier:Modifier){Column(modifier.clip(RoundedCornerShape(17.dp)).background(Panel).padding(14.dp)){Text(value,color=Cyan,fontSize=19.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(5.dp));Text(label,color=Muted,fontSize=8.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)} }
@Composable private fun ProfileRow(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String,onClick:()->Unit){Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=22.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(39.dp).clip(RoundedCornerShape(13.dp)).background(PanelLight),contentAlignment=Alignment.Center){Icon(icon,null,tint=Violet,modifier=Modifier.size(19.dp))};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(title,color=TextPrimary,fontSize=13.sp,fontWeight=FontWeight.Medium);Text(subtitle,color=Muted,fontSize=10.sp)};Icon(Icons.Default.ChevronRight,null,tint=Muted,modifier=Modifier.size(18.dp))} }

@Composable fun WalletScreen(isDemo:Boolean,onBack:()->Unit,onToast:(String)->Unit){Column(Modifier.fillMaxSize()){ScreenHeader("Wallet","WEB3 CONNECTIVITY",onBack);Column(Modifier.fillMaxWidth().padding(horizontal=22.dp).clip(RoundedCornerShape(23.dp)).background(Brush.linearGradient(listOf(Color(0xFF153641),Panel))).padding(20.dp)){Text("NETWORK",color=Muted,fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.4.sp);Spacer(Modifier.height(7.dp));Text("Polygon Amoy Testnet",color=TextPrimary,fontSize=18.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(15.dp));Chip("${if(isDemo)"DEMO" else "NOT CONNECTED"} · NO WALLET",Cyan.copy(alpha=.12f),Cyan)};Spacer(Modifier.height(16.dp));DarkCard(Modifier.padding(horizontal=22.dp).fillMaxWidth()){Column(Modifier.padding(19.dp)){Text("Wallet connection",color=TextPrimary,fontSize=15.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(8.dp));Text("A wallet connector and contract address are not configured. Sample collectibles remain off chain.",color=Muted,fontSize=12.sp,lineHeight=18.sp);Spacer(Modifier.height(14.dp));PrimaryButton("Configure wallet",Modifier.fillMaxWidth(),onClick={onToast("Wallet connection is unavailable until a connector is configured")})}};Spacer(Modifier.height(18.dp));Text("No signing requests or transactions have been made.",color=Muted,fontSize=10.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())} }

@Composable fun HistoryScreen(onBack:()->Unit){Column(Modifier.fillMaxSize()){ScreenHeader("Activity","TRANSACTION HISTORY",onBack);Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.ReceiptLong,null,tint=Violet,modifier=Modifier.size(45.dp));Spacer(Modifier.height(14.dp));Text("Nothing on chain yet",color=TextPrimary,fontSize=18.sp,fontWeight=FontWeight.SemiBold);Text("Demo discoveries are local and do not appear as transactions.",color=Muted,fontSize=11.sp,textAlign=TextAlign.Center,modifier=Modifier.padding(horizontal=40.dp,vertical=8.dp))}}} }

@Composable fun AuthScreen(onBack:()->Unit,onContinue:()->Unit){var email by remember{mutableStateOf("")};var pass by remember{mutableStateOf("")};Column(Modifier.fillMaxSize()){ScreenHeader("Welcome back","EXPLORER ACCESS",onBack);Column(Modifier.fillMaxWidth().padding(22.dp).verticalScroll(rememberScrollState())){Text("Your next story is\nout there.",color=TextPrimary,fontSize=31.sp,fontWeight=FontWeight.Bold,lineHeight=37.sp);Spacer(Modifier.height(12.dp));Text("Sign in to sync your collection across devices.",color=Muted,fontSize=13.sp);Spacer(Modifier.height(25.dp));OutlinedTextField(email,{email=it},modifier=Modifier.fillMaxWidth(),label={Text("Email")},singleLine=true,shape=RoundedCornerShape(15.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Violet,unfocusedBorderColor=PanelLight,focusedTextColor=TextPrimary,unfocusedTextColor=TextPrimary));Spacer(Modifier.height(12.dp));OutlinedTextField(pass,{pass=it},modifier=Modifier.fillMaxWidth(),label={Text("Password")},singleLine=true,shape=RoundedCornerShape(15.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Violet,unfocusedBorderColor=PanelLight,focusedTextColor=TextPrimary,unfocusedTextColor=TextPrimary));Spacer(Modifier.height(18.dp));PrimaryButton("Continue in demo mode",Modifier.fillMaxWidth(),onClick=onContinue);Spacer(Modifier.height(9.dp));Text("Firebase Auth is not configured. This continues locally and does not create an account.",color=Muted,fontSize=10.sp,lineHeight=15.sp,textAlign=TextAlign.Center);Spacer(Modifier.height(17.dp));TextButton(onClick=onContinue,modifier=Modifier.align(Alignment.CenterHorizontally)){Text("Continue as guest",color=Cyan)} } } }
