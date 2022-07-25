package ru.IrinaTik.diploma;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.IrinaTik.diploma.service.PageService;

@SpringBootApplication
public class Main implements CommandLineRunner {

    @Autowired
    private PageService pageService;

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Override
    public void run(String... args) {
        pageService.getSiteMap();
        System.exit(0);
    }
}
