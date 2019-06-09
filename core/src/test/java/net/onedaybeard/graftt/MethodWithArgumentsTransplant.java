package net.onedaybeard.graftt;

@Graft.Recipient(MethodWithArguments.class)
public class MethodWithArgumentsTransplant {

    @Graft.Fuse
    public int yolo(int a, int b) {
        return yolo(a - 1, b - a);
    }
}
