package com.example.shelflife

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class MonthlyUsage(
    val key: Int,
    val month: String,
    val used: Int,
    val wasted: Int
)

@Composable
fun InsightsScreen(
    pantryItems: List<PantryItem>
) {

    val usedItems = pantryItems.filter {
        it.usageStatus == STATUS_USED
    }

    val wastedItems = pantryItems.filter {
        it.usageStatus == STATUS_WASTED
    }

    val completedCount = usedItems.size + wastedItems.size

    val wastePercentage =
        if (completedCount > 0) {
            (wastedItems.size * 100) / completedCount
        } else {
            0
        }

    val currentMonthKey = remember {
        val calendar = Calendar.getInstance()
        calendar.get(Calendar.YEAR) * 12 +
                calendar.get(Calendar.MONTH)
    }

    val currentMonthUsed = usedItems.count {
        it.updatedAt != null &&
                getMonthKey(it.updatedAt) == currentMonthKey
    }

    val currentMonthWasted = wastedItems.count {
        it.updatedAt != null &&
                getMonthKey(it.updatedAt) == currentMonthKey
    }

    val monthlyUsage = remember(pantryItems) {

        val result = mutableListOf<MonthlyUsage>()

        val calendar = Calendar.getInstance()

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        calendar.add(Calendar.MONTH, -5)

        val formatter = SimpleDateFormat(
            "MMM",
            Locale.getDefault()
        )

        repeat(6) {

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)

            val key = year * 12 + month

            val used = usedItems.count { item ->
                item.updatedAt != null &&
                        getMonthKey(item.updatedAt) == key
            }

            val wasted = wastedItems.count { item ->
                item.updatedAt != null &&
                        getMonthKey(item.updatedAt) == key
            }

            result.add(
                MonthlyUsage(
                    key = key,
                    month = formatter.format(calendar.time),
                    used = used,
                    wasted = wasted
                )
            )

            calendar.add(Calendar.MONTH, 1)
        }

        result
    }

    val maximumMonthlyCount = maxOf(
        monthlyUsage.maxOfOrNull { it.used } ?: 0,
        monthlyUsage.maxOfOrNull { it.wasted } ?: 0,
        1
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Text(
            text = "Food Insights",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreen
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Understand your food habits and reduce waste.",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // OVERALL STATISTICS

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            InsightStatCard(
                title = "Items Used",
                count = usedItems.size,
                background = SoftSage,
                modifier = Modifier.weight(1f)
            )

            InsightStatCard(
                title = "Items Wasted",
                count = wastedItems.size,
                background = Color(0xFFFFE8DF),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // WASTE PERCENTAGE

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {

            Column(
                modifier = Modifier.padding(18.dp)
            ) {

                Text(
                    text = "Food Waste Rate",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "$wastePercentage%",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (wastePercentage > 50) {
                        Color(0xFFB85C38)
                    } else {
                        PrimaryGreen
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (completedCount == 0) {
                        "Mark items as Used or Wasted to see your waste rate."
                    } else {
                        "Percentage of completed items that were wasted."
                    },
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(14.dp))

                LinearProgressIndicator(
                    progress = {
                        wastePercentage / 100f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp),
                    color = Color(0xFFB85C38),
                    trackColor = SoftSage
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$completedCount completed items recorded",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // CURRENT MONTH

        Text(
            text = "This Month",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreen
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            InsightStatCard(
                title = "Used",
                count = currentMonthUsed,
                background = SoftSage,
                modifier = Modifier.weight(1f)
            )

            InsightStatCard(
                title = "Wasted",
                count = currentMonthWasted,
                background = Color(0xFFFFE8DF),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // MONTHLY CHART

        Text(
            text = "Monthly Comparison",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreen
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Used vs Wasted items over the last 6 months",
            fontSize = 13.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    PrimaryGreen,
                                    RoundedCornerShape(3.dp)
                                )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text("Used", fontSize = 12.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    Color(0xFFB85C38),
                                    RoundedCornerShape(3.dp)
                                )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text("Wasted", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                monthlyUsage.forEach { month ->

                    Text(
                        text = month.month,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkGreen,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    MonthlyBar(
                        count = month.used,
                        maximum = maximumMonthlyCount,
                        color = PrimaryGreen
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    MonthlyBar(
                        count = month.wasted,
                        maximum = maximumMonthlyCount,
                        color = Color(0xFFB85C38)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Used: ${month.used}  •  Wasted: ${month.wasted}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Small steps today can mean less food waste tomorrow. 🌿",
            fontSize = 14.sp,
            color = DarkGreen,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// MONTH KEY HELPER

private fun getMonthKey(timestamp: Long): Int {

    val calendar = Calendar.getInstance().apply {
        timeInMillis = timestamp
    }

    return calendar.get(Calendar.YEAR) * 12 +
            calendar.get(Calendar.MONTH)
}

// INSIGHT STAT CARD

@Composable
private fun InsightStatCard(
    title: String,
    count: Int,
    background: Color,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = background
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = count.toString(),
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                color = DarkGreen
            )
        }
    }
}

// MONTHLY BAR

@Composable
private fun MonthlyBar(
    count: Int,
    maximum: Int,
    color: Color
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .weight(1f)
                .height(9.dp)
                .background(
                    SoftSage,
                    RoundedCornerShape(10.dp)
                )
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth(
                        if (maximum > 0) {
                            (count.toFloat() / maximum)
                                .coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                    )
                    .fillMaxHeight()
                    .background(
                        color,
                        RoundedCornerShape(10.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = count.toString(),
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

