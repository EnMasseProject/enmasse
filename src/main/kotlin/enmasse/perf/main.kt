package enmasse.perf

public fun main(args: Array<String>) {
    val env = System.getenv("OPENSHIFT_USER");
    println("Heisann ${env}")
}