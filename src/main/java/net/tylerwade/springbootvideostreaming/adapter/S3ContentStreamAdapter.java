package net.tylerwade.springbootvideostreaming.adapter;

import lombok.extern.slf4j.Slf4j;
import net.tylerwade.springbootvideostreaming.model.Range;
import net.tylerwade.springbootvideostreaming.model.StreamContentRequest;
import net.tylerwade.springbootvideostreaming.model.StreamedContent;
import net.tylerwade.springbootvideostreaming.model.StreamedContentMetadata;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.io.IOException;
import java.io.InputStream;
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
	public StreamedContent loadContent(StreamContentRequest contentRequest) throws IOException {
		try {

			// Load object metadata
			StreamedContentMetadata metadata = getContentMetadata(contentRequest.getKey());

			// Validate range
			Range validRange = createValidRange(contentRequest.getRange(), metadata.getFileSize());
			Long contentLength = validRange.getEnd() - validRange.getStart() + 1;

			// Stream Content
			StreamingResponseBody content = streamContent(contentRequest.getKey(), validRange);

			return StreamedContent.builder()
					.key(contentRequest.getKey())
					.metadata(metadata)
					.content(content)
					.contentLength(contentLength)
					.range(validRange)
					.build();
		} catch (Exception e) {
			log.error("Failed to load S3 content for key {}.", contentRequest.getKey(), e);
			throw new IOException("Failed to load S3 content for key " + contentRequest.getKey(), e);
		}
	}

	private StreamingResponseBody streamContent(String objectKey, Range range) {
		String rangeHeader = String.format("bytes=%d-%d", range.getStart(), range.getEnd());

		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(bucket)
				.key(objectKey)
				.range(rangeHeader)
				.build();

		log.debug("Streaming S3 Object {}/{}. {}", bucket, objectKey, rangeHeader);

		return outputStream -> {
			try (InputStream is = s3Client.getObject(getObjectRequest)) {
				is.transferTo(outputStream);
				outputStream.flush();
			} catch (Exception e) {
				log.error("Error streaming S3 Object {}/{}. {}", bucket, objectKey, rangeHeader, e);
				throw new IOException("Error streaming S3 Object.", e);
			}
		};
	}

	@Override
	public Long getContentSize(String key) throws IOException {
		StreamedContentMetadata metadata = getContentMetadata(key);
		return metadata.getFileSize();
	}

	@Override
	public StreamedContentMetadata getContentMetadata(String key) throws IOException {
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
			throw new IOException("Failed to get S3 content metadata for key " + key, e);
		}
	}

	@Override
	public List<StreamedContentMetadata> getAllContentMetadata() throws IOException {
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
			throw new IOException("Failed to list S3 content.", e);
		}
	}
}