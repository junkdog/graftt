package net.onedaybeard.graftt;

@Graft.Recipient(MethodWithArguments.class)
public class MethodWithArgumentsTransplant {

    @Graft.Fuse
    public int update(int a, int b) {
        return update(a - 1, b + 1);
    }
}
