package cl.duoc.ventas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Crea el cliente de Amazon S3 (AWS SDK v2) que usa el resto de la aplicacion.
 *
 * Las credenciales del Learner Lab son temporales (access key + secret key + session token)
 * y llegan por variables de entorno: AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY y AWS_SESSION_TOKEN.
 */
@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(@Value("${app.s3.region}") String region,
                             @Value("${app.aws.access-key}") String accessKey,
                             @Value("${app.aws.secret-key}") String secretKey,
                             @Value("${app.aws.session-token}") String sessionToken) {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credenciales(accessKey, secretKey, sessionToken))
                .build();
    }

    /**
     * Si vienen las tres credenciales se usan tal cual. Si no (por ejemplo en los tests),
     * se usa la cadena por defecto de AWS, que solo se evalua cuando se llama a S3;
     * asi la aplicacion puede arrancar sin credenciales.
     */
    private AwsCredentialsProvider credenciales(String accessKey, String secretKey, String sessionToken) {
        if (accessKey.isBlank() || secretKey.isBlank() || sessionToken.isBlank()) {
            return DefaultCredentialsProvider.create();
        }
        return StaticCredentialsProvider.create(
                AwsSessionCredentials.create(accessKey, secretKey, sessionToken));
    }
}
