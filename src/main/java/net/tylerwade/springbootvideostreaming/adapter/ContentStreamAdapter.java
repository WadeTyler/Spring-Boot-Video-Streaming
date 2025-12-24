package net.tylerwade.springbootvideostreaming.adapter;

import net.tylerwade.springbootvideostreaming.model.Range;
import net.tylerwade.springbootvideostreaming.model.StreamContentRequest;
import net.tylerwade.springbootvideostreaming.model.StreamedContent;
import net.tylerwade.springbootvideostreaming.model.StreamedContentMetadata;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;

/**
 * The {@code ContentStreamAdapter} interface defines the contract for implementing content streaming
 * functionality. It provides methods for loading content in chunks, retrieving metadata, and performing
 * content-based computations such as range validation and MIME type extraction.
 * <p>
 * If a ContentStreamAdapter bean is not manually created, then a {@link LocalContentStreamAdapter} will
 * automatically be created by {@link net.tylerwade.springbootvideostreaming.config.AutoConfig}
 */
public interface ContentStreamAdapter {

	long MAX_CHUNK_SIZE = 1024 * 1024L; // 1MB

	/**
	 * Loads a segment of content as specified by the given {@code StreamContentRequest}.
	 * The content is retrieved based on the resource key and the specified range for streaming.
	 *
	 * @param contentRequest the request containing the key to identify the content
	 *                       and the range of bytes to be streamed.
	 * @return a {@code StreamedContent} object containing the requested content segment,
	 *         its metadata, and details about the range and content length.
	 */
	Mono<StreamedContent> loadContent(StreamContentRequest contentRequest);

	/**
	 * Retrieves the size of the content associated with the specified key.
	 *
	 * @param key the unique identifier for the content whose size is to be retrieved
	 * @return the size of the content in bytes
	 * @throws IOException if an I/O error occurs while accessing the content
	 */
	Long getContentSize(String key) throws IOException;

	/**
	 * Retrieves*/
	StreamedContentMetadata getContentMetadata(String key) throws IOException;

	/**
	 * Retrieves metadata for all available streamed content.
	 *
	 * @return a list of {@code StreamedContentMetadata} objects, each containing metadata
	 *         such as the key, content type, and size of the corresponding resource
	 * @throws IOException if an error occurs while accessing or reading the resources
	 */
	List<StreamedContentMetadata> getAllContentMetadata() throws IOException;

	/**
	 * Creates a valid range object based on the requested range and the size of the file.
	 * This method ensures that the returned range adheres to constraints such as the maximum
	 * chunk size {@code MAX_CHUNK_SIZE} and the file size boundary.
	 * If the requested range is invalid or null, the range is adjusted to fall within acceptable limits.
	 *
	 * @param requestedRange the requested range with start and end byte positions, which may be null
	 *                       or have null values for start or end
	 * @param fileSize the size of the file in bytes, used to enforce range boundaries
	 * @return a {@code Range} object representing the validated range
	 */
	default Range createValidRange(Range requestedRange, Long fileSize) {
		if (requestedRange == null) {
			requestedRange = new Range(0L, fileSize - 1L);
		}

		Long start = requestedRange.getStart() == null ? 0L : requestedRange.getStart();
		Long end = requestedRange.getEnd();

		if (end == null || end - start + 1 > MAX_CHUNK_SIZE) {
			end = Math.min(start + MAX_CHUNK_SIZE - 1, fileSize - 1);
		}

		return new Range(start, end);
	}

	/**
	 * Extracts the content type of a given file based on its file extension.
	 * If the file name contains a valid extension, the method generates a MIME
	 * type in the format "video/{extension}". If no extension is found, the method
	 * defaults to "application/octet-stream".
	 *
	 * @param fileName the name of the file, potentially including an extension
	 * @return a string representing the MIME type of the file based on its extension,
	 *         or "application/octet-stream" if no valid extension is present
	 */
	default String extractContentType(String fileName) {
		return fileName.contains(".")
				? "video/" + fileName.substring(fileName.lastIndexOf(".") + 1)
				: "application/octet-stream";
	}


	default long getMaxChunkSize() {
		return MAX_CHUNK_SIZE;
	}
}