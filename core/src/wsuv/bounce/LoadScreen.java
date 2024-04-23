package wsuv.bounce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.ScreenUtils;

public class LoadScreen extends ScreenAdapter {
    MarsGame bounceGame;
    int frames;
    BitmapFont font;
    int scrollrate;
    int linesShown;
    String[] credits;

    public LoadScreen(MarsGame game) {
        bounceGame = game;
        linesShown = 10;
        scrollrate = 30;  // this is currently pixels/frame, not pixels/sec!

        // really our app will load quickly, but let's
        // fake a more complicated system... we'll wait
        // until we show all the credits...
        frames = 0;
        FileHandle file = Gdx.files.internal("credits.txt");
        credits = file.readString().split("\n");
    }

    @Override
    public void show() {
        Gdx.app.log("LoadScreen", "show");
    }

    public void render(float delta) {

        int credits_offset = frames < 120 ? 0 : (frames - 120) / scrollrate;
        ScreenUtils.clear(0, 0, 0, 1);
        // let the AssetManager load for 15 milliseconds (~1 frame)
        // this happens in another thread
        bounceGame.am.update(10);

        if (font == null && bounceGame.am.isLoaded(MarsGame.RSC_MONO_FONT)) {
            font = bounceGame.am.get(MarsGame.RSC_MONO_FONT);
        } else if (bounceGame.am.isFinished() && (credits_offset >= credits.length || Gdx.input.isKeyPressed(Input.Keys.SPACE)) ) {
            bounceGame.setScreen(new PlayScreen(bounceGame));
        } else if (font != null) {
            // once the font is loaded, start showing credits.
            // we'll assume a fairly smooth framerate at just start scrolling
            // at a fixed rate per frame until all credits are off screen
            // then we'll switch to the playing state.
            bounceGame.batch.begin();
            float y = 400;
            float lineHeight = font.getLineHeight();
            font.draw(bounceGame.batch, "Thanks to...", 20f, y + 2 * lineHeight);

            for(int i = 0; i < linesShown && (credits_offset + i) < credits.length; i++) {
                font.draw(bounceGame.batch, credits[credits_offset + i], 20f, y);
                y -= lineHeight;
            }
            bounceGame.batch.end();
            frames += 1;
        }
    }
}
