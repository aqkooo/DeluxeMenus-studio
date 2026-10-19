package com.ejyqyl.dmstudio;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AppStartupTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
        Platform.setImplicitExit(false);
    }

    @Test
    void testAppStartupAndSceneInitialization() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                App app = new App();
                Stage stage = new Stage();
                app.start(stage);

                assertThat(App.getPrimaryStage()).isNotNull();
                assertThat(stage.getScene()).isNotNull();
                assertThat(stage.getTitle()).isNotNull();
                assertThat(stage.getScene().getRoot()).isNotNull();

                stage.close();
                success.set(true);
            } catch (Throwable t) {
                failure.set(t);
                t.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        if (failure.get() != null) {
            throw new AssertionError("App failed to start", failure.get());
        }
        assertThat(finished).isTrue();
        assertThat(success.get()).isTrue();
    }
}
