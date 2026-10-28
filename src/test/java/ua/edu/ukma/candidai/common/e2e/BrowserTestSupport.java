package ua.edu.ukma.candidai.common.e2e;

import com.github.tomakehurst.wiremock.WireMockServer;
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
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import ua.edu.ukma.candidai.common.wiremock.ExternalServicesStubConfiguration;

public abstract class BrowserTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(BrowserTestSupport.class);
    private static final Path REPORT_DIR = Paths.get("build", "reports", "playwright");

    private static Playwright playwright;
    private static Browser browser;

    @LocalServerPort
    protected int port;

    @Autowired
    protected WireMockServer wireMockServer;

    protected BrowserContext context;
    protected Page page;

    @RegisterExtension
    protected final AfterTestExecutionCallback failureArtifactCallback = this::recordArtifactsOnFailure;

    private boolean tracingSaved;

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
        wireMockServer.resetAll();
        ExternalServicesStubConfiguration.setupDefaultStubs(wireMockServer);

        context = browser.newContext(new Browser.NewContextOptions()
                .setBaseURL("http://localhost:" + port)
                .setLocale("uk-UA"));
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));
        page = context.newPage();
        tracingSaved = false;
    }

    @AfterEach
    void destroyContext() {
        if (context != null) {
            if (!tracingSaved) {
                stopTracingQuietly();
            }
            context.close();
            context = null;
        }
    }

    private void recordArtifactsOnFailure(ExtensionContext extContext) {
        if (extContext.getExecutionException().isEmpty()) {
            return;
        }
        createReportDirectory();
        String testName = extContext.getRequiredTestClass().getSimpleName()
                + "-" + extContext.getRequiredTestMethod().getName();
        saveFailureScreenshot(testName);
        saveFailureTrace(testName);
        tracingSaved = true;
    }

    private void createReportDirectory() {
        try {
            Files.createDirectories(REPORT_DIR);
        } catch (Exception ex) {
            LOG.warn("Failed to create Playwright report directory: {}", ex.getMessage());
        }
    }

    private void saveFailureScreenshot(String testName) {
        if (page == null) {
            return;
        }
        try {
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(REPORT_DIR.resolve(testName + "-failure.png"))
                    .setFullPage(true));
        } catch (Exception ex) {
            LOG.warn("Failed to capture failure screenshot: {}", ex.getMessage());
        }
    }

    private void saveFailureTrace(String testName) {
        if (context == null) {
            return;
        }
        try {
            context.tracing().stop(new Tracing.StopOptions()
                    .setPath(REPORT_DIR.resolve(testName + "-trace.zip")));
        } catch (Exception ex) {
            LOG.warn("Failed to save Playwright trace: {}", ex.getMessage());
        }
    }

    private void stopTracingQuietly() {
        try {
            context.tracing().stop();
        } catch (Exception ex) {
            LOG.warn("Failed to stop Playwright tracing: {}", ex.getMessage());
        }
    }
}
