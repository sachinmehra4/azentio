package com.turnstile.login.api;

import com.turnstile.login.repository.LoginRiskRepository;
import com.turnstile.login.service.LoginRiskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/risk")
public class LoginRiskController {
    private final LoginRiskService service;
    private final LoginRiskRepository repository;

    public LoginRiskController(LoginRiskService service, LoginRiskRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @PostMapping("/login")
    public RiskDecisionResponse login(@Valid @RequestBody LoginRequest request) {
        return service.evaluate(request);
    }

    @PostMapping("/decisions/{id}/step-up")
    public RiskDecisionResponse stepUp(@PathVariable long id,
                                       @Valid @RequestBody StepUpRequest request) {
        return service.recordStepUp(id, request.passed());
    }

    @GetMapping("/decisions")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be >= 0 and size must be 1..100");
        }
        List<RiskDecisionResponse> content = repository.listDecisions(size, page * size);
        long total = repository.countDecisions();
        return Map.of(
                "content", content,
                "page", page,
                "size", size,
                "totalElements", total,
                "totalPages", (int) Math.ceil((double) total / size)
        );
    }
}
