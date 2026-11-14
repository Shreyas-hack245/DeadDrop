package com.deaddrop.feature.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.location.LocationServices
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executor
import com.deaddrop.core.theme.*
import com.deaddrop.data.model.Treasure

@Composable fun DetailScreen(t:Treasure,onBack:()->Unit,onStart:()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(330.dp).background(Brush.linearGradient(listOf(Color(0xFF30245F),Color(0xFF101E35),Ink)))) {
            Box(Modifier.align(Alignment.Center).size(185.dp).border(1.dp,Violet.copy(alpha=.26f),CircleShape).padding(17.dp).border(1.dp,Cyan.copy(alpha=.2f),CircleShape),contentAlignment=Alignment.Center){ Text(t.emoji,fontSize=90.sp) }
            ScreenHeader("Signal detected", "TREASURE INTEL",onBack, trailing={Chip(t.rarity,Gold.copy(alpha=.13f),Gold)})
            Row(Modifier.align(Alignment.BottomStart).padding(24.dp),verticalAlignment=Alignment.CenterVertically){ Icon(Icons.Default.LocationOn,null,tint=Cyan,modifier=Modifier.size(17.dp)); Spacer(Modifier.width(5.dp));Text("NORTH BEACH · ${t.distance}",color=TextPrimary,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.4.sp) }
        }
        Column(Modifier.padding(horizontal=22.dp, vertical=22.dp)) {
            Text(t.category,color=Cyan,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=2.sp)
            Spacer(Modifier.height(8.dp)); Text(t.name,color=TextPrimary,fontSize=28.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(20.dp)); DarkCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(19.dp)) { Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.AutoAwesome,null,tint=Violet);Spacer(Modifier.width(9.dp));Text("A whispered clue",color=TextPrimary,fontSize=15.sp,fontWeight=FontWeight.SemiBold)}; Spacer(Modifier.height(12.dp)); Text("“${t.clue}”",color=Muted,fontSize=15.sp,lineHeight=23.sp) } }
            Spacer(Modifier.height(18.dp)); Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) { Stat("DISTANCE",t.distance,Modifier.weight(1f));Stat("RARITY",t.rarity,Modifier.weight(1f));Stat("REWARD","Artifact",Modifier.weight(1f)) }
            Spacer(Modifier.height(20.dp)); Text("How the hunt works",color=TextPrimary,fontWeight=FontWeight.SemiBold,fontSize=16.sp); Spacer(Modifier.height(12.dp)); StepRow("01","Follow your compass to the signal radius");StepRow("02","Verify your location and scan the marker");StepRow("03","Solve the final cipher to claim your relic")
            Spacer(Modifier.height(24.dp)); PrimaryButton("Begin expedition  →",Modifier.fillMaxWidth(),onClick=onStart); Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable private fun Stat(label:String,value:String,modifier:Modifier=Modifier){Column(modifier.clip(RoundedCornerShape(16.dp)).background(Panel).padding(13.dp)){Text(label,color=Muted,fontSize=8.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp);Spacer(Modifier.height(7.dp));Text(value,color=TextPrimary,fontSize=12.sp,fontWeight=FontWeight.Bold,maxLines=1)} }
@Composable private fun StepRow(n:String,text:String){Row(Modifier.padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){Text(n,color=Violet,fontSize=10.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.width(13.dp));Text(text,color=Muted,fontSize=12.sp)} }

