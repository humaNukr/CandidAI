package ua.edu.ukma.candidai.common.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import ua.edu.ukma.candidai.common.testcontainers.TestcontainersConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@Sql(scripts = "/sql/reset-data.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public abstract class BrowserTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(BrowserTestSupport.class);

    private static Playwright playwright;
    private static Browser browser;

    @LocalServerPort
    protected int port;

    protected BrowserContext context;
    protected Page page;

    @RegisterExtension
    protected final TestWatcher failureWatcher = new TestWatcher() {
        @Override
        public void testFailed(ExtensionContext extContext, Throwable cause) {
            Path reportDir = Paths.get("build", "reports", "playwright");
            try {
                Files.createDirectories(reportDir);
            } catch (Exception ex) {
                LOG.warn("Failed to create report dir: {}", ex.getMessage());
            }
            String testName = extContext.getRequiredTestMethod().getName();
            try {
                if (page != null) {
                    page.screenshot(new Page.ScreenshotOptions()
                            .setPath(reportDir.resolve(testName + "-failure.png")));
                }
            } catch (Exception ex) {
                LOG.warn("Failed to take screenshot: {}", ex.getMessage());
            }
            try {
                if (context != null) {
                    context.tracing().stop(new Tracing.StopOptions()
                            .setPath(reportDir.resolve(testName + "-trace.zip")));
                }
            } catch (Exception ex) {
                LOG.warn("Failed to stop tracing: {}", ex.getMessage());
            }
        }

        @Override
        public void testSuccessful(ExtensionContext extContext) {
            try {
                context.tracing().stop(new Tracing.StopOptions());
            } catch (Exception ex) {
                LOG.warn("Failed to stop Playwright tracing: {}", ex.getMessage());
            }
        }
    };

    @BeforeAll
    static void initBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void destroyBrowser() {
        if (browser != null) {
            browser.close();
            browser = null;
        }
        if (playwright != null) {
            playwright.close();
            playwright = null;
        }
    }

    @BeforeEach
    void initContext() {
        context = browser.newContext(new Browser.NewContextOptions().setLocale("uk-UA"));
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));
        page = context.newPage();
    }

    @AfterEach
    void destroyContext() {
        if (context != null) {
            context.close();
            context = null;
        }
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }
}
