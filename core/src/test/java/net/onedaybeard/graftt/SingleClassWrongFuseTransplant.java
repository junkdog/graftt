package net.onedaybeard.graftt;

@Graft.Recipient(SingleClassMethod.class)
public class SingleClassWrongFuseTransplant {
    @Graft.Fuse
    private String yolo() { return ""; }
}
