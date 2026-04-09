package com.audit.server.service;

import com.audit.server.model.Audit;
import com.audit.server.model.SuccessCriteria;
import com.audit.server.projection.SuccessCriteriaProjection;
import com.audit.server.repo.AuditRepository;
import com.audit.server.repo.SuccessCriteriaRepository;
import org.jsoup.select.Elements;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestTemplate;

import java.awt.*;
import java.beans.Encoder;
import java.util.*;
import java.util.List;
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
                "1.3.5", url -> checkLabels(jSoupService.getLabelsAndInput(url)),
                "1.4.2", url -> checkAudio(jSoupService.getAudioElements(url)),
                "2.4.2", url -> checkPageTitle(jSoupService.getTitle(url)),
                "2.4.4", url -> checkLinkPurpose(jSoupService.getLinks(url)),
                "2.4.6", url -> checkLabelHeadings(jSoupService.getLabelsHeading(url)),
                "3.1.1", url -> checkLanguage(jSoupService.getLangElement(url), url),
                "3.1.2", url -> checkAllLanguage(jSoupService.getAllLangElements(url), url),
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

        if (Objects.equals(criteriaId, "2.5.3")){
            return checkLabelNames(jSoupService.getLabels(url));
        }

        if (handler == null) {
            throw new UnsupportedOperationException("No handler for criteria: " + criteria.getRefId());
        }

        return handler.apply(url);
    }

    public String generateResponseWithPicture(String criteriaId, String auditId, List<byte[]> image){
        SuccessCriteriaProjection projection = criteriaRepo.findBySuccessCriteriaRefId(criteriaId);
        Optional<Audit> audit = auditRepo.findById(auditId);

        if (projection == null || projection.getSuccessCriteria().isEmpty()) {
            throw new RuntimeException("No criteria found for refId: " + criteriaId);
        }

        if (audit.isEmpty()) {
            throw new RuntimeException("No audit found for id: " + auditId);
        }

        if (Objects.equals(criteriaId, "1.3.2")){
            return checkMeaningfulness(image);
        }

        if (Objects.equals(criteriaId, "1.3.4")){
            return checkOrientation(image);
        }

        if (Objects.equals(criteriaId, "1.4.1")){
            return checkUseOfColor(image);
        }

        if (Objects.equals(criteriaId, "1.4.3")){
            return checkContrast(image);
        }

        if (Objects.equals(criteriaId, "1.4.4")){
            return checkResize(image);
        }

        if (Objects.equals(criteriaId, "1.4.5")){
            return checkImageText(image);
        }

        if (Objects.equals(criteriaId, "1.4.10")){
            return checkReflow(image);
        }

        if (Objects.equals(criteriaId, "1.4.12")){
            return checkSpacing(image);
        }

        return "";
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
     * Used for rule 1.3.2
     * @param images List that contains one or more byte[] need to be analyzed
     * @return String JSON format with the answer
     */
    public String checkMeaningfulness(List<byte[]> images) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Map<String, Object>> contentParts = new ArrayList<>();

        contentParts.add(Map.of(
                "type", "text",
                "text", "You are an Accessibility Expert (WCAG Specialist) responsible for detecting WCAG 2.2 violations on websites." +
                        " Do not limit your findings to the violations mentioned in common failures or test rules; explore beyond these areas for potential issues." +
                        "The image I am going to share needs to be checked based on the criteria in the WCAG 2.2 rules, criteria 1.3.2. " +
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
                        " Remember no backticks and only respond with the given format."
        ));

        for (byte[] imageBytes : images) {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = detectMimeType(imageBytes);
            String dataUrl = "data:" + mimeType + ";base64," + base64Image;

            contentParts.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", dataUrl)
            ));
        }

        Map<String, Object> message = Map.of(
                "role", "user",
                "content", contentParts
        );

        Map<String, Object> body = Map.of(
                "model", "meta-llama/llama-4-scout-17b-16e-instruct",
                "messages", List.of(message)
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(
                "https://api.groq.com/openai/v1/chat/completions",
                request,
                Map.class
        );

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");

        return msg.get("content").toString();
    }

    /**
     * Used for rule 1.3.4
     * @param images List that contains one or more byte[] need to be analyzed
     * @return String JSON format with the answer
     */
    public String checkOrientation(List<byte[]> images) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Map<String, Object>> contentParts = new ArrayList<>();

        contentParts.add(Map.of(
                "type", "text",
                "text", "You are an Accessibility Expert (WCAG Specialist) responsible for detecting WCAG 2.2 violations on websites." +
                        " Do not limit your findings to the violations mentioned in common failures or test rules; explore beyond these areas for potential issues." +
                        "The two images I am going to share needs to be checked based on the criteria in the WCAG 2.2 rules, criteria 1.3.4. " +
                        "One of them will be in landscape orientation, while the other one is in portrait orientation." +
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
                        "\n If nothing is provided, or you can't read the urls, you can return (remember no backticks): " +
                        "{\n" +
                        "    \"overall_violation\": \"N/A\",\n" +
                        "    \"violated_elements_and_reasons\": [}\n" +
                        "}" +
                        " Remember no backticks and only respond with the given format."
        ));

        for (byte[] imageBytes : images) {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = detectMimeType(imageBytes);
            String dataUrl = "data:" + mimeType + ";base64," + base64Image;

            contentParts.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", dataUrl)
            ));
        }

        Map<String, Object> message = Map.of(
                "role", "user",
                "content", contentParts
        );

        Map<String, Object> body = Map.of(
                "model", "meta-llama/llama-4-scout-17b-16e-instruct",
                "messages", List.of(message)
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(
                "https://api.groq.com/openai/v1/chat/completions",
                request,
                Map.class
        );

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");

        return msg.get("content").toString();
    }

    /**
     * Use for rule 1.3.5
     * @param e String that need to be analyzed
     * @return String JSON format with the answer
     */
    public String checkLabels(String e){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", "openai/gpt-oss-120b",
                "messages", List.of(Map.of("role", "user", "content",
                        "You are an Accessibility Expert (WCAG Specialist) responsible for detecting WCAG 2.2 violations on websites." +
                                "Only follow the rules provided by the official WCAG rules themselves." +
                                "The elements I am going to share needs to be checked based on the criteria in the WCAG 2.2 rules, criteria 1.3.5. " +
                                "Labels and input tags will be shared with you, and it is your job to make sure inputs have a corresponding label." +
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
                                "Here are the elements that need to be examined (elements can't be forgotten, so if no element is provided please respond with the correct response): " + e))
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
     * Used for rule 1.4.1
     * @param images List that contains one or more byte[] need to be analyzed
     * @return String JSON format with the answer
     */
    public String checkUseOfColor(List<byte[]> images) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Map<String, Object>> contentParts = new ArrayList<>();

        contentParts.add(Map.of(
                "type", "text",
                "text", "You are an Accessibility Expert (WCAG Specialist) responsible for detecting WCAG 2.2 violations on websites." +
                        " Do not limit your findings to the violations mentioned in common failures or test rules; explore beyond these areas for potential issues." +
                        "The image/images I am going to share need to be checked based on the criteria in the WCAG 2.2 rules, criteria 1.4.1. " +
                        "They will contain screenshots of a web application, it is up for you to analyse them and determine if color is not used as the " +
                        "only visual means of conveying information." +
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
                        "\n If nothing is provided, or you can't read the urls, you can return (remember no backticks): " +
                        "{\n" +
                        "    \"overall_violation\": \"N/A\",\n" +
                        "    \"violated_elements_and_reasons\": [}\n" +
                        "}" +
                        " Remember no backticks and only respond with the given format."
        ));

        for (byte[] imageBytes : images) {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = detectMimeType(imageBytes);
            String dataUrl = "data:" + mimeType + ";base64," + base64Image;

            contentParts.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", dataUrl)
            ));
        }

        Map<String, Object> message = Map.of(
                "role", "user",
                "content", contentParts
        );

        Map<String, Object> body = Map.of(
                "model", "meta-llama/llama-4-scout-17b-16e-instruct",
                "messages", List.of(message)
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(
                "https://api.groq.com/openai/v1/chat/completions",
                request,
                Map.class
        );

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");

        return msg.get("content").toString();
    }

    /**
     * Use for rule 1.4.2
     * @param e Elements that need to be analyzed
     * @return String JSON format with the answer
     */
    public String checkAudio(Elements e){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", "openai/gpt-oss-120b",
                "messages", List.of(Map.of("role", "user", "content",
                        "You are an Accessibility Expert (WCAG Specialist) responsible for detecting WCAG 2.2 violations on websites." +
                                "Only follow the rules provided by the official WCAG rules themselves." +
                                "The elements I am going to share needs to be checked based on the criteria in the WCAG 2.2 rules, criteria 1.4.2. " +
                                "Audio tags will be shared with you, and you have to determine if these pass the criteria mention in 1.4.2" +
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
                                "Here are the elements that need to be examined (elements can't be forgotten, so if no element is provided please respond with the correct response): " + e))
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
     * Used for rule 1.4.3
     * @param images List that contains one or more byte[] need to be analyzed
     * @return String JSON format with the answer
     */
    public String checkContrast(List<byte[]> images) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Map<String, Object>> contentParts = new ArrayList<>();

        contentParts.add(Map.of(
                "type", "text",
                "text", "You are an Accessibility Expert (WCAG Specialist) responsible for detecting WCAG 2.2 violations on websites." +
                        " Do not limit your findings to the violations mentioned in common failures or test rules; explore beyond these areas for potential issues." +
                        "The image/images I am going to share need to be checked based on the criteria in the WCAG 2.2 rules, criteria 1.4.3. " +
                        "They will contain screenshots of a web application, it is up for you to analyse them and determine if the contrast of text and images of text is " +
                        "according to the criteria mentioned in WCAG 1.4.3." +
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
                        "\n If nothing is provided, or you can't read the urls, you can return (remember no backticks): " +
                        "{\n" +
                        "    \"overall_violation\": \"N/A\",\n" +
                        "    \"violated_elements_and_reasons\": [}\n" +
                        "}" +
                        " Remember no backticks and only respond with the given format."
        ));

        for (byte[] imageBytes : images) {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = detectMimeType(imageBytes);
            String dataUrl = "data:" + mimeType + ";base64," + base64Image;

            contentParts.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", dataUrl)
            ));
        }

        Map<String, Object> message = Map.of(
                "role", "user",
                "content", contentParts
        );

        Map<String, Object> body = Map.of(
                "model", "meta-llama/llama-4-scout-17b-16e-instruct",
                "messages", List.of(message)
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(
                "https://api.groq.com/openai/v1/chat/completions",
                request,
                Map.class
        );

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");

        return msg.get("content").toString();
    }

    /**
     * Used for rule 1.4.4
     * @param images List that contains one or more byte[] need to be analyzed
     * @return String JSON format with the answer
     */
    public String checkResize(List<byte[]> images) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Map<String, Object>> contentParts = new ArrayList<>();

        contentParts.add(Map.of(
                "type", "text",
                "text", "You are an Accessibility Expert (WCAG Specialist) responsible for detecting WCAG 2.2 violations on websites." +
                        " Do not limit your findings to the violations mentioned in common failures or test rules; explore beyond these areas for potential issues." +
                        "The images I am going to share need to be checked based on the criteria in the WCAG 2.2 rules, criteria 1.4.4. " +
                        "They will contain screenshots of a web application, one that is in its regular size, and one zoomed in 200% " +
                        "Analyze these according to the criteria mentioned in WCAG 1.4.4." +
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
                        "\n If nothing is provided, or you can't read the urls, you can return (remember no backticks): " +
                        "{\n" +
                        "    \"overall_violation\": \"N/A\",\n" +
                        "    \"violated_elements_and_reasons\": [}\n" +
                        "}" +
                        " Remember no backticks and only respond with the given format."
        ));

        for (byte[] imageBytes : images) {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = detectMimeType(imageBytes);
            String dataUrl = "data:" + mimeType + ";base64," + base64Image;

            contentParts.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", dataUrl)
            ));
        }

        Map<String, Object> message = Map.of(
                "role", "user",
                "content", contentParts
        );

        Map<String, Object> body = Map.of(
                "model", "meta-llama/llama-4-scout-17b-16e-instruct",
                "messages", List.of(message)
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(
                "https://api.groq.com/openai/v1/chat/completions",
                request,
                Map.class
        );

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");

        return msg.get("content").toString();
    }

    /**
     * Used for rule 1.4.5
     * @param images List that contains one or more byte[] need to be analyzed
     * @return String JSON format with the answer
     */
    public String checkImageText(List<byte[]> images) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Map<String, Object>> contentParts = new ArrayList<>();

        contentParts.add(Map.of(
                "type", "text",
                "text", "You are an Accessibility Expert (WCAG Specialist) responsible for detecting WCAG 2.2 violations on websites." +
                        " Do not limit your findings to the violations mentioned in common failures or test rules; explore beyond these areas for potential issues." +
                        "The images I am going to share need to be checked based on the criteria in the WCAG 2.2 rules, criteria 1.4.5. Focus on this criterion only, " +
                        "with the exception of: 1. decorative images 2. text that is not significant 3. the text in the image is essential" +
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
                        "\n If nothing is provided, or you can't read the urls, you can return (remember no backticks): " +
                        "{\n" +
                        "    \"overall_violation\": \"N/A\",\n" +
                        "    \"violated_elements_and_reasons\": [}\n" +
                        "}" +
                        " Remember no backticks and only respond with the given format."
        ));

        for (byte[] imageBytes : images) {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = detectMimeType(imageBytes);
            String dataUrl = "data:" + mimeType + ";base64," + base64Image;

            contentParts.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", dataUrl)
            ));
        }

        Map<String, Object> message = Map.of(
                "role", "user",
                "content", contentParts
        );

        Map<String, Object> body = Map.of(
                "model", "meta-llama/llama-4-scout-17b-16e-instruct",
                "messages", List.of(message)
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(
                "https://api.groq.com/openai/v1/chat/completions",
                request,
                Map.class
        );

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");

        return msg.get("content").toString();
    }

    /**
     * Used for rule 1.4.10
     * @param images List that contains one or more byte[] need to be analyzed
     * @return String JSON format with the answer
     */
    public String checkReflow(List<byte[]> images) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Map<String, Object>> contentParts = new ArrayList<>();

        contentParts.add(Map.of(
                "type", "text",
                "text", "You are an Accessibility Expert (WCAG Specialist) responsible for detecting WCAG 2.2 violations on websites." +
                        " Do not limit your findings to the violations mentioned in common failures or test rules; explore beyond these areas for potential issues." +
                        "The images I am going to share need to be checked based on the criteria in the WCAG 2.2 rules, criteria 1.4.10. " +
                        "They will contain screenshots of a web application, one that is in its regular size, and one zoomed in 400% " +
                        "Analyze these according to the criteria mentioned in WCAG 1.4.10. Please pay attention to: 1. content disappearing 2. content requiring scrolling in two dimensions" +
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
                        "\n If nothing is provided, or you can't read the urls, you can return (remember no backticks): " +
                        "{\n" +
                        "    \"overall_violation\": \"N/A\",\n" +
                        "    \"violated_elements_and_reasons\": [}\n" +
                        "}" +
                        " Remember no backticks and only respond with the given format."
        ));

        for (byte[] imageBytes : images) {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = detectMimeType(imageBytes);
            String dataUrl = "data:" + mimeType + ";base64," + base64Image;

            contentParts.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", dataUrl)
            ));
        }

        Map<String, Object> message = Map.of(
                "role", "user",
                "content", contentParts
        );

        Map<String, Object> body = Map.of(
                "model", "meta-llama/llama-4-scout-17b-16e-instruct",
                "messages", List.of(message)
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(
                "https://api.groq.com/openai/v1/chat/completions",
                request,
                Map.class
        );

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");

        return msg.get("content").toString();
    }

    /**
     * Used for rule 1.4.12
     * @param images List that contains one or more byte[] need to be analyzed
     * @return String JSON format with the answer
     */
    public String checkSpacing(List<byte[]> images) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Map<String, Object>> contentParts = new ArrayList<>();

        contentParts.add(Map.of(
                "type", "text",
                "text", "You are an Accessibility Expert (WCAG Specialist) responsible for detecting WCAG 2.2 violations on websites." +
                        " Do not limit your findings to the violations mentioned in common failures or test rules; explore beyond these areas for potential issues." +
                        "The image/images I am going to share need to be checked based on the criteria in the WCAG 2.2 rules, criteria 1.4.12. Focus on this criterion." +
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
                        "\n If nothing is provided, or you can't read the urls, you can return (remember no backticks): " +
                        "{\n" +
                        "    \"overall_violation\": \"N/A\",\n" +
                        "    \"violated_elements_and_reasons\": [}\n" +
                        "}" +
                        " Remember no backticks and only respond with the given format."
        ));

        for (byte[] imageBytes : images) {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = detectMimeType(imageBytes);
            String dataUrl = "data:" + mimeType + ";base64," + base64Image;

            contentParts.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", dataUrl)
            ));
        }

        Map<String, Object> message = Map.of(
                "role", "user",
                "content", contentParts
        );

        Map<String, Object> body = Map.of(
                "model", "meta-llama/llama-4-scout-17b-16e-instruct",
                "messages", List.of(message)
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(
                "https://api.groq.com/openai/v1/chat/completions",
                request,
                Map.class
        );

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");

        return msg.get("content").toString();
    }

    /**
     * Use for rule 2.4.2
     * @param e Elements that need to be analyzed
     * @return String JSON format with the answer
     */
    public String checkPageTitle(Elements e){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", "openai/gpt-oss-120b",
                "messages", List.of(Map.of("role", "user", "content",
                        "You are an Accessibility Expert (WCAG Specialist) responsible for detecting WCAG 2.2 violations on websites." +
                                "Only follow the rules provided by the official WCAG rules themselves." +
                                "The elements I am going to share needs to be checked based on the criteria in the WCAG 2.2 rules, criteria 2.4.2. " +
                                "Determine if the titles are descriptive enough and give you a clue on what the website could be about" +
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
                                "Here are the elements that need to be examined (elements can't be forgotten, so if no element is provided please respond with the correct response): " + e))
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
     * Use for rule 2.4.4
     * @param e Elements that need to be analyzed
     * @return String JSON format with the answer
     */
    public String checkLinkPurpose(Elements e){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", "openai/gpt-oss-120b",
                "messages", List.of(Map.of("role", "user", "content",
                        "You are an Accessibility Expert (WCAG Specialist) responsible for detecting WCAG 2.2 violations on websites." +
                                "Only follow the rules provided by the official WCAG rules themselves." +
                                "The elements I am going to share needs to be checked based on the criteria in the WCAG 2.2 rules, criteria 2.4.4. " +
                                "Determine if the titles are descriptive enough and give you a clue on what the link could lead to." +
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
                                "Here are the elements that need to be examined (elements can't be forgotten, so if no element is provided please respond with the correct response): " + e))
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
     * Use for rule 2.4.6
     * @param e Elements that need to be analyzed
     * @return String JSON format with the answer
     */
    public String checkLabelHeadings(Elements e){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", "openai/gpt-oss-120b",
                "messages", List.of(Map.of("role", "user", "content",
                        "You are an Accessibility Expert (WCAG Specialist) responsible for detecting WCAG 2.2 violations on websites." +
                                "Only follow the rules provided by the official WCAG rules themselves." +
                                "The elements I am going to share needs to be checked based on the criteria in the WCAG 2.2 rules, criteria 2.4.6. " +
                                "Determine if the labels and heading are descriptive enough and give you a clue on what the link could lead to." +
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
                                "Here are the elements that need to be examined (elements can't be forgotten, so if no element is provided please respond with the correct response): " + e))
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
     * Use for rule 2.5.3
     * @param e Elements that need to be analyzed
     * @return String JSON format with the answer
     */
    public String checkLabelNames(Elements e){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", "openai/gpt-oss-120b",
                "messages", List.of(Map.of("role", "user", "content",
                        "You are an Accessibility Expert (WCAG Specialist) responsible for detecting WCAG 2.2 violations on websites." +
                                "Only follow the rules provided by the official WCAG rules themselves." +
                                "The elements I am going to share needs to be checked based on the criteria in the WCAG 2.2 rules, criteria 2.5.3. " +
                                "Determine if the labels have a similar name as the text they are presenting." +
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
                                "Here are the elements that need to be examined (elements can't be forgotten, so if no element is provided please respond with the correct response): " + e))
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

    private String detectMimeType(byte[] bytes) {
        if (bytes.length >= 3
                && bytes[0] == (byte) 0xFF
                && bytes[1] == (byte) 0xD8
                && bytes[2] == (byte) 0xFF) {
            return "image/jpeg";
        } else if (bytes.length >= 8
                && bytes[0] == (byte) 0x89
                && bytes[1] == 0x50 // 'P'
                && bytes[2] == 0x4E // 'N'
                && bytes[3] == 0x47) { // 'G'
            return "image/png";
        } else if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return "image/gif";
        } else if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F') {
            return "image/webp";
        }
        return "image/jpeg"; // fallback
    }
}