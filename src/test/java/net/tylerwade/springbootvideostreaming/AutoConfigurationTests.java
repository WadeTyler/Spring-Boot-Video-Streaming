package net.tylerwade.springbootvideostreaming;

import net.tylerwade.springbootvideostreaming.adapter.ContentStreamAdapter;
import net.tylerwade.springbootvideostreaming.adapter.LocalContentStreamAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AutoConfigurationTests {

	@Autowired
	private ContentStreamAdapter contentStreamAdapter;

	@Test
	void contentStreamAdapter_isAutowired() {
		assertNotNull(contentStreamAdapter);
	}

	@Test
	void contentStreamAdapter_isLocalContentStreamAdapter() {
		assertNotNull(contentStreamAdapter);
		assertEquals(LocalContentStreamAdapter.class, contentStreamAdapter.getClass());
	}

}