package at.j0s.meyercard.app.adapter.export

import at.j0s.meyercard.app.BuildConfig
import at.j0s.meyercard.app.domain.CardLineStyle
import at.j0s.meyercard.app.domain.MeyerCard
import at.j0s.meyercard.app.domain.contentCode

/**
 * Not live yet — the subdomain is reserved but unpointed. Embedded regardless, so every file
 * exported from here on is already tagged with it once it does resolve, rather than only the
 * ones exported after that point.
 */
internal const val FECHTKARTE_URL = "https://fechtkarte.j0s.at/"

/**
 * A generated card's rights statement is CC0, deliberately distinct from the app's own
 * Apache-2.0 licence (`NOTICE`) — see [PdfRights]'s doc comment for why a rendered drill diagram
 * someone prints and shares doesn't carry a personal copyright claim the way the app's own
 * source code does.
 */
private const val CC0_DEED_URL = "https://creativecommons.org/publicdomain/zero/1.0/"

internal fun creationSoftware(): String = "Fechtkarte ${BuildConfig.VERSION_NAME}"

/**
 * The PNG tEXt keyword/text pairs embedded in every exported or shared image: the same content
 * code used for the filename (see [contentCode]) — carried into the image itself so it survives
 * a rename — which software produced it, and where to learn more.
 */
internal fun MeyerCard.pngMetadataEntries(lineStyle: CardLineStyle): List<Pair<String, String>> = listOf(
    "Software" to creationSoftware(),
    "Comment" to contentCode(lineStyle),
    "URL" to FECHTKARTE_URL,
)

/**
 * The PDF Document Info dictionary embedded in every exported PDF — see [pngMetadataEntries]
 * for the PNG equivalent, and [withPdfInfoDictionary] for why this is applied after the fact
 * rather than during rendering.
 */
internal fun MeyerCard.pdfInfo(lineStyle: CardLineStyle): PdfInfo {
    val code = contentCode(lineStyle)
    val software = creationSoftware()
    return PdfInfo(title = "Fechtkarte card $code", author = software, subject = FECHTKARTE_URL, creator = software)
}

/**
 * The rights-management fields embedded in every exported PDF's XMP packet — see [PdfRights]'s
 * own doc comment for why these are CC0, not the app's own Apache-2.0.
 */
internal fun pdfRights(): PdfRights = PdfRights(
    marked = false,
    rightsStatement = "CC0 1.0 Universal — no rights reserved",
    webStatementUrl = CC0_DEED_URL,
    usageTerms = "No rights reserved (CC0 1.0 Universal). See the linked deed for details.",
)
