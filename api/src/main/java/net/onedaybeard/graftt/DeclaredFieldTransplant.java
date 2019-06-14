package net.onedaybeard.graftt;

@Graft.Recipient(DeclaredField.class)
public class DeclaredFieldTransplant {
    // N.B. field values are only copied over for primitive types
    private String suffix = "blake";

    @Graft.Fuse
    public String yolo() {
        return yolo() + " " + suffix;
    }
}
