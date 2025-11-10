package com.example.crp.gateway.bff;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

class TokenCache {
    static class Entry { final String accessToken; final Instant exp; Entry(String at, Instant exp){ this.accessToken=at; this.exp=exp; } }
    private final ConcurrentHashMap<String, Entry> map = new ConcurrentHashMap<>();
    Entry get(String key){ Entry e = map.get(key); if(e==null) return null; if (e.exp.isBefore(Instant.now())) { map.remove(key, e); return null; } return e; }
    void put(String key, Entry e){ map.put(key, e); }
    void remove(String key){ map.remove(key); }
    static String keyFromRefresh(String refresh){ return Integer.toHexString(Objects.requireNonNull(refresh).hashCode()); }
}

