package net.onedaybeard.graftt;

public class NeverThrowFromMethod {

    private int n = 0;

    public String maybeDangerous() {
        if (n++ % 2 == 0) {
            throw new NullPointerException();
        } else {
            return "yolo";
        }
    }
}
