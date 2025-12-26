package com.example.crp.bpm.camunda;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CamundaRestClient {
    private final WebClient camundaClient;

    public CamundaRestClient(WebClient camundaClient) {
        this.camundaClient = camundaClient;
    }

    public Map<String, Object> startProcess(String key, Map<String, Object> variables) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("variables", toCamundaVariables(variables));
        return camundaClient.post()
                .uri("/process-definition/key/{key}/start", key)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    public Map<String, Object> evaluateDecision(String key, Map<String, Object> variables) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("variables", toCamundaVariables(variables));
        return camundaClient.post()
                .uri("/decision-definition/key/{key}/evaluate", key)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    public Map<String, Object> deployResources(String deploymentName, List<Resource> resources) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("deployment-name", deploymentName);
        builder.part("enable-duplicate-filtering", "true");
        builder.part("deploy-changed-only", "true");
        for (Resource resource : resources) {
            builder.part("data", resource)
                    .filename(resource.getFilename());
        }
        MultiValueMap<String, ?> body = builder.build();
        return camundaClient.post()
                .uri("/deployment/create")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    private Map<String, Object> toCamundaVariables(Map<String, Object> variables) {
        Map<String, Object> out = new HashMap<>();
        if (variables == null) {
            return out;
        }
        variables.forEach((key, value) -> out.put(key, Map.of("value", value)));
        return out;
    }
}
