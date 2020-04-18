package net.onedaybeard.graftt;


@Graft.Recipient(Foo.class)
public class FooTransplant {
    @Graft.Fuse
    public static void foo() {
        // ok
    }
}
