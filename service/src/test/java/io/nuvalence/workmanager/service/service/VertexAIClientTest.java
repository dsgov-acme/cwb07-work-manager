package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.cloud.vertexai.api.Candidate;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.preview.GenerativeModel;
import io.nuvalence.workmanager.service.service.vertexai.VertexAIClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class VertexAIClientTest {

    @Mock private GenerateContentResponse generateContentResponse;

    @Mock private Content content;

    @Mock private Candidate candidate;

    @Mock private Part part;

    @InjectMocks private VertexAIClient vertexAIClient;

    @Test
    @SuppressWarnings("unused")
    void testAskVertexAI_positive() throws IOException {

        try (MockedConstruction<GenerativeModel> mockedGenerativeModel =
                Mockito.mockConstruction(
                        GenerativeModel.class,
                        (mock, context) -> {
                            when(mock.generateContent(any(Content.class)))
                                    .thenReturn(generateContentResponse);
                        })) {

            when(generateContentResponse.getCandidatesCount()).thenReturn(1);
            when(generateContentResponse.getCandidates(0)).thenReturn(candidate);
            when(candidate.getContent()).thenReturn(content);
            when(content.getPartsCount()).thenReturn(1);
            when(content.getParts(0)).thenReturn(part);
            when(part.getText()).thenReturn("test response");

            Optional<String> result = vertexAIClient.askVertexAI("test question");

            assertEquals("test response", result.get());
        }
    }

    @Test
    @SuppressWarnings("unused")
    void testAskVertexAI_negative() throws IOException {

        try (MockedConstruction<GenerativeModel> mockedGenerativeModel =
                Mockito.mockConstruction(
                        GenerativeModel.class,
                        (mock, context) -> {
                            when(mock.generateContent(any(Content.class)))
                                    .thenReturn(generateContentResponse);
                        })) {

            when(generateContentResponse.getCandidatesCount()).thenReturn(0);

            Optional<String> result = vertexAIClient.askVertexAI("test question");

            assertEquals(Optional.empty(), result);
        }
    }
}
