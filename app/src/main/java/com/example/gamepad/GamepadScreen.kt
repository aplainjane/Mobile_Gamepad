package com.example.gamepad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.gamepad.ui.theme.GamePadTheme
import kotlin.math.sqrt

private val Backdrop = Color(0xFFC7DDE6)
private val Panel = Color(0xFFFFF8EF)
private val Line = Color(0xFF777477)
private val Muted = Color(0xFF67747A)
private val Ink = Color(0xFF514F50)
private val Accent = Color(0xFF536F82)
private val Sky = Color(0xFFB8D4E5)
private val Sage = Color(0xFFC3CEAF)
private val Rose = Color(0xFFF0B9B5)
private val Butter = Color(0xFFF6D8AA)

/** Recessed bottom edge and a raised face, with no change to the touch target. */
private fun Modifier.cartoonKey(tint: Color, shape: Shape, pressed: Boolean) =
    background(Line.copy(alpha = .8f), shape)
        .padding(bottom = if (pressed) 0.dp else 4.dp, top = if (pressed) 4.dp else 0.dp)
        .background(Brush.verticalGradient(listOf(tint, tint.copy(alpha = .85f))), shape)
        .border(2.dp, Line, shape)
        .drawWithCache {
            onDrawWithContent {
                drawContent()
                if (!pressed) drawLine(Color.White.copy(alpha = .55f),
                    Offset(size.width * .27f, 5.dp.toPx()),
                    Offset(size.width * .68f, 5.dp.toPx()), 2.dp.toPx())
            }
        }

@Composable
private fun CartoonButton(label: String, onClick: () -> Unit, tint: Color = Sky,
    enabled: Boolean = true, modifier: Modifier = Modifier) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier,
        shape = RoundedCornerShape(18.dp), border = BorderStroke(1.5.dp, Line),
        colors = ButtonDefaults.buttonColors(containerColor = tint, contentColor = Ink,
            disabledContainerColor = tint.copy(alpha = .45f), disabledContentColor = Muted),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun DrawScope.sparkle(point: Offset, radius: Float) {
    val path = Path().apply {
        moveTo(point.x, point.y - radius)
        quadraticTo(point.x + radius * .22f, point.y - radius * .22f, point.x + radius, point.y)
        quadraticTo(point.x + radius * .22f, point.y + radius * .22f, point.x, point.y + radius)
        quadraticTo(point.x - radius * .22f, point.y + radius * .22f, point.x - radius, point.y)
        quadraticTo(point.x - radius * .22f, point.y - radius * .22f, point.x, point.y - radius)
        close()
    }
    drawPath(path, Panel.copy(alpha = .75f))
}

