/**
* Filename : TestFile.java
* Author : zhangkai
* Creation time : 2018年9月14日下午1:06:06 
* Description :
*/
package com.kedacom.u2f;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

import com.yubico.u2f.U2F;

import com.kedacom.u2f.users.IUserStore;
import com.kedacom.u2f.users.UsersStoreInmemory;
import com.kedacom.u2f.consts.U2fConsts;

@SpringBootApplication
public class KedacomU2FDemoApplication {

	private static Logger logger = LoggerFactory.getLogger(KedacomU2FDemoApplication.class);

	@Value("${https.port}")
	private Integer https_port;

	@Value("${https.ssl.key-store}")
	private String key_store_name;

	@Value("${https.ssl.key-store-password}")
	private String key_store_password;

	@Value("${https.ssl.keyAlias}")
	private String key_Alias;

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
		logger.info("UsersStore initated!");
		return us;
	}

	/**
	 * initiate the main business object
	 * 
	 * @return
	 */
	@Bean
	public U2F initU2f() {
		logger.info("U2F initated!");
		return new U2F();
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
		logger.info("ChallengeStorage initated!");
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
