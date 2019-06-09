package net.onedaybeard.graftt;

@Graft.Recipient(SingleClassMethod.class)
public class SingleClassMethodTransplant {
    @Graft.Fuse
    private void yolo() {
        SingleClassMethod.invokedWithTransplant = true;
        yolo(); // SingleClassMethod::yolo
    }
}
