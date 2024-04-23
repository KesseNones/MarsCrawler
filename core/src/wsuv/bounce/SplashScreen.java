package wsuv.bounce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.ScreenUtils;

//Used to display splash screen at the start of the game.
public class SplashScreen extends ScreenAdapter {
    MarsGame mars;
    int timer;

    //Establishes new splash screen.
    public SplashScreen(MarsGame game) {
        mars = game;
        timer = 0;
    }

    @Override
    public void show() {
        Gdx.app.log("SplashScreen", "show");
    }

    //Displays splash screen image.
    public void render(float delta) {
        timer++;
        ScreenUtils.clear(0, 0, 0, 1);
        mars.am.update(10);

        //Moves on to load screen if time is up or player presses ESC.
        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE) || timer > 300){
            mars.setScreen(new LoadScreen(mars));
        }

        //Draws splash screen text.
        mars.batch.begin();
        if (mars.am.isLoaded(mars.RSC_SPLASH_TEXT_IMG, Texture.class)){
            mars.batch.draw(mars.am.get(mars.RSC_SPLASH_TEXT_IMG, Texture.class),
                    (Gdx.graphics.getWidth() / 2f) - 195, (Gdx.graphics.getHeight() / 2f) - 150);
        }

        //This causes the splash screen text to have a fading in effect.
        mars.batch.setColor(1, 1, 1, (float)timer / 180f);
        mars.batch.end();

    }
}
