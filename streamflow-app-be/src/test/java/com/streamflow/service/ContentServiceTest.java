package com.streamflow.service;

import com.streamflow.dto.ContentDetailResponse;
import com.streamflow.dto.CreateContentRequest;
import com.streamflow.entity.Content;
import com.streamflow.entity.SeriesSeason;
import com.streamflow.exception.BadRequestException;
import com.streamflow.exception.ResourceNotFoundException;
import com.streamflow.repository.ContentRepository;
import com.streamflow.repository.EpisodeRepository;
import com.streamflow.repository.SeriesSeasonRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.streamflow.config.TestFixtures.CONTENT_ID;
import static com.streamflow.config.TestFixtures.createContentRequest;
import static com.streamflow.entity.enums.ContentType.MOVIE;
import static com.streamflow.entity.enums.ContentType.SERIES;
import static com.streamflow.entity.enums.PublishStatus.DRAFT;
import static com.streamflow.entity.enums.PublishStatus.PUBLISHED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock
    private ContentRepository contentRepository;
    @Mock
    private SeriesSeasonRepository seriesSeasonRepository;
    @Mock
    private EpisodeRepository episodeRepository;

    @InjectMocks
    private ContentService contentService;

    @Nested
    @DisplayName("createContent")
    class CreateContent {

        @Test
        @DisplayName("creates content in DRAFT and returns detail")
        void createsContentInDraft() {
            CreateContentRequest request = createContentRequest("My Movie", MOVIE);
            Content saved = new Content();
            saved.setId(CONTENT_ID);
            saved.setTitle("My Movie");
            saved.setContentType(MOVIE);
            saved.setPublishStatus(DRAFT);
            when(contentRepository.save(any(Content.class))).thenReturn(saved);
            lenient().when(seriesSeasonRepository.findByContentIdOrderBySeasonNumberAsc(CONTENT_ID)).thenReturn(List.of());

            ContentDetailResponse response = contentService.createContent(request);

            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo("My Movie");
            assertThat(response.getPublishStatus()).isEqualTo(DRAFT);
            ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
            verify(contentRepository).save(captor.capture());
            assertThat(captor.getValue().getTitle()).isEqualTo("My Movie");
            assertThat(captor.getValue().getPublishStatus()).isEqualTo(DRAFT);
        }
    }

    @Nested
    @DisplayName("publishContent")
    class PublishContent {

        @Test
        @DisplayName("throws when content not found")
        void throwsWhenContentNotFound() {
            when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> contentService.publishContent(CONTENT_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Content");
        }

        @Test
        @DisplayName("throws when content not DRAFT")
        void throwsWhenNotDraft() {
            Content content = new Content();
            content.setId(CONTENT_ID);
            content.setPublishStatus(PUBLISHED);
            when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));

            assertThatThrownBy(() -> contentService.publishContent(CONTENT_ID))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("DRAFT");
        }

        @Test
        @DisplayName("publishes content and returns detail")
        void publishesContent() {
            Content content = new Content();
            content.setId(CONTENT_ID);
            content.setTitle("Movie");
            content.setPublishStatus(DRAFT);
            content.setContentType(MOVIE);
            when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));
            when(contentRepository.save(any(Content.class))).thenAnswer(i -> i.getArgument(0));
            lenient().when(seriesSeasonRepository.findByContentIdOrderBySeasonNumberAsc(CONTENT_ID)).thenReturn(List.of());

            ContentDetailResponse response = contentService.publishContent(CONTENT_ID);

            assertThat(response.getPublishStatus()).isEqualTo(PUBLISHED);
            verify(contentRepository).save(content);
            assertThat(content.getPublishStatus()).isEqualTo(PUBLISHED);
        }
    }

    @Nested
    @DisplayName("getContentDetail")
    class GetContentDetail {

        @Test
        @DisplayName("throws when content not found")
        void throwsWhenNotFound() {
            when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> contentService.getContentDetail(CONTENT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("returns detail when content exists")
        void returnsDetailWhenExists() {
            Content content = new Content();
            content.setId(CONTENT_ID);
            content.setTitle("Title");
            content.setContentType(MOVIE);
            content.setPublishStatus(PUBLISHED);
            when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));
            lenient().when(seriesSeasonRepository.findByContentIdOrderBySeasonNumberAsc(CONTENT_ID)).thenReturn(List.of());

            ContentDetailResponse response = contentService.getContentDetail(CONTENT_ID);

            assertThat(response.getId()).isEqualTo(CONTENT_ID);
            assertThat(response.getTitle()).isEqualTo("Title");
        }
    }
}
