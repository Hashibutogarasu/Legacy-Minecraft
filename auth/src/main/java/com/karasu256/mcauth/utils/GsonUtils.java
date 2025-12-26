package com.karasu256.mcauth.utils;

import com.google.gson.Gson;
import com.karasu256.mcauth.McAuthResult;

public class GsonUtils {
    private static final Gson GSON = new Gson();

    public static McAuthResult decode(String json) {
        return GSON.fromJson(json, McAuthResult.class);
    }
    
    public static String encode(Object obj) {
        return GSON.toJson(obj);
    }
}
