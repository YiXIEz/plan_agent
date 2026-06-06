package com.planagent.scoring;

import java.util.HashMap;
import java.util.Map;

public class ScenarioWeights {
    private final Map<String, Double> weights;

    public ScenarioWeights(Map<String, Double> weights) {
        this.weights = new HashMap<>(weights);
    }

    public double get(String key) {
        return weights.getOrDefault(key, 0.0);
    }

    public boolean containsKey(String key) {
        return weights.containsKey(key);
    }
}
