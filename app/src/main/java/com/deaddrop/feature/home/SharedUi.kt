package com.deaddrop.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deaddrop.core.theme.*

@Composable fun ScreenHeader(title:String, eyebrow:String="", onBack:(()->Unit)?=null, trailing:(@Composable ()->Unit)?=null) {
    Row(Modifier.fillMaxWidth().padding(horizontal=22.dp, vertical=18.dp),verticalAlignment=Alignment.CenterVertically) {
        if(onBack!=null) { Box(Modifier.size(40.dp).clip(CircleShape).background(Panel).clickable { onBack() },contentAlignment=Alignment.Center){Icon(Icons.Default.ArrowBack,null,tint=TextPrimary)}; Spacer(Modifier.width(14.dp)) }
        Column(Modifier.weight(1f)) { if(eyebrow.isNotBlank()) Text(eyebrow.uppercase(),color=Cyan,fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.7.sp); Text(title,color=TextPrimary,fontSize=23.sp,fontWeight=FontWeight.SemiBold) }
        trailing?.invoke()
    }
}
@Composable fun Chip(text:String, background:Color=Violet.copy(alpha=.13f), color:Color=Violet) { Box(Modifier.clip(RoundedCornerShape(99.dp)).background(background).padding(horizontal=10.dp,vertical=6.dp)){Text(text,color=color,fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=.8.sp)} }
@Composable fun PrimaryButton(text:String, modifier:Modifier=Modifier, enabled:Boolean=true, onClick:()->Unit) { Button(onClick=onClick,modifier=modifier.height(54.dp),enabled=enabled,shape=RoundedCornerShape(17.dp),colors=ButtonDefaults.buttonColors(containerColor=Violet,contentColor=Ink,disabledContainerColor=PanelLight,disabledContentColor=Muted),contentPadding=PaddingValues(horizontal=20.dp)){Text(text,fontSize=14.sp,fontWeight=FontWeight.Bold,letterSpacing=.2.sp)} }
@Composable fun DarkCard(modifier:Modifier=Modifier, content:@Composable ColumnScope.()->Unit) { Card(modifier,shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Panel),content=content) }
