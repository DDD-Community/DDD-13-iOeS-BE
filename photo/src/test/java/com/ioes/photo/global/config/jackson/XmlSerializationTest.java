package com.ioes.photo.global.config.jackson;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * XML 직렬화/역직렬화 테스트.
 * 이 테스트는 해당 컨버터의 기반이 되는 XmlMapper가 정상 동작하는지 검증합니다.
 *
 * @author 황제연
 */
@DisplayName("Jackson XML 직렬화/역직렬화 테스트")
class XmlSerializationTest {

    private XmlMapper xmlMapper;

    @BeforeEach
    void setUp() {
        xmlMapper = new XmlMapper();
    }

    @Test
    @DisplayName("jackson-dataformat-xml 의존성이 클래스패스에 존재한다 (XmlMapper 인스턴스 생성)")
    void xmlMapper_canBeInstantiated() {
        assertThatCode(XmlMapper::new).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("객체를 XML 문자열로 직렬화할 수 있다")
    void serialize_toXml() throws Exception {
        PhotoDto dto = new PhotoDto("photo-001", "테스트 이미지");

        String xml = xmlMapper.writeValueAsString(dto);

        assertThat(xml).contains("<id>photo-001</id>");
        assertThat(xml).contains("<title>테스트 이미지</title>");
    }

    @Test
    @DisplayName("XML 문자열을 객체로 역직렬화할 수 있다")
    void deserialize_fromXml() throws Exception {
        String xml = "<PhotoDto><id>photo-001</id><title>테스트 이미지</title></PhotoDto>";

        PhotoDto result = xmlMapper.readValue(xml, PhotoDto.class);

        assertThat(result.getId()).isEqualTo("photo-001");
        assertThat(result.getTitle()).isEqualTo("테스트 이미지");
    }

    @Test
    @DisplayName("XML 직렬화 후 역직렬화 시 원본 객체와 동일하다 (round-trip)")
    void xml_roundTrip() throws Exception {
        PhotoDto original = new PhotoDto("round-trip-id", "왕복 테스트 제목");

        String xml = xmlMapper.writeValueAsString(original);
        PhotoDto result = xmlMapper.readValue(xml, PhotoDto.class);

        assertThat(result.getId()).isEqualTo(original.getId());
        assertThat(result.getTitle()).isEqualTo(original.getTitle());
    }

    @Test
    @DisplayName("숫자·불리언 타입 필드도 XML로 올바르게 직렬화된다")
    void serialize_primitiveTypes() throws Exception {
        MetaDto dto = new MetaDto(42, true);

        String xml = xmlMapper.writeValueAsString(dto);

        assertThat(xml).contains("<count>42</count>");
        assertThat(xml).contains("<active>true</active>");
    }

    @Test
    @DisplayName("숫자·불리언 타입 필드를 XML에서 올바르게 역직렬화한다")
    void deserialize_primitiveTypes() throws Exception {
        String xml = "<MetaDto><count>42</count><active>true</active></MetaDto>";

        MetaDto result = xmlMapper.readValue(xml, MetaDto.class);

        assertThat(result.getCount()).isEqualTo(42);
        assertThat(result.isActive()).isTrue();
    }

    // ---- 내부 테스트용 DTO ----

    static class PhotoDto {
        private String id;
        private String title;

        PhotoDto() {}

        PhotoDto(String id, String title) {
            this.id = id;
            this.title = title;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
    }

    static class MetaDto {
        private int count;
        private boolean active;

        MetaDto() {}

        MetaDto(int count, boolean active) {
            this.count = count;
            this.active = active;
        }

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}