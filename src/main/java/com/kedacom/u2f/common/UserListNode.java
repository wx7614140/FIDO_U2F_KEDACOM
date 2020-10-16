/**
* Filename : TestFile.java
* Author : zhangkai
* Creation time : 2018年9月14日下午1:06:06
* Description :
*/
package com.kedacom.u2f.common;

import com.kedacom.u2f.data.CredentialRegistration;
import com.yubico.webauthn.data.ByteArray;

import java.util.Map;

public class UserListNode {

	private UserInfo userinfo;
	private Map<ByteArray,CredentialRegistration> userreginfo;

	public UserInfo getUserinfo() {
		return userinfo;
	}
	public void setUserinfo(UserInfo userinfo) {
		this.userinfo = userinfo;
	}
	public Map<ByteArray, CredentialRegistration> getUserreginfo() {
		return userreginfo;
	}
	public void setUserreginfo(Map<ByteArray, CredentialRegistration> userreginfo) {
		this.userreginfo = userreginfo;
	}



}
