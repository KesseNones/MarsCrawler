package wsuv.bounce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;

public class Rover extends Sprite {
    enum RoverState  {STANDARD, SPRINT, RECHARGE}
    RoverState currState;
    float moveSpeed;
    private boolean isAlive;
    private boolean isCarryingMineral;
    public boolean isShielded;
    final Texture[] roverTextures;
    private float respawnX;
    private float respawnY;

    private int totalCharge;
    private int reactorChargeRate;
    private int reactorDrainRate;

    TileMap tiles;

    public Rover(MarsGame game, float startX, float startY, TileMap map){
        super(game.am.get("rover.png", Texture.class));
        isAlive = true;
        isCarryingMineral = false;
        isShielded = false;
        roverTextures = new Texture[2];
        roverTextures[0] = new Texture(game.RSC_ROVER);
        roverTextures[1] = new Texture(game.RSC_ROVER_W_MINERAL);

        respawnX = startX + (getWidth() / 2f);
        respawnY = startY + (getHeight() / 2f);

        currState = RoverState.STANDARD;

        //Establishes rover location.
        setCenter(respawnX, respawnY);

        moveSpeed = 2;
        tiles = map;

        totalCharge = 5040;
        reactorChargeRate = 15;
        reactorDrainRate = 12;

    }

    public void instantCharge(){
        totalCharge = 5040;
        currState = RoverState.STANDARD;
    }

    public int getTotalCharge(){
        return totalCharge;
    }

    //Updates the rover's battery based on its state.
    public void updateBattery(boolean hasMoved){
        //Determines what drain rate of the battery
        // should currently be based on rover's state.
        switch (currState){
            case STANDARD:
                reactorDrainRate = 17;
                break;
            case SPRINT:
                reactorDrainRate = 36;
                break;
            case RECHARGE:
                reactorDrainRate = 0;
                break;
            default:
                //SHOULD NEVER GET HERE
                reactorDrainRate = 9999;
        }

        if (!hasMoved){reactorDrainRate = 0;}

        int netChargeChange = reactorChargeRate - reactorDrainRate;
        totalCharge += netChargeChange;

        //Makes sure battery stops at 100 percent full.
        if (totalCharge > 5040){
            totalCharge = 5040;
        }

        //Enters recharge mode if rover is out of juice.
        if (totalCharge <= 0){
            totalCharge = 0;
            currState = RoverState.RECHARGE;
        }

        //Boots back up in standard if rover is above 10 percent battery.
        if (currState == RoverState.RECHARGE && totalCharge >= 1260){
            currState = RoverState.STANDARD;
        }

        //Enters emergency speed mode.
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)){
            currState = RoverState.SPRINT;
        }

        //Enters emergency speed mode.
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)){
            currState = RoverState.STANDARD;
        }

    }

    public boolean alive(){
        return isAlive;
    }

    public boolean carriesMineral(){
        return isCarryingMineral;
    }

    public void murder(){
        isAlive = false;
    }

    public void respawn(){
        isAlive = true;
        setCenter(respawnX, respawnY);

    }

    public void pickUpMineral(){
        isCarryingMineral = true;
        setTexture(roverTextures[1]);
    }

    public void dropMineral(){
        isCarryingMineral = false;
        setTexture(roverTextures[0]);
    }

    public float[] getCenter(){
        float[] coords = new float[2];
        coords[0] = getX() + getWidth();
        coords[1] = getY() + getHeight();
        return coords;
    }

    public void moveRover(){
        //Only moves if rover isn't charging.
        if (currState != RoverState.RECHARGE){
            //Sets up move speed based on rover state.
            if (currState == RoverState.STANDARD){
                moveSpeed = 2;
            }else{
                moveSpeed = 6;
            }

            float[] center = getCenter();
            float upperBound = 15;

            int row = (int)center[1] / tiles.tileSize;
            int col = (int)center[0] / tiles.tileSize;

            //Handles upwards movement case.
            if (Gdx.input.isKeyPressed(Input.Keys.W)){
                updateBattery(true);
                int currIndex = tiles.coordsToIndex(center[0], center[1]);
                int[] idealCoords = tiles.indexToCoords(currIndex);
                float xDelta = center[0] - idealCoords[0];

                //If player's center is past the tile center,
                // the player can make their turn.
                if (xDelta >= 0 && xDelta <= upperBound){
                    //Always happens.
                    setRotation(90);
                    setX(idealCoords[0] - getWidth());

                    //Moves rover upwards if tile is move-able ground.
                    int moveToIndex = tiles.coordsToIndex(idealCoords[0], idealCoords[1]);
                    if (tiles.tileData[moveToIndex - 1] == 1){
                        setY(getY() + moveSpeed);
                    }
                }

            }

            //Handles downwards motion case.
            else if (Gdx.input.isKeyPressed(Input.Keys.S)){
                updateBattery(true);
                int[] idealCoords = tiles.indexToCoords(tiles.coordsToIndex(center[0], center[1]));
                float xDelta = center[0] - idealCoords[0];

                if (xDelta >= 0 && xDelta <= upperBound){
                    setRotation(270);
                    setX(idealCoords[0] - getWidth());

                    //Moves rover downwards if tile is move-able ground.
                    int moveToIndex = tiles.coordsToIndex(idealCoords[0] - tiles.tileSize, idealCoords[1] - tiles.tileSize);
                    if (tiles.tileData[moveToIndex] == 1){
                        setY(getY() - moveSpeed);
                    }
                }

            }

            //Handles left motion case.
            else if (Gdx.input.isKeyPressed(Input.Keys.A)){
                updateBattery(true);
                int[] idealCoords = tiles.indexToCoords(tiles.coordsToIndex(center[0], center[1]));
                float yDelta = center[1] - idealCoords[1];

                if (yDelta >= 0 && yDelta <= upperBound){
                    setRotation(180);
                    setY(idealCoords[1] - getHeight());

                    //Moves rover to the left if next tile isn't a wall.
                    int moveIndex = tiles.coordsToIndex(idealCoords[0] - tiles.tileSize, idealCoords[1] - tiles.tileSize);
                    if (tiles.tileData[moveIndex] == 1) {
                        setX(getX() - moveSpeed);
                    }
                }
            }

            //Handles right motion case.
            else if (Gdx.input.isKeyPressed(Input.Keys.D)){
                updateBattery(true);
                int[] idealCoords = tiles.indexToCoords(tiles.coordsToIndex(center[0], center[1]));
                float yDelta = center[1] - idealCoords[1];

                if (yDelta >= 0 && yDelta <= upperBound){
                    setRotation(0);
                    setY(idealCoords[1] - getHeight());

                    //Moves rover to the right if possible.
                    int moveIndex = tiles.coordsToIndex(idealCoords[0], idealCoords[1] - tiles.tileSize);
                    //tiles.tileData[moveIndex] = 0;
                    if (tiles.tileData[moveIndex] == 1){
                        setX(getX()  + moveSpeed);
                    }
                }

            }else{
                updateBattery(false);
            }
        }else{
            updateBattery(false);
        }
    }
}
