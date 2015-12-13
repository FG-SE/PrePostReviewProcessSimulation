package de.unihannover.se.processSimulation.serialReviewAnalysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math.special.Gamma;

public class SerialReviewAnalysis {

    private static abstract class Term {
        static String implode(List<?> list, String glue) {
            if (list.isEmpty()) {
                return "";
            }
            final StringBuilder ret = new StringBuilder();
            ret.append(list.get(0));
            for (int i = 1; i < list.size(); i++) {
                ret.append(glue).append(list.get(i));
            }
            return ret.toString();
        }

        public abstract Term simplify();
    }

    private static class Const extends Term {
        private final double val;

        public Const(double val) {
            this.val = val;
        }

        @Override
        public String toString() {
            return Double.toString(this.val);
        }

        @Override
        public Term simplify() {
            return this;
        }

    }

    private static class Var extends Term {
        private final String name;

        public Var(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public Term simplify() {
            return this;
        }

    }

    private static class Sum extends Term {
        private final List<Term> subterms;
        public Sum(Term t1, Term t2) {
            this.subterms = Arrays.asList(t1, t2);
        }
        public Sum(List<Term> subterms2) {
            this.subterms = subterms2;
        }

        @Override
        public String toString() {
            return "(" + implode(this.subterms, " + ") + ")";
        }

        @Override
        public Term simplify() {
            if (this.subterms.isEmpty()) {
                return new Const(0);
            } else if (this.subterms.size() == 1) {
                return this.subterms.get(0).simplify();
            }
            return this;
        }

    }

    private static class Product extends Term {
        private final List<Term> subterms;
        public Product(Term t1, Term t2) {
            this.subterms = Arrays.asList(t1, t2);
        }

        @Override
        public String toString() {
            return "(" + implode(this.subterms, "*") + ")";
        }

        @Override
        public Term simplify() {
            if (this.subterms.isEmpty()) {
                return new Const(1);
            } else if (this.subterms.size() == 1) {
                return this.subterms.get(0).simplify();
            }
            return this;
        }
    }

    private static class Exp extends Term {
        private final Term base;
        private final int exp;
        public Exp(Term p, int exp) {
            this.base = p;
            this.exp = exp;
        }

        @Override
        public String toString() {
            return "(" + this.base + ")^" + this.exp;
        }

        @Override
        public Term simplify() {
            if (this.exp == 0) {
                return new Const(1);
            } else if (this.exp == 1) {
                return this.base.simplify();
            }
            return this;
        }
    }

    public static void main(String[] args) {
        System.out.println(getExpectedNumberOfBugsFound(2));
        System.out.println(getExpectedNumberOfBugsFound(2).simplify());
        System.out.println(binomial(2, 1));
        System.out.println(binomial(3, 1));
        System.out.println(binomial(3, 2));

        for (int bugs = 0; bugs < 10; bugs++) {
            for (double p = 0.0; p <= 1.0; p += 0.2) {
                final double calc = calcExpectedNumberOfBugsFound(bugs, p);
                System.out.println("b=" + bugs + ",\tp=" + p + ",\tresult=" + calc + ",\t\tratio=" + (calc / bugs));
            }
            System.out.println();
        }
    }

    private static Term getExpectedNumberOfBugsFound(int numberOfBugs) {
        if (numberOfBugs == 0) {
            return new Const(0);
        }
        final List<Term> subterms = new ArrayList<>();
        final Term p = new Var("p");
        final Term pInv = new Var("pInv");
        for (int numberOfBugsFound = 1; numberOfBugsFound <= numberOfBugs; numberOfBugsFound++) {
            final Term pNobf = new Product(new Exp(p, numberOfBugsFound), new Exp(pInv, numberOfBugs - numberOfBugsFound));
            final Term exp = new Product(pNobf, new Sum(new Const(numberOfBugsFound), getExpectedNumberOfBugsFound(numberOfBugs - numberOfBugsFound)));
            subterms.add(exp);
        }
        return new Sum(subterms);
    }

    private static double calcExpectedNumberOfBugsFound(int numberOfBugs, double p) {
        if (numberOfBugs == 0) {
            return 0.0;
        }
        final double pInv = 1.0 - p;
        double ret = 0.0;
        for (int numberOfBugsFound = 1; numberOfBugsFound <= numberOfBugs; numberOfBugsFound++) {
            final double pNobf = binomial(numberOfBugs, numberOfBugsFound) * Math.pow(p, numberOfBugsFound) * Math.pow(pInv, numberOfBugs - numberOfBugsFound);
            final double exp = pNobf * (numberOfBugsFound + calcExpectedNumberOfBugsFound(numberOfBugs - numberOfBugsFound, p));
            ret += exp;
        }
        return ret;
    }

    public static double binomial(double x, double y) {
        final double res = Math.exp(Gamma.logGamma(x + 1) - (Gamma.logGamma(y + 1) + Gamma.logGamma(x - y + 1)));
        if(Double.isNaN(res)){
            return 0.0;
        }
        return res;
    }

}
