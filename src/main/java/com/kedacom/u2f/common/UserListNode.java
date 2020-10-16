/**
* Filename : TestFile.java
* Author : zhangkai
* Creation time : 2018年9月14日下午1:06:06
* Description :
*/
package com.kedacom.u2f.common;

import com.yubico.webauthn.RegisteredCredential;

import java.util.Map;

public class UserListNode {

	private UserInfo userinfo;
	private Map<String,RegisteredCredential> userreginfo;

	public UserInfo getUserinfo() {
		return userinfo;
	}
	public void setUserinfo(UserInfo userinfo) {
		this.userinfo = userinfo;
	}
	public Map<String, RegisteredCredential> getUserreginfo() {
		return userreginfo;
	}
	public void setUserreginfo(Map<String, RegisteredCredential> userreginfo) {
		this.userreginfo = userreginfo;
	}



}
