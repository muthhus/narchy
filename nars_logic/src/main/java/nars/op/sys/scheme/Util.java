package nars.op.sys.scheme;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum Util {
    ;

    public static <T> T first(@NotNull List<T> list) {
        return list.get(0);
    }

    public static <T> List<T> rest(@NotNull List<T> list) {
        return list.subList(1, list.size());
    }
}
