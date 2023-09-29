package io.github.cjustinn.specialisedworkforce2.models.SQL;

public class MySQLProperty {
    public final String type;
    public final Object value;
    public final int index;

    public MySQLProperty(String type, Object value, int index) {
        this.type = type;
        this.value = value;
        this.index = index;
    }
}
