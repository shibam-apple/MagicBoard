package com.doubtboard.core.board

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BoardActionTest {
    @Test
    fun `closed set matches the spec`() {
        BoardAction.entries.map { it.wireName } shouldContainExactly
            listOf("write", "equation", "arrow_note", "underline", "box", "highlight", "plot", "region")
    }

    @Test
    fun `known names round-trip`() {
        BoardAction.entries.forEach { BoardAction.fromWireName(it.wireName) shouldBe it }
    }

    @Test
    fun `unknown actions are rejected`() {
        BoardAction.fromWireName("fade_in").shouldBeNull()
        BoardAction.fromWireName("Equation").shouldBeNull()
    }
}
