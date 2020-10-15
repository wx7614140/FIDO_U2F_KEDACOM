/**
* Filename : TestFile.java
* Author : zhangkai
* Creation time : 2018年9月14日下午1:06:06
* Description :
*/
package com.kedacom.u2f.u2foper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.cache.Cache;
import com.google.common.collect.Sets;
import com.kedacom.u2f.data.AssertionRequestWrapper;
import com.kedacom.u2f.data.CredentialRegistration;
import com.kedacom.u2f.data.RegistrationRequest;
import com.kedacom.u2f.data.U2fRegistrationResponse;
import com.kedacom.u2f.users.RegistrationStorage;
import com.kedacom.util.Either;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.data.exception.Base64UrlException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.kedacom.u2f.common.ResponseStateInfo;
import com.kedacom.u2f.common.UserInfo;
import com.kedacom.u2f.consts.U2fConsts.ResponseState;

@Slf4j
@RestController
public class AuthenticationService {

	@Autowired
	private RegistrationStorage registrationStorage;

	@Autowired
	private WebAuthnServer server;


	private final ObjectMapper jsonMapper = new ObjectMapper()
			.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
			.setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
			.registerModule(new Jdk8Module());

	private final class StartRegistrationResponse {
		public final boolean success = true;
		public final RegistrationRequest request;
		public final String appId;
		public final Set<RegisterRequest> registerRequests = new HashSet<>();
		public final Set<RegisteredKey> registeredKeys = new HashSet<>();
		private StartRegistrationResponse(RegistrationRequest request) {
			this.request = request;
			appId = server.getRp().getAppId().get().getId();
			RegisterRequest registerRequest = new RegisterRequest(request);
			registerRequests.add(registerRequest);

			if(request.getPublicKeyCredentialCreationOptions().getExcludeCredentials().isPresent()){
				registeredKeys.addAll(request.getPublicKeyCredentialCreationOptions().getExcludeCredentials().get().stream().map(cred ->{
					return new RegisteredKey(cred.getId());
				}).collect(Collectors.toSet()));
			}
		}
	}
	@Data
	private final class RegisteredKey{
		private final String version = "U2F_V2";
		private String keyHandle ;
		private RegisteredKey(ByteArray keyHandle) {
			this.keyHandle = keyHandle.toJsonString();
		}

	}
	@Data
	private final class RegisterRequest{
		private final String version = "U2F_V2";
		private final String challenge;
		private final String attestation;
		public RegisterRequest(RegistrationRequest request) {
			challenge = request.getPublicKeyCredentialCreationOptions().getChallenge().toJsonString();
			attestation = request.getPublicKeyCredentialCreationOptions().getAttestation().toJsonString();
		}
	}
	private final class StartAuthenticationResponse {
		public final boolean success = true;
		public final AssertionRequestWrapper request;
		public StartAuthenticationResponse(AssertionRequestWrapper request) throws MalformedURLException {
			this.request = request;
		}
	}
	private final class StartAuthenticationActions {
		private StartAuthenticationActions() throws MalformedURLException {
		}
	}
	private final JsonNodeFactory jsonFactory = JsonNodeFactory.instance;

	/**
	 * user login through the index page. If the user has bind the U2f , start
	 * sign.
	 *
	 * @param ui
	 * @param session
	 * @return
	 */
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	private ResponseStateInfo loginWithStartAuthen(@RequestBody UserInfo ui, HttpSession session) throws ExecutionException, MalformedURLException {

		if (registrationStorage.checkUser(ui.getUsername(), ui.getPassword())) {
			log.info(String.format("login success:%s/%s", ui.getUsername(), ui.getPassword()));
			Collection<CredentialRegistration> reginfo = registrationStorage.getRegistrationsByUsername(ui.getUsername());
			if ((null != reginfo) && (reginfo.size() > 0)) {
				log.info("startAuthentication username: {}", ui.getUsername());
				Either<List<String>, AssertionRequestWrapper> request = server.startAuthentication(Optional.ofNullable(ui.getUsername()));
				if (request.isRight()) {
					return startResponse(ResponseState.START_SIGN.getStateId(), new StartAuthenticationResponse(request.right().get()));
				} else {
					return messagesJson(ResponseState.SERVER_ERROR.getStateId(), request.left().get());
				}

			} else {
				// state LOGIN_WITHOUT_U2F
				session.setAttribute("username", ui.getUsername());
				log.info("[success]LOGIN_WITHOUT_U2F Username:" + ui.getUsername());
				return messagesJson(ResponseState.LOGIN_WITHOUT_U2F.getStateId(),
						"登录成功"
				);
			}
			/* return "redirect:/mainContent.html"; */
		} else {
			log.info(String.format("login failed:%s/%s", ui.getUsername(), ui.getPassword()));

			return messagesJson(ResponseState.USERINFO_INVALID.getStateId(),
					"密码不正确"
			);
		}
	}

