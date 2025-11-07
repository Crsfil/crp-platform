package com.example.crp.edocs.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/edocs")
public class EdocsController {
    @GetMapping("/templates")
    public List<String> templates(){ return List.of("agreement.docx","act.docx"); }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generate(@RequestBody Map<String,Object> params){
        String content = "DOC TEMPLATE FILLED: "+params;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=generated.txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content.getBytes(StandardCharsets.UTF_8));
    }
}

