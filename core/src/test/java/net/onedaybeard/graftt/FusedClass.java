package net.onedaybeard.graftt;


public interface FusedClass {
    @interface AA {}
    @interface BB {}

    @AA
    class Foo {
        public String hmm;
    }

    @BB
    @Graft.Recipient(Foo.class)
    class FooTransplant {

    }

    @AA
    @Graft.Recipient(Foo.class)
    class FooClashingTransplant {

    }

    @Graft.Recipient(Foo.class)
    @Graft.Annotations(remove = AA.class)
    class FooRemoverTransplant {

    }
}
