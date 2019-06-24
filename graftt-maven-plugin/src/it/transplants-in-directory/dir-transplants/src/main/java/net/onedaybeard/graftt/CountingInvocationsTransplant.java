package net.onedaybeard.graftt;

@Graft.Recipient(CountingInvocations.class)
public class CountingInvocationsTransplant {

    int count = 0;

    @Graft.Fuse
    public void callMe() {
        callMe();
        count++;
    }
}
