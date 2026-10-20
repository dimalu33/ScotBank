package uk.co.asepstrath.bank.kyc;

public class MinMax {
    private long min;
    private long max;

    public MinMax(long min, long max) {
        this.min = min;
        this.max = max;
    }

    public long getMin() {
        return min;
    }

    public long getMax() {
        return max;
    }
}
