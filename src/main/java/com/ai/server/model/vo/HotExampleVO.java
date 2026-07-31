package com.ai.server.model.vo;

import com.ai.server.model.entity.HotExampleEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotExampleVO {
    private String title;
    private String describe;

    public HotExampleVO(HotExampleEntity hotExampleEntity) {
        this.title = hotExampleEntity.getTitle();
        this.describe = hotExampleEntity.getDescribe();
    }
}
