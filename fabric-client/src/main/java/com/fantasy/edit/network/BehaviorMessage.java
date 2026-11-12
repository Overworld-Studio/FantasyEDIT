package com.fantasy.edit.network;

import com.fantasy.edit.behavior.BehaviorRule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端 ⇄ 服务端的行为同步报文：只携带 key 与规则数组，体积极小，
 * 可直接放进 Bukkit 插件消息通道（上限 32KB）。
 */
public class BehaviorMessage {

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .disableHtmlEscaping()
            .create();

    public String key = "";
    public List<BehaviorRule> behaviors = new ArrayList<>();

    public static BehaviorMessage of(String key, List<BehaviorRule> rules) {
        BehaviorMessage message = new BehaviorMessage();
        message.key = key;
        message.behaviors = new ArrayList<>(rules);
        return message;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static BehaviorMessage fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return GSON.fromJson(json, BehaviorMessage.class);
    }
}
