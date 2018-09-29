/**
* Filename : TestFile.java
* Author : zhangkai
* Creation time : 2018年9月14日下午1:06:06 
* Description :
*/
package com.kedacom.u2f.u2foper;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.yubico.u2f.U2F;
import com.yubico.u2f.data.DeviceRegistration;
import com.yubico.u2f.data.messages.RegisterRequestData;
import com.yubico.u2f.data.messages.RegisterResponse;
import com.yubico.u2f.data.messages.SignRequestData;
import com.yubico.u2f.data.messages.SignResponse;
import com.yubico.u2f.exceptions.DeviceCompromisedException;
import com.yubico.u2f.exceptions.NoEligibleDevicesException;
import com.yubico.u2f.exceptions.U2fAuthenticationException;
import com.yubico.u2f.exceptions.U2fBadConfigurationException;
import com.yubico.u2f.exceptions.U2fBadInputException;
import com.yubico.u2f.exceptions.U2fRegistrationException;


import com.kedacom.u2f.common.ResponseStateInfo;
import com.kedacom.u2f.common.FinishRegSignRequest;
import com.kedacom.u2f.common.UserInfo;
import com.kedacom.u2f.consts.U2fConsts.ResponseState;
import com.kedacom.u2f.consts.U2fConsts.U2F_ErrorCode;
import com.kedacom.u2f.users.IUserStore;

@RestController
public class AuthenticationService {

	private static Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
	
	@Value("${u2f.appId}")
	private String u2f_appId;

	@Autowired
	private IUserStore us;

	@Autowired
	private U2F u2f;

	@Autowired
	private Map<String, String> challengeStore;

	/**
	 * user login through the index page. If the user has bind the U2f , start
	 * sign.
	 * 
	 * @param ui
	 * @param session
	 * @return
	 */
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	private ResponseStateInfo loginWithStartAuthen(@RequestBody UserInfo ui, HttpSession session) {
		ResponseStateInfo lsi = new ResponseStateInfo();
		if (us.checkUser(ui.getUsername(), ui.getPassword())) {
			logger.info(String.format("login success:%s/%s", ui.getUsername(), ui.getPassword()));
			List<DeviceRegistration> reginfo = us.getUserRegInfo(ui.getUsername());
			if ((null != reginfo) && (reginfo.size() > 0)) {
				// state WAIT_SIGN,startAuthentication
				lsi.setResponseState(ResponseState.START_SIGN.getStateId());
				try {
					SignRequestData signRequestData = u2f.startSignature(u2f_appId, reginfo);
					challengeStore.put(signRequestData.getRequestId(), signRequestData.toJson());
					lsi.setResponseData(signRequestData.toJson());
					logger.info("[success]request_registration:  signRequestData:" + lsi.getResponseData());
				} catch (NoEligibleDevicesException e) {
					logger.error("loginWithStartAuthen error:", e);
					lsi.setResponseState(ResponseState.SERVER_ERROR.getStateId());
				} catch (U2fBadConfigurationException e) {
					logger.error("loginWithStartAuthen error:", e);
					lsi.setResponseState(ResponseState.SERVER_ERROR.getStateId());
				}
			} else {
				// state LOGIN_WITHOUT_U2F
				lsi.setResponseState(ResponseState.LOGIN_WITHOUT_U2F.getStateId());
				session.setAttribute("username", ui.getUsername());
				logger.info("[success]LOGIN_WITHOUT_U2F Username:" + ui.getUsername());
			}
			/* return "redirect:/mainContent.html"; */
		} else {
			logger.info(String.format("login failed:%s/%s", ui.getUsername(), ui.getPassword()));
			lsi.setResponseState(ResponseState.USERINFO_INVALID.getStateId());
			/* return "redirect:/index.html?error"; */
		}
		return lsi;
	}
	
