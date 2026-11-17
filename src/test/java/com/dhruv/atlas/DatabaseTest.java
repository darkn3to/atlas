package com.dhruv.atlas;

import com.dhruv.atlas.entity.IngestionSource;
import com.dhruv.atlas.repository.IngestionSourceRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class DatabaseTest {
    @Autowired
    IngestionSourceRepository ingestionSourceRepository;

    @Test
    @Transactional
    void testDatabaseConnectionAndCrud() {
        IngestionSource src = new IngestionSource();
        src.setId(1L);
        src.setName("Test Source");
        src.setSourceType("API");
        src.setSourceUrl("https://api.example.com/data");
        src.setCreatedAt(LocalDateTime.now());

        // 2. Save to the database
        IngestionSource savedSource = ingestionSourceRepository.save(src);

        // 3. Verify it was saved successfully
        assertThat(savedSource).isNotNull();
        assertThat(savedSource.getId()).isEqualTo(1L);

        // 4. Retrieve from the database
        IngestionSource fetchedSource = ingestionSourceRepository.findById(1L).orElse(null);

        // 5. Assertions
        assertThat(fetchedSource).isNotNull();
        assertThat(fetchedSource.getName()).isEqualTo("Test Source");
        assertThat(fetchedSource.getSourceType()).isEqualTo("API");
    }
}
