package com.audit.server.service;

import com.audit.server.model.Audit;
import com.audit.server.model.SuccessCriteria;
import com.audit.server.projection.SuccessCriteriaProjection;
import com.audit.server.repo.AuditRepository;
import com.audit.server.repo.SuccessCriteriaRepository;
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
import java.util.function.Function;

@Service
public class AiService {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private final SuccessCriteriaRepository criteriaRepo;
    private final AuditRepository auditRepo;
    private final JSoupService jSoupService;

    private final Map<String, Function<String, String>> criteriaHandlers;

    public AiService(SuccessCriteriaRepository criteriaRepo, AuditRepository auditRepo, JSoupService jSoupService) {
        this.criteriaRepo = criteriaRepo;
        this.auditRepo = auditRepo;
        this.jSoupService = jSoupService;

        this.criteriaHandlers = Map.of(
                "1.1.1", url -> checkAltText(jSoupService.getAltText(url), url),
                "3.1.1", url -> checkLanguage(jSoupService.getLangElement(url), url),
                "3.1.2", url -> checkAllLanguage(jSoupService.getAllLangElements(url), url),
//                "3.3.1", url -> checkErrorIdentification(jSoupService.getFormElements(url), url),
                "3.3.2", url -> checkFormInstructions(jSoupService.getFormElements(url), url),
                "4.1.2", url -> checkRole(jSoupService.getCustomElements(url), url)
        );
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

    public String generateResponse(String criteriaId, String auditId){
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

        Function<String, String> handler = criteriaHandlers.get(criteria.getRefId());

        if (handler == null) {
            throw new UnsupportedOperationException("No handler for criteria: " + criteria.getRefId());
        }

        return handler.apply(url);
    }

    /**
     * Used for rule 1.1.1
     * @param e Elements that need to be analyzed
     * @return String JSON format with the answer
     */
    public String checkAltText(Elements e, String url){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", "openai/gpt-oss-120b",
                "messages", List.of(Map.of("role", "user", "content",
                        "You are an Accessibility Expert (WCAG Specialist) responsible for detecting WCAG 2.2 violations on websites." +
                                " Do not limit your findings to the violations mentioned in common failures or test rules; explore beyond these areas for potential issues." +
                        "The elements I am going to share needs to be checked based on the criteria in the WCAG 2.2 rules, criteria 1.1.1. " +
                                "Respond in the format I hand to you, making sure to respond only with that JSON and nothing else. " +
                                "Give the criteria an answer of failing or passing, and don't use backticks in your answers. Only respond with the JSON." +
                                "The format is in JSON and right after this line (remember no backticks) \n" +
                                "{\n" +
                                "    \"overall_violation\": \"passed or failed\",\n" +
                                "    \"violated_elements_and_reasons\": [\n" +
                                "        {\n" +
                                "            \"title\": \"A single sentence to describe the problem\",\n" +
                                "            \"description\": \"Explanation of why it violates the criterion\",\n" +
                                "            \"recommendation\": \"Recommendation to fix theviolation for this specific element\"\n" +
                                "        }\n" +
                                "    ]\n" +
                                "}" +
                                "If there are no violations, the response should be (remember no backticks): \n" +
                                "{\n" +
                                "    \"overall_violation\": \"passed\",\n" +
                                "    \"violated_elements_and_reasons\": [}\n" +
                                "}" +
                        "Here are the elements that need to be examined: " + e +
                        "\n If no element is provided you can return (remember no backticks): " +
                                "{\n" +
                                "    \"overall_violation\": \"N/A\",\n" +
                                "    \"violated_elements_and_reasons\": [}\n" +
                                "}" +
                                " And finally the URL of the website: " + url +
                        " Remember no backticks and only respond with the given format."))
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
        return msg.get("content").toString();
    }


    /**
     * Used for rule 3.1.1
     * @param e boolean whether the tag is present
     * @return String JSON format with the answer
     */
    public String checkLanguage(boolean e, String url){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", "openai/gpt-oss-120b",
                "messages", List.of(Map.of("role", "user", "content",
                        "You are an Accessibility Expert (WCAG Specialist) responsible for detecting WCAG 2.2 violations on websites." +
                                "Do not limit your findings to the violations mentioned in common failures or test rules; explore beyond these areas for potential issues." +
                                "The elements I am going to share needs to be checked based on the criteria in the WCAG 2.2 rules, criteria 3.1.1. " +
                                "Respond in the format I hand to you, making sure to respond only with that JSON and nothing else. " +
                                "Give the criteria an answer of failing or passing, and don't use backticks in your answers. Only respond with the JSON." +
                                "The format is in JSON and right after this line (remember no backticks) \n" +
                                "{\n" +
                                "    \"overall_violation\": \"passed or failed\",\n" +
                                "    \"violated_elements_and_reasons\": [\n" +
                                "        {\n" +
                                "            \"title\": \"A single sentence to describe the problem\",\n" +
                                "            \"description\": \"Explanation of why it violates the criterion\",\n" +
                                "            \"recommendation\": \"Recommendation to fix theviolation for this specific element\"\n" +
                                "        }\n" +
                                "    ]\n" +
                                "}" +
                                "If there are no violations, the response should be (remember no backticks): \n" +
                                "{\n" +
                                "    \"overall_violation\": \"passed\",\n" +
                                "    \"violated_elements_and_reasons\": [}\n" +
                                "}" +
                                "The element has already been checked, i will provide you with a boolean that determines whether the website contains a lang attribute. The boolean is: " + e +
                        "which means that it is " + e + " that the website contains a lang element. Please only check if the element is present, the other rule will check if it is valid"  +
                                " And finally the URL of the website: " + url))
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
        return msg.get("content").toString();
    }

    /**
     * Used for rule 3.1.2
     * @param s String that need to be analyzed
     * @return String JSON format with the answer
     */
    public String checkAllLanguage(String s, String url){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", "openai/gpt-oss-120b",
                "messages", List.of(Map.of("role", "user", "content",
                        "You are an Accessibility Expert (WCAG Specialist) responsible for detecting WCAG 2.2 violations on websites." +
                                "Only follow the rules provided by the official WCAG rules themselves." +
                                "The elements I am going to share needs to be checked based on the criteria in the WCAG 2.2 rules, criteria 3.1.2. " +
                                "Respond in the format I hand to you, making sure to respond only with that JSON and nothing else. " +
                                "Give the criteria an answer of failing or passing, and don't use backticks in your answers. Only respond with the JSON." +
                                "The format is in JSON and right after this line (remember no backticks) \n" +
                                "{\n" +
                                "    \"overall_violation\": \"passed or failed\",\n" +
                                "    \"violated_elements_and_reasons\": [\n" +
                                "        {\n" +
                                "            \"title\": \"A single sentence to describe the problem\",\n" +
                                "            \"description\": \"Explanation of why it violates the criterion\",\n" +
                                "            \"recommendation\": \"Recommendation to fix theviolation for this specific element\"\n" +
                                "        }\n" +
                                "    ]\n" +
                                "}" +
                                "If there are no violations, the response should be (remember no backticks): \n" +
                                "{\n" +
                                "    \"overall_violation\": \"passed\",\n" +
                                "    \"violated_elements_and_reasons\": [}\n" +
                                "}" +
                                "\n If no element is provided you can return (remember no backticks): " +
                                "{\n" +
                                "    \"overall_violation\": \"N/A\",\n" +
                                "    \"violated_elements_and_reasons\": [}\n" +
                                "}" +
                                "Here are the elements that need to be examined (elements can't be forgotten, so if no element is provided please respond with the correct response): " + s +
                                " And finally the URL of the website: " + url))
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
        return msg.get("content").toString();
    }

    /**
     * Used for rule 3.3.1
     * @param e Elements that need to be analyzed
     * @return String JSON format with the answer
     */
    public String checkErrorIdentification(Elements e, String url){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", "openai/gpt-oss-120b",
                "messages", List.of(Map.of("role", "user", "content",
                        "You are an Accessibility Expert (WCAG Specialist) responsible for detecting WCAG 2.2 violations on websites." +
                                "Only follow the rules provided by the official WCAG rules themselves." +
                                "The elements I am going to share needs to be checked based on the criteria in the WCAG 2.2 rules, criteria 3.3.1. " +
                                "The entire form will be shared with you, and it is your job to make sure any form of error detection in present. Try to look for subtle components, such as div right underneath" +
                                "the input fields. Or hidden elements that could potentially be used to show error." +
                                "Respond in the format I hand to you, making sure to respond only with that JSON and nothing else. " +
                                "Give the criteria an answer of failing or passing, and DO NOT use backticks in your answers. Only respond with the JSON." +
                                "The format is in JSON and right after this line (remember no backticks) \n" +
                                "{\n" +
                                "    \"overall_violation\": \"passed or failed\",\n" +
                                "    \"violated_elements_and_reasons\": [\n" +
                                "        {\n" +
                                "            \"title\": \"A single sentence to describe the problem\",\n" +
                                "            \"description\": \"Explanation of why it violates the criterion\",\n" +
                                "            \"recommendation\": \"Recommendation to fix the violation for this specific element\"\n" +
                                "        }\n" +
                                "    ]\n" +
                                "}" +
                                "If there are no violations, the response should be (remember no backticks): \n" +
                                "{\n" +
                                "    \"overall_violation\": \"passed\",\n" +
                                "    \"violated_elements_and_reasons\": [}\n" +
                                "}" +
                                "\n If no element is provided you can return (remember no backticks): " +
                                "{\n" +
                                "    \"overall_violation\": \"N/A\",\n" +
                                "    \"violated_elements_and_reasons\": [}\n" +
                                "}" +
                                "Here are the elements that need to be examined (elements can't be forgotten, so if no element is provided please respond with the correct response): " + e +
                                " And finally the URL of the website: " + url))
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
        return msg.get("content").toString();
    }

/**
     * Used for rule 3.3.2
     * @param e Elements that need to be analyzed
     * @return String JSON format with the answer
     */
    public String checkFormInstructions(Elements e, String url){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", "openai/gpt-oss-120b",
                "messages", List.of(Map.of("role", "user", "content",
                        "You are an Accessibility Expert (WCAG Specialist) responsible for detecting WCAG 2.2 violations on websites." +
                                "Only follow the rules provided by the official WCAG rules themselves." +
                                "The elements I am going to share needs to be checked based on the criteria in the WCAG 2.2 rules, criteria 3.3.2. " +
                                "The entire form will be shared with you, and it is your job to make sure any form of error detection in present." +
                                "Respond in the format I hand to you, making sure to respond only with that JSON and nothing else. " +
                                "Give the criteria an answer of failing or passing, and DO NOT use backticks in your answers. Only respond with the JSON." +
                                "The format is in JSON and right after this line (remember no backticks) \n" +
                                "{\n" +
                                "    \"overall_violation\": \"passed or failed\",\n" +
                                "    \"violated_elements_and_reasons\": [\n" +
                                "        {\n" +
                                "            \"title\": \"A single sentence to describe the problem\",\n" +
                                "            \"description\": \"Explanation of why it violates the criterion\",\n" +
                                "            \"recommendation\": \"Recommendation to fix the violation for this specific element\"\n" +
                                "        }\n" +
                                "    ]\n" +
                                "}" +
                                "If there are no violations, the response should be (remember no backticks): \n" +
                                "{\n" +
                                "    \"overall_violation\": \"passed\",\n" +
                                "    \"violated_elements_and_reasons\": [}\n" +
                                "}" +
                                "\n If no element is provided you can return (remember no backticks): " +
                                "{\n" +
                                "    \"overall_violation\": \"N/A\",\n" +
                                "    \"violated_elements_and_reasons\": [}\n" +
                                "}" +
                                "Here are the elements that need to be examined (elements can't be forgotten, so if no element is provided please respond with the correct response): " + e +
                                " And finally the URL of the website: " + url))
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
        return msg.get("content").toString();
    }

    /**
     * Used for rule 3.3.2
     * @param s String that needs to be analyzed
     * @return String JSON format with the answer
     */
    public String checkRole(String s, String url){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", "openai/gpt-oss-120b",
                "messages", List.of(Map.of("role", "user", "content",
                        "You are an Accessibility Expert (WCAG Specialist) responsible for detecting WCAG 2.2 violations on websites." +
                                "Only follow the rules provided by the official WCAG rules themselves." +
                                "The elements I am going to share needs to be checked based on the criteria in the WCAG 2.2 rules, criteria 4.1.2. " +
                                "The entire form will be shared with you, and it is your job to make sure any form of error detection in present." +
                                "Respond in the format I hand to you, making sure to respond only with that JSON and nothing else. " +
                                "Give the criteria an answer of failing or passing, and DO NOT use backticks in your answers. Only respond with the JSON." +
                                "The format is in JSON and right after this line (remember no backticks) \n" +
                                "{\n" +
                                "    \"overall_violation\": \"passed or failed\",\n" +
                                "    \"violated_elements_and_reasons\": [\n" +
                                "        {\n" +
                                "            \"title\": \"A single sentence to describe the problem\",\n" +
                                "            \"description\": \"Explanation of why it violates the criterion\",\n" +
                                "            \"recommendation\": \"Recommendation to fix the violation for this specific element\"\n" +
                                "        }\n" +
                                "    ]\n" +
                                "}" +
                                "If there are no violations, the response should be (remember no backticks): \n" +
                                "{\n" +
                                "    \"overall_violation\": \"passed\",\n" +
                                "    \"violated_elements_and_reasons\": [}\n" +
                                "}" +
                                "\n If no element is provided you can return (remember no backticks): " +
                                "{\n" +
                                "    \"overall_violation\": \"N/A\",\n" +
                                "    \"violated_elements_and_reasons\": [}\n" +
                                "}" +
                                "Here are the elements that need to be examined (elements can't be forgotten, so if no element is provided please respond with the correct response): " + s +
                        " And finally the URL of the website: " + url))
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
        return msg.get("content").toString();
    }

}