package com.na.naknak.server.upload.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.upload.infrastructure.S3Uploader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {

    @InjectMocks
    private UploadService uploadService;

    @Mock
    private S3Uploader s3Uploader;

    private MultipartFile imageFile(String name) {
        return new MockMultipartFile("files", name, "image/jpeg", "content".getBytes());
    }

    @Test
    void 이미지_업로드_성공() {
        // given
        MultipartFile file1 = imageFile("a.jpg");
        MultipartFile file2 = imageFile("b.png");
        given(s3Uploader.upload(file1)).willReturn("https://cdn/uploads/a.jpg");
        given(s3Uploader.upload(file2)).willReturn("https://cdn/uploads/b.png");

        // when
        List<String> urls = uploadService.uploadImages(List.of(file1, file2));

        // then
        assertThat(urls).containsExactly("https://cdn/uploads/a.jpg", "https://cdn/uploads/b.png");
    }

    @Test
    void 이미지가_아닌_파일_예외() {
        // given
        MultipartFile textFile = new MockMultipartFile("files", "a.txt", "text/plain", "content".getBytes());

        // when & then
        assertThatThrownBy(() -> uploadService.uploadImages(List.of(textFile)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 빈_파일_예외() {
        // given
        MultipartFile empty = new MockMultipartFile("files", "a.jpg", "image/jpeg", new byte[0]);

        // when & then
        assertThatThrownBy(() -> uploadService.uploadImages(List.of(empty)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 최대_개수_초과_예외() {
        // given
        List<MultipartFile> files = List.of(
                imageFile("1.jpg"), imageFile("2.jpg"), imageFile("3.jpg"),
                imageFile("4.jpg"), imageFile("5.jpg"), imageFile("6.jpg")
        );

        // when & then
        assertThatThrownBy(() -> uploadService.uploadImages(files))
                .isInstanceOf(BusinessException.class);
    }
}