@Composable
fun GamepadScreen(gamepad: BluetoothGamepad, onStart: () -> Unit, onPair: () -> Unit) {
    var showConnection by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    val report = gamepad.report
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFD7E8EC), Backdrop)))) {
        Canvas(Modifier.fillMaxSize()) {
            sparkle(Offset(size.width * .035f, size.height * .4f), 12.dp.toPx())
            sparkle(Offset(size.width * .97f, size.height * .74f), 16.dp.toPx())
            drawCircle(Butter.copy(alpha = .55f), 8.dp.toPx(), Offset(size.width * .54f, size.height * .08f))
            drawCircle(Panel.copy(alpha = .6f), 4.dp.toPx(), Offset(size.width * .94f, size.height * .24f))
        }
        Column(
            Modifier.fillMaxSize()
                .padding(WindowInsets.displayCutout.asPaddingValues())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.gamepad_mascot), contentDescription = null,
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("掌上手柄", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("POCKET GAMEPAD", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.background(if (gamepad.connected) Sage else Panel.copy(alpha = .75f),
                    RoundedCornerShape(14.dp)).border(1.dp, Line.copy(alpha = .4f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(if (gamepad.connected) Color(0xFF657853) else Muted, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(when {
                        gamepad.connected -> "已连接"
                        gamepad.connecting -> "正在连接"
                        else -> "未连接 · 可体验布局"
                    }, color = Muted, fontSize = 11.sp)
                }
                Spacer(Modifier.width(14.dp))
                CartoonButton(if (gamepad.connected) "管理连接" else "连接电脑",
                    onClick = { gamepad.releaseAll(); gamepad.refreshPairedHosts(); showConnection = true },
                    tint = Butter)
                Spacer(Modifier.width(8.dp))
                CartoonButton("使用指南", onClick = { gamepad.releaseAll(); showHelp = true }, tint = Panel)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PadKey("L1", report.buttons and PadButton.L1.mask != 0,
                        Modifier.width(78.dp).height(40.dp)) { gamepad.button(PadButton.L1, it) }
                    PadKey("L2", report.leftTrigger > 0,
                        Modifier.width(78.dp).height(40.dp), tint = Butter) { gamepad.trigger(true, it) }
                }
                Text("✦  双摇杆 · 随时开玩  ✦", color = Muted, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PadKey("R2", report.rightTrigger > 0,
                        Modifier.width(78.dp).height(40.dp), tint = Butter) { gamepad.trigger(false, it) }
                    PadKey("R1", report.buttons and PadButton.R1.mask != 0,
                        Modifier.width(78.dp).height(40.dp)) { gamepad.button(PadButton.R1, it) }
                }
            }
            Spacer(Modifier.height(10.dp))
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()
                .shadow(5.dp, RoundedCornerShape(32.dp))
                .background(Panel, RoundedCornerShape(32.dp))
                .border(2.dp, Line.copy(alpha = .75f), RoundedCornerShape(32.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)) {
                val stickSize = minOf(180.dp, maxHeight - 46.dp, maxWidth * .24f).coerceAtLeast(64.dp)
                val rightStickSize = minOf(144.dp, stickSize)
                val faceSize = minOf(150.dp, maxWidth * .21f, maxHeight - 24.dp).coerceAtLeast(72.dp)
                val dpadSize = minOf(126.dp, (maxHeight - 84.dp).coerceAtLeast(44.dp))
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Stick("左摇杆", report.leftX, report.leftY, stickSize) { x, y -> gamepad.stick(true, x, y) }
                        Spacer(Modifier.height(6.dp))
                        PadKey("L3", report.buttons and PadButton.L3.mask != 0,
                            Modifier.width(64.dp).height(32.dp), tint = Sky) { gamepad.button(PadButton.L3, it) }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PadKey("SELECT", report.buttons and PadButton.SELECT.mask != 0,
                                Modifier.width(62.dp).height(34.dp), tint = Color(0xFFE5DDD4)) { gamepad.button(PadButton.SELECT, it) }
                            PadKey("START", report.buttons and PadButton.START.mask != 0,
                                Modifier.width(62.dp).height(34.dp), tint = Color(0xFFE5DDD4)) { gamepad.button(PadButton.START, it) }
                        }
                        DirectionPad(report, dpadSize, gamepad::direction)
                        PadKey("HOME", report.buttons and PadButton.HOME.mask != 0,
                            Modifier.width(62.dp).height(30.dp), tint = Rose) { gamepad.button(PadButton.HOME, it) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Stick("右摇杆", report.rightX, report.rightY, rightStickSize) { x, y ->
                                gamepad.stick(false, x, y)
                            }
                            Spacer(Modifier.height(6.dp))
                            PadKey("R3", report.buttons and PadButton.R3.mask != 0,
                                Modifier.width(64.dp).height(32.dp), tint = Sky) { gamepad.button(PadButton.R3, it) }
                        }
                        FaceButtons(report, faceSize, gamepad::button)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(gamepad.status, color = Muted, fontSize = 10.sp,
                    modifier = Modifier.weight(1f))
                CartoonButton("复位输入", onClick = gamepad::releaseAll, tint = Sage,
                    modifier = Modifier.height(34.dp))
            }
        }
    }
    if (showConnection) ConnectionDialog(gamepad, onStart, onPair) { showConnection = false }
    if (showHelp) HelpDialog { showHelp = false }
}

@Preview(name = "Cartoon gamepad", widthDp = 900, heightDp = 410, showBackground = true)
@Preview(name = "Compact landscape", widthDp = 640, heightDp = 320, showBackground = true)
@Composable
private fun GamepadScreenPreview() {
    val context = LocalContext.current
    val gamepad = remember { BluetoothGamepad(context) }
    GamePadTheme {
        GamepadScreen(gamepad, onStart = {}, onPair = {})
    }
}

