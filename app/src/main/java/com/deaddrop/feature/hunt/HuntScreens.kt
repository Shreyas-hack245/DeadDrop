package com.deaddrop.feature.home

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.deaddrop.core.theme.*
import com.deaddrop.data.model.Treasure
import com.google.android.gms.location.LocationServices
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

@Composable fun DetailScreen(t:Treasure,onBack:()->Unit,onStart:()->Unit) {
    Column(Modifier.fillMaxSize().background(Ink)) {
        ScreenHeader("Discovery details",t.category,onBack,trailing={Chip(t.rarity,SoftGreen,Gold)})
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=24.dp)) {
            Box(Modifier.fillMaxWidth().height(245.dp).clip(RoundedCornerShape(20.dp))) {
                CollectibleArtwork(t.id,Modifier.fillMaxSize(),dark=true)
                Box(Modifier.align(Alignment.BottomStart).padding(15.dp).clip(RoundedCornerShape(10.dp)).background(Panel.copy(alpha=.94f)).padding(horizontal=11.dp,vertical=8.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Place,null,tint=Forest,modifier=Modifier.size(15.dp));Spacer(Modifier.width(5.dp));Text("NORTH BEACH · ${t.distance}",color=TextPrimary,fontSize=10.sp,fontWeight=FontWeight.Medium)}}
            }
            Spacer(Modifier.height(22.dp));Text(t.category,color=Forest,fontSize=10.sp,fontWeight=FontWeight.SemiBold,letterSpacing=1.45.sp)
            Spacer(Modifier.height(6.dp));Text(t.name,style=MaterialTheme.typography.headlineLarge,color=TextPrimary)
            Spacer(Modifier.height(12.dp));Text("A place worth finding, a story worth keeping.",color=Muted,fontSize=14.sp,lineHeight=21.sp)
            Spacer(Modifier.height(20.dp));DarkCard(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.AutoAwesome,null,tint=Gold,modifier=Modifier.size(18.dp));Spacer(Modifier.width(9.dp));Text("A clue to begin",color=TextPrimary,fontSize=15.sp,fontWeight=FontWeight.SemiBold)}
                Spacer(Modifier.height(10.dp));Text("“${t.clue}”",color=Muted,fontSize=15.sp,lineHeight=23.sp)
            }}
            Spacer(Modifier.height(16.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)){Stat("DISTANCE",t.distance,Modifier.weight(1f));Stat("RARITY",t.rarity,Modifier.weight(1f));Stat("REWARD","Artifact",Modifier.weight(1f))}
            Spacer(Modifier.height(22.dp));Text("The discovery",style=MaterialTheme.typography.titleLarge,color=TextPrimary);Spacer(Modifier.height(9.dp))
            StepRow("01","Follow the clue toward the public discovery zone");StepRow("02","Check device distance and scan the marker");StepRow("03","Solve the field note to keep your artifact")
            Spacer(Modifier.height(24.dp))
        }
        Column(Modifier.fillMaxWidth().background(Ink).padding(horizontal=24.dp,vertical=12.dp)){PrimaryButton("Start discovery",Modifier.fillMaxWidth(),onClick=onStart);Text("Demo coordinates · stay in public places",color=Muted,fontSize=10.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(top=8.dp))}
    }
}

@Composable private fun Stat(label:String,value:String,modifier:Modifier=Modifier){Column(modifier.clip(RoundedCornerShape(14.dp)).background(Panel).border(1.dp,Border,RoundedCornerShape(14.dp)).padding(12.dp)){Text(label,color=Muted,fontSize=9.sp,fontWeight=FontWeight.SemiBold,letterSpacing=.65.sp);Spacer(Modifier.height(7.dp));Text(value,color=TextPrimary,fontSize=12.sp,fontWeight=FontWeight.SemiBold,maxLines=1)} }
@Composable private fun StepRow(n:String,text:String){Row(Modifier.fillMaxWidth().padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically){Text(n,color=Gold,fontSize=11.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.width(14.dp));Text(text,color=Muted,fontSize=13.sp)} }

