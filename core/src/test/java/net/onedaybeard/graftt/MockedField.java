package net.onedaybeard.graftt;

public class MockedField {
    private String prepend = "12345";

    public String withPrependField(String text) {
        return prepend + " " + text;
    }
}
