package com.github.haoyiyu.create_headsupdisplay.api;
public record ConfigParamDescriptor(String key, ConfigParamType type, Object defaultValue) {
    public static ConfigParamDescriptor of(String key, ConfigParamType type, Object defaultValue) {
        return new ConfigParamDescriptor(key, type, defaultValue);
    }
}
