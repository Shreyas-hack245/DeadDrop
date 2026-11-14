package com.deaddrop.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deaddrop.core.theme.*

@Composable fun OnboardingScreen(onStart:()->Unit,onSignIn:()->Unit) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF17152F),Ink)))) {
        Canvas(Modifier.fillMaxSize()) { val c=Offset(size.width*.5f,size.height*.34f); for(i in 1..5) drawCircle(Violet.copy(alpha=.04f),size.minDimension*i/5f,c) }
        Column(Modifier.fillMaxSize().padding(horizontal=28.dp, vertical=32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
            Box(Modifier.size(120.dp).background(Brush.linearGradient(listOf(Violet.copy(alpha=.3f),Cyan.copy(alpha=.12f))),CircleShape),contentAlignment=Alignment.Center) { Icon(Icons.Default.Explore,null,tint=Cyan,modifier=Modifier.size(54.dp)) }
            Spacer(Modifier.height(29.dp));Text("DEADDROP",color=Cyan,fontSize=12.sp,fontWeight=FontWeight.Bold,letterSpacing=4.sp)
            Spacer(Modifier.height(14.dp));Text("Good finds are\nout there.",color=TextPrimary,fontSize=34.sp,fontWeight=FontWeight.Bold,lineHeight=39.sp,textAlign=TextAlign.Center)
            Spacer(Modifier.height(12.dp));Text("Follow hidden signals, crack curious clues, and collect the stories tucked into your city.",color=Muted,fontSize=14.sp,lineHeight=22.sp,textAlign=TextAlign.Center)
            Spacer(Modifier.height(24.dp));Chip("✦  OFFLINE DEMO READY",Cyan.copy(alpha=.12f),Cyan)
            Spacer(Modifier.height(45.dp));PrimaryButton("Start exploring  →",Modifier.fillMaxWidth(),onClick=onStart)
            Spacer(Modifier.height(7.dp));TextButton(onClick=onSignIn){Text("Sign in or continue as guest",color=TextPrimary,fontSize=13.sp)}
            Spacer(Modifier.height(20.dp));Text("Explore responsibly. Never enter restricted or unsafe areas.",color=Muted,fontSize=10.sp,textAlign=TextAlign.Center)
        }
    }
}
