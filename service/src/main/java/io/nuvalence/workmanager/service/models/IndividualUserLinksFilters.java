package io.nuvalence.workmanager.service.models;

import io.nuvalence.workmanager.service.domain.profile.IndividualUserLink;
import io.nuvalence.workmanager.service.domain.profile.ProfileAccessLevel;
import io.nuvalence.workmanager.service.usermanagementapi.UserManagementService;
import io.nuvalence.workmanager.service.usermanagementapi.models.UserDTO;
import io.nuvalence.workmanager.service.utils.UserUtility;
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

@Getter
public class IndividualUserLinksFilters extends BaseFilters {

    private final UUID userId;
    private final String email;
    private final String name;

    private final UserManagementService userManagementService;

    @SuppressWarnings("java:S107")
    @Builder
    public IndividualUserLinksFilters(
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize,
            UUID userId,
            String email,
            String name,
            UserManagementService userManagementService) {
        super(sortBy, sortOrder, pageNumber, pageSize);
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.userManagementService = userManagementService;
    }

    public Specification<IndividualUserLink> getIndividualLinksSpecification() {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<>();

            if (StringUtils.isNotBlank(this.name)) {
                List<UserDTO> users = userManagementService.getUsers(this.name, "");
                userIsPresent(root, criteriaBuilder, predicates, users);
            }

            if (this.userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), this.userId.toString()));
            }

            if (StringUtils.isNotBlank(this.email)) {
                List<UserDTO> users = userManagementService.getUsers("", this.email);
                userIsPresent(root, criteriaBuilder, predicates, users);
            }

            if (UserUtility.getAuthenticatedUserType().equals("public")) {
                predicates.add(
                        criteriaBuilder.notEqual(
                                root.get("accessLevel"), ProfileAccessLevel.AGENCY_READONLY));
            }
            return !predicates.isEmpty()
                    ? criteriaBuilder.or(predicates.toArray(new Predicate[0]))
                    : null;
        };
    }

    private void userIsPresent(
            Root<IndividualUserLink> root,
            CriteriaBuilder criteriaBuilder,
            ArrayList<Object> predicates,
            List<UserDTO> users) {
        if (!users.isEmpty()) {
            List<String> userIds = new ArrayList<>();
            users.forEach(userDTO -> userIds.add(userDTO.getId().toString()));
            predicates.add(criteriaBuilder.in(root.get("userId")).value(userIds));
        }
    }
}
