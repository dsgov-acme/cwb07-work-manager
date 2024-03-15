package io.nuvalence.workmanager.service.domain.customerprovideddocument;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

/**
 * ID class for rejection reason composite key.
 */
@Data
public class RejectionReasonId implements Serializable {
    private static final long serialVersionUID = -9158081738362781234L;
    private UUID customerProvidedDocumentId;
    private RejectionReasonType rejectionReasonValue;
}