@Composable fun HuntScreen(t:Treasure,onBack:()->Unit,onScan:()->Unit) {
    var permissionGranted by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var demoArrival by remember { mutableStateOf(false) }
    var distanceMeters by remember { mutableStateOf<Float?>(null) }
    var locationMessage by remember { mutableStateOf("Location has not been checked") }
    val context=androidx.compose.ui.platform.LocalContext.current
    fun checkDistance() {
        locationMessage = "Checking device location…"
        readDistance(context,t) { meters, error ->
            distanceMeters = meters
            locationMessage = error ?: if (meters != null && meters <= 100f) "Within the 100 m discovery radius" else "Outside the discovery radius"
        }
    }
    val permission=rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()){ result ->
        permissionGranted=result[android.Manifest.permission.ACCESS_FINE_LOCATION]==true || result[android.Manifest.permission.ACCESS_COARSE_LOCATION]==true
        permissionDenied=!permissionGranted
        if(permissionGranted) checkDistance() else locationMessage="Location permission denied. Demo mode is still available."
    }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Find the signal","GPS DISCOVERY",onBack)
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal=18.dp).clip(RoundedCornerShape(26.dp)).background(Brush.radialGradient(listOf(Color(0xFF21355A),Color(0xFF111A2D),Ink)))) {
            Canvas(Modifier.fillMaxSize()) { val c=Offset(size.width*.5f,size.height*.5f); for(i in 1..5) drawCircle(Cyan.copy(alpha=.09f),size.minDimension*i/9f,c,style=Stroke(1.dp.toPx())); drawCircle(Violet.copy(alpha=.25f),size.minDimension*.13f,c) }
            Column(Modifier.align(Alignment.Center),horizontalAlignment=Alignment.CenterHorizontally) { Box(Modifier.size(94.dp).border(1.dp,Cyan.copy(alpha=.55f),CircleShape).background(Panel.copy(alpha=.5f),CircleShape),contentAlignment=Alignment.Center){Icon(Icons.Default.NearMe,null,tint=Cyan,modifier=Modifier.size(39.dp))}; Spacer(Modifier.height(18.dp));Text(if(demoArrival) "DEMO SIGNAL ACQUIRED" else if(distanceMeters!=null && distanceMeters!!<=100f) "WITHIN DISCOVERY RADIUS" else "SIGNAL NEARBY",color=if(demoArrival || distanceMeters?.let{it<=100f}==true) Cyan else TextPrimary,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.5.sp);Spacer(Modifier.height(6.dp));Text(if(demoArrival) "Arrival simulated for demo" else distanceMeters?.let{"${it.toInt()} m from the seeded coordinates"} ?: locationMessage,color=Muted,fontSize=13.sp,textAlign=TextAlign.Center) }
            Box(Modifier.align(Alignment.TopCenter).padding(top=20.dp)){Chip(if(demoArrival) "DEMO ARRIVAL · SIMULATED" else "DEVICE GPS · CLIENT SIDE ONLY",Violet.copy(alpha=.15f),Violet)}
        }
        Column(Modifier.padding(22.dp)) {
            DarkCard(Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Text(t.emoji,fontSize=25.sp);Spacer(Modifier.width(12.dp));Column{Text(t.name,color=TextPrimary,fontSize=14.sp,fontWeight=FontWeight.SemiBold);Text("Stay safe. Hunt in public places.",color=Muted,fontSize=11.sp)}}}
            Spacer(Modifier.height(14.dp))
            if(demoArrival || distanceMeters?.let{it<=100f}==true) PrimaryButton("Continue to marker scan  →",Modifier.fillMaxWidth(),onClick=onScan)
            else PrimaryButton(if(permissionGranted) "Refresh device location" else "Enable location",Modifier.fillMaxWidth(),onClick={ if(permissionGranted) checkDistance() else permission.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION)) })
            if(!demoArrival && distanceMeters?.let{it<=100f}!=true) {
                TextButton(onClick={demoArrival=true},modifier=Modifier.fillMaxWidth()){Text("Simulate arrival in demo mode",color=Cyan)}
            }
            Text(if(permissionDenied) "Location was denied. You can retry in Settings or use the labeled demo path." else "Client location is informational only. Production discovery must be verified by a trusted server.",color=Muted,fontSize=10.sp,lineHeight=15.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())
        }
    }
}

