package io.nuvalence.workmanager.service.domain.profile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RelatedPartyId implements Serializable {

    private static final long serialVersionUID = -708532041950258527L;

    private ProfileType type;
    private UUID profileId;
}
