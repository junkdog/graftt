package net.onedaybeard.graftt;

public class SingleClassMethod {
    public final void yo() {
        yolo();
    }

    private void yolo() {
        yoloCalled = true;
    }

    public boolean yoloCalled = false;
    public static boolean invokedWithTransplant  = false;
}
