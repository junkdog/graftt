package net.onedaybeard.graftt;

public interface RetrofitInterface2 {

    interface Bar {
        int hi();
    }

    class Foo {
        int hi() { return 0xf00; }
    }

    @Graft.Recipient(Foo.class)
    class FooTransplant implements Bar { // Bar interface transplanted to Foo

        // Foo already implements hi(); avoid transplanting
        // it by declaring this hi() a mock.
        @Graft.Mock @Override
        public int hi() { return 0; }
    }
}
