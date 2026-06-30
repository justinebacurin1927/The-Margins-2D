package com.margins.quest;

public class QuestObjective {
    public static final int HARVEST = 0;
    public static final int TALK = 1;

    public final int type;
    public final int targetId;
    public final int required;
    public final String description;
    public int current;

    public QuestObjective(int type, int targetId, int required, String description) {
        this.type = type;
        this.targetId = targetId;
        this.required = required;
        this.description = description;
        this.current = 0;
    }

    public boolean isComplete() {
        return current >= required;
    }
}
