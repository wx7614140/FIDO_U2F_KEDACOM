/**
* Filename : TestFile.java
* Author : zhangkai
* Creation time : 2018年9月14日下午1:06:06
* Description :
*/
package com.kedacom.u2f.common;

import com.kedacom.u2f.users.User;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
	private UserInfo(Builder builder) {
		this.username = builder.username;
		this.password = builder.password;
		this.displayName = builder.displayName;
		this.credentialNickname = builder.credentialNickname;
		this.sessionToken = builder.sessionToken;
		this.requireResidentKey = builder.requireResidentKey;
	}

	public static class Builder {
		private String username;
		private String password;
		private String displayName;
		private String credentialNickname;
		private String sessionToken;
		private boolean requireResidentKey;

		public Builder withUser(User user){
			this.username = user.getUsername();
			this.password = user.getPassword();
			return this;
		}

		public Builder withDisplayName(String displayName){
			this.displayName = displayName;
			return this;
		}
		public Builder withCredentialNickname(String credentialNickname){
			this.credentialNickname = credentialNickname;
			return this;
		}
		public Builder withSessionToken(String sessionToken){
			this.sessionToken = sessionToken;
			return this;
		}

		public Builder withRequireResidentKey(boolean requireResidentKey){
			this.requireResidentKey = requireResidentKey;
			return this;
		}
		public UserInfo build(){
			return new UserInfo(this);
		}
	}
}
