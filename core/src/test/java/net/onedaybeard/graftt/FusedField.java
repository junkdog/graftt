package net.onedaybeard.graftt;


public interface FusedField {

    class Foo {
        @Yolo2
        public String hmm;
    }

    @Graft.Recipient(Foo.class)
    class FooTransplant {

        // note: fusing fields only ever makes sense when
        // annotations need to be updated
        @Graft.Fuse @Yolo
        public String hmm;

        @Yolo
        public String transplantedWithAnnotation;
    }

    @Graft.Recipient(Foo.class)
    class FooWrongSigTransplant {

        @Graft.Fuse
        @Yolo
        public String ohNo;
    }

    @Graft.Recipient(Foo.class)
    class FooRemoverTransplant {

        @Graft.Fuse
        @Graft.Annotations(remove = Yolo2.class)
        public String hmm;
    }

    @Graft.Recipient(Foo.class)
    class FooWrongRemoverTransplant {

        @Graft.Fuse
        @Graft.Annotations(remove = Yolo.class) // not present
        public String hmm;
    }

    @interface Yolo {}
    @interface Yolo2 {}
}
