package com.krishagni.catissueplus.core.common.service.impl;

import java.util.Calendar;

import org.hibernate.SessionFactory;
import org.hibernate.type.IntegerType;

import com.krishagni.catissueplus.core.common.service.ChangeLogService;

public class ChangeLogServiceImpl implements ChangeLogService {

	private SessionFactory sessionFactory;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public boolean doesChangeLogExists(String id, String author, String filename) {
		Number count = (Number) sessionFactory.getCurrentSession().createSQLQuery(GET_CHANGE_LOG_COUNT_SQL)
			.addScalar("cnt", IntegerType.INSTANCE)
			.setParameter("id", id)
			.setParameter("author", author)
			.setParameter("filename", filename)
			.uniqueResult();
		return count.intValue() > 0;
	}

	@Override
	public void insertChangeLog(String id, String author, String filename) {
		Number number = (Number) sessionFactory.getCurrentSession().createSQLQuery(GET_LATEST_CHANGE_LOG_ORDER_SQL)
			.addScalar("orderNo", IntegerType.INSTANCE)
			.uniqueResult();

		int orderNo = number != null ? number.intValue() : 0;
		sessionFactory.getCurrentSession().createSQLQuery(INSERT_CHANGE_LOG)
			.setParameter("id", id)
			.setParameter("author", author)
			.setParameter("filename", filename)
			.setParameter("executionDate", Calendar.getInstance().getTime())
			.setParameter("executionOrder", orderNo + 1)
			.executeUpdate();
	}

	private static final String GET_CHANGE_LOG_COUNT_SQL =
		"select " +
		"  count(*) as cnt " +
		"from " +
		"  databasechangelog " +
		"where " +
		"  id = :id and author = :author and filename = :filename";


	private static final String GET_LATEST_CHANGE_LOG_ORDER_SQL =
		"select max(orderexecuted) as orderNo from databasechangelog";

	private static final String INSERT_CHANGE_LOG =
		"insert into " +
		"  databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype) " +
		"values" +
		"  (:id, :author, :filename, :executionDate, :executionOrder, 'EXECUTED')";
}
