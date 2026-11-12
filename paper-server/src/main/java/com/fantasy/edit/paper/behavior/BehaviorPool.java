package com.fantasy.edit.paper.behavior;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 服务端动态行为池：key 为 {@code namespace:path}，可被 GUI 推送实时替换。
 */
public final class BehaviorPool {

    private final Map<String, List<BehaviorRule>> rules = new ConcurrentHashMap<>();

    public void replace(String key, List<BehaviorRule> incoming) {
        if (key == null || key.isBlank()) {
            return;
        }
        if (incoming == null || incoming.isEmpty()) {
            this.rules.remove(key);
            return;
        }
        List<BehaviorRule> clean = new CopyOnWriteArrayList<>();
        for (BehaviorRule rule : incoming) {
            if (rule != null && rule.isValid() && clean.size() < BehaviorRule.MAX_RULES) {
                clean.add(rule);
            }
        }
        if (clean.isEmpty()) {
            this.rules.remove(key);
        } else {
            this.rules.put(key, clean);
        }
    }

    public List<BehaviorRule> get(String key) {
        return key == null ? null : this.rules.get(key);
    }

    public boolean has(String key) {
        return key != null && this.rules.containsKey(key);
    }

    public int size() {
        return this.rules.size();
    }

    public void clear() {
        this.rules.clear();
    }

    public Map<String, List<BehaviorRule>> snapshot() {
        return Map.copyOf(this.rules);
    }
}
