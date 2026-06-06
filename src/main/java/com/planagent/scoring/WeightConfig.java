package com.planagent.scoring;

import com.planagent.model.SessionContext.Scenario;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "scoring")
public class WeightConfig {

    private Map<String, Double> family = new HashMap<>();
    private Map<String, Double> friends = new HashMap<>();

    public Map<String, Double> getFamily() { return family; }
    public void setFamily(Map<String, Double> family) { this.family = family; }

    public Map<String, Double> getFriends() { return friends; }
    public void setFriends(Map<String, Double> friends) { this.friends = friends; }

    public ScenarioWeights forScenario(Scenario scenario) {
        return scenario == Scenario.FAMILY
            ? new ScenarioWeights(family)
            : new ScenarioWeights(friends);
    }
}
