package com.example.snap_app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// API key - add PLACES_API_KEY to local.properties for production
private const val PLACES_API_KEY = "AIzaSyDGGYNtQlrc-SoJjwPFvuVhc15huXyCceA"

data class PlaceResult(
    val placeId: String,
    val name: String,
    val vicinity: String,
    val rating: Double?,
    val userRatingsTotal: Int?,
    val openNow: Boolean?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonutShopsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var permissionGranted by remember { mutableStateOf(false) }
    var locationStatus by remember { mutableStateOf<String?>(null) }
    var donutShops by remember { mutableStateOf<List<PlaceResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        permissionGranted = fineGranted || coarseGranted
        if (permissionGranted) {
            locationStatus = null
            errorMessage = null
        } else {
            locationStatus = "Location permission denied"
            errorMessage = "Location access is required to find nearby donut shops."
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        permissionGranted = hasFine || hasCoarse
        if (!permissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun fetchDonutShops() {
        if (!permissionGranted) {
            errorMessage = "Please grant location permission first."
            return
        }
        scope.launch {
            isLoading = true
            errorMessage = null
            locationStatus = "Getting your location..."
            try {
                val location = withContext(Dispatchers.IO) {
                    getLastKnownLocation(context)
                }
                if (location != null) {
                    locationStatus = "Searching for donut shops..."
                    val shops = withContext(Dispatchers.IO) {
                        fetchNearbyDonutShops(location.first, location.second)
                    }
                    donutShops = shops
                    locationStatus = if (shops.isEmpty()) "No donut shops found nearby" else null
                } else {
                    errorMessage = "Could not get your location. Please ensure GPS is enabled."
                }
            } catch (e: Exception) {
                Log.e("DonutShops", "Error fetching donut shops", e)
                errorMessage = e.message ?: "Failed to fetch donut shops"
            }
            isLoading = false
        }
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted && donutShops.isEmpty() && !isLoading) {
            fetchDonutShops()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NeonPink,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = "Donut Shops Nearby",
                style = MaterialTheme.typography.headlineMedium,
                color = NeonPink,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Find the nearest donut shops from your location",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = errorMessage!!,
                    modifier = Modifier.padding(16.dp),
                    color = Color.White
                )
            }
        }

        if (locationStatus != null && errorMessage == null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = locationStatus!!,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (!permissionGranted && errorMessage == null) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Location permission needed",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "We need access to your location to find donut shops nearby.",
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                    ) {
                        Text("Grant permission")
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { fetchDonutShops() },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isLoading) "Searching..." else "Refresh")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading && donutShops.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonPink)
                }
            } else if (donutShops.isEmpty()) {
                Text(
                    text = "No donut shops found. Try refreshing or check your location.",
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = donutShops,
                        key = { it.placeId.ifEmpty { it.name + it.vicinity } }
                    ) { place ->
                        DonutShopCard(place = place)
                    }
                }
            }
        }
    }
}

@Composable
fun DonutShopCard(place: PlaceResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = place.name,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            if (place.vicinity.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = place.vicinity,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                place.rating?.let { rating ->
                    Text(
                        text = "★ $rating",
                        color = Color(0xFFFFC107),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    place.userRatingsTotal?.let { total ->
                        Text(
                            text = " ($total reviews)",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                place.openNow?.let { openNow ->
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (openNow) "Open now" else "Closed",
                        color = if (openNow) Color(0xFF4CAF50) else Color(0xFFB71C1C).copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun getLastKnownLocation(context: Context): Pair<Double, Double>? {
    return try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val gpsProvider = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val networkProvider = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        val location = gpsProvider ?: networkProvider
        location?.let { Pair(it.latitude, it.longitude) }
    } catch (e: Exception) {
        Log.e("DonutShops", "Error getting location", e)
        null
    }
}

private fun fetchNearbyDonutShops(lat: Double, lng: Double): List<PlaceResult> {
    if (PLACES_API_KEY.isEmpty()) {
        throw IllegalStateException("Places API key not configured.")
    }
    val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=$lat,$lng&radius=5000&keyword=donut&key=$PLACES_API_KEY"
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 10000
    connection.readTimeout = 10000
    connection.connect()
    val response = connection.inputStream.bufferedReader().use { it.readText() }
    connection.disconnect()

    val json = JSONObject(response)
    val status = json.optString("status", "")
    if (status != "OK" && status != "ZERO_RESULTS") {
        throw Exception("Places API error: $status")
    }
    val results = json.optJSONArray("results") ?: return emptyList()
    val places = mutableListOf<PlaceResult>()
    for (i in 0 until results.length()) {
        val obj = results.getJSONObject(i)
        val openingHours = obj.optJSONObject("opening_hours")
        places.add(
            PlaceResult(
                placeId = obj.optString("place_id", ""),
                name = obj.optString("name", ""),
                vicinity = obj.optString("vicinity", ""),
                rating = if (obj.has("rating")) obj.getDouble("rating") else null,
                userRatingsTotal = if (obj.has("user_ratings_total")) obj.getInt("user_ratings_total") else null,
                openNow = if (openingHours != null && openingHours.has("open_now")) openingHours.optBoolean("open_now") else null
            )
        )
    }
    return places
}
