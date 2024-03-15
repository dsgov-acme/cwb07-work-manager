package io.nuvalence.workmanager.service.models.auditevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Data for an audit event regarding transaction submitted.
 */
@Data
@AllArgsConstructor
@Slf4j
public class TransactionSubmittedAuditEventDto {

    private final String userId;

    /**
     * Converts object to JSON string.
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
