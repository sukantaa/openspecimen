package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.services.ContainerSelectionRule;

@Configurable
public class ContainerTypeRestriction implements ContainerSelectionRule {

	@Autowired
	private SessionFactory sessionFactory;

	@Override
	public String getName() {
		return "ContainerType";
	}

	@Override
	public String getSql(String containerTabAlias, Map<String, Object> params) {
		return String.format(SQL_TMPL, containerTabAlias, getTypeName(params));
	}

	@Override
	public Criterion getRestriction(String containerObjAlias, Map<String, Object> params) {
		String typeName = getTypeName(params);

		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(StorageContainer.class, "container")
			.setProjection(Projections.distinct(Projections.property("container.id")));
		Criteria query = detachedCriteria.getExecutableCriteria(sessionFactory.getCurrentSession());
		query.createAlias("type", "type")
			.add(Restrictions.eq("type.name", typeName))
			.add(Restrictions.eq("container.storeSpecimenEnabled", true));
		return Subqueries.propertyIn(containerObjAlias + ".id", detachedCriteria);
	}

	@Override
	public boolean eval(StorageContainer container, Map<String, Object> params) {
		return container.getType() != null && container.getType().getName().equals(getTypeName(params));
	}

	private String getTypeName(Map<String, Object> params) {
		Object value = params.get("name");
		if (!(value instanceof String)) {
			throw new IllegalArgumentException("Invalid container type name");
		}

		return (String) value;
	}

	private static final String SQL_TMPL = "%s.identifier in ( " +
		"select " +
		"  c.identifier " +
		"from " +
		"  os_storage_containers c " +
		"  inner join os_container_types t on t.identifier = c.type_id " +
		"where" +
		"  t.name = '%s' and " +
		"  c.store_specimens = 1 and " +
		"  c.activity_status != 'Disabled')";
}
