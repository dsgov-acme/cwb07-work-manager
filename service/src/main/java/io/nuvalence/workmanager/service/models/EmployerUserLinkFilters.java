package io.nuvalence.workmanager.service.models;

import io.nuvalence.workmanager.service.domain.profile.EmployerUserLink;
import io.nuvalence.workmanager.service.domain.profile.ProfileAccessLevel;
import io.nuvalence.workmanager.service.utils.UserUtility;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

@Getter
public class EmployerUserLinkFilters extends BaseFilters {
    private final UUID profileId;
    private final List<UUID> userIds;

    @Builder
    public EmployerUserLinkFilters(
            UUID profileId,
            List<UUID> userIds,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {
        super(sortBy, sortOrder, pageNumber, pageSize);
        this.profileId = profileId;
        this.userIds = userIds;
    }

    public Specification<EmployerUserLink> getEmployerUserLinkSpecification() {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (profileId != null) {
                predicates.add(
                        criteriaBuilder.equal(root.get("profile").get("id"), this.profileId));
            }
            if (userIds != null) {
                predicates.add(root.get("userId").in(userIds));
            }

            if (UserUtility.getAuthenticatedUserType().equals("public")) {
                predicates.add(
                        criteriaBuilder.notEqual(
                                root.get("profileAccessLevel"),
                                ProfileAccessLevel.AGENCY_READONLY));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
