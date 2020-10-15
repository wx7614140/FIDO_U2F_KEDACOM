/**
* Filename : TestFile.java
* Author : zhangkai
* Creation time : 2018年9月14日下午1:06:06
* Description :
*/
package com.kedacom.u2f.users;

import java.util.List;

import com.kedacom.u2f.data.CredentialRegistration;
import com.kedacom.u2f.common.UserListNode;

public interface IUserStore {

	boolean addUser(String username, String pwd);

	boolean removeUser(String username);

	boolean modifyPassword(String username, String pwd);

	boolean checkUser(String username, String pwd);

	boolean ifUserExists(String username);

	List<CredentialRegistration> getUserRegInfo(String username);

	boolean addUserRegInfo(String username,String keyhandle, String reginfojson);

	boolean delUserRegInfo(String username,String keyhandle);

	List<UserListNode> getUserList(String username); //if admin,return all;if not admin,just return himself.


}
