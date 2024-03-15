package io.nuvalence.workmanager.service.models;

import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.usermanagementapi.UserManagementService;
import io.nuvalence.workmanager.service.usermanagementapi.models.UserDTO;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Filters for individual profiles.
 */
@Getter
public class IndividualFilters extends BaseFilters {

    private final UUID ownerUserId;
    private final String ssn;
    private final String email;
    private final String name;

    private final UserManagementService userManagementService;

    @SuppressWarnings("java:S107")
    @Builder
    public IndividualFilters(
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize,
            UUID ownerUserId,
            String ssn,
            String email,
            String name,
            UserManagementService userManagementService) {
        super(sortBy, sortOrder, pageNumber, pageSize);
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.ssn = ssn;
        this.email = email;
        this.userManagementService = userManagementService;
    }

    public Specification<Individual> getIndividualProfileSpecification() {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<>();

            if (StringUtils.isNotBlank(this.name)) {
                List<UserDTO> users = userManagementService.getUsers(this.name, "");
                userIsPresent(root, criteriaBuilder, predicates, users);
            }

            if (StringUtils.isNotBlank(ssn)) {
                predicates.add(criteriaBuilder.equal(root.get("ssn"), this.ssn));
            }

            if (this.ownerUserId != null) {
                predicates.add(criteriaBuilder.equal(root.get("ownerUserId"), this.ownerUserId));
            }

            if (StringUtils.isNotBlank(this.email)) {
                List<UserDTO> users = userManagementService.getUsers("", this.email);
                userIsPresent(root, criteriaBuilder, predicates, users);
            }
            return !predicates.isEmpty()
                    ? criteriaBuilder.or(predicates.toArray(new Predicate[0]))
                    : null;
        };
    }

    private void userIsPresent(
            Root<Individual> root,
            CriteriaBuilder criteriaBuilder,
            ArrayList<Object> predicates,
            List<UserDTO> users) {
        if (!users.isEmpty()) {
            List<UUID> userIds = new ArrayList<>();
            users.forEach(userDTO -> userIds.add(userDTO.getId()));
            predicates.add(criteriaBuilder.in(root.get("ownerUserId")).value(userIds));
        }
    }
}
