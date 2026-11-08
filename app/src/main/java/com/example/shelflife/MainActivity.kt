
package com.example.shelflife

import android.app.DatePickerDialog
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// APP COLORS

val PrimaryGreen = Color(0xFF397A45)
val DarkGreen = Color(0xFF1F4D2B)
val SoftSage = Color(0xFFE5EFD9)
val AppBackground = Color(0xFFFAFBF6)

// USAGE STATUS

const val STATUS_ACTIVE = "ACTIVE"
const val STATUS_USED = "USED"
const val STATUS_WASTED = "WASTED"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                ShelfLifeApp()
            }
        }
    }
}

// MAIN APP

@Composable
fun ShelfLifeApp() {

    var showAddForm by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<PantryItem?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    val context = LocalContext.current

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { }

    LaunchedEffect(Unit) {
        NotificationHelper.createNotificationChannel(context)
        ExpiryWorkScheduler.schedule(context.applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            val permissionGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

            if (!permissionGranted) {
                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    val database = remember {
        PantryDatabase.getDatabase(context)
    }

    val pantryDao = database.pantryDao()

    val pantryItems by pantryDao.getAllItems()
        .collectAsState(initial = emptyList())

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = AppBackground,

        bottomBar = {

            if (!showAddForm) {

                NavigationBar(
                    containerColor = Color.White,
                    contentColor = PrimaryGreen,
                    tonalElevation = 8.dp
                ) {

                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },

                        icon = {
                            Text(
                                text = "▦",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) PrimaryGreen else Color.Gray
                            )
                        },

                        label = {
                            Text(
                                text = "Pantry",
                                fontWeight = if (selectedTab == 0) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                }
                            )
                        },

                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryGreen,
                            selectedTextColor = PrimaryGreen,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = SoftSage
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },

                        icon = {
                            Text(
                                text = "◔",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 1) PrimaryGreen else Color.Gray
                            )
                        },

                        label = {
                            Text(
                                text = "Insights",
                                fontWeight = if (selectedTab == 1) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                }
                            )
                        },

                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryGreen,
                            selectedTextColor = PrimaryGreen,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = SoftSage
                        )
                    )
                }
            }
        }

    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            AnimatedContent(
                targetState = if (showAddForm) -1 else selectedTab,

                transitionSpec = {

                    val movingForward = targetState > initialState

                    (
                            fadeIn(animationSpec = tween(250)) +
                                    slideInHorizontally(
                                        animationSpec = tween(250)
                                    ) { width ->
                                        if (movingForward) width / 10 else -width / 10
                                    }
                            ) togetherWith (
                            fadeOut(animationSpec = tween(180)) +
                                    slideOutHorizontally(
                                        animationSpec = tween(180)
                                    ) { width ->
                                        if (movingForward) -width / 10 else width / 10
                                    }
                            )
                },

                label = "screen_transition",
                contentKey = { it }

            ) { screen ->

                when (screen) {

                    -1 -> {

                        AddItemScreen(
                            initialItem = editingItem,

                            onBack = {
                                showAddForm = false
                                editingItem = null
                            },

                            onSave = { item ->

                                coroutineScope.launch {

                                    if (item.id == 0) {
                                        pantryDao.insertItem(item)
                                    } else {
                                        pantryDao.updateItem(item)
                                    }

                                    showAddForm = false
                                    editingItem = null
                                }
                            }
                        )
                    }

                    0 -> {

                        DashboardScreen(
                            pantryItems = pantryItems,

                            onAddClick = {
                                editingItem = null
                                showAddForm = true
                            },

                            onEditClick = { item ->
                                editingItem = item
                                showAddForm = true
                            },

                            onDeleteClick = { item ->
                                coroutineScope.launch {
                                    pantryDao.deleteItem(item)
                                }
                            },

                            onUsageStatusChange = { item, newStatus ->

                                coroutineScope.launch {

                                    pantryDao.updateItem(
                                        item.copy(
                                            usageStatus = newStatus,
                                            updatedAt = System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                        )
                    }

                    else -> {

                        InsightsScreen(
                            pantryItems = pantryItems
                        )
                    }
                }
            }
        }
    }
}

// EXPIRY STATUS

fun getExpiryStatus(expiryDate: String): Pair<String, Color> {

    val formatter = SimpleDateFormat(
        "dd/MM/yyyy",
        Locale.getDefault()
    ).apply {
        isLenient = false
    }

    if (!expiryDate.matches(Regex("\\d{1,2}/\\d{1,2}/\\d{4}"))) {
        return Pair("Invalid Date", Color.Gray)
    }

    val expiry = try {
        formatter.parse(expiryDate)
    } catch (e: Exception) {
        null
    } ?: return Pair("Invalid Date", Color.Gray)

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val expiryDay = Calendar.getInstance().apply {
        time = expiry
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val daysLeft =
        (expiryDay.timeInMillis - today.timeInMillis) /
                (24 * 60 * 60 * 1000L)

    return when {
        daysLeft < 0 -> Pair("Expired", Color.Red)
        daysLeft <= 3 -> Pair("Expiring Soon", Color(0xFFE59A00))
        else -> Pair("Fresh", PrimaryGreen)
    }
}

// DASHBOARD SCREEN


@Composable
fun DashboardScreen(
    pantryItems: List<PantryItem>,
    onAddClick: () -> Unit,
    onEditClick: (PantryItem) -> Unit,
    onDeleteClick: (PantryItem) -> Unit,
    onUsageStatusChange: (PantryItem, String) -> Unit
) {

    var itemToDelete by remember { mutableStateOf<PantryItem?>(null) }
    var itemToUpdate by remember { mutableStateOf<PantryItem?>(null) }
    var pendingStatus by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val activeItems = pantryItems.filter {
        it.usageStatus == STATUS_ACTIVE
    }

    val totalItems = activeItems.size

    val expiringSoonCount = activeItems.count {
        getExpiryStatus(it.expiryDate).first == "Expiring Soon"
    }

    val expiredCount = activeItems.count {
        getExpiryStatus(it.expiryDate).first == "Expired"
    }

    val categories = remember(activeItems) {
        listOf("All") +
                activeItems
                    .map { it.category.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .sorted()
    }

    val filteredItems = activeItems.filter { item ->

        val matchesSearch =
            item.name.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true) ||
                    item.quantity.contains(searchQuery, ignoreCase = true)

        val matchesCategory =
            selectedCategory == "All" ||
                    item.category.equals(selectedCategory, ignoreCase = true)

        matchesSearch && matchesCategory
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),

        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {

        // HEADER

        item {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "ShelfLife",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            )

            Text(
                text = "A little less waste, one meal at a time.",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        // WELCOME CARD

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),

                colors = CardDefaults.cardColors(
                    containerColor = SoftSage
                ),

                shape = RoundedCornerShape(18.dp)
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "Welcome to your pantry!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Keep track of your food and reduce waste.",
                        color = DarkGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // STATISTICS

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                StatCard(
                    title = "Total Items",
                    count = totalItems,
                    background = SoftSage,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Expiring Soon",
                    count = expiringSoonCount,
                    background = Color(0xFFFFF1D6),
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Expired",
                    count = expiredCount,
                    background = Color(0xFFFFE2E2),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // PANTRY HEADING AND ADD BUTTON

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Your Pantry",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen
                )

                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+ Add Item")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // SEARCH BAR

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Search your pantry...")
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        // CATEGORY FILTERS

        item {
            Text(
                text = "Categories",
                fontWeight = FontWeight.SemiBold,
                color = DarkGreen,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                items(categories) { category ->

                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = category
                        },
                        label = {
                            Text(category)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SoftSage,
                            selectedLabelColor = DarkGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // EMPTY STATE OR PANTRY ITEMS

        if (filteredItems.isEmpty()) {

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),

                    contentAlignment = Alignment.Center
                ) {

                    if (activeItems.isEmpty()) {

                        EmptyStateCard(
                            emoji = "🥬",
                            title = "Your pantry is waiting!",
                            message = "Start adding your food items and keep track of their freshness.",
                            buttonText = "Add Your First Item",
                            onClick = onAddClick
                        )

                    } else {

                        EmptyStateCard(
                            emoji = "🔎",
                            title = "No items found",
                            message = "We couldn't find anything matching your search or selected category.",
                            buttonText = "Clear Search & Filters",
                            onClick = {
                                searchQuery = ""
                                selectedCategory = "All"
                            }
                        )
                    }
                }
            }

        } else {

            items(
                items = filteredItems,
                key = { it.id }
            ) { item ->

                PantryItemCard(
                    item = item,

                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .animateItem(
                            fadeInSpec = tween(250),
                            fadeOutSpec = tween(200),
                            placementSpec = tween(300)
                        ),

                    onEditClick = {
                        onEditClick(item)
                    },

                    onDeleteClick = {
                        itemToDelete = item
                    },

                    onUsedClick = {
                        itemToUpdate = item
                        pendingStatus = STATUS_USED
                    },

                    onWastedClick = {
                        itemToUpdate = item
                        pendingStatus = STATUS_WASTED
                    }
                )
            }
        }

        // BOTTOM SPACING

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // DELETE CONFIRMATION

    if (itemToDelete != null) {

        AlertDialog(
            onDismissRequest = {
                itemToDelete = null
            },

            title = {
                Text("Delete Item?")
            },

            text = {
                Text(
                    "Are you sure you want to permanently delete ${itemToDelete?.name}?"
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        itemToDelete?.let {
                            onDeleteClick(it)
                        }
                        itemToDelete = null
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        itemToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // USAGE STATUS CONFIRMATION

    if (itemToUpdate != null) {

        val actionText =
            if (pendingStatus == STATUS_USED) "Used" else "Wasted"

        AlertDialog(
            onDismissRequest = {
                itemToUpdate = null
                pendingStatus = ""
            },

            title = {
                Text("Mark as $actionText?")
            },

            text = {
                Text(
                    "Do you want to mark ${itemToUpdate?.name} as $actionText? It will be removed from your active pantry but saved in your history."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        itemToUpdate?.let {
                            onUsageStatusChange(it, pendingStatus)
                        }

                        itemToUpdate = null
                        pendingStatus = ""
                    }
                ) {
                    Text("Confirm", color = PrimaryGreen)
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        itemToUpdate = null
                        pendingStatus = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}


// EMPTY STATE CARD

@Composable
fun EmptyStateCard(
    emoji: String,
    title: String,
    message: String,
    buttonText: String,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .animateContentSize(),

        shape = RoundedCornerShape(22.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(
                        SoftSage,
                        RoundedCornerShape(24.dp)
                    ),

                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 36.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                fontSize = 13.sp,
                color = Color.Gray,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onClick,

                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                ),

                shape = RoundedCornerShape(12.dp)
            ) {

                Text(
                    text = buttonText,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// STATISTICS CARD

@Composable
fun StatCard(
    title: String,
    count: Int,
    background: Color,
    modifier: Modifier = Modifier
) {

    val subtitle = when (title) {
        "Total Items" -> "In pantry"
        "Expiring Soon" -> "Within 3 days"
        "Expired" -> "Past expiry"
        else -> ""
    }

    Card(
        modifier = modifier
            .heightIn(min = 112.dp)
            .animateContentSize(),

        colors = CardDefaults.cardColors(
            containerColor = background
        ),

        shape = RoundedCornerShape(18.dp),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 14.dp),

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            AnimatedContent(
                targetState = count,

                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                },

                label = "stat_count"

            ) { animatedCount ->

                Text(
                    text = animatedCount.toString(),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DarkGreen
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreen,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = DarkGreen.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}

// PANTRY ITEM CARD

@Composable
fun PantryItemCard(
    item: PantryItem,
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUsedClick: () -> Unit,
    onWastedClick: () -> Unit
) {

    val status = getExpiryStatus(item.expiryDate)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = item.name,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text("Quantity: ${item.quantity}")
            Text("Category: ${item.category}")
            Text("Expiry Date: ${item.expiryDate}")

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = status.second.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            ) {

                Text(
                    text = status.first,
                    color = status.second,
                    fontWeight = FontWeight.Bold,

                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                OutlinedButton(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit", color = PrimaryGreen)
                }

                OutlinedButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.weight(1f),

                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Text("Delete")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Button(
                    onClick = onUsedClick,
                    modifier = Modifier.weight(1f),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen
                    )
                ) {
                    Text("✓ Used")
                }

                Button(
                    onClick = onWastedClick,
                    modifier = Modifier.weight(1f),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB85C38)
                    )
                ) {
                    Text("Wasted")
                }
            }
        }
    }
}

// ADD AND EDIT ITEM SCREEN

@Composable
fun AddItemScreen(
    initialItem: PantryItem? = null,
    onBack: () -> Unit,
    onSave: (PantryItem) -> Unit
) {

    var name by remember(initialItem) {
        mutableStateOf(initialItem?.name ?: "")
    }

    var quantity by remember(initialItem) {
        mutableStateOf(initialItem?.quantity ?: "")
    }

    var category by remember(initialItem) {
        mutableStateOf(initialItem?.category ?: "")
    }

    var expiryDate by remember(initialItem) {
        mutableStateOf(initialItem?.expiryDate ?: "")
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    val context = LocalContext.current

    // OPEN ANDROID CALENDAR

    fun openExpiryDatePicker() {

        val calendar = Calendar.getInstance()

        // When editing, open the calendar on the saved expiry date.
        if (expiryDate.isNotBlank()) {

            try {

                val formatter = SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                ).apply {
                    isLenient = false
                }

                val existingDate = formatter.parse(expiryDate)

                if (existingDate != null) {
                    calendar.time = existingDate
                }

            } catch (_: Exception) {
                // Use today's date if the saved date is invalid.
            }
        }

        DatePickerDialog(
            context,

            { _, year, month, dayOfMonth ->

                expiryDate = String.format(
                    Locale.getDefault(),
                    "%02d/%02d/%04d",
                    dayOfMonth,
                    month + 1,
                    year
                )

                errorMessage = ""
            },

            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)

        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        TextButton(onClick = onBack) {
            Text("← Back", color = PrimaryGreen)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (initialItem == null) {
                "Add Pantry Item"
            } else {
                "Edit Pantry Item"
            },

            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreen
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Food Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Quantity") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // CLICKABLE EXPIRY DATE FIELD

        OutlinedButton(
            onClick = {
                openExpiryDatePicker()
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),

            shape = RoundedCornerShape(12.dp),

            contentPadding = PaddingValues(
                horizontal = 16.dp
            ),

            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = DarkGreen
            )
        ) {

            Text(
                text = if (expiryDate.isBlank()) {
                    "Select Expiry Date"
                } else {
                    expiryDate
                },

                modifier = Modifier.weight(1f),

                color = if (expiryDate.isBlank()) {
                    Color.Gray
                } else {
                    DarkGreen
                },

                textAlign = TextAlign.Start
            )

            Text(
                text = "📅",
                fontSize = 20.sp
            )
        }

        if (errorMessage.isNotEmpty()) {

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {

                when {

                    name.isBlank() ||
                            quantity.isBlank() ||
                            category.isBlank() ||
                            expiryDate.isBlank() -> {

                        errorMessage = "Please fill in all fields."
                    }

                    getExpiryStatus(expiryDate).first == "Invalid Date" -> {

                        errorMessage = "Please select a valid expiry date."
                    }

                    else -> {

                        errorMessage = ""

                        onSave(
                            PantryItem(
                                id = initialItem?.id ?: 0,
                                name = name.trim(),
                                quantity = quantity.trim(),
                                category = category.trim(),
                                expiryDate = expiryDate.trim(),

                                usageStatus = initialItem?.usageStatus
                                    ?: STATUS_ACTIVE,

                                updatedAt = initialItem?.updatedAt
                            )
                        )
                    }
                }
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),

            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGreen
            ),

            shape = RoundedCornerShape(12.dp)

        ) {

            Text(
                text = if (initialItem == null) {
                    "Save Item"
                } else {
                    "Update Item"
                },

                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
