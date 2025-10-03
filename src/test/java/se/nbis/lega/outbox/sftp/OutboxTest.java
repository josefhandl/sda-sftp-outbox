package se.nbis.lega.outbox.sftp;

import org.apache.commons.codec.digest.Crypt;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHeaders;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import se.nbis.lega.outbox.pojo.Credentials;
import se.nbis.lega.outbox.pojo.KeyAlgorithm;
import se.nbis.lega.outbox.pojo.PasswordHashingAlgorithm;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;

@SpringBootTest(classes = LocalStorageOutboxApplication.class)
@TestPropertySource(locations = "classpath:local-storage.application.properties")
@RunWith(SpringRunner.class)
public abstract class OutboxTest {

    protected String outboxFolder;
    protected String cegaEndpoint;
    protected String cegaCredentials;
    protected String username;
    protected String password;
    protected String passwordHash;
    protected String publicKey;
    private RestTemplate restTemplate;

    @Before
    public void generateUser() throws IOException, URISyntaxException {
        username = UUID.randomUUID().toString();
        password = UUID.randomUUID().toString();
        mockCEGAEndpoint(username, password, PasswordHashingAlgorithm.BLOWFISH, KeyAlgorithm.RSA);
    }

    @After
    public void cleanup() throws IOException {
        File userFolder = new File(outboxFolder + "/" + username + "/");
        FileUtils.deleteDirectory(userFolder);
    }

    protected void mockCEGAEndpoint(String username, String password, PasswordHashingAlgorithm passwordHashingAlgorithm, KeyAlgorithm keyAlgorithm) throws URISyntaxException, IOException {
        mockCEGAEndpoint(username, password, passwordHashingAlgorithm, keyAlgorithm, HttpStatus.OK);
    }

    protected void mockCEGAEndpoint(String username, String password, PasswordHashingAlgorithm passwordHashingAlgorithm, KeyAlgorithm keyAlgorithm, HttpStatus httpStatus) throws URISyntaxException, IOException {
        passwordHash = passwordHashingAlgorithm == PasswordHashingAlgorithm.BLOWFISH
                ? BCrypt.hashpw(password, BCrypt.gensalt())
                : Crypt.crypt(password, passwordHashingAlgorithm.getMagicString() + BCrypt.gensalt() + "$");
        URI cegaURI = new URL(String.format(cegaEndpoint, username)).toURI();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(cegaCredentials.getBytes()));
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        List<String> publickeyList = new ArrayList<>();
        publicKey = FileUtils.readFileToString(new File(classloader.getResource(String.format("%s.ssh", keyAlgorithm.name()).toLowerCase()).toURI()), Charset.defaultCharset());
        publickeyList.add(publicKey);
        Credentials credentials = new Credentials();
        credentials.setPasswordHash(passwordHash);
        credentials.setPublicKey(publickeyList);
        when(restTemplate.exchange(cegaURI, HttpMethod.GET, new HttpEntity<>(headers), Credentials.class)).thenReturn(new ResponseEntity<>(credentials, httpStatus));
    }

    @Value("${outbox.local.directory}")
    public void setOutboxFolder(String outboxFolder) {
        this.outboxFolder = outboxFolder;
    }

    @Value("${outbox.cega.endpoint}")
    public void setCegaEndpoint(String cegaEndpoint) {
        this.cegaEndpoint = cegaEndpoint;
    }

    @Value("${outbox.cega.credentials}")
    public void setCegaCredentials(String cegaCredentials) {
        this.cegaCredentials = cegaCredentials;
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

}
