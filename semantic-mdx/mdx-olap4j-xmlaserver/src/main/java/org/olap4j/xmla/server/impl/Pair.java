package org.olap4j.xmla.server.impl;

public class Pair<L, R> {

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    private L left;

    private R right;

    public static boolean isPass(Pair<Boolean, ?> pair) {
        return pair != null && pair.getLeft();
    }

    public static boolean isFail(Pair<Boolean, ?> pair) {
        return pair == null || !pair.getLeft();
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }

    public void setLeft(L left) {
        this.left = left;
    }

    public void setRight(R right) {
        this.right = right;
    }
}
