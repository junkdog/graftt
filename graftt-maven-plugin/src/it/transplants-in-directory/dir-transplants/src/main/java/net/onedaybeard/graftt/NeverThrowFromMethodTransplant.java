package net.onedaybeard.graftt;

@Graft.Recipient(NeverThrowFromMethod.class)
public class NeverThrowFromMethodTransplant {

    @Graft.Fuse
    public String maybeDangerous() {
        try {
            return maybeDangerous();
        } catch (Exception e) {
            return "";
        }
    }
}
