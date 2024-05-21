package wsuv.bounce;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.Iterator;

//Used to run the whole game.
public class PlayScreen extends ScreenAdapter {
    private enum SubState {READY, DEAD, GAME_OVER, PLAYING, LEVEL_WON, GAME_VICTORY}
    private boolean gameHasEnded;
    private boolean wonLevel;
    private int lives;
    private MarsGame marsGame;
    private HUD hud;
    private SubState state;
    private int level;
    private float timer;
    private boolean godModeEnabled;
    private boolean debugDisplayEnabled;
    private TileMap tiles;
    private String[] tileTextures;
    private Rover rover;
    private RoverShield shield;
    private BatteryIndicator batt;
    private int previousRoverIndex;
    private int[][] mineralLocations;
    private int[][] shieldPowerupLocations;
    private int numberofShieldsBase;
    private int currentNumberOfShields;
    private int mineralsLeft;
    private int mineralsForLevel;

    private int[] telaporterCenter;
    private int[][] chargeStationLocations;

    private String[] numberSprites;

    private Blob[] blobs;

    private PathFinder blobGuide;
    private DebugCircle blobRadarIndicator;

    public void establishMineralCoordSet(int tileIndex, int index){
        int[] coords = tiles.indexToCoords(tileIndex);
        mineralLocations[index][0] = coords[0] + (tiles.tileSize / 2);
        mineralLocations[index][1] = coords[1] + (tiles.tileSize / 2);
    }