@Composable fun HuntScreen(t:Treasure,onBack:()->Unit,onScan:()->Unit) {
    var permissionGranted by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var demoArrival by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var distanceMeters by remember { mutableStateOf<Float?>(null) }
    var locationMessage by remember { mutableStateOf("Your location has not been checked") }
    val context=androidx.compose.ui.platform.LocalContext.current
    fun checkDistance(){checking=true;locationMessage="Checking device location…";readDistance(context,t){meters,error->checking=false;distanceMeters=meters;locationMessage=error?:if(meters!=null&&meters<=100f)"Within the 100 m discovery radius" else "Outside the discovery radius"}}
    val permission=rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()){result->
        permissionGranted=result[android.Manifest.permission.ACCESS_FINE_LOCATION]==true||result[android.Manifest.permission.ACCESS_COARSE_LOCATION]==true
        permissionDenied=!permissionGranted
        if(permissionGranted)checkDistance() else locationMessage="Location permission denied. Demo mode remains available."
    }
    val actualNearby=distanceMeters?.let{it<=100f}==true
    val progress by animateFloatAsState(if(actualNearby)1f else distanceMeters?.let{(1f-it/1500f).coerceIn(.06f,.94f)}?:.12f,tween(500),label="distanceProgress")
    Column(Modifier.fillMaxSize().background(Ink)){
        ScreenHeader("Find the place","LOCATION CHECK",onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=24.dp)){
            DarkCard(Modifier.fillMaxWidth()){Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically){CollectibleArtwork(t.id,Modifier.size(58.dp));Spacer(Modifier.width(13.dp));Column(Modifier.weight(1f)){Text(t.name,color=TextPrimary,fontSize=14.sp,fontWeight=FontWeight.SemiBold);Text("Seeded coordinates · San Francisco",color=Muted,fontSize=11.sp)};Text(t.distance,color=Forest,fontSize=12.sp,fontWeight=FontWeight.SemiBold)}}
            Spacer(Modifier.height(18.dp));Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(Panel).border(1.dp,Border,RoundedCornerShape(19.dp)).padding(vertical=24.dp),contentAlignment=Alignment.Center){
                Column(horizontalAlignment=Alignment.CenterHorizontally){
                    Box(Modifier.size(154.dp),contentAlignment=Alignment.Center){Canvas(Modifier.fillMaxSize()){val c=Offset(size.width/2,size.height/2);drawCircle(PanelLight,size.minDimension*.43f,c,style=Stroke(7.dp.toPx()));drawArc(Forest,-90f,360f*progress,false,topLeft=Offset(size.width*.07f,size.height*.07f),size=androidx.compose.ui.geometry.Size(size.width*.86f,size.height*.86f),style=Stroke(7.dp.toPx(),cap=StrokeCap.Round));drawCircle(Gold,5.dp.toPx(),Offset(c.x,c.y-size.minDimension*.27f))};Icon(if(actualNearby)Icons.Default.Check else Icons.Default.MyLocation,null,tint=Forest,modifier=Modifier.size(35.dp))}
                    Spacer(Modifier.height(17.dp));Text(when{demoArrival->"Demo arrival simulated";actualNearby->"Within the discovery radius";distanceMeters!=null->"${distanceMeters!!.toInt()} m away";else->"Ready to check your location"},color=TextPrimary,fontSize=16.sp,fontWeight=FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp));Text(if(demoArrival)"This is a demo step, not a verified discovery." else locationMessage,color=Muted,fontSize=12.sp,textAlign=TextAlign.Center,modifier=Modifier.padding(horizontal=22.dp))
                }
            }
            Spacer(Modifier.height(13.dp));Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Info,null,tint=Muted,modifier=Modifier.size(16.dp));Spacer(Modifier.width(7.dp));Text("Device distance is approximate and is not server validated.",color=Muted,fontSize=11.sp)}
            if(permissionDenied){Spacer(Modifier.height(10.dp));Text("Location access was denied. You can retry or use the labeled demo arrival.",color=Color(0xFF9C4D42),fontSize=12.sp,lineHeight=18.sp)}
            Spacer(Modifier.height(18.dp))
        }
        Column(Modifier.padding(horizontal=24.dp,vertical=12.dp)){
            if(demoArrival||actualNearby)PrimaryButton("Continue to QR scan",Modifier.fillMaxWidth(),onClick=onScan)
            else PrimaryButton(if(checking)"Checking location…" else if(permissionGranted)"Refresh device location" else "Allow location access",Modifier.fillMaxWidth(),enabled=!checking,onClick={if(permissionGranted)checkDistance()else permission.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION))})
            if(!demoArrival&&!actualNearby)TextButton(onClick={demoArrival=true},modifier=Modifier.fillMaxWidth()){Text("Simulate arrival in demo mode",color=Forest,fontWeight=FontWeight.Medium)}
        }
    }
}