	/**
	 * It checks the sign occured by U2F epuipment using the user registered data
	 * @param frr
	 * @param session
	 * @return
	 */
	@RequestMapping(value = "/checkSign", method = RequestMethod.POST)
	private ResponseStateInfo checkSign(@RequestBody FinishRegSignRequest frr, HttpSession session) {
		ResponseStateInfo lsi = new ResponseStateInfo();
		if (frr.getErrorCode() == U2F_ErrorCode.OK.getStateId()) {
			// errcode is ok
			try {
				SignResponse signResponse = SignResponse.fromJson(frr.getTokenResponse());
				SignRequestData authenticateRequest = SignRequestData
						.fromJson(challengeStore.remove(frr.getChallenge()));
				DeviceRegistration registration = null;
				registration = u2f.finishSignature(authenticateRequest, signResponse,
						us.getUserRegInfo(frr.getUsername()));
				session.setAttribute("username", frr.getUsername());
				lsi.setResponseState(ResponseState.FINISH_SIGN.getStateId());			
				logger.info("[success]check_Sign:  challenge:" + frr.getChallenge() + " DeviceRegistration:" + registration.toJson());	
			} catch (U2fBadInputException ube) {
				logger.error("finishRegistration U2fBadInputException:", ube);
				lsi.setResponseState(ResponseState.SERVER_ERROR.getStateId());
			} catch (DeviceCompromisedException e) {
				logger.error("Device possibly compromised and therefore blocked: " + e.getMessage());
				lsi.setResponseState(ResponseState.SERVER_ERROR.getStateId());
			} catch (U2fAuthenticationException e) {
				logger.error("Authentication failed: " + e.getCause().getMessage());
				lsi.setResponseState(ResponseState.SERVER_ERROR.getStateId());
			}
		} else {
			// errcode is not ok,remove challenge and return error
			challengeStore.remove(frr.getChallenge());
			logger.error("checkSign get errorcode from client:" + frr.getErrorCode() + ".Remove challenge:"
					+ frr.getChallenge());
			lsi.setResponseState(ResponseState.REMOVE_CHALLENGE.getStateId());
		}
		return lsi;
	}

	/**
	 * user start register
	 * 
	 * @param ui
	 * @param session
	 * @return
	 */
	@RequestMapping(value = "/requestRegistration", method = RequestMethod.POST)
	private ResponseStateInfo requestRegistration(@RequestBody UserInfo ui, HttpSession session) {
		ResponseStateInfo lsi = new ResponseStateInfo();
		List<DeviceRegistration> reginfo = us.getUserRegInfo(ui.getUsername());
		try {
			RegisterRequestData registerRequestData = u2f.startRegistration(u2f_appId, reginfo);
			challengeStore.put(registerRequestData.getRequestId(), registerRequestData.toJson());
			lsi.setResponseState(ResponseState.START_REGISTER.getStateId());
			lsi.setResponseData(registerRequestData.toJson());
			logger.info("[success]request_Registration:  registerRequestData: " + registerRequestData.toJson());
		} catch (U2fBadConfigurationException e) {
			lsi.setResponseState(ResponseState.SERVER_ERROR.getStateId());
			logger.error("startRegistration:", e);
		}
		return lsi;
	}

	/**
	 * accept the registration response from u2f and finish registration
	 * 
	 * @param frr
	 * @param session
	 * @return
	 */
	@RequestMapping(value = "/saveRegistration", method = RequestMethod.POST)
	private ResponseStateInfo saveRegistration(@RequestBody FinishRegSignRequest frr, HttpSession session) {
		ResponseStateInfo lsi = new ResponseStateInfo();
		if (frr.getErrorCode() == U2F_ErrorCode.OK.getStateId()) {
			// errcode is ok
			try {
				RegisterResponse registerResponse = RegisterResponse.fromJson(frr.getTokenResponse());
				RegisterRequestData registerRequestData = RegisterRequestData
						.fromJson(challengeStore.remove(registerResponse.getRequestId()));
				DeviceRegistration registration = u2f.finishRegistration(registerRequestData, registerResponse);
				// add reg info to usersStore
				us.addUserRegInfo(frr.getUsername(), registration.getKeyHandle(), registration.toJson());
				// return the reginfo to the front
				lsi.setResponseState(ResponseState.FINISH_REGISTER.getStateId());
				lsi.setResponseData(registration.toJson());
				logger.info("[success]save_Registration:  challenge:" + frr.getChallenge() + " reginfo:" + registration.toJson());
			} catch (U2fBadInputException ube) {
				logger.error("finishRegistration U2fBadInputException:", ube);
				lsi.setResponseState(ResponseState.SERVER_ERROR.getStateId());
			} catch (U2fRegistrationException ure) {
				logger.error("finishRegistration U2fRegistrationException:", ure);
				lsi.setResponseState(ResponseState.SERVER_ERROR.getStateId());
			}
		} else {
			// errcode is not ok,remove challenge and return error
			challengeStore.remove(frr.getChallenge());
			logger.error("finishRegistration get errorcode from client:" + frr.getErrorCode() + ".Remove challenge:"
					+ frr.getChallenge());
			lsi.setResponseState(ResponseState.REMOVE_CHALLENGE.getStateId());
		}
		return lsi;
	}	

}