private fun readDistance(context: Context, treasure: Treasure, callback: (Float?, String?) -> Unit) {
    val fine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
    if(!fine && !coarse) { callback(null,"Location permission is required"); return }
    try {
        LocationServices.getFusedLocationProviderClient(context).lastLocation
            .addOnSuccessListener { location ->
                if(location==null) callback(null,"No recent device location is available")
                else { val result=FloatArray(1); Location.distanceBetween(location.latitude,location.longitude,treasure.latitude,treasure.longitude,result); callback(result[0],null) }
            }
            .addOnFailureListener { callback(null,"Could not read device location") }
    } catch(_: SecurityException) { callback(null,"Location permission was revoked") }
}

@Composable fun ScanScreen(t:Treasure,onBack:()->Unit,onPuzzle:()->Unit) {
    var cameraEnabled by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Align this hunt’s signed QR marker in the frame") }
    val permission=rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()){ granted -> cameraEnabled=granted;permissionDenied=!granted;if(!granted)message="Camera permission denied. You can use the seeded demo QR." }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Scan the marker","QR CHALLENGE",onBack)
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal=22.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xFF101521)),contentAlignment=Alignment.Center) {
            if(cameraEnabled) {
                CameraQrPreview(expected="DD-${t.id.uppercase()}-DEMO", modifier=Modifier.fillMaxSize(), onValid={message="Sample marker recognized";onPuzzle()}, onError={message=it})
                Column(Modifier.align(Alignment.BottomCenter).padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){Chip("CAMERAX · ML KIT",Panel, Cyan);Spacer(Modifier.height(8.dp));Text(message,color=TextPrimary,fontSize=11.sp,textAlign=TextAlign.Center)}
            } else Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.padding(28.dp)) {
                Box(Modifier.size(180.dp).border(2.dp,Cyan.copy(alpha=.55f),RoundedCornerShape(24.dp)),contentAlignment=Alignment.Center){Text("▦",fontSize=78.sp,color=Color.White.copy(alpha=.3f))}
                Spacer(Modifier.height(20.dp));Text("Scan this hunt’s QR marker",color=TextPrimary,fontSize=15.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(7.dp));Text(if(permissionDenied) "Camera access was denied." else "Camera preview starts after permission is granted.",color=Muted,fontSize=11.sp,textAlign=TextAlign.Center)
            }
            Box(Modifier.align(Alignment.TopCenter).padding(top=17.dp)){Chip(if(cameraEnabled) "LIVE CAMERA · QR ONLY" else "DEMO TOKEN: DD-${t.id.uppercase()}-DEMO",PanelLight,Muted)}
        }
        Column(Modifier.padding(22.dp)) {
            if(cameraEnabled) Text("Camera scanning is active · hold the marker in frame",color=Cyan,fontSize=11.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(vertical=12.dp))
            else PrimaryButton(if(permissionDenied) "Try camera permission again" else "Enable camera scanner",Modifier.fillMaxWidth(),onClick={permission.launch(android.Manifest.permission.CAMERA)})
            TextButton(onClick=onPuzzle,modifier=Modifier.fillMaxWidth()){Text("Use seeded demo QR challenge",color=Cyan)}
            Text("Real camera scans are decoded on device with ML Kit. Production must verify signed, expiring challenge tokens on a trusted server.",color=Muted,fontSize=10.sp,lineHeight=15.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())
        }
    }
}

@Composable private fun CameraQrPreview(expected:String,modifier:Modifier,onValid:()->Unit,onError:(String)->Unit) {
    val context=androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner=LocalLifecycleOwner.current
    val previewView=remember { PreviewView(context).apply { scaleType=PreviewView.ScaleType.FILL_CENTER;implementationMode=PreviewView.ImplementationMode.COMPATIBLE } }
    AndroidView(factory={previewView},modifier=modifier)
    DisposableEffect(lifecycleOwner,expected) {
        val executor=ContextCompat.getMainExecutor(context)
        val scanner=BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
        var provider:ProcessCameraProvider?=null
        var handled=false
        val future=ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                provider=future.get()
                val preview=Preview.Builder().build().also{it.surfaceProvider=previewView.surfaceProvider}
                val analysis=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(executor) { proxy ->
                    val media=proxy.image
                    if(media==null) { proxy.close() } else {
                        scanner.process(InputImage.fromMediaImage(media,proxy.imageInfo.rotationDegrees))
                            .addOnSuccessListener { codes ->
                                val value=codes.firstOrNull()?.rawValue
                                if(value==expected && !handled) { handled=true;onValid() }
                                else if(value!=null && value!=expected) onError("QR found, but it belongs to a different hunt")
                            }
                            .addOnFailureListener { onError("Could not read QR image") }
                            .addOnCompleteListener { proxy.close() }
                    }
                }
                provider?.unbindAll()
                provider?.bindToLifecycle(lifecycleOwner,CameraSelector.DEFAULT_BACK_CAMERA,preview,analysis)
            } catch(_:Exception) { onError("Camera could not be started on this device") }
        },executor)
        onDispose { provider?.unbindAll();scanner.close() }
    }
}