    //Starts new game.
    public PlayScreen(MarsGame game) {
        game.batch.setColor(1, 1, 1, 1);
        godModeEnabled = false;
        debugDisplayEnabled = false;
        timer = 0;
        lives = 3;
        gameHasEnded = false;
        wonLevel = false;
        marsGame = game;
        hud = new HUD(marsGame.am.get(MarsGame.RSC_MONO_FONT));
        tiles = new TileMap();
        tiles.readFileToTileData("testMap.txt");
        level = 1;

        int roverStartIndex = tiles.coordsToIndex(10 * 30, 15 * 30);
        previousRoverIndex = roverStartIndex;
        int[] coords = tiles.indexToCoords( roverStartIndex );
        rover = new Rover(marsGame, coords[0], coords[1], tiles);
        shield = new RoverShield(game);
        batt = new BatteryIndicator(game);

        mineralsForLevel = 16;
        mineralsLeft = mineralsForLevel;
        placeMinerals();

        //Sets up pathfinder and graph for blobs to use for pathfinding.
        blobGuide = new PathFinder(tiles);
        blobGuide.dsAlg(previousRoverIndex);

        numberofShieldsBase = 4;
        currentNumberOfShields = numberofShieldsBase;
        placeShields();

        //Sets up game's blobs with initial positions.
        blobs = new Blob[4];
        placeBlobs();

        blobRadarIndicator = new DebugCircle(game, blobs[0].distanceThresholdInit);

        tileTextures = new String[3];
        tileTextures[0] = MarsGame.RSC_VOID_TILE_IMG;
        tileTextures[1] = MarsGame.RSC_GROUND_TILE_IMG;
        tileTextures[2] = MarsGame.RSC_WALL_TILE_IMG;

        numberSprites = new String[10];
        numberSprites[0] = MarsGame.RSC_0;
        numberSprites[1] = MarsGame.RSC_1;
        numberSprites[2] = MarsGame.RSC_2;
        numberSprites[3] = MarsGame.RSC_3;
        numberSprites[4] = MarsGame.RSC_4;
        numberSprites[5] = MarsGame.RSC_5;
        numberSprites[6] = MarsGame.RSC_6;
        numberSprites[7] = MarsGame.RSC_7;
        numberSprites[8] = MarsGame.RSC_8;
        numberSprites[9] = MarsGame.RSC_9;

        telaporterCenter = tiles.indexToCoords(309);
        telaporterCenter[0] += 30;
        telaporterCenter[1] += 30;

        placeChargers();

        // the HUD will show FPS always, by default.  Here's how
        // to use the HUD interface to silence it (and other HUD Data)
        hud.setDataVisibility(HUDViewCommand.Visibility.WHEN_OPEN);

        //Cheat command that adds one life to the player's life counter,
        // giving them more room to fail.
        hud.registerAction("addlife", new HUDActionCommand() {
            static final String desc = "Adds life to lives.";

            @Override
            public String execute(String[] cmd) {
                lives++;
                return "Life Added";
            }

            public String help(String[] cmd){
                return desc;
            }
        });

        //Makes the player immune to death
        hud.registerAction("godmode", new HUDActionCommand() {
            static final String desc = "Makes the player immune to blobs while enabled.";

            @Override
            public String execute(String[] cmd) {
                String godString;
                godModeEnabled = !godModeEnabled;
                if (godModeEnabled){
                    godString = "God Mode Enabled";
                }else{
                    godString = "God Mode Disabled";
                }
                return godString;
            }

            public String help(String[] cmd){
                return desc;
            }
        });

        //Enables/disables debug pathfinding display.
        hud.registerAction("debug", new HUDActionCommand() {
            static final String desc = "Enables/disables blob pathfinding display.";

            @Override
            public String execute(String[] cmd) {
                String debugStr;
                debugDisplayEnabled = !debugDisplayEnabled;
                if (debugDisplayEnabled){
                    debugStr = "Debug Display Enabled";
                }else{
                    debugStr = "Debug Display Disabled";
                }
                return debugStr;
            }

            public String help(String[] cmd){
                return desc;
            }
        });

        //Causes the player to win level by destroying all bricks.
        hud.registerAction("winlevel", new HUDActionCommand() {
            static final String desc = "Wins the level for the player with no effort.";

            @Override
            public String execute(String[] cmd) {
                state = SubState.LEVEL_WON;
                rover.dropMineral();
                timer = 0;
                return "Conglaturation !!! Your Winner   !";
            }

            public String help(String[] cmd){
                return desc;
            }
        });

        //Kills player causing life loss/game over.
        hud.registerAction("die", new HUDActionCommand() {
            static final String desc = "Causes the player to instantly enter \n    " +
                    "the losing state and lose a life \n    and game over if no lives are left.";

            @Override
            public String execute(String[] cmd) {
                timer = 0;
                state = SubState.DEAD;

                //Properly drops mineral at desired location.
                if (rover.carriesMineral()){
                    float[] currRoverLoc = rover.getCenter();
                    for (int i = 0; i < mineralLocations.length; i++){
                        if (mineralLocations[i][0] == 0){
                            mineralLocations[i][0] = (int)currRoverLoc[0] - (tiles.tileSize / 2);
                            mineralLocations[i][1] = (int)currRoverLoc[1] - (tiles.tileSize / 2);
                            break;
                        }
                    }
                }
                rover.dropMineral();
                rover.murder();
                rover.isShielded = false;
                shield.opacity = 1;
                lives--;
                if (lives < 1){
                    state = SubState.GAME_OVER;
                }
                return "You died!";
            }

            public String help(String[] cmd){
                return desc;
            }
        });

        //Causes player to instantly lose game.
        hud.registerAction("losegame", new HUDActionCommand() {
            static final String desc = "Causes the player to instantly enter \n    " +
                    "the game over state and lose the game";

            @Override
            public String execute(String[] cmd) {
                timer = 0;
                lives = 0;
                state = SubState.GAME_OVER;
                rover.dropMineral();
                rover.murder();

                return "You lost the game!";
            }

            public String help(String[] cmd){
                return desc;
            }
        });

        //Causes player to instantly win game.
        hud.registerAction("wingame", new HUDActionCommand() {
            static final String desc = "Causes the player to instantly enter \n    " +
                    "the game victory state and win the game";

            @Override
            public String execute(String[] cmd) {
                timer = 0;
                rover.dropMineral();
                state = SubState.GAME_VICTORY;

                return "You won the game!";
            }

            public String help(String[] cmd){
                return desc;
            }
        });

        //This stops the entire game process. As a result,
        // it may need to be removed later if deemed too dangerous.
        hud.registerAction("exit", new HUDActionCommand() {
            static final String desc = "Exits the game entirely.";

            @Override
            public String execute(String[] cmd) {
                Gdx.app.exit();
                return "Goodbye!";
            }

            public String help(String[] cmd){
                return desc;
            }
        });

        //Sets level to input level if valid number in range 1 to 3.
        hud.registerAction("setlevel", new HUDActionCommand() {
            static final String desc = "Sets the level in range 1 to 3. \n    Usage: setlevel [LEVEL_NUMBER]";

            @Override
            public String execute(String[] cmd) {
                try {
                    int lvl = Integer.parseInt(cmd[1]);
                    if (lvl > 0 && lvl < 4){
                        state = SubState.READY;
                        timer = 0;
                        rover.dropMineral();
                        level = lvl;
                        rover.instantCharge();
                        rover.isShielded = false;
                        shield.opacity = 1;
                        placeBlobs();
                        placeMinerals();
                        placeChargers();
                        placeShields();
                    }else{
                        return "Invalid level";
                    }
                }catch (Exception e){
                    return desc;
                }

                return "Set level success";
            }

            public String help(String[] cmd){
                return desc;
            }
        });

        // we're adding an input processor AFTER the HUD has been created,
        // so we need to be a bit careful here and make sure not to clobber
        // the HUD's input controls. Do that by using an InputMultiplexer
        InputMultiplexer multiplexer = new InputMultiplexer();
        // let the HUD's input processor handle things first....
        multiplexer.addProcessor(Gdx.input.getInputProcessor());
        // then pass input to our new handler...
        Gdx.input.setInputProcessor(multiplexer);

    }

