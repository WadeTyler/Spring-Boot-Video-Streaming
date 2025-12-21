package net.tylerwade.springbootvideostreaming.adapter;

import net.tylerwade.springbootvideostreaming.model.Range;
import net.tylerwade.springbootvideostreaming.model.StreamContentRequest;
import net.tylerwade.springbootvideostreaming.model.StreamedContent;
import net.tylerwade.springbootvideostreaming.model.StreamedContentMetadata;

import java.io.IOException;
import java.util.List;

public interface ContentStreamAdapter {

	static final long MAX_CHUNK_SIZE = 1024 * 1024L; // 1MB

	StreamedContent loadContent(StreamContentRequest contentRequest) throws IOException;

	Long getContentSize(String key) throws IOException;

	StreamedContentMetadata getContentMetadata(String key) throws IOException;

	List<StreamedContentMetadata> getAllContentMetadata() throws IOException;

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

	default String extractContentType(String fileName) {
		return fileName.contains(".")
				? "video/" + fileName.substring(fileName.lastIndexOf(".") + 1)
				: "application/octet-stream";
	}

}