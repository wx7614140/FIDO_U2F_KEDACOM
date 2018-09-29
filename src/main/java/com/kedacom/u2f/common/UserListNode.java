/**
* Filename : TestFile.java
* Author : zhangkai
* Creation time : 2018年9月14日下午1:06:06 
* Description :
*/
package com.kedacom.u2f.common;

import java.util.Map;

public class UserListNode {
	
	private UserInfo userinfo;
	private Map<String,String> userreginfo;
	
	public UserInfo getUserinfo() {
		return userinfo;
	}
	public void setUserinfo(UserInfo userinfo) {
		this.userinfo = userinfo;
	}
	public Map<String, String> getUserreginfo() {
		return userreginfo;
	}
	public void setUserreginfo(Map<String, String> userreginfo) {
		this.userreginfo = userreginfo;
	}
	
	

}
