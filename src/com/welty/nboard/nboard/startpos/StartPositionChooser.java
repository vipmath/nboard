package com.welty.nboard.nboard.startpos;

import com.orbanova.common.feed.Feeds;
import com.welty.novello.core.Position;
import com.welty.othello.gdk.OsMove;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class StartPositionChooser {
    private static final XotChooser xotChooser = new XotChooser();

    public static StartPosition next(String startPositionType) {
        final StartPosition startPosition;
        switch (startPositionType) {
            case "Standard":
                startPosition = new StartPosition(Position.START_POSITION);
                break;
            case "Alternate":
                startPosition = new StartPosition(Position.ALTERNATE_START_POSITION);
                break;
            case "XOT":
                startPosition = xotChooser.next();
                break;
            case "F5":
                startPosition = new StartPosition(Position.START_POSITION, new OsMove("F5"));
                break;
            default:
                throw new RuntimeException("Unknown start position type : " + startPositionType);
        }
        return startPosition;
    }

    private static class XotChooser {
        private final List<String> xots;
        private int lastIndex = 0;

        private XotChooser() {
            final InputStream in = XotChooser.class.getResourceAsStream("xot-large.txt");
            xots = Feeds.ofLines(in).asList();
            Collections.shuffle(xots);
        }

        public synchronized StartPosition next() {
            lastIndex++;
            if (lastIndex >= xots.size()) {
                lastIndex = 0;
            }

            return new StartPosition(Position.START_POSITION, xots.get(lastIndex));
        }
    }
}