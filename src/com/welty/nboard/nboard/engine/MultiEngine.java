package com.welty.nboard.nboard.engine;

import com.welty.othello.api.NBoardState;
import com.welty.othello.api.PingPong;
import com.welty.othello.api.StatelessEngine;
import org.jetbrains.annotations.NotNull;

/**
 * A StatelessEngine that combines multiple PingEngines into one.
 * <p/>
 * No synchronization is performed by this object; instead the caller checks the ping state.
 */
public class MultiEngine implements StatelessEngine {
    private @NotNull StatelessEngine engine;

    /**
     * @param engine initial engine
     */
    public MultiEngine(@NotNull StatelessEngine engine) {
        this.engine = engine;
    }

    /**
     */
    public synchronized void setEngine(PingPong pingPong, StatelessEngine engine) {
        if (engine != this.engine) {
            this.engine = engine;
            pingPong.next(); // invalidate all previous engine responses
        }
    }

    @Override public synchronized void terminate() {
        throw new IllegalStateException("Not implemented");
    }

    @Override public synchronized void learn(PingPong pingPong, NBoardState state) {
        engine.learn(pingPong, state);
    }

    @Override public synchronized void requestHints(PingPong pingPong, NBoardState state, int nMoves) {
        engine.requestHints(pingPong, state, nMoves);
    }

    @Override public synchronized void requestMove(PingPong pingPong, NBoardState state) {
        engine.requestMove(pingPong, state);
    }

    @NotNull @Override public synchronized String getName() {
        return engine.getName();
    }

    @NotNull @Override public synchronized String getStatus() {
        return engine.getStatus();
    }

    @Override public synchronized boolean isReady() {
        return engine.isReady();
    }
}
