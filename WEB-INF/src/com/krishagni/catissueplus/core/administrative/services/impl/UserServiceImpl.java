
package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.opensaml.saml2.core.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;

import com.krishagni.catissueplus.core.administrative.domain.ForgotPasswordToken;
import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.UserEvent;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserFactory;
import com.krishagni.catissueplus.core.administrative.events.AnnouncementDetail;
import com.krishagni.catissueplus.core.administrative.events.InstituteDetail;
import com.krishagni.catissueplus.core.administrative.events.PasswordDetails;
import com.krishagni.catissueplus.core.administrative.events.UserDetail;
import com.krishagni.catissueplus.core.administrative.repository.UserDao;
import com.krishagni.catissueplus.core.administrative.repository.UserListCriteria;
import com.krishagni.catissueplus.core.administrative.services.UserService;
import com.krishagni.catissueplus.core.auth.domain.AuthDomain;
import com.krishagni.catissueplus.core.auth.domain.AuthErrorCode;
import com.krishagni.catissueplus.core.auth.domain.LoginAuditLog;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.domain.Notification;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.BulkEntityDetail;
import com.krishagni.catissueplus.core.common.events.DeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.service.EmailService;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.service.impl.EventPublisher;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.NotifUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.rbac.events.SubjectRoleDetail;
import com.krishagni.rbac.events.SubjectRoleOpNotif;
import com.krishagni.rbac.service.RbacService;

public class UserServiceImpl implements UserService, ObjectAccessor, InitializingBean, UserDetailsService, SAMLUserDetailsService {
	private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

	private static final String DEFAULT_AUTH_DOMAIN = "openspecimen";
	
	private static final String FORGOT_PASSWORD_EMAIL_TMPL = "users_forgot_password_link"; 
	
	private static final String PASSWD_CHANGED_EMAIL_TMPL = "users_passwd_changed";
	
	private static final String SIGNED_UP_EMAIL_TMPL = "users_signed_up";
	
	private static final String NEW_USER_REQUEST_EMAIL_TMPL = "users_new_user_request";
	
	private static final String USER_CREATED_EMAIL_TMPL = "users_created";

	private static final String ANNOUNCEMENT_EMAIL_TMPL = "announcement_email";

	private static final String USER_OP_EMAIL_TMPL = "users_op_notif";

	private static final String ADMIN_MOD = "administrative";

	private static final String ACTIVE_USER_LOGIN_DAYS_CFG = "active_users_login_days";

	private static final String USER_SIGN_UP = "user_sign_up";

	private static final String AUTH_MOD = "auth";

	private static final String FORGOT_PASSWD = "forgot_password";

	private static final Map<String, String> NOTIF_OPS = new HashMap<String, String>() {{
		put("created",  "CREATE");
		put("deleted",  "DELETE");
		put("rejected", "DELETE");
		put("unlocked", "UPDATE");
		put("approved", "UPDATE");
		put("locked",   "UPDATE");
	}};

	private DaoFactory daoFactory;

	private UserFactory userFactory;
	
	private EmailService emailService;
	
	private RbacService rbacSvc;

	private ExportService exportSvc;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setUserFactory(UserFactory userFactory) {
		this.userFactory = userFactory;
	}
	
	public void setEmailService(EmailService emailService) {
		this.emailService = emailService;
	}

	public void setRbacSvc(RbacService rbacSvc) {
		this.rbacSvc = rbacSvc;
	}

