package wsuv.bounce;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;

public class BatteryIndicator extends Sprite {
    float baseWidth;

    public BatteryIndicator (MarsGame game){
        super(game.am.get(MarsGame.RSC_BATT_FILLING, Texture.class));
        baseWidth = getWidth();
    }
}
