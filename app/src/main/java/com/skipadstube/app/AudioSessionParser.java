package com.skipadstube.app;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts playback sessions from the text of {@code dumpsys audio}. Pure and tolerant: the exact
 * layout of an AudioPlaybackConfiguration line differs between Android versions, so every field is
 * matched on its own and a missing session id is reported as -1 instead of dropping the player.
 */
final class AudioSessionParser {
    static final class Player {
        final int uid;
        final int sessionId;
        final String state;
        Player(int uid, int sessionId, String state) {
            this.uid = uid; this.sessionId = sessionId; this.state = state;
        }
    }

    private static final Pattern UID = Pattern.compile("u/pid:(\\d+)/\\d+");
    private static final Pattern SESSION = Pattern.compile("(?:sessionId|session|sessId):\\s*(\\d+)");
    private static final Pattern STATE = Pattern.compile("state:\\s*(\\w+)");

    static List<Player> parse(String dump) {
        List<Player> players = new ArrayList<>();
        if (dump == null) return players;
        for (String line : dump.split("\\r?\\n")) {
            if (!line.contains("AudioPlaybackConfiguration")) continue;
            Matcher uid = UID.matcher(line);
            if (!uid.find()) continue;
            Matcher session = SESSION.matcher(line);
            Matcher state = STATE.matcher(line);
            players.add(new Player(Integer.parseInt(uid.group(1)),
                session.find() ? Integer.parseInt(session.group(1)) : -1,
                state.find() ? state.group(1) : "unknown"));
        }
        return players;
    }

    private AudioSessionParser() {}
}
