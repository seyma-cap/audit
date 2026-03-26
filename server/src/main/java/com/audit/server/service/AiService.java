package com.audit.server.service;

import com.audit.server.model.Audit;
import com.audit.server.model.SuccessCriteria;
import com.audit.server.projection.SuccessCriteriaProjection;
import com.audit.server.repo.AuditRepository;
import com.audit.server.repo.SuccessCriteriaRepository;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AiService {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private final SuccessCriteriaRepository criteriaRepo;
    private final AuditRepository auditRepo;
    private final JSoupService jSoupService;

    public AiService(SuccessCriteriaRepository criteriaRepo, AuditRepository auditRepo, JSoupService jSoupService) {
        this.criteriaRepo = criteriaRepo;
        this.auditRepo = auditRepo;
        this.jSoupService = jSoupService;
    }

    public String ask(String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", "meta-llama/llama-4-scout-17b-16e-instruct",
                "messages", List.of(Map.of("role", "user", "content", message))
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(
                "https://api.groq.com/openai/v1/chat/completions",
                request,
                Map.class
        );

        // Extract the content from the response
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
        return (String) msg.get("content");
    }

    public Map<String, String> generateResponse(String criteriaId, String auditId){
        SuccessCriteriaProjection projection = criteriaRepo.findBySuccessCriteriaRefId(criteriaId);
        Optional<Audit> audit = auditRepo.findById(auditId);

        if (projection == null || projection.getSuccessCriteria().isEmpty()) {
            throw new RuntimeException("No criteria found for refId: " + criteriaId);
        }

        if (audit.isEmpty()) {
            throw new RuntimeException("No audit found for id: " + auditId);
        }

        String url = audit.get().getUrl();
        SuccessCriteria criteria = projection.getSuccessCriteria().getFirst();

        checkAltText(jSoupService.getAltText(url));

        return Map.of();
    }

    public Map<String, String> checkAltText(Elements e){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", "meta-llama/llama-4-scout-17b-16e-instruct",
                "messages", List.of(Map.of("role", "user", "content", message))
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(
                "https://api.groq.com/openai/v1/chat/completions",
                request,
                Map.class
        );

        // Extract the content from the response
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
        return (String) msg.get("content");
    }
}