package net.onedaybeard.graftt;

public interface RetrofitGenericInterface {

    class Foo {}

    @Graft.Recipient(Foo.class)
    class FooTransplant implements InterfaceT<Boolean> {

        @Override
        public Boolean helloT() { return Boolean.TRUE; }
    }
}