    //Useful game status info.
    @Override
    public void show() {
        Gdx.app.log("PlayScreen", "show");
        state = SubState.READY;
        level = 1;
    }

    //Randomly places two charge stations around the outer edges of the map.
    public void placeChargers(){
        chargeStationLocations = new int[2][2];
        for (int i = 0; i < chargeStationLocations.length; i++){
            int randomIndex = (int)(Math.random() * tiles.tileData.length);
            while (tiles.tileData[randomIndex] != 1 || (randomIndex <= 400 && randomIndex >= 200)){
                randomIndex = (int)(Math.random() * tiles.tileData.length);
            }
            //Sets coordinates of charge stations.
            int[] coords = tiles.indexToCoords(randomIndex);
            chargeStationLocations[i][0] = coords[0] + (tiles.tileSize / 2);
            chargeStationLocations[i][1] = coords[1] + (tiles.tileSize / 2);
        }
    }

    //Places shield powerups on level wherever they can be placed.
    public void placeShields(){
        currentNumberOfShields = numberofShieldsBase + (2 * (level - 1));
        shieldPowerupLocations = new int[currentNumberOfShields][2];

        for (int i = 0; i < currentNumberOfShields; i++){
            int randomIndex = (int)(Math.random() * tiles.tileData.length);
            while (tiles.tileData[randomIndex] != 1 || (randomIndex <= 400 && randomIndex >= 200)){
                randomIndex = (int)(Math.random() * tiles.tileData.length);
            }
            //Sets coordinates of given shield powerup.
            int[] coords = tiles.indexToCoords(randomIndex);
            shieldPowerupLocations[i][0] = coords[0] + (tiles.tileSize / 2);
            shieldPowerupLocations[i][1] = coords[1] + (tiles.tileSize / 2);
        }
    }

    //Places minerals randomly in the world.
    public void placeMinerals(){
        mineralsLeft = mineralsForLevel + (8 * (level - 1));
        mineralLocations = new int[mineralsLeft][2];

        for (int i = 0; i < mineralsLeft; i++){
            int randomIndex = (int)(Math.random() * tiles.tileData.length);
            while (tiles.tileData[randomIndex] != 1 || (randomIndex <= 400 && randomIndex >= 200)){
                randomIndex = (int)(Math.random() * tiles.tileData.length);
            }
            establishMineralCoordSet(randomIndex, i);
        }
    }

