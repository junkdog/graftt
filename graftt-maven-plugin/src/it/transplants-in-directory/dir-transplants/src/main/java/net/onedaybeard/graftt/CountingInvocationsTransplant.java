package net.onedaybeard.graftt;

@Graft.Recipient(CountingInvocations.class)
public class CountingInvocationsTransplant {

    int count;

    @Graft.Fuse
    public void callMe() {
        callMe();
        count++;
    }
}
