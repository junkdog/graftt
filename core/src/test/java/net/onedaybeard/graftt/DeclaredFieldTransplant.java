package net.onedaybeard.graftt;

@Graft.Recipient(DeclaredField.class)
public class DeclaredFieldTransplant {
    private String suffix = "hello";

    @Graft.Fuse
    public String yolo(String text) {
        return yolo(text) + suffix;
    }
}
