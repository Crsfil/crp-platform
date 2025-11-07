package com.example.crp.billing.web;

import com.example.crp.billing.domain.Invoice;
import com.example.crp.billing.repo.InvoiceRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/billing")
public class BillingController {
    private final InvoiceRepository repo;
    public BillingController(InvoiceRepository repo){ this.repo=repo; }
    @GetMapping("/invoices") public List<Invoice> list(){ return repo.findAll(); }
    @PostMapping("/invoices") public Invoice issue(@RequestBody Invoice i){ if(i.getStatus()==null) i.setStatus("ISSUED"); if(i.getDueDate()==null) i.setDueDate(LocalDate.now().plusMonths(1)); return repo.save(i);}    
    @PostMapping("/invoices/{id}/pay") public Map<String,Object> pay(@PathVariable Long id){ var inv=repo.findById(id).orElseThrow(); inv.setStatus("PAID"); repo.save(inv); return Map.of("status","PAID"); }
}

