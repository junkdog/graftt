package net.onedaybeard.graftt;

@Graft.Recipient(DeclaredField.class)
public class DeclaredFieldStaticFieldWithValueTransplant {
    private static final String name = "blake";

    @Override
    public String toString() {
        return name;
    }
}
