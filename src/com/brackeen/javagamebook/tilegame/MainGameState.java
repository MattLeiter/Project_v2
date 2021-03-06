package com.brackeen.javagamebook.tilegame;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Iterator;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;


import com.brackeen.javagamebook.graphics.*;
import com.brackeen.javagamebook.sound.*;
import com.brackeen.javagamebook.input.*;
import com.brackeen.javagamebook.tilegame.sprites.*;
import com.brackeen.javagamebook.state.*;

public class MainGameState implements GameState {

    private static final int DRUM_TRACK = 1;

    public static final float GRAVITY = 0.002f;
    public static float HEALTH = 20;
    public static int SCORE = 0;
    public static boolean star_flag = false;
    public static long star_time = 0;
    public static long star_count = 0;

    public static boolean gas_flag = false;
    public static long gas_time = 0;
    public static long gas_count = 0;


    private SoundManager soundManager;
    private MidiPlayer midiPlayer;
    private TileGameResourceManager resourceManager;
    private int width;
    private int height;

    private Point pointCache = new Point();
    private Sound prizeSound;
    private Sound boopSound;
    private Sequence music;
    private Sequence prize;
    private TileMap map;
    private TileMapRenderer renderer;

    private String stateChange;

    private GameAction moveLeft;
    private GameAction moveRight;
    private GameAction moveUp;
    private GameAction moveDown;
    private GameAction jump;
    private GameAction exit;

    // new stuff
    private GameAction shoot;

    public MainGameState(SoundManager soundManager,
        MidiPlayer midiPlayer, int width, int height)
    {
        this.soundManager = soundManager;
        this.midiPlayer = midiPlayer;
        this.width = width;
        this.height = height;
        moveLeft = new GameAction("moveLeft");
        moveRight = new GameAction("moveRight");
        moveUp = new GameAction("moveUp");
        moveDown = new GameAction("moveDown");
        shoot = new GameAction("shoot"); // ****** new
        jump = new GameAction("jump",
            GameAction.DETECT_INITAL_PRESS_ONLY);
        exit = new GameAction("exit",
            GameAction.DETECT_INITAL_PRESS_ONLY);

        renderer = new TileMapRenderer();
        toggleDrumPlayback();
    }

    public String getName() {
        return "Main";
    }


    public String checkForStateChange() {
        return stateChange;
    }

    public void loadResources(ResourceManager resManager) {

        resourceManager = (TileGameResourceManager)resManager;

        resourceManager.loadResources();

        renderer.setBackground(
            resourceManager.loadImage("whitehouse.jpg"));

        // load first map
        map = resourceManager.loadNextMap();

        // load sounds
        prizeSound = resourceManager.loadSound("sounds/prize.wav");
        boopSound = resourceManager.loadSound("sounds/boop2.wav");
        music = resourceManager.loadSequence("sounds/music.midi");
        prize = resourceManager.loadSequence("sounds/prize.mid");
    }

    public void start(InputManager inputManager) {
        inputManager.mapToKey(moveLeft, KeyEvent.VK_LEFT);
        inputManager.mapToKey(moveRight, KeyEvent.VK_RIGHT);
        inputManager.mapToKey(moveUp, KeyEvent.VK_UP);
        inputManager.mapToKey(moveDown, KeyEvent.VK_DOWN);
        inputManager.mapToKey(jump, KeyEvent.VK_SPACE);
        inputManager.mapToKey(exit, KeyEvent.VK_ESCAPE);
        inputManager.mapToKey(shoot, KeyEvent.VK_S);

        soundManager.setPaused(false);
        midiPlayer.setPaused(false);
//        midiPlayer.play(music, true);
        toggleDrumPlayback();
    }

    public void stop() {
        soundManager.setPaused(true);
        midiPlayer.setPaused(true);
    }


    public void draw(Graphics2D g) {
        renderer.draw(g, map, width, height);
    }


    /**
        Turns on/off drum playback in the midi music (track 1).
    */
    public void toggleDrumPlayback() {
        Sequencer sequencer = midiPlayer.getSequencer();
        if (sequencer != null) {
            sequencer.setTrackMute(DRUM_TRACK,
                !sequencer.getTrackMute(DRUM_TRACK));
        }
    }
    public boolean isShooting = false;
    private int shootingCount = 0;
    private boolean coolDown = false;
    private long coolDownStart = System.currentTimeMillis();
    private long bulletStart = System.currentTimeMillis();

