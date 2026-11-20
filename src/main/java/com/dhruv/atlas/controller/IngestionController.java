package com.dhruv.atlas.controller;
import com.dhruv.atlas.entity.IngestionSource;
import com.dhruv.atlas.ingestion.IngestionOrchestrator;
import com.dhruv.atlas.service.PersistenceService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ingest")
public class IngestionController {
    private final IngestionOrchestrator ingestionOrchestrator;

    private final PersistenceService persistenceService;

    public IngestionController(IngestionOrchestrator ingestionOrchestrator,
                               PersistenceService persistenceService) {
        this.ingestionOrchestrator = ingestionOrchestrator;
        this.persistenceService = persistenceService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<String> ingest() {
        ingestionOrchestrator.ingest();
        return ResponseEntity.ok("Documents ingested successfully. ");
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @RequestParam @NotBlank String name,
            @RequestParam @NotBlank String sourceType,
            @RequestParam @NotBlank String sourceUrl) {

        IngestionSource src = persistenceService.registerIngestionSource(
                name, sourceType, sourceUrl
        );

        if (src != null) {
            return ResponseEntity.ok("Ingestion Source registered successfully.");
        }

        return ResponseEntity.badRequest().body("Registration failed.");
    }
}