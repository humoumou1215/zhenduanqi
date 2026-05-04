package com.zhenduanqi.service;

import com.zhenduanqi.client.ArthasHttpClient;
import com.zhenduanqi.config.TokenEncryptionUtil;
import com.zhenduanqi.dto.ArthasServerDTO;
import com.zhenduanqi.entity.ArthasServerEntity;
import com.zhenduanqi.repository.ArthasServerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArthasServerServiceTest {

    @Mock
    private ArthasServerRepository repository;

    @Mock
    private TokenEncryptionUtil encryptionUtil;

    @Mock
    private ArthasHttpClient arthasHttpClient;

    private ArthasServerService service;

    @BeforeEach
    void setUp() {
        service = new ArthasServerService(repository, encryptionUtil, arthasHttpClient);
    }

    @Test
    void create_withValidData_succeeds() {
        ArthasServerDTO dto = new ArthasServerDTO();
        dto.setId("server-1");
        dto.setName("测试服务器");
        dto.setHost("192.168.1.100");
        dto.setHttpPort(8563);

        when(repository.existsById("server-1")).thenReturn(false);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        ArthasServerDTO result = service.create(dto);
        assertThat(result.getId()).isEqualTo("server-1");
    }

    @Test
    void create_withDuplicateId_throwsException() {
        ArthasServerDTO dto = new ArthasServerDTO();
        dto.setId("server-1");
        dto.setName("测试服务器");
        dto.setHost("192.168.1.100");
        dto.setHttpPort(8563);

        when(repository.existsById("server-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("服务器 ID 已存在");
    }

    @Test
    void create_withEmptyId_throwsException() {
        ArthasServerDTO dto = new ArthasServerDTO();
        dto.setId("");
        dto.setName("测试服务器");
        dto.setHost("192.168.1.100");
        dto.setHttpPort(8563);

        when(repository.existsById("")).thenReturn(false);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("服务器 ID 不能为空");
    }

    @Test
    void create_withInvalidIdFormat_throwsException() {
        ArthasServerDTO dto = new ArthasServerDTO();
        dto.setId("server@1");
        dto.setName("测试服务器");
        dto.setHost("192.168.1.100");
        dto.setHttpPort(8563);

        when(repository.existsById("server@1")).thenReturn(false);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("服务器 ID 格式无效");
    }

    @Test
    void create_withEmptyName_throwsException() {
        ArthasServerDTO dto = new ArthasServerDTO();
        dto.setId("server-1");
        dto.setName("");
        dto.setHost("192.168.1.100");
        dto.setHttpPort(8563);

        when(repository.existsById("server-1")).thenReturn(false);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("服务器名称不能为空");
    }

    @Test
    void create_withEmptyHost_throwsException() {
        ArthasServerDTO dto = new ArthasServerDTO();
        dto.setId("server-1");
        dto.setName("测试服务器");
        dto.setHost("");
        dto.setHttpPort(8563);

        when(repository.existsById("server-1")).thenReturn(false);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("主机地址不能为空");
    }

    @Test
    void create_withInvalidHostFormat_throwsException() {
        ArthasServerDTO dto = new ArthasServerDTO();
        dto.setId("server-1");
        dto.setName("测试服务器");
        dto.setHost("invalid host!");
        dto.setHttpPort(8563);

        when(repository.existsById("server-1")).thenReturn(false);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("主机地址格式无效");
    }

    @Test
    void create_withInvalidPort_throwsException() {
        ArthasServerDTO dto = new ArthasServerDTO();
        dto.setId("server-1");
        dto.setName("测试服务器");
        dto.setHost("192.168.1.100");
        dto.setHttpPort(70000);

        when(repository.existsById("server-1")).thenReturn(false);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("端口范围必须在 1-65535 之间");
    }

    @Test
    void create_withValidHostname_succeeds() {
        ArthasServerDTO dto = new ArthasServerDTO();
        dto.setId("server-1");
        dto.setName("测试服务器");
        dto.setHost("example.com");
        dto.setHttpPort(8563);

        when(repository.existsById("server-1")).thenReturn(false);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        ArthasServerDTO result = service.create(dto);
        assertThat(result.getId()).isEqualTo("server-1");
    }

    @Test
    void update_withNonExistentId_throwsException() {
        ArthasServerDTO dto = new ArthasServerDTO();
        dto.setName("更新名称");
        dto.setHost("192.168.1.100");
        dto.setHttpPort(8563);

        when(repository.findById("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("non-existent", dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("服务器不存在");
    }

    @Test
    void delete_withNonExistentId_throwsException() {
        when(repository.existsById("non-existent")).thenReturn(false);

        assertThatThrownBy(() -> service.delete("non-existent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("服务器不存在");
    }
}
