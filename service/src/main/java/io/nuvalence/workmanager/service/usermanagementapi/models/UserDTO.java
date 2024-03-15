package io.nuvalence.workmanager.service.usermanagementapi.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Generated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Generated
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@Builder
@Jacksonized
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private UUID id;

    private String firstName;

    private String middleName;

    private String lastName;

    private String displayName;

    private String email;
}
