package com.margins.quest;

import java.util.List;

public class Quest {
    public final String name;
    public final String description;
    public final List<QuestObjective> objectives;
    public final int rewardItem;
    public final int rewardAmount;

    public Quest(String name, String description, List<QuestObjective> objectives, int rewardItem, int rewardAmount) {
        this.name = name;
        this.description = description;
        this.objectives = objectives;
        this.rewardItem = rewardItem;
        this.rewardAmount = rewardAmount;
    }

    public boolean isCompleted() {
        for (QuestObjective o : objectives) {
            if (!o.isComplete()) return false;
        }
        return true;
    }
}
