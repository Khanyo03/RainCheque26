package com.raincheque.repository;

import com.raincheque.entity.Event;
import com.raincheque.entity.Venue;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for building dynamic Event queries with optional filters.
 */
public final class EventSpecifications {

    private EventSpecifications() {
        // Utility class - prevent instantiation
    }

    /**
     * Builds a combined specification from optional filter parameters.
     * All non-null parameters are applied conjunctively (AND).
     *
     * @param status   optional event status filter
     * @param category optional category filter (exact match)
     * @param keyword  optional keyword for case-insensitive search across title, description, and venue name
     * @return a Specification that combines all non-null filters
     */
    public static Specification<Event> withFilters(Event.EventStatus status, String category, String keyword) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (category != null && !category.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                Join<Event, Venue> venueJoin = root.join("venue", JoinType.LEFT);

                Predicate titleMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")), pattern);
                Predicate descriptionMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")), pattern);
                Predicate venueNameMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(venueJoin.get("name")), pattern);

                predicates.add(criteriaBuilder.or(titleMatch, descriptionMatch, venueNameMatch));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Specification that filters events by status.
     */
    public static Specification<Event> hasStatus(Event.EventStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }

    /**
     * Specification that filters events by category (exact match).
     */
    public static Specification<Event> hasCategory(String category) {
        return (root, query, criteriaBuilder) ->
                (category == null || category.isBlank()) ? null : criteriaBuilder.equal(root.get("category"), category);
    }

    /**
     * Specification that searches events by keyword across title, description, and venue name.
     * Case-insensitive LIKE matching.
     */
    public static Specification<Event> containsKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            Join<Event, Venue> venueJoin = root.join("venue", JoinType.LEFT);

            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(venueJoin.get("name")), pattern)
            );
        };
    }
}