private fun readDistance(context:Context,treasure:Treasure,callback:(Float?,String?)->Unit){
    val fine=ContextCompat.checkSelfPermission(context,android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
    val coarse=ContextCompat.checkSelfPermission(context,android.Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
    if(!fine&&!coarse){callback(null,"Location permission is required");return}
    try{LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener{location->
        if(location==null)callback(null,"No recent device location is available")else{val result=FloatArray(1);Location.distanceBetween(location.latitude,location.longitude,treasure.latitude,treasure.longitude,result);callback(result[0],null)}
    }.addOnFailureListener{callback(null,"Could not read device location")}}catch(_:SecurityException){callback(null,"Location permission was revoked")}
}

@Composable fun ScanScreen(t:Treasure,onBack:()->Unit,onPuzzle:()->Unit){
    var cameraEnabled by remember{mutableStateOf(false)}
    var permissionDenied by remember{mutableStateOf(false)}
    var message by remember{mutableStateOf("Align this hunt’s marker in the frame")}
    val permission=rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()){granted->cameraEnabled=granted;permissionDenied=!granted;if(!granted)message="Camera access denied. You can still use the sample challenge."}
    Column(Modifier.fillMaxSize().background(Ink)){
        ScreenHeader("Scan the marker","QR CHALLENGE",onBack)
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal=18.dp).clip(RoundedCornerShape(20.dp)).background(if(cameraEnabled)Color(0xFF202522) else Panel).border(1.dp,Border,RoundedCornerShape(20.dp)),contentAlignment=Alignment.Center){
            if(cameraEnabled){
                CameraQrPreview(expected="DD-${t.id.uppercase()}-DEMO",modifier=Modifier.fillMaxSize(),onValid={message="Sample marker recognized";onPuzzle()},onError={message=it})
                ScanFrame(Modifier.fillMaxSize().padding(48.dp))
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha=.58f)).padding(14.dp),horizontalAlignment=Alignment.CenterHorizontally){Chip("CAMERAX · ML KIT",Color.White.copy(alpha=.14f),Color.White);Spacer(Modifier.height(7.dp));Text(message,color=Color.White,fontSize=11.sp,textAlign=TextAlign.Center)}
            }else Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.padding(28.dp)){
                Box(Modifier.size(64.dp).clip(CircleShape).background(SoftGreen),contentAlignment=Alignment.Center){Icon(Icons.Default.QrCodeScanner,null,tint=Forest,modifier=Modifier.size(28.dp))}
                Spacer(Modifier.height(16.dp));Text("Scan the field marker",style=MaterialTheme.typography.titleLarge,color=TextPrimary);Spacer(Modifier.height(7.dp));Text(if(permissionDenied)"Camera permission was denied." else "Camera preview opens only after you allow access.",color=Muted,fontSize=12.sp,textAlign=TextAlign.Center)
            }
            Chip(if(cameraEnabled)"LIVE CAMERA" else "SAMPLE TOKEN · DD-${t.id.uppercase()}-DEMO",if(cameraEnabled)Color.Black.copy(alpha=.55f) else Color.White.copy(alpha=.92f),if(cameraEnabled)Color.White else Muted,modifier=Modifier.align(Alignment.TopCenter).padding(top=15.dp))
        }
        Column(Modifier.padding(horizontal=24.dp,vertical=14.dp)){
            if(!cameraEnabled)PrimaryButton(if(permissionDenied)"Try camera permission again" else "Enable camera",Modifier.fillMaxWidth(),onClick={permission.launch(android.Manifest.permission.CAMERA)})
            else Text("Camera scanning is active · hold the marker in frame",color=Muted,fontSize=11.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(vertical=9.dp))
            TextButton(onClick=onPuzzle,modifier=Modifier.fillMaxWidth()){Text("Use the sample QR challenge",color=Forest,fontWeight=FontWeight.Medium)}
            Text("QR decoding happens on device. Production discovery still requires trusted server validation.",color=Muted,fontSize=10.sp,lineHeight=15.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())
        }
    }
}

