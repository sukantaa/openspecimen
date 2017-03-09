package com.krishagni.catissueplus.core.administrative.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.krishagni.catissueplus.core.administrative.events.AutoFreezerProviderErrorCode;
import com.krishagni.catissueplus.core.administrative.services.AutomatedFreezer;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public class AutoFreezerProvider extends BaseEntity {
	private static final Log logger = LogFactory.getLog(AutoFreezerProvider.class);

	private static final Map<String, Class<AutomatedFreezer>> providersClassMap = new WeakHashMap<>();

	private String name;

	private String implClass;

	private Map<String, String> props = new HashMap<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getImplClass() {
		return implClass;
	}

	public void setImplClass(String implClass) {
		this.implClass = implClass;
	}

	public Map<String, String> getProps() {
		return props;
	}

	public void setProps(Map<String, String> props) {
		this.props = props;
	}

	public void update(AutoFreezerProvider provider) {
		setName(provider.getName());
		setImplClass(provider.getImplClass());
		setProps(provider.getProps());
	}

	public AutomatedFreezer getInstance() {
		try {
			Class<AutomatedFreezer> klass = providersClassMap.get(implClass);
			if (klass == null) {
				klass = (Class<AutomatedFreezer>) Class.forName(implClass);
				providersClassMap.put(implClass, klass);
			}

			return klass.getConstructor().newInstance();
		} catch (Exception e) {
			logger.error("Error obtaining an instance of automated freezer", e);
			throw OpenSpecimenException.userError(AutoFreezerProviderErrorCode.INVALID_CLASS, name, e.getMessage());
		}
	}
}
