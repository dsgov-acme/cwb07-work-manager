package io.nuvalence.workmanager.service.utils.testutils;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class StringDateUtils {
    public static Matcher<String> areStringDateAndOffsetDateTimeEqual(
            OffsetDateTime expectedTimestamp) {
        return new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String actualTimestampStr) {
                DateTimeFormatter formatter =
                        new DateTimeFormatterBuilder()
                                .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                .appendOffsetId()
                                .toFormatter();

                OffsetDateTime actualTimestamp =
                        OffsetDateTime.parse(actualTimestampStr, formatter);
                return actualTimestamp.equals(expectedTimestamp);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Expected and actual dates are different");
            }
        };
    }
}
