package com.xieh.springbootminio.vo;

/**
 * @author 谢辉
 * @Classname Res
 * @Description Minio响应类
 * @Date 2020/10/13 21:46
 */
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Res implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer code;
    private Object data = "";
    private String message = "";
}
