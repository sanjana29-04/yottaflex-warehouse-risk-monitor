package com.warehouse.riskmonitor.dto;

public class GenerationRequestDto {
    private String scale = "starter"; // "sample", "starter", "full"
    private long seed = 20261022L;
    private boolean exportFiles = true;

    public GenerationRequestDto() {}

    public GenerationRequestDto(String scale, long seed, boolean exportFiles) {
        this.scale = scale;
        this.seed = seed;
        this.exportFiles = exportFiles;
    }

    public String getScale() { return scale; }
    public void setScale(String scale) { this.scale = scale; }

    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }

    public boolean isExportFiles() { return exportFiles; }
    public void setExportFiles(boolean exportFiles) { this.exportFiles = exportFiles; }
}

