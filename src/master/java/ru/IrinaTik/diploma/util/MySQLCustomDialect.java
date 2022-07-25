package ru.IrinaTik.diploma.util;

import org.hibernate.dialect.MySQL5Dialect;

/*
    Переопределение диалекта, чтобы избежать ошибок типа:
    java.sql.SQLException: Incorrect string value: \xF0\x9F\x98\x8D for column 'content'
    Используется в application.properties для свойства spring.jpa.properties.hibernate.dialect
    Подробнее https://dev.mysql.com/doc/refman/5.6/en/charset-unicode-utf8mb4.html
*/

public class MySQLCustomDialect extends MySQL5Dialect {

    @Override
    public String getTableTypeString() {
        return " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE = utf8mb4_general_ci";
    }
}
