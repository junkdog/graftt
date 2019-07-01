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
    @java.lang.annotation.Target(TYPE)
    public @interface Recipient { Class<?> value(); }

    /**
     * Mock to keep the compiler happy when you need to reference
     * fields or methods in the target class. Mocked references point
     * to recipient class after transplant.
     */
    @Documented
    @Retention(CLASS)
    @java.lang.annotation.Target({FIELD, METHOD})
    public @interface Mock {}

    /**
     * Transplant the compiled bytecode over to the recipient class,
     * translating any references so that they point to the target
     * class once transplanted.
     */
    @Documented
    @Retention(CLASS)
    @java.lang.annotation.Target(METHOD)
    public @interface Fuse {}
}
