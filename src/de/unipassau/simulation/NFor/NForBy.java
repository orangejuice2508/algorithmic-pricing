package de.unipassau.simulation.NFor;

// SOURCE CODE IS FROM https://github.com/BeUndead/NFor

final class NForBy<T extends Number & Comparable<T>> {

    private final T[] by;

    @SafeVarargs
    NForBy(final T... by) {
        this.by = by;
    }

    final T[] get() {
        return by;
    }

    final int size() {
        return by.length;
    }

}
