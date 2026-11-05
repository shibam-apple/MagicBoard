package com.doubtboard.core.board

/**
 * The closed set of things a `BoardScript` step may put on the board (spec §4).
 * Anything else fails validation; the renderer never interprets free text.
 */
enum class BoardAction(val wireName: String) {
    WRITE("write"),
    EQUATION("equation"),
    ARROW_NOTE("arrow_note"),
    UNDERLINE("underline"),
    BOX("box"),
    HIGHLIGHT("highlight"),
    PLOT("plot"),
    REGION("region");

    companion object {
        private val byWireName = entries.associateBy { it.wireName }

        /** Returns the action for [name], or null when it is not in the closed set. */
        fun fromWireName(name: String): BoardAction? = byWireName[name]
    }
}