	public void setExportSvc(ExportService exportSvc) {
		this.exportSvc = exportSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<UserSummary>> getUsers(RequestEvent<UserListCriteria> req) {
		UserListCriteria crit = req.getPayload();

		if (StringUtils.isNotBlank(crit.type())) {
			try {
				User.Type.valueOf(crit.type());
			} catch (IllegalArgumentException iae) {
				return ResponseEvent.userError(UserErrorCode.INVALID_TYPE, crit.type());
			}
		}

		List<User> users = daoFactory.getUserDao().getUsers(addUserListCriteria(crit));
		return ResponseEvent.response(UserSummary.from(users));
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<Long> getUsersCount(RequestEvent<UserListCriteria> req) {
		return ResponseEvent.response(daoFactory.getUserDao().getUsersCount(addUserListCriteria(req.getPayload())));
	}

	@Override
	public UserDetails loadUserByUsername(String username)
	throws UsernameNotFoundException {
		int slashIdx = username.indexOf('/');
		String loginName = slashIdx != -1 ? username.substring(0, slashIdx) : username;
		String domain    = slashIdx != -1 ? username.substring(slashIdx + 1) : DEFAULT_AUTH_DOMAIN;
		return daoFactory.getUserDao().getUser(loginName, domain);
	}
	
	@Override
	@PlusTransactional
	public Object loadUserBySAML(SAMLCredential credential)
	throws UsernameNotFoundException {
		if (logger.isDebugEnabled()) {
			for (Attribute attr : credential.getAttributes()) {
				logger.debug(String.format(
					"Credential attr: %s (%s) = %s",
					attr.getName(), attr.getFriendlyName(), credential.getAttributeAsString(attr.getName())));
			}
		}

		//
		// The assumption is - there can be only one SAML auth provider
		// We should perhaps use SAML local entity ID
		//
		AuthDomain domain = daoFactory.getAuthDao().getAuthDomainByType("saml");

		Map<String, String> props = domain.getAuthProvider().getProps();
		String loginNameAttr = props.get("loginNameAttr");
		String emailAttr     = props.get("emailAddressAttr");
		
		User user = null;
		if (StringUtils.isNotBlank(loginNameAttr)) {
			String loginName = getCredentialAttrValue(credential, loginNameAttr);
			user = daoFactory.getUserDao().getUser(loginName, domain.getName());
		} else if (StringUtils.isNotBlank(emailAttr)) {
			String email = getCredentialAttrValue(credential, emailAttr);
			user = daoFactory.getUserDao().getUserByEmailAddress(email);
		}
		
		if (user == null) {
			throw new UsernameNotFoundException(MessageUtil.getInstance().getMessage("user_not_found"));
		}
		
		return user;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<UserDetail> getUser(RequestEvent<Long> req) {
		User user = daoFactory.getUserDao().getById(req.getPayload());
		if (user == null) {
			return ResponseEvent.userError(UserErrorCode.NOT_FOUND);
		}
		
		return ResponseEvent.response(UserDetail.from(user));
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<UserDetail> createUser(RequestEvent<UserDetail> req) {
		try {
			boolean isSignupReq = (AuthUtil.getCurrentUser() == null);

			UserDetail detail = req.getPayload();
			if (isSignupReq) {
				ensureSignupAllowed();
				detail.setActivityStatus(Status.ACTIVITY_STATUS_PENDING.getStatus());
			}
			
			User user = userFactory.createUser(detail);
			resetAttrs(user);

			if (!isSignupReq) {
				AccessCtrlMgr.getInstance().ensureCreateUserRights(user);
			}
		
			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			ensureUniqueLoginNameInDomain(user.getLoginName(), user.getAuthDomain().getName(), ose);
			ensureUniqueEmailAddress(user.getEmailAddress(), ose);
			ose.checkAndThrow();

			daoFactory.getUserDao().saveOrUpdate(user);
			
			if (user.isInstituteAdmin()) {
				addDefaultSiteAdminRole(user, "CREATE");
			}

			if (isSignupReq) {
				sendUserSignupEmail(user);
				notifyUserSignup(user);
			} else {
				ForgotPasswordToken token = generateForgotPwdToken(user);
				sendUserCreatedEmail(user, token);
				notifyUserUpdated(user, "created");
			}

			return ResponseEvent.response(UserDetail.from(user));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<UserDetail> updateUser(RequestEvent<UserDetail> req) {
		return updateUser(req, false);
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<UserDetail> patchUser(RequestEvent<UserDetail> req) {
		return updateUser(req, true);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<UserDetail> updateStatus(RequestEvent<UserDetail> req) {
		try {
			UserDetail detail = req.getPayload();
			User user =  daoFactory.getUserDao().getById(detail.getId());
			if (user == null) {
				return ResponseEvent.userError(UserErrorCode.NOT_FOUND);
			}
			
			String currentStatus = user.getActivityStatus();
			String newStatus = detail.getActivityStatus();
			if (currentStatus.equals(newStatus)) {
				return ResponseEvent.response(UserDetail.from(user));
			}
			
			AccessCtrlMgr.getInstance().ensureUpdateUserRights(user);
			
			if (!isStatusChangeAllowed(newStatus)) {
				return ResponseEvent.userError(UserErrorCode.STATUS_CHANGE_NOT_ALLOWED);
			}

			if (isActivated(currentStatus, newStatus)) {
				user.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
				onAccountActivation(user, currentStatus);
			} else if (isLocked(currentStatus, newStatus)) {
				user.setActivityStatus(Status.ACTIVITY_STATUS_LOCKED.getStatus());
				notifyUserUpdated(user, "locked");
			} else if (isDeleted(currentStatus, newStatus)) {
				user.delete();
				notifyUserUpdated(user, "deleted");
			}

			return ResponseEvent.response(UserDetail.from(user));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch(Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<DependentEntityDetail>> getDependentEntities(RequestEvent<Long> req) {
		try {
			User existing = daoFactory.getUserDao().getById(req.getPayload());
			if (existing == null) {
				return ResponseEvent.userError(UserErrorCode.NOT_FOUND);
			}
			
			return ResponseEvent.response(existing.getDependentEntities());
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<UserDetail> deleteUser(RequestEvent<DeleteEntityOp> req) {
		try {
			DeleteEntityOp deleteEntityOp = req.getPayload();
			User existing =  daoFactory.getUserDao().getById(deleteEntityOp.getId());
			if (existing == null) {
				return ResponseEvent.userError(UserErrorCode.NOT_FOUND);
			}

			AccessCtrlMgr.getInstance().ensureDeleteUserRights(existing);

			/*
			 * Appending timestamp to email address, loginName of user while deleting user.
			 * To send request reject mail, need original user object.
			 * So creating user object clone.
			 */
			User user = new User();
			user.setId(existing.getId());
			user.setActivityStatus(existing.getActivityStatus());
			user.update(existing);
			existing.delete();

			boolean sendRequestRejectedMail = user.getActivityStatus().equals(Status.ACTIVITY_STATUS_PENDING.getStatus());
			if (sendRequestRejectedMail) {
				notifyUserUpdated(user, "rejected");
			} else {
				notifyUserUpdated(user, "deleted");
			}

			return ResponseEvent.response(UserDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> changePassword(RequestEvent<PasswordDetails> req) {
		try {
			PasswordDetails detail = req.getPayload();
			User user = daoFactory.getUserDao().getById(detail.getUserId());
			User currentUser = AuthUtil.getCurrentUser();

			if (user == null) {
				return ResponseEvent.userError(UserErrorCode.NOT_FOUND);
			}
			
			if (!user.getAuthDomain().getName().equals(DEFAULT_AUTH_DOMAIN)) {
				return ResponseEvent.userError(UserErrorCode.PASSWD_CHANGE_NOT_ALLOWED, user.getAuthDomain().getName());
			}

			if (currentUser.equals(user)) {
				if (!user.isValidOldPassword(detail.getOldPassword())) {
					return ResponseEvent.userError(UserErrorCode.INVALID_OLD_PASSWD);
				}

			} else if (!currentUser.isAdmin()) {
				return ResponseEvent.userError(UserErrorCode.PERMISSION_DENIED);
			}
			user.changePassword(detail.getNewPassword());
			daoFactory.getUserDao().saveOrUpdate(user);
			sendPasswdChangedEmail(user);
			return ResponseEvent.response(true);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> resetPassword(RequestEvent<PasswordDetails> req) {
		try {
			UserDao dao = daoFactory.getUserDao();
			PasswordDetails detail = req.getPayload();
			if (StringUtils.isEmpty(detail.getResetPasswordToken())) {
				return ResponseEvent.userError(UserErrorCode.INVALID_PASSWD_TOKEN);
			}
			
			ForgotPasswordToken token = dao.getFpToken(detail.getResetPasswordToken());
			if (token == null) {
				return ResponseEvent.userError(UserErrorCode.INVALID_PASSWD_TOKEN);
			}
			
			User user = token.getUser();
			if (!user.getLoginName().equals(detail.getLoginName())) {
				return ResponseEvent.userError(UserErrorCode.NOT_FOUND);
			}
			
			if (token.hasExpired()) {
				dao.deleteFpToken(token);
				return ResponseEvent.userError(UserErrorCode.INVALID_PASSWD_TOKEN, true);
			}
			
			user.changePassword(detail.getNewPassword());
			dao.deleteFpToken(token);
			sendPasswdChangedEmail(user);
			return ResponseEvent.response(true);
		}catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> forgotPassword(RequestEvent<String> req) {
		try {
			boolean forgotPasswdAllowed = ConfigUtil.getInstance().getBoolSetting(AUTH_MOD, FORGOT_PASSWD, true);
			if (!forgotPasswdAllowed) {
				throw OpenSpecimenException.userError(UserErrorCode.FORGOT_PASSWD_DISABLED);
			}

			UserDao userDao = daoFactory.getUserDao();

			User user = userDao.getUser(req.getPayload(), DEFAULT_AUTH_DOMAIN);
			if (user == null || user.isPending() || user.isClosed()) {
				return ResponseEvent.userError(UserErrorCode.NOT_FOUND_IN_OS_DOMAIN, req.getPayload());
			} else if (user.isLocked()) {
				return ResponseEvent.userError(AuthErrorCode.USER_LOCKED);
			}

			ForgotPasswordToken oldToken = userDao.getFpTokenByUser(user.getId());
			if (oldToken != null) {
				userDao.deleteFpToken(oldToken);
			}

			ForgotPasswordToken token = new ForgotPasswordToken(user);
			userDao.saveFpToken(token);
			sendForgotPasswordLinkEmail(user, token.getToken());
			return ResponseEvent.response(true);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<UserDetail>> bulkUpdateUsers(RequestEvent<BulkEntityDetail<UserDetail>> req) {
		try {
			List<UserDetail> updatedUsers = new ArrayList<>();

			BulkEntityDetail<UserDetail> buDetail = req.getPayload();
			UserDetail detail = curateBulkUpdateFields(buDetail.getDetail());
			for (Long userId : Utility.nullSafe(buDetail.getIds())) {
				detail.setId(userId);
				updatedUsers.add(updateUser(detail, true));
			}

			detail.setId(null);
			for (String emailAddress : Utility.nullSafe(buDetail.getNames())) {
				detail.setEmailAddress(emailAddress);
				updatedUsers.add(updateUser(detail, true));
			}

			return ResponseEvent.response(updatedUsers);
		} catch(OpenSpecimenException ose){
			return ResponseEvent.error(ose);
		} catch(Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SubjectRoleDetail>> getCurrentUserRoles() {
		return rbacSvc.getSubjectRoles(new RequestEvent<>(AuthUtil.getCurrentUser().getId()));
	}		
	
	@Override
	@PlusTransactional
	public ResponseEvent<InstituteDetail> getInstitute(RequestEvent<Long> req) {
		Institute institute = getInstitute(req.getPayload());
		return ResponseEvent.response(InstituteDetail.from(institute));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> broadcastAnnouncement(RequestEvent<AnnouncementDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();

			AnnouncementDetail detail = req.getPayload();
			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			ensureValidAnnouncement(detail, ose);
			ose.checkAndThrow();

			//
			// For now announcements are broadcast using emails;
			// therefore fetching only email IDs
			// Later we can broadcast using SMS, WhatsApp, Facebook, Twitter, and anything
			//
			emailAnnouncements(detail, getActiveUsersEmailIds());
			return ResponseEvent.response(true);
		} catch(OpenSpecimenException ose){
			return ResponseEvent.error(ose);
		} catch(Exception e) {
			return ResponseEvent.serverError(e);
		}
	}


	@Override
	public String getObjectName() {
		return User.getEntityName();
	}

	@Override
	public Map<String, Object> resolveUrl(String key, Object value) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public String getAuditTable() {
		return "CATISSUE_USER_AUD";
	}

	@Override
	public void ensureReadAllowed(Long id) {
		getUser(id, null);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		exportSvc.registerObjectsGenerator("user", this::getUsersGenerator);
	}

	private UserListCriteria addUserListCriteria(UserListCriteria crit) {
		if (!AuthUtil.isAdmin() && !crit.listAll()) {
			crit.instituteName(getCurrUserInstitute().getName());
		}

		return crit;
	}

	private ResponseEvent<UserDetail> updateUser(RequestEvent<UserDetail> req, boolean partial) {
		try {
			return ResponseEvent.response(updateUser(req.getPayload(), partial));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private UserDetail updateUser(UserDetail detail, boolean partial) {
		User existingUser = getUser(detail.getId(), detail.getEmailAddress());

		if (Status.ACTIVITY_STATUS_DISABLED.getStatus().equals(detail.getActivityStatus())) {
			AccessCtrlMgr.getInstance().ensureDeleteUserRights(existingUser);
		} else {
			AccessCtrlMgr.getInstance().ensureUpdateUserRights(existingUser);
		}

		User user = null;
		if (partial) {
			user = userFactory.createUser(existingUser, detail);
		} else {
			user = userFactory.createUser(detail);
		}

		resetAttrs(existingUser, user);

		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		ensureUniqueEmail(existingUser, user, ose);
		ensureUniqueLoginName(existingUser, user, ose);
		ose.checkAndThrow();

		boolean wasInstituteAdmin = existingUser.isInstituteAdmin();
		String prevStatus = existingUser.getActivityStatus();
		existingUser.update(user);

		if (isActivated(prevStatus, user.getActivityStatus())) {
			onAccountActivation(user, prevStatus);
		} else if (isLocked(prevStatus, user.getActivityStatus())) {
			notifyUserUpdated(user, "locked");
		} else if (isDeleted(prevStatus, user.getActivityStatus())) {
			notifyUserUpdated(user, "deleted");
		}

		if (!wasInstituteAdmin && existingUser.isInstituteAdmin()) {
			addDefaultSiteAdminRole(existingUser, "UPDATE");
		} else if (wasInstituteAdmin && !existingUser.isInstituteAdmin()) {
			removeDefaultSiteAdminRole(existingUser, "UPDATE");
		}

		return UserDetail.from(existingUser);
	}

	private User getUser(Long id, String emailAddress) {
		User user = null;
		Object key = null;

		if (id != null) {
			user = daoFactory.getUserDao().getById(id);
			key = id;
		} else if (StringUtils.isNotBlank(emailAddress)) {
			user = daoFactory.getUserDao().getUserByEmailAddress(emailAddress);
			key = emailAddress;
		}

		if (key == null) {
			throw OpenSpecimenException.userError(UserErrorCode.EMAIL_REQUIRED);
		} else if (user == null) {
			throw OpenSpecimenException.userError(UserErrorCode.NOT_FOUND, key);
		}

		return user;
	}

	private void addDefaultSiteAdminRole(User user, String userOp) {
		rbacSvc.addSubjectRole(null, null, user, getDefaultSiteAdminRole(), getNotifReq(user, userOp, "ADD"));
	}

	private void removeDefaultSiteAdminRole(User user, String userOp) {
		rbacSvc.removeSubjectRole(null, null, user, getDefaultSiteAdminRole(), getNotifReq(user, userOp, "REMOVE"));
	}

	private SubjectRoleOpNotif getNotifReq(User user, String userOp, String roleOp) {
		SubjectRoleOpNotif notifReq = new SubjectRoleOpNotif();
		notifReq.setEndUserOp(userOp);
		notifReq.setAdmins(AccessCtrlMgr.getInstance().getSuperAdmins());
		notifReq.setAdminNotifMsg("admin_notif_role_" + roleOp.toLowerCase());
		notifReq.setAdminNotifParams(new Object[] { user.getFirstName(), user.getLastName(), user.getInstitute().getName() });
		notifReq.setUser(user);
		notifReq.setSubjectNotifMsg("user_notif_role_" + roleOp.toLowerCase());
		notifReq.setSubjectNotifParams(new Object[] { user.getInstitute().getName() });
		return notifReq;
	}


	private String[] getDefaultSiteAdminRole() {
		return new String[] {"Administrator"};
	}

	private void sendForgotPasswordLinkEmail(User user, String token) {
		Map<String, Object> props = new HashMap<String, Object>();
		props.put("user", user);
		props.put("token", token);
		
		emailService.sendEmail(FORGOT_PASSWORD_EMAIL_TMPL, new String[]{user.getEmailAddress()}, props);
	}
	
	private void sendPasswdChangedEmail(User user) {
		Map<String, Object> props = new HashMap<String, Object>();
		props.put("user", user);
		
		emailService.sendEmail(PASSWD_CHANGED_EMAIL_TMPL, new String[]{user.getEmailAddress()}, props);
	} 
	
	private void sendUserCreatedEmail(User user, ForgotPasswordToken token) {
		Map<String, Object> props = new HashMap<>();
		props.put("user", user);
		props.put("token", token);
		props.put("ccAdmin", false);
		
		EmailUtil.getInstance().sendEmail(USER_CREATED_EMAIL_TMPL, new String[]{user.getEmailAddress()}, null, props);
	}

	private void sendUserSignupEmail(User user) {
		Map<String, Object> props = new HashMap<>();
		props.put("user", user);
		props.put("ccAdmin", false);
		
		emailService.sendEmail(SIGNED_UP_EMAIL_TMPL, new String[]{user.getEmailAddress()}, props);
	}
	
	private void notifyUserSignup(User newUser) {
		String [] subjParams = new String[] {newUser.getFirstName(), newUser.getLastName()};

		Map<String, Object> emailProps = new HashMap<>();
		emailProps.put("newUser",  newUser);
		emailProps.put("$subject", subjParams);
		emailProps.put("ccAdmin",  false);

		List<User> users = daoFactory.getUserDao().getSuperAndInstituteAdmins(newUser.getInstitute().getName());
		sendEmails(users, NEW_USER_REQUEST_EMAIL_TMPL, emailProps);

		String msg = MessageUtil.getInstance().getMessage(NEW_USER_REQUEST_EMAIL_TMPL + "_subj", subjParams);
		addNotification(newUser, users, "created", msg);
	}

	private void notifyUserUpdated(User user, String op) {
		boolean newAccountCreated = op.equals("approved") || op.equals("created");
		if (newAccountCreated) {
			EventPublisher.getInstance().publish(UserEvent.CREATED, user);
		}

		String opDesc = MessageUtil.getInstance().getMessage("users_op_" + op);
		String [] subjParams = new String[] {user.getFirstName(), user.getLastName(), opDesc};

		Map<String, Object> emailProps = new HashMap<>();
		emailProps.put("currentUser", AuthUtil.getCurrentUser());
		emailProps.put("updatedUser", user);
		emailProps.put("$subject", subjParams);
		emailProps.put("ccAdmin", false);
		emailProps.put("operation", op);

		List<User> users = daoFactory.getUserDao().getSuperAndInstituteAdmins(user.getInstitute().getName());
		if (AuthUtil.getCurrentUser() != null && !users.contains(AuthUtil.getCurrentUser())) {
			//
			// if the current user is not institute or super admin
			//
			users.add(AuthUtil.getCurrentUser());
		}

		if (!newAccountCreated && !users.contains(user)) {
			//
			// hack: for approved and created op, a separate email is sent to the user
			//
			users.add(user);
		}

		sendEmails(users, USER_OP_EMAIL_TMPL, emailProps);

		String msg = MessageUtil.getInstance().getMessage(USER_OP_EMAIL_TMPL + "_subj", subjParams);
		addNotification(user, users, op, msg);
	}

	private void sendEmails(List<User> rcpts, String emailTmpl, Map<String, Object> emailProps) {
		for (User rcpt : rcpts) {
			emailProps.put("user", rcpt);
			EmailUtil.getInstance().sendEmail(emailTmpl, new String[] {rcpt.getEmailAddress()}, null, emailProps);
		}
	}

	private void addNotification(User tgtUser, List<User> notifyUsers, String op, String message) {
		if (notifyUsers.contains(tgtUser)) {
			notifyUsers.remove(tgtUser);
		}

		User triggeredBy = AuthUtil.getCurrentUser();

		Notification notif = new Notification();
		notif.setEntityType(User.getEntityName());
		notif.setEntityId(tgtUser.getId());
		notif.setOperation(NOTIF_OPS.get(op));
		notif.setMessage(message);
		notif.setCreatedBy(triggeredBy != null ? triggeredBy : tgtUser);
		notif.setCreationTime(Calendar.getInstance().getTime());
		NotifUtil.getInstance().notify(notif, Collections.singletonMap("user-overview", notifyUsers));
	}

	private void resetAttrs(User newUser) {
		resetAttrs(null, newUser);
	}
	
	private void resetAttrs(User existingUser, User newUser) {
		if (AuthUtil.isAdmin()) {
			return;
		}

		//
		// Only super admin can update these attributes; therefore reset to
		// their earlier value or default value
		//
		newUser.setType(existingUser != null ? existingUser.getType() : User.Type.NONE);
		newUser.setManageForms(existingUser != null && existingUser.canManageForms());
	}
	
	private void ensureUniqueEmail(User existingUser, User newUser, OpenSpecimenException ose) {
		if (!existingUser.getEmailAddress().equals(newUser.getEmailAddress())) {
			ensureUniqueEmailAddress(newUser.getEmailAddress(), ose);
		}
	}

	private void ensureUniqueEmailAddress(String emailAddress, OpenSpecimenException ose) {
		if (!daoFactory.getUserDao().isUniqueEmailAddress(emailAddress)) {
			ose.addError(UserErrorCode.DUP_EMAIL);
		}
	}
	
	private void ensureUniqueLoginName(User existingUser, User newUser, OpenSpecimenException ose) {
		if (!existingUser.getLoginName().equals(newUser.getLoginName())) {
			ensureUniqueLoginNameInDomain(newUser.getLoginName(), newUser.getAuthDomain().getName(), ose);
		}
	}

	private void ensureUniqueLoginNameInDomain(String loginName, String domainName, OpenSpecimenException ose) {
		if (User.SYS_USER.equals(loginName.trim()) && User.DEFAULT_AUTH_DOMAIN.equals(domainName.trim())) {
			ose.addError(UserErrorCode.SYS_LOGIN_NAME, loginName);
			return;
		}
		
		if (!daoFactory.getUserDao().isUniqueLoginName(loginName, domainName)) {
			ose.addError(UserErrorCode.DUP_LOGIN_NAME);
		}
	}

	private void ensureSignupAllowed() {
		boolean signUpAllowed = ConfigUtil.getInstance().getBoolSetting(ADMIN_MOD, USER_SIGN_UP, false);
		if (!signUpAllowed) {
			throw OpenSpecimenException.userError(UserErrorCode.SIGN_UP_NOT_ALLOWED);
		}
	}


	private boolean isStatusChangeAllowed(String newStatus) {
		return newStatus.equals(Status.ACTIVITY_STATUS_ACTIVE.getStatus()) || 
			newStatus.equals(Status.ACTIVITY_STATUS_LOCKED.getStatus()) ||
			newStatus.equals(Status.ACTIVITY_STATUS_DISABLED.getStatus());
	}
	
	private boolean isActivated(String currentStatus, String newStatus) {
		return !currentStatus.equals(Status.ACTIVITY_STATUS_ACTIVE.getStatus()) && 
			newStatus.equals(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
	}
	
	private boolean isLocked(String currentStatus, String newStatus) {
		return currentStatus.equals(Status.ACTIVITY_STATUS_ACTIVE.getStatus()) &&
			newStatus.equals(Status.ACTIVITY_STATUS_LOCKED.getStatus());
	}

	private boolean isDeleted(String currentStatus, String newStatus) {
		return !currentStatus.equals(Status.ACTIVITY_STATUS_DISABLED.getStatus()) &&
			newStatus.equals(Status.ACTIVITY_STATUS_DISABLED.getStatus());
	}
		
	private Institute getCurrUserInstitute() {
		return getInstitute(AuthUtil.getCurrentUser().getId());
	}
	
	private Institute getInstitute(Long id) {
		User user = daoFactory.getUserDao().getById(id);
		return user.getInstitute();		
	}
	
	private ForgotPasswordToken generateForgotPwdToken(User user) {
		ForgotPasswordToken token = null;
		if (user.getAuthDomain().getName().equals(DEFAULT_AUTH_DOMAIN)) {
			token = new ForgotPasswordToken(user);
			daoFactory.getUserDao().saveFpToken(token);
		}
		return token;
	}

	private String getCredentialAttrValue(SAMLCredential credential, String attrName) {
		Attribute attr = credential.getAttributes().stream()
			.filter(a -> attrName.equals(a.getName()) || attrName.equals(a.getFriendlyName()))
			.findFirst().orElse(null);

		if (attr == null) {
			return null;
		}

		return credential.getAttributeAsString(attr.getName());
	}

	private void ensureValidAnnouncement(AnnouncementDetail detail, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getSubject())) {
			ose.addError(UserErrorCode.ANN_SUBJECT_REQ);
		}

		if (StringUtils.isBlank(detail.getMessage())) {
			ose.addError(UserErrorCode.ANN_MESSAGE_REQ);
		}
	}

	private List<String> getActiveUsersEmailIds() {
		Date today = Calendar.getInstance().getTime();
		return daoFactory.getUserDao().getActiveUsersEmailIds(getActiveUserLastLoginCutoff(today), today);
	}

	private Date getActiveUserLastLoginCutoff(Date present) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(present);
		cal.add(Calendar.DAY_OF_YEAR, -getActiveUserCfgDays());
		return Utility.chopTime(cal.getTime());
	}

	private int getActiveUserCfgDays() {
		return ConfigUtil.getInstance().getIntSetting(ADMIN_MOD, ACTIVE_USER_LOGIN_DAYS_CFG, 90);
	}

	private void emailAnnouncements(AnnouncementDetail detail, List<String> emailAddresses) {
		String[] adminEmailAddr = {ConfigUtil.getInstance().getAdminEmailId()};
		String[] rcpts = emailAddresses.toArray(new String[emailAddresses.size()]);

		Map<String, Object> props = new HashMap<>();
		props.put("user", AuthUtil.getCurrentUser());
		props.put("$subject", new String[] { detail.getSubject() });
		props.put("annDetail", detail);
		emailService.sendEmail(ANNOUNCEMENT_EMAIL_TMPL, adminEmailAddr, rcpts, null, props);
	}

	private void onAccountActivation(User user, String prevStatus) {
		if (prevStatus.equals(Status.ACTIVITY_STATUS_PENDING.getStatus())) {
			ForgotPasswordToken token = generateForgotPwdToken(user);
			sendUserCreatedEmail(user, token);
			notifyUserUpdated(user, "approved");
		} else if (prevStatus.equals(Status.ACTIVITY_STATUS_LOCKED.getStatus())) {
			addAutoLogin(user);
			notifyUserUpdated(user, "unlocked");
		}
	}

	private void addAutoLogin(User user) {
		LoginAuditLog log = new LoginAuditLog();
		log.setUser(user);
		log.setIpAddress("0.0.0.0");
		log.setLoginTime(Calendar.getInstance().getTime());
		log.setLogoutTime(log.getLoginTime());
		log.setLoginSuccessful(true);
		daoFactory.getAuthDao().saveLoginAuditLog(log);
	}

	private UserDetail curateBulkUpdateFields(UserDetail input) {
		UserDetail detail = new UserDetail();
		if (input.isAttrModified("instituteName")) {
			detail.setInstituteName(input.getInstituteName());
		}

		if (input.isAttrModified("primarySite")) {
			detail.setPrimarySite(input.getPrimarySite());
		}

		if (input.isAttrModified("type")) {
			detail.setType(input.getType());
		}

		if (input.isAttrModified("manageForms")) {
			detail.setManageForms(input.getManageForms());
		}

		if (input.isAttrModified("activityStatus")) {
			detail.setActivityStatus(input.getActivityStatus());
		}

		return detail;
	}

	private Function<ExportJob, List<? extends Object>> getUsersGenerator() {
		return new Function<ExportJob, List<? extends Object>>() {
			private boolean endOfUsers;

			private int startAt;

			private boolean paramsInited;

			@Override
			public List<? extends Object> apply(ExportJob job) {
				initParams();

				if (endOfUsers) {
					return Collections.emptyList();
				}

				UserListCriteria listCrit = addUserListCriteria(new UserListCriteria().startAt(startAt).ids(job.getRecordIds()));
				List<User> users = daoFactory.getUserDao().getUsers(listCrit);
				startAt += users.size();
				if (users.isEmpty() || CollectionUtils.isNotEmpty(job.getRecordIds())) {
					endOfUsers = true;
				}

				return UserDetail.from(users);
			}

			private void initParams() {
				if (paramsInited) {
					return;
				}

				endOfUsers = !AccessCtrlMgr.getInstance().hasUserEximRights();
				paramsInited = true;
			}
		};
	}
}
