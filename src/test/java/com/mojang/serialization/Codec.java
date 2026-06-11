package com.mojang.serialization;

import java.util.Map;
import java.util.function.Function;

public class Codec<T> {

    public static final Codec<String> STRING = new Codec<>();

    public static <K, V> Codec<Map<K, V>> unboundedMap(Codec<K> keyCodec, Codec<V> valueCodec) {
        return new Codec<>();
    }

    public <U> Codec<U> xmap(Function<? super T, ? extends U> to, Function<? super U, ? extends T> from) {
        return new Codec<>();
    }

    public FieldCodec<T> fieldOf(String name) {
        return new FieldCodec<>();
    }

    public static final class FieldCodec<T> {

        public <R> FieldCodec<T> forGetter(Function<R, T> getter) {
            return this;
        }
    }
}
