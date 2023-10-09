package io.github.cjustinn.specialisedworkforce2.enums;

public enum WorkforceRewardType {
    ECONOMIC(1),
    EXPERIENCE(2),
    PROFESSION_EXPERIENCE(3);

    public final int id;
    private WorkforceRewardType(int value) {
        this.id = value;
    }
}
