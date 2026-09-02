package at.j0s.meyercard.app.domain

/**
 * A short, reversible encoding of what an exported image actually looks like: [MeyerCard]'s
 * actions and palette, plus the [lineStyle] it's rendered with (a display preference, not a
 * property of [MeyerCard] itself — see [at.j0s.meyercard.app.application.port.spi.CardExporter]).
 * Used as the exported file's name instead of an export timestamp, so exporting the same card
 * under the same line style twice reuses the same file instead of piling up identical
 * duplicates — and exporting it again under a *different* style names a genuinely different
 * file, since the image is genuinely different.
 *
 * Deliberately reversible rather than hashed: every bit here is meaningful — palette, line
 * style, then each action's direction/radius/thrust flag in sequence order — packed tight and
 * Crockford Base32-encoded (5 bits/character; 0/O, 1/I/L, U excluded so a written-down code
 * can't be misread), so the filename alone is enough to reconstruct the exact rendered image,
 * and two different images can never collide the way two different hash inputs theoretically
 * could. [decodeCardContent] exists to prove that property with a real round-trip test, not as
 * a decoder this app ships or calls in production — nothing in the UI needs to decode a
 * filename back into a card.
 *
 * [LINE_STYLE_BITS] gives [CardLineStyle] room for two more values beyond today's
 * `COMPASS`/`SEQUENCE` before the encoding would need to change shape — `docs/LINE_STYLE_DESIGN.md`
 * already names two more (`BRIDGE`, `NONE`), so 2 bits (4 slots) is sized to a concrete, already-
 * documented future rather than an arbitrary guess. Not free-floating headroom either: it
 * exactly fills 2 bits that were previously wasted as padding at the 8-action maximum (see
 * `CardContentCodeTest`'s length assertion), so today's filenames are no longer than before.
 *
 * [VERSION_BITS] exists because a code is no longer only a filename this app reads back the same
 * day — it's now also embedded as PNG/PDF metadata (`ExportMetadata.kt`), so it can outlive any
 * particular encoding shape by years on a printed card or a file someone kept. A future encoding
 * change (a new field, a wider bit width for a config option not designed yet) would otherwise
 * be indistinguishable from today's shape to a decoder — same alphabet, same general structure,
 * silently wrong bits instead of a clear "I don't understand this" the way a version number
 * gives it.
 *
 * **Not free, checked rather than assumed.** [BitWriter.toByteArray] rounds up to a whole byte
 * *before* Crockford's own 5-bits-per-character rounding runs, so the two padding bits
 * [LINE_STYLE_BITS]'s own comment describes aren't always still spare once 2 more bits compete
 * for them — a real `chars(n)` sweep for `n` = 1..8 actions shows most action counts unaffected,
 * but `n` = 3 and `n` = 8 (the generator's actual maximum) each cost 2 extra characters, caught
 * by `CardContentCodeTest`'s own length assertion rather than assumed to still hold. Still small
 * in absolute terms (12 characters at 8 actions, the worst case) next to a 16+-character hash,
 * which is the comparison that actually matters here.
 *
 * 2 bits, not 3: version value `3` (`0b11`, [VERSION_BITS]'s own max) is reserved as an
 * *extension* marker rather than assigned to a fourth real format — a future decoder that sees
 * it reads further bits for the real version number, the same escape-value technique UTF-8 uses
 * for a byte count beyond what one leading byte can hold. Nothing about that extension is built
 * here; there is no fourth format yet to extend into. Reserving the value now costs nothing and
 * avoids a breaking migration later — the alternative, discovering the need for a 4th version
 * only after 0/1/2 are all already in the wild, would mean shipping a decoder that has to guess
 * whether `3` means "version 3" or "read more," which is exactly the ambiguity this field exists
 * to avoid in the first place.
 *
 * [CURRENT_VERSION] is the only version [decodeCardContent] understands yet; it asserts on a
 * mismatch rather than guessing, since there is nothing else to decode a mismatched version *as*
 * until a second shape actually exists — that dispatch logic belongs with whichever future
 * change is the first to actually need it, not speculatively built now for versions 1-2 that
 * don't exist.
 *
 * Deliberately excludes [MeyerCard.hand] and [MeyerCard.origin]: [hand] only ever selects a
 * palette during generation and has no effect on rendering by itself once a palette is chosen,
 * and [origin] is either a generation timestamp (which would defeat the whole point — every
 * generated card would code differently even when visually identical) or a fixed historical
 * card number (already implied by which actions/palette it has).
 *
 * Only supports the two radii [DrillGenerator] ever produces ([Radius.INNER]/[Radius.OUTER]) —
 * every card this is actually called on is a Train-screen generated card, never a continuous
 * historical radius. A different radius fails loudly rather than silently encoding it wrong.
 */
fun MeyerCard.contentCode(lineStyle: CardLineStyle): String {
    val actionsBySequence = actions.sortedBy { it.sequenceNumber }
    val bits = BitWriter()
    bits.write(CURRENT_VERSION, VERSION_BITS)
    bits.write(palette.ordinal, PALETTE_BITS)
    bits.write(lineStyle.ordinal, LINE_STYLE_BITS)
    bits.write(actionsBySequence.size - 1, ACTION_COUNT_BITS)
    for (action in actionsBySequence) {
        bits.write(action.slot.direction.ordinal, DIRECTION_BITS)
        bits.write(radiusBit(action.slot.radius), RADIUS_BITS)
        bits.write(if (action.isThrust) 1 else 0, THRUST_BITS)
    }
    return crockfordBase32Encode(bits.toByteArray())
}

