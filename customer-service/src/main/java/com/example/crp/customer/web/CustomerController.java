package com.example.crp.customer.web;

import com.example.crp.customer.domain.Customer;
import com.example.crp.customer.repo.CustomerRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {
    private final CustomerRepository repo;
    public CustomerController(CustomerRepository repo) { this.repo = repo; }

    @GetMapping public List<Customer> list(){ return repo.findAll(); }
    @PostMapping public Customer create(@RequestBody Customer c){ if(c.getKycStatus()==null) c.setKycStatus("PENDING"); return repo.save(c);}    
}

