package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import kotlin.math.abs

/**
 * Data item rendered inside the iOS 27 Liquid Glass interactive draggable bottom dock.
 */
data class Ios27DockTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

/**
 * Pure RTL/LTR geometric math for mapping touch coordinates to visual tabs and
 * computing optical lens magnification proximity across the Liquid Glass capsule.
 */
object Ios27LiquidGlassDockMath {
    fun visualCenterXForTab(
        tabIndex: Int,
        tabCount: Int,
        dockWidthPx: Float,
        isRtl: Boolean
    ): Float {
        if (tabCount <= 0 || dockWidthPx <= 0f) return 0f
        val safeIndex = tabIndex.coerceIn(0, tabCount - 1)
        val visualSlot = if (isRtl) (tabCount - 1 - safeIndex) else safeIndex
        val slotWidth = dockWidthPx / tabCount.toFloat()
        return (visualSlot + 0.5f) * slotWidth
    }

    fun tabIndexForPointerX(
        pointerX: Float,
        tabCount: Int,
        dockWidthPx: Float,
        isRtl: Boolean
    ): Int {
        if (tabCount <= 1 || dockWidthPx <= 0f) return 0
        val clampedX = pointerX.coerceIn(0f, (dockWidthPx - 0.001f).coerceAtLeast(0f))
        val visualSlot = ((clampedX / dockWidthPx) * tabCount).toInt().coerceIn(0, tabCount - 1)
        return if (isRtl) (tabCount - 1 - visualSlot) else visualSlot
    }

    fun clampDropletCenterX(
        pointerX: Float,
        tabCount: Int,
        dockWidthPx: Float
    ): Float {
        if (tabCount <= 0 || dockWidthPx <= 0f) return 0f
        val halfSlot = (dockWidthPx / tabCount.toFloat()) * 0.5f
        val edgeMargin = halfSlot * 0.78f
        return pointerX.coerceIn(edgeMargin, (dockWidthPx - edgeMargin).coerceAtLeast(edgeMargin))
    }

    fun lensProximity(
        tabCenterX: Float,
        dropletCenterX: Float,
        slotWidthPx: Float
    ): Float {
        if (slotWidthPx <= 0f) return 0f
        return (1f - abs(tabCenterX - dropletCenterX) / (slotWidthPx * 0.92f)).coerceIn(0f, 1f)
    }
}

/**
 * iOS 27 Photos-inspired interactive Liquid Glass bottom navigation dock.
 *
 * Interaction model:
 * - Tap any tab -> liquid glass lens springs elastically onto that tab.
 * - Press & hold anywhere on the dock (or slide horizontally) -> the crystalline refractive
 *   droplet inflates with haptic feedback, stretches dynamically with finger velocity,
 *   magnifies icons & labels underneath like convex liquid glass, and snaps smoothly to the
 *   highlighted tab on release. Fully RTL (Arabic) and LTR (English) aware.
 */
