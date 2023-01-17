# Поисковый движок

![Java](https://img.shields.io/badge/-Java-0a0a0a?style=for-the-badge&logo=Java)
![Spring Boot](https://img.shields.io/badge/-springboot-0a0a0a?style=for-the-badge&logo=springboot)
![Spring JPA](https://img.shields.io/badge/-springjpa-0a0a0a?style=for-the-badge&logo=springjpa)
![MySQL](https://img.shields.io/badge/-MySQL-0a0a0a?style=for-the-badge&logo=MySQL)
![JSOUP](https://img.shields.io/badge/-JSOUP-0a0a0a?style=for-the-badge&logo=JSOUP)
![Lombok](https://img.shields.io/badge/-Lombok-0a0a0a?style=for-the-badge&logo=Lombok)

Проект реализует REST API для индексации и поиска по заданным сайтам.

[![WorkStatus](https://img.shields.io/badge/Status-InProgress-red.svg)](https://shields.io/)


    <properties>
        <maven.compiler.source>13</maven.compiler.source>
        <maven.compiler.target>13</maven.compiler.target>
    </properties>

<!--    <parent>-->
<!--        <groupId>org.springframework.boot</groupId>-->
<!--        <artifactId>spring-boot-starter-parent</artifactId>-->
<!--        <version>2.6.3</version>-->
<!--    </parent>-->


    <dependencies>
<!--        <dependency>-->
<!--            <groupId>org.springframework.boot</groupId>-->
<!--            <artifactId>spring-boot-starter-web</artifactId>-->
<!--        </dependency>-->
<!--        <dependency>-->
<!--            <groupId>org.springframework.boot</groupId>-->
<!--            <artifactId>spring-boot-starter-data-jpa</artifactId>-->
<!--        </dependency>-->
        <dependency>
            <groupId>org.jsoup</groupId>
            <artifactId>jsoup</artifactId>
            <version>1.13.1</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.15</version>
        </dependency>
        <dependency>
            <groupId>org.hibernate</groupId>
            <artifactId>hibernate-core</artifactId>
            <version>5.4.26.Final</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.6</version>
        </dependency>
    </dependencies>

        - url: https://www.lenta.ru
          name: Лента.ру
        - url: https://skillbox.ru
          name: Skillbox