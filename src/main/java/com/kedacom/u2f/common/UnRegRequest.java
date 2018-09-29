/**
* Filename : UnRegRequest.java
* Author : zhangkai
* Creation time : 2018年9月27日下午5:12:21 
* Description :
*/
package com.kedacom.u2f.common;

/**
 * @author zhangkai
 *
 */
public class UnRegRequest {
	
	private String username;
	private String keyhandle;
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getKeyhandle() {
		return keyhandle;
	}
	public void setKeyhandle(String keyhandle) {
		this.keyhandle = keyhandle;
	}

}
