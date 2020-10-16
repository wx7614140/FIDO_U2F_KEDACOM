/**
* Filename : TestFile.java
* Author : zhangkai
* Creation time : 2018年9月14日下午1:06:06
* Description :
*/
package com.kedacom.u2f.online;

import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpSession;

import com.kedacom.u2f.data.CredentialRegistration;
import com.kedacom.u2f.u2foper.BaseController;
import com.kedacom.u2f.users.RegistrationStorage;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.exception.Base64UrlException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.kedacom.u2f.common.UserInfo;
import com.kedacom.u2f.common.UnRegRequest;
import com.kedacom.u2f.common.UserListNode;
import com.kedacom.u2f.common.ResponseStateInfo;
import com.kedacom.u2f.consts.U2fConsts.ResponseState;

@Controller
@Slf4j
public class SystemService  extends BaseController {

	@Autowired
	private RegistrationStorage registrationStorage;

	/**
	 * User enter the main page and get the user name of current session
	 *
	 * @param session
	 * @return
	 */
	@RequestMapping(value = "/getLoginName", method = RequestMethod.POST)
	@ResponseBody
	public String getLoginName(HttpSession session) {
		String un = (String) session.getAttribute("username");
		if (null == un) {
			un = "";
		}
		log.info("the front get current login name is:" + un);
		return un;
	}

	/**
	 * User logout the site.
	 *
	 * @param session
	 * @return
	 */
	@RequestMapping(value = "/logout", method = RequestMethod.POST)
	public ModelAndView logOut(HttpSession session) {
		ModelAndView modelAndView = new ModelAndView("redirect:/");
		String cu = (String) session.getAttribute("username");
		log.info("user " + cu + " logout!");
		session.removeAttribute("username");
		return modelAndView;
	}

	/**
	 * The main page load user data from server to init the page.
	 *
	 * @param session
	 * @return
	 */
	@RequestMapping(value = "/loadUserInfo", method = RequestMethod.POST)
	@ResponseBody
	public List<UserListNode> loadUserInfo(HttpSession session) {
		String un = (String) session.getAttribute("username");
		if (null == un) {
			un = "";
		}
		log.info("the current login name is:" + un);
		return registrationStorage.getUserList(un);
	}

	@RequestMapping(value = "/loadUserRegInfo", method = RequestMethod.POST)
	@ResponseBody
	public Collection<CredentialRegistration> loadUserRegInfo(HttpSession session) {
		String un = (String) session.getAttribute("username");
		if (null == un) {
			un = "";
		}
		log.info("the current login name is:" + un);
		return registrationStorage.getRegistrationsByUsername(un);
	}
	/**
	 * Add user on the page.
	 *
	 * @param ui
	 * @return
	 */
	@RequestMapping(value = "/addUser", method = RequestMethod.POST)
	@ResponseBody
	private ResponseStateInfo addUser(@RequestBody UserInfo ui) {
		ResponseStateInfo lsi = new ResponseStateInfo();
		if (registrationStorage.userExists(ui.getUsername())) {
			lsi.setResponseState(ResponseState.USER_EXISTED.getStateId());
			log.error("add user fail:" + ui.getUsername() + " exists!");
		} else {
			if (registrationStorage.addUser(ui.getUsername(), ui.getPassword())) {
				lsi.setResponseState(ResponseState.USER_ADDED.getStateId());
				log.info("[success]adduser:" + ui.getUsername());
			} else {
				lsi.setResponseState(ResponseState.SERVER_ERROR.getStateId());
				log.info("add user fail:" + ui.getUsername() + " oper fail!");
			}
			;
		}
		return lsi;
	}


	/**
	 * Del user on the page.
	 * @param ui
	 * @return
	 */
	@RequestMapping(value = "/delUser", method = RequestMethod.POST)
	@ResponseBody
	private ResponseStateInfo delUser(@RequestBody UserInfo ui) {
		ResponseStateInfo lsi = new ResponseStateInfo();
		if (registrationStorage.removeUser(ui.getUsername())) {
			lsi.setResponseState(ResponseState.USER_DELED.getStateId());
			log.info("[success]deluser:" + ui.getUsername());
		} else {
			lsi.setResponseState(ResponseState.SERVER_ERROR.getStateId());
			log.info("del user fail:" + ui.getUsername() + " oper fail!");
		}
		;
		return lsi;
	}

	/**
	 * modify user's password on the page.
	 *
	 * @param ui
	 * @return
	 */
	@RequestMapping(value = "/modifyPassword", method = RequestMethod.POST)
	@ResponseBody
	private ResponseStateInfo modifyPassword(@RequestBody UserInfo ui) {
		ResponseStateInfo lsi = new ResponseStateInfo();
		if (registrationStorage.modifyPassword(ui.getUsername(),ui.getPassword())) {
			lsi.setResponseState(ResponseState.PASSWORD_MODIFIED.getStateId());
			lsi.setResponseData(ui.getPassword());
			log.info("[success]modifyPassword:" + ui.getUsername());
		} else {
			lsi.setResponseState(ResponseState.SERVER_ERROR.getStateId());
			log.info("modifyPassword fail:" + ui.getUsername() + " oper fail!");
		}
		;
		return lsi;
	}

	/**
	 * unbind regiser data from user on the page.
	 *
	 * @return
	 */
	@RequestMapping(value = "/unRegistration", method = RequestMethod.POST)
	@ResponseBody
	private ResponseStateInfo unRegistration(@RequestBody UnRegRequest urr) {
		ResponseStateInfo lsi = new ResponseStateInfo();
		final ByteArray credentialId;
		try {
			credentialId = ByteArray.fromBase64Url(urr.getCredentialId());
		} catch (Base64UrlException e) {
			return messagesJson(
					ResponseState.SERVER_ERROR.getStateId(),
					"Credential ID is not valid Base64Url data: " + urr.getCredentialId()
			);
		}
		if (registrationStorage.delUserRegInfo(urr.getUsername(), credentialId)) {
			lsi.setResponseState(ResponseState.DEL_REGISTRATION.getStateId());
			log.info("[success]unRegistration:" + urr.getUsername() + " with keyhandle:" + urr.getCredentialId());
		} else {
			lsi.setResponseState(ResponseState.SERVER_ERROR.getStateId());
			log.info("unRegistration fail:" + urr.getUsername() + " with keyhandle:" + urr.getCredentialId()
					+ " oper fail!");
		}
		;
		return lsi;
	}

}
