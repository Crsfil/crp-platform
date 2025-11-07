package com.example.crp.bpm.web;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/bpm")
public class BpmController {
    private final RuntimeService runtimeService;
    public BpmController(RuntimeService runtimeService) { this.runtimeService = runtimeService; }

    @PostMapping("/process/start")
    public Map<String,Object> start(@RequestBody Map<String,Object> vars){
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("applicationProcess", vars);
        return Map.of("id", pi.getId(), "definitionId", pi.getProcessDefinitionId(), "businessKey", pi.getBusinessKey());
    }
}
