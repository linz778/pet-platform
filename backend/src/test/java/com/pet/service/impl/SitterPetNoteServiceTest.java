package com.pet.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pet.common.api.ResultCode;
import com.pet.common.exception.BusinessException;
import com.pet.dto.SitterPetNoteSaveDTO;
import com.pet.entity.Order;
import com.pet.entity.Pet;
import com.pet.entity.SitterPetNote;
import com.pet.mapper.OrderMapper;
import com.pet.mapper.PetMapper;
import com.pet.mapper.SitterPetNoteMapper;
import com.pet.security.LoginUser;
import com.pet.security.UserContext;
import com.pet.vo.SitterPetNoteVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SitterPetNoteServiceTest {

    private static final long SITTER_ID = 3L;

    @Mock private SitterPetNoteMapper noteMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private PetMapper petMapper;

    private SitterPetNoteServiceImpl service;

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        MapperBuilderAssistant noteAssistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        noteAssistant.setCurrentNamespace(SitterPetNoteMapper.class.getName());
        TableInfoHelper.initTableInfo(noteAssistant, SitterPetNote.class);
        MapperBuilderAssistant orderAssistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        orderAssistant.setCurrentNamespace(OrderMapper.class.getName());
        TableInfoHelper.initTableInfo(orderAssistant, Order.class);
    }

    @BeforeEach
    void setUp() {
        service = new SitterPetNoteServiceImpl(orderMapper, petMapper);
        ReflectionTestUtils.setField(service, "baseMapper", noteMapper);
        UserContext.set(new LoginUser(SITTER_ID, "sitter", "SITTER"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("接单员可以为服务过的宠物保存私人照护手记")
    void savesNoteForServedPet() {
        when(orderMapper.selectCount(any())).thenReturn(1L);
        Pet pet = new Pet();
        pet.setId(8L);
        pet.setName("奶糖");
        pet.setSpecies("猫");
        when(petMapper.selectSnapshots(List.of(8L))).thenReturn(List.of(pet));
        when(noteMapper.selectOne(any(), anyBoolean())).thenReturn(null);
        when(noteMapper.insert(any(SitterPetNote.class))).thenAnswer(invocation -> {
            SitterPetNote note = invocation.getArgument(0);
            note.setId(18L);
            return 1;
        });

        SitterPetNoteSaveDTO dto = new SitterPetNoteSaveDTO();
        dto.setPetId(8L);
        dto.setHabits("  怕生，先让它闻手  ");
        dto.setCareNotes("喜欢羽毛棒");

        SitterPetNoteVO result = service.saveMine(dto);

        assertThat(result.getId()).isEqualTo(18L);
        assertThat(result.getPetName()).isEqualTo("奶糖");
        assertThat(result.getHabits()).isEqualTo("怕生，先让它闻手");
    }

    @Test
    @DisplayName("不能为没有接单服务关系的宠物建立笔记")
    void rejectsUnservedPet() {
        when(orderMapper.selectCount(any())).thenReturn(0L);
        SitterPetNoteSaveDTO dto = new SitterPetNoteSaveDTO();
        dto.setPetId(99L);
        dto.setCareNotes("测试");

        assertThatThrownBy(() -> service.saveMine(dto))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ResultCode.SITTER_PET_NOT_SERVED.getCode());
        verify(petMapper, never()).selectSnapshots(any());
    }
}
