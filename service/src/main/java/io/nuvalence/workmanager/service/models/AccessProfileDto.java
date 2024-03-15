package io.nuvalence.workmanager.service.models;

import io.nuvalence.workmanager.service.domain.profile.ProfileAccessLevel;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Data
@Slf4j
@Builder
public class AccessProfileDto {
    private UUID
            id; // refers to the profile id (profiles are being moved to tokens and user-management,
    // when so if this object is needed may be changed to profileId)
    private ProfileAccessLevel level;
    private ProfileType type;
}
