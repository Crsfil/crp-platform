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

    @PatchMapping("/{id}/restructure")
    public Map<String, Object> restructure(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Agreement agreement = repo.findById(id).orElseThrow();
        Integer termMonths = toInt(body.get("termMonths"));
        Double rateAnnualPct = toDouble(body.get("rateAnnualPct"));
        if (termMonths != null) {
            agreement.setTermMonths(termMonths);
        }
        if (rateAnnualPct != null) {
            agreement.setRateAnnualPct(rateAnnualPct);
        }
        agreement.setStatus("RESTRUCTURED");
        agreement.setRestructuredAt(OffsetDateTime.now());
        agreement.setRestructureVersion((agreement.getRestructureVersion() == null ? 0 : agreement.getRestructureVersion()) + 1);
        repo.save(agreement);
        return Map.of(
                "status", agreement.getStatus(),
                "restructureVersion", agreement.getRestructureVersion()
        );
    }

    private Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(value.toString());
    }
}
