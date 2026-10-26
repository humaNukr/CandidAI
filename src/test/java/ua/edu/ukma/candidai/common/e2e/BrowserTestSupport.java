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
                String testName = extContext.getRequiredTestMethod().getName();
                if (page != null) {
                    page.screenshot(new Page.ScreenshotOptions()
                            .setPath(reportDir.resolve(testName + "-failure.png")));
                }
            } catch (Exception ex) {
                LOG.warn("Failed to take failure screenshot: {}", ex.getMessage());
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
    void destroyContext(org.junit.jupiter.api.TestInfo testInfo) {
        if (context != null) {
            try {
                Path reportDir = Paths.get("build", "reports", "playwright");
                Files.createDirectories(reportDir);
                String testName = testInfo.getTestMethod()
                        .map(java.lang.reflect.Method::getName)
                        .orElse("test");
                context.tracing().stop(new Tracing.StopOptions()
                        .setPath(reportDir.resolve(testName + "-trace.zip")));
            } catch (Exception ex) {
                LOG.warn("Failed to stop Playwright tracing: {}", ex.getMessage());
            }
            context.close();
            context = null;
        }
    }

    protected void captureScreenshot(String filename) {
        try {
            Path dir = Paths.get("docs", "screenshots", "e2e");
            Files.createDirectories(dir);
            page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve(filename)));
        } catch (Exception ex) {
            LOG.warn("Failed to capture screenshot {}: {}", filename, ex.getMessage());
        }
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }
}
