
package com.krishagni.catissueplus.core.common.errors;

import com.krishagni.commons.errors.AppException;
import com.krishagni.commons.errors.ErrorCode;
import com.krishagni.commons.errors.ErrorType;

public class OpenSpecimenException extends AppException {
	private static final long serialVersionUID = -1473557909717365251L;
	
	public OpenSpecimenException(ErrorType type, ErrorCode error, Object ... params) {
		super(type, error, params);
	}
	
	public OpenSpecimenException(ErrorType type) {
		super(type);
	}
	
	public OpenSpecimenException(Throwable t) {
		super(t);
	}

	public OpenSpecimenException(AppException ae) {
		super(ae);
	}

	public OpenSpecimenException(Long exceptionId, Throwable t) {
		super(exceptionId, t);
	}

	public void rethrow(ErrorCode oldError, ErrorCode newError, Object ... params) {
		if (containsError(oldError)) {
			throw OpenSpecimenException.userError(newError, params);
		}
		throw this;
	}	
	
	public static OpenSpecimenException userError(ErrorCode error, Object ... params) {
		return new OpenSpecimenException(ErrorType.USER_ERROR, error, params);
	}
	
	public static OpenSpecimenException serverError(ErrorCode error, Object ... params) {
		return new OpenSpecimenException(ErrorType.SYSTEM_ERROR, error, params);
	}
	
	public static OpenSpecimenException serverError(Throwable e) {
		return new OpenSpecimenException(e);
	}
	
	public static OpenSpecimenException serverError(Long exceptionId, Throwable e) {
		return new OpenSpecimenException(exceptionId, e);
	}
}
