package com.ejyqyl.dmstudio.service.i18n;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class I18nCompletenessTest {

    @Test
    void testRuAndEnKeyParity() throws Exception {
        Properties ruProps = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/i18n/messages_ru.properties")) {
            assertNotNull(is, "messages_ru.properties must exist");
            ruProps.load(new InputStreamReader(is, StandardCharsets.UTF_8));
        }

        Properties enProps = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/i18n/messages_en.properties")) {
            assertNotNull(is, "messages_en.properties must exist");
            enProps.load(new InputStreamReader(is, StandardCharsets.UTF_8));
        }

        Set<String> ruKeys = ruProps.stringPropertyNames();
        Set<String> enKeys = enProps.stringPropertyNames();

        for (String key : ruKeys) {
            assertTrue(enKeys.contains(key), "Key missing in EN properties: " + key);
        }

        for (String key : enKeys) {
            assertTrue(ruKeys.contains(key), "Key missing in RU properties: " + key);
        }
    }
}
