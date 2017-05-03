package jcog.net.attn;

import com.fasterxml.jackson.core.JsonProcessingException;
import jcog.Util;
import jcog.pri.PLink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by me on 5/2/17.
 */
public class HashMapTagSet implements TagSet, Serializable {

    public static final HashMapTagSet EMPTY = new HashMapTagSet() {

        @Override
        public float pri(String tag) {
            return 0f;
        }

        @Override
        public boolean pri(String tag, float pri) {
            return false;
        }
    };


    public String id;
    public ConcurrentHashMap<String, Float> data;

    protected HashMapTagSet() {
        this.id = "";
        this.data = new ConcurrentHashMap<>();
    }

    public HashMapTagSet(@NotNull String id) {
        this.id = id;
        this.data = new ConcurrentHashMap<>();
    }

    @Override
    public String toString() {
        return id + "=" + data.toString();
    }

    @NotNull
    @Override public String id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HashMapTagSet)) return false;
        HashMapTagSet h = (HashMapTagSet) o;
        return id.equals(h.id) && data.equals(h.data);
    }

    @Override
    public int hashCode() {
        return Util.hashCombine(super.hashCode(), id.hashCode());
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public boolean pri(String tag, float pri) {
        pri = Util.unitize(pri);
        if (Util.equals(pri, 0f, PLink.EPSILON_DEFAULT))
            return data.remove(tag)!=null;
        else {
            Float existing = data.put(tag, pri);
            return existing == null || !Util.equals(existing, PLink.EPSILON_DEFAULT);
        }
    }

    @Override
    public float pri(String tag) {
        Float f = data.get(tag);
        if (f == null)
            return 0;
        return f;
    }

    public byte[] toBytes() {
        try {
            return Util.toBytes(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public static HashMapTagSet fromBytes(byte[] b) {
        try {
            return Util.fromBytes(b, HashMapTagSet.class);
        } catch (IOException e) {
            return null;
        }
    }
}
