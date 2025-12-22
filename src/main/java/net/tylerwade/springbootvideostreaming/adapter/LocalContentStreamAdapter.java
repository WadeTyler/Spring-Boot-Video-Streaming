package net.tylerwade.springbootvideostreaming.adapter;

import net.tylerwade.springbootvideostreaming.model.Range;
import net.tylerwade.springbootvideostreaming.model.StreamContentRequest;
import net.tylerwade.springbootvideostreaming.model.StreamedContent;
import net.tylerwade.springbootvideostreaming.model.StreamedContentMetadata;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

/**
 * The {@code LocalContentStreamAdapter} class implements the {@code ContentStreamAdapter} interface
 * and provides functionality to stream content from local resources. It retrieves video files or similar
 * resources stored in a specified directory and enables chunk-based streaming, metadata retrieval,
 * and content range validation.
 * <p>
 * This implementation uses a {@code ResourceLoader} to load resources and manages a directory of
 * resources, defaulting to "classpath:videos" if no directory is specified.
 */
public class LocalContentStreamAdapter implements ContentStreamAdapter {

	private final ResourceLoader resourceLoader;

	/**
	 * Defaults to "classpath:videos" if not provided.
	 */
	private final String videosDirectory;

	public LocalContentStreamAdapter(ResourceLoader resourceLoader, String videosDirectory) {
		this.resourceLoader = resourceLoader;
		this.videosDirectory = "classpath:" + videosDirectory;
	}

	public LocalContentStreamAdapter(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
		this.videosDirectory = "classpath:videos";
	}

	@Override
	public StreamedContent loadContent(StreamContentRequest contentRequest) throws MissingResourceException, IOException {
		Resource resource = loadResource(contentRequest.getKey());

		Long fileSize = resource.contentLength();
		Range validRange = createValidRange(contentRequest.getRange(), fileSize);

		Long contentLength = validRange.getEnd() - validRange.getStart() + 1;

		StreamingResponseBody content = readContent(resource, validRange.getStart(), contentLength);

		StreamedContentMetadata metadata = StreamedContentMetadata.builder()
				.key(resource.getFilename())
				.contentType(extractContentType(contentRequest.getKey()))
				.fileSize(fileSize)
				.build();

		return StreamedContent.builder()
				.key(resource.getFilename())
				.metadata(metadata)
				.content(content)
				.contentLength(contentLength)
				.range(validRange)
				.build();
	}

	private StreamingResponseBody readContent(Resource videoResource, Long start, Long contentLength) {
		return outputStream -> {
			try (InputStream is = videoResource.getInputStream()) {
				is.skipNBytes(start);

				byte[] buffer = new byte[8192];
				long remaining = contentLength;
				int read;

				while (remaining > 0 && (read = is.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
					outputStream.write(buffer, 0, read);
					remaining -= read;
				}
				outputStream.flush();
			}
		};
	}

	@Override
	public Long getContentSize(String key) throws MissingResourceException, IOException {
		return loadResource(key).contentLength();
	}

	@Override
	public StreamedContentMetadata getContentMetadata(String key) throws MissingResourceException, IOException {
		Resource resource = loadResource(key);

		return StreamedContentMetadata.builder()
				.key(key)
				.contentType(extractContentType(key))
				.fileSize(resource.contentLength())
				.build();
	}

	@Override
	public List<StreamedContentMetadata> getAllContentMetadata() throws IOException {
		// Load all resources in videosDirectory.
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		Resource[] resources = resolver.getResources(videosDirectory + "/*");

		List<StreamedContentMetadata> metadataList = new ArrayList<>();
		for (Resource resource : resources) {
			String fileName = resource.getFilename();

			if (fileName == null) {
				continue;
			}

			StreamedContentMetadata metadata = StreamedContentMetadata.builder()
					.key(fileName)
					.contentType(extractContentType(fileName))
					.fileSize(resource.contentLength())
					.build();

			metadataList.add(metadata);
		}

		return metadataList;
	}

	private Resource loadResource(String key) throws MissingResourceException {
		Resource resource = resourceLoader.getResource(videosDirectory + "/" + key);
		if (!resource.exists()) {
			throw new MissingResourceException(String.format("Resource with key '%s' does not exist.", key), this.getClass().toString(), key);
		}
		return resource;
	}

}