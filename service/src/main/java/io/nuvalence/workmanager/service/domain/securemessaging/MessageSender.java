package io.nuvalence.workmanager.service.domain.securemessaging;

import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.profile.ProfileTypeConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "message_sender")
public class MessageSender {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_display_name")
    private String displayName;

    @Column(name = "user_type", nullable = false)
    private String userType;

    @Column(name = "profile_id", nullable = true)
    private UUID profileId;

    @Column(name = "profile_type", nullable = true)
    @Convert(converter = ProfileTypeConverter.class)
    private ProfileType profileType;
}
