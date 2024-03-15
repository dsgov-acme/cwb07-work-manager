package io.nuvalence.workmanager.service.models;

import io.nuvalence.workmanager.service.domain.profile.Employer;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Locale;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Getter
public class EmployerFilters extends BaseFilters {

    private final String fein;
    private final String name;
    private final String type;
    private final String industry;

    @SuppressWarnings("java:S107")
    @Builder
    public EmployerFilters(
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize,
            String fein,
            String name,
            String type,
            String industry) {
        super(sortBy, sortOrder, pageNumber, pageSize);
        this.fein = fein;
        this.name = name;
        this.type = type;
        this.industry = industry;
    }

    public Specification<Employer> getEmployerProfileSpecification() {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<>();

            if (StringUtils.isNotBlank(this.name)) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("legalName")),
                                "%" + this.name.toLowerCase(Locale.ROOT) + "%"));

                Subquery<String> otherNamesSubquery = query.subquery(String.class);
                Root<Employer> subqueryRoot = otherNamesSubquery.correlate(root);
                Join<Employer, String> otherNamesJoin =
                        subqueryRoot.join("otherNames", JoinType.LEFT);
                otherNamesSubquery.select(otherNamesJoin);
                otherNamesSubquery.where(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(otherNamesJoin),
                                "%" + this.name.toLowerCase(Locale.ROOT) + "%"));
                predicates.add(criteriaBuilder.exists(otherNamesSubquery));
            }

            if (StringUtils.isNotBlank(this.fein)) {
                predicates.add(criteriaBuilder.equal(root.get("fein"), this.fein));
            }

            if (StringUtils.isNotBlank(this.type)) {
                predicates.add(criteriaBuilder.equal(root.get("type"), this.type));
            }

            if (StringUtils.isNotBlank(this.industry)) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("industry")),
                                "%" + this.industry.toLowerCase(Locale.ROOT) + "%"));
            }

            return !predicates.isEmpty()
                    ? criteriaBuilder.or(predicates.toArray(new Predicate[0]))
                    : null;
        };
    }
}
