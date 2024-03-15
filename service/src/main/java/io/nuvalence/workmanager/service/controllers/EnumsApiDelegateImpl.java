package io.nuvalence.workmanager.service.controllers;

import io.nuvalence.workmanager.service.domain.ApplicationEnum;
import io.nuvalence.workmanager.service.domain.ApplicationEnumRanked;
import io.nuvalence.workmanager.service.domain.NoteType;
import io.nuvalence.workmanager.service.domain.TransactionRecordLinkType;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.RejectionReasonType;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.ReviewStatus;
import io.nuvalence.workmanager.service.domain.dynamicschema.attributes.SupportedTypes;
import io.nuvalence.workmanager.service.domain.profile.ProfileAccessLevel;
import io.nuvalence.workmanager.service.domain.transaction.TransactionPriority;
import io.nuvalence.workmanager.service.domain.transaction.UserType;
import io.nuvalence.workmanager.service.generated.controllers.EnumerationsApiDelegate;
import io.nuvalence.workmanager.service.generated.models.EnumerationResponseModelInner;
import io.nuvalence.workmanager.service.generated.models.EnumsModel;
import io.nuvalence.workmanager.service.generated.models.RankedEnumsModel;
import io.nuvalence.workmanager.service.service.NoteService;
import io.nuvalence.workmanager.service.utils.UserUtility;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the EnumsApiDelegate interface.
 */
@Service
@AllArgsConstructor
public class EnumsApiDelegateImpl implements EnumerationsApiDelegate {

    private static final Map<String, Class<? extends Enum<? extends ApplicationEnum>>> ENUMS_MAP =
            new HashMap<>();

    private static final String NOTE_TYPES_KEY = "note-types";
    private NoteService noteService;

    static {
        ENUMS_MAP.put("document-review-statuses", ReviewStatus.class);
        ENUMS_MAP.put("document-rejection-reasons", RejectionReasonType.class);
        ENUMS_MAP.put("transaction-priorities", TransactionPriority.class);
        ENUMS_MAP.put("schema-attribute-types", SupportedTypes.class);
        ENUMS_MAP.put("user-types", UserType.class);
        ENUMS_MAP.put("transaction-record-link-types", TransactionRecordLinkType.class);
        ENUMS_MAP.put("profile-access-levels", ProfileAccessLevel.class);
    }

    @Override
    public ResponseEntity<Map<String, List<EnumerationResponseModelInner>>> getEnumerations() {

        Map<String, List<EnumerationResponseModelInner>> enumsResponse = new HashMap<>();
        ENUMS_MAP.forEach(
                (key, enumClass) -> enumsResponse.put(key, buildEnumResponseModel(enumClass)));

        enumsResponse.put(NOTE_TYPES_KEY, buildNoteTypes());

        return ResponseEntity.ok(enumsResponse);
    }

    @Override
    public ResponseEntity<List<EnumerationResponseModelInner>> getEnumerationsById(
            String enumerationId) {
        if (enumerationId.equals(NOTE_TYPES_KEY)) {
            return ResponseEntity.ok(buildNoteTypes());
        }
        Class<? extends Enum<? extends ApplicationEnum>> enumClass = ENUMS_MAP.get(enumerationId);
        if (enumClass == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(buildEnumResponseModel(enumClass));
    }

    private List<EnumerationResponseModelInner> buildEnumResponseModel(
            Class<? extends Enum<? extends ApplicationEnum>> enumClass) {

        List<EnumerationResponseModelInner> enumValues = new ArrayList<>();
        String authenticatedUserType = UserUtility.getAuthenticatedUserType();

        for (Enum<? extends ApplicationEnum> enumConstant : enumClass.getEnumConstants()) {

            var appEnum = (ApplicationEnum) enumConstant;
            if (!appEnum.isHiddenFromApi()
                    && !(authenticatedUserType.equalsIgnoreCase("public")
                            && appEnum.isHiddenForPublicUsers())) {

                EnumerationResponseModelInner enumsModel =
                        enumConstant instanceof ApplicationEnumRanked
                                ? getRankedEnumModel((ApplicationEnumRanked) appEnum)
                                : getEnumModel(appEnum);

                enumValues.add(enumsModel);
            }
        }
        return enumValues;
    }

    private EnumsModel getEnumModel(ApplicationEnum appEnum) {
        EnumsModel enumsModel = new EnumsModel();

        enumsModel.setLabel(appEnum.getLabel());
        enumsModel.setValue(appEnum.getValue());

        return enumsModel;
    }

    private RankedEnumsModel getRankedEnumModel(ApplicationEnumRanked appEnum) {
        RankedEnumsModel enumsModel = new RankedEnumsModel();

        enumsModel.setLabel(appEnum.getLabel());
        enumsModel.setValue(appEnum.getValue());
        enumsModel.setRank(appEnum.getRank());

        return enumsModel;
    }

    private List<EnumerationResponseModelInner> buildNoteTypes() {
        List<EnumerationResponseModelInner> enumValues = new ArrayList<>();

        List<NoteType> noteTypes = noteService.getAllNoteTypes();
        for (NoteType noteType : noteTypes) {
            if (!noteType.isHiddenFromApi()) {
                enumValues.add(getEnumModel(noteType));
            }
        }
        return enumValues;
    }
}
