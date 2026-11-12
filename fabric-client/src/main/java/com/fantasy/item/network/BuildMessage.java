package com.fantasy.item.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

/**
 * 建造报文模型。客户端与服务端共用同一份 JSON 结构。
 */
public final class BuildMessage {

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

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

    public String toJson() {
        return GSON.toJson(this);
    }

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
}
