package at.j0s.meyercard.app.adapter.ui.render

import android.graphics.Paint as NativePaint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import at.j0s.meyercard.app.domain.CARD_ASPECT_INVERSE
import at.j0s.meyercard.app.domain.CardPalette
import at.j0s.meyercard.app.domain.MeyerCard
import at.j0s.meyercard.app.domain.toCardPoint

// Fractions of card width — docs/PLAN.md §4, this app's own verified rendering constants.
internal const val CORNER_RADIUS_FRACTION = 0.02f
internal const val DISC_DIAMETER_FRACTION = 0.14f
// 0.05 originally (docs/PLAN.md §4); shrunk alongside Radius.INNER (see
// its own comment) once an exhaustive pairwise check of all 16 generator slots showed the
// original size left no combination of Radius.INNER/THRUST_DOT_GAP_FRACTION with a genuine
// positive margin everywhere — see DESIGN_CHOICES.md.
internal const val THRUST_DOT_DIAMETER_FRACTION = 0.03f
internal const val RAY_STROKE_WIDTH_FRACTION = 0.006f
internal const val BORDER_STROKE_WIDTH_FRACTION = 0.01f
// Breathing room between the thrust dot and its own disc — see thrustDotCenter(). Deliberately
// small: this is what makes a dot read as "belonging to" the disc right next to it rather than
// looking equidistant between two — the closer it sits to its own disc, the wider the margin
// to every *other* disc ends up being too, on every one of the 16 generator slots (checked
// exhaustively, not assumed) — see DESIGN_CHOICES.md.
internal const val THRUST_DOT_GAP_FRACTION = 0.015f
// Both fractions of card height (not width, unlike the others above): the tightest values
// that still clear every action disc/thrust dot in the real technique-card dataset, with a
// small buffer — checked by computing every technique card's disc/thrust-dot extent, not by
// eye. The worst case (card 99, action 1, SE at radius 0.752) extends to 92.44% of height;
// this banner occupies 93%-98%.
// Lining figures. UnifrakturMaguntia's default digits are text (old-style) figures, which vary
// in height and sit partly below the baseline — historically right for running text, but a
// sequence numeral has to be read at a glance inside a small disc, and there the uniform height
// of lining figures wins. Not a workaround for the 2017 font update: the version shipped before
// it defaulted to these same lining forms, so this keeps the numerals rendering exactly as they
// always have while the rest of the font moves forward. See DESIGN_CHOICES.md.
internal const val NUMERAL_FONT_FEATURES = "lnum"

internal const val BANNER_HEIGHT_FRACTION = 0.05f
internal const val BANNER_BOTTOM_MARGIN_FRACTION = 0.02f

internal fun cornerRadiusPx(cardWidthPx: Float) = cardWidthPx * CORNER_RADIUS_FRACTION
internal fun discRadiusPx(cardWidthPx: Float) = cardWidthPx * DISC_DIAMETER_FRACTION / 2f
internal fun thrustDotRadiusPx(cardWidthPx: Float) = cardWidthPx * THRUST_DOT_DIAMETER_FRACTION / 2f

internal data class ResolvedCardColors(val light: Color, val dark: Color, val ink: Color, val paper: Color)

/**
 * Which of [CardPalette]'s two verified colour sets (docs/PALETTE.md "Dark mode", T7.4) a card
 * actually renders with — extracted from `draw()` so the theme-selection logic is checkable on
 * the JVM without a Canvas, same reasoning as [thrustDotCenter] and friends.
 */
internal fun resolveColors(palette: CardPalette, isDarkTheme: Boolean): ResolvedCardColors = if (isDarkTheme) {
    ResolvedCardColors(
        light = Color(palette.darkThemeLight),
        dark = Color(palette.darkThemeDark),
        ink = Color(CardPalette.DARK_THEME_INK),
        paper = Color(CardPalette.DARK_THEME_PAPER),
    )
} else {
    ResolvedCardColors(
        light = Color(palette.light),
        dark = Color(palette.dark),
        ink = Color(CardPalette.INK),
        paper = Color(CardPalette.PAPER),
    )
}

/**
 * The thrust dot sits directly *below* its own numeral, clearing the disc by
 * [THRUST_DOT_GAP_FRACTION] — screen-down, the same for every slot regardless of direction or
 * [at.j0s.meyercard.app.domain.Radius]. That's what the notation means: a thrust is marked by
 * a small circle beneath the number, which is also exactly how the Learn screen describes it.
 *
 * Earlier versions offset the dot *toward the card centre* instead. Indistinguishable from
 * this for anything in the upper half of the card — and wrong for everything in the lower
 * half, where "toward the centre" is upward, so the dot rendered above its numeral rather
 * than below it. The bug survived a full exhaustive geometry check (which only ever asked
 * whether dots *overlapped* anything, never which side of the numeral they sat on) and its own
 * screenshot goldens, because every thrust in those fixtures happened to be an upper-half slot.
 *
 * Placing it below unconditionally is also strictly safer, not just more correct: it clears
 * every other disc across all 16 generator slots by a wider margin than the centre-facing
 * version managed, and stays comfortably inside the card border — checked exhaustively, see
 * the tests and DESIGN_CHOICES.md. It removes the [at.j0s.meyercard.app.domain.Radius.CENTRE]
 * degenerate case too: a disc at the card centre has no "toward the centre" direction, but
 * "below" is always well defined.
 */
