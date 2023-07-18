package com.example.dial


import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt


private enum class IndicatorType {
    START, END
}

enum class TimeType {
    PREVIOUS, CURRENT, NEXT
}

enum class TimeEditType {
    SPLIT, EDIT, NORMAL
}

data class EditDutyTime(var start: Int, var end: Int, val type: TimeType) {
    init {
        if (start >= 24) {
            start = 0
        }
        if (end <= 0) {
            end = 24
        }
    }
}

@Composable
fun DialView(
    start: Int,
    end: Int,
    limitRange: IntRange = start..end,
    editType: TimeEditType,
    hasPre: Boolean = false,
    hasNext: Boolean = false,
    modifier: Modifier = Modifier,
    closedAction: (List<EditDutyTime>) -> Unit = {}
) {
    var startTime by remember(start) {
        mutableStateOf(start)
    }
    var endTime by remember(end) {
        mutableStateOf(end)
    }
    val circleSize by remember {
        mutableStateOf(226.dp)
    }
    val circleStroke by remember {
        mutableStateOf(28.dp)
    }
    var enableChange by remember {
        mutableStateOf(false)
    }
    val previousRange by remember(start, limitRange) {
        val rangeList = if (start < limitRange.first) {
            listOf(0..start, limitRange.first..24)
        } else {
            listOf(limitRange.first..start)
        }
        mutableStateOf(rangeList)
    }
    val currentRange by remember(startTime, endTime, limitRange) {
        val rangeList = if (startTime > endTime) {
            listOf(startTime..24, 0..endTime)
        } else {
            listOf(startTime..endTime)
        }
        mutableStateOf(rangeList)
    }
    val nextRange by remember(end, limitRange) {
        val rangeList = if (end > limitRange.last) {
            listOf(end..24, 0..limitRange.last)
        } else {
            listOf(end..limitRange.last)
        }
        mutableStateOf(rangeList)
    }
    val density = LocalDensity.current

    fun Dp.toFloat(): Float {
        return this.value * density.density
    }

    fun timeToString(time: Int): String {
        return if (time < 10) {
            "0${time}:00"
        } else {
            "${time}:00"
        }
    }

    fun isFullTime(): Boolean =
        limitRange.first == limitRange.last || (limitRange.first == 0 && limitRange.last == 24)

    fun getResultList(): List<EditDutyTime> {
        val resultList = mutableListOf<EditDutyTime>()
        when (editType) {
            TimeEditType.SPLIT -> {
                if (startTime != limitRange.first) {
                    resultList.add(
                        EditDutyTime(
                            start = limitRange.first,
                            end = startTime,
                            type = TimeType.PREVIOUS
                        )
                    )
                }
                resultList.add(
                    EditDutyTime(
                        start = startTime,
                        end = endTime,
                        type = TimeType.CURRENT
                    )
                )
                if (endTime != limitRange.last) {
                    resultList.add(
                        EditDutyTime(
                            start = endTime,
                            end = limitRange.last,
                            type = TimeType.NEXT
                        )
                    )
                }
            }

            TimeEditType.EDIT -> {
                val canAddPrevious =
                    hasPre && !previousRange.all { pRange -> currentRange.any { cRange -> cRange.first <= pRange.first && cRange.last >= pRange.last } }
                val canAddNext =
                    hasNext && !nextRange.all { nRange -> currentRange.any { cRange -> cRange.first <= nRange.first && cRange.last >= nRange.last } }
                if (canAddPrevious) {
                    if (isFullTime()) {
                        resultList.add(
                            EditDutyTime(
                                start = if (canAddNext) limitRange.first else endTime,
                                end = startTime,
                                type = TimeType.PREVIOUS
                            )
                        )
                    } else {
                        resultList.add(
                            EditDutyTime(
                                start = limitRange.first,
                                end = startTime,
                                type = TimeType.PREVIOUS
                            )
                        )
                    }
                }
                resultList.add(
                    EditDutyTime(
                        start = startTime,
                        end = endTime,
                        type = TimeType.CURRENT
                    )
                )

                if (canAddNext) {
                    if (isFullTime()) {
                        resultList.add(
                            EditDutyTime(
                                start = endTime,
                                end = if (canAddPrevious) limitRange.last else startTime,
                                type = TimeType.NEXT
                            )
                        )
                    } else {
                        resultList.add(
                            EditDutyTime(
                                start = endTime,
                                end = limitRange.last,
                                type = TimeType.NEXT
                            )
                        )
                    }
                }
            }

            else -> {}
        }
        Log.d("Duty", "result list : ${resultList}")
        return resultList
    }

    var durationTime by remember {
        mutableStateOf(0)
    }
    var showWarning by remember {
        mutableStateOf(false)
    }
    var warningText by remember {
        mutableStateOf("")
    }
    LaunchedEffect(startTime, endTime) {
        enableChange = start != startTime || end != endTime
        val time = if (startTime >= endTime) {
            24 - startTime + endTime
        } else {
            endTime - startTime
        }
        durationTime = time
    }
    Box(modifier) {
        Column(
            modifier = Modifier
                .wrapContentHeight()
                .clip(RoundedCornerShape(15.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 35.dp)
                    .wrapContentSize()
            ) {
                IndicatorComponent(
                    circleSize = circleSize,
                    circleStroke = circleStroke,
                    sweepColor = Color(0xFFECECEC)
                )
                IndicatorComponent(
                    start = limitRange.first,
                    end = limitRange.last,
                    circleSize = circleSize,
                    circleStroke = circleStroke,
                    sweepColor = MaterialTheme.colors.primary.copy(alpha = 0.5f)
                )
                IndicatorComponent(
                    start = startTime,
                    end = endTime,
                    limitRange = limitRange,
                    enable = true,
                    circleSize = circleSize,
                    circleStroke = circleStroke,
                    sweepColor = MaterialTheme.colors.primary,
                    minSpace = 0,
                    indicatorRes = mapOf(
                        6..12 to R.drawable.morning,
                        12..17 to R.drawable.afternoon,
                        17..22 to R.drawable.evening,
                        22..24 to R.drawable.night,
                        0..6 to R.drawable.night
                    ),
                    updateTimeAction = { start, end ->
                        startTime = start
                        endTime = if (end == 0) 24 else end
                    }
                ) {
                    DrawDial(
                        maxWidth / 2,
                        Offset(maxWidth.toFloat() / 2, maxWidth.toFloat() / 2),
                        startTime,
                        endTime
                    )
                    Text(
                        text = stringResource(
                            R.string.edit_time_hour,
                            durationTime
                        ),
                        fontSize = 40.sp,
                        color = Color(0xff666666),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }
            }
            Text(
                text = stringResource(R.string.edit_time_title),
                Modifier.padding(top = 15.dp, bottom = 9.dp),
                fontSize = 12.sp,
                color = Color.Black
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    timeToString(startTime),
                    color = Color(0xFF666666),
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.wrapContentSize()
                )
                Divider(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .width(15.dp),
                    color = Color(0xFF979797),
                    thickness = 1.dp
                )
                Text(
                    timeToString(endTime),
                    color = Color(0xFF666666),
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.wrapContentSize()
                )
            }
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(56.dp)
            ) {
                Text(
                    stringResource(R.string.edit_time_title_today),
                    color = Color(0xFF666666),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.wrapContentSize()
                )
                Text(
                    if (endTime > startTime) stringResource(R.string.edit_time_title_today) else stringResource(
                        R.string.edit_time_title_tomorrow
                    ),
                    color = Color(0xFF666666),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.wrapContentSize()
                )
            }
            Row(
                modifier = Modifier
                    .padding(top = 30.dp, bottom = 18.dp, start = 27.dp, end = 27.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Box(modifier = Modifier.weight(1f).clickable {
                    closedAction.invoke(emptyList())
                }) {
                    Text(
                        stringResource(R.string.cancel),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Box(modifier = Modifier.weight(1f).clickable {
                    val resultList = getResultList()
                    var showMergeUp = false
                    var showMergeDown = false
                    if (hasPre && resultList.none { it.type == TimeType.PREVIOUS }) {
                        showMergeUp = true
                    }
                    if (hasNext && resultList.none { it.type == TimeType.NEXT }) {
                        showMergeDown = true
                    }
                    warningText = "是否确定覆盖执勤时段\n"
                    if (showMergeUp) {
                        warningText += "【${
                            limitRange.first.toFloat().formatTimeString()
                        } ~ ${start.toFloat().formatTimeString()}】"
                    }
                    if (showMergeDown) {
                        warningText += "【${
                            end.toFloat().formatTimeString()
                        } ~ ${limitRange.last.toFloat().formatTimeString()}】"
                    }
                    showWarning = showMergeUp || showMergeDown
                    if (!showWarning) {
                        closedAction.invoke(resultList)
                    }
                }) {
                    Text(
                        stringResource(R.string.confirm),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
//        AnimatedVisibility(showWarning) {
//            Box(modifier = Modifier.padding(horizontal = 15.dp).fillMaxWidth().wrapContentHeight()) {
//
//            }
//        }
    }


//    MimikkoDialog(
//        visible = showWarning,
//        onDismissRequest = { showWarning = false },
//        closable = false
//    ) {
//        MimikkoDialogFrame(
//            onDismissRequest = { showWarning = false },
//            modifier = Modifier.fillMaxWidth(),
//            title = {
//                Text(text = stringResource(id = R.string.character_dialog_title_tips))
//            },
//            buttons = {
//                MimikkoButton(OUTLINE, modifier = Modifier.weight(1f), onClick = {
//                    showWarning = false
//                }) {
//                    Text(
//                        stringResource(R.string.character_cancel),
//                        modifier = Modifier.align(Alignment.Center)
//                    )
//                }
//                MimikkoButton(modifier = Modifier.weight(1f), onClick = {
//                    closedAction.invoke(getResultList())
//                }) {
//                    Text(
//                        stringResource(R.string.character_confirm),
//                        modifier = Modifier.align(Alignment.Center)
//                    )
//                }
//            }
//        ) {
//            S("character-dialog-tips_content") {
//                Text(
//                    warningText,
//                    modifier = Modifier
//                        .align(Alignment.TopStart)
//                        .styled(),
//                )
//            }
//
//        }
//    }
}


@Composable
private fun DrawDial(radius: Dp, center: Offset, startTime: Int = 0, endTime: Int = 0) {

    Canvas(modifier = Modifier.size(radius * 2)) {
        val tickSize = 5.dp
        val tickCount = 24
        val degreesPerTick = 360f / tickCount.toFloat()
        val strokeWidth = 1.dp
        val tickPadding = 2.dp
        val textPadding = 12.dp
        drawCircle(Color.Transparent, radius.toPx(), center)
        for (i in 0 until tickCount) {
            val number = (i + 6) % 24 // 计算数字值，从1开始
            val isSelected = number == startTime || number == endTime
            val lineColor = if (isSelected) Color(0xFF666666) else Color(0xFFDDDDDD)
            val textColor = if (isSelected) Color(0xFF666666) else Color(0xFFDDDDDD)

            val angle = i * degreesPerTick
            val markStart = Offset(
                x = center.x + (radius.toPx() - tickPadding.toPx() - tickSize.toPx()) * cos(
                    Math.toRadians(
                        angle.toDouble()
                    )
                ).toFloat(),
                y = center.y + (radius.toPx() - tickPadding.toPx() - tickSize.toPx()) * sin(
                    Math.toRadians(
                        angle.toDouble()
                    )
                ).toFloat()
            )
            val markEnd = Offset(
                x = center.x + (radius.toPx() - tickPadding.toPx() - strokeWidth.toPx()) * cos(
                    Math.toRadians(
                        angle.toDouble()
                    )
                ).toFloat(),
                y = center.y + (radius.toPx() - tickPadding.toPx() - strokeWidth.toPx()) * sin(
                    Math.toRadians(
                        angle.toDouble()
                    )
                ).toFloat()
            )
            drawLine(
                lineColor,
                start = markStart,
                end = markEnd,
                strokeWidth = strokeWidth.toPx(),
                cap = StrokeCap.Round
            )

            val tSize = (if (isSelected) 15 else 9).sp.toPx()
            val textPaint = Paint().apply {
                textAlign = Paint.Align.CENTER
                textSize = tSize
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = textColor.toArgb()
                getFontMetrics(Paint.FontMetrics())
            }
            drawIntoCanvas {
                val fontMetrics = textPaint.fontMetrics
                val textWidth = textPaint.measureText(angle.toString())
                val textHeight = fontMetrics.descent
                val textRadius = radius.toPx() - textHeight / 2
                val textOffset = Offset(
                    x = center.x + (radius.toPx() - tSize / 2 - textPadding.toPx()) * cos(
                        Math.toRadians(
                            angle.toDouble()
                        )
                    ).toFloat(),
                    y = center.y + (radius.toPx() - tSize / 2 - textPadding.toPx()) * sin(
                        Math.toRadians(
                            angle.toDouble()
                        )
                    ).toFloat() + textHeight
                )
                drawContext.canvas.nativeCanvas.drawText(
                    number.toString(),
                    textOffset.x,
                    textOffset.y,
                    textPaint
                )
            }
        }
    }
}

@Composable
private fun IndicatorComponent(
    start: Int = 0,
    end: Int = 24,
    limitRange: IntRange = start..end,
    enable: Boolean = false,
    editType: TimeEditType = TimeEditType.NORMAL,
    circleSize: Dp = 200.dp,
    circleStroke: Dp = 100.dp,
    sweepColor: Color = Color(0xFFFFA0C0),
    minSpace: Int = 0,
    indicatorRes: Map<IntRange, Int> = emptyMap(),
    updateTimeAction: (startTime: Int, endTime: Int) -> Unit = { _, _ -> },
    content: @Composable BoxWithConstraintsScope.() -> Unit = {}
) {
    var startTime by remember(start) {
        mutableStateOf(start)
    }
    var endTime by remember(end) {
        mutableStateOf(end)
    }
    val startImage by remember(startTime) {
        var res: Int? = null
        indicatorRes.forEach {
            if (it.key.contains(startTime)) {
                res = it.value
                return@forEach
            }
        }
        mutableStateOf(res)
    }
    val endImage by remember(endTime) {
        var res: Int? = null
        indicatorRes.forEach {
            if (it.key.contains(endTime)) {
                res = it.value
                return@forEach
            }
        }
        mutableStateOf(res)
    }

    var startAngle by remember {
        mutableStateOf(0f)
    }
    val context = LocalContext.current
    LaunchedEffect(startTime) {
        val angle = timeToAngle(startTime)
        startAngle = angle
    }
    var endAngle by remember {
        mutableStateOf(0f)
    }
    LaunchedEffect(endTime) {
        val angle = timeToAngle(endTime)
        endAngle = angle
    }
    val sweepAngle = remember {
        mutableStateOf(0f)
    }
    LaunchedEffect(startTime, endTime) {
        val angle = if (endTime <= startTime) {
            (24 - startTime + endTime).toFloat() / 24f * 360f
        } else {
            (endTime - startTime).toFloat() / 24f * 360f
        }
        sweepAngle.value = angle
    }
    val density = LocalDensity.current

    fun Dp.toFloat(): Float {
        return this.value * density.density
    }

    fun Float.toDp(): Dp {
        return (this / density.density).dp
    }

    fun checkMinSpace(start: Int, end: Int): Boolean {
        val duration = if (start <= end) {
            end - start
        } else {
            24 - start + end
        }
        return duration >= minSpace
    }

    fun inLimitRange(time: Int): Boolean = if (limitRange.first < limitRange.last) {
        time in limitRange
    } else {
        time in limitRange.first..24 || time in 0..limitRange.last
    }

    fun isFullTime(): Boolean =
        limitRange.first == limitRange.last || (limitRange.first == 0 && limitRange.last == 24)

    fun updateStartTime(start: Int) {
        val start = if (start >= 24) 0 else start
        if (!checkMinSpace(start, endTime)) return
        // 是否跨天
        if (limitRange.first > limitRange.last) {
            if (start < limitRange.first && start > limitRange.last) {
                return
            } else {
                // 是否占据对方有效区域
                if (endTime < limitRange.first) {
                    if (start in endTime..limitRange.last) return
                } else {
                    if (start in endTime..24 || start in 0..limitRange.last) return
                }
                // 是否强制改动对方节点
                val endOffset = if (endTime <= limitRange.last) {
                    limitRange.last - endTime
                } else {
                    24 - endTime + limitRange.last
                }
                val startOffset = if (start >= limitRange.first) {
                    start - limitRange.first
                } else {
                    24 - limitRange.first + start
                }
                if ((startOffset + endOffset) < minSpace) {
                    var nextEnd = limitRange.last - minSpace - startOffset
                    if (nextEnd <= 0) {
                        nextEnd += 24
                    }
                    endTime = nextEnd
                }
            }
        } else if (limitRange.first == limitRange.last) {
            if (start == endTime) {
                if (endTime > 0) {
                    endTime -= 1
                } else {
                    endTime = 23
                }
            }
        } else {
            if (!inLimitRange(start)) return
            if (!isFullTime() || editType == TimeEditType.EDIT) {
                if (start in endTime..limitRange.last) return
            }
            val startOffset = start - limitRange.first
            val endOffset = limitRange.last - endTime
            if (startOffset + endOffset < minSpace) {
                val nextEnd = limitRange.last - minSpace - startOffset
                endTime = nextEnd
            }
        }
        context.vibrator()
        startTime = start
        updateTimeAction(startTime, endTime)
    }

    fun updateEndTime(end: Int) {
        val end = if (end <= 0) 24 else end
        if (!checkMinSpace(startTime, end)) return
        if (limitRange.first > limitRange.last) {
            if (end < limitRange.first && end > limitRange.last) {
                return
            } else {
                // 是否占据对方有效区域
                if (startTime < limitRange.last) {
                    if (end in limitRange.first..24 || end in 0..startTime) return
                } else {
                    if (end in limitRange.first..startTime) return
                }
                // 是否强制改动对方节点
                val startOffset = if (startTime >= limitRange.first) {
                    startTime - limitRange.first
                } else {
                    24 - limitRange.first + startTime
                }
                val endOffset = if (end <= limitRange.last) {
                    limitRange.last - end
                } else {
                    24 - end + limitRange.last
                }
                if ((startOffset + endOffset) < minSpace) {
                    var nextStart = limitRange.first + minSpace + endOffset
                    if (nextStart >= 24) {
                        nextStart -= 24
                    }
                    startTime = nextStart
                }
            }
        } else if (limitRange.first == limitRange.last) {
            if (end == startTime) {
                if (startTime < 23) {
                    startTime += 1
                } else {
                    startTime = 0
                }
            }
        } else {
            if (!inLimitRange(end)) return
            if (!isFullTime() || editType == TimeEditType.EDIT) {
                if (end in limitRange.first..startTime) return
            }
            val startOffset = startTime - limitRange.first
            val endOffset = limitRange.last - end
            if (startOffset + endOffset < minSpace) {
                val nextStart = limitRange.first + minSpace + endOffset
                startTime = nextStart
            }
        }
        context.vibrator()
        endTime = end
        updateTimeAction(startTime, endTime)
    }

    val componentSize by remember(circleSize) {
        mutableStateOf(circleSize)
    }
    val circleStroke by remember(circleStroke) {
        mutableStateOf(circleStroke.toFloat())
    }
    var circleScale by remember {
        mutableStateOf(0f)
    }
    LaunchedEffect(circleStroke, componentSize) {
        val scale = 1f - circleStroke / componentSize.toFloat()
        circleScale = scale
    }
    val indicatorComponentSize by remember(componentSize, circleScale) {
        mutableStateOf(Size(componentSize.toFloat(), componentSize.toFloat()) * circleScale)
    }
    var indicatorSize by remember {
        mutableStateOf(0f)
    }
    LaunchedEffect(circleStroke) {
        val size = circleStroke / sqrt(2.0)
        indicatorSize = size.toFloat()
    }

    var startIndicatorX by remember {
        mutableStateOf(0f)
    }

    var startIndicatorY by remember {
        mutableStateOf(0f)
    }
    var endIndicatorX by remember {
        mutableStateOf(0f)
    }

    var endIndicatorY by remember {
        mutableStateOf(0f)
    }
    val tapScale by remember {
        mutableStateOf(2f)
    }
    LaunchedEffect(startAngle) {
        val centerX = componentSize.toFloat() / 2
        val centerY = centerX
        val radius = centerX - circleStroke / 2
        val startRadians = Math.toRadians(startAngle.toDouble()).toFloat()
        val x = centerX + radius * cos(startRadians) - indicatorSize / 2
        val y = centerY + radius * sin(startRadians) - indicatorSize / 2
        startIndicatorX = x
        startIndicatorY = y
    }
    LaunchedEffect(endAngle) {
        val centerX = componentSize.toFloat() / 2
        val centerY = centerX
        val radius = centerX - circleStroke / 2
        val endRadians = Math.toRadians(endAngle.toDouble()).toFloat()
        val x = centerX + radius * cos(endRadians) - indicatorSize / 2
        val y = centerY + radius * sin(endRadians) - indicatorSize / 2
        endIndicatorX = x
        endIndicatorY = y
    }
    LaunchedEffect(Unit) {
        if (startTime == endTime) {
            endTime -= minSpace
            updateTimeAction(startTime, endTime)
        }
    }

    Box(
        modifier = Modifier
            .size(componentSize)
            .background(Color.Transparent)
            .drawBehind {
                drawForegroundIndicator(
                    componentSize = indicatorComponentSize,
                    stroke = circleStroke,
                    color = sweepColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle.value
                )
            }
            .run {
                if (enable) {
                    pointerInput(Unit) {
                        awaitEachGesture {
                            val startIndicatorRect = Rect(
                                offset = Offset(
                                    startIndicatorX - indicatorSize / 2 * (tapScale - 1),
                                    startIndicatorY - indicatorSize / 2 * (tapScale - 1)
                                ),
                                size = Size(indicatorSize, indicatorSize) * tapScale
                            )
                            val endIndicatorRect = Rect(
                                offset = Offset(
                                    endIndicatorX - indicatorSize / 2 * (tapScale - 1),
                                    endIndicatorY - indicatorSize / 2 * (tapScale - 1)
                                ),
                                size = Size(indicatorSize, indicatorSize) * tapScale
                            )
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val indicatorType: IndicatorType? =
                                if (down.position in startIndicatorRect) {
                                    IndicatorType.START
                                } else if (down.position in endIndicatorRect) {
                                    IndicatorType.END
                                } else {
                                    null
                                }
                            if (indicatorType == null) return@awaitEachGesture
                            var currentStartTime = startTime
                            var currentEndTime = endTime
                            while (true) {
                                val event = awaitPointerEvent()
                                val dragEvent =
                                    event.changes.firstOrNull { it.id == down.id } ?: continue
                                if (!dragEvent.pressed) return@awaitEachGesture
                                var angle = Math.toDegrees(
                                    atan2(
                                        dragEvent.position.y - componentSize.toFloat() / 2f,
                                        dragEvent.position.x - componentSize.toFloat() / 2f
                                    ).toDouble()
                                )
                                if (angle < 0) {
                                    angle += 360
                                } else if (angle > 180) {
                                    angle -= 360
                                }
                                when (indicatorType) {
                                    IndicatorType.START -> {
                                        val startTime = angleToTime(angle)
                                        if (startTime != currentStartTime) {
                                            currentStartTime = startTime
                                            updateStartTime(startTime)
                                        }
                                    }

                                    IndicatorType.END -> {
                                        val endTime = angleToTime(angle)
                                        if (endTime != currentEndTime) {
                                            currentEndTime = endTime
                                            updateEndTime(endTime)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else this
            }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .padding(circleStroke.toDp())
                .fillMaxSize()
        ) {
            content()
        }
        if (enable) {
            Box(
                modifier = Modifier
                    .size(indicatorSize.toDp())
                    .offset(startIndicatorX.toDp(), startIndicatorY.toDp())
                    .clip(CircleShape)
                    .background(Color.White, CircleShape)
            ) {
                startImage?.let { res ->
                    Image(
                        painter = painterResource(id = res),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(1.5.dp)
                            .fillMaxSize(),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(indicatorSize.toDp())
                    .offset(endIndicatorX.toDp(), endIndicatorY.toDp())
                    .clip(CircleShape)
                    .background(Color.White, CircleShape)
            ) {
                endImage?.let { res ->
                    Image(
                        painter = painterResource(id = res),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(1.5.dp)
                            .fillMaxSize(),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawForegroundIndicator(
    componentSize: Size,
    stroke: Float = 100f,
    color: Color,
    startAngle: Float = 0f,
    sweepAngle: Float
) {
    drawArc(
        size = componentSize,
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        style = Stroke(cap = StrokeCap.Round, width = stroke),
        topLeft = Offset(
            x = ((size.width - componentSize.width) / 2),
            y = ((size.height - componentSize.height) / 2)
        )
    )
}

private fun angleToTime(angle: Double): Int {
    // Convert angle to positive value between 0 and 360 degrees
    var positiveAngle = angle % 360
    if (positiveAngle < 0) {
        positiveAngle += 360
    }
    var time = (positiveAngle / 15).roundToInt() + 6
    if (time > 24) {
        time -= 24
    }
    return time
}

private fun timeToAngle(time: Int): Float {
    val angle = if (time < 6) {
        (time.toFloat() + 18f) / 24f * 360f
    } else if (time > 6f) {
        (time.toFloat() - 6f) / 24f * 360f
    } else {
        0f
    }
    return angle
}
