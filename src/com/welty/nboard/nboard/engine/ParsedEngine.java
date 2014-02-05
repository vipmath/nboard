package com.welty.nboard.nboard.engine;

import com.welty.othello.api.ApiEngine;
import com.welty.othello.api.NBoardEngine;
import com.welty.othello.c.CReader;
import com.welty.othello.core.CMove;
import com.welty.othello.engine.ExternalNBoardEngine;
import com.welty.othello.gdk.COsGame;
import com.welty.othello.gdk.COsMoveListItem;
import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.IOException;

/**
 * Class that controls communication with an engine.
 */
public class ParsedEngine extends ApiEngine implements NBoardEngine.Listener {
    private int m_pong;
    private String name = "ntest";
    private final @NotNull NBoardEngine engine;

    /**
     * Construct with the default NBoardEngine
     *
     * @throws IOException
     */
    public ParsedEngine() throws IOException {
        this(new ExternalNBoardEngine());
    }

    /**
     * @param engine command-line engine
     */
    ParsedEngine(@NotNull final NBoardEngine engine) {
        this.engine = engine;
        this.engine.sendCommand("nboard 1");

        engine.addListener(this);
    }


    private void ping(int ping) {
        engine.sendCommand("ping " + ping);
    }

    /**
     * Terminate the thread that sends messages to the window.
     * <p/>
     * This is called when the OS copy of the window is about to be destroyed. Sending
     * additional messages to the window could result in crashes.
     */
    @Override public void terminate() {
        engine.sendCommand("quit");
    }

    public String getName() {
        return name;
    }

    private void SetName(String name) {
        this.name = name;
    }

    /**
     * Set the NBoard protocol's current game.
     *
     * @param game game to set.
     */
    @Override public void setGame(int ping, COsGame game) {
        ping(ping);
        engine.sendCommand("set game " + game);
    }

    /**
     * Tell the Engine to learn the current game.
     */
    @Override public void learn() {
        engine.sendCommand("learn");
    }

    /**
     * Set the engine's contempt factor (scoring of proven draws).
     *
     * @param contempt contempt, in centidisks.
     */
    @Override public void setContempt(int ping, int contempt) {
        ping(ping);
        engine.sendCommand("set contempt " + contempt);
    }


    @Override public void setMaxDepth(int ping, int maxDepth) {
        ping(ping);
        engine.sendCommand("set depth " + maxDepth);
    }

    /**
     * Append a move to the NBoard protocol's current game.
     *
     * @param mli the move to append to the protocol's current game.
     */
    @Override public void sendMove(int ping, COsMoveListItem mli) {
        ping(ping);
        engine.sendCommand("move " + mli);
    }

    /**
     * Request hints (evaluation of the top n moves) from the engine, for the current board
     *
     * @param nMoves number of moves to evaluate
     */
    @Override public void requestHints(int nMoves) {
        engine.sendCommand("hint " + nMoves);
    }

    /**
     * Request a valid move from the engine, for the current board.
     * <p/>
     * Unlike {@link #requestHints(int)}, the engine does not have to return an evaluation;
     * if it has only one legal move it may choose to return that move immediately without searching.
     */
    @Override public void requestMove() {
        engine.sendCommand("go");
    }

    /**
     * Parse a command received from the engine and notify listeners.
     * <p/>
     * Listeners are always notified regardless of whether ping is up to date.
     */
    @Override public void onMessageReceived(String message) {
        final CReader is = new CReader(message);
        String sCommand = is.readString();
        is.ignoreWhitespace();

        if (sCommand.equals("pong")) {
            int n;
            try {
                n = is.readInt();
            } catch (EOFException e) {
                throw new IllegalStateException("Engine response is garbage : " + message);
            }
            m_pong = n;
            firePong(m_pong);
        } else if (sCommand.equals("status")) {
            // the engine is busy and is telling the user why
            fireStatus(m_pong, is.readLine());
        } else if (sCommand.equals("set")) {
            String variable = is.readString();
            if (variable.equals("myname")) {
                String sName = is.readString();
                SetName(sName);
            }
        }
        // These commands are only used if the computer is up-to-date
        else if (true) {
            switch (sCommand) {
                case "===":
                    fireStatus(m_pong, "");
                    // now update the move list

                    // Edax produces the mli with spaces between components rather than slashes.
                    // Translate to normal form if there are spaces.
                    final String mliText = is.readLine().trim().replaceAll("\\s+", "/");
                    final COsMoveListItem mli = new COsMoveListItem(mliText);

                    fireEngineMove(m_pong, mli);
                    break;
                // computer giving hints
                // search [pv] [eval] 0 [depth] [freeform text]
                // book [pv] [eval] [# games] [depth] [freeform text]
                case "book":
                case "search":
                    try {
                        final boolean isBook = sCommand.equals("book");

                        final String pv = is.readString();
                        final CMove move;
                        try {
                            move = new CMove(pv.substring(0, 2));
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException("Can't create move from first two characters of pv (" + pv + ")");
                        }
                        final String eval = is.readString();
                        final int nGames = is.readInt();
                        final String depth = is.readString();
                        final String freeformText = is.readLine();
                        fireHint(m_pong, isBook, pv, move, eval, nGames, depth, freeformText);
                    } catch (EOFException | IllegalArgumentException e) {
                        fireParseError(m_pong, message, e.toString());
                    }
                    break;
                case "learn":
                    fireStatus(m_pong, "");
                    break;
            }
        }
    }

    @Override public void onEngineTerminated() {
        fireStatus(m_pong, "The engine (" + name + ") has terminated.");
    }
}
