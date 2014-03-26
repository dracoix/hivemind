
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

}