@Composable private fun ScanFrame(modifier:Modifier){Canvas(modifier){val c=Forest;val s=size.minDimension;val l=s*.24f;val r=s*.76f;val sw=3.dp.toPx();drawLine(c,Offset(l,0f),Offset(0f,0f),sw);drawLine(c,Offset(0f,0f),Offset(0f,l),sw);drawLine(c,Offset(r,0f),Offset(s,0f),sw);drawLine(c,Offset(s,0f),Offset(s,l),sw);drawLine(c,Offset(l,s),Offset(0f,s),sw);drawLine(c,Offset(0f,s),Offset(0f,r),sw);drawLine(c,Offset(r,s),Offset(s,s),sw);drawLine(c,Offset(s,s),Offset(s,r),sw)}}

@Composable private fun CameraQrPreview(expected:String,modifier:Modifier,onValid:()->Unit,onError:(String)->Unit){
    val context=androidx.compose.ui.platform.LocalContext.current;val lifecycleOwner=LocalLifecycleOwner.current
    val previewView=remember{PreviewView(context).apply{scaleType=PreviewView.ScaleType.FILL_CENTER;implementationMode=PreviewView.ImplementationMode.COMPATIBLE}}
    AndroidView(factory={previewView},modifier=modifier)
    DisposableEffect(lifecycleOwner,expected){
        val executor=ContextCompat.getMainExecutor(context);val scanner=BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build());var provider:ProcessCameraProvider?=null;var handled=false
        val future=ProcessCameraProvider.getInstance(context)
        future.addListener({try{provider=future.get();val preview=Preview.Builder().build().also{it.surfaceProvider=previewView.surfaceProvider};val analysis=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analysis.setAnalyzer(executor){proxy->val media=proxy.image;if(media==null)proxy.close()else scanner.process(InputImage.fromMediaImage(media,proxy.imageInfo.rotationDegrees)).addOnSuccessListener{codes->val value=codes.firstOrNull()?.rawValue;if(value==expected&&!handled){handled=true;onValid()}else if(value!=null&&value!=expected)onError("QR found, but it belongs to a different hunt")}.addOnFailureListener{onError("Could not read QR image")}.addOnCompleteListener{proxy.close()}}
            provider?.unbindAll();provider?.bindToLifecycle(lifecycleOwner,CameraSelector.DEFAULT_BACK_CAMERA,preview,analysis)
        }catch(_:Exception){onError("Camera could not be started on this device")}},executor)
        onDispose{provider?.unbindAll();scanner.close()}
    }
}

