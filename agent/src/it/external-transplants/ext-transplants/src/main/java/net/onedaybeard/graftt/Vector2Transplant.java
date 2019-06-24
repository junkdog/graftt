package net.onedaybeard.graftt;

import com.badlogic.gdx.math.Vector2;

@Graft.Recipient(Vector2.class)
public class Vector2Transplant {
    @Graft.Fuse
    public String toString() {
        return "overloaded: " + toString();
    }
}
