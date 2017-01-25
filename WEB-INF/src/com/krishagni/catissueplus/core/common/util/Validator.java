package com.krishagni.catissueplus.core.common.util;

import java.util.List;

import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.commons.errors.ParameterizedError;

public interface Validator {
	boolean supports(Class<?> klass);
	
	List<ParameterizedError> validate(Object target);
	
	boolean validate(Object target, OpenSpecimenException ose);
}
