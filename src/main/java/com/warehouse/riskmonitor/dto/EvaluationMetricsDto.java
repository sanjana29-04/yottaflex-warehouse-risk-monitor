package com.warehouse.riskmonitor.dto;

import com.warehouse.riskmonitor.entity.GroundTruthAnomaly;
import java.util.ArrayList;
import java.util.List;

public class EvaluationMetricsDto {
    private int truePositives;
    private int falsePositives;
    private int falseNegatives;
    private double precision;
    private double recall;
    private double f1Score;
    private double meanDetectionLatencyMinutes;
    private List<GroundTruthAnomaly> anomalies = new ArrayList<>();

    public EvaluationMetricsDto() {}

    public int getTruePositives() { return truePositives; }
    public void setTruePositives(int truePositives) { this.truePositives = truePositives; }

    public int getFalsePositives() { return falsePositives; }
    public void setFalsePositives(int falsePositives) { this.falsePositives = falsePositives; }

    public int getFalseNegatives() { return falseNegatives; }
    public void setFalseNegatives(int falseNegatives) { this.falseNegatives = falseNegatives; }

    public double getPrecision() { return precision; }
    public void setPrecision(double precision) { this.precision = precision; }

    public double getRecall() { return recall; }
    public void setRecall(double recall) { this.recall = recall; }

    public double getF1Score() { return f1Score; }
    public void setF1Score(double f1Score) { this.f1Score = f1Score; }

    public double getMeanDetectionLatencyMinutes() { return meanDetectionLatencyMinutes; }
    public void setMeanDetectionLatencyMinutes(double meanDetectionLatencyMinutes) { this.meanDetectionLatencyMinutes = meanDetectionLatencyMinutes; }

    public List<GroundTruthAnomaly> getAnomalies() { return anomalies; }
    public void setAnomalies(List<GroundTruthAnomaly> anomalies) { this.anomalies = anomalies; }
}

