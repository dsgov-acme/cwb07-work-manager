package io.nuvalence.workmanager.service.domain.dynamicschema.attributes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represents a custom dynamic schema attribute for a Document.
 */
@Getter
@Setter
@Builder
@ToString
public class Document implements Serializable {

    private static final long serialVersionUID = -5467558151323449097L;
    private final UUID documentId;
    private final String filename;

    /**
     * Constructor documentId.
     *
     * @param documentId documentId
     * @param filename user reported filename of document
     */
    @JsonCreator
    public Document(
            @JsonProperty("documentId") UUID documentId,
            @JsonProperty("filename") String filename) {
        this.documentId = documentId;
        this.filename = filename;
    }
}
