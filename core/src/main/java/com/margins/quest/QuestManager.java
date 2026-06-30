package com.margins.quest;

import com.margins.item.Inventory;
import com.margins.item.Item;

import java.util.ArrayList;
import java.util.List;

public class QuestManager {
    private final List<Quest> active = new ArrayList<>();
    private final List<Quest> completed = new ArrayList<>();

    public boolean start(Quest q, Inventory inv) {
        if (isActive(q) || isCompleted(q)) return false;
        for (QuestObjective o : q.objectives) {
            if (o.type == QuestObjective.HARVEST) {
                o.current = Math.min(inv.count(o.targetId), o.required);
            }
        }
        active.add(q);
        return true;
    }

    public void harvest(int itemType, int amount, Inventory inv) {
        List<Quest> done = new ArrayList<>();
        for (Quest q : active) {
            for (QuestObjective o : q.objectives) {
                if (o.type == QuestObjective.HARVEST && o.targetId == itemType) {
                    int before = o.current;
                    o.current = Math.min(o.current + amount, o.required);
                    if (o.isComplete() && before < o.required) {
                        done.add(q);
                    }
                }
            }
        }
        for (Quest q : done) {
            checkCompletion(q, inv);
        }
    }

    public void talk(String npcName, Inventory inv) {
        List<Quest> done = new ArrayList<>();
        for (Quest q : active) {
            for (QuestObjective o : q.objectives) {
                if (o.type == QuestObjective.TALK && o.targetId == npcName.hashCode()) {
                    if (!o.isComplete()) {
                        o.current = o.required;
                        done.add(q);
                    }
                }
            }
        }
        for (Quest q : done) {
            checkCompletion(q, inv);
        }
    }

    private void checkCompletion(Quest q, Inventory inv) {
        if (q.isCompleted()) {
            active.remove(q);
            completed.add(q);
            if (q.rewardItem >= 0 && q.rewardAmount > 0) {
                inv.add(q.rewardItem, q.rewardAmount);
            }
        }
    }

    public void clear() {
        active.clear();
        completed.clear();
    }

    public void restoreActive(Quest q, int[] objectiveProgress) {
        if (q == null) return;
        if (active.contains(q) || completed.contains(q)) return;
        for (int i = 0; i < q.objectives.size() && i < objectiveProgress.length; i++) {
            q.objectives.get(i).current = objectiveProgress[i];
        }
        active.add(q);
    }

    public void restoreCompleted(Quest q) {
        if (q == null || completed.contains(q)) return;
        active.remove(q);
        completed.add(q);
    }

    public Quest findByName(String name) {
        for (Quest q : active) if (q.name.equals(name)) return q;
        for (Quest q : completed) if (q.name.equals(name)) return q;
        return null;
    }

    public boolean isActive(Quest q) { return active.contains(q); }
    public boolean isCompleted(Quest q) { return completed.contains(q); }
    public List<Quest> getActive() { return active; }
    public List<Quest> getCompleted() { return completed; }
}
