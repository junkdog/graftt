package net.onedaybeard.graftt;

@Graft.Recipient(WantInterfaces.class)
public class WantInterfacesTransplant implements Point {
    @Graft.Mock private int x = 1;
    @Graft.Mock private int y = 2;


    @Override
    public int x() {
        return x;
    }

    @Override
    public int y() {
        return y;
    }
}