/** Each control follows its own pointer, allowing sticks and buttons to be held together. */
@Composable
private fun PadKey(
    label: String,
    pressed: Boolean,
    modifier: Modifier,
    tint: Color = Sky,
    round: Boolean = false,
    onPressed: (Boolean) -> Unit,
) {
    val callback by rememberUpdatedState(onPressed)
    val haptic = LocalHapticFeedback.current
    val shape = if (round) CircleShape else RoundedCornerShape(14.dp)
    Box(modifier
        .semantics {
            contentDescription = "$label 按键"
            role = Role.Button
            onClick { callback(true); callback(false); true }
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                callback(true)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                try {
                    var held = true
                    while (held) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                        held = change?.pressed == true
                        change?.consume()
                    }
                } finally {
                    callback(false)
                }
            }
        }
        .cartoonKey(tint, shape, pressed), contentAlignment = Alignment.Center) {
        Text(label, color = Ink,
            fontSize = if (round) 22.sp else 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Stick(label: String, x: Float, y: Float, diameter: Dp, onMove: (Float, Float) -> Unit) {
    val callback by rememberUpdatedState(onMove)
    Box(Modifier.size(diameter).semantics { contentDescription = label }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                fun move(position: Offset) {
                    val delta = position - Offset(size.width / 2f, size.height / 2f)
                    val radius = minOf(size.width, size.height) * .29f
                    val distance = delta.getDistance()
                    val magnitude = (distance / radius).coerceAtMost(1f)
                    val scaled = if (magnitude < .12f) 0f else (magnitude - .12f) / .88f
                    val unit = if (distance > 0f) delta / distance else Offset.Zero
                    callback(unit.x * scaled, unit.y * scaled)
                }
                move(down.position)
                try {
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        change.consume()
                        if (!change.pressed) break
                        move(change.position)
                    }
                } finally {
                    callback(0f, 0f)
                }
            }
        }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val travel = radius * .58f
            val knob = center + Offset(x * travel, y * travel)
            drawCircle(Color(0xFFD9CFC6), radius * .95f, center + Offset(0f, 3.dp.toPx()))
            drawCircle(Color(0xFFF4ECE3), radius * .94f)
            drawCircle(Line.copy(alpha = .8f), radius * .94f, style = Stroke(2.dp.toPx()))
            drawCircle(Brush.radialGradient(listOf(Color(0xFFD4E3E8), Color(0xFFBACFD9))), radius * .76f)
            drawCircle(Color(0xFF96ACB8), radius * .76f, style = Stroke(2.dp.toPx()))
            listOf(Offset(0f, -1f), Offset(1f, 0f), Offset(0f, 1f), Offset(-1f, 0f)).forEach {
                drawCircle(Color.White.copy(alpha = .7f), 2.dp.toPx(), center + it * (radius * .65f))
            }
            if (sqrt(x * x + y * y) > .01f) {
                drawCircle(Color.White.copy(alpha = .16f), radius * .73f)
            }
            drawCircle(Line.copy(alpha = .18f), radius * .37f, knob + Offset(0f, 4.dp.toPx()))
            drawCircle(Line, radius * .35f, knob)
            drawCircle(Brush.radialGradient(listOf(Color(0xFFC5DFEF), Color(0xFF90B4D0)),
                center = knob + Offset(-radius * .13f, -radius * .16f), radius = radius * .6f),
                radius * .30f, knob - Offset(0f, 2.dp.toPx()))
            drawCircle(Color.White.copy(alpha = .3f), radius * .085f,
                knob + Offset(-radius * .12f, -radius * .15f))
        }
        Text(label, color = Muted, fontSize = 10.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
    }
}

@Composable
private fun FaceButtons(report: GamepadReport, size: Dp, onButton: (PadButton, Boolean) -> Unit) {
    val keys = listOf(
        Triple(PadButton.Y, Alignment.TopCenter, Butter),
        Triple(PadButton.X, Alignment.CenterStart, Sky),
        Triple(PadButton.B, Alignment.CenterEnd, Rose),
        Triple(PadButton.A, Alignment.BottomCenter, Sage),
    )
    Box(Modifier.size(size)) {
        keys.forEach { (button, alignment, tint) ->
            PadKey(button.name, report.buttons and button.mask != 0,
                Modifier.align(alignment).size(size * .34f), tint = tint, round = true) {
                onButton(button, it)
            }
        }
    }
}

@Composable
private fun DirectionPad(report: GamepadReport, size: Dp, onDirection: (PadDirection, Boolean) -> Unit) {
    val keys = listOf(
        Triple(PadDirection.UP, Alignment.TopCenter, "↑"),
        Triple(PadDirection.RIGHT, Alignment.CenterEnd, "→"),
        Triple(PadDirection.DOWN, Alignment.BottomCenter, "↓"),
        Triple(PadDirection.LEFT, Alignment.CenterStart, "←"),
    )
    Box(Modifier.size(size)) {
        Box(Modifier.align(Alignment.Center).size(size * .28f)
            .background(Sage, RoundedCornerShape(8.dp)))
        keys.forEach { (direction, alignment, label) ->
            PadKey(label, report.directions and direction.mask != 0,
                Modifier.align(alignment).size(size * .33f), tint = Sage) { onDirection(direction, it) }
        }
    }
}

