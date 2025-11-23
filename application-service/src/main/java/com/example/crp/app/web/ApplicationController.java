package com.example.crp.app.web;

import com.example.crp.app.domain.Application;
import com.example.crp.app.repo.ApplicationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/applications")
public class ApplicationController {
    private final ApplicationRepository repo;
    private final WebClient bpmClient;

    public ApplicationController(ApplicationRepository repo,
                                 WebClient bpmClient) {
        this.repo = repo;
        this.bpmClient = bpmClient;
    }

    @GetMapping public List<Application> list(){ return repo.findAll(); }
    @PostMapping public Application create(@RequestBody Application a){
        a.setStatus("SUBMITTED"); a.setCreatedAt(OffsetDateTime.now());
        Application saved = repo.save(a);
        startBpm(saved);
        return saved;
    }

    @PostMapping("/{id}/process")
    public Map<String,Object> process(@PathVariable Long id) {
        Application app = repo.findById(id).orElseThrow();
        startBpm(app);
        return Map.of("status", app.getStatus(), "bpm", "started");
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Application> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return repo.findById(id)
                .map(app -> {
                    String status = body.getOrDefault("status", app.getStatus());
                    app.setStatus(status);
                    return ResponseEntity.ok(repo.save(app));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private void startBpm(Application app) {
        bpmClient.post()
                .uri("/bpm/process/start")
                .bodyValue(Map.of(
                        "applicationId", app.getId(),
                        "customerId", app.getCustomerId(),
                        "amount", app.getAmount(),
                        "termMonths", app.getTermMonths(),
                        "rateAnnualPct", app.getRateAnnualPct()
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
}