    private long previousShot = 0;
    private boolean wasShooting = false;
    private boolean paused = false;
    private void checkInput(long elapsedTime) {

        if (exit.isPressed()) {
            stateChange = GameStateManager.EXIT_GAME;
            return;
        }

        Player player = (Player)map.getPlayer();
        if (player.isAlive()) {
            float velocityX = 0;
            float velocityY = 0;

            if (moveLeft.isPressed()) {
                velocityX-=player.getMaxSpeed();
            }
            if (moveRight.isPressed()) {
                velocityX+=player.getMaxSpeed();
            }
            if (moveDown.isPressed()) {
                velocityY+=player.getMaxSpeed();
            }
            if (moveUp.isPressed()) {
                velocityY-=player.getMaxSpeed();
            }

            if(moveUp.isPressed() && moveRight.isPressed())
            {
                player.jump(true);
            }
//            if (jump.isPressed()) {
//                player.jump(false);
//            }

            if(shoot.isPressed()) {
                boolean canShoot = false;

                if(gas_flag)
                {
                    if (System.currentTimeMillis() - gas_time <= 1000 && gas_count <= 10) {
                        canShoot = false;
                    }
                    else
                    {
                        canShoot = true;
                        gas_flag = false;
                        gas_count = 0;
                    }
                }
                else
                {
                    canShoot = true;
                }



                if (canShoot) {
                    // delay
                    if (System.currentTimeMillis() - previousShot <= 200) {
                        wasShooting = isShooting;
                        isShooting = false;
                    }
                    // not delaying
                    else {
                        // if cooldown
                        if (coolDown) {
                            if (System.currentTimeMillis() - coolDownStart >= 1000) {
                                coolDown = false;
                                shootingCount = 0;
                            }
                        }
                        // not cooldown
                        else {
                            // shooting count at 10
                            if (shootingCount >= 10) {
                                coolDown = true;
                                coolDownStart = System.currentTimeMillis();
                                isShooting = false;
                            }
                            //otherwise not at 10
                            else {
                                shootingCount += 1;
                                isShooting = true;
                                previousShot = System.currentTimeMillis();
                            }
                        }
                    }
                } else {
                    isShooting = false;
                    shootingCount = 0;
                }
            }
            player.setVelocityX(velocityX);
            player.setVelocityY(velocityY);

        }

    }


    /**
        Gets the tile that a Sprites collides with. Only the
        Sprite's X or Y should be changed, not both. Returns null
        if no collision is detected.
    */
    public Point getTileCollision(Sprite sprite,
        float newX, float newY)
    {
        float fromX = Math.min(sprite.getX(), newX);
        float fromY = Math.min(sprite.getY(), newY);
        float toX = Math.max(sprite.getX(), newX);
        float toY = Math.max(sprite.getY(), newY);

        // get the tile locations
        int fromTileX = TileMapRenderer.pixelsToTiles(fromX);
        int fromTileY = TileMapRenderer.pixelsToTiles(fromY);
        int toTileX = TileMapRenderer.pixelsToTiles(
            toX + sprite.getWidth() - 1);
        int toTileY = TileMapRenderer.pixelsToTiles(
            toY + sprite.getHeight() - 1);

        // check each tile for a collision
        for (int x=fromTileX; x<=toTileX; x++) {
            for (int y=fromTileY; y<=toTileY; y++) {
                if (x < 0 || x >= map.getWidth() ||
                    map.getTile(x, y) != null)
                {
                    // collision found, return the tile
                    pointCache.setLocation(x, y);
                    return pointCache;
                }
            }
        }

        // no collision found
        return null;
    }


    /**
        Checks if two Sprites collide with one another. Returns
        false if the two Sprites are the same. Returns false if
        one of the Sprites is a Creature that is not alive.
    */
    public boolean isCollision(Sprite s1, Sprite s2) {
        // if the Sprites are the same, return false
        if (s1 == s2) {
            return false;
        }

        // if one of the Sprites is a dead Creature, return false
        if (s1 instanceof Creature && !((Creature)s1).isAlive()) {
            return false;
        }
        if (s2 instanceof Creature && !((Creature)s2).isAlive()) {
            return false;
        }

        // get the pixel location of the Sprites
        int s1x = Math.round(s1.getX());
        int s1y = Math.round(s1.getY());
        int s2x = Math.round(s2.getX());
        int s2y = Math.round(s2.getY());

        // check if the two sprites' boundaries intersect
        return (s1x < s2x + s2.getWidth() &&
            s2x < s1x + s1.getWidth() &&
            s1y < s2y + s2.getHeight() &&
            s2y < s1y + s1.getHeight());
    }


    /**
        Gets the Sprite that collides with the specified Sprite,
        or null if no Sprite collides with the specified Sprite.
    */
    public Sprite getSpriteCollision(Sprite sprite) {

        // run through the list of Sprites
        Iterator i = map.getSprites();
        while (i.hasNext()) {
            Sprite otherSprite = (Sprite)i.next();
            if (isCollision(sprite, otherSprite)) {
                // collision found, return the Sprite
                return otherSprite;
            }
        }

        // no collision found
        return null;
    }


