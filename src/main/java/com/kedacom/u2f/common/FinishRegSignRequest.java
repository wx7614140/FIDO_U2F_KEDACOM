/**
* Filename : TestFile.java
* Author : zhangkai
* Creation time : 2018年9月15日下午1:06:06 
* Description :
*/
package com.kedacom.u2f.common;

public class FinishRegSignRequest {
	
	private String username;
	private int errorCode;
	private String challenge;
	private String tokenResponse;
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public int getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
	public String getChallenge() {
		return challenge;
	}
	public void setChallenge(String challenge) {
		this.challenge = challenge;
	}
	public String getTokenResponse() {
		return tokenResponse;
	}
	public void setTokenResponse(String tokenResponse) {
		this.tokenResponse = tokenResponse;
	}

}
