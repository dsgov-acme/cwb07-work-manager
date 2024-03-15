package io.nuvalence.workmanager.service.domain.profile;

import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "related_party")
@IdClass(RelatedPartyId.class)
public class RelatedParty {

    @Id
    @Column(name = "profile_type")
    @Enumerated(EnumType.STRING)
    private ProfileType type;

    @Id
    @Column(name = "profile_id")
    private UUID profileId;

    @ManyToOne
    @JoinColumn(name = "transaction_id_additional_parties")
    private Transaction transactionAdditionalParties;
}
