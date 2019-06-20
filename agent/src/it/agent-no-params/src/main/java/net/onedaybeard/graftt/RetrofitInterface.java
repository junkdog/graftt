package net.onedaybeard.graftt;

public interface RetrofitInterface {

    interface Bar {
        int hi();
    }

    class Foo {
        private int hello = 0xf00;
    }

    @Graft.Recipient(Foo.class)
    class FooTransplant implements Bar { // Bar interface transplanted to Foo
        @Graft.Mock
        int hello = 0;

        @Override
        public int hi() { return hello; } // Foo.hello == 0xf00
    }
}