    /**
        Updates Animation, position, and velocity of all Sprites
        in the current map.
    */
    public void update(long elapsedTime) {
        Creature player = (Creature)map.getPlayer();
        Sprite playerBullet = (Sprite) resourceManager.getBullet().clone();
        Sprite enemyBullets;

        // player is dead! start map over
        if (player.getState() == Creature.STATE_DEAD) {
            map = resourceManager.reloadMap();
            HEALTH = 20;
            return;
        }

        // get keyboard/mouse input
        checkInput(elapsedTime);

        // update player
        updateCreature(player, elapsedTime);
        player.update(elapsedTime);

        // update bullets
        if(isShooting)
        {
            playerBullet.setY(player.getY());

            if(player.direction == "right"){
                playerBullet.setVelocityX(1.0f);
                playerBullet.setX(player.getX());
            }else{
                playerBullet.setVelocityX(-1.0f);
                playerBullet.setX(player.getX());
            }
            playerBullet.setVelocityY(0);
            map.addSprite(playerBullet);
            //soundManager.play(shootingSound);
        }
        // update other sprites
        Iterator i = map.getSprites();
        while (i.hasNext()) {
            Sprite sprite = (Sprite)i.next();
            if (sprite instanceof Creature) {
                Creature creature = (Creature)sprite;
                if (creature.getState() == Creature.STATE_DEAD) {
                    if(creature instanceof Grub || creature instanceof Fly)
                    {
                        MainGameState.SCORE += 1;
                        MainGameState.HEALTH += 5;

                    }
                    i.remove();
                }
                else {
                    enemyBullets = updateCreature(creature, elapsedTime);
                    if(enemyBullets != null)
                    {
                        map.addEnemyBullet(enemyBullets);
                    }
                }
            }

            // normal update
            sprite.update(elapsedTime);
        }
        map.transfer_buffer();
    }

private long prevMotionLessTime = 0;
private boolean prevMotionLess = false;
    /**
        Updates the creature, applying gravity for creatures that
        aren't flying, and checks collisions.
    */
    private EnemyBullet updateCreature(Creature creature,
        long elapsedTime)
    {
        if(star_flag  &&  System.currentTimeMillis() - star_time > 3000){
            star_flag = false;
            star_count = 0;
        }
        else if(star_flag && star_count > 10){
            star_flag = false;
            star_count = 0;
        }

        // apply gravity
        if (!creature.isFlying()) {
            creature.setVelocityY(creature.getVelocityY() +
                GRAVITY * elapsedTime);
        }

        // change x
        float dx = creature.getVelocityX();
        float oldX = creature.getX();
        float newX = oldX + dx * elapsedTime;
        Point tile =
            getTileCollision(creature, newX, creature.getY());
        if (tile == null) {
            creature.setX(newX);
        }
        else {
            // line up with the tile boundary
            if (dx > 0) {
                creature.setX(
                    TileMapRenderer.tilesToPixels(tile.x) -
                    creature.getWidth());
            }
            else if (dx < 0) {
                creature.setX(
                    TileMapRenderer.tilesToPixels(tile.x + 1));
            }
            creature.collideHorizontal();

            if (creature instanceof Bullet || creature instanceof EnemyBullet) {
                try{

                    creature.setY(1000);
                    creature.setX(1000);
                    creature.setVelocityX(0);
                    creature.setVelocityY(0);

                }
                catch(Exception e){
                    System.out.println("Caught error");
                }
            }
        }
        if (creature instanceof Player) {
            checkPlayerCollision((Player)creature, false);
        }

        // change y
        float dy = creature.getVelocityY();
        float oldY = creature.getY();
        float newY = oldY + dy * elapsedTime;
        tile = getTileCollision(creature, creature.getX(), newY);
        if (tile == null) {
            creature.setY(newY);
        }
        else {
            // line up with the tile boundary
            if (dy > 0) {
                creature.setY(
                    TileMapRenderer.tilesToPixels(tile.y) -
                    creature.getHeight());
            }
            else if (dy < 0) {
                creature.setY(
                    TileMapRenderer.tilesToPixels(tile.y + 1));
            }
            creature.collideVertical();
        }
        if (creature instanceof Player) {
            boolean canKill = (oldY < creature.getY());
            checkPlayerCollision((Player)creature, canKill);
            boolean movement = false;

            if(newX != oldX)
            {
                star_count++;
                gas_count++;
                prevMotionLess = false;
                HEALTH += 0.05;
            }
            else if(newX == oldX) // || newY == oldY))
            {
                if (prevMotionLess) {
                    if (System.currentTimeMillis() - prevMotionLessTime >= 1000) {
                        HEALTH += 1;
                        prevMotionLessTime = System.currentTimeMillis();
                    } else if (System.currentTimeMillis() - prevMotionLessTime < 1000) {
                        // do nothing
                    }
                }
                else{
                    prevMotionLess = true;
                    prevMotionLessTime  = System.currentTimeMillis();
                }
            }
            if(HEALTH > 40) HEALTH = 40;
        }

        // check for bullet collision with creatures
        Sprite collisionSprite = getSpriteCollision(creature);
        if (collisionSprite instanceof Bullet) {
            if(creature.isAlive() && !(creature instanceof Player) & !(creature instanceof Bullet))
            {
                creature.setState(Creature.STATE_DYING);

            }
        }

        if(creature instanceof Grub){
            if(creature.getVelocityX() != 0f){
                if((creature.BULLETCOUNT > 0 &&
                        System.currentTimeMillis() - creature.LASTBUGSHOT > 800) ||
                        (creature.BULLETCOUNT == 0 &&
                                ((map.getPlayer().getVelocityX()==0 && System.currentTimeMillis() - creature.LASTBUGSHOT > 2000) ||
                                        (map.getPlayer().getVelocityX()!=0 && System.currentTimeMillis() - creature.LASTBUGSHOT > 500)))){
                    EnemyBullet bullet =
                            (EnemyBullet) resourceManager.getEnemyBullet().clone();
                    if(creature.direction != "left"){
                        bullet.setX(creature.getX() + 70);
                        bullet.setY(creature.getY() - 20);
                        bullet.setVelocityX(0.7f);
                    }else{
                        bullet.setX(creature.getX() - 70);
                        bullet.setY(creature.getY() - 20);
                        bullet.setVelocityX(-0.7f);
                    }
                    creature.LASTBUGSHOT = System.currentTimeMillis();
                    creature.BULLETCOUNT++;
                    return bullet;
                }
            }else{
                creature.LASTBUGSHOT = System.currentTimeMillis();
            }
        }

        return null;
    }


