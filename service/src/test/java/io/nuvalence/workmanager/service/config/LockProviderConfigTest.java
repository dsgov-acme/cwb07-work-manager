package io.nuvalence.workmanager.service.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;

@ExtendWith(MockitoExtension.class)
class LockProviderConfigTest {

    @Mock private DataSource mockDataSource;

    private LockProviderConfig lockProviderConfig;

    @BeforeEach
    void setUp() {
        lockProviderConfig = new LockProviderConfig();
    }

    @Test
    void testLockProviderBeanCreation() {
        LockProvider lockProvider = lockProviderConfig.lockProvider(mockDataSource);

        assertNotNull(lockProvider, "LockProvider bean should not be null");
        assertTrue(
                lockProvider instanceof JdbcTemplateLockProvider,
                "LockProvider should be an instance of JdbcTemplateLockProvider");
    }
}