@Composable fun PuzzleScreen(t:Treasure,answer:String,onAnswer:(String)->Unit,onBack:()->Unit,onWin:()->Unit){
    Column(Modifier.fillMaxSize().background(Ink)){
        ScreenHeader("The field note","CHALLENGE 03 OF 03",onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=24.dp)){
            CollectibleArtwork(t.id,Modifier.fillMaxWidth().height(190.dp),dark=true)
            Spacer(Modifier.height(21.dp));Text("A quiet answer",style=MaterialTheme.typography.headlineLarge,color=TextPrimary)
            Spacer(Modifier.height(10.dp));Text(t.clue,color=Muted,fontSize=14.sp,lineHeight=22.sp)
            Spacer(Modifier.height(10.dp));Text("Name the word hidden in the clue to complete this demo hunt.",color=TextPrimary,fontSize=13.sp,lineHeight=20.sp)
            Spacer(Modifier.height(20.dp));OutlinedTextField(value=answer,onValueChange=onAnswer,modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(13.dp),label={Text("Your answer")},placeholder={Text("Enter a word",color=Muted)},colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Forest,unfocusedBorderColor=Border,focusedTextColor=TextPrimary,unfocusedTextColor=TextPrimary,cursorColor=Forest,focusedContainerColor=Panel,unfocusedContainerColor=Panel))
            Spacer(Modifier.height(10.dp));Chip("DEMO HINT · MOON",SoftGreen,Gold)
        }
        Column(Modifier.padding(horizontal=24.dp,vertical=14.dp)){PrimaryButton("Complete discovery",Modifier.fillMaxWidth(),enabled=answer.trim().equals("moon",true),onClick=onWin);Text("Answer checking is local in demo mode.",color=Muted,fontSize=10.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(top=8.dp))}
    }
}

@Composable fun RewardScreen(t:Treasure,onCollection:()->Unit,onMap:()->Unit){
    var reveal by remember{mutableStateOf(false)};LaunchedEffect(Unit){reveal=true}
    val scale by animateFloatAsState(if(reveal)1f else .92f,tween(600),label="artifactReveal")
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.CenterHorizontally){
        ScreenHeader("Discovery complete","YOUR FIELD JOURNAL")
        Spacer(Modifier.height(8.dp));Chip("ADDED TO YOUR COLLECTION",SoftGreen,Forest);Spacer(Modifier.height(19.dp))
        Box(Modifier.padding(horizontal=27.dp).fillMaxWidth().height(315.dp).scale(scale).clip(RoundedCornerShape(22.dp))){CollectibleArtwork(t.id,Modifier.fillMaxSize(),dark=true);Chip("${t.rarity} · OFF CHAIN",Color.Black.copy(alpha=.55f),Color(0xFFD8C6A2),modifier=Modifier.align(Alignment.TopStart).padding(15.dp))}
        Spacer(Modifier.height(21.dp));Text(t.rewardName,style=MaterialTheme.typography.headlineLarge,color=TextPrimary,textAlign=TextAlign.Center)
        Spacer(Modifier.height(7.dp));Text("${t.category}  ·  ${t.rarity}",color=Gold,fontSize=10.sp,fontWeight=FontWeight.SemiBold,letterSpacing=1.sp)
        Spacer(Modifier.height(11.dp));Text(t.rewardDescription,color=Muted,fontSize=14.sp,lineHeight=21.sp,textAlign=TextAlign.Center,modifier=Modifier.padding(horizontal=34.dp))
        Spacer(Modifier.height(15.dp));Text("Saved to your local demo collection. NFT minting is unavailable until a wallet and testnet service are configured.",color=Muted,fontSize=11.sp,lineHeight=17.sp,textAlign=TextAlign.Center,modifier=Modifier.padding(horizontal=35.dp))
        Spacer(Modifier.height(21.dp));PrimaryButton("View collection",Modifier.fillMaxWidth().padding(horizontal=24.dp),onClick=onCollection);TextButton(onClick=onMap){Text("Return to explore",color=Forest)};Spacer(Modifier.height(12.dp))
    }
}
