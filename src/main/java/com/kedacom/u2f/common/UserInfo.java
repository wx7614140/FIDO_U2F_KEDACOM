/**
* Filename : TestFile.java
* Author : zhangkai
* Creation time : 2018年9月14日下午1:06:06
* Description :
*/
package com.kedacom.u2f.common;

import lombok.Data;

@Data
public class UserInfo {

	private String username;
	private String password;
	private String displayName;
	private String credentialNickname;
	private String sessionToken;
	private boolean requireResidentKey = false;

	public String getDisplayName(){
		if(displayName == null){
			return username;
		}
		return displayName;
	}
}
