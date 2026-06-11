package com.mojang.serialization.codecs;

import com.mojang.serialization.Codec;

import java.util.function.Function;

public final class RecordCodecBuilder {

    private RecordCodecBuilder() {
    }

    public static <T> Codec<T> create(Function<Instance<T>, Codec<T>> builder) {
        return builder.apply(new Instance<>());
    }

    public static final class Instance<T> {

        public <A> Group1<A, T> group(Codec.FieldCodec<A> field) {
            return new Group1<>();
        }
    }

    public static final class Group1<A, T> {

        public <R> Codec<R> apply(Instance<T> instance, Function<A, R> constructor) {
            return new Codec<>();
        }
    }
}
