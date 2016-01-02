package de.unihannover.se.processSimulation.clusterControl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.paralleluniverse.common.util.Pair;

public abstract class DecisionTreeNode {

    private static class NumericDecisionNode extends DecisionTreeNode {

        private final String name;
        private final List<Double> splitPoints;
        private final List<DecisionTreeNode> subNodes;

        public NumericDecisionNode(String name, List<Double> splitPoints, List<DecisionTreeNode> subNodes) {
            assert splitPoints.size() == subNodes.size() - 1;
            this.name = name;
            this.splitPoints = splitPoints;
            this.subNodes = subNodes;
        }

        @Override
        public Pair<Map<String, ParamRestriction>, Double> getRestrictionsForWorstLeaf() {
            int currentWorstIdx = -1;
            Pair<Map<String, ParamRestriction>, Double> currentWorst = null;
            for (int i = 0; i < this.subNodes.size(); i++) {
                final Pair<Map<String, ParamRestriction>, Double> worstForChild = this.subNodes.get(i).getRestrictionsForWorstLeaf();
                if (currentWorst == null || currentWorst.getSecond() > worstForChild.getSecond()) {
                    currentWorst = worstForChild;
                    currentWorstIdx = i;
                }
            }

            final Map<String, ParamRestriction> newRestrictions = new HashMap<>(currentWorst.getFirst());
            final ParamRestriction childRestriction;
            if (currentWorstIdx == this.splitPoints.size()) {
                childRestriction = new ParamRestriction(this.name, this.splitPoints.get(currentWorstIdx - 1), Double.MAX_VALUE);
            } else if (currentWorstIdx == 0) {
                childRestriction = new ParamRestriction(this.name, Double.MIN_VALUE, this.splitPoints.get(0));
            } else {
                childRestriction = new ParamRestriction(this.name, this.splitPoints.get(currentWorstIdx - 1), this.splitPoints.get(currentWorstIdx));
            }
            if (newRestrictions.containsKey(this.name)) {
                newRestrictions.put(this.name, newRestrictions.get(this.name).intersect(childRestriction));
            } else {
                newRestrictions.put(this.name, childRestriction);
            }
            return new Pair<>(newRestrictions, currentWorst.getSecond());
        }

    }

    private static class NominalDecisionNode extends DecisionTreeNode {

        private final String name;
        private final List<String> values;
        private final List<DecisionTreeNode> subNodes;

        public NominalDecisionNode(String name, List<String> values, List<DecisionTreeNode> subNodes) {
            assert values.size() == subNodes.size();
            this.name = name;
            this.values = values;
            this.subNodes = subNodes;
        }

        @Override
        public Pair<Map<String, ParamRestriction>, Double> getRestrictionsForWorstLeaf() {
            int currentWorstIdx = -1;
            Pair<Map<String, ParamRestriction>, Double> currentWorst = null;
            for (int i = 0; i < this.subNodes.size(); i++) {
                final Pair<Map<String, ParamRestriction>, Double> worstForChild = this.subNodes.get(i).getRestrictionsForWorstLeaf();
                if (currentWorst == null || currentWorst.getSecond() > worstForChild.getSecond()) {
                    currentWorst = worstForChild;
                    currentWorstIdx = i;
                }
            }

            final Map<String, ParamRestriction> newRestrictions = new HashMap<>(currentWorst.getFirst());
            final ParamRestriction childRestriction = new ParamRestriction(
                            this.name, Collections.singleton(this.values.get(currentWorstIdx)));
            if (newRestrictions.containsKey(this.name)) {
                newRestrictions.put(this.name, newRestrictions.get(this.name).intersect(childRestriction));
            } else {
                newRestrictions.put(this.name, childRestriction);
            }
            return new Pair<>(newRestrictions, currentWorst.getSecond());
        }

    }

    private static class LeafNode extends DecisionTreeNode {

        private final String clazz;
        private final int pos;
        private final int neg;

        public LeafNode(String clazz, int pos, int neg) {
            this.clazz = clazz;
            this.pos = pos;
            this.neg = neg;
        }

        @Override
        public Pair<Map<String, ParamRestriction>, Double> getRestrictionsForWorstLeaf() {
            final double sum = this.pos + this.neg;
            return new Pair<Map<String, ParamRestriction>, Double>(
                            Collections.emptyMap(),
                            sum == 0 ? 1.0 : this.pos / sum);
        }

    }


    private static class StringIter {
        private final String str;
        private int pos;

        public StringIter(String treeInPrefixNotation) {
            this.str = treeInPrefixNotation;
            this.pos = 0;
        }

        public char next() {
            return this.str.charAt(this.pos++);
        }
    }

    public static DecisionTreeNode parse(String treeInPrefixNotation) {
        final StringIter s = new StringIter(treeInPrefixNotation);
        final char first = s.next();
        assert first == '[';
        return parse(s);
    }

    private static DecisionTreeNode parse(StringIter s) {
        final StringBuilder desc = new StringBuilder();
        final List<DecisionTreeNode> subNodes = new ArrayList<>();
        while (true) {
            final char ch = s.next();
            if (ch == ']') {
                return createNode(desc.toString(), subNodes);
            } else if (ch == '[') {
                subNodes.add(parse(s));
            } else {
                desc.append(ch);
            }
        }
    }

    private static DecisionTreeNode createNode(String desc, List<DecisionTreeNode> subNodes) {
        if (subNodes.isEmpty()) {
            final Pattern p = Pattern.compile("([A-Z_]+) \\(([0-9]+)\\.0(/([0-9]+)\\.0)?\\)");
            final Matcher m = p.matcher(desc);
            if (!m.matches()) {
                throw new RuntimeException("invalid description: " + desc);
            }
            final String g4 = m.group(4);
            return new LeafNode(m.group(1), Integer.parseInt(m.group(2)), g4 == null ? 0 : Integer.parseInt(g4));
        } else {
            final String[] split1 = desc.split(":");
            final String name = split1[0];
            if (split1[1].contains("<=")) {
                final String[] limits = split1[1].split(",");
                final List<Double> splitPoints = new ArrayList<>();
                for (int i = 0; i < limits.length - 1; i++) {
                    final String limit = limits[i].trim();
                    assert limit.startsWith("<= ") : "invalid limit: " + limit;
                    splitPoints.add(Double.parseDouble(limit.substring(3)));
                }
                return new NumericDecisionNode(name, splitPoints, subNodes);
            } else {
                final String[] limits = split1[1].split(",");
                final List<String> splitPoints = new ArrayList<>();
                for (final String limit2 : limits) {
                    final String limit = limit2.trim();
                    assert limit.startsWith("= ") : "invalid limit: " + limit;
                    splitPoints.add(limit.substring(2).trim());
                }
                return new NominalDecisionNode(name, splitPoints, subNodes);
            }
        }
    }

    public abstract Pair<Map<String, ParamRestriction>, Double> getRestrictionsForWorstLeaf();

}