	/**
	 * It checks the sign occured by U2F epuipment using the user registered data
	 * @param response
	 * @param session
	 * @return
	 */
	@RequestMapping(value = "/checkSign", method = RequestMethod.POST)
	private ResponseStateInfo checkSign(@RequestBody String response, HttpSession session) {
		log.info("finishSign responseJson: {}", response);
		Either<List<String>, WebAuthnServer.SuccessfulAuthenticationResult> result = server.finishAuthentication(response);
		if (result.isRight()) {
			session.setAttribute("username", result.right().get().getUsername());
			return finishResponse(
					result,
					"U2F registration failed; further error message(s) were unfortunately lost to an internal server error.",
					ResponseState.FINISH_SIGN.getStateId(),
					response
			);
		} else {

			return messagesJson(ResponseState.SERVER_ERROR.getStateId(), result.left().get());
		}

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
		Collection<CredentialRegistration> reginfo = registrationStorage.getRegistrationsByUsername(ui.getUsername());
		try {

			log.info("startRegistration username: {}, displayName: {}, credentialNickname: {}, requireResidentKey: {}", ui.getUsername(), ui.getDisplayName(), ui.getCredentialNickname(), ui.isRequireResidentKey());
			Either<String, RegistrationRequest> result = server.startRegistration(
					ui.getUsername(),
					Optional.ofNullable(ui.getDisplayName()),
					Optional.ofNullable(ui.getCredentialNickname()),
					ui.isRequireResidentKey(),
					Optional.ofNullable(ui.getSessionToken()).map(base64 -> {
						try {
							return ByteArray.fromBase64Url(base64);
						} catch (Base64UrlException e) {
							throw new RuntimeException(e);
						}
					})
			);

			if (result.isRight()) {
				return startResponse(ResponseState.START_REGISTER.getStateId(), new StartRegistrationResponse(result.right().get()));
			} else {
				return messagesJson(ResponseState.SERVER_ERROR.getStateId(),
						result.left().get()
				);
			}
		} catch (ExecutionException e) {
			lsi.setResponseState(ResponseState.SERVER_ERROR.getStateId());
			log.error("startRegistration:", e);
		}
		return lsi;
	}

	/**
	 * accept the registration response from u2f and finish registration
	 *
	 * @param session
	 * @return
	 */
	@RequestMapping(value = "/saveRegistration", method = RequestMethod.POST)
	private ResponseStateInfo saveRegistration(@RequestBody String response, HttpSession session) throws ExecutionException, JsonProcessingException {
		log.info("finishRegistration responseJson: {}", response);
		Either<List<String>, WebAuthnServer.SuccessfulU2fRegistrationResult> result = server.finishU2fRegistration(response);
		return finishResponse(
				result,
				"U2F registration failed; further error message(s) were unfortunately lost to an internal server error.",
				ResponseState.FINISH_REGISTER.getStateId(),
				response
		);
	}

	private ResponseStateInfo startResponse(int state, Object request) {
		ResponseStateInfo lsi = new ResponseStateInfo();
		try {
			lsi.setResponseState(state);
			String json = writeJson(request);
			lsi.setResponseData(json);
			log.debug("{} JSON response: {}", state, json);
			return lsi;
		} catch (IOException e) {
			log.error("Failed to encode response as JSON: {}", request, e);
			return jsonFail(lsi);
		}
	}

	private ResponseStateInfo finishResponse(Either<List<String>, ?> result, String jsonFailMessage, int state, String responseJson) {
		ResponseStateInfo lsi = new ResponseStateInfo();
		lsi.setResponseState(state);
		if (result.isRight()) {
			try {
				lsi.setResponseData(writeJson(result.right().get()));
			} catch (JsonProcessingException e) {
				log.error("Failed to encode response as JSON: {}", result.right().get(), e);
				return messagesJson(
						state,
						jsonFailMessage
				);
			}
		} else {
			log.debug("fail {} responseJson: {}", state, responseJson);
			return messagesJson(
					state,
					result.left().get()
			);
		}
		return lsi;
	}

	private ResponseStateInfo jsonFail(ResponseStateInfo lsi) {
		lsi.setResponseState(ResponseState.SERVER_ERROR.getStateId());
		return lsi;
	}

	private ResponseStateInfo messagesJson(int state, String message) {
		return messagesJson(state, Arrays.asList(message));
	}

	private ResponseStateInfo messagesJson(int state, List<String> messages) {
		ResponseStateInfo lsi = new ResponseStateInfo();
		log.debug("Encoding messages as JSON: {}", messages);
		lsi.setResponseState(state);
		try {
			lsi.setResponseData(writeJson(
					jsonFactory.objectNode()
							.set("messages", jsonFactory.arrayNode()
									.addAll(messages.stream().map(jsonFactory::textNode).collect(Collectors.toList()))
							)
			));
			return lsi;
		} catch (JsonProcessingException e) {
			log.error("Failed to encode messages as JSON: {}", messages, e);
			return jsonFail(lsi);
		}
	}

	private String writeJson(Object o) throws JsonProcessingException {
		return jsonMapper.writeValueAsString(o);
	}

}
