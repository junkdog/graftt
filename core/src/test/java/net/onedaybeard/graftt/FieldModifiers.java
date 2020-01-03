package net.onedaybeard.graftt;


public interface FieldModifiers {

    class Foo {
        public int a;
        private final int b = 0;
        Foo c;
        public volatile static int d = 0;
    }

    // note that 'static' changes the get/put field instructions,
    // this doesn't propagate to existing methods
    @Graft.Recipient(Foo.class)
    class FooTransplant {
        @Graft.Fuse volatile int a;
        @Graft.Fuse transient int b;
        @Graft.Fuse public static FooTransplant c;
        @Graft.Fuse private static final int d = 0;
    }
}
