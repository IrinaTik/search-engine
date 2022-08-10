package ru.IrinaTik.diploma;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.IrinaTik.diploma.service.LemmaService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LemmaServiceTest {

    private static final String TEXT = "Повторное появление леопарда в Осетии позволяет предположить, что " +
            "леопард постоянно обитает в некоторых районах Северного Кавказа.";

    private static Map<String, Integer> expected;

    @BeforeAll
    public static void before() {
        expected = new HashMap<>();
        expected.put("повторный", 1);
        expected.put("появление", 1);
        expected.put("постоянно", 1);
        expected.put("позволять", 1);
        expected.put("предположить", 1);
        expected.put("северный", 1);
        expected.put("район", 1);
        expected.put("кавказ", 1);
        expected.put("осетия", 1);
        expected.put("леопард", 2);
        expected.put("обитать", 1);
    }

    @Test
    @DisplayName("Получение лемм из текста")
    public void collectLemmasTest() {
        LemmaService lemmaService = new LemmaService();
        Map<String, Integer> lemmas = lemmaService.getLemmas(TEXT);
        assertEquals(expected, lemmas);
    }
}
