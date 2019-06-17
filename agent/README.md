## graftt - agent

A java-agent intercepting classes before they reach the classloader. For the agent to work,
it must know about the transplant classes before loading the recipient classes (`@Graft.Target`).


### Usage

Start java with the agent.

```bash
java -javaagent:graftt-agent-$VERSION.jar ...
```

With the agent running, next up is registering transplant classes. This should be done as early as
possible during the program's execution - at least before the classloader knows about the
recipient classes. 

Instantiate transplants to register them.

```kotlin
fun main() {
    // let the agent know about the transplant before SingleClassMethod is loaded by classloader;
    // the easiest approach is to instantiate each transplant at the beginning of the program.
    SingleClassMethodTransplant() // agent registers donor

    val scm = SingleClassMethod() // donor transplanted by agent 
    scm.yo()
    println("invokedWithTransplant=" + SingleClassMethod.invokedWithTransplant)
    println("yoloCalled=" + scm.yoloCalled)
}
```