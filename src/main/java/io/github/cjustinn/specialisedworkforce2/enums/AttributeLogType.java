package io.github.cjustinn.specialisedworkforce2.enums;

public enum AttributeLogType {
    FURNACE(1),
    BREWING_STAND(2);

    public final int value;
    private AttributeLogType(int value) {
        this.value = value;
    }
}
