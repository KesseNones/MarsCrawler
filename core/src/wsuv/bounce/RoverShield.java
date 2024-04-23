package wsuv.bounce;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;

public class RoverShield extends Sprite {
    float opacity;

    public RoverShield(MarsGame game){
        super(game.am.get(MarsGame.RSC_SHIELD_SPRITE, Texture.class));
        opacity = 1.0f;
    }
}