@Composable
fun Ios27LiquidGlassBottomDock(
    tabs: List<Ios27DockTabItem>,
    selectedRoute: String,
    onSelectTab: (Ios27DockTabItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tabs.isEmpty()) return

    val noorColors = LocalNoorColors.current
    val isDark = noorColors.isDark
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val haptic = LocalHapticFeedback.current

    val selectedIndex = remember(tabs, selectedRoute) {
        tabs.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)
    }

    val latestTabs by rememberUpdatedState(tabs)
    val latestIsRtl by rememberUpdatedState(isRtl)
    val latestOnSelectTab by rememberUpdatedState(onSelectTab)
    val latestSelectedIndex by rememberUpdatedState(selectedIndex)

    var dockWidthPx by remember { mutableFloatStateOf(0f) }
    var isDraggingLiquid by remember { mutableStateOf(false) }
    var dragCenterPx by remember { mutableFloatStateOf(0f) }
    var dragVelocityPx by remember { mutableFloatStateOf(0f) }
    var hoveredTabIndex by remember { mutableIntStateOf(selectedIndex) }

    val restingCenterPx = remember(selectedIndex, tabs.size, dockWidthPx, isRtl) {
        Ios27LiquidGlassDockMath.visualCenterXForTab(
            tabIndex = selectedIndex,
            tabCount = tabs.size,
            dockWidthPx = dockWidthPx,
            isRtl = isRtl
        )
    }

    // Smooth elastic spring when resting/snapping; direct finger tracking when dragging liquid
    val animatedCenterPx by animateFloatAsState(
        targetValue = if (isDraggingLiquid) dragCenterPx else restingCenterPx,
        animationSpec = if (isDraggingLiquid) {
            spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessHigh)
        } else {
            spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "ios27_liquid_center_x"
    )

    // Liquid glass droplet bloom when held/dragged (just like iOS 27 Photos magnifying pill)
    val liquidBloom by animateFloatAsState(
        targetValue = if (isDraggingLiquid) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.56f, stiffness = Spring.StiffnessMedium),
        label = "ios27_liquid_bloom"
    )

    val velocityStretch by animateFloatAsState(
        targetValue = if (isDraggingLiquid) {
            (abs(dragVelocityPx) / 26f).coerceIn(0f, 0.22f)
        } else {
            0f
        },
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "ios27_liquid_stretch"
    )

    val refractionShift by animateFloatAsState(
        targetValue = if (isDraggingLiquid) {
            (dragVelocityPx / 18f).coerceIn(-1f, 1f)
        } else {
            0f
        },
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "ios27_liquid_refraction_shift"
    )

    val activeIconColor = if (isDark) GoldBright else noorColors.goldAccent
    val inactiveIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
        alpha = if (isDark) 0.82f else 0.76f
    )
    val dockTopColor = noorColors.glassDockTop
    val dockBottomColor = noorColors.glassDockBottom
    val dockBorderTop = noorColors.glassBorderTop
    val dockBorderBottom = noorColors.glassBorderBottom
    val specularTop = noorColors.glassSpecularTop
    val primaryTint = MaterialTheme.colorScheme.primary

    // Outer container gives extra vertical headroom (78.dp around 66.dp capsule) so the
    // inflated liquid glass magnifying droplet & chromatic aura never clip!
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(76.dp)
            .onSizeChanged { sz ->
                dockWidthPx = sz.width.toFloat()
            }
            .pointerInput(tabs.size) {
                val touchSlopPx = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val width = size.width.toFloat()
                    val currentTabs = latestTabs
                    if (width <= 1f || currentTabs.isEmpty()) return@awaitEachGesture

                    val downX = down.position.x
                    val initialTab = Ios27LiquidGlassDockMath.tabIndexForPointerX(
                        pointerX = downX,
                        tabCount = currentTabs.size,
                        dockWidthPx = width,
                        isRtl = latestIsRtl
                    )

                    var enteredLiquidMode = false
                    var releasedEarly = false
                    var cancelled = false
                    var lastX = downX
                    var activeTab = initialTab

                    // Phase 1: Wait up to 125ms for press-and-hold OR immediate horizontal slide
                    val waitOutcome = withTimeoutOrNull(125L) {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.firstOrNull()
                                ?: break

                            if (change.changedToUpIgnoreConsumed() || !change.pressed) {
                                releasedEarly = true
                                break
                            }

                            val dxFromDown = change.position.x - downX
                            val dyFromDown = change.position.y - down.position.y
                            if (abs(dxFromDown) > touchSlopPx * 0.35f &&
                                abs(dxFromDown) >= abs(dyFromDown) * 0.75f
                            ) {
                                // Immediate horizontal glide across the Liquid Glass dock
                                enteredLiquidMode = true
                                change.consume()
                                lastX = change.position.x
                                dragCenterPx = Ios27LiquidGlassDockMath.clampDropletCenterX(
                                    pointerX = lastX,
                                    tabCount = currentTabs.size,
                                    dockWidthPx = width
                                )
                                activeTab = Ios27LiquidGlassDockMath.tabIndexForPointerX(
                                    pointerX = lastX,
                                    tabCount = currentTabs.size,
                                    dockWidthPx = width,
                                    isRtl = latestIsRtl
                                )
                                hoveredTabIndex = activeTab
                                dragVelocityPx = dxFromDown
                                isDraggingLiquid = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                break
                            }
                        }
                        true
                    }

                    // If timeout elapsed while finger is still pressed -> Press-and-Hold Liquid Glass activated!
                    if (waitOutcome == null && !releasedEarly && !enteredLiquidMode) {
                        enteredLiquidMode = true
                        dragCenterPx = Ios27LiquidGlassDockMath.clampDropletCenterX(
                            pointerX = downX,
                            tabCount = currentTabs.size,
                            dockWidthPx = width
                        )
                        activeTab = initialTab
                        hoveredTabIndex = activeTab
                        dragVelocityPx = 0f
                        isDraggingLiquid = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }

                    // Quick tap path: release happened cleanly before hold/slide threshold
                    if (releasedEarly && !enteredLiquidMode) {
                        hoveredTabIndex = initialTab
                        if (initialTab != latestSelectedIndex) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        latestOnSelectTab(currentTabs[initialTab])
                        return@awaitEachGesture
                    }

                    // Phase 2: Continuous real-time Liquid Glass finger tracking until lift
                    if (enteredLiquidMode) {
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                    ?: event.changes.firstOrNull()
                                    ?: break

                                if (change.changedToUpIgnoreConsumed() || !change.pressed) {
                                    break
                                }

                                change.consume()
                                val currentX = change.position.x
                                val deltaX = currentX - lastX
                                lastX = currentX

                                dragVelocityPx = dragVelocityPx * 0.55f + deltaX * 0.45f
                                dragCenterPx = Ios27LiquidGlassDockMath.clampDropletCenterX(
                                    pointerX = currentX,
                                    tabCount = currentTabs.size,
                                    dockWidthPx = width
                                )

                                val nextTab = Ios27LiquidGlassDockMath.tabIndexForPointerX(
                                    pointerX = currentX,
                                    tabCount = currentTabs.size,
                                    dockWidthPx = width,
                                    isRtl = latestIsRtl
                                )
                                if (nextTab != activeTab) {
                                    activeTab = nextTab
                                    hoveredTabIndex = nextTab
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        } catch (_: Throwable) {
                            cancelled = true
                        } finally {
                            isDraggingLiquid = false
                            dragVelocityPx = 0f
                            if (!cancelled && activeTab in currentTabs.indices) {
                                latestOnSelectTab(currentTabs[activeTab])
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Layer 1 & 2: Frosted Floating Glass Capsule + Interactive Refractive Liquid Glass Lens
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 1f || h <= 1f) return@Canvas

            val capsuleInsetY = 5.dp.toPx()
            val capsuleHeight = h - capsuleInsetY * 2f
            val capsuleRadius = capsuleHeight * 0.5f
            val capsuleTopLeft = Offset(0f, capsuleInsetY)
            val capsuleSize = Size(w, capsuleHeight)

            // 1. Soft floating ambient shadow halo underneath the glass capsule
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = if (isDark) 0.46f else 0.14f),
                        Color.Transparent
                    ),
                    startY = capsuleInsetY + capsuleHeight * 0.55f,
                    endY = h + 4.dp.toPx()
                ),
                topLeft = Offset(6.dp.toPx(), capsuleInsetY + 4.dp.toPx()),
                size = Size(w - 12.dp.toPx(), capsuleHeight),
                cornerRadius = CornerRadius(capsuleRadius, capsuleRadius)
            )

            // 2. Main translucent frosted glass capsule body
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        dockTopColor.copy(alpha = if (isDark) 0.86f else 0.90f),
                        dockBottomColor.copy(alpha = if (isDark) 0.92f else 0.95f)
                    ),
                    startY = capsuleInsetY,
                    endY = capsuleInsetY + capsuleHeight
                ),
                topLeft = capsuleTopLeft,
                size = capsuleSize,
                cornerRadius = CornerRadius(capsuleRadius, capsuleRadius)
            )

            // 3. Subtle inner frosted glass shimmer wash
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.11f else 0.45f),
                        Color.Transparent,
                        GoldPrimary.copy(alpha = if (isDark) 0.06f else 0.08f)
                    ),
                    start = Offset(0f, capsuleInsetY),
                    end = Offset(w, capsuleInsetY + capsuleHeight)
                ),
                topLeft = capsuleTopLeft,
                size = capsuleSize,
                cornerRadius = CornerRadius(capsuleRadius, capsuleRadius)
            )

            // 4. Crisp beveled glass outer border (top luminous rim -> golden mid -> bottom rim)
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        dockBorderTop.copy(alpha = if (isDark) 0.55f else 0.85f),
                        GoldPrimary.copy(alpha = if (isDark) 0.34f else 0.30f),
                        dockBorderBottom.copy(alpha = if (isDark) 0.32f else 0.50f)
                    ),
                    start = Offset(0f, capsuleInsetY),
                    end = Offset(w, capsuleInsetY + capsuleHeight)
                ),
                topLeft = capsuleTopLeft,
                size = capsuleSize,
                cornerRadius = CornerRadius(capsuleRadius, capsuleRadius),
                style = Stroke(width = 1.25.dp.toPx())
            )

            // 5. Top specular glass reflection hairline across the capsule
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        specularTop.copy(alpha = if (isDark) 0.52f else 0.85f),
                        Color.Transparent
                    ),
                    startX = w * 0.12f,
                    endX = w * 0.88f
                ),
                start = Offset(w * 0.12f, capsuleInsetY + 1.5.dp.toPx()),
                end = Offset(w * 0.88f, capsuleInsetY + 1.5.dp.toPx()),
                strokeWidth = 1.6.dp.toPx()
            )

            // 6. MOVING iOS 27 LIQUID GLASS MAGNIFYING DROPLET
            val slotW = w / tabs.size.toFloat()
            val centerX = if (animatedCenterPx > 0f) {
                animatedCenterPx
            } else {
                Ios27LiquidGlassDockMath.visualCenterXForTab(
                    tabIndex = selectedIndex,
                    tabCount = tabs.size,
                    dockWidthPx = w,
                    isRtl = isRtl
                )
            }
            val centerY = h * 0.5f

            val baseLensW = (slotW * 0.90f).coerceAtLeast(56.dp.toPx())
            val baseLensH = capsuleHeight - 10.dp.toPx()

            // Elastic liquid jelly deformation: inflates on hold & elongates subtly with slide speed
            val lensW = baseLensW * (1f + 0.15f * liquidBloom + velocityStretch)
            val lensH = baseLensH * (1f + 0.20f * liquidBloom - velocityStretch * 0.22f)
            val lensRadius = lensH * 0.5f
            val lensLeft = (centerX - lensW * 0.5f).coerceIn(2.dp.toPx(), (w - lensW - 2.dp.toPx()).coerceAtLeast(2.dp.toPx()))
            val lensTop = centerY - lensH * 0.5f

            // 6a. Ambient luminous refraction aura when held/dragged
            if (liquidBloom > 0.02f) {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GoldBright.copy(alpha = (if (isDark) 0.26f else 0.22f) * liquidBloom),
                            Color(0xFF7BE8FF).copy(alpha = (if (isDark) 0.16f else 0.14f) * liquidBloom),
                            Color.Transparent
                        ),
                        center = Offset(lensLeft + lensW * 0.5f, centerY),
                        radius = lensW * 0.85f
                    ),
                    topLeft = Offset(lensLeft - 10.dp.toPx(), lensTop - 8.dp.toPx()),
                    size = Size(lensW + 20.dp.toPx(), lensH + 16.dp.toPx()),
                    cornerRadius = CornerRadius(lensRadius + 8.dp.toPx(), lensRadius + 8.dp.toPx())
                )
            }

            // 6b. Crystalline translucent liquid droplet body (convex optical glass fill)
            val causticShiftPx = -refractionShift * 10.dp.toPx()
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = if (isDark) {
                        listOf(
                            Color.White.copy(alpha = 0.20f + 0.12f * liquidBloom),
                            primaryTint.copy(alpha = 0.26f + 0.10f * liquidBloom),
                            GoldPrimary.copy(alpha = 0.14f + 0.08f * liquidBloom)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.78f + 0.14f * liquidBloom),
                            Color.White.copy(alpha = 0.42f + 0.12f * liquidBloom),
                            GoldPrimary.copy(alpha = 0.16f + 0.08f * liquidBloom)
                        )
                    },
                    center = Offset(
                        lensLeft + lensW * 0.5f + causticShiftPx,
                        lensTop + lensH * 0.34f
                    ),
                    radius = lensW * 0.78f
                ),
                topLeft = Offset(lensLeft, lensTop),
                size = Size(lensW, lensH),
                cornerRadius = CornerRadius(lensRadius, lensRadius)
            )

            // 6c. Upper convex liquid-glass specular crescent highlight (moves subtly with motion)
            val highlightInsetX = lensW * 0.14f
            val highlightW = lensW * 0.72f
            val highlightH = lensH * 0.36f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = (if (isDark) 0.44f else 0.86f) + 0.12f * liquidBloom),
                        Color.White.copy(alpha = 0.0f)
                    ),
                    startY = lensTop + 1.5.dp.toPx(),
                    endY = lensTop + 1.5.dp.toPx() + highlightH
                ),
                topLeft = Offset(
                    lensLeft + highlightInsetX + causticShiftPx * 0.35f,
                    lensTop + 2.dp.toPx()
                ),
                size = Size(highlightW, highlightH),
                cornerRadius = CornerRadius(highlightH * 0.5f, highlightH * 0.5f)
            )

            // 6d. Bottom internal refractive caustic glow (gives the 3D liquid droplet thickness)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF9DF0FF).copy(alpha = (if (isDark) 0.14f else 0.20f) + 0.14f * liquidBloom),
                        GoldBright.copy(alpha = (if (isDark) 0.20f else 0.18f) + 0.12f * liquidBloom)
                    ),
                    startY = lensTop + lensH * 0.62f,
                    endY = lensTop + lensH - 1.dp.toPx()
                ),
                topLeft = Offset(lensLeft + 3.dp.toPx(), lensTop + lensH * 0.60f),
                size = Size((lensW - 6.dp.toPx()).coerceAtLeast(1f), lensH * 0.38f),
                cornerRadius = CornerRadius(lensRadius, lensRadius)
            )

            // 6e. Prismatic optical refraction rim stroke (cyan-white-gold dispersion like iOS 27 Liquid Glass)
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = (if (isDark) 0.68f else 0.96f)),
                        Color(0xFF8BE9FF).copy(alpha = 0.32f + 0.36f * liquidBloom),
                        GoldBright.copy(alpha = 0.44f + 0.34f * liquidBloom),
                        Color.White.copy(alpha = (if (isDark) 0.48f else 0.78f))
                    ),
                    start = Offset(lensLeft + causticShiftPx * 0.5f, lensTop),
                    end = Offset(lensLeft + lensW - causticShiftPx * 0.5f, lensTop + lensH)
                ),
                topLeft = Offset(lensLeft, lensTop),
                size = Size(lensW, lensH),
                cornerRadius = CornerRadius(lensRadius, lensRadius),
                style = Stroke(width = (1.35f + 0.75f * liquidBloom).dp.toPx())
            )
        }

        // Layer 3: Tab Icons & Labels with real-time convex optical lens magnification
        val currentDropletCenter = if (animatedCenterPx > 0f) {
            animatedCenterPx
        } else {
            restingCenterPx
        }
        val slotWidthPx = if (dockWidthPx > 0f) dockWidthPx / tabs.size.toFloat() else 0f

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = if (isDraggingLiquid) hoveredTabIndex == index else selectedIndex == index
                val tabCenterX = Ios27LiquidGlassDockMath.visualCenterXForTab(
                    tabIndex = index,
                    tabCount = tabs.size,
                    dockWidthPx = dockWidthPx,
                    isRtl = isRtl
                )
                val proximity = if (slotWidthPx > 0f) {
                    Ios27LiquidGlassDockMath.lensProximity(
                        tabCenterX = tabCenterX,
                        dropletCenterX = currentDropletCenter,
                        slotWidthPx = slotWidthPx
                    )
                } else {
                    if (isSelected) 1f else 0f
                }

                // Optical magnification boost as the liquid glass lens passes over this tab
                val scale = 1f + 0.10f * proximity + (0.14f * proximity * liquidBloom)
                val liftPx = -(2.5f * proximity + 3.5f * proximity * liquidBloom)
                val tintColor = lerp(inactiveIconColor, activeIconColor, proximity)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .testTag(tab.testTag)
                        .semantics(mergeDescendants = true) {
                            role = Role.Tab
                            selected = selectedIndex == index
                            contentDescription = tab.label
                            onClick {
                                onSelectTab(tab)
                                true
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationY = liftPx
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (proximity > 0.55f) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(23.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        color = tintColor,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            lineHeight = 13.sp
                        ),
                        fontWeight = if (proximity > 0.55f) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
