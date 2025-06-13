package com.vethaa.dao;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MigrationDao<T> {

	@Autowired
	SessionFactory sessionFactory;
	
	@Transactional
	public void save(T productionSection) {
		System.out.println("test");
		sessionFactory.getCurrentSession().persist(productionSection);
	}
}