    //Randomly places blobs on the map when called.
    public void placeBlobs(){
        int[] blobCoords;
        //Randomly places 4 blobs on the map.
        for (int i = 0; i < blobs.length; i++){
            int randomIndex = (int)(Math.random() * tiles.tileData.length);
            while (tiles.tileData[randomIndex] != 1 || (randomIndex <= 400 && randomIndex >= 200)){
                randomIndex = (int)(Math.random() * tiles.tileData.length);
            }
            blobCoords = tiles.indexToCoords(randomIndex);
            blobs[i] = new Blob(marsGame, (float)blobCoords[0], (float)blobCoords[1], tiles, blobGuide);
            blobs[i].currentDistanceThreshold = blobs[i].distanceThresholdInit + (level - 1);
        }
    }

    //Updates the game based on state info and time delta.
    public void update(float delta) {
        timer += delta;

        //Moves forward based on if player pressed a key in the READY state.
        if (!hud.isOpen() && state == SubState.READY && Gdx.input.isKeyPressed(Input.Keys.ANY_KEY)) {
            state = SubState.PLAYING;
            rover.respawn();
        }

        //Death state of game.
        if (!hud.isOpen() && state == SubState.DEAD && rover.alive()){
            rover.dropMineral();
            rover.murder();
            rover.instantCharge();
            timer = 0;
            lives--;

            //Shifts to game over state if no lives remain.
            if (lives <= 0){
                state = SubState.GAME_OVER;
            }
        }

        //Game over state of game.
        if (!hud.isOpen() && state == SubState.GAME_OVER && timer > 5f){
            level = 1;
            timer = 0;
            lives = 3;
            state = SubState.READY;
            rover.instantCharge();
            rover.isShielded = false;
            shield.opacity = 1;
            placeBlobs();
            placeMinerals();
            placeChargers();
            placeShields();
        }

        //Dead rover case.
        if (!hud.isOpen() && state == SubState.DEAD && timer > 5f){
            state = SubState.READY;
            rover.instantCharge();
            placeBlobs();
        }

        //Level victory case.
        if (!hud.isOpen() && state == SubState.LEVEL_WON && timer > 5f){
            level++;
            state = SubState.READY;
            rover.instantCharge();
            rover.isShielded = false;
            shield.opacity = 1;
            placeBlobs();
            placeMinerals();
            placeChargers();
            placeShields();
        }

        //Game win case.
        if (!hud.isOpen() && state == state.GAME_VICTORY && timer > 5f){
            level = 1;
            timer = 0;
            lives = 3;
            state = SubState.READY;
            rover.instantCharge();
            rover.isShielded = false;
            shield.opacity = 1;
            placeBlobs();
            placeMinerals();
            placeChargers();
            placeShields();
        }

        //Rover level win that leads to game win.
        if (!hud.isOpen() && state == SubState.LEVEL_WON && (level + 1) > 3){
            state = SubState.GAME_VICTORY;
            timer = 0;
        }

        //Main playing state of game.
        if (!hud.isOpen() && state == SubState.PLAYING){
            if (rover.alive()){
                rover.moveRover();
                //If rover has moved, recompute pathfinding for blobs.
                if (previousRoverIndex != tiles.coordsToIndex(rover.getX(), rover.getY())){
                    blobGuide.dsAlg(tiles.coordsToIndex(rover.getX(), rover.getY()));
                    previousRoverIndex = tiles.coordsToIndex(rover.getX(), rover.getY());
                }
            }

            //Moves all existing blobs.
            for (Blob blob : blobs) {
                if (blob != null && blob.isAlive) {
                    blob.move(rover.isShielded, rover.getCenter());

                    //If blob is over rover, consume the rover and drop mineral if the rover's carrying one.
                    float xDelta = Math.abs(rover.getX() - blob.getX());
                    float yDelta = Math.abs(rover.getY() - blob.getY());
                    if (xDelta <= 15 && yDelta <= 15){
                        if (!rover.isShielded && !godModeEnabled && blob.state != (Blob.BlobState.VAPEROUS)){
                            //Drops mineral on the ground where the player is if the player is consumed by a blob.
                            if (rover.carriesMineral()){
                                float[] currRoverLoc = rover.getCenter();
                                for (int i = 0; i < mineralLocations.length; i++){
                                    if (mineralLocations[i][0] == 0){
                                        mineralLocations[i][0] = (int)currRoverLoc[0] - (tiles.tileSize / 2);
                                        mineralLocations[i][1] = (int)currRoverLoc[1] - (tiles.tileSize / 2);
                                        break;
                                    }
                                }
                            }
                            state = SubState.DEAD;
                            timer = 0;
                        }else{
                            if (rover.isShielded){
                                blob.state = Blob.BlobState.VAPEROUS;
                            }
                        }
                    }
                }
            }

            //Checks through each mineral and sees
            // if the existing ones have come into contact with the rover.
            for (int[] location : mineralLocations){
                //If mineral still exists,
                // check if it is in contact with rover.
                if (location[0] != 0){
                    float[] roverCoords = rover.getCenter();
                    int xDelta = Math.abs( ((int)roverCoords[0] - (tiles.tileSize / 2)) - location[0]);
                    int yDelta = Math.abs( ((int)roverCoords[1] - (tiles.tileSize / 2)) - location[1]);
                    if ((xDelta <= 15) && (yDelta <= 15) && (!rover.carriesMineral()) && rover.alive()){
                        location[0] = 0;
                        rover.pickUpMineral();
                    }
                }
            }

            //Detects if rover interacts with shield powerup.
            // Removes shield powerup from level if yes.
            for (int[] loc : shieldPowerupLocations){
                if (loc[0] != 0){
                    float[] roverLoc = rover.getCenter();
                    int xDelta = Math.abs( ((int)roverLoc[0] - (tiles.tileSize / 2)) - loc[0]);
                    int yDelta = Math.abs( ((int)roverLoc[1] - (tiles.tileSize / 2)) - loc[1]);
                    if ((xDelta <= 15) && (yDelta <= 15) && (!rover.isShielded) && rover.alive()){
                        loc[0] = 0;
                        rover.isShielded = true;
                    }
                }
            }

            //Shield powerup gradually runs down.
            if (rover.isShielded){
                shield.opacity -= 0.001;
                //Shield turns off if it ran out.
                if (shield.opacity <= 0){
                    rover.isShielded = false;
                    shield.opacity = 1.0f;
                }
            }

            //Makes it so player interacts with charging station.
            for (int[] loc : chargeStationLocations){
                if (loc[0] != 0){
                    float[] roverLoc = rover.getCenter();
                    int xDelta = Math.abs( ((int)roverLoc[0] - (tiles.tileSize / 2)) - loc[0]);
                    int yDelta = Math.abs( ((int)roverLoc[1] - (tiles.tileSize / 2)) - loc[1]);
                    if ((xDelta <= 15) && (yDelta <= 15) && rover.alive()){
                        rover.instantCharge();
                    }
                }
            }

            //Checks to see if player can drop of the mineral they're carrying.
            //Does so if yes.
            if (rover.carriesMineral()){
                float[] roverCoords = rover.getCenter();
                int xDelta = Math.abs( ((int)roverCoords[0] - (tiles.tileSize / 2)) - telaporterCenter[0]);
                int yDelta = Math.abs( ((int)roverCoords[1] - (tiles.tileSize / 2)) - telaporterCenter[1]);
                if ( (xDelta <= 30) && (yDelta <= 30)){
                    mineralsLeft--;
                    rover.dropMineral();
                    rover.instantCharge();
                    if (mineralsLeft < 1){
                        state = SubState.LEVEL_WON;
                        timer = 0;
                    }
                }
            }
        }
    }

