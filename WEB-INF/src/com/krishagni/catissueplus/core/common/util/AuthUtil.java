package com.krishagni.catissueplus.core.common.util;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import com.krishagni.auth.Util;
import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.User;

public class AuthUtil {
	public static Authentication getAuth() {
		return SecurityContextHolder.getContext().getAuthentication();
	}
	
	public static User getCurrentUser() {
		return getAuth() == null ? null : (User) getAuth().getPrincipal();
	}

	public static Institute getCurrentUserInstitute() {
		User user = getCurrentUser();
		return (user != null) ? user.getInstitute() : null;
	}
	
	public static String getRemoteAddr() {
		Authentication auth = getAuth();
		if (auth == null) {
			return null;
		}
		
		Object obj = auth.getDetails();
		if (obj instanceof WebAuthenticationDetails) {
			WebAuthenticationDetails details = (WebAuthenticationDetails)obj;
			return details.getRemoteAddress();
		}
		
		return null;
	}

	public static void setCurrentUser(User user) {
		setCurrentUser(user, null, null);
	}

	public static void setCurrentUser(User user, String authToken, HttpServletRequest httpReq) {
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user, authToken, user.getAuthorities());
		if (httpReq != null) {
			token.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpReq));
		}

		SecurityContextHolder.getContext().setAuthentication(token);
	}

	public static void clearCurrentUser() {
		SecurityContextHolder.clearContext();
	}
	
	public static boolean isAdmin() {
		return getCurrentUser() != null && getCurrentUser().isAdmin();
	}
	
	public static boolean isInstituteAdmin() {
		return getCurrentUser() != null && getCurrentUser().isInstituteAdmin();
	}
	
	public static String encodeToken(String token) {
		return Util.encodeToken(token);
	}
	
	public static String decodeToken(String token) {
		return Util.decodeToken(token);
	}
	
	public static String getTokenFromCookie(HttpServletRequest httpReq) {
		return Util.getTokenFromCookie(httpReq, "osAuthToken");
	}
}