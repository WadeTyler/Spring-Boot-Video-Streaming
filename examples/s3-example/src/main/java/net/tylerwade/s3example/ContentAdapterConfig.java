package net.tylerwade.s3example;

import lombok.RequiredArgsConstructor;
import net.tylerwade.springbootvideostreaming.adapter.ContentStreamAdapter;
import net.tylerwade.springbootvideostreaming.adapter.S3ContentStreamAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@RequiredArgsConstructor
public class ContentAdapterConfig {

	private final S3Client s3Client;

	// You shouldn't hardcode your bucket name, but this is just an example.
	private static final String bucket = "tw-video-streaming-test-bucket";

	@Bean
	public ContentStreamAdapter contentStreamAdapter() {
		return new S3ContentStreamAdapter(s3Client, bucket);
	}

}