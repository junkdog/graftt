package net.onedaybeard.graftt;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Rewrite existing classes by grafting bytecode from {@code Transplant}
 * classes. Transplants are plain java classes; annotations define
 * interactions with the target class.
 */
public final class Graft {
    private Graft() {}

    /** Specifies the receiver of the transplant */
    @Documented
    @Retention(CLASS)
    @Target(TYPE)
    public @interface Recipient { Class<?> value(); }

    /**
     * Mock to keep the compiler happy when you need to reference
     * fields or methods in the target class. Mocked references point
     * to recipient class after transplant.
     */
    @Documented
    @Retention(CLASS)
    @Target({FIELD, METHOD})
    public @interface Mock {}

    /**
     * Transplant the compiled bytecode over to the recipient class,
     * translating any references so that they point to the target
     * class once transplanted.
     */
    @Documented
    @Retention(CLASS)
    @Target({METHOD, FIELD})
    public @interface Fuse {}

    /**
     * Specifies how to deal with annotations on recipient class. This
     * annotation is scoped to the immediate element it is decorating:
     * class, method or field.
     * <p/>
     * All annotations from fused methods and added fields are copied
     * over to the recipient by default. This annotation only needed for
     * removing annotations, and enabling overwriting/updating annotations.
     */
    @Documented
    @Retention(CLASS)
    @Target({TYPE, FIELD, METHOD})
    public @interface Annotations {
        Class<? extends Annotation>[] remove() default {};
        boolean overwrite() default false;
    }
}
