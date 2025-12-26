package com.example.crp.bpm.web;

import com.example.crp.bpm.camunda.CamundaRestClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/bpm/restructuring")
public class RestructuringController {
    private final CamundaRestClient camundaRestClient;

    public RestructuringController(CamundaRestClient camundaRestClient) {
        this.camundaRestClient = camundaRestClient;
    }

    @PostMapping("/start")
    public Map<String, Object> start(@RequestBody Map<String, Object> vars) {
        return camundaRestClient.startProcess("restructuringProcess", vars);
    }
}
