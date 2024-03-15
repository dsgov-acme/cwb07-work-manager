package io.nuvalence.workmanager.service.audit.util;

import io.nuvalence.workmanager.service.domain.profile.Address;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class to centralize common logic related to sending audit events.
 */
public class AuditMapManagementUtility {

    private AuditMapManagementUtility() {
        throw new AssertionError(
                "Utility class should not be instantiated, use the static methods.");
    }

    /**
     * Removes the same items between the before state and the after state to only be left with what changes.
     *
     * @param before previous state Map
     * @param after new state Map
     */
    public static void removeCommonItems(Map<String, String> before, Map<String, String> after) {
        HashMap<String, String> copyBefore = new HashMap<>(before);
        removeEntriesHelper(before, after);
        removeEntriesHelper(after, copyBefore);
    }

    public static Map<String, String> convertAddressToMap(String mapPrefix, Address address) {
        Map<String, String> map = new HashMap<>();

        if (address == null) {
            return map;
        }

        map.put(mapPrefix + "address1", address.getAddress1());
        map.put(mapPrefix + "address2", address.getAddress2());
        map.put(mapPrefix + "city", address.getCity());
        map.put(mapPrefix + "state", address.getState());
        map.put(mapPrefix + "postalCode", address.getPostalCode());
        map.put(mapPrefix + "country", address.getCountry());
        map.put(mapPrefix + "county", address.getCounty());

        return map;
    }

    private static void removeEntriesHelper(
            Map<String, String> mapToProcess, Map<String, String> mapToCompare) {
        mapToProcess
                .entrySet()
                .removeIf(
                        entry ->
                                entry.getValue() == null
                                        || entry.getValue().isBlank()
                                        || (mapToCompare.containsKey(entry.getKey())
                                                && Objects.equals(
                                                        mapToCompare.get(entry.getKey()),
                                                        entry.getValue())));
    }
}
