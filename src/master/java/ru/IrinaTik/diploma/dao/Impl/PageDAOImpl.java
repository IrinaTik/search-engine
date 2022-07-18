package ru.IrinaTik.diploma.dao.Impl;

import org.hibernate.Session;
import org.hibernate.Transaction;
import ru.IrinaTik.diploma.dao.PageDAO;
import ru.IrinaTik.diploma.entity.Page;
import ru.IrinaTik.diploma.util.HibernateSessionUtil;

public class PageDAOImpl implements PageDAO {

    @Override
    public void save(Page page) {
        Session session = HibernateSessionUtil.getNewSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            session.save(page);
            transaction.commit();
        } catch (Exception ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            ex.printStackTrace();
        } finally {
            session.close();
        }
    }

}
