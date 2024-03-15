package io.nuvalence.workmanager.service.service.vertexai;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.preview.ContentMaker;
import com.google.cloud.vertexai.generativeai.preview.GenerativeModel;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
@Profile("!test")
public class VertexAIClient {

    private static final String MODEL = "gemini-pro";

    @org.springframework.beans.factory.annotation.Value("${spring.cloud.gcp.project-id}")
    private String gcpProjectId;

    @org.springframework.beans.factory.annotation.Value("${spring.cloud.gcp.vertex-ai.location}")
    private String vertexAILocation;

    public Optional<String> askVertexAI(String question) throws IOException {
        try (VertexAI vertexAI = new VertexAI(gcpProjectId, vertexAILocation)) {

            GenerativeModel model = new GenerativeModel(MODEL, vertexAI);
            GenerateContentResponse response =
                    model.generateContent(ContentMaker.fromString(question));

            if (response.getCandidatesCount() > 0) {
                Content responseContent = response.getCandidates(0).getContent();
                if (responseContent.getPartsCount() > 0) {
                    return Optional.of(responseContent.getParts(0).getText());
                }
            }
            return Optional.empty();
        }
    }
}
