package net.onedaybeard.graftt



fun main() {
    // instantiate transplant before recipient is loaded by classloader
    SingleClassMethodTransplant()

    val scm = SingleClassMethod()
    scm.yo()
    println("invokedWithTransplant=" + SingleClassMethod.invokedWithTransplant)
    println("yoloCalled=" + scm.yoloCalled)
}
