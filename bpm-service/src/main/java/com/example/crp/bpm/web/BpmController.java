package com.example.crp.bpm.web;

import org.springframework.web.bind.annotation.*;
import com.example.crp.bpm.camunda.CamundaRestClient;

import java.util.Map;

@RestController
@RequestMapping("/bpm")
public class BpmController {
    private final CamundaRestClient camundaRestClient;

    public BpmController(CamundaRestClient camundaRestClient) {
        this.camundaRestClient = camundaRestClient;
    }

    @PostMapping("/process/start")
    public Map<String,Object> start(@RequestBody Map<String,Object> vars){
        return camundaRestClient.startProcess("applicationProcess", vars);
    }
}