internal fun thrustDotCenter(discCenter: Offset, cardWidthPx: Float): Offset {
    val offset = discRadiusPx(cardWidthPx) + thrustDotRadiusPx(cardWidthPx) + cardWidthPx * THRUST_DOT_GAP_FRACTION
    return Offset(discCenter.x, discCenter.y + offset)
}

/**
 * Draws [card] onto [canvas]. Steps 1-6 of docs/PLAN.md §4. Screen, PNG and
 * PDF export all call this — see DESIGN_CHOICES.md.
 *
 * [size]'s width is authoritative; height is derived from [CARD_ASPECT_INVERSE]
 * so the card never distorts regardless of what's passed. Callers (the
 * Composable wrapper, T2.4) are responsible for constraining their layout to
 * that same aspect ratio before calling this.
 *
 * [numeralTypeface] is loaded by the caller — a `Context` is needed to resolve
 * `R.font.unifraktur_maguntia` (see T2.3), and this renderer takes no `Context`
 * so it stays plain-JVM-constructible. Defaults to the platform font so callers
 * that haven't loaded the real typeface yet still render something legible.
 * The instruction banner's text is never blackletter — a technique instruction
 * needs to read instantly, not be decorative.
 *
 * [instructionText] is resolved by the caller for the same reason as [numeralTypeface]: the
 * banner's wording is a string resource (T9.8), resolving it needs `Resources`, and this
 * renderer deliberately has neither that nor a `Context`. Callers pass
 * `card.instruction?.displayName(resources)`; `null` (the default) draws no banner, which is
 * also the correct rendering for any card that has no instruction at all.
 *
 * [isDarkTheme] (T7.4) selects [CardPalette.darkThemeLight]/[darkThemeDark] and
 * [CardPalette.DARK_THEME_INK]/[DARK_THEME_PAPER] instead of the light-theme values — a
 * separately verified palette (docs/PALETTE.md "Dark mode"), not a filter applied to the
 * light-theme render. Defaults `false`: PNG/PDF export always renders light-theme regardless
 * of the device's theme, the same reasoning a printed page doesn't have a "dark mode" — only
 * the live on-screen Composable (T2.4's `MeyerSquareCard`) passes the device's actual setting.
 */
object CardRenderer {

    fun draw(
        canvas: Canvas,
        card: MeyerCard,
        size: Size,
        numeralTypeface: Typeface = Typeface.DEFAULT,
        instructionText: String? = null,
        isDarkTheme: Boolean = false,
    ) {
        val widthPx = size.width
        val heightPx = widthPx * CARD_ASPECT_INVERSE

        val (lightColor, darkColor, inkColor, paperColor) = resolveColors(card.palette, isDarkTheme)

        drawBackgroundAndQuadrants(canvas, widthPx, heightPx, lightColor, darkColor)
        drawRays(canvas, widthPx, heightPx, inkColor)
        drawBorder(canvas, widthPx, heightPx, inkColor)

        // Drawn before the action badges below, not after: technique cards can have up to 8
        // actions, and a badge whose disc falls within the banner's horizontal strip needs to
        // stay readable — the banner is supporting text, a numeral badge is the primary
        // content. Found on a real device; T2.4's screenshot test only ever exercised a
        // 2-action card, which never triggered this overlap.
        instructionText?.let { text ->
            drawInstructionBanner(canvas, text, widthPx, heightPx, paperColor, inkColor)
        }

        for (action in card.actions) {
            val point = action.slot.toCardPoint()
            // CardPoint's y is already expressed in units of card-width (see
            // domain/CardGeometry.kt), so both axes scale by widthPx.
            val discCenter = Offset(point.x * widthPx, point.y * widthPx)
            drawAction(canvas, discCenter, action.sequenceNumber, widthPx, inkColor, paperColor, numeralTypeface)

            if (action.isThrust) {
                val dotCenter = thrustDotCenter(discCenter, widthPx)
                canvas.drawCircle(dotCenter, thrustDotRadiusPx(widthPx), Paint().apply { color = inkColor })
            }
        }
    }

    private fun cardClipPath(widthPx: Float, heightPx: Float): Path =
        Path().apply {
            addRoundRect(RoundRect(0f, 0f, widthPx, heightPx, cornerRadiusPx(widthPx), cornerRadiusPx(widthPx)))
        }

    private fun drawBackgroundAndQuadrants(
        canvas: Canvas,
        widthPx: Float,
        heightPx: Float,
        lightColor: Color,
        darkColor: Color,
    ) {
        canvas.save()
        canvas.clipPath(cardClipPath(widthPx, heightPx))

        canvas.drawRect(0f, 0f, widthPx, heightPx, Paint().apply { color = lightColor })

        val darkPaint = Paint().apply { color = darkColor }
        canvas.drawRect(widthPx / 2f, 0f, widthPx, heightPx / 2f, darkPaint) // top-right
        canvas.drawRect(0f, heightPx / 2f, widthPx / 2f, heightPx, darkPaint) // bottom-left

        canvas.restore()
    }

