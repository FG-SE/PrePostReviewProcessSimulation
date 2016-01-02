package de.unihannover.se.processSimulation.clusterControl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import desmoj.core.dist.MersenneTwisterRandomGenerator;

final class ParamRestriction {
    private final String paramName;
    private final List<Double> from;
    private final List<Double> width;
    private final List<String> values;

    private ParamRestriction(String paramName, boolean nominal) {
        this.paramName = paramName;
        if (nominal) {
            this.from = null;
            this.width = null;
            this.values = new ArrayList<>();
        } else {
            this.from = new ArrayList<>();
            this.width = new ArrayList<>();
            this.values = null;
        }
    }

    public ParamRestriction(String paramName, Set<String> values) {
        this(paramName, true);
        this.values.addAll(values);
    }

    public ParamRestriction(String paramName, double from, double to) {
        this(paramName, false);
        assert to > from;
        this.from.add(from);
        this.width.add(to - from);
    }

    public String sample(MersenneTwisterRandomGenerator rng) {
        if (this.values != null) {
            final int idx = (int) (rng.nextDouble() * this.values.size());
            return this.values.get(idx);
        } else {
            double totalWidth = 0.0;
            for (int i = 0; i < this.width.size(); i++) {
                totalWidth += this.width.get(i);
            }
            double scaledRand = totalWidth * rng.nextDouble();
            for (int i = 0; i < this.width.size(); i++) {
                if (scaledRand < this.width.get(i)) {
                    return String.format(Locale.ENGLISH, "%f", this.from.get(i) + scaledRand);
                }
                scaledRand -= this.width.get(i);
            }
            throw new AssertionError("impossible choice " + this.from + ", " + this.width);
        }
    }

    public String getName() {
        return this.paramName;
    }

    public ParamRestriction intersect(ParamRestriction furtherRestriction) {
        assert this.paramName.equals(furtherRestriction.paramName);
        assert (this.values == null) == (furtherRestriction.values == null);
        if (this.values == null) {
            final ParamRestriction intersection = new ParamRestriction(this.paramName, false);
            int i = 0;
            int j = 0;
            while (i < this.from.size() && j < furtherRestriction.from.size()) {
                final double fromI = this.from.get(i);
                final double fromJ = furtherRestriction.from.get(j);
                final double toI = fromI + this.width.get(i);
                final double toJ = fromJ + furtherRestriction.width.get(j);
                if (toI < fromJ) {
                    i++;
                } else if (toJ < fromI) {
                    j++;
                } else if (toI < toJ) {
                    final double fromMax = Math.max(fromI, fromJ);
                    intersection.from.add(fromMax);
                    intersection.width.add(toI - fromMax);
                    i++;
                } else {
                    final double fromMax = Math.max(fromI, fromJ);
                    intersection.from.add(fromMax);
                    intersection.width.add(toJ - fromMax);
                    j++;
                }
            }
            return intersection;
        } else {
            final Set<String> intersection = new LinkedHashSet<>(this.values);
            intersection.retainAll(furtherRestriction.values);
            return new ParamRestriction(this.paramName, intersection);
        }
    }

}