package com.ejyqyl.dmstudio.ui.dialog;

import com.ejyqyl.dmstudio.service.i18n.I18nService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class HeadConfigurationDialogTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    @Test
    void testHeadDialogComponentsLoad() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        HeadConfigurationDialog.class.getResource("/fxml/head_dialog.fxml"),
                        I18nService.getInstance().getBundle()
                );
                Parent root = loader.load();
                HeadConfigurationDialog controller = loader.getController();
                assertThat(controller).isNotNull();
                assertThat(root).isNotNull();
                success.set(true);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        boolean finished = latch.await(5, TimeUnit.SECONDS);
        assertThat(finished).isTrue();
        assertThat(success.get()).isTrue();
    }
}