@Composable fun PuzzleScreen(t:Treasure,answer:String,onAnswer:(String)->Unit,onBack:()->Unit,onWin:()->Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("The final cipher","PUZZLE CHALLENGE",onBack)
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Box(Modifier.size(124.dp).clip(RoundedCornerShape(30.dp)).background(Brush.linearGradient(listOf(Violet.copy(alpha=.27f),Cyan.copy(alpha=.12f)))),contentAlignment=Alignment.Center){Text("✧",fontSize=72.sp,color=Gold)}
            Spacer(Modifier.height(22.dp));Chip("FIELD NOTE 03 / 03");Spacer(Modifier.height(20.dp));Text("A quiet answer",color=TextPrimary,fontSize=25.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(10.dp));Text("${t.clue}\n\nType the hidden word to unlock the artifact.",color=Muted,fontSize=14.sp,lineHeight=22.sp,textAlign=TextAlign.Center)
            Spacer(Modifier.height(25.dp));OutlinedTextField(value=answer,onValueChange=onAnswer,modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(16.dp),placeholder={Text("Your answer",color=Muted)},colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Violet,unfocusedBorderColor=PanelLight,focusedTextColor=TextPrimary,unfocusedTextColor=TextPrimary,cursorColor=Cyan))
            Spacer(Modifier.height(10.dp));Text("DEMO ANSWER: MOON",color=Muted,fontSize=9.sp,letterSpacing=1.3.sp)
        }
        Column(Modifier.padding(22.dp)){PrimaryButton("Unlock artifact  ✦",Modifier.fillMaxWidth(),enabled=answer.trim().equals("moon",true),onClick=onWin);Spacer(Modifier.height(8.dp));Text("Challenge validation runs locally in demo mode.",color=Muted,fontSize=10.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())}
    }
}

@Composable fun RewardScreen(t:Treasure,onCollection:()->Unit,onMap:()->Unit) {
    val scale by animateFloatAsState(1f,label="reveal")
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF21194A),Ink))),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
        Chip("ARTIFACT DISCOVERED",Gold.copy(alpha=.16f),Gold);Spacer(Modifier.height(30.dp))
        Box(Modifier.size(230.dp).scale(scale),contentAlignment=Alignment.Center) { Canvas(Modifier.fillMaxSize()){for(i in 1..4)drawCircle(Gold.copy(alpha=.11f),size.minDimension*i/8f,style=Stroke(1.dp.toPx()))};Text(t.emoji,fontSize=104.sp) }
        Text("A new relic is yours",color=TextPrimary,fontSize=27.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(9.dp));Text(t.rewardName,color=Gold,fontSize=17.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(9.dp));Text(t.rewardDescription,color=Muted,fontSize=13.sp,textAlign=TextAlign.Center,lineHeight=20.sp,modifier=Modifier.padding(horizontal=38.dp))
        Spacer(Modifier.height(22.dp));Chip("DEMO COLLECTIBLE · OFF CHAIN",PanelLight,Muted);Spacer(Modifier.height(34.dp))
        PrimaryButton("View my collection",Modifier.fillMaxWidth(.82f),onClick=onCollection);Spacer(Modifier.height(10.dp));TextButton(onClick=onMap){Text("Back to map",color=Muted)}
    }
}
