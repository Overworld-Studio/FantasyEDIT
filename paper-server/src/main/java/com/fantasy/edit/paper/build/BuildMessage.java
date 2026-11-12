package com.fantasy.edit.paper.build;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

/**
 * 建造报文模型（与客户端 {@code com.fantasy.edit.network.BuildMessage} 字段一一对应）。
 */
public final class BuildMessage {

    private static final Gson GSON = new Gson();

    public String action;
    public String dimension;
    public int[] min;
    public int[] max;
    public int[] origin;
    public String block;
    public String target;
    public int radius;
    public String shape;
    public int rotation;
    public boolean mirror;
    public List<String> blocks = new ArrayList<>();

    public static BuildMessage fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return GSON.fromJson(json, BuildMessage.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    public String toJson() {
        return GSON.toJson(this);
    }
}