/** The inverse of [contentCode] — see that function's own doc comment for why this exists. */
internal fun decodeCardContent(code: String): Triple<CardPalette, CardLineStyle, List<Action>> {
    val bits = BitReader(crockfordBase32Decode(code))
    val version = bits.read(VERSION_BITS)
    require(version == CURRENT_VERSION) { "Unsupported content code version: $version (this decoder only understands $CURRENT_VERSION)" }
    val palette = CardPalette.entries[bits.read(PALETTE_BITS)]
    val lineStyle = CardLineStyle.entries[bits.read(LINE_STYLE_BITS)]
    val actionCount = bits.read(ACTION_COUNT_BITS) + 1
    val actions = (1..actionCount).map { sequenceNumber ->
        val direction = Direction.entries[bits.read(DIRECTION_BITS)]
        val radius = if (bits.read(RADIUS_BITS) == 1) Radius.OUTER else Radius.INNER
        val isThrust = bits.read(THRUST_BITS) == 1
        Action(sequenceNumber, Slot(direction, radius), isThrust)
    }
    return Triple(palette, lineStyle, actions)
}

private fun radiusBit(radius: Radius): Int = when (radius) {
    Radius.OUTER -> 1
    Radius.INNER -> 0
    else -> error("contentCode only supports Radius.INNER/OUTER, was $radius")
}

private const val CURRENT_VERSION = 0
private const val VERSION_BITS = 2
private const val PALETTE_BITS = 3
private const val LINE_STYLE_BITS = 2
private const val ACTION_COUNT_BITS = 3
private const val DIRECTION_BITS = 3
private const val RADIUS_BITS = 1
private const val THRUST_BITS = 1

/**
 * Excludes I, L, O, U from standard Base32 — the four letters most easily misread as 1, 1, 0
 * and V, or confused for each other — matching Douglas Crockford's own Base32 variant. Not
 * required for correctness (nothing in this app ever hand-types one of these codes), but it's
 * the standard choice for a compact code and costs nothing to keep.
 */
private const val CROCKFORD_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

private fun crockfordBase32Encode(bytes: ByteArray): String {
    val reader = BitReader(bytes)
    val totalBits = bytes.size * 8
    val charCount = (totalBits + 4) / 5
    var bitsLeft = totalBits
    return buildString {
        repeat(charCount) {
            val chunkBits = minOf(5, bitsLeft)
            val value = reader.read(chunkBits) shl (5 - chunkBits)
            append(CROCKFORD_ALPHABET[value])
            bitsLeft -= chunkBits
        }
    }
}

/**
 * The exact inverse of [crockfordBase32Encode] for strings *this file* produced — recovering
 * the original byte count from the code's length only works because the encoder always emits
 * `ceil(byteCount * 8 / 5)` characters for a given `byteCount`, which [crockfordBase32Decode]
 * inverts arithmetically; it is not a general-purpose Base32 decoder for arbitrary input.
 */
private fun crockfordBase32Decode(code: String): ByteArray {
    val byteCount = (code.length * 5) / 8
    val totalBits = byteCount * 8
    val writer = BitWriter()
    var bitsWritten = 0
    for (char in code) {
        val value = CROCKFORD_ALPHABET.indexOf(char.uppercaseChar())
        require(value >= 0) { "Invalid Crockford Base32 character: '$char'" }
        val chunkBits = minOf(5, totalBits - bitsWritten)
        writer.write(value shr (5 - chunkBits), chunkBits)
        bitsWritten += chunkBits
    }
    return writer.toByteArray()
}

/** Accumulates fields of a known bit width, MSB-first, then packs them into whole bytes. */
private class BitWriter {
    private var accumulator = 0L
    private var bitCount = 0

    fun write(value: Int, bits: Int) {
        accumulator = (accumulator shl bits) or (value.toLong() and ((1L shl bits) - 1))
        bitCount += bits
    }

    /** Trailing bits beyond the last whole byte are zero-padded; [BitReader] never reads them. */
    fun toByteArray(): ByteArray {
        val byteCount = (bitCount + 7) / 8
        val aligned = accumulator shl (byteCount * 8 - bitCount)
        return ByteArray(byteCount) { i -> ((aligned shr (8 * (byteCount - 1 - i))) and 0xFF).toByte() }
    }
}

/** Reads fields of a known bit width back out of a byte array written by [BitWriter], in order. */
private class BitReader(private val bytes: ByteArray) {
    private var bitPos = 0

    fun read(bits: Int): Int {
        var result = 0
        repeat(bits) {
            val byteIndex = bitPos / 8
            val bitIndexFromMsb = 7 - (bitPos % 8)
            val bit = (bytes[byteIndex].toInt() shr bitIndexFromMsb) and 1
            result = (result shl 1) or bit
            bitPos++
        }
        return result
    }
}