@Composable
private fun ConnectionDialog(
    gamepad: BluetoothGamepad, onStart: () -> Unit, onPair: () -> Unit, onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(2.dp, Line, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        containerColor = Panel,
        title = { Text("连接电脑", color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(gamepad.status, color = Ink,
                    modifier = Modifier.fillMaxWidth().background(Sky.copy(alpha = .5f), RoundedCornerShape(14.dp))
                        .padding(12.dp))
                if (!gamepad.registered) {
                    CartoonButton("开启蓝牙并启动手柄", onClick = onStart, tint = Butter)
                } else if (!gamepad.connected) {
                    Text("首次使用或升级 3.0：在 Windows 删除旧的手机配对，允许手机被发现，再添加这台手机并确认两端验证码。", color = Muted)
                    CartoonButton("允许电脑发现手机（120 秒）", onClick = onPair)
                }
                if (gamepad.connected) {
                    Text("已连接 ${gamepad.connectedName}。可关闭此窗口开始操作。", color = Ink)
                    CartoonButton("断开连接", onClick = gamepad::disconnect, tint = Rose)
                } else {
                    HorizontalDivider(color = Line)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("已配对设备", color = Ink, modifier = Modifier.weight(1f))
                        CartoonButton("刷新", onClick = gamepad::refreshPairedHosts, tint = Panel)
                    }
                    if (gamepad.pairedHosts.isEmpty()) {
                        Text("尚无配对设备。请先在电脑上完成蓝牙配对。", color = Muted)
                    }
                    gamepad.pairedHosts.forEach { host ->
                        Row(Modifier.fillMaxWidth().background(Color(0xFFF0EBE2), RoundedCornerShape(16.dp))
                            .border(1.dp, Line.copy(alpha = .35f), RoundedCornerShape(16.dp)).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(host.name, color = Ink)
                                Text(host.address, color = Muted, fontSize = 10.sp)
                            }
                            CartoonButton("连接", onClick = { gamepad.connect(host.address) },
                                enabled = gamepad.registered && !gamepad.connecting, tint = Sage)
                        }
                    }
                }
                Text("连接后在 Steam 控制器设置中测试并配置布局，在游戏属性中启用 Steam Input。", color = Muted, fontSize = 11.sp)
            }
        },
        confirmButton = { CartoonButton("完成", onClick = onDismiss, tint = Butter) },
    )
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(2.dp, Line, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        containerColor = Panel,
        title = { Text("把手机变成电脑手柄", color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("1  蓝牙连接", color = Accent, fontWeight = FontWeight.Bold)
                Text("点击「连接电脑」，启动手柄并允许手机被发现。升级 3.0 后先在 Windows 删除旧的手机配对，再添加蓝牙设备并从应用内连接电脑。手机保持应用在前台。", color = Ink)
                Text("2  检查输入", color = Accent, fontWeight = FontWeight.Bold)
                Text("电脑按 Win + R，输入 joy.cpl，打开手柄属性。分别操作两个摇杆、扳机和按键，确认松手回中。网页中的前四条轴对应双摇杆；若没有自动布局，请查看原始输入。", color = Ink)
                Text("3  配置 Steam", color = Accent, fontWeight = FontWeight.Bold)
                Text("打开 Steam → 设置 → 控制器，检查识别结果并测试输入。如果显示通用手柄，需要设置按钮映射；在游戏属性 → 控制器中启用 Steam Input，然后重新从 Steam 启动游戏。", color = Ink)
                Text("操作说明", color = Accent, fontWeight = FontWeight.Bold)
                Text("双摇杆可与按键同时操作，松手自动回中。L3 / R3 对应摇杆按下，L2 / R2 是全按或松开的扳机。切到后台会复位输入并断开手柄，回到应用后重新连接。", color = Ink)
                Text("兼容范围", color = Accent, fontWeight = FontWeight.Bold)
                Text("本应用通过蓝牙 HID 发送真实手柄输入。游戏支持情况取决于游戏自身或 Steam Input 的映射；本版保留触屏按键触感，不提供游戏震动回传。", color = Muted)
            }
        },
        confirmButton = { CartoonButton("知道了", onClick = onDismiss, tint = Butter) },
    )
}
