package net.onedaybeard.graftt;


public interface SubstituteAnnoValues {

    @interface AA { Class<?>[] value(); }
    @AA(BarTransplant.class) // expect AA(Bar.class) after transplant
    @Graft.Recipient(Foo.class)
    class FooTransplant {}
    class Foo {}

    @Graft.Recipient(Bar.class)
    class BarTransplant {}
    class Bar {}
}
