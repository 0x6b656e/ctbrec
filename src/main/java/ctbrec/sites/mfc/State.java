package ctbrec.sites.mfc;

import java.util.Optional;

public enum State {
    ONLINE("online"),
    CAMOFF("online - cam off"),
    RECORDING("recording"),
    INCLUDE("include"),
    EXCLUDE("exclude"),
    DELETE("delete"),
    AWAY("away"),
    PRIVATE("private"),
    GROUP_SHOW("group_show"),
    OFFLINE("offline"),
    UNKNOWN("unknown");

    String literal;
    State(String literal) {
        this.literal = literal;
    }

    public static State of(Integer vs) {
        Integer s = Optional.ofNullable(vs).orElse(Integer.MAX_VALUE);
        switch (s) {
        case 0:
            return ONLINE;
        case 90:
            return CAMOFF;
        case -4:
            return RECORDING;
        case -3:
            return INCLUDE;
        case -2:
            return EXCLUDE;
        case -1:
            return DELETE;
        case 2:
            return AWAY;
        case 12:
        case 91:
            return PRIVATE;
        case 13:
            return GROUP_SHOW;
        case 127:
            return OFFLINE;
        default:
            return UNKNOWN;
        }
    }

    @Override
    public String toString() {
        return literal;
    }
}
