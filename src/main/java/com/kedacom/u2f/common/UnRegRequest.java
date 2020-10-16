/**
* Filename : UnRegRequest.java
* Author : zhangkai
* Creation time : 2018年9月27日下午5:12:21
* Description :
*/
package com.kedacom.u2f.common;

import lombok.Data;

/**
 * @author zhangkai
 *
 */
@Data
public class UnRegRequest {

	private String username;
	private String credentialId;

}
