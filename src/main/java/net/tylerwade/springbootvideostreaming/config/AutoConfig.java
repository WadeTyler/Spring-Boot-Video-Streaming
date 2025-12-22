package net.tylerwade.springbootvideostreaming.config;

import lombok.RequiredArgsConstructor;
import net.tylerwade.springbootvideostreaming.adapter.ContentStreamAdapter;
import net.tylerwade.springbootvideostreaming.adapter.LocalContentStreamAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

@AutoConfiguration
@RequiredArgsConstructor
public class AutoConfig {

	private final ResourceLoader resourceLoader;

	@Bean
	@ConditionalOnMissingBean
	public ContentStreamAdapter contentStreamAdapter() {
		return new LocalContentStreamAdapter(resourceLoader);
	}

}