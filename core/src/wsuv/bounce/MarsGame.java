package wsuv.bounce;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;

import java.util.Random;

public class MarsGame extends Game {
    public static final int RSC_EXPLOSION_FRAMES_ROWS = 8;
    public static final int RSC_EXPLOSION_FRAMES_COLS = 8;
    public static final String RSC_EXPLOSION_FRAMES = "explosion8x8.png";

    //Used in indicating to the player the current game state.
    public static final String RSC_DEATH_MSG = "deathMessage.png";
    public static final String RSC_GAME_OVER_MSG = "gameOverText.png";
    public static final String RSC_GAME_VICTORY_MSG = "gameWinMessage.png";
    public static final String RSC_KEY_PROMPT_MSG = "keyPromptText.png";
    public static final String RSC_LVL_VICTORY_MSG = "levelVictoryMessage.png";

    public static final String RSC_MONO_FONT_FILE = "JetBrainsMono-Regular.ttf";
    public static final String RSC_MONO_FONT = "JBM.ttf";
    public static final String RSC_EXPLOSION_SFX = "explosion7s.wav";
    public static final String RSC_SPLASH_TEXT_IMG = "splashText.png";

    public static final String RSC_GROUND_TILE_IMG = "groundTile.png";
    public static final String RSC_VOID_TILE_IMG = "voidTile.png";

    public static final String RSC_WALL_TILE_IMG = "wallTile.png";

    public static final String RSC_ROVER = "rover.png";
    public static final String RSC_MINERAL = "mineralSprite.png";
    public static final String RSC_BIG_MINERAL = "mineralSpriteBIG.png";
    public static final String RSC_ROVER_W_MINERAL = "roverCarryingMineral.png";
    public static final String RSC_SHIELD_PWRP = "shieldPowerupSprite.png";
    public static final String RSC_SHIELD_SPRITE = "shieldSprite.png";
    public static final String RSC_SHIELD_TEXT = "shieldIndicator.png";

    public static final String RSC_0 = "zeroSprite.png";
    public static final String RSC_1 = "oneSprite.png";
    public static final String RSC_2 = "twoSprite.png";
    public static final String RSC_3 = "threeSprite.png";
    public static final String RSC_4 = "fourSprite.png";
    public static final String RSC_5 = "fiveSprite.png";
    public static final String RSC_6 = "sixSprite.png";
    public static final String RSC_7 = "sevenSprite.png";
    public static final String RSC_8 = "eightSprite.png";
    public static final String RSC_9 = "nineSprite.png";

    public static final String RSC_TIMES = "timesSymbolSprite.png";
    public static final String RSC_LVL = "levelSprite.png";

    public static final String RSC_OFF_TELEPORT = "inactiveTeleporter.png";
    public static final String RSC_ON_TELEPORT = "activeTeleporter.png";

    public static final String RSC_BATT_CASE = "batteryCase.png";
    public static final String RSC_BATT_FILLING = "batteryFilling.png";
    public static final String RSC_CHG_TXT = "chargeText.png";
    public static final String RSC_SPNT_TXT = "sprintText.png";
    public static final String RSC_CHG_STATION = "chargeStationSprite.png";

    public static final String RSC_BLOB = "blobMonster.png";
    public static final String RSC_CHASE_BLOB = "blobMonsterAngry.png";
    public static final String RSC_GAS_BLOB = "blobMonsterGasForm.png";
    public static final String RSC_DBG_TILE = "pathOverlay.png";
    public static final String RSC_DETECTION_CIRCLE = "playerDetectionCircle.png";

    AssetManager am;  // AssetManager provides a single source for loaded resources
    SpriteBatch batch;

    Random random = new Random();

    //Music music;
    @Override
    public void create() {
        am = new AssetManager();

		/* True Type Fonts are a bit of a pain. We need to tell the AssetManager
           a bit more than simply the file name in order to get them into an
           easily usable (BitMap) form...
		 */
        FileHandleResolver resolver = new InternalFileHandleResolver();
        am.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
        am.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolver));
        FreetypeFontLoader.FreeTypeFontLoaderParameter myFont = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
        myFont.fontFileName = RSC_MONO_FONT_FILE;
        myFont.fontParameters.size = 14;
        am.load(RSC_MONO_FONT, BitmapFont.class, myFont);

        // Load Textures after the font...
        am.load(RSC_EXPLOSION_FRAMES, Texture.class);
        am.load(RSC_SPLASH_TEXT_IMG, Texture.class);
        am.load(RSC_GROUND_TILE_IMG, Texture.class);
        am.load(RSC_WALL_TILE_IMG, Texture.class);
        am.load(RSC_VOID_TILE_IMG, Texture.class);
        am.load(RSC_ROVER, Texture.class);
        am.load(RSC_MINERAL, Texture.class);
        am.load(RSC_BIG_MINERAL, Texture.class);
        am.load(RSC_ROVER_W_MINERAL, Texture.class);
        am.load(RSC_OFF_TELEPORT, Texture.class);
        am.load(RSC_ON_TELEPORT, Texture.class);
        am.load(RSC_0, Texture.class);
        am.load(RSC_1, Texture.class);
        am.load(RSC_2, Texture.class);
        am.load(RSC_3, Texture.class);
        am.load(RSC_4, Texture.class);
        am.load(RSC_5, Texture.class);
        am.load(RSC_6, Texture.class);
        am.load(RSC_7, Texture.class);
        am.load(RSC_8, Texture.class);
        am.load(RSC_9, Texture.class);
        am.load(RSC_TIMES, Texture.class);
        am.load(RSC_LVL, Texture.class);
        am.load(RSC_BLOB, Texture.class);
        am.load(RSC_DBG_TILE, Texture.class);
        am.load(RSC_DEATH_MSG, Texture.class);
        am.load(RSC_GAME_OVER_MSG, Texture.class);
        am.load(RSC_GAME_VICTORY_MSG, Texture.class);
        am.load(RSC_KEY_PROMPT_MSG, Texture.class);
        am.load(RSC_LVL_VICTORY_MSG, Texture.class);
        am.load(RSC_DETECTION_CIRCLE, Texture.class);
        am.load(RSC_CHASE_BLOB, Texture.class);
        am.load(RSC_SHIELD_PWRP, Texture.class);
        am.load(RSC_SHIELD_SPRITE, Texture.class);
        am.load(RSC_GAS_BLOB, Texture.class);
        am.load(RSC_BATT_CASE, Texture.class);
        am.load(RSC_BATT_FILLING, Texture.class);
        am.load(RSC_CHG_TXT, Texture.class);
        am.load(RSC_SPNT_TXT, Texture.class);
        am.load(RSC_CHG_STATION, Texture.class);
        am.load(RSC_SHIELD_TEXT, Texture.class);

        // Load Sounds
        am.load(RSC_EXPLOSION_SFX, Sound.class);

        batch = new SpriteBatch();
        setScreen(new SplashScreen(this));

        // start the music right away.
        // this one we'll only reference via the GameInstance, and it's streamed
        // so, no need to add it to the AssetManager...
//        music = Gdx.audio.newMusic(Gdx.files.internal("sadshark.mp3"));
//        music.setLooping(true);
//        music.setVolume(.5f);
//        music.play();
    }

    @Override
    public void dispose() {
        batch.dispose();
        am.dispose();
    }
}