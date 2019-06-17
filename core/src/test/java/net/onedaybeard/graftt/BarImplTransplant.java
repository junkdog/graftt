package net.onedaybeard.graftt;

@Graft.Recipient(BarImpl.class)
public class BarImplTransplant{

    // overriding method in BarBase by adding method to
    // final BarImpl. no annotation necessary as we have
    // no BarImpl::hello to fuse with.
    public String hello() {
        return "hi";
    }
}
