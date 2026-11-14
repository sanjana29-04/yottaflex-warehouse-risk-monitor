package com.warehouse.riskmonitor.generator;

import java.util.List;

public class Mulberry32 {
    private int state;

    public Mulberry32(long seed) {
        this.state = (int) (seed & 0xFFFFFFFFL);
    }

    public double next() {
        state += 0x6D2B79F5;
        int t = state;
        t = (int) (((long) (t ^ (t >>> 15)) * (long) (t | 1)) & 0xFFFFFFFFL);
        t ^= (int) (((long) t + (long) (((t ^ (t >>> 7)) * (t | 61)) & 0xFFFFFFFFL)) & 0xFFFFFFFFL);
        long unsignedT = (t ^ (t >>> 14)) & 0xFFFFFFFFL;
        return (double) unsignedT / 4294967296.0;
    }

    public int nextInt(int min, int max) {
        if (min == max) return min;
        return (int) Math.floor(next() * (max - min + 1)) + min;
    }

    public double nextDouble(double min, double max) {
        return min + next() * (max - min);
    }

    public <T> T choice(List<T> list) {
        if (list == null || list.isEmpty()) return null;
        return list.get(nextInt(0, list.size() - 1));
    }

    public boolean chance(double probability) {
        return next() < probability;
    }

    public double nextGaussian(double mean, double stdDev) {
        double u1 = next();
        double u2 = next();
        while (u1 <= 1e-15) {
            u1 = next();
        }
        double z0 = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
        return mean + z0 * stdDev;
    }
}