    /**
        Checks for Player collision with other Sprites. If
        canKill is true, collisions with Creatures will kill
        them.
    */
    public void checkPlayerCollision(Player player,
        boolean canKill)
    {
        if (!player.isAlive()) {
            return;
        }

        // check for player collision with other sprites
        Sprite collisionSprite = getSpriteCollision(player);
        if (collisionSprite instanceof PowerUp) {
            if(acquirePowerUp((PowerUp)collisionSprite))
            {
                if (player.getVelocityY() > 0) {
                    player.setVelocityY((float)-0.005);
                }
            }
        }
        else if (collisionSprite instanceof Creature && !star_flag) {
            if(collisionSprite instanceof Bullet) {
                return;
            }

            if(collisionSprite instanceof EnemyBullet)
            {

                if(HEALTH <= 5)
                {
                    HEALTH = 0;
                    SCORE = 0;
                    player.setState(Creature.STATE_DYING);
                }
                else
                {
                    HEALTH -= 5;
                    map.removeSprite(collisionSprite);
                }
                return;
            }

            HEALTH = 0;
            SCORE = 0;
            player.setState(Creature.STATE_DYING);
        }

    }


    /**
        Gives the player the speicifed power up and removes it
        from the map.
    */
    public boolean acquirePowerUp(PowerUp powerUp) {
        // remove it from the map
        //map.removeSprite(powerUp);

        if (powerUp instanceof PowerUp.Star) {
            // do something here, like give the player points
            soundManager.play(prizeSound);
            star_flag = true;
            star_time = System.currentTimeMillis();
            star_count = 0;
        }
        else if (powerUp instanceof PowerUp.Music) {
            // change the music
            soundManager.play(prizeSound);
            toggleDrumPlayback();
        }
        else if (powerUp instanceof PowerUp.Goal) {
            // advance to next map
//            soundManager.play(prizeSound);
            midiPlayer.play(prize, true);
//            soundManager.play(prizeSound, new EchoFilter(2000, .7f), false);
//            map = resourceManager.loadNextMap();
            HEALTH += 5;
        }
        else if(powerUp instanceof PowerUp.Gas) {
                gas_time = System.currentTimeMillis();
            gas_flag = true;
            gas_count = 0;
             return true;
        }
        else if(powerUp instanceof PowerUp.Explode) {
            if(((PowerUp.Explode) powerUp).EXPLOSIVE == true)
            {
                HEALTH -= 10;
                ((PowerUp.Explode) powerUp).EXPLOSIVE = false;
            }
            return true;
        }


        map.removeSprite(powerUp);
        return false;
    }

}