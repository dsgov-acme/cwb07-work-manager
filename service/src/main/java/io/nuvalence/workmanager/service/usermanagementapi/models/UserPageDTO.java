package io.nuvalence.workmanager.service.usermanagementapi.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.nuvalence.workmanager.service.generated.models.PagingMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Generated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Generated
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserPageDTO {
    private List<UserDTO> users;

    private PagingMetadata pagingMetadata;
}
