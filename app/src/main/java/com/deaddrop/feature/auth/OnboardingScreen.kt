package com.deaddrop.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deaddrop.core.theme.*

@Composable fun OnboardingScreen(onStart:()->Unit,onSignIn:()->Unit) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered=true }
    val alpha by animateFloatAsState(if(entered) 1f else 0f,animationSpec=tween(650),label="welcomeFade")
    val offset by animateFloatAsState(if(entered) 0f else 18f,animationSpec=tween(650),label="welcomeRise")
    Column(Modifier.fillMaxSize().background(Ink).padding(horizontal=28.dp, vertical=26.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){BrandMark(Modifier.size(34.dp));Spacer(Modifier.width(10.dp));Text("DeadDrop",style=MaterialTheme.typography.titleLarge,color=TextPrimary);Spacer(Modifier.weight(1f));Chip("DEMO",SoftGreen,Forest)}
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth().graphicsLayer{this.alpha=alpha;translationY=offset},horizontalAlignment=Alignment.CenterHorizontally) {
            Box(Modifier.size(106.dp).border(1.dp,Border,CircleShape),contentAlignment=Alignment.Center) {
                Canvas(Modifier.fillMaxSize().padding(15.dp)){val c=Offset(size.width*.5f,size.height*.5f);drawCircle(Forest.copy(alpha=.8f),size.minDimension*.34f,c,style=androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()));drawCircle(Gold,size.minDimension*.07f,Offset(c.x+size.minDimension*.17f,c.y-size.minDimension*.09f))}
                Icon(Icons.Outlined.Explore,null,tint=Forest,modifier=Modifier.size(33.dp))
            }
            Spacer(Modifier.height(34.dp));Text("A little farther.\nA little wonder.",style=MaterialTheme.typography.displayLarge,color=TextPrimary,textAlign=TextAlign.Center)
            Spacer(Modifier.height(16.dp));Text("Follow thoughtful clues, find hidden places, and collect the stories you uncover.",color=Muted,fontSize=15.sp,lineHeight=23.sp,textAlign=TextAlign.Center,modifier=Modifier.padding(horizontal=12.dp))
            Spacer(Modifier.height(28.dp));Chip("OFFLINE EXPLORATION PREVIEW",Color(0xFFEDE9DE),Muted)
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton("Explore nearby",Modifier.fillMaxWidth(),onClick=onStart)
        Spacer(Modifier.height(8.dp));TextButton(onClick=onSignIn){Text("Sign in or continue as guest",color=Forest,fontWeight=FontWeight.Medium)}
        Spacer(Modifier.height(12.dp));Text("Discover responsibly. Stay in public places.",color=Muted,fontSize=11.sp,textAlign=TextAlign.Center)
    }
}

@Composable private fun BrandMark(modifier:Modifier=Modifier) {
    Canvas(modifier) { val c=Offset(size.width*.5f,size.height*.5f);drawCircle(Forest,size.minDimension*.43f,c,style=androidx.compose.ui.graphics.drawscope.Stroke(size.minDimension*.08f));drawCircle(Gold,size.minDimension*.1f,Offset(c.x+size.minDimension*.2f,c.y-size.minDimension*.13f)) }
}