    //Renders needed assets of game.
    @Override
    public void render(float delta) {
        update(delta);

        //Clears screen and renders any needed explosions.
        ScreenUtils.clear(0, 0, 0, 1);
        marsGame.batch.begin();

        //Draws tiles based on their current states.
        for (int i = 0; i < (tiles.tilesInRow * tiles.numRows); i++){
            int[] coords = tiles.indexToCoords(i);
            marsGame.batch.draw(marsGame.am.get(tileTextures[tiles.tileData[i]], Texture.class),
                    coords[0], coords[1]);
        }

        //Draws big mineral sprite and times symbol sprite
        // as part of minerals left indicator.
        int[] hudElementCoords = tiles.indexToCoords(580);
        marsGame.batch.draw(marsGame.am.get(MarsGame.RSC_BIG_MINERAL, Texture.class),
                hudElementCoords[0], hudElementCoords[1]);
        hudElementCoords = tiles.indexToCoords(581);
        marsGame.batch.draw(marsGame.am.get(MarsGame.RSC_TIMES, Texture.class),
                hudElementCoords[0], hudElementCoords[1]);

        //Draws tens place of number corresponding to remaining minerals.
        hudElementCoords = tiles.indexToCoords(582);
        marsGame.batch.draw(marsGame.am.get(numberSprites[mineralsLeft / 10], Texture.class),
                hudElementCoords[0], hudElementCoords[1]);

        //Draws ones place of number corresponding to remaining minerals.
        hudElementCoords = tiles.indexToCoords(583);
        marsGame.batch.draw(marsGame.am.get(numberSprites[mineralsLeft % 10], Texture.class),
                hudElementCoords[0], hudElementCoords[1]);

        //Draws level indicator text.
        hudElementCoords = tiles.indexToCoords(560);
        marsGame.batch.draw(marsGame.am.get(MarsGame.RSC_LVL, Texture.class),
                hudElementCoords[0], hudElementCoords[1]);
        hudElementCoords = tiles.indexToCoords(562);
        marsGame.batch.draw(marsGame.am.get(numberSprites[level], Texture.class),
                hudElementCoords[0], hudElementCoords[1]);

        //Draws available lives of rover
        hudElementCoords = tiles.indexToCoords(20);
        for (int i = 0; i < lives - 1; i++){
            marsGame.batch.draw(marsGame.am.get(MarsGame.RSC_ROVER, Texture.class),
                    hudElementCoords[0] + (i * 30), hudElementCoords[1]);
        }

        if (rover.isShielded){
            hudElementCoords = tiles.indexToCoords(552);
            marsGame.batch.draw(marsGame.am.get(MarsGame.RSC_SHIELD_TEXT, Texture.class), hudElementCoords[0], hudElementCoords[1]);
        }


        //Makes battery case.
        hudElementCoords = tiles.indexToCoords(16);
        marsGame.batch.draw(marsGame.am.get(MarsGame.RSC_BATT_CASE, Texture.class),
                hudElementCoords[0], hudElementCoords[1]);

        //Makes battery indication chunk.
        batt.setRotation(180);
        batt.setX(hudElementCoords[0] + 12);
        batt.setY(hudElementCoords[1] + 12);
        batt.setSize(batt.baseWidth * (rover.getTotalCharge() / 5040f), batt.getHeight());
        batt.draw(marsGame.batch);
        //Draws indicator for sprint if player is sprinting, since sprint uses extra battery.
        if (rover.currState == Rover.RoverState.SPRINT && (rover.getTotalCharge() > 0)){
            marsGame.batch.draw(marsGame.am.get(MarsGame.RSC_SPNT_TXT, Texture.class),
                    hudElementCoords[0], hudElementCoords[1]);
        }

        if (rover.currState == Rover.RoverState.RECHARGE){
            marsGame.batch.draw(marsGame.am.get(MarsGame.RSC_CHG_TXT, Texture.class),
                    hudElementCoords[0], hudElementCoords[1]);
        }

        //Draws telaporter pad.
        marsGame.batch.draw(marsGame.am.get(MarsGame.RSC_ON_TELEPORT, Texture.class),
                telaporterCenter[0] - 30, telaporterCenter[1] - 30);

        //Draws existing minerals (minerals whose coordinates are not 0).
        for (int[] mineralLocation : mineralLocations) {
            if (mineralLocation[0] != 0) {
                marsGame.batch.draw(marsGame.am.get(MarsGame.RSC_MINERAL, Texture.class),
                        mineralLocation[0] - (tiles.tileSize / 2f), mineralLocation[1] - (tiles.tileSize / 2f));
            }
        }

        for (int[] shieldLoc : shieldPowerupLocations){
            if (shieldLoc[0] != 0){
                marsGame.batch.draw(marsGame.am.get(MarsGame.RSC_SHIELD_PWRP, Texture.class),
                        shieldLoc[0] - (tiles.tileSize / 2), shieldLoc[1] - (tiles.tileSize / 2f));
            }
        }

        //Draws charging stations.
        for (int[] loc : chargeStationLocations){
            if (loc[0] != 0){
                marsGame.batch.draw(marsGame.am.get(MarsGame.RSC_CHG_STATION, Texture.class),
                        loc[0] - (tiles.tileSize / 2), loc[1] - (tiles.tileSize / 2));
            }
        }

        //Draws rover wherever it's at.
        if (rover.alive()) {
            rover.draw(marsGame.batch);
        }

        //Draws shield powerup based on rover's coords and current shield opacity.
        if (rover.isShielded){
            shield.setX(rover.getX());
            shield.setY(rover.getY());
            shield.setColor(1, 1, shield.opacity, shield.opacity);
            shield.draw(marsGame.batch);
        }

        //Draws blobs that exist.
        for (Blob blob : blobs) {
            if (blob != null && blob.isAlive) {
                blob.draw(marsGame.batch);

                if (debugDisplayEnabled && blob.state != Blob.BlobState.VAPEROUS){
                    if (blob.state == Blob.BlobState.CHASE){
                        int index = tiles.coordsToIndex(blob.getX(), blob.getY());
                        while(blobGuide.graph[index].distanceToSource != 0){
                            int[] drawCoords = tiles.indexToCoords(index);
                            marsGame.batch.draw(marsGame.am.get(MarsGame.RSC_DBG_TILE, Texture.class),
                                    drawCoords[0], drawCoords[1]);
                            index = blobGuide.graph[index].closerVert;
                        }
                    }else{
                        float[] blobCenter = blob.getCenter();
                        blobRadarIndicator.radius = blob.currentDistanceThreshold;
                        blobRadarIndicator.setSize(blobRadarIndicator.radius * (tiles.tileSize * 2),
                                blobRadarIndicator.radius * (tiles.tileSize * 2));
                        blobRadarIndicator.setCenter(blobCenter[0] - (tiles.tileSize / 2f),
                                blobCenter[1] - (tiles.tileSize / 2f));
                        blobRadarIndicator.draw(marsGame.batch);
                    }
                }
            }
        }

        float textX = (Gdx.graphics.getWidth() / 2f) - 195;
        float textY = (Gdx.graphics.getHeight() / 2f) - 150;
        //Displays necessary text based on state.
        switch (state) {
            case DEAD:
                marsGame.batch.draw(marsGame.am.get(MarsGame.RSC_DEATH_MSG, Texture.class), textX, textY);
                break;
            case GAME_OVER:
                marsGame.batch.draw(marsGame.am.get(MarsGame.RSC_GAME_OVER_MSG, Texture.class), textX, textY);
                break;
            case LEVEL_WON:
                marsGame.batch.draw(marsGame.am.get(MarsGame.RSC_LVL_VICTORY_MSG, Texture.class), textX, textY);
                break;
            case GAME_VICTORY:
                marsGame.batch.draw(marsGame.am.get(MarsGame.RSC_GAME_VICTORY_MSG, Texture.class), textX, textY);
                break;
            case READY:
                marsGame.batch.draw(marsGame.am.get(MarsGame.RSC_KEY_PROMPT_MSG, Texture.class), textX, textY);
                break;
            case PLAYING:
                break;
        }

        //Draws sprites and ends batch.
        hud.draw(marsGame.batch);
        marsGame.batch.end();
    }
}