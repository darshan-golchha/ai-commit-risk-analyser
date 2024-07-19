package sev.service;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sev.model.Metric;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Service
public class SeverityService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${sonarcloud.api.measures.url}")
    private String sonarMeasuresUrl;

    @Value("${sonarcloud.api.commit.hash.url}")
    private String sonarCommitHashUrl;

    @Value("${bitbucket.api.commit.diff.url}")
    private String bitbucketCommitDiffUrl;

    @Value("${sonarcloud.username}")
    private String sonarUsername;

    @Value("${sonarcloud.password}")
    private String sonarPassword;

    @Value("${bitbucket.username}")
    private String bitbucketUsername;

    @Value("${bitbucket.app.password}")
    private String bitbucketAppPassword;

    @Value("${analytics.url}")
    private String analyticsUrl;
    
    @Value("${complexity.url}")
    private String complexityBaseUrl;
    
    private List<Metric> metrics;
    
    private double averageComplexity;

    public SeverityService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.metrics = new ArrayList<>();
        metrics.add(new Metric("minor_violations", 1000, 0.001, 2));
        metrics.add(new Metric("code_smells", 3000, 0.001, 3));
        metrics.add(new Metric("reliability_rating", 5, 1, 5));
        metrics.add(new Metric("new_code_smells", 5, 1, 3));
        metrics.add(new Metric("new_vulnerabilities", 5, 1, 6));
        metrics.add(new Metric("sqale_index", 20000, 0.00015, 5));
        metrics.add(new Metric("violations", 5000, 0.0008, 5));
        metrics.add(new Metric("security_rating", 5, 1, 5));
        metrics.add(new Metric("critical_violations", 20, 0.5, 5));
        metrics.add(new Metric("vulnerabilities", 10, 0.6, 6));
        metrics.add(new Metric("new_violations", 5, 1, 5));
        metrics.add(new Metric("bugs", 200, 0.02, 6));
        metrics.add(new Metric("major_violations", 1500, 0.0066, 4));
        metrics.add(new Metric("new_bugs", 5, 1, 6));
    }

    public ObjectNode sonarReciever(String jsonData) {
        try {
            // Fetch data concurrently
            CompletableFuture<String> metricsFuture = CompletableFuture.supplyAsync(() -> getFromSonarCloud(sonarMeasuresUrl));
            CompletableFuture<String> commitHashFuture = CompletableFuture.supplyAsync(() -> getFromSonarCloud(sonarCommitHashUrl));

            // Wait for all futures to complete
            CompletableFuture.allOf(metricsFuture, commitHashFuture).join();

            // Retrieve the results
            String metricsResponse = metricsFuture.get();
            String commitHashResponse = commitHashFuture.get();

            // Extract commit hash and fetch commit diff from Bitbucket
            String commitHash = extractLatestCommitHash(commitHashResponse);
            String commitDiffUrl = bitbucketCommitDiffUrl + "/" + commitHash;
            String commitDiffResponse = getFromBitbucket(commitDiffUrl);

            // Parse the responses into JSON objects
            JsonNode metricsJson = objectMapper.readTree(metricsResponse);
            JsonNode payloadJson = objectMapper.readTree(jsonData);
            JsonNode commitDiffJson = objectMapper.createObjectNode().put("diff", commitDiffResponse);

            // Extract filenames from the commit diff
            List<String> filenames = extractFilenamesFromDiff(commitDiffResponse);

            // Fetch complexities for each file
            Map<String, Integer> complexities = fetchComplexitiesForFiles(filenames);
            
            this.averageComplexity = calculateAverageComplexity(complexities);
            

            // Send the diff to the analytics URL and get the response
            String analyticsResponse = sendDiffForAnalytics(commitDiffResponse);
            
            // Parse the analytics response into JSON
            JsonNode analyticsJson = consumeAnalytics(objectMapper.readTree(analyticsResponse));
            
            // Calculate Sonar severity
            double sonarSeverity = calculateSonarSeverity(metricsJson);
            double masterSeverity = calculateMasterSeverity(sonarSeverity, analyticsJson.get("reviewSeverities").asDouble(), analyticsJson.get("resultSeverities").asDouble());

            // Create a combined JSON object
            ObjectNode combinedData = objectMapper.createObjectNode();
            combinedData.set("metrics", metricsJson);
            combinedData.set("payload", payloadJson);
            combinedData.put("commitHash", commitHash);
            combinedData.set("commitDiff", commitDiffJson);
            combinedData.set("analytics", analyticsJson);
            combinedData.put("sonarSeverity", sonarSeverity);
            combinedData.put("masterSeverity", masterSeverity);
            combinedData.set("complexity", objectMapper.valueToTree(this.averageComplexity));

            // Send the combined data to the frontend
            messagingTemplate.convertAndSend("/sonarmetrics/recieved", combinedData.toString());
            return combinedData;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getFromSonarCloud(String url) {
        HttpHeaders headers = new HttpHeaders();
        String auth = sonarUsername + ":" + sonarPassword;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return response.getBody();
    }

    private String extractLatestCommitHash(String commitHashResponse) throws Exception {
        JsonNode root = objectMapper.readTree(commitHashResponse);
        JsonNode analyses = root.path("analyses");

        // Get the first analysis entry which is the latest
        JsonNode latestAnalysis = analyses.get(0);

        return latestAnalysis.path("revision").asText();
    }

    private String getFromBitbucket(String url) {
        HttpHeaders headers = new HttpHeaders();
        String auth = bitbucketUsername + ":" + bitbucketAppPassword;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return response.getBody();
    }

    private String sendDiffForAnalytics(String diff) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            ObjectNode requestJson = objectMapper.createObjectNode();
            requestJson.put("diff_content", diff);

            HttpEntity<String> requestEntity = new HttpEntity<>(requestJson.toString(), headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(analyticsUrl, HttpMethod.POST, requestEntity, String.class);

            return responseEntity.getBody();
        } catch (Exception e) {
            return null;
        }
    }
    
    private JsonNode consumeAnalytics(JsonNode analytics) {
        // Initialize lists to store severities
        List<Double> resultSeverities = new ArrayList<>();
        List<Integer> reviewSeverities = new ArrayList<>();
        
        // Extract severities from the results section
        JsonNode results = analytics.path("results");
        for (JsonNode result : results) {
            double severity = result.path("severity").asDouble();
            resultSeverities.add(severity);
        }
        
        // Extract severities from the reviews section
        JsonNode reviews = analytics.path("reviews");
        for (JsonNode review : reviews) {
            String reviewText = review.asText();
            String severityMarker = "[severity: ";
            int severityIndex = reviewText.indexOf(severityMarker);
            if (severityIndex != -1) {
                int start = severityIndex + severityMarker.length();
                int end = reviewText.indexOf("/10]", start);
                if (end != -1) {
                    String severityStr = reviewText.substring(start, end).trim();
                    try {
                        int severity = Integer.parseInt(severityStr);
                        reviewSeverities.add(severity);
                    } catch (NumberFormatException e) {
                        // Handle the case where severity is not a valid integer
                        e.printStackTrace();
                    }
                }
            }
        }
        
        
        double avgResSev = 0;
        for (Double d: resultSeverities) {
            avgResSev+=d;
        }
        avgResSev = avgResSev/resultSeverities.size();
        
        double avgRevSev = 0;
        for (Integer d: reviewSeverities) {
            avgRevSev+=d;
        }
        avgRevSev = avgRevSev/reviewSeverities.size();

        
        // Create an ObjectNode to hold the extracted severities
        ObjectNode severities = objectMapper.createObjectNode();
        severities.putPOJO("resultSeverities", avgResSev);
        severities.putPOJO("reviewSeverities", avgRevSev);
        severities.putPOJO("results", analytics.path("results"));
        severities.putPOJO("reviews", analytics.path("reviews"));
        
        return severities;
    }
    
    private double calculateSonarSeverity(JsonNode metricsJson) {
        double w_value = 0.0;
        double m_value = 0.0;

        Map<String, Metric> metricsMap = metrics.stream()
                                                 .collect(Collectors.toMap(Metric::getName, metric -> metric));

        for (JsonNode measure : metricsJson.path("component").path("measures")) {
            String metricName = measure.path("metric").asText();
            double value;
            if (measure.has("period")) {
                value = measure.path("period").path("value").asDouble();
            } else {
                value = measure.path("value").asDouble();
            }

            Metric metric = metricsMap.get(metricName);
            if (metric == null) {
                continue;
            }

            double equaliser = metric.getEqualiser();
            double maxValue = metric.getMaxValue();

            w_value += equaliser * value;
            m_value += equaliser * maxValue;
        }
//        Adding w_value and m_value for cyclomatic complexity of the file
        w_value += 0.015 * this.averageComplexity;
        m_value += 0.015 * 100;

        return (w_value / m_value) * 100;
    }


    private double calculateMasterSeverity(double sonarSeverity, double reviewSeverities, double resultSeverities) {
    	// Define the importance weights
        int weightSonar = 60;
        int weightBugFrequency = 30;
        int weightCodeReview = 10;

        // Scale sonarSeverity from 6000-135000 to 0-1000
        double scaledSonarSeverity = (sonarSeverity - 0) / (100 - 0) * 100;
        
        // Scale codeReviewSeverity from 1-10 to 0-1000
        double scaledCodeReviewSeverity = (reviewSeverities - 1) / (10 - 1) * 100;
        
        // Scale bugFrequencySeverity from 0-150 to 0-1000
        double scaledBugFrequencySeverity = (resultSeverities / 150) * 100;
        
        // Calculate the masterSeverity
        double masterSeverity = (weightSonar * scaledSonarSeverity 
                                 + weightBugFrequency * scaledBugFrequencySeverity 
                                 + weightCodeReview * scaledCodeReviewSeverity) 
                                 / (weightSonar + weightBugFrequency + weightCodeReview);

        return masterSeverity;
    }

    private List<String> extractFilenamesFromDiff(String commitDiffResponse) throws Exception {
        List<String> filenames = new ArrayList<>();
        String[] lines = commitDiffResponse.split("\n");

        for (String line : lines) {
            if (line.startsWith("diff --git")) {
                String[] parts = line.split(" ");
                if (parts.length >= 3) {
                    String fileA = parts[2].substring(2); // remove the 'a/' prefix
                    String fileB = parts[3].substring(2); // remove the 'b/' prefix

                    if (!filenames.contains(fileA)) {
                        filenames.add(fileA);
                    }
                    if (!filenames.contains(fileB)) {
                        filenames.add(fileB);
                    }
                }
            }
        }

        return filenames;
    }


    private Map<String, Integer> fetchComplexitiesForFiles(List<String> filenames) {
        Map<String, Integer> complexities = new HashMap<>();
        for (String filename : filenames) {
            String complexityUrl = complexityBaseUrl + filename;
            String response = getFromSonarCloud(complexityUrl);
            try {
                JsonNode complexityJson = objectMapper.readTree(response);
                int complexity = extractComplexity(complexityJson);
                complexities.put(filename, complexity);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return complexities;
    }

    private int extractComplexity(JsonNode complexityJson) {
        int complexity = 0;
        JsonNode components = complexityJson.path("components");
        
        if (components.isArray()) {
            for (JsonNode component : components) {
                JsonNode measures = component.path("measures");
                if (measures.isArray()) {
                    for (JsonNode measure : measures) {
                        if (measure.path("metric").asText().equals("complexity")) {
                            complexity = measure.path("value").asInt();
                            return complexity;  // Assuming you want to return the first complexity found
                        }
                    }
                }
            }
        }
        
        return complexity;
    }
    
    private double calculateAverageComplexity(Map<String, Integer> complexities) {
        int totalComplexity = 0;
        int numberOfFiles = complexities.size();

        for (int complexity : complexities.values()) {
            totalComplexity += complexity;
        }

        return numberOfFiles == 0 ? 0 : (double) totalComplexity / numberOfFiles;
    }
}
//
//
//}

