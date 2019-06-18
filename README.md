# graftt - annotation-driven bytecode surgery 

Extend existing classes by grafting bytecode from *Transplant* classes. Transplants are
plain java classes and function like templates or blueprints; annotations define interactions
with the recipient class.

Transplants have complete access to the recipient class. Existing methods can be wrapped,
changed or replaced entirely.

An agent (`java -javaagent:agent.jar`) applies transplants at load-time. Alternatively, 
`graftt-maven-plugin` finds and applies transplants within `target/classes`.  


Sprung from [artemis-odb/570](https://github.com/junkdog/artemis-odb/issues/570), refer to
it for the discussion leading up to this.


## Use-cases
- Functionality for editors: callbacks and tighter tooling integration
- Additional debugging capabilities: logging, record stack traces, additional callbacks
- Extending existing functionality for final or otherwise non-extendable classes
- The odd bug fix in imported dependencies
- Add or modify `hashcode()` and `toString()`
- Retrofit classes with additional interfaces 


## Example Transplant for SomeClass

### A third-party class we wish to modify

```java
public class SomeClass {
    public final void yo() {
        yolo();
    }

    private void yolo() {
        // boo! we want to call "invokedWithTransplant = true"
        // here (for some reason or other), but yo() is final
        // and can't be extended, and this method is private
        //
        // ...
        //
        yoloCalled = true;
    }

    public boolean yoloCalled = false;
    public static boolean invokedWithTransplant = false;
}
```

### Create a _transplant_ class to donate some bytecode 

```java
@Graft.Recipient(SomeClass.class) // target to modify
public class SomeClassTransplant {
    
    @Graft.Fuse // fuse with method in SomeClass
    private void yolo() { // signature matches SomeClass.yolo()
        SomeClass.invokedWithTransplant = true; // whoop-whoop 
        yolo(); // "recursive continuation", actually invokes SomeClass::yolo  
    }
}
```

### Resulting class

Once transplanted, decompiling the modified class yields something similar to:

```java
public class SomeClass {
    public final void yo() {
        yolo();
    }

    private void yolo() {
        SomeClass.invokedWithTransplant = true;
        yolo$original();
    }

    private void yolo$original() {
        yoloCalled = true;
    }

    public boolean yoloCalled = false;
    public static boolean invokedWithTransplant = false;
}
```

## API 

- **`@Graft.Recipient`** specifies which class to transplant to.
- **`@Graft.Fuse`** transplants bytecode over to `@Graft.Recipient`, translating any
  `FooTransplant` references to `Foo`. Call the original method at any time by invoking the
  method currently being fused; e.g. Fusing `FooTransplant::bar` with `Foo::bar`, any
  call to `bar()` inside the transplant will point to `Foo::bar$original` once applied.
- **`@Graft.Mock`** to keep the compiler happy when you need to reference fields or
  methods in the target class. Mocked references point to target class after transplant.
- Any non-annotated methods or fields are copied over as-is.
- All interfaces are copied over to recipient.

Nice to have, but not now:
- **`@Graft.Remove`**: Remove field or method from target class.

## Caveats
- You're working against internal implementation; there are no semver guarantees
- No retrofitting of interfaces or parent type on target class
- Annotations aren't fused (is there a use-case?)
- No `@Graft.Fuse` for constructors; nice to have, but not initially
- No GWT support
- No android support (possible but not planned)

