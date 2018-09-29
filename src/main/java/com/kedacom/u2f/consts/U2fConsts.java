/**
* Filename : TestFile.java
* Author : zhangkai
* Creation time : 2018年9月17日下午1:06:06 
* Description :
*/
package com.kedacom.u2f.consts;

public interface U2fConsts {

	enum ResponseState implements EnumConstructor {
		USERINFO_INVALID(0), /* username or password is error */
		LOGIN_WITHOUT_U2F(1), /* userinfo passed and no reginfo */
		START_SIGN(2), /*
						 * userinfo passed and return chanllage, wait to get
						 * sign from u2f token
						 */
		START_REGISTER(3), /*
							 * return chanllage and wait to get register info
							 * from u2f token
							 */
		FINISH_SIGN(4), FINISH_REGISTER(5), REMOVE_CHALLENGE(6),
		USER_EXISTED(7),
		USER_ADDED(8),
		USER_DELED(9),
		DEL_REGISTRATION(10),
		PASSWORD_MODIFIED(11),
		SERVER_ERROR(99);/* error occured at server side */

		private int id;

		private ResponseState(int index) {
			this.id = index;
		}

		public int getStateId() {
			return this.id;
		}
	}

	enum U2F_ErrorCode implements EnumConstructor {
		OK(0), OTHER_ERROR(1), BAD_REQUEST(2), CONFIGURATION_UNSUPPORTED(3), DEVICE_INELIGIBLE(4), TIMEOUT(5);

		private int id;

		private U2F_ErrorCode(int index) {
			this.id = index;
		}

		@Override
		public int getStateId() {

			return this.id;
		}
	}

	String VERSION = "U2F_V2";
	String ADMINNAME = "admin";
	String ADMINPWD = "admin";

}
