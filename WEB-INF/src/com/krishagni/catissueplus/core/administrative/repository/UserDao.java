
package com.krishagni.catissueplus.core.administrative.repository;

import java.util.List;

import com.krishagni.catissueplus.core.administrative.domain.ForgotPasswordToken;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface UserDao extends Dao<User> {
	public List<UserSummary> getUsers(UserListCriteria criteria);
	
	public List<User> getUsersByIds(List<Long> userIds);
	
	public User getUser(String loginName, String domain);
	
	public User getUserByEmailAddress(String emailAddress);
	
	public Boolean isUniqueLoginName(String loginName, String domainName);
	
	public Boolean isUniqueEmailAddress(String emailAddress);
	
	public List<Object[]> getUserDependentEntities(Long userId);
	
	public ForgotPasswordToken getFpToken(String token);
	
	public ForgotPasswordToken getFpTokenByUser(Long userId);
	
	public void saveFpToken(ForgotPasswordToken token);
	
	public void deleteFpToken(ForgotPasswordToken token);
	
}
