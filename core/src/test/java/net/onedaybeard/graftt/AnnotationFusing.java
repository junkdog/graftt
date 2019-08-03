package net.onedaybeard.graftt;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.CLASS;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public interface AnnotationFusing {

    @Retention(CLASS)
    @Target({TYPE, METHOD, FIELD})

    @interface MyAnno { int value() default 0; }

    @Retention(RUNTIME)
    @Target({TYPE, METHOD, FIELD})
    @interface MyAnnoRt { int value() default 0; }

    class Bar {
        @MyAnno
        @MyAnnoRt
        public void hmm() {}
    }

    @Graft.Recipient(Bar.class)
    class BarTransplant {
        @Graft.Fuse
        public void hmm() {}
    }


    class Foo {
        @MyAnno
        @MyAnnoRt
        public void a() {}

        @MyAnnoRt
        public void b() {}

        public void c() {}

        @MyAnno
        public String aa;

        public String bb;
    }

    @Graft.Recipient(Foo.class)
    class FooTransplant {
        @Graft.Fuse(remove = {MyAnno.class, MyAnnoRt.class})
        @MyAnno
        public void a() {}

        @MyAnnoRt(1)
        public void b() {}

        public void c() {}

        @MyAnno
        public String aa;

        public String bb;
    }
}
