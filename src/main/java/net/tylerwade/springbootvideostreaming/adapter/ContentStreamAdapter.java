package net.tylerwade.springbootvideostreaming.adapter;

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

}