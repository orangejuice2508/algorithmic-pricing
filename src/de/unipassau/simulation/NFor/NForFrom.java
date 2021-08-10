package de.unipassau.simulation.NFor;

// SOURCE CODE IS FROM https://github.com/BeUndead/NFor

final class NForFrom<T extends Number & Comparable<T>> {

    private final T[] from;

    @SafeVarargs
    NForFrom(final T... from) {
        this.from = from;
    }

    final T[] get() {
        return from;
    }

    final int size() {
        return from.length;
    }

}
