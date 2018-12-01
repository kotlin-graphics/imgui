package imgui

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class test : StringSpec() {

    init {

        "strcmp" {

            val a = "日本語".toCharArray(CharArray(32))
            val b = "日本語a".toCharArray(CharArray(32))

            println(String(a).compareTo(String(b)))
            a.cmp(b) shouldBe false

            val c = "Hello World!s".toCharArray(CharArray(32))
            val d = "Hello World!".toCharArray(CharArray(32))

            println(String(c).compareTo(String(d)))
            c.cmp(d) shouldBe false
        }
    }
}