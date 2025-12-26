package com.karasu256.mcauth.processor;

/**
 * Progress information for processor chain execution.
 */
public record ProcessorProgress(String processorName, int current, int total) {
    
    public float getPercentage() {
        return total > 0 ? (float) current / total : 0f;
    }
}
