_psst. this is still WIP, a lot of core functionality hasn't been wired or implemented yet._

# graftt - annotation-driven bytecode surgery 

Extend existing classes by grafting bytecode from *Transplant* classes. Transplants are
plain java classes; annotations define interactions with the target class.

A custom classloader is responsible for applying the transformations at load-time. The
classloader is setup either using a javaagent or prior to referencing any targeted classes.

Sprung from [artemis-odb/570](https://github.com/junkdog/artemis-odb/issues/570), refer to
it for the discussion leading up to this.


## Use-cases
- Functionality for editors: callbacks and tighter tooling integration
- Additional debugging capabilities: logging, record stack traces, additional callbacks
- Extending existing functionality for final or otherwise non-extendable classes
- The odd bug fix in imported dependencies


## Example Transplant for ComponentMapper

```java
// a third-party class we wish to extend
public class SingleClassMethod {
    public final void yo() {
        yolo();
    }

    private void yolo() {
        yoloCalled = true;
    }

    public boolean yoloCalled = false;
    public static boolean invokedWithTransplant = false;
}


// begin transplant by specifying the recipient class.
@Graft.Target(SingleClassMethod.class)
public class SingleClassMethodTransplant {
    
    @Graft.Fuse // fuse with method in SingleClassMethod
    private void yolo() { // signature matches SingleClassMethod.yolo()
        SingleClassMethod.invokedWithTransplant = true; // insert new statement
        yolo(); // "recursive continuation", actually invokes SingleClassMethod::yolo 
    }
}
```

Once transplanted, decompiling the modified class yields something similar to:

```java
public class SingleClassMethod {
    public final void yo() {
        yolo();
    }

    private void yolo() {
        SingleClassMethod.invokedWithTransplant = true;
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

- **`@Graft.Target`** specifies which class to transplant to.
- **`@Graft.Fuse`** transplants bytecode over to `@Graft.Target`, translating any references to `ComponentMapperTransplant` -> `ComponentMapper`.
- **`@Graft.Mock`** to keep the compiler happy when you need to reference fields or methods in the target class. Mocked references point to target class after transplant.
- Any non-annotated methods or fields are copied over as-is.

Nice to have, but not now:
- ~~**`@Graft.Replace`**: Like `@Graft.Fuse` but removes the original method.~~ Better to resolve it when applying `@Graft.Fuse`.
- **`@Graft.Remove`**: Remove field or method from target class.

## Caveats
- You're working against internal implementation; there are no semver guarantees
- No retrofitting of interfaces or parent type on target class
- Annotations aren't fused (is there a use-case?)
- No `@Graft.Fuse` for constructors; nice to have, but not initially
- No GWT support
- No android support (possible but not planned)

