package io.nuvalence.workmanager.service.models.auditevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Data for an audit event regarding customer provided document status changed.
 */
@Data
@AllArgsConstructor
@Slf4j
public class DocumentStatusChangedAuditEventDto {
    private String documentId;
    private String transactionId;
    private String documentFieldPath;
    private List<String> rejectedReasons;

    /**
     * Builder for TransactionFilters.
     *
     * @return the object converted to JSON.
     */
    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            return null;
        }
    }
}
