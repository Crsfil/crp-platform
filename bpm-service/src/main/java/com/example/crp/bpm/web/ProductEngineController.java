package com.example.crp.bpm.web;

import com.example.crp.bpm.camunda.CamundaRestClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/bpm/product-engine")
public class ProductEngineController {
    private final CamundaRestClient camundaRestClient;

    public ProductEngineController(CamundaRestClient camundaRestClient) {
        this.camundaRestClient = camundaRestClient;
    }

    @PostMapping("/start")
    public Map<String, Object> start(@RequestBody Map<String, Object> vars) {
        return camundaRestClient.startProcess("productEngine", vars);
    }

    @PostMapping("/evaluate")
    public Map<String, Object> evaluate(@RequestBody Map<String, Object> vars) {
        return camundaRestClient.evaluateDecision("productRules", vars);
    }
}
