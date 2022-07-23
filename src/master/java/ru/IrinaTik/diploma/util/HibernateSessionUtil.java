package ru.IrinaTik.diploma.util;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class HibernateSessionUtil {

    private static SessionFactory sessionFactory = getSessionFactory();

    private HibernateSessionUtil() {
    }

    private static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                StandardServiceRegistry registry = new StandardServiceRegistryBuilder().configure("hibernate.cfg.xml").build();
                Metadata metadata = new MetadataSources(registry).getMetadataBuilder().build();
                sessionFactory = metadata.getSessionFactoryBuilder().build();
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
        return sessionFactory;
    }

    public static void stopFactory() {
        sessionFactory.close();
    }

    public static Session getNewSession() {
        return sessionFactory.openSession();
    }
}
