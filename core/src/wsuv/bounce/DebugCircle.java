package wsuv.bounce;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;

public class DebugCircle extends Sprite {
    int radius;
    public DebugCircle (MarsGame game, int startingRadius){
        super(game.am.get(MarsGame.RSC_DETECTION_CIRCLE, Texture.class));
        radius = startingRadius;
    }
}
