# 1.如何构建运行
&emsp;&emsp;该项目客户端使用jquery和jquery-ui编写，服务端使用spring-boot框架编写。可使用maven进行构建，使用如下命令：
```
./mvnw clean package
```
&emsp;&emsp;对于生成的jar包,可使用如下命令(建议JDK1.8)直接运行（需要注意当前工作目录下要有tomcat.keystore文件，不然会报spring的注入错误）：
```
java -jar Kedacom-U2F-DEMO-0.0.1-SNAPSHOT.jar
```
&emsp;&emsp;上面的程序运行后，启动了一个tomcat服务器，支持http和https两种模式。用户可在浏览器中使用“http://localhost:8080” 和 “https://localhost:8443” 两种模式访问，在http模式下不支持U2F设备。
# 2.如何支持HTTPS
&emsp;&emsp;为使用HTTPS，需要使用自签名证书，我们使用JDK自带的keytool生成自签名证书tomcat.keystore，项目运行时与运行jar包放在同一目录下。生成命令如下：
```
keytool -genkeypair -keystore tomcat.keystore  -alias tomcat -keyalg RSA -keysize 2048 -validity 5000 -dname "CN=localhost, OU=kedacom, O=kedacom, L=shanghai, ST=shanghai, C=cn" -ext "SAN=DNS:localhost,IP:172.16.64.59" -ext "BC=ca:true"
```
&emsp;&emsp;为使得浏览器对自签名证书不产生告警，需要从tomcat.keystore中导出公钥证书(cer文件)以导入浏览器的“受信任的根证书颁发机构”，导出公钥证书的命令如下：
```
keytool -keystore tomcat.keystore -export -alias tomcat -file tomcat.cer
```
&emsp;&emsp;在项目的application.properties文件中，定义了相关HTTPS参数，这些参数在项目启动时，被spring注入到变量中，application.properties定义如下：
```
#https
https.port=8443
https.ssl.key-store=tomcat.keystore
https.ssl.key-store-password=tomcat
https.ssl.keyAlias=tomcat

#u2f
u2f.appId=https://localhost:8443
```
&emsp;&emsp;如果读者使用该项目的代码构建自己的站点时，一定要注意保证application.properties文件中“u2f.appId”，tomcat.keystore中CN,SAN对域名(机器名)的一致性。
# 3.	如何实现用户数据的持久化
&emsp;&emsp;该项目中未实现用户数据的磁盘持久化，这意味着服务器一重启，之前保存的用户数据都将丢失。但要实现持久化对于有兴趣的读者而言也是非常简单的事情，项目中对于用户数据的操作是使用com.kedacom.u2f.users.IUserStore实现的，系统启动时注入该接口的实现对象，目前项目代码中使用的是com.kedacom.u2f.users.UsersStoreInmemory对象注入的。读者只需将实现IUserStore的自定义持久化对象替代UsersStoreInmemory注入即可。
# 4.	站点功能
&emsp;&emsp;该项目站点DEMO提供完整的用户和U2F设备管理功能，提供用户的增加删除修改，U2F设备的注册绑定和鉴权等功能。
## 4.1 用户管理功能
&emsp;&emsp;站点启动时已经缺省生成了admin用户，可使用“admin/admin”的初始用户名和密码登录。图1展示了用户的增加、删除和修改密码功能。
![图1](https://github.com/solarkai/FIDO_U2F_KEDACOM/blob/master/doc/figure1.png)
## 4.2.	绑定、解绑U2F设备
&emsp;&emsp;该站点中一个用户可绑定（注册）多个U2F设备，对同一个U2F设备不可绑定两次。而同一个U2F设备可被多个用户绑定。
&emsp;&emsp; 图2显示了一个用户的设备绑定过程，站点在绑定时会提示用户触摸设备。
![图2](https://github.com/solarkai/FIDO_U2F_KEDACOM/blob/master/doc/figure2.png)
&emsp;&emsp;图3显示了设备绑定后的数据。
![图3](https://github.com/solarkai/FIDO_U2F_KEDACOM/blob/master/doc/figure3.png)
&emsp;&emsp;图4显示了一个用户绑定多个设备的注册数据，每个设备的绑定数据以keyHandle作为标识。
![图4](https://github.com/solarkai/FIDO_U2F_KEDACOM/blob/master/doc/figure4.png)
&emsp;&emsp;可选择其中的一个keyHandle解除绑定，该keyHandle对应的U2F设备在登录鉴权时将不再起作用，如图5所示。
![图5](https://github.com/solarkai/FIDO_U2F_KEDACOM/blob/master/doc/figure5.png)
## 4.3.	用户登录鉴权
&emsp;&emsp;对于绑定了U2F设备的用户，在登录时不仅要校验用户名和密码，还需要验证U2F设备，如图6所示。
![图6](https://github.com/solarkai/FIDO_U2F_KEDACOM/blob/master/doc/figure6.png)
# 5.	使用的第三方库
&emsp;&emsp;该项目在客户端使用的U2F签名和注册接口脚本均来自<https://demo.yubico.com/js/u2f-api.js>，服务端使用了yubico提供的u2flib-server-core和u2flib-server-attestation这两个库，可在pom文件中增加如下依赖：
```
    <dependency>
			<groupId>com.yubico</groupId>
			<artifactId>u2flib-server-core</artifactId>
			<version>0.19.0</version>
		</dependency>

		<dependency>
			<groupId>com.yubico</groupId>
			<artifactId>u2flib-server-attestation</artifactId>
			<version>0.19.0</version>
		</dependency>
```
&emsp;&emsp;这两个库完成U2F设备注册信息中证书的验证、公钥的提取、签名的验证等功能，其核心类为com.yubico.u2f.U2F类，引用了java.security相关的包和类，代码值得一读。
