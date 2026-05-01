/*
 *  Copyright 2021 frank bauer.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package de.fau.tf.lgdv.phaser;

import de.fau.tf.lgdv.CodeBlocks;
import de.fau.tf.lgdv.CodeBlocksBaseMessage;
import java.util.stream.Stream;

public class MapGame {
    public final int rows;
    public final int columns;
    private final GameEventHandler gameHandler;
    private final FigureEventHandler figureHandler;
    private final AnimatedSpriteEventHandler animHandler;
    private final EventHandler handler;
    private final java.util.ArrayList<Figure> figures = new java.util.ArrayList<>(2);
    private final java.util.ArrayList<Sprite> sprites = new java.util.ArrayList<>(2);

    public MapGame(String[] args) {
        this(args, null, null, null, null);
    }

    public MapGame(String[] args, GameEventHandler handler) {
        this(args, handler, null, null, null);
    }

    public MapGame(String[] args, GameEventHandler gameHandler, FigureEventHandler figureHandler) {
        this(args, gameHandler, figureHandler, null, null);
    }

    public MapGame(String[] args, GameEventHandler gameHandler, FigureEventHandler figureHandler,
            AnimatedSpriteEventHandler animHandler) {
        this(args, gameHandler, figureHandler, animHandler, null);
    }

    public MapGame(String[] args, GameEventHandler gameHandler, FigureEventHandler figureHandler, EventHandler handler) {
        this(args, gameHandler, figureHandler, null, handler);
    }

    public MapGame(String[] args, GameEventHandler gameHandler, AnimatedSpriteEventHandler animHandler) {
        this(args, gameHandler, null, animHandler, null);
    }

    public MapGame(String[] args, GameEventHandler gameHandler, EventHandler handler) {
        this(args, gameHandler, null, null, handler);
    }

    public MapGame(String[] args, FigureEventHandler handler) {
        this(args, null, handler, null, null);
    }

    public MapGame(String[] args, FigureEventHandler figureHandler, AnimatedSpriteEventHandler animHandler) {
        this(args, null, figureHandler, animHandler, null);
    }

    public MapGame(String[] args, FigureEventHandler figureHandler, AnimatedSpriteEventHandler animHandler,
            EventHandler handler) {
        this(args, null, figureHandler, animHandler, handler);
    }

    public MapGame(String[] args, FigureEventHandler figureHandler, EventHandler handler) {
        this(args, null, figureHandler, null, handler);
    }

    public MapGame(String[] args, AnimatedSpriteEventHandler handler) {
        this(args, null, null, handler, null);
    }

    public MapGame(String[] args, AnimatedSpriteEventHandler animHandler, EventHandler handler) {
        this(args, null, null, animHandler, handler);
    }

    public MapGame(String[] args, EventHandler handler) {
        this(args, null, null, null, handler);
    }

    public MapGame(String[] args, GameEventHandler gameHandler, FigureEventHandler figureHandler,
            AnimatedSpriteEventHandler animHandler, EventHandler handler) {
        this.gameHandler =
                gameHandler != null ? gameHandler : (this instanceof GameEventHandler ? (GameEventHandler) this : null);
        this.figureHandler = figureHandler != null ? figureHandler
                : (this instanceof FigureEventHandler ? (FigureEventHandler) this : null);
        this.animHandler = animHandler != null ? animHandler
                : (this instanceof AnimatedSpriteEventHandler ? (AnimatedSpriteEventHandler) this : null);
        this.handler = handler != null ? handler : (this instanceof EventHandler ? (EventHandler) this : null);
        if (args.length < 3) {
            throw new RuntimeException("Failed to create Game. Not enough Arguments supplied.");
        }
        if (!"isometric".equals(args[0])) {
            throw new RuntimeException("Failed to create Game. Unknown Game Type.");
        }
        this.columns = Integer.parseInt(args[1]);
        this.rows = Integer.parseInt(args[2]);

        CodeBlocks.startReceivingEvents(this::onMessage);
    }

    protected void onMessage(CodeBlocksBaseMessage request) {
        switch (request.getCommand()) {
            case "clickedTile": {
                RPCTileMessage m = request.cast();

                if (gameHandler != null) {
                    gameHandler.onClick(m.getC(), m.getR());
                }
                return;
            }
            case "onEnterTile": {
                RPCTileMessage m = request.cast();
                Figure f = getFigureFromRequest(m);
                if (f != null) {
                    f.row = m.getR();
                    f.column = m.getC();
                    f.onEnterTile(m.getC(), m.getR());

                    if (figureHandler != null) {
                        figureHandler.onEnterTile(f, m.getC(), m.getR());
                    }
                }
                return;
            }
            case "onLeaveTile": {
                RPCTileMessage m = request.cast();
                Figure f = getFigureFromRequest(m);
                if (f != null) {
                    f.onLeaveTile(m.getC(), m.getR());

                    if (figureHandler != null) {
                        figureHandler.onLeaveTile(f, m.getC(), m.getR());
                    }

                    f.row = m.getR();
                    f.column = m.getC();
                }
                return;
            }
            case "onFinishedWalking": {
                RPCIDMessage m = request.cast();
                Figure f = getFigureFromRequest(m);
                if (f != null) {
                    f.onFinishedWalking();

                    if (figureHandler != null) {
                        figureHandler.onFinishedWalking(f);
                    }
                }
                return;
            }
            case "onFinishedAnimation": {
                RPCIDMessage m = request.cast();
                Sprite f = getSpriteFromRequest(m);
                if (f instanceof AnimatedSprite) {
                    AnimatedSprite a = (AnimatedSprite) f;
                    a.onFinishedAnimation();

                    if (animHandler != null) {
                        animHandler.onFinishedAnimation(a);
                    }
                }
                return;
            }
            case "exit": {
                System.out.println("SHUTDOWN");
                CodeBlocks.exit(0);
                return;
            }
        }

        if (handler != null) {
            handler.onMessage(request);
        }
    }

    private Figure getFigureFromRequest(RPCIDMessage request) {
        if (request == null || request.getID() == null) {
            return null;
        }
        return figures.stream().filter(f -> request.getID().equals(f.uID)).findFirst().orElse(null);
    }

    private Sprite getSpriteFromRequest(RPCIDMessage request) {
        if (request == null || request.getID() == null) {
            return null;
        }
        return sprites.stream().filter(f -> request.getID().equals(f.uID)).findFirst().orElse(null);
    }

    public Figure createFigureOnTile(String type, int c, int r) {
        Figure f = new Figure(this, false, type, c, r);
        register(f);
        return f;
    }

    void register(Figure f) {
        postCreateMessage(f, "createFigureOnTile", false, 0);
        figures.add(f);
        f.sendBaseSpeed();
    }

    public Sprite createSpriteOnTile(String type, int c, int r) {
        Sprite s = new Sprite(this, false, type, c, r);
        register(s, false, 0);
        return s;
    }

    void register(Sprite s) {
        register(s, true, 0);
    }

    void register(Sprite s, boolean start, int frame) {
        postCreateMessage(s, "createSpriteOnTile", start, frame);
        sprites.add(s);
    }

    public AnimatedSprite createAnimatedSpriteOnTile(String type, int c, int r) {
        return createAnimatedSpriteOnTile(type, c, r, true, 0);
    }

    public AnimatedSprite createAnimatedSpriteOnTile(String type, int c, int r, boolean start) {
        return createAnimatedSpriteOnTile(type, c, r, start, 0);
    }

    public AnimatedSprite createAnimatedSpriteOnTile(String type, int c, int r, int frame) {
        return createAnimatedSpriteOnTile(type, c, r, false, frame);
    }

    public AnimatedSprite createAnimatedSpriteOnTile(String type, int c, int r, boolean start, int frame) {
        AnimatedSprite s = new AnimatedSprite(this, false, type, c, r);
        register(s, start, frame);
        return s;
    }

    public void remove(Figure f) {
        f.remove();
    }

    public void remove(Sprite s) {
        s.remove();
    }

    void _remove(Figure f) {
        figures.remove(f);
        postRemoveMessage(f);
    }

    void _remove(Sprite s) {
        sprites.remove(s);
        postRemoveMessage(s);
    }

    private void postCreateMessage(Sprite s, String createSpriteOnTile, boolean start, int frame) {
        RPCNewAnimationMessage reply = CodeBlocks.createJSObject();
        reply.setCommand(createSpriteOnTile);
        reply.setC(s.getColumn());
        reply.setR(s.getRow());
        reply.setID(s.uID);
        reply.setType(s.type);
        reply.setStart(start);
        reply.setFrame(frame);
        CodeBlocks.postMessage(reply);
    }

    private void postRemoveMessage(Sprite s) {
        RPCIDMessage reply = CodeBlocks.createJSObject();
        reply.setCommand("remove");
        reply.setID(s.uID);
        CodeBlocks.postMessage(reply);
    }

    @Override
    public String toString() {
        return "MapGame{" +
                "rows=" + rows +
                ", columns=" + columns +
                ", figures=" + figures.size() +
                ", sprites=" + sprites.size() +
                '}';
    }

    public Stream<Figure> getFigureStream(){
        return figures.stream();
    }

    public int getFigureCount(){
        return figures.size();
    }

    public Stream<Sprite> getSpriteStream(){
        return sprites.stream();
    }

    public int getSpriteCount(){
        return sprites.size();
    }
}
