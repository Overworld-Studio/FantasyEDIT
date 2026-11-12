package com.fantasy.edit.paper.net;

import com.fantasy.edit.paper.behavior.BehaviorRule;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

/**
 * 与 Fabric 端 {@code com.fantasy.edit.network.BehaviorMessage} 完全同构的报文。
 */
public final class BehaviorMessage {

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .disableHtmlEscaping()
            .create();

    public String key = "";
    public List<BehaviorRule> behaviors = new ArrayList<>();

    public static BehaviorMessage fromJson(String json) {
        try {
            return GSON.fromJson(json, BehaviorMessage.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    public String toJson() {
        return GSON.toJson(this);
    }
}
