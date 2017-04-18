package com.krishagni.catissueplus.core.common;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class AttributeModifiedSupport implements Serializable {
	private static final long serialVersionUID = -6538288756745006122L;

	private Set<String> modifiedAttrs = new HashSet<String>();
	
	public void attrModified(String attr) {
		modifiedAttrs.add(attr);
	}
	
	public boolean isAttrModified(String attr) {
		return modifiedAttrs.contains(attr);
	}
}
