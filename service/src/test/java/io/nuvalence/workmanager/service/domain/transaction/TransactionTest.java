package io.nuvalence.workmanager.service.domain.transaction;

import io.nuvalence.workmanager.service.domain.customerprovideddocument.CustomerProvidedDocument;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.ReviewStatus;
import io.nuvalence.workmanager.service.domain.profile.RelatedParty;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.apache.commons.beanutils.BasicDynaBean;
import org.apache.commons.beanutils.BasicDynaClass;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaClass;
import org.apache.commons.beanutils.DynaProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
class TransactionTest {

    @Test
    void equalsHashcodeContract() {
        // we shouldn't need to mock this, since we tell EqualsVerifier to ignore data, but there
        // appears to be a bug.
        final CustomerProvidedDocument blueDocument =
                CustomerProvidedDocument.builder()
                        .id(UUID.randomUUID())
                        .active(true)
                        .reviewStatus(ReviewStatus.NEW)
                        .dataPath("document")
                        .rejectionReasons(new ArrayList<>())
                        .build();
        final CustomerProvidedDocument redDocument =
                CustomerProvidedDocument.builder()
                        .id(UUID.randomUUID())
                        .active(true)
                        .reviewStatus(ReviewStatus.NEW)
                        .dataPath("document2")
                        .rejectionReasons(new ArrayList<>())
                        .build();

        final TransactionDefinition transactionDefinition1 =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .name("test1")
                        .key("test1")
                        .description("test1")
                        .build();
        final TransactionDefinition transactionDefinition2 =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .name("test2")
                        .key("test2")
                        .description("test2")
                        .build();

        final BasicDynaClass redDynaClass =
                new BasicDynaClass(
                        "redschema",
                        BasicDynaBean.class,
                        List.of(
                                        new DynaProperty("foo", String.class),
                                        new DynaProperty("bar", List.class, String.class),
                                        new DynaProperty("baz", Integer.class))
                                .toArray(new DynaProperty[0]));
        final BasicDynaClass blueDynaClass =
                new BasicDynaClass(
                        "blueschema",
                        BasicDynaBean.class,
                        List.of(
                                        new DynaProperty("foo", String.class),
                                        new DynaProperty("baz", String.class))
                                .toArray(new DynaProperty[0]));
        final BasicDynaBean redDynaBean = new BasicDynaBean(redDynaClass);
        redDynaBean.set("foo", "foo");
        redDynaBean.set("bar", List.of("bar"));
        redDynaBean.set("baz", 42);
        final BasicDynaBean blueDynaBean = new BasicDynaBean(redDynaClass);
        blueDynaBean.set("foo", "foo");
        blueDynaBean.set("bar", List.of("baz"));

        RelatedParty relatedParty1 = new RelatedParty(); // Set properties as needed
        RelatedParty relatedParty2 = new RelatedParty(); // Ensure different from relatedParty1

        // Create lists of RelatedParty if needed
        List<RelatedParty> relatedParties1 = Arrays.asList(relatedParty1);
        List<RelatedParty> relatedParties2 = Arrays.asList(relatedParty2);

        EqualsVerifier.forClass(Transaction.class)
                .withPrefabValues(DynaClass.class, redDynaClass, blueDynaClass)
                .withPrefabValues(DynaBean.class, redDynaBean, blueDynaBean)
                .withPrefabValues(CustomerProvidedDocument.class, blueDocument, redDocument)
                .withPrefabValues(
                        TransactionDefinition.class, transactionDefinition1, transactionDefinition2)
                .withPrefabValues(RelatedParty.class, relatedParty1, relatedParty2)
                .withPrefabValues(
                        List.class,
                        relatedParties1,
                        relatedParties2) // Add if Transaction has List<RelatedParty>
                .usingGetClass()
                .verify();
    }
}
