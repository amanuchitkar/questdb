package io.questdb.test;

import io.questdb.*;
import io.questdb.cutlass.json.JsonException;
import io.questdb.log.Log;
import io.questdb.log.LogFactory;
import io.questdb.mp.SOCountDownLatch;
import io.questdb.std.Os;
import io.questdb.test.tools.TestUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class ReloadingPropServerConfigurationTest {
    @ClassRule
    public static final TemporaryFolder temp = new TemporaryFolder();
    protected static final Log LOG = LogFactory.getLog(ReloadingPropServerConfigurationTest.class);
    protected static String root;

    @AfterClass
    public static void afterClass() {
        TestUtils.removeTestPath(root);
    }

    @BeforeClass
    public static void setupMimeTypes() throws IOException {
        File root = new File(temp.getRoot(), "root");
        TestUtils.copyMimeTypes(root.getAbsolutePath());
        ReloadingPropServerConfigurationTest.root = root.getAbsolutePath();
    }


    @Test
    public void testSimpleReload() throws Exception {
        Properties properties = new Properties();
        ReloadingPropServerConfiguration configuration = newReloadingPropServerConfiguration(root, properties, null, new BuildInformationHolder());
        Assert.assertEquals(4, configuration.getHttpServerConfiguration().getHttpContextConfiguration().getConnectionPoolInitialCapacity());

        properties.setProperty(String.valueOf(PropertyKey.HTTP_CONNECTION_POOL_INITIAL_CAPACITY), "99");
        Assert.assertEquals("99", properties.getProperty(String.valueOf(PropertyKey.HTTP_CONNECTION_POOL_INITIAL_CAPACITY)));
        configuration.reload(properties, null);
        Assert.assertEquals(99, configuration.getHttpServerConfiguration().getHttpContextConfiguration().getConnectionPoolInitialCapacity());
    }

    @Test
    public void testConcurrentReload() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(String.valueOf(PropertyKey.HTTP_CONNECTION_POOL_INITIAL_CAPACITY), "99");
        ReloadingPropServerConfiguration configuration = newReloadingPropServerConfiguration(root, properties, null, new BuildInformationHolder());

        int concurrencyLevel = 4;
        AtomicInteger values = new AtomicInteger(10);

        CyclicBarrier startBarrier = new CyclicBarrier(concurrencyLevel + 1);
        CyclicBarrier valueBarrier = new CyclicBarrier(concurrencyLevel + 1);
        SOCountDownLatch endLatch = new SOCountDownLatch(concurrencyLevel);

        new Thread(() -> {
            TestUtils.await(startBarrier);
            while (values.get() >= 0) {
                properties.setProperty(String.valueOf(PropertyKey.HTTP_CONNECTION_POOL_INITIAL_CAPACITY), Integer.toString(values.decrementAndGet()));
                configuration.reload(properties, null);
                TestUtils.await(valueBarrier);
            }
        }).start();

        for (int i = 0; i < concurrencyLevel; i++) {
            new Thread(() -> {
                TestUtils.await(startBarrier);
                try {
                    while (true) {
                        if (configuration.getHttpServerConfiguration().getHttpContextConfiguration().getConnectionPoolInitialCapacity() == values.get()) {
                            TestUtils.await(valueBarrier);
                        }
                        Os.pause();
                        if (values.get() == -1) {
                            break;
                        }
                    }
                }
                finally {
                    endLatch.countDown();
                }
            }).start();
        }
        endLatch.await();
    }

    @NotNull
    protected ReloadingPropServerConfiguration newReloadingPropServerConfiguration(
            String root,
            Properties properties,
            @Nullable Map<String, String> env,
            BuildInformation buildInformation
    ) throws ServerConfigurationException, JsonException {
        return new ReloadingPropServerConfiguration(root, properties, env, ReloadingPropServerConfigurationTest.LOG, buildInformation);
    }

}
