package com.rowingclub.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "membership_plan_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipPlanType {

    @EmbeddedId
    private MembershipPlanTypeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("planId")
    @JoinColumn(name = "plan_id")
    private MembershipPlan plan;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("membershipTypeId")
    @JoinColumn(name = "membership_type_id")
    private MembershipType membershipType;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class MembershipPlanTypeId implements Serializable {

        @Column(name = "plan_id")
        private UUID planId;

        @Column(name = "membership_type_id")
        private UUID membershipTypeId;
    }
}