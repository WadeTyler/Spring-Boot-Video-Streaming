package net.tylerwade.springbootvideostreaming.adapter;

import lombok.extern.slf4j.Slf4j;
import net.tylerwade.springbootvideostreaming.model.Range;
import net.tylerwade.springbootvideostreaming.model.StreamContentRequest;
import net.tylerwade.springbootvideostreaming.model.StreamedContent;
import net.tylerwade.springbootvideostreaming.model.StreamedContentMetadata;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.util.List;

@Slf4j
public class S3ContentStreamAdapter implements ContentStreamAdapter {

	private final S3Client s3Client;
	private final String bucket;

	public S3ContentStreamAdapter(S3Client s3Client, String bucket) {
		this.s3Client = s3Client;
		this.bucket = bucket;
	}

	@Override
	public Mono<StreamedContent> loadContent(StreamContentRequest contentRequest) {
		return Mono.fromCallable(() -> getContentMetadata(contentRequest.getKey()))
				.subscribeOn(Schedulers.boundedElastic())
				.flatMap(metadata -> {
					// Validate range
					Range validRange = createValidRange(contentRequest.getRange(), metadata.getFileSize());
					Long contentLength = validRange.getEnd() - validRange.getStart() + 1;

					// Stream Content
					Flux<DataBuffer> content = streamContent(contentRequest.getKey(), validRange);

					return Mono.just(StreamedContent.builder()
							.key(contentRequest.getKey())
							.metadata(metadata)
							.content(content)
							.contentLength(contentLength)
							.range(validRange)
							.build());
				})
				.doOnError(e -> log.error("Failed to load S3 content for key {}.", contentRequest.getKey(), e));
	}

	private Flux<DataBuffer> streamContent(String objectKey, Range range) {
		String rangeHeader = String.format("bytes=%d-%d", range.getStart(), range.getEnd());

		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(bucket)
				.key(objectKey)
				.range(rangeHeader)
				.build();

		log.debug("Streaming S3 Object {}/{}. {}", bucket, objectKey, rangeHeader);

		return DataBufferUtils.readInputStream(
				() -> s3Client.getObject(getObjectRequest),
				new DefaultDataBufferFactory(),
				8192 // 8 KB
		).doOnError(e -> log.error("Error streaming S3 Object {}/{}. {}", bucket, objectKey, rangeHeader, e));
	}

	@Override
	public Long getContentSize(String key) {
		StreamedContentMetadata metadata = getContentMetadata(key);
		return metadata.getFileSize();
	}

	@Override
	public StreamedContentMetadata getContentMetadata(String key) {
		try {

			HeadObjectRequest request = HeadObjectRequest.builder()
					.bucket(bucket)
					.key(key)
					.build();

			HeadObjectResponse response = s3Client.headObject(request);

			return StreamedContentMetadata.builder()
					.key(key)
					.contentType(response.contentType())
					.fileSize(response.contentLength())
					.build();
		} catch (Exception e) {
			log.error("Failed to get S3 content metadata for key {}.", key, e);
			throw e;
		}
	}

	@Override
	public List<StreamedContentMetadata> getAllContentMetadata() {
		try {
			ListObjectsV2Request request = ListObjectsV2Request.builder()
					.bucket(bucket)
					.build();

			ListObjectsV2Iterable response = s3Client.listObjectsV2Paginator(request);

			return response.contents().stream()
					.map(object -> StreamedContentMetadata.builder()
							.key(object.key())
							.contentType(extractContentType(object.key()))
							.fileSize(object.size())
							.build()
					).toList();
		} catch (Exception e) {
			log.error("Failed to list S3 content.", e);
			throw e;
		}
	}
}