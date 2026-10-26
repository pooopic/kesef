package com.edan.kesef.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edan.kesef.ui.theme.K
import com.edan.kesef.util.Money
import kotlin.math.roundToInt

/**
 * העיגול הגדול של המסך הראשי.
 *
 * הטבעת מראה "כמה נשאר" מההכנסות של החודש: בתחילת החודש היא מלאה,
 * וכל הוצאה "אוכלת" ממנה. אם הוצאת יותר ממה שנכנס, הטבעת ריקה והיתרה באדום.
 */
@Composable
fun BalanceRing(income: Long, expenses: Long, modifier: Modifier = Modifier) {
    val balance = income - expenses
    val spentRatio = when {
        income > 0 -> (expenses.toFloat() / income).coerceIn(0f, 1f)
        expenses > 0 -> 1f
        else -> 0f
    }
    // אנימציה: הטבעת "מתמלאת/מתרוקנת" בעדינות כשהמספרים משתנים
    val remaining by animateFloatAsState(1f - spentRatio, tween(900), label = "ring")
    val overspent = balance < 0

    Box(modifier.size(270.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 22.dp.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)

            // הילה רכה מאחורי העיגול
            drawCircle(
                Brush.radialGradient(
                    listOf((if (overspent) K.Expense else K.Accent).copy(alpha = 0.22f), Color.Transparent),
                    center = center,
                    radius = size.minDimension / 2,
                ),
            )
            // המסלול (הרקע של הטבעת)
            drawArc(K.Track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            // החלק שנשאר, מתחיל מלמעלה (90- מעלות)
            if (remaining > 0.001f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(K.Cyan, K.Income, K.Accent, K.Cyan), center),
                    startAngle = -90f,
                    sweepAngle = 360f * remaining,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("יתרה החודש", color = K.Muted, fontSize = 15.sp)
            Text(
                Money.display(balance),
                color = if (overspent) K.Expense else K.Text,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (income > 0) "נוצלו ${(spentRatio * 100).roundToInt()}% מההכנסות"
                else if (expenses > 0) "אין הכנסות רשומות החודש"
                else "עוד לא נרשמו תנועות",
                color = K.Muted,
                fontSize = 13.sp,
            )
        }
    }
}
