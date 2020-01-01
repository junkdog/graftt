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
     * All annotations are copied over to the recipient by default, excluding
     * any graft annotations. This annotation is only needed for removing
     * annotations and enabling overwriting annotations.
     */
    @Documented
    @Retention(CLASS)
    @Target({TYPE, FIELD, METHOD})
    public @interface Annotations {

        /**
         * Indicates which annotations to remove from the recipient.
         *
         * @return Annotations to remove
         */
        Class<? extends Annotation>[] remove() default {};


        /**
         * Indicates whether annotations that exist on both the transplant
         * and recipient should be overwritten.
         *
         * @return true if annotations should be overwritten
         */
        boolean overwrite() default false;
    }
}
