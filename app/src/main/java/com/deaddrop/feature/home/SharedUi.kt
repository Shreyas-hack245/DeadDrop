package com.deaddrop.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deaddrop.core.theme.*

@Composable
fun ScreenHeader(title:String, eyebrow:String="", onBack:(()->Unit)?=null, trailing:(@Composable ()->Unit)?=null) {
    Row(Modifier.fillMaxWidth().padding(horizontal=24.dp, vertical=16.dp),verticalAlignment=Alignment.CenterVertically) {
        if(onBack!=null) {
            IconButton(onClick=onBack, modifier=Modifier.size(42.dp).clip(CircleShape).background(Panel).border(1.dp,Border,CircleShape)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription="Back", tint=TextPrimary)
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            if(eyebrow.isNotBlank()) Text(eyebrow, color=Muted, fontSize=10.sp, fontWeight=FontWeight.SemiBold, letterSpacing=1.35.sp)
            Text(title, color=TextPrimary, style=MaterialTheme.typography.headlineMedium)
        }
        trailing?.invoke()
    }
}

@Composable
fun Chip(text:String, background:Color=SoftGreen, color:Color=Forest, modifier:Modifier=Modifier) {
    Box(modifier.clip(RoundedCornerShape(100.dp)).background(background).padding(horizontal=11.dp,vertical=6.dp)) {
        Text(text,color=color,fontSize=10.sp,fontWeight=FontWeight.SemiBold,letterSpacing=.35.sp)
    }
}

@Composable
fun PrimaryButton(text:String, modifier:Modifier=Modifier, enabled:Boolean=true, onClick:()->Unit) {
    Button(onClick=onClick,modifier=modifier.height(54.dp),enabled=enabled,shape=RoundedCornerShape(14.dp),
        colors=ButtonDefaults.buttonColors(containerColor=Forest,contentColor=Color.White,disabledContainerColor=PanelLight,disabledContentColor=Muted),
        contentPadding=PaddingValues(horizontal=20.dp)) {
        Text(text,fontSize=14.sp,fontWeight=FontWeight.SemiBold,letterSpacing=.1.sp)
    }
}

@Composable
fun DarkCard(modifier:Modifier=Modifier, content:@Composable ColumnScope.()->Unit) {
    Card(modifier,shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Panel),
        border=androidx.compose.foundation.BorderStroke(1.dp,Border),content=content)
}

@Composable
fun CollectibleArtwork(id:String,modifier:Modifier=Modifier, dark:Boolean=false) {
    val backdrop = if(dark) Color(0xFF241B38) else PanelLight
    val accent = when(id) { "foxglove" -> Color(0xFFFF86C8); "tideglass" -> Cyan; else -> Gold }
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(if(dark) listOf(Color(0xFF36234B),Color(0xFF111323),ArtifactNight) else listOf(Color(0xFF24203A),PanelLight))),contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c=Offset(size.width*.5f,size.height*.49f)
            val radius=size.minDimension*.28f
            if(id=="moonwell") {
                drawCircle(accent.copy(alpha=.88f),radius,c,style=Stroke(size.minDimension*.012f))
                drawCircle(accent.copy(alpha=.5f),radius*.68f,c,style=Stroke(size.minDimension*.007f))
                drawCircle(if(dark) Color(0xFF241B38) else backdrop,radius*.48f,Offset(c.x+radius*.3f,c.y-radius*.16f))
                drawCircle(accent,radius*.075f,Offset(c.x-radius*.23f,c.y+radius*.2f))
            } else if(id=="foxglove") {
                val stem=Path().apply{moveTo(c.x,c.y+radius*.95f);cubicTo(c.x-radius*.18f,c.y+radius*.35f,c.x+radius*.3f,c.y-radius*.2f,c.x,c.y-radius*.88f)}
                drawPath(stem,accent,style=Stroke(size.minDimension*.018f,cap=StrokeCap.Round))
                listOf(-.55f to -.38f,.42f to -.58f,-.4f to .1f,.5f to .18f).forEach{(x,y)->
                    drawOval(accent.copy(alpha=.86f),topLeft=Offset(c.x+radius*x-size.minDimension*.045f,c.y+radius*y-size.minDimension*.09f),size=androidx.compose.ui.geometry.Size(size.minDimension*.09f,size.minDimension*.18f))
                }
                drawCircle(Gold,radius*.11f,Offset(c.x,c.y-radius*.12f))
            } else {
                val outer=Path().apply{moveTo(c.x,c.y-radius);lineTo(c.x+radius*.72f,c.y-radius*.12f);lineTo(c.x+radius*.52f,c.y+radius*.72f);lineTo(c.x,c.y+radius);lineTo(c.x-radius*.52f,c.y+radius*.72f);lineTo(c.x-radius*.72f,c.y-radius*.12f);close()}
                drawPath(outer,accent.copy(alpha=.84f))
                drawLine(Color.White.copy(alpha=.75f),Offset(c.x,c.y-radius),Offset(c.x,c.y+radius),size.minDimension*.009f)
                drawLine(Color.White.copy(alpha=.55f),Offset(c.x-radius*.72f,c.y-radius*.12f),Offset(c.x,c.y+radius),size.minDimension*.009f)
                drawLine(Color.White.copy(alpha=.55f),Offset(c.x+radius*.72f,c.y-radius*.12f),Offset(c.x,c.y+radius),size.minDimension*.009f)
                drawLine(Color.White.copy(alpha=.45f),Offset(c.x-radius*.52f,c.y+radius*.72f),Offset(c.x+radius*.52f,c.y+radius*.72f),size.minDimension*.009f)
            }
            drawCircle(if(dark) Color.White.copy(alpha=.12f) else Color.White.copy(alpha=.64f),size.minDimension*.018f,Offset(size.width*.78f,size.height*.22f))
        }
    }
}
