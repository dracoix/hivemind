package hivemind;

public enum data_enum implements java.io.Serializable {

    LEFT(0), RIGHT(1), UP(2), DOWN(3),
    A(4), B(5), X(6), Y(7),
    START(8), SELECT(9), LB(10), RB(11),
    WAIT(12), DEMOCRACY(13), ANARCHY(14), SEQUENCE(15),
    RIOT(16), CHAT(17), YES(18), NO(19);

    private int type;

    private data_enum(int c) {
        type = c;
    }

    public int getCode() {
        return type;
    }

    public String getString() {
        switch (type) {
            case 0:
                return "left";
            case 1:
                return "right";
            case 2:
                return "up";
            case 3:
                return "down";
            case 4:
                return "a";
            case 5:
                return "b";
            case 6:
                return "x";
            case 7:
                return "y";
            case 8:
                return "start";
            case 9:
                return "select";
            case 10:
                return "l";
            case 11:
                return "r";
            case 12:
                return "wait";
            case 13:
                return "democracy";
            case 14:
                return "anarchy";
            case 18:
                return "yes";
            case 19:
                return "no";
        }
        return "";
    }
}
