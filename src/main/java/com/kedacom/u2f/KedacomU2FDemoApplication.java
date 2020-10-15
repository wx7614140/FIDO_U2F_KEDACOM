/**
* Filename : TestFile.java
* Author : zhangkai
* Creation time : 2018年9月14日下午1:06:06
* Description :
*/
package com.kedacom.u2f;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import com.kedacom.u2f.consts.U2fConsts;
import com.kedacom.u2f.data.AssertionRequestWrapper;
import com.kedacom.u2f.data.RegistrationRequest;
import com.kedacom.u2f.users.IUserStore;
import com.kedacom.u2f.users.InMemoryRegistrationStorage;
import com.kedacom.u2f.users.RegistrationStorage;
import com.kedacom.u2f.users.UsersStoreInmemory;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.extension.appid.AppId;
import com.yubico.webauthn.extension.appid.InvalidAppIdException;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;

@SpringBootApplication
public class KedacomU2FDemoApplication {

	private static Logger log = LoggerFactory.getLogger(KedacomU2FDemoApplication.class);

	@Value("${https.port}")
	private Integer https_port;

	@Value("${https.ssl.key-store}")
	private String key_store_name;

	@Value("${https.ssl.key-store-password}")
	private String key_store_password;

	@Value("${https.ssl.keyAlias}")
	private String key_Alias;

	@Value("${u2f.appId}")
	private String u2f_appId;

	@Value("${u2f.appName}")
	private String u2f_appName;

	@Value("${u2f.origins}")
	private String u2f_origins;

	public static void main(String[] args) {
		SpringApplication.run(KedacomU2FDemoApplication.class, args);
	}

	/**
	 * initiate the users store and insert the first user 'admin'
	 *
	 * @return
	 */
	@Bean
	public IUserStore initUsersStore() {
		UsersStoreInmemory us = new UsersStoreInmemory();
		us.addUser(U2fConsts.ADMINNAME, U2fConsts.ADMINPWD);
		log.info("UsersStore initated!");
		return us;
	}

	@Bean
	public RegistrationStorage initCredentialRepository() {
		InMemoryRegistrationStorage us = new InMemoryRegistrationStorage();
		us.addUser(U2fConsts.ADMINNAME, U2fConsts.ADMINPWD);
		log.info("UsersStore initated!");
		return us;
	}

	@Bean
	public RelyingPartyIdentity initRelyingPartyIdentity(){
		RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
				.id(u2f_appId)
				.name(u2f_appName)
				.build();
		return rpIdentity;
	}

//	@Bean
//	public RelyingParty initRelyingParty() throws InvalidAppIdException {
//		RelyingParty rp = RelyingParty.builder()
//				.identity(initRelyingPartyIdentity())
//				.credentialRepository(initCredentialRepository())
//				.origins(origins())
//				.appId(new AppId(u2f_appId))
//				.build();
//		return rp;
//	}

	@Bean
	public Set<String> origins(){
		return Sets.newHashSet(u2f_origins.split(","));
	}
	private static <K, V> Cache<K, V> newCache() {
		return CacheBuilder.newBuilder()
				.maximumSize(100)
				.expireAfterAccess(10, TimeUnit.MINUTES)
				.build();
	}

	@Bean
	public Cache<ByteArray, AssertionRequestWrapper> assertRequestStorage(){
		return newCache();
	}

	@Bean
	public Cache<ByteArray, RegistrationRequest> registerRequestStorage(){
		return newCache();
	}
	/**
	 * which initate the challenge store to store the temp challenge value and
	 * maped sign/register data. It is just a simple implement. In fact, if
	 * added timer controller, it will be better.
	 *
	 * @return
	 */
	@Bean
	public Map<String, String> initChallengeStore() {
		log.info("ChallengeStorage initated!");
		return new ConcurrentHashMap<String, String>();
	}

	@Bean
	public ServletWebServerFactory servletContainer() {
		TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
		tomcat.addAdditionalTomcatConnectors(createSslConnector());
		return tomcat;
	}

	private Connector createSslConnector() {
		Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
		File keystoreFile = new File(key_store_name);

		if (keystoreFile.exists()) {
			connector.setScheme("https");
			connector.setSecure(true);
			connector.setPort(https_port);
			protocol.setSSLEnabled(true);
			protocol.setKeystoreFile(keystoreFile.getAbsolutePath());
			protocol.setKeystorePass(key_store_password);
			protocol.setKeyAlias(key_Alias);
			return connector;
		} else {
			throw new IllegalStateException(
					"can't access keystore: [" + "keystore" + "] or truststore: [" + "keystore" + "]");
		}
	}

}