    /**
     * Clipped to the same rounded-rect card shape [drawBackgroundAndQuadrants]
     * uses — the rays are drawn corner to corner of the card's bounding box,
     * which is *sharp*, so without this clip each ray's end pokes past the
     * border wherever the border's own corner curves inward (found on a real
     * printed export, not visible enough on screen to have been noticed
     * earlier).
     */
    private fun drawRays(canvas: Canvas, widthPx: Float, heightPx: Float, inkColor: Color) {
        canvas.save()
        canvas.clipPath(cardClipPath(widthPx, heightPx))

        val rayPaint = Paint().apply {
            color = inkColor
            strokeWidth = widthPx * RAY_STROKE_WIDTH_FRACTION
        }
        canvas.drawLine(Offset(0f, 0f), Offset(widthPx, heightPx), rayPaint)
        canvas.drawLine(Offset(widthPx, 0f), Offset(0f, heightPx), rayPaint)
        canvas.drawLine(Offset(widthPx / 2f, 0f), Offset(widthPx / 2f, heightPx), rayPaint)
        canvas.drawLine(Offset(0f, heightPx / 2f), Offset(widthPx, heightPx / 2f), rayPaint)

        canvas.restore()
    }

    private fun drawBorder(canvas: Canvas, widthPx: Float, heightPx: Float, inkColor: Color) {
        val borderPaint = Paint().apply {
            color = inkColor
            style = PaintingStyle.Stroke
            strokeWidth = widthPx * BORDER_STROKE_WIDTH_FRACTION
        }
        val radius = cornerRadiusPx(widthPx)
        canvas.drawRoundRect(0f, 0f, widthPx, heightPx, radius, radius, borderPaint)
    }

    private fun drawAction(
        canvas: Canvas,
        center: Offset,
        sequenceNumber: Int,
        widthPx: Float,
        inkColor: Color,
        paperColor: Color,
        numeralTypeface: Typeface,
    ) {
        val radius = discRadiusPx(widthPx)
        canvas.drawCircle(center, radius, Paint().apply { color = paperColor })
        canvas.drawCircle(
            center,
            radius,
            Paint().apply {
                color = inkColor
                style = PaintingStyle.Stroke
                strokeWidth = widthPx * BORDER_STROKE_WIDTH_FRACTION
            },
        )
        drawCenteredText(
            canvas,
            sequenceNumber.toString(),
            center,
            radius * 1.15f,
            inkColor,
            numeralTypeface,
            fontFeatures = NUMERAL_FONT_FEATURES,
        )
    }

    private fun drawInstructionBanner(
        canvas: Canvas,
        instructionText: String,
        widthPx: Float,
        heightPx: Float,
        paperColor: Color,
        inkColor: Color,
    ) {
        // Anchored near the bottom edge, not vertically centred: every E/W-direction action
        // sits exactly on the card's horizontal centre line by construction (E and W's own
        // edge points are both at y = centre), so a centred banner collides with any
        // technique card that has an E or W action — confirmed against the real dataset,
        // not assumed: 12 of the 21 technique cards hit this. BANNER_HEIGHT_FRACTION and
        // BANNER_BOTTOM_MARGIN_FRACTION were chosen so the banner's occupied band
        // (heightPx - bottomMargin - bannerHeight .. heightPx - bottomMargin) sits below
        // every action disc's lowest extent (including its own radius) across the real
        // dataset — empirically checked, not just visually eyeballed.
        val bannerHeight = heightPx * BANNER_HEIGHT_FRACTION
        val bottom = heightPx - heightPx * BANNER_BOTTOM_MARGIN_FRACTION
        val top = bottom - bannerHeight
        val margin = widthPx * 0.06f

        canvas.drawRect(margin, top, widthPx - margin, bottom, Paint().apply { color = paperColor })
        canvas.drawRect(
            margin,
            top,
            widthPx - margin,
            bottom,
            Paint().apply {
                color = inkColor
                style = PaintingStyle.Stroke
                strokeWidth = widthPx * BORDER_STROKE_WIDTH_FRACTION
            },
        )
        drawCenteredText(
            canvas,
            instructionText,
            Offset(widthPx / 2f, (top + bottom) / 2f),
            bannerHeight * 0.4f,
            inkColor,
        )
    }

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        center: Offset,
        textSizePx: Float,
        color: Color,
        typeface: Typeface = Typeface.DEFAULT,
        fontFeatures: String? = null,
    ) {
        val paint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            textAlign = NativePaint.Align.CENTER
            textSize = textSizePx
            this.typeface = typeface
            fontFeatureSettings = fontFeatures
        }
        val metrics = paint.fontMetrics
        val baselineY = center.y - (metrics.ascent + metrics.descent) / 2f
        canvas.nativeCanvas.drawText(text, center.x, baselineY, paint)
    }
}
