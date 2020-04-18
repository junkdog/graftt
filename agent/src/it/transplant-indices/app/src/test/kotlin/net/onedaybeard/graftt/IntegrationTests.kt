package net.onedaybeard.graftt

import org.junit.Test

class IntegrationTests {

    @Test
    fun `verify foo transplant`() {
        Foo.foo()
    }

    @Test
    fun `verify bar transplant`() {
        Bar.bar()
    }

    @Test
    fun `verify bar2 transplant`() {
        Bar2.bar2()
    }
}
