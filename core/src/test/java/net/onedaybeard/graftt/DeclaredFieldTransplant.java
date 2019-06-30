package net.onedaybeard.graftt;

@Graft.Recipient(DeclaredField.class)
public class DeclaredFieldTransplant {
    private String suffix;

    @Graft.Fuse
    public String yolo() {
        return yolo() + " " + suffix;
    }
}
