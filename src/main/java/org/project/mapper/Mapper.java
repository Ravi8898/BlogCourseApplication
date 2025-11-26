package org.project.mapper;

@FunctionalInterface
public interface Mapper<S, T> {
    T map(S source);
}
