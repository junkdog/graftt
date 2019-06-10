package net.onedaybeard.graftt;

public class MockedMethod {
    private String more() {
        return "AAA";
    }

    public final String withMethod(String text) {
        return more() + " " + text;
    }
}
