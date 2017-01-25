package com.krishagni.catissueplus.rest.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.GenericFilterBean;

import com.krishagni.auth.domain.AuthToken;
import com.krishagni.auth.domain.LoginAuditLog;
import com.krishagni.auth.domain.UserApiCallLog;
import com.krishagni.auth.events.LoginDetail;
import com.krishagni.auth.events.TokenDetail;
import com.krishagni.auth.repository.AuthDaoFactory;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.auth.services.UserAuthServiceWrapper;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.AuthUtil;

public class AuthTokenFilter extends GenericFilterBean {
	private static final String OS_AUTH_TOKEN_HDR = "X-OS-API-TOKEN";
	
	private static final String OS_CLIENT_HDR = "X-OS-API-CLIENT";
	
	private static final String BASIC_AUTH = "Basic ";
	
	private static final String DEFAULT_AUTH_DOMAIN = "openspecimen";

	private UserAuthServiceWrapper authService;

	private AuthDaoFactory authDaoFactory;

	private Map<String, List<String>> excludeUrls = new HashMap<>();

	public UserAuthServiceWrapper getAuthService() {
		return authService;
	}

	public void setAuthService(UserAuthServiceWrapper authService) {
		this.authService = authService;
	}

	public AuthDaoFactory getAuthDaoFactory() {
		return authDaoFactory;
	}

	public void setAuthDaoFactory(AuthDaoFactory authDaoFactory) {
		this.authDaoFactory = authDaoFactory;
	}

	public Map<String, List<String>> getExcludeUrls() {
		return excludeUrls;
	}

	public void setExcludeUrls(Map<String, List<String>> excludeUrls) {
		this.excludeUrls = excludeUrls;
	}

	public void addExcludeUrl(String method, String resourceUrl) {
		List<String> urls = excludeUrls.get(method);
		if (urls == null) {
			urls = new ArrayList<String>();
			excludeUrls.put(method, urls);
		}

		if (urls.indexOf(resourceUrl) == -1) {
			urls.add(resourceUrl);
		}
	}

	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
	throws IOException, ServletException {
		if (!(req instanceof HttpServletRequest)) {
			throw new IllegalAccessError("Unknown protocol request");
		}
		
		HttpServletRequest httpReq = (HttpServletRequest)req;
		HttpServletResponse httpResp = (HttpServletResponse)resp;
		
		httpResp.setHeader("Access-Control-Allow-Origin", "http://localhost:9000");
		httpResp.setHeader("Access-Control-Allow-Credentials", "true");
		httpResp.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, PATCH, OPTIONS");
		httpResp.setHeader("Access-Control-Allow-Headers", "Origin, Accept, Content-Type, X-OS-API-TOKEN, X-OS-API-CLIENT");

		httpResp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		httpResp.setHeader("Pragma", "no-cache");
		httpResp.setDateHeader("Expires", 0);

		if (httpReq.getMethod().equalsIgnoreCase("options")) {
			httpResp.setStatus(HttpServletResponse.SC_OK);	
			return;
		}

		List<String> urls = excludeUrls.get(httpReq.getMethod());
		if (urls == null) {
			urls = Collections.emptyList();
		}

		for (String url : urls) {
			if (matches(httpReq, url)) {
				chain.doFilter(req, resp);
				return;
			}
		}

		User userDetails = null;
		String authToken = httpReq.getHeader(OS_AUTH_TOKEN_HDR);
		if (authToken == null) {
			authToken = AuthUtil.getTokenFromCookie(httpReq);
		}
		
		LoginAuditLog loginAuditLog = null;
		if (authToken != null) {
			TokenDetail tokenDetail = new TokenDetail();
			tokenDetail.setToken(authToken);
			tokenDetail.setIpAddress(httpReq.getRemoteAddr());			
			
			RequestEvent<TokenDetail> atReq = new RequestEvent<TokenDetail>(tokenDetail);			
			ResponseEvent<AuthToken> atResp = authService.validateToken(atReq);
			if (atResp.isSuccessful()) {
				userDetails = (User) atResp.getPayload().getUser();
				loginAuditLog = atResp.getPayload().getLoginAuditLog();
			}
		} else if(httpReq.getHeader(HttpHeaders.AUTHORIZATION) != null) {
			userDetails = doBasicAuthentication(httpReq, httpResp);
		}
		
		if (userDetails == null) {
			String clientHdr = httpReq.getHeader(OS_CLIENT_HDR);
			if (clientHdr != null && clientHdr.equals("webui")) {
				setUnauthorizedResp(httpResp);
			} else {
				setRequireAuthResp(httpResp);
			}
			return;
		}

		AuthUtil.setCurrentUser(userDetails, authToken, httpReq);
		Date callStartTime = Calendar.getInstance().getTime();
		chain.doFilter(req, resp);
		AuthUtil.clearCurrentUser();
	
		UserApiCallLog apiCallLog = new UserApiCallLog();
		apiCallLog.setUser(userDetails);
		apiCallLog.setUrl(httpReq.getRequestURI());
		apiCallLog.setMethod(httpReq.getMethod());
		apiCallLog.setCallStartTime(callStartTime);
		apiCallLog.setCallEndTime(Calendar.getInstance().getTime());
		apiCallLog.setResponseCode(Integer.toString(httpResp.getStatus()));
		apiCallLog.setLoginAuditLog(loginAuditLog);
		saveApiCallLog(apiCallLog);
	}
	
	private User doBasicAuthentication(HttpServletRequest httpReq, HttpServletResponse httpResp) throws UnsupportedEncodingException {
		String header = httpReq.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith(BASIC_AUTH)) {
			return null;
		}

		byte[] base64Token = header.substring(BASIC_AUTH.length()).getBytes();
		String[] parts = new String(Base64.decode(base64Token)).split(":");
		if (parts.length != 2) {
			return null;
		}

		LoginDetail detail = new LoginDetail();
		detail.setLoginName(parts[0]);
		detail.setPassword(parts[1]);
		detail.setIpAddress(httpReq.getRemoteAddr());
		detail.setDomainName(DEFAULT_AUTH_DOMAIN);
		detail.setDoNotGenerateToken(true);

		RequestEvent<LoginDetail> req = new RequestEvent<>(detail);
		ResponseEvent<Map<String, Object>> resp = authService.authenticateUser(req);
		if (resp.isSuccessful()) {
			return (User)resp.getPayload().get("user");
		}

		return null;
	}

	private void setRequireAuthResp(HttpServletResponse httpResp) throws IOException {
		httpResp.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"OpenSpecimen\"");
		setUnauthorizedResp(httpResp);
	}

	private void setUnauthorizedResp(HttpServletResponse httpResp) throws IOException {
		httpResp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
				"You must supply valid credentials to access the OpenSpecimen REST API");
	}

	private boolean matches(HttpServletRequest httpReq, String url) {
		if (!url.startsWith("/**")) {
			String prefix = "/**";
			if (!url.startsWith("/")) {
				prefix += "/";
			}

			url = prefix + url;
		}

		return new AntPathRequestMatcher(url, httpReq.getMethod(), true).matches(httpReq);
	}

	@PlusTransactional
	private void saveApiCallLog(UserApiCallLog callLog) {
		authDaoFactory.getUserApiCallLogDao().saveOrUpdate(callLog);
	}
}
