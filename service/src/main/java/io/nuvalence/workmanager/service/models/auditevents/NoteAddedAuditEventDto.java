package io.nuvalence.workmanager.service.models.auditevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Data for an audit event regarding transaction note creation.
 */
@Data
@AllArgsConstructor
@Slf4j
public class NoteAddedAuditEventDto {
    private String agentId;
    private String noteId;
    private String noteTitle;

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
