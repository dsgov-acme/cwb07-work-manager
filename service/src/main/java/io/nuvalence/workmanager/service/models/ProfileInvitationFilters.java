package io.nuvalence.workmanager.service.models;

import io.nuvalence.workmanager.service.domain.profile.ProfileAccessLevel;
import io.nuvalence.workmanager.service.domain.profile.ProfileInvitation;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

@Getter
public class ProfileInvitationFilters extends BaseFilters {

    private final String accessLevel;
    private final String email;
    private final Boolean exactEmailMatch;
    private final String type;
    private final UUID profileId;

    @SuppressWarnings("java:S107")
    @Builder
    public ProfileInvitationFilters(
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize,
            String accessLevel,
            String email,
            Boolean exactEmailMatch,
            UUID profileId,
            String type) {
        super(sortBy, sortOrder, pageNumber, pageSize);
        this.accessLevel = accessLevel;
        this.email = email;
        this.exactEmailMatch = exactEmailMatch;
        this.type = type;
        this.profileId = profileId;
    }

    public Specification<ProfileInvitation> getProfileInvitationSpecification() {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<>();

            if (StringUtils.isNotBlank(this.accessLevel)) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("accessLevel"),
                                ProfileAccessLevel.fromValue(this.accessLevel)));
            }

            if (StringUtils.isNotBlank(this.email) && Boolean.TRUE.equals(this.exactEmailMatch)) {
                predicates.add(criteriaBuilder.equal(root.get("email"), this.email));
            }

            if (StringUtils.isNotBlank(this.email) && Boolean.FALSE.equals(this.exactEmailMatch)) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("email")),
                                email.toLowerCase(Locale.ROOT) + "%"));
            }

            if (StringUtils.isNotBlank(String.valueOf(this.profileId))) {
                predicates.add(criteriaBuilder.equal(root.get("profileId"), this.profileId));
            }

            if (StringUtils.isNotBlank(this.type)) {
                predicates.add(
                        criteriaBuilder.equal(root.get("type"), ProfileType.fromValue(this.type)));
            }

            return !predicates.isEmpty()
                    ? criteriaBuilder.and(predicates.toArray(new Predicate[0]))
                    : null;
        };
    }
}
