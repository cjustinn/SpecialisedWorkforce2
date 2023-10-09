package io.github.cjustinn.specialisedworkforce2.models;

import io.github.cjustinn.specialisedworkforce2.enums.AttributeLogType;

public class WorkforceInteractionLogValue {
    public final String uuid;
    public final AttributeLogType type;

    public WorkforceInteractionLogValue(String uuid, AttributeLogType type) {
        this.uuid = uuid;
        this.type = type;
    }
}
