package com.example.crp.agreement.web;

import com.example.crp.agreement.domain.Agreement;
import com.example.crp.agreement.repo.AgreementRepository;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/agreements")
public class AgreementController {
    private final AgreementRepository repo;
    public AgreementController(AgreementRepository repo){ this.repo=repo; }
    @GetMapping public List<Agreement> list(){ return repo.findAll(); }
    @PostMapping public Agreement create(@RequestBody Agreement a){ a.setStatus("DRAFT"); return repo.save(a);}    
    @PostMapping("/{id}/sign") public Map<String,Object> sign(@PathVariable Long id){ var a=repo.findById(id).orElseThrow(); a.setStatus("ACTIVE"); a.setSignedAt(OffsetDateTime.now()); a.setNumber("AGR-"+a.getId()); repo.save(a); return Map.of("status", a.getStatus(), "number", a.getNumber()); }
}

